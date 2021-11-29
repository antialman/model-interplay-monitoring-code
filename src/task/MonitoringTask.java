package task;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XTrace;
import org.processmining.ltl2automaton.plugins.automaton.Transition;
import org.processmining.plugins.declareminer.ExecutableAutomaton;

import controller.TraceVisualisationController;
import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.VBox;
import main.MainGui;
import model.AbstractModel;
import proposition.PropositionData;
import utils.AutomatonUtils;
import utils.LogUtils;
import utils.enums.MonitoringState;

public class MonitoringTask extends Task<VBox> {

	private XTrace xtrace;
	private List<AbstractModel> processModels;
	private PropositionData propositionData;
	private ExecutableAutomaton globalAutomaton;
	private Map<org.processmining.ltl2automaton.plugins.automaton.State, Map<AbstractModel, MonitoringState>> globalAutomatonColours;
	private Map<org.processmining.ltl2automaton.plugins.automaton.State, Integer> costCurrMap;
	private Map<org.processmining.ltl2automaton.plugins.automaton.State, Integer> costBestMap;
	private List<Long> eventProcessingTimes;

	private Map<AbstractModel, MonitoringState> truthValues; //Truth value of each model
	private MonitoringState globalTruthValue; //Global truth value
	

	private VBox resultsVbox;
	private TraceVisualisationController traceVisualisationController;

	public MonitoringTask(XTrace xtrace, List<AbstractModel> processModels, PropositionData propositionData, ExecutableAutomaton globalAutomaton, Map<org.processmining.ltl2automaton.plugins.automaton.State, Map<AbstractModel, MonitoringState>> globalAutomatonColours, Map<org.processmining.ltl2automaton.plugins.automaton.State, Integer> costCurrMap, Map<org.processmining.ltl2automaton.plugins.automaton.State, Integer> costBestMap, List<Long> eventProcessingTimes) {
		super();
		this.xtrace = xtrace;
		this.processModels = processModels;
		this.propositionData = propositionData;
		this.globalAutomaton = globalAutomaton;
		this.globalAutomatonColours = globalAutomatonColours;
		this.costCurrMap = costCurrMap;
		this.costBestMap = costBestMap;
		this.eventProcessingTimes=eventProcessingTimes;

		truthValues = new HashMap<AbstractModel, MonitoringState>(processModels.size());
	}

	@Override
	protected VBox call() throws Exception {
		loadTraceVisualisation();
		traceVisualisationController.setModels(processModels);

		String traceName = XConceptExtension.instance().extractName(xtrace);
		writeDebugMessage("\n\n\n");
		writeDebugMessage("===========================================");
		writeDebugMessage("Replaying trace: " + traceName);
		writeDebugMessage("===========================================");
		writeDebugMessage("\n");

		//Initialising the automata at the start of each trace
		for (AbstractModel processModel : processModels) {
			processModel.getExecutableAutomaton().ini();
			truthValues.put(processModel, MonitoringState.INIT);
		}
		globalAutomaton.ini();
		globalTruthValue = MonitoringState.INIT;

		for (XEvent xevent : xtrace) {
			long startTime = System.nanoTime();
			
			String eventName = XConceptExtension.instance().extractName(xevent);
			writeDebugMessage("Next event in event log: " + eventName);
			writeDebugMessage("-------------------------------------------");

			String eventProposition = LogUtils.getEventProposition(xevent, propositionData);

			globalTruthValue = AutomatonUtils.execPropositionOnAutomaton(eventProposition, globalAutomaton, costBestMap);
			org.processmining.ltl2automaton.plugins.automaton.State globalState = globalAutomaton.currentState().get(0);
			writeDebugMessage("Reached state: " + globalState);
			writeDebugMessage("Global truth value: " + globalTruthValue);
			traceVisualisationController.addGlobalState(globalTruthValue);
			
			for (AbstractModel processModel : processModels) {
				traceVisualisationController.addModelState(processModel, globalAutomatonColours.get(globalState).get(processModel));
			}

			////Uncomment to compare global automata based states with individual automata states
			//for (AbstractModel processModel : processModels) {
			//	ExecutableAutomaton executableAutomaton = processModel.getExecutableAutomaton();
			//	MonitoringState newTruthValue = AutomatonUtils.execPropositionOnAutomaton(eventProposition, executableAutomaton, null);
			//	truthValues.put(processModel, newTruthValue);				
			//	writeDebugMessage("\tModel " + processModel.getModelName() + ": " + globalAutomatonColours.get(globalState).get(processModel));
			//	//processResultString("\tTruth value: " + truthValues.get(processModel));
			//	//processResultString("\tGlobal colour: " + globalAutomatonColours.get(globalState).get(processModel));
			//	if (!truthValues.get(processModel).equals(globalAutomatonColours.get(globalState).get(processModel))) {
			//		//If this happens then there must be a mistake in either creating or colouring the global automaton
			//		System.err.println("Global colour does not match truth value, something is wrong with the cross-product!");
			//	}
			//}

			//Getting the transitions that lead to best achievable cost from the current state (excluding self loops)
			Integer bestAchievableCost = costBestMap.get(globalState);
			Map<org.processmining.ltl2automaton.plugins.automaton.State, List<Transition>> bestNextTransitions = new HashMap<org.processmining.ltl2automaton.plugins.automaton.State, List<Transition>>();
			for (Transition t : globalState.getOutput()) {
				if (costBestMap.get(t.getTarget()).intValue() == bestAchievableCost.intValue() && t.getSource() != t.getTarget()) {
					if (!bestNextTransitions.containsKey(t.getTarget())) {
						bestNextTransitions.put(t.getTarget(), new ArrayList<Transition>());
					}
					bestNextTransitions.get(t.getTarget()).add(t);
				}
			}

			//Printing the recommendations for the next course of action
			writeDebugMessage("---");
			writeDebugMessage("Best achievable cost from current state: " + bestAchievableCost);
			traceVisualisationController.addCostBestValue(bestAchievableCost);
			StringBuilder recommendationSb = new StringBuilder("Number of next states that can lead to the best cost: ").append(bestNextTransitions.size()).append("\n\n");
			writeDebugMessage("Number of next states that can lead to the best cost: " + bestNextTransitions.size());
			
			//Considering each transition as a separate option
			Map<String, List<String>> stateToOption = new HashMap<String, List<String>>(); //Gathering all the options per each immediate successor state
			for (List<Transition> transitions : bestNextTransitions.values()) {
				for (Transition transition : transitions) {
					String targetStateString = transition.getTarget().toString();
					if (!stateToOption.containsKey(targetStateString)) {
						stateToOption.put(targetStateString, new ArrayList<String>());
					}
					
					if (transition.isPositive()) {
						stateToOption.get(targetStateString).add("\tEvent: " + propositionData.propositionToActivityString(transition.getPositiveLabel(), false));
					} else if (transition.isNegative()) {
						String anyEventString = "\tAny event except: ";
						Iterator<String> it = transition.getNegativeLabels().iterator();
						while (it.hasNext()) {
							anyEventString = anyEventString + propositionData.propositionToActivityString(it.next(), false);
							if (it.hasNext()) {
								anyEventString = anyEventString + " , ";
							}
						}
						stateToOption.get(targetStateString).add(anyEventString);
					} else {
						stateToOption.get(targetStateString).add("\t-any evet-");
					}
				}
			}
			
			
			for (String targetStateString : stateToOption.keySet()) {
				writeDebugMessage("Next state " + targetStateString + " is reached with:");
				recommendationSb.append(targetStateString).append(" is reached with:").append("\n");
				
				for (String option : stateToOption.get(targetStateString)) {
					writeDebugMessage(option);
					recommendationSb.append(option).append("\n");
				}
			}
			
			
			
			
			
			
//			for (List<Transition> transitions : bestNextTransitions.values()) {
//				//Printing all possible events that fit the given transition
//				for (Transition transition : transitions) {
//					writeDebugMessage("\tNext state " + transition.getTarget() + " is reached with:");
//					recommendationSb.append(transition.getTarget()).append(" is reached with:").append("\n");
//					if (transition.isPositive()) {
//						writeDebugMessage("\tEvent: " + propositionData.propositionToActivityString(transition.getPositiveLabel(), false));
//						recommendationSb.append("\tEvent: ").append(propositionData.propositionToActivityString(transition.getPositiveLabel(), false)).append("\n");
//					} else if (transition.isNegative()) {
//						String anyEventString = "\tAny event except: ";
//						Iterator<String> it = transition.getNegativeLabels().iterator();
//						while (it.hasNext()) {
//							anyEventString = anyEventString + propositionData.propositionToActivityString(it.next(), false);
//							if (it.hasNext()) {
//								anyEventString = anyEventString + " , ";
//							}
//						}
//						writeDebugMessage(anyEventString);
//						recommendationSb.append(anyEventString).append("\n");
//					} else {
//						writeDebugMessage("\t-any evet-");
//						recommendationSb.append("\t-any evet-").append("\n");
//					}
//				}
//			}

			String activityString = propositionData.propositionToActivityString(eventProposition, true);
			traceVisualisationController.addEventLabel(activityString, recommendationSb.toString());
			writeDebugMessage("Stopping cost: " + costCurrMap.get(globalState));
			traceVisualisationController.addCostCurrValue(costCurrMap.get(globalState));
			
			eventProcessingTimes.add(System.nanoTime() - startTime);

			writeDebugMessage("");
			writeDebugMessage("");
		}


		writeDebugMessage("Final states at trace end");
		org.processmining.ltl2automaton.plugins.automaton.State globalState = globalAutomaton.currentState().get(0);
		writeDebugMessage("Global state: " + globalState);
		if (globalTruthValue.equals(MonitoringState.POSS_SAT)) {
			writeDebugMessage("Global truth value: " + MonitoringState.SAT);
			traceVisualisationController.addGlobalState(MonitoringState.SAT);
		} else if (globalTruthValue.equals(MonitoringState.POSS_VIOL)) {
			writeDebugMessage("Global truth value: " + MonitoringState.VIOL);
			traceVisualisationController.addGlobalState(MonitoringState.VIOL);
		} else {
			writeDebugMessage("Global truth value: " + globalTruthValue);
			traceVisualisationController.addGlobalState(globalTruthValue);
		}

		for (AbstractModel processModel : processModels) {
			if (globalAutomatonColours.get(globalState).get(processModel).equals(MonitoringState.POSS_SAT)) {
				writeDebugMessage("\tModel " + processModel.getModelName() + ": " + MonitoringState.SAT);
				traceVisualisationController.addModelState(processModel, MonitoringState.SAT);
			} else if(globalAutomatonColours.get(globalState).get(processModel).equals(MonitoringState.POSS_VIOL)) {
				writeDebugMessage("\tModel " + processModel.getModelName() + ": " + MonitoringState.VIOL);
				traceVisualisationController.addModelState(processModel, MonitoringState.VIOL);
			} else {
				writeDebugMessage("\tModel " + processModel.getModelName() + ": " + globalAutomatonColours.get(globalState).get(processModel));
				traceVisualisationController.addModelState(processModel, globalAutomatonColours.get(globalState).get(processModel));
			}
		}
		traceVisualisationController.addEventLabel("(complete)", null);


		return resultsVbox;
	}

	private void loadTraceVisualisation() throws IOException {
		String fxmlPath = "fxml/TraceVisualisation.fxml";
		FXMLLoader fxmlLoader = new FXMLLoader(MainGui.class.getClassLoader().getResource(fxmlPath));
		resultsVbox = fxmlLoader.load();
		traceVisualisationController = (TraceVisualisationController)fxmlLoader.getController();
	}

	private void writeDebugMessage(String debugMessage) {
		traceVisualisationController.addDebugMessage(debugMessage);
		System.out.println(debugMessage);
	}

}
