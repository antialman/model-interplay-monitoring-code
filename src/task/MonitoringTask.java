package task;

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

import javafx.concurrent.Task;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
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
	
	Map<AbstractModel, MonitoringState> truthValues; //Truth value of each model
	MonitoringState globalTruthValue; //Global truth value
	
	public MonitoringTask(XTrace xtrace, List<AbstractModel> processModels, PropositionData propositionData, ExecutableAutomaton globalAutomaton, Map<org.processmining.ltl2automaton.plugins.automaton.State, Map<AbstractModel, MonitoringState>> globalAutomatonColours, Map<org.processmining.ltl2automaton.plugins.automaton.State, Integer> costCurrMap, Map<org.processmining.ltl2automaton.plugins.automaton.State, Integer> costBestMap) {
		super();
		this.xtrace = xtrace;
		this.processModels = processModels;
		this.propositionData = propositionData;
		this.globalAutomaton = globalAutomaton;
		this.globalAutomatonColours = globalAutomatonColours;
		this.costCurrMap = costCurrMap;
		this.costBestMap = costBestMap;
		
		truthValues = new HashMap<AbstractModel, MonitoringState>(processModels.size());
	}

	@Override
	protected VBox call() throws Exception {
		VBox resultsVbox = new VBox();
		
		String traceName = XConceptExtension.instance().extractName(xtrace);
		processResultString("\n\n\n", resultsVbox);
		processResultString("===========================================", resultsVbox);
		processResultString("Replaying trace: " + traceName, resultsVbox);
		processResultString("===========================================", resultsVbox);
		processResultString("\n", resultsVbox);

		//Initialising the automata at the start of each trace
		for (AbstractModel processModel : processModels) {
			processModel.getExecutableAutomaton().ini();
			truthValues.put(processModel, MonitoringState.INIT);
		}
		globalAutomaton.ini();
		globalTruthValue = MonitoringState.INIT;

		for (XEvent xevent : xtrace) {
			String eventName = XConceptExtension.instance().extractName(xevent);
			processResultString("Next event in event log: " + eventName, resultsVbox);
			processResultString("-------------------------------------------", resultsVbox);

			String eventProposition = LogUtils.getEventProposition(xevent, propositionData);

			globalTruthValue = AutomatonUtils.execPropositionOnAutomaton(eventProposition, globalAutomaton);
			org.processmining.ltl2automaton.plugins.automaton.State globalState = globalAutomaton.currentState().get(0);
			processResultString("Reached state: " + globalState, resultsVbox);
			processResultString("Global truth value: " + globalTruthValue, resultsVbox);

			//Using individual automata to double-check global automata correctness (functionally not needed)
			for (AbstractModel processModel : processModels) {
				ExecutableAutomaton executableAutomaton = processModel.getExecutableAutomaton();
				MonitoringState newTruthValue = AutomatonUtils.execPropositionOnAutomaton(eventProposition, executableAutomaton);
				truthValues.put(processModel, newTruthValue);				
				processResultString("\tModel " + processModel.getModelName() + ": " + globalAutomatonColours.get(globalState).get(processModel), resultsVbox);
				//processResultString("\tTruth value: " + truthValues.get(processModel));
				//processResultString("\tGlobal colour: " + globalAutomatonColours.get(globalState).get(processModel));
				if (!truthValues.get(processModel).equals(globalAutomatonColours.get(globalState).get(processModel))) {
					//If this happens then there must be a mistake in either creating or colouring the global automaton
					System.err.println("Global colour does not match truth value, something is wrong with the cross-product!");
				}
			}

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
			processResultString("---", resultsVbox);
			processResultString("Best achievable cost from current state: " + bestAchievableCost, resultsVbox);
			processResultString("Number of next states that can lead to the best cost: " + bestNextTransitions.size(), resultsVbox);
			//Considering each transition as a separate option
			for (List<Transition> transitions : bestNextTransitions.values()) {
				//Printing all possible events that fit the given transition
				for (Transition transition : transitions) {
					processResultString("Next state " + transition.getTarget() + " is reached with:", resultsVbox);
					if (transition.isPositive()) {
						processResultString("\tEvent: " + propositionData.propositionToActivityString(transition.getPositiveLabel()), resultsVbox);
					} else if (transition.isNegative()) {
						String anyEventString = "\tAny event except: ";
						Iterator<String> it = transition.getNegativeLabels().iterator();
						while (it.hasNext()) {
							anyEventString = anyEventString + propositionData.propositionToActivityString(it.next());
							if (it.hasNext()) {
								anyEventString = anyEventString + " , ";
							}
						}
						processResultString(anyEventString, resultsVbox);
					} else {
						processResultString("\t-any evet-", resultsVbox);
					}
				}
			}
			processResultString("Stopping cost: " + costCurrMap.get(globalState), resultsVbox);

			processResultString("", resultsVbox);
			processResultString("", resultsVbox);
		}

		
		processResultString("Final states at trace end", resultsVbox);
		org.processmining.ltl2automaton.plugins.automaton.State globalState = globalAutomaton.currentState().get(0);
		processResultString("Global state: " + globalState, resultsVbox);
		if (globalTruthValue.equals(MonitoringState.POSS_SAT)) {
			processResultString("Global truth value: " + MonitoringState.SAT, resultsVbox);
		} else if (globalTruthValue.equals(MonitoringState.POSS_VIOL)) {
			processResultString("Global truth value: " + MonitoringState.VIOL, resultsVbox);
		} else {
			processResultString("Global truth value: " + globalTruthValue, resultsVbox);
		}
		
		for (AbstractModel processModel : processModels) {
			if (globalAutomatonColours.get(globalState).get(processModel).equals(MonitoringState.POSS_SAT)) {
				processResultString("\tModel " + processModel.getModelName() + ": " + MonitoringState.SAT, resultsVbox);
			} else if(globalAutomatonColours.get(globalState).get(processModel).equals(MonitoringState.POSS_VIOL)) {
				processResultString("\tModel " + processModel.getModelName() + ": " + MonitoringState.VIOL, resultsVbox);
			} else {
				processResultString("\tModel " + processModel.getModelName() + ": " + globalAutomatonColours.get(globalState).get(processModel), resultsVbox);
			}
		}
		
		
		return resultsVbox;
	}
	
	private void processResultString(String resultString, VBox resultsVbox) {
		Label resultLabel = new Label(resultString);
		resultsVbox.getChildren().add(resultLabel);
		System.out.println(resultString);
	}

}
