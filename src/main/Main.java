package main;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.ltl2automaton.plugins.automaton.State;
import org.processmining.ltl2automaton.plugins.automaton.Transition;
import org.processmining.plugins.declareminer.ExecutableAutomaton;
import data.DeclareConstraint;
import data.PropositionData;
import data.proposition.AttributeType;
import utils.AutomatonUtils;
import utils.DeclareModelUtils;
import utils.LogUtils;
import utils.LtlUtils;
import utils.enums.ConstraintState;

public class Main {

	private static PropositionData propositionData = new PropositionData();

	private static List<DeclareConstraint> declareConstrains;
	private static Map<String, AttributeType> attributeTypeMap;

	public static void main(String[] args) {
		//TODO Uncomment and review after the code is done
//		CommandLine cmd = CmdArgsUtil.handleArgs(args);
//		String declareModelPath = cmd.getOptionValue("declareModel");
//		String logPath = cmd.getOptionValue("log");

		String declareModelPath = "input/DrivingTest-Model.decl";
		String logPath = "input/DrivingTest-Log-Negative.xes";


		//Reading the data needed for propositionalization
		try {
			declareConstrains = DeclareModelUtils.readConstraints(declareModelPath);
			attributeTypeMap = DeclareModelUtils.readAttributeTypes(declareModelPath);
		} catch (FileNotFoundException e) {
			System.out.println("Unable to open Declare model: " + declareModelPath);
			e.printStackTrace();
			System.exit(-1);
		}

		//Populates the data structure that is used for propositionalization of constraints and also events
		DeclareModelUtils.updatePropositionData(declareConstrains, attributeTypeMap, propositionData);

		//Creating propositionalized ltl formulas for each declare constraint 
		Map<DeclareConstraint, String> ltlFormulaMap = LtlUtils.getPropositionalizedLtlFormulaMap(declareConstrains, propositionData);

		//Creates the individual automatons for each constraint
		Map<ExecutableAutomaton, DeclareConstraint> constraintAutomata = AutomatonUtils.createConstraintAutomata(ltlFormulaMap);

		//Creates the global automaton
		ExecutableAutomaton globalAutomaton = AutomatonUtils.createGlobalAutomaton(ltlFormulaMap);

		//Colouring of the global automaton
		Map<State, Map<ExecutableAutomaton, ConstraintState>> globalAutomatonColours = AutomatonUtils.getGlobalAutomatonColours(globalAutomaton, constraintAutomata);

		//Calculating the current cost for each node in the global automaton (cost_curr values)
		Map<State, Integer> costCurrMap = AutomatonUtils.getCostCurrMap(globalAutomaton, globalAutomatonColours, constraintAutomata);
		
		//Calculating the best cost for each node in the global automaton (cost_best values)
		Map<State, Integer> costBestMap = AutomatonUtils.getCostBestMap(globalAutomaton, costCurrMap);
		
		//For tracking the truth values of individual constraint automata
		Map<ExecutableAutomaton, ConstraintState> truthValues = new HashMap<ExecutableAutomaton, ConstraintState>(constraintAutomata.size());
		ConstraintState globalTruthValue; //And for tracking the global truth value

		//Replaying the event log
		System.out.println("Replaying the event log");
		XLog xlog = LogUtils.convertToXlog(logPath);
		for (XTrace xtrace : xlog) {
			String traceName = XConceptExtension.instance().extractName(xtrace);
			System.out.println("Trace: " + traceName);
			System.out.println("===========================================");

			//Initialising the automata at the start of each trace
			for (ExecutableAutomaton executableAutomaton : constraintAutomata.keySet()) {
				executableAutomaton.ini();
				truthValues.put(executableAutomaton, ConstraintState.INIT);
			}
			globalAutomaton.ini();
			globalTruthValue = ConstraintState.INIT;

			for (XEvent xevent : xtrace) {
				String eventProposition = LogUtils.getEventProposition(xevent, propositionData);

				globalTruthValue = AutomatonUtils.execPropositionOnAutomaton(eventProposition, globalAutomaton);
				State globalState = globalAutomaton.currentState().get(0);
				System.out.println("Global state: " + globalState);
				System.out.println("Global truth value: " + globalTruthValue);

				//Using individual automata to double-check global automata correctness (functionally not needed)
				for (ExecutableAutomaton executableAutomaton : constraintAutomata.keySet()) {
					ConstraintState newTruthValue = AutomatonUtils.execPropositionOnAutomaton(eventProposition, executableAutomaton);
					truthValues.put(executableAutomaton, newTruthValue);				
					System.out.println("Constraint: " + constraintAutomata.get(executableAutomaton).getConstraintString());
					System.out.println("\tTruth value: " + truthValues.get(executableAutomaton));
					System.out.println("\tGlobal colour: " + globalAutomatonColours.get(globalState).get(executableAutomaton));
					if (!truthValues.get(executableAutomaton).equals(globalAutomatonColours.get(globalState).get(executableAutomaton))) {
						//If this happens then there must be a mistake in either creating or colouring the global automaton
						System.err.println("Global colour does not match truth value, something is wrong!");
					}
				}
				
				//Getting the transitions that lead to best achievable cost from the current state
				Integer bestAchievableCost = costBestMap.get(globalState);
				List<Transition> bestNextTransitions = new ArrayList<Transition>();
				for (Transition t : globalState.getOutput()) {
					if (costBestMap.get(t.getTarget()).intValue() == bestAchievableCost.intValue()) {
						//TODO: Translate transition labels (propositions) into corresponding activities with corresponding attribute value ranges
						bestNextTransitions.add(t);
					}
				}
				System.out.println("Best achievable cost from current state: " + bestAchievableCost);
				System.out.println("\twith transitions: " + bestNextTransitions);
				System.out.println("\tstopping cost: " + costCurrMap.get(globalState));
				
				System.out.println();
			}

			System.out.println("Final states at trace end");
			State globalState = globalAutomaton.currentState().get(0);
			for (ExecutableAutomaton executableAutomaton : constraintAutomata.keySet()) {
				if (globalAutomatonColours.get(globalState).get(executableAutomaton).equals(ConstraintState.POSS_SAT)) {
					truthValues.put(executableAutomaton, ConstraintState.SAT);
				} else if(globalAutomatonColours.get(globalState).get(executableAutomaton).equals(ConstraintState.POSS_VIOL)) {
					truthValues.put(executableAutomaton, ConstraintState.VIOL);
				}
				System.out.println("Constraint: " + constraintAutomata.get(executableAutomaton).getConstraintString());
				System.out.println("\t Truth value: " + truthValues.get(executableAutomaton));
			}

			if (globalTruthValue.equals(ConstraintState.POSS_SAT)) {
				globalTruthValue = ConstraintState.SAT;
			} else if (globalTruthValue.equals(ConstraintState.POSS_VIOL)) {
				globalTruthValue = ConstraintState.VIOL;
			}
			System.out.println("Global state: " + globalAutomaton.currentState());
			System.out.println("\tTruth value: " + globalTruthValue);
			System.out.println();
		}

		//test();


	}

	private static void test() {
		//Tests code based on input/DrivingTest-Model.decl
		System.out.println("DrivingLesson");
		System.out.println(propositionData.getActivity("DrivingLesson"));
		System.out.println(propositionData.getActivity("DrivingLesson").getAllPropositions());
		System.out.println();

		System.out.println("DrivingTest");
		System.out.println(propositionData.getActivity("DrivingTest"));
		System.out.println(propositionData.getActivity("DrivingTest").getAllPropositions());
		System.out.println();

		System.out.println("GettingLicence");
		System.out.println(propositionData.getActivity("GettingLicence"));
		System.out.println(propositionData.getActivity("GettingLicence").getAllPropositions());
		System.out.println();


		System.out.println("DrivingLesson.lessonsDone");
		System.out.println(propositionData.getActivity("DrivingLesson").getAttributes().get("lessonsDone"));
		String condition = "A.lessonsDone is true";
		System.out.println(condition + ": " + propositionData.getActivity("DrivingLesson").getAttributes().get("lessonsDone").getMatchingPropositionNames(condition));
		condition = "A.lessonsDone is not true";
		System.out.println(condition + ": " + propositionData.getActivity("DrivingLesson").getAttributes().get("lessonsDone").getMatchingPropositionNames(condition));
		condition = "A.lessonsDone in (true)";
		System.out.println(condition + ": " + propositionData.getActivity("DrivingLesson").getAttributes().get("lessonsDone").getMatchingPropositionNames(condition));
		condition = "A.lessonsDone in (true, false)";
		System.out.println(condition + ": " + propositionData.getActivity("DrivingLesson").getAttributes().get("lessonsDone").getMatchingPropositionNames(condition));
		condition = "A.lessonsDone not in (true)";
		System.out.println(condition + ": " + propositionData.getActivity("DrivingLesson").getAttributes().get("lessonsDone").getMatchingPropositionNames(condition));
		condition = "A.lessonsDone not in (true,false)";
		System.out.println(condition + ": " + propositionData.getActivity("DrivingLesson").getAttributes().get("lessonsDone").getMatchingPropositionNames(condition));
		System.out.println(propositionData.getActivity("DrivingLesson").getAttributes().get("lessonsDone"));
		System.out.println();

		System.out.println("DrivingTest.grade");
		System.out.println(propositionData.getActivity("DrivingTest").getAttributes().get("grade"));
		condition = "A.grade=1";
		System.out.println(condition + ": " + propositionData.getActivity("DrivingTest").getAttributes().get("grade").getMatchingPropositionNames(condition));
		condition = "A.grade=3";
		System.out.println(condition + ": " + propositionData.getActivity("DrivingTest").getAttributes().get("grade").getMatchingPropositionNames(condition));
		condition = "A.grade!=3";
		System.out.println(condition + ": " + propositionData.getActivity("DrivingTest").getAttributes().get("grade").getMatchingPropositionNames(condition));
		condition = "A.grade>3";
		System.out.println(condition + ": " + propositionData.getActivity("DrivingTest").getAttributes().get("grade").getMatchingPropositionNames(condition));
		condition = "A.grade>=3";
		System.out.println(condition + ": " + propositionData.getActivity("DrivingTest").getAttributes().get("grade").getMatchingPropositionNames(condition));
		condition = "A.grade<3";
		System.out.println(condition + ": " + propositionData.getActivity("DrivingTest").getAttributes().get("grade").getMatchingPropositionNames(condition));
		condition = "A.grade<=3";
		System.out.println(condition + ": " + propositionData.getActivity("DrivingTest").getAttributes().get("grade").getMatchingPropositionNames(condition));
		condition = "A.grade<=b";
		System.out.println(condition + ": " + propositionData.getActivity("DrivingTest").getAttributes().get("grade").getMatchingPropositionNames(condition));
		condition = "A.grade<=-1";
		System.out.println(condition + ": " + propositionData.getActivity("DrivingTest").getAttributes().get("grade").getMatchingPropositionNames(condition));
		condition = "A.grade=-1";
		System.out.println(condition + ": " + propositionData.getActivity("DrivingTest").getAttributes().get("grade").getMatchingPropositionNames(condition));
		condition = "A.grade!=-1";
		System.out.println(condition + ": " + propositionData.getActivity("DrivingTest").getAttributes().get("grade").getMatchingPropositionNames(condition));
		System.out.println();


		condition = "A.grade=1 and A.errorType is a";
		System.out.println(condition + ": " + propositionData.getActivity("DrivingTest").getMatchingPropositionNames(condition));
		condition = "A.grade>3 and A.errorType is a";
		System.out.println(condition + ": " + propositionData.getActivity("DrivingTest").getMatchingPropositionNames(condition));
		condition = "A.grade>3 or A.errorType is a";
		System.out.println(condition + ": " + propositionData.getActivity("DrivingTest").getMatchingPropositionNames(condition));

		condition = "A.grade>3 and A.grade<3";
		System.out.println(condition + ": " + propositionData.getActivity("DrivingTest").getMatchingPropositionNames(condition));
		condition = "A.errorType in (a,b) and A.errorType not in (b,c)";
		System.out.println(condition + ": " + propositionData.getActivity("DrivingTest").getMatchingPropositionNames(condition));
		condition = "A.errorType in (a,b) and A.errorType not in (b,c)";
		System.out.println(condition + ": " + propositionData.getActivity("DrivingTest").getMatchingPropositionNames(condition));


		condition = "A.grade>3";
		System.out.println(condition + ": " + propositionData.getActivity("DrivingTest").getMatchingPropositionNames(condition));
		condition = "A.grade>3 and A.grade>4";
		System.out.println(condition + ": " + propositionData.getActivity("DrivingTest").getMatchingPropositionNames(condition));
		condition = "A.grade>3 and A.grade!=4";
		System.out.println(condition + ": " + propositionData.getActivity("DrivingTest").getMatchingPropositionNames(condition));
	}
}
