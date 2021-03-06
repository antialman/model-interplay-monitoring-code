package main;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.cli.CommandLine;
import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.ltl2automaton.plugins.automaton.State;
import org.processmining.ltl2automaton.plugins.automaton.Transition;
import org.processmining.plugins.declareminer.ExecutableAutomaton;

import model.AbstractModel;
import proposition.PropositionData;
import utils.AutomatonUtils;
import utils.CmdArgsUtil;
import utils.LogUtils;
import utils.ModelUtils;
import utils.enums.MonitoringState;

public class MainCmd {

	public static void main(String[] args) {
		//Data structures
		List<AbstractModel> processModels;
		PropositionData propositionData = new PropositionData();
		ExecutableAutomaton globalAutomaton;
		Map<State, Map<AbstractModel, MonitoringState>> globalAutomatonColours;
		Map<State, Integer> costCurrMap;
		Map<State, Integer> costBestMap;
		
		//statistics
		long monitoringAutomatonTime;
		List<Long> eventProcessingTimes = new ArrayList<Long>();


		System.out.println("Start: Handling parameters");
		CommandLine cmd = CmdArgsUtil.handleArgs(args);
		String costsFilePath = cmd.getOptionValue("costsFile");
		String eventLogPath = cmd.getOptionValue("eventLog");
		String statsFilePath = cmd.getOptionValue("statsFile");
		

		//String costsFilePath = "input/costModel.txt";
		//String eventLogPath = "input/eventlog.xes";
		System.out.println("Done: Handling parameters\n");


		System.out.println("PROCESSING INPUT MODELS");
		System.out.println("======================================================");

		System.out.println("Start: Loading input models");
		processModels = ModelUtils.loadProcessModels(costsFilePath);
		if (processModels == null || processModels.isEmpty()) {
			System.err.println("No models found from the costs file");
			System.exit(-1);
		}
		System.out.println("Done: Loading input models\n");

		long autStartTime = System.nanoTime();
		
		System.out.println("Start: Populating propositionalization data structure");
		for (AbstractModel processModel : processModels) {
			propositionData.addActivities(processModel);
			propositionData.addAttributePropositions(processModel);
		}
		System.out.println("Done: Populating propositionalization data structure\n");


		//Creating a propositionalized automaton of each input model
		System.out.println("Start: Creating a propositionalized automaton of each input model");
		for (AbstractModel processModel : processModels) {
			processModel.initializeAutomaton(propositionData);
		}
		System.out.println("Done: Creating a propositionalized automaton of each input model\n");


		System.out.println("Start: Creating the global automaton");
		globalAutomaton = AutomatonUtils.createGlobalAutomaton(processModels);
		System.out.println("Done: Creating the global automaton\n");

		System.out.println("Start: Calculating colors for each state of the global automaton");
		globalAutomatonColours = AutomatonUtils.getGlobalAutomatonColours(processModels, globalAutomaton);
		System.out.println("Done: Calculating colors for each state of the global automaton\n");

		System.out.println("Start: Calculating cost_curr and cost_best values for each state of the global automaton");
		costCurrMap = AutomatonUtils.getCostCurrMap(processModels, globalAutomaton, globalAutomatonColours);
		costBestMap = AutomatonUtils.getCostBestMap(globalAutomaton, costCurrMap);
		System.out.println("Done: Calculating cost_curr and cost_best values for each state of the global automaton\n");

		monitoringAutomatonTime = System.nanoTime() - autStartTime;

		//Replaying the event log (based on old implementation, cleaned up a bit, but more could probably be done)
		Map<AbstractModel, MonitoringState> truthValues = new HashMap<AbstractModel, MonitoringState>(processModels.size()); //Truth value of each model
		MonitoringState globalTruthValue; //Global truth value
		XLog xlog = LogUtils.convertToXlog(eventLogPath);
		
		for (XTrace xtrace : xlog) {
			
			String traceName = XConceptExtension.instance().extractName(xtrace);
			System.out.println("\n\n\n");
			System.out.println("===========================================");
			System.out.println("Replaying trace: " + traceName);
			System.out.println("===========================================");
			System.out.println("\n");

			//Initialising the automata at the start of each trace
			for (AbstractModel processModel : processModels) {
				processModel.getExecutableAutomaton().ini();
				truthValues.put(processModel, MonitoringState.INIT);
			}
			globalAutomaton.ini();
			globalTruthValue = MonitoringState.INIT;

			for (XEvent xevent : xtrace) {
				long evStartTime = System.nanoTime();
				String eventName = XConceptExtension.instance().extractName(xevent);
				System.out.println("Next event in event log: " + eventName);
				System.out.println("-------------------------------------------");

				String eventProposition = LogUtils.getEventProposition(xevent, propositionData);

				globalTruthValue = AutomatonUtils.execPropositionOnAutomaton(eventProposition, globalAutomaton, costBestMap);
				State globalState = globalAutomaton.currentState().get(0);
				System.out.println("Reached state: " + globalState);
				System.out.println("Global truth value: " + globalTruthValue);

				//Uncomment to compare global automata based states with individual automata states
				//for (AbstractModel processModel : processModels) {
				//	ExecutableAutomaton executableAutomaton = processModel.getExecutableAutomaton();
				//	MonitoringState newTruthValue = AutomatonUtils.execPropositionOnAutomaton(eventProposition, executableAutomaton, null);
				//	truthValues.put(processModel, newTruthValue);				
				//	System.out.println("\tModel " + processModel.getModelName() + ": " + globalAutomatonColours.get(globalState).get(processModel));
				//	//System.out.println("\tTruth value: " + truthValues.get(processModel));
				//	//System.out.println("\tGlobal colour: " + globalAutomatonColours.get(globalState).get(processModel));
				//	if (!truthValues.get(processModel).equals(globalAutomatonColours.get(globalState).get(processModel))) {
				//		//If this happens then there must be a mistake in either creating or colouring the global automaton
				//		System.err.println("Global colour does not match truth value, something is wrong with the cross-product!");
				//	}
				//}

				//Getting the transitions that lead to best achievable cost from the current state (excluding self loops)
				Integer bestAchievableCost = costBestMap.get(globalState);
				Map<State, List<Transition>> bestNextTransitions = new HashMap<State, List<Transition>>();
				for (Transition t : globalState.getOutput()) {
					if (costBestMap.get(t.getTarget()).intValue() == bestAchievableCost.intValue() && t.getSource() != t.getTarget()) {
						if (!bestNextTransitions.containsKey(t.getTarget())) {
							bestNextTransitions.put(t.getTarget(), new ArrayList<Transition>());
						}
						bestNextTransitions.get(t.getTarget()).add(t);
					}
				}

				//Printing the recommendations for the next course of action
				System.out.println("---");
				System.out.println("Best achievable cost from current state: " + bestAchievableCost);
				System.out.println("Number of next states that can lead to the best cost: " + bestNextTransitions.size());
				//Considering each transition as a separate option
				for (List<Transition> transitions : bestNextTransitions.values()) {
					//Printing all possible events that fit the given transition
					for (Transition transition : transitions) {
						System.out.println("Next state " + transition.getTarget() + " is reached with:");
						if (transition.isPositive()) {
							System.out.println("\tEvent: " + propositionData.propositionToActivityString(transition.getPositiveLabel(), false));
						} else if (transition.isNegative()) {
							System.out.print("\tAny event except: ");
							Iterator<String> it = transition.getNegativeLabels().iterator();
							while (it.hasNext()) {
								System.out.print(propositionData.propositionToActivityString(it.next(), false));
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
				
				eventProcessingTimes.add(System.nanoTime() - evStartTime);

				System.out.println();
				System.out.println();
			}

			
			System.out.println("Final states at trace end");
			State globalState = globalAutomaton.currentState().get(0);
			System.out.println("Global state: " + globalState);
			if (globalTruthValue.equals(MonitoringState.POSS_SAT)) {
				System.out.println("Global truth value: " + MonitoringState.SAT);
			} else if (globalTruthValue.equals(MonitoringState.POSS_VIOL)) {
				System.out.println("Global truth value: " + MonitoringState.VIOL);
			} else {
				System.out.println("Global truth value: " + globalTruthValue);
			}
			
			for (AbstractModel processModel : processModels) {
				if (globalAutomatonColours.get(globalState).get(processModel).equals(MonitoringState.POSS_SAT)) {
					System.out.println("\tModel " + processModel.getModelName() + ": " + MonitoringState.SAT);
				} else if(globalAutomatonColours.get(globalState).get(processModel).equals(MonitoringState.POSS_VIOL)) {
					System.out.println("\tModel " + processModel.getModelName() + ": " + MonitoringState.VIOL);
				} else {
					System.out.println("\tModel " + processModel.getModelName() + ": " + globalAutomatonColours.get(globalState).get(processModel));
				}
			}
		}
		
		
		
		System.out.println("\n\n\n");
		System.out.println("===========================================");
		System.out.println("Statistics");
		System.out.println("===========================================");
		double autTime = TimeUnit.MILLISECONDS.convert(monitoringAutomatonTime, TimeUnit.NANOSECONDS)/1000.0;
		System.out.println("Monitoring automaton creation time (s): " + autTime);
		int autStates = globalAutomaton.stateCount();
		System.out.println("Monitoring automaton number of states: " + autStates);
		
		double minEvTime = TimeUnit.MICROSECONDS.convert(Collections.min(eventProcessingTimes), TimeUnit.NANOSECONDS)/1000.0;
		System.out.println("Min event processing time (ms): " + minEvTime);
		double maxEvTime = TimeUnit.MICROSECONDS.convert(Collections.max(eventProcessingTimes), TimeUnit.NANOSECONDS)/1000.0;
		System.out.println("Max event processing time (ms): " + maxEvTime);
		long sum = 0;
		for(int i = 0; i < eventProcessingTimes.size(); i++) {
	        sum += eventProcessingTimes.get(i);
		}
		long avg = sum / eventProcessingTimes.size();
		double avgEvTime = TimeUnit.MICROSECONDS.convert(avg, TimeUnit.NANOSECONDS)/1000.0;
		System.out.println("Avg event processing time (ms): " + avgEvTime);
		
		
		System.out.println();
		System.out.println("For easier copying (Automaton)");
		System.out.println("AutTime\tAutStates");
		System.out.println(autTime + "\t" + autStates);

		System.out.println("For easier copying (Monitoring)");
		System.out.println("EvMin\tEvMax\tEvAvg");
		System.out.println(minEvTime + "\t" + maxEvTime + "\t" + avgEvTime);
		
		System.out.println("For easier copying (full)");
		System.out.println("AutTime\tAutStates\tEvMin\tEvMax\tEvAvg");
		System.out.println(autTime + "\t" + autStates + "\t" + minEvTime + "\t" + maxEvTime + "\t" + avgEvTime);
		
		if (statsFilePath != null) {
			try {
				FileWriter fw = new FileWriter(statsFilePath, true);
				BufferedWriter bw = new BufferedWriter(fw);
			    bw.write(autTime + "\t" + autStates + "\t" + avgEvTime);
			    bw.newLine();
			    bw.close();
			} catch (IOException e) {
				System.err.println("Cant write to statsFile: " + statsFilePath);
				e.printStackTrace();
			}
		}
		
	}
}
