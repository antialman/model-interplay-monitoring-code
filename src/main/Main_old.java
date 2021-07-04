package main;

import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.ltl2automaton.plugins.automaton.Automaton;
import org.processmining.ltl2automaton.plugins.automaton.DOTExporter;
import org.processmining.ltl2automaton.plugins.automaton.State;
import org.processmining.ltl2automaton.plugins.automaton.Transition;
import org.processmining.plugins.declareminer.ExecutableAutomaton;

import data.proposition_old.DeclareConstraint;
import data.proposition_old.LtlModel;
import data.proposition_old.PropositionData_old;
import proposition.attribute.AttributeType;
import utils.AutomatonUtils;
import utils.ModelUtils_old;
import utils.LogUtils;
import utils.LtlUtils;
import utils.enums.MonitoringState;

public class Main_old {

	private static PropositionData_old propositionData = new PropositionData_old();

	private static List<DeclareConstraint> declareConstrains;
	private static Map<String, AttributeType> attributeTypeMap;
	private static List<LtlModel> ltlModels;

	public static void main(String[] args) {
		//TODO Uncomment and review after the code is done
//		CommandLine cmd = CmdArgsUtil.handleArgs(args);
//		String declareModelPath = cmd.getOptionValue("declareModel");
//		String ltlModelPath = cmd.getOptionValue("ltlModel");
//		String eventLogPath = cmd.getOptionValue("eventLog");

		String declareModelPath = "input/BPM2021_ltl/notCoexistence_AT-WT.decl";
		String ltlModelPath = "input/BPM2021_ltl/PU_VT_models_ltl.txt";
		String eventLogPath = "input/BPM2021_ltl/eventLog.xes";


		//An test requested by Fabrizio
		Automaton test1 = AutomatonUtils.createAutomatonForLTLFormula_old("!( []( ( performSurgery -> X(( (! ( performSurgery )) U prescribeRehabilitation ) )) ) ) && ( <> ( checkxRayRisk ) ) && ( examinePatient ) && !( ! (applyCast ) U performSurgery ) \\/ ([](!(applyCast))) /\\ (  ! (applyCast ) \\/ (!(X(performSurgery)) /\\ !(X(!(performSurgery))) ) ) && !( ! (removeCast ) U performSurgery ) \\/ ([](!(removeCast))) /\\ (  ! (removeCast ) \\/ (!(X(performSurgery)) /\\ !(X(!(performSurgery))) ) ) && !( []( ( performSurgery -> <>( prescribeRehabilitation ) ) ))");
		System.out.println("test1 done");
		Automaton test2 = AutomatonUtils.createAutomatonForLTLFormula_old("(((( !(checkxRayRisk) U applyCast) \\/ []( !(checkxRayRisk))) /\\ []((checkxRayRisk ->( (!(X(applyCast)) /\\ !(X(!(applyCast))) ) \\/ X((( !(checkxRayRisk) U applyCast) \\/ []( !(checkxRayRisk)))))))) /\\ (  ! (checkxRayRisk ) \\/ (!(X(applyCast)) /\\ !(X(!(applyCast))) ) )) && (((( !(checkxRayRisk) U examinePatient) \\/ []( !(checkxRayRisk))) /\\ []((checkxRayRisk ->( (!(X(examinePatient)) /\\ !(X(!(examinePatient))) ) \\/ X((( !(checkxRayRisk) U examinePatient) \\/ []( !(checkxRayRisk)))))))) /\\ (  ! (checkxRayRisk ) \\/ (!(X(examinePatient)) /\\ !(X(!(examinePatient))) ) )) && (((( !(checkxRayRisk) U performReposition) \\/ []( !(checkxRayRisk))) /\\ []((checkxRayRisk ->( (!(X(performReposition)) /\\ !(X(!(performReposition))) ) \\/ X((( !(checkxRayRisk) U performReposition) \\/ []( !(checkxRayRisk)))))))) /\\ (  ! (checkxRayRisk ) \\/ (!(X(performReposition)) /\\ !(X(!(performReposition))) ) )) && (((( !(checkxRayRisk) U performSurgery) \\/ []( !(checkxRayRisk))) /\\ []((checkxRayRisk ->( (!(X(performSurgery)) /\\ !(X(!(performSurgery))) ) \\/ X((( !(checkxRayRisk) U performSurgery) \\/ []( !(checkxRayRisk)))))))) /\\ (  ! (checkxRayRisk ) \\/ (!(X(performSurgery)) /\\ !(X(!(performSurgery))) ) )) && (((( !(checkxRayRisk) U performxRay) \\/ []( !(checkxRayRisk))) /\\ []((checkxRayRisk ->( (!(X(performxRay)) /\\ !(X(!(performxRay))) ) \\/ X((( !(checkxRayRisk) U performxRay) \\/ []( !(checkxRayRisk)))))))) /\\ (  ! (checkxRayRisk ) \\/ (!(X(performxRay)) /\\ !(X(!(performxRay))) ) )) && (((( !(checkxRayRisk) U removeCast) \\/ []( !(checkxRayRisk))) /\\ []((checkxRayRisk ->( (!(X(removeCast)) /\\ !(X(!(removeCast))) ) \\/ X((( !(checkxRayRisk) U removeCast) \\/ []( !(checkxRayRisk)))))))) /\\ (  ! (checkxRayRisk ) \\/ (!(X(removeCast)) /\\ !(X(!(removeCast))) ) )) && (  <> (checkxRayRisk) /\\ ! ( <> ( ( checkxRayRisk /\\ X(<>(checkxRayRisk)) ) ) ) ) && ( <> ( applyCast ) ) && ( <> ( performReposition ) ) && ( <> ( performSurgery ) ) && ( <> ( performxRay ) ) && ( <> ( removeCast ) ) && ( examinePatient ) && ( ! (performReposition ) U applyCast ) \\/ ([](!(performReposition))) /\\ (  ! (performReposition ) \\/ (!(X(applyCast)) /\\ !(X(!(applyCast))) ) ) && ( ! (performSurgery ) U applyCast ) \\/ ([](!(performSurgery))) /\\ (  ! (performSurgery ) \\/ (!(X(applyCast)) /\\ !(X(!(applyCast))) ) ) && ( ! (performxRay ) U performReposition ) \\/ ([](!(performxRay))) /\\ (  ! (performxRay ) \\/ (!(X(performReposition)) /\\ !(X(!(performReposition))) ) ) && ( ! (performxRay ) U performSurgery ) \\/ ([](!(performxRay))) /\\ (  ! (performxRay ) \\/ (!(X(performSurgery)) /\\ !(X(!(performSurgery))) ) ) && ( ! (applyCast ) U removeCast ) \\/ ([](!(applyCast))) /\\ (  ! (applyCast ) \\/ (!(X(removeCast)) /\\ !(X(!(removeCast))) ) ) && (( ( <>( applyCast ) -> (<>( checkxRayRisk ) )) )) && (( ( <>( applyCast ) -> (<>( performSurgery ) )) )) && (( ( <>( examinePatient ) -> (<>( applyCast ) )) )) && (( ( <>( performReposition ) -> (<>( performSurgery ) )) )) && (( ( <>( performSurgery ) -> (<>( performReposition ) )) )) && (( ( <>( performSurgery ) -> (<>( performxRay ) )) )) && ( []( ( applyCast -> <>( removeCast ) ) ))");
		System.out.println("test2 done");
		Automaton test3 = AutomatonUtils.createAutomatonForLTLFormula_old("!( []( ( performSurgery -> X(( (! ( performSurgery )) U prescribeRehabilitation ) )) ) ) && ( <> ( checkxRayRisk ) ) && ( examinePatient ) && !( ! (applyCast ) U performSurgery ) \\/ ([](!(applyCast))) /\\ (  ! (applyCast ) \\/ (!(X(performSurgery)) /\\ !(X(!(performSurgery))) ) ) && !( ! (removeCast ) U performSurgery ) \\/ ([](!(removeCast))) /\\ (  ! (removeCast ) \\/ (!(X(performSurgery)) /\\ !(X(!(performSurgery))) ) ) && !( []( ( performSurgery -> <>( prescribeRehabilitation ) ) )) && (((( !(checkxRayRisk) U applyCast) \\/ []( !(checkxRayRisk))) /\\ []((checkxRayRisk ->( (!(X(applyCast)) /\\ !(X(!(applyCast))) ) \\/ X((( !(checkxRayRisk) U applyCast) \\/ []( !(checkxRayRisk)))))))) /\\ (  ! (checkxRayRisk ) \\/ (!(X(applyCast)) /\\ !(X(!(applyCast))) ) )) && (((( !(checkxRayRisk) U examinePatient) \\/ []( !(checkxRayRisk))) /\\ []((checkxRayRisk ->( (!(X(examinePatient)) /\\ !(X(!(examinePatient))) ) \\/ X((( !(checkxRayRisk) U examinePatient) \\/ []( !(checkxRayRisk)))))))) /\\ (  ! (checkxRayRisk ) \\/ (!(X(examinePatient)) /\\ !(X(!(examinePatient))) ) )) && (((( !(checkxRayRisk) U performReposition) \\/ []( !(checkxRayRisk))) /\\ []((checkxRayRisk ->( (!(X(performReposition)) /\\ !(X(!(performReposition))) ) \\/ X((( !(checkxRayRisk) U performReposition) \\/ []( !(checkxRayRisk)))))))) /\\ (  ! (checkxRayRisk ) \\/ (!(X(performReposition)) /\\ !(X(!(performReposition))) ) )) && (((( !(checkxRayRisk) U performSurgery) \\/ []( !(checkxRayRisk))) /\\ []((checkxRayRisk ->( (!(X(performSurgery)) /\\ !(X(!(performSurgery))) ) \\/ X((( !(checkxRayRisk) U performSurgery) \\/ []( !(checkxRayRisk)))))))) /\\ (  ! (checkxRayRisk ) \\/ (!(X(performSurgery)) /\\ !(X(!(performSurgery))) ) )) && (((( !(checkxRayRisk) U performxRay) \\/ []( !(checkxRayRisk))) /\\ []((checkxRayRisk ->( (!(X(performxRay)) /\\ !(X(!(performxRay))) ) \\/ X((( !(checkxRayRisk) U performxRay) \\/ []( !(checkxRayRisk)))))))) /\\ (  ! (checkxRayRisk ) \\/ (!(X(performxRay)) /\\ !(X(!(performxRay))) ) )) && (((( !(checkxRayRisk) U removeCast) \\/ []( !(checkxRayRisk))) /\\ []((checkxRayRisk ->( (!(X(removeCast)) /\\ !(X(!(removeCast))) ) \\/ X((( !(checkxRayRisk) U removeCast) \\/ []( !(checkxRayRisk)))))))) /\\ (  ! (checkxRayRisk ) \\/ (!(X(removeCast)) /\\ !(X(!(removeCast))) ) )) && (  <> (checkxRayRisk) /\\ ! ( <> ( ( checkxRayRisk /\\ X(<>(checkxRayRisk)) ) ) ) ) && ( <> ( applyCast ) ) && ( <> ( performReposition ) ) && ( <> ( performSurgery ) ) && ( <> ( performxRay ) ) && ( <> ( removeCast ) ) && ( examinePatient ) && ( ! (performReposition ) U applyCast ) \\/ ([](!(performReposition))) /\\ (  ! (performReposition ) \\/ (!(X(applyCast)) /\\ !(X(!(applyCast))) ) ) && ( ! (performSurgery ) U applyCast ) \\/ ([](!(performSurgery))) /\\ (  ! (performSurgery ) \\/ (!(X(applyCast)) /\\ !(X(!(applyCast))) ) ) && ( ! (performxRay ) U performReposition ) \\/ ([](!(performxRay))) /\\ (  ! (performxRay ) \\/ (!(X(performReposition)) /\\ !(X(!(performReposition))) ) ) && ( ! (performxRay ) U performSurgery ) \\/ ([](!(performxRay))) /\\ (  ! (performxRay ) \\/ (!(X(performSurgery)) /\\ !(X(!(performSurgery))) ) ) && ( ! (applyCast ) U removeCast ) \\/ ([](!(applyCast))) /\\ (  ! (applyCast ) \\/ (!(X(removeCast)) /\\ !(X(!(removeCast))) ) ) && (( ( <>( applyCast ) -> (<>( checkxRayRisk ) )) )) && (( ( <>( applyCast ) -> (<>( performSurgery ) )) )) && (( ( <>( examinePatient ) -> (<>( applyCast ) )) )) && (( ( <>( performReposition ) -> (<>( performSurgery ) )) )) && (( ( <>( performSurgery ) -> (<>( performReposition ) )) )) && (( ( <>( performSurgery ) -> (<>( performxRay ) )) )) && ( []( ( applyCast -> <>( removeCast ) ) ))");
		System.out.println("test3 done");
		
		FileWriter w;
		try {
			w = new FileWriter("test.dot");
			DOTExporter.exportToDot(test3, "test", w);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		Automaton test4 = test1.op.intersect(test2);
		
		//Reading the data needed for propositionalization
		try {
			declareConstrains = ModelUtils_old.readConstraints(declareModelPath);
			attributeTypeMap = ModelUtils_old.readAttributeTypes(declareModelPath);
		} catch (FileNotFoundException e) {
			System.out.println("Unable to open Declare model: " + declareModelPath);
			e.printStackTrace();
			System.exit(-1);
		}

		//Populates the data structure that is used for propositionalization of constraints and also events
		ModelUtils_old.updatePropositionData(declareConstrains, attributeTypeMap, propositionData);
		
		if (ltlModelPath != null) {
			try {
				ltlModels = LtlUtils.processLtlModelFile(ltlModelPath, propositionData);
			} catch (FileNotFoundException e) {
				System.out.println("Unable to open LTL model file: " + ltlModelPath);
				System.out.println("Continuing with only the Declare model");
				e.printStackTrace();
			}
		}
		

		//Creating propositionalized ltl formulas for each declare constraint 
		Map<DeclareConstraint, String> ltlFormulaMap = LtlUtils.getPropositionalizedLtlFormulaMap(declareConstrains, propositionData);
		
		Map<LtlModel, String> ltlModelFormulaMap = LtlUtils.getPropositionalizedLtlModelFormulaMap(ltlModels, propositionData);

		//Creates the individual automatons for each constraint
		System.out.println("Creating the individual automata");
		Map<ExecutableAutomaton, DeclareConstraint> constraintAutomata = AutomatonUtils.createConstraintAutomata(ltlFormulaMap);
		
		Map<ExecutableAutomaton, LtlModel> ltlModelAutomata = AutomatonUtils.createLtlModelAutomata(ltlModelFormulaMap);

		//Creates the global automaton
		System.out.println("Creating the global automaton");
		ExecutableAutomaton globalAutomaton = AutomatonUtils.createGlobalAutomaton(ltlFormulaMap, ltlModelFormulaMap);

		//Colouring of the global automaton
		Map<State, Map<ExecutableAutomaton, MonitoringState>> globalAutomatonColours = AutomatonUtils.getGlobalAutomatonColours(globalAutomaton, constraintAutomata, ltlModelAutomata);

		//Calculating the current cost for each node in the global automaton (cost_curr values)
		Map<State, Integer> costCurrMap = AutomatonUtils.getCostCurrMap(globalAutomaton, globalAutomatonColours, constraintAutomata, ltlModelAutomata);
		
		//Calculating the best cost for each node in the global automaton (cost_best values)
		Map<State, Integer> costBestMap = AutomatonUtils.getCostBestMap_old(globalAutomaton, costCurrMap);
		
		//For tracking the truth values of individual constraint automata
		Map<ExecutableAutomaton, MonitoringState> truthValues = new HashMap<ExecutableAutomaton, MonitoringState>(constraintAutomata.size());
		MonitoringState globalTruthValue; //And for tracking the global truth value

		//Replaying the event log
		System.out.println("Replaying the event log");
		XLog xlog = LogUtils.convertToXlog(eventLogPath);
		for (XTrace xtrace : xlog) {
			String traceName = XConceptExtension.instance().extractName(xtrace);
			System.out.println("Trace: " + traceName);
			System.out.println("===========================================");

			//Initialising the automata at the start of each trace
			for (ExecutableAutomaton executableAutomaton : constraintAutomata.keySet()) {
				executableAutomaton.ini();
				truthValues.put(executableAutomaton, MonitoringState.INIT);
			}
			for (ExecutableAutomaton executableAutomaton : ltlModelAutomata.keySet()) {
				executableAutomaton.ini();
				truthValues.put(executableAutomaton, MonitoringState.INIT);
			}
			globalAutomaton.ini();
			globalTruthValue = MonitoringState.INIT;

			for (XEvent xevent : xtrace) {
				String eventName = XConceptExtension.instance().extractName(xevent);
				System.out.println("Next event in event log: " + eventName);
				System.out.println("-------------------------------------------");
				
				String eventProposition = LogUtils.getEventProposition_old(xevent, propositionData);

				globalTruthValue = AutomatonUtils.execPropositionOnAutomaton(eventProposition, globalAutomaton);
				State globalState = globalAutomaton.currentState().get(0);
				System.out.println("Global state: " + globalState);
				System.out.println("Global truth value: " + globalTruthValue);

				//Using individual automata to double-check global automata correctness (functionally not needed)
				for (ExecutableAutomaton executableAutomaton : constraintAutomata.keySet()) {
					MonitoringState newTruthValue = AutomatonUtils.execPropositionOnAutomaton(eventProposition, executableAutomaton);
					truthValues.put(executableAutomaton, newTruthValue);				
					System.out.println("Constraint: " + constraintAutomata.get(executableAutomaton).getConstraintString());
					System.out.println("\tTruth value: " + truthValues.get(executableAutomaton));
					System.out.println("\tGlobal colour: " + globalAutomatonColours.get(globalState).get(executableAutomaton));
					if (!truthValues.get(executableAutomaton).equals(globalAutomatonColours.get(globalState).get(executableAutomaton))) {
						//If this happens then there must be a mistake in either creating or colouring the global automaton
						System.err.println("Global colour does not match truth value, something is wrong!");
					}
				}
				for (ExecutableAutomaton executableAutomaton : ltlModelAutomata.keySet()) {
					MonitoringState newTruthValue = AutomatonUtils.execPropositionOnAutomaton(eventProposition, executableAutomaton);
					truthValues.put(executableAutomaton, newTruthValue);				
					System.out.println("Model: " + ltlModelAutomata.get(executableAutomaton).getName());
					System.out.println("\tTruth value: " + truthValues.get(executableAutomaton));
					System.out.println("\tGlobal colour: " + globalAutomatonColours.get(globalState).get(executableAutomaton));
					if (!truthValues.get(executableAutomaton).equals(globalAutomatonColours.get(globalState).get(executableAutomaton))) {
						//If this happens then there must be a mistake in either creating or colouring the global automaton
						System.err.println("Global colour does not match truth value, something is wrong!");
					}
				}
				
				//Getting the transitions that lead to best achievable cost from the current state
				Integer bestAchievableCost = costBestMap.get(globalState);
				Map<State, List<Transition>> bestNextTransitions = new HashMap<State, List<Transition>>();
				for (Transition t : globalState.getOutput()) {
					if (costBestMap.get(t.getTarget()).intValue() == bestAchievableCost.intValue()) {
						if (!bestNextTransitions.containsKey(t.getTarget())) {
							bestNextTransitions.put(t.getTarget(), new ArrayList<Transition>());
						}
						bestNextTransitions.get(t.getTarget()).add(t);
					}
				}
				
				//Printing the recommendations for the next course of action
				System.out.println("---");
				System.out.println("Best achievable cost from current state: " + bestAchievableCost);
				System.out.println("Number of options for achieving the best cost: " + bestNextTransitions.size());
				int optionNr = 0;
				//Considering each transition as a separate option
				for (List<Transition> transitions : bestNextTransitions.values()) {
					optionNr++;
					System.out.println("Option " + optionNr + ":");
					//Printing all possible events that fit the given transition
					for (Transition transition : transitions) {
						if (transition.isPositive()) {
							System.out.println("\tEvent: " + propositionData.propositionToActivityString(transition.getPositiveLabel()));
						} else if (transition.isNegative()) {
							System.out.print("\tAny event except: ");
							Iterator<String> it = transition.getNegativeLabels().iterator();
							while (it.hasNext()) {
								System.out.print(propositionData.propositionToActivityString(it.next()));
								if (it.hasNext()) {
									System.out.print(" , ");
								}
							}
							System.out.println();
						} else {
							System.out.println("\t-any evet-");
						}
					}
				}
				System.out.println("Stopping cost: " + costCurrMap.get(globalState));
				
				System.out.println();
				System.out.println();
			}

			System.out.println("Final states at trace end");
			State globalState = globalAutomaton.currentState().get(0);
			for (ExecutableAutomaton executableAutomaton : constraintAutomata.keySet()) {
				if (globalAutomatonColours.get(globalState).get(executableAutomaton).equals(MonitoringState.POSS_SAT)) {
					truthValues.put(executableAutomaton, MonitoringState.SAT);
				} else if(globalAutomatonColours.get(globalState).get(executableAutomaton).equals(MonitoringState.POSS_VIOL)) {
					truthValues.put(executableAutomaton, MonitoringState.VIOL);
				}
				System.out.println("Constraint: " + constraintAutomata.get(executableAutomaton).getConstraintString());
				System.out.println("\t Truth value: " + truthValues.get(executableAutomaton));
			}
			for (ExecutableAutomaton executableAutomaton : ltlModelAutomata.keySet()) {
				if (globalAutomatonColours.get(globalState).get(executableAutomaton).equals(MonitoringState.POSS_SAT)) {
					truthValues.put(executableAutomaton, MonitoringState.SAT);
				} else if(globalAutomatonColours.get(globalState).get(executableAutomaton).equals(MonitoringState.POSS_VIOL)) {
					truthValues.put(executableAutomaton, MonitoringState.VIOL);
				}
				System.out.println("Model: " + ltlModelAutomata.get(executableAutomaton).getName());
				System.out.println("\t Truth value: " + truthValues.get(executableAutomaton));
			}

			if (globalTruthValue.equals(MonitoringState.POSS_SAT)) {
				globalTruthValue = MonitoringState.SAT;
			} else if (globalTruthValue.equals(MonitoringState.POSS_VIOL)) {
				globalTruthValue = MonitoringState.VIOL;
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
