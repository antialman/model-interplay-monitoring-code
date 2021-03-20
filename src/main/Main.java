package main;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.plugins.declareminer.ExecutableAutomaton;
import data.DeclareConstraint;
import data.PropositionData;
import data.proposition.AttributeType;
import utils.AutomatonUtils;
import utils.DeclareModelUtils;
import utils.LogUtils;
import utils.LtlUtils;

public class Main {

	private static PropositionData propositionData = new PropositionData();

	private static List<DeclareConstraint> declareConstraints;
	private static Map<String, AttributeType> attributeTypeMap;

	public static void main(String[] args) {
		//TODO Uncomment and review after the code is done
		//CommandLine cmd = CmdArgsUtil.handleArgs(args);
		//String declareModelPath = cmd.getOptionValue("declareModel");
		//String logPath = cmd.getOptionValue("log");

		String declareModelPath = "input/DrivingTest-Model.decl";
		String logPath = "input/DrivingTest-Log-Negative.xes";


		//Reading the data needed for propositionalization
		try {
			declareConstraints = DeclareModelUtils.readConstraints(declareModelPath);
			attributeTypeMap = DeclareModelUtils.readAttributeTypes(declareModelPath);
		} catch (FileNotFoundException e) {
			System.out.println("Unable to open Declare model: " + declareModelPath);
			e.printStackTrace();
		}

		//Populates the data structure that is used for propositionalization of constraints and also events
		DeclareModelUtils.updatePropositionData(declareConstraints, attributeTypeMap, propositionData);

		//Creating propositionalized ltl formulas for each declare constraint 
		Map<DeclareConstraint, String> ltlFormulaMap = LtlUtils.getPropositionalizedLtlFormulaMap(declareConstraints, propositionData);

		//Creates the individual automatons for each constraint
		Map<ExecutableAutomaton, String> constraintAutomata = AutomatonUtils.createConstraintAutomata(ltlFormulaMap);

		//Creates the global automaton
		ExecutableAutomaton globalAutomaton = AutomatonUtils.createGlobalAutomaton(ltlFormulaMap);

		//TODO: colouring of the automata
		Map<String, Map<ExecutableAutomaton, String>> globalAutomatonColours = AutomatonUtils.getGlobalAutomatonColours(globalAutomaton, constraintAutomata);


		//Map for tracking automata states
		Map<ExecutableAutomaton, String> truthValues = new HashMap<ExecutableAutomaton, String>(constraintAutomata.size());
		String globalTruthValue;

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
				truthValues.put(executableAutomaton, "init");
			}
			globalAutomaton.ini();
			globalTruthValue = "init";

			for (XEvent xevent : xtrace) {
				String eventProposition = LogUtils.getEventProposition(xevent, propositionData);

				globalTruthValue = AutomatonUtils.execPropositionOnAutomaton(eventProposition, globalAutomaton, null);
				String globalState = globalAutomaton.currentState().get(0).toString();
				System.out.println("Global state: " + globalState);
				System.out.println("Global truth value: " + globalTruthValue);

				for (ExecutableAutomaton executableAutomaton : constraintAutomata.keySet()) {
					String newTruthValue = AutomatonUtils.execPropositionOnAutomaton(eventProposition, executableAutomaton, constraintAutomata.get(executableAutomaton));
					truthValues.put(executableAutomaton, newTruthValue);				
					System.out.println("Constraint: " + constraintAutomata.get(executableAutomaton));
					System.out.println("\tTruth value: " + truthValues.get(executableAutomaton));
					System.out.println("\tGlobal colour: " + globalAutomatonColours.get(globalState).get(executableAutomaton));
					if (!truthValues.get(executableAutomaton).equals(globalAutomatonColours.get(globalState).get(executableAutomaton))) {
						System.err.println("Global colour does not match truth value, something is wrong!");
					}
				}
				System.out.println("");
			}

			System.out.println("Final states at trace end");
			String globalState = globalAutomaton.currentState().get(0).toString();
			for (ExecutableAutomaton executableAutomaton : constraintAutomata.keySet()) {
				if (globalAutomatonColours.get(globalState).get(executableAutomaton).equals("poss.sat")) {
					truthValues.put(executableAutomaton, "sat");
				} else if(globalAutomatonColours.get(globalState).get(executableAutomaton).equals("poss.viol")) {
					truthValues.put(executableAutomaton, "viol");
				}
				System.out.println("Constraint: " + constraintAutomata.get(executableAutomaton));
				System.out.println("\t Truth value: " + truthValues.get(executableAutomaton));
			}

			if (globalTruthValue.equals("poss.sat")) {
				globalTruthValue = "sat";
			} else if (globalTruthValue.equals("poss.viol")) {
				globalTruthValue = "viol";
			}
			System.out.println("Global state: " + globalAutomaton.currentState());
			System.out.println("\tTruth value: " + globalTruthValue);
			System.out.println("");
		}

		//test();


	}

	private static void test() {
		//Tests code based on input/DrivingTest-Model.decl
		System.out.println("DrivingLesson");
		System.out.println(propositionData.getActivity("DrivingLesson"));
		System.out.println(propositionData.getActivity("DrivingLesson").getAllPropositions());
		System.out.println("");

		System.out.println("DrivingTest");
		System.out.println(propositionData.getActivity("DrivingTest"));
		System.out.println(propositionData.getActivity("DrivingTest").getAllPropositions());
		System.out.println("");

		System.out.println("GettingLicence");
		System.out.println(propositionData.getActivity("GettingLicence"));
		System.out.println(propositionData.getActivity("GettingLicence").getAllPropositions());
		System.out.println("");


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
		System.out.println("");

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
		System.out.println("");


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
