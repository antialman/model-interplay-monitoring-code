package model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.processmining.datapetrinets.DataPetriNetsWithMarkings;
import org.processmining.ltl2automaton.plugins.automaton.DefaultAutomatonFactory;
import org.processmining.ltl2automaton.plugins.automaton.State;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.graphbased.directed.petrinetwithdata.newImpl.DataElement;
import org.processmining.models.graphbased.directed.petrinetwithdata.newImpl.PNWDTransition;
import org.processmining.models.semantics.IllegalTransitionException;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.models.semantics.petrinet.PetrinetSemantics;
import org.processmining.models.semantics.petrinet.impl.PetrinetSemanticsFactory;
import org.processmining.plugins.declareminer.ExecutableAutomaton;

import proposition.PropositionData;
import proposition.attribute.AttributeType;

public class DpnModel extends AbstractModel {

	private DataPetriNetsWithMarkings dataPetriNet;

	public DpnModel(String modelName, int violationCost, Set<String> activityNames, Map<String, AttributeType> attributeTypeMap, DataPetriNetsWithMarkings dataPetriNet) {
		super(modelName, ModelType.DPN, violationCost, activityNames, attributeTypeMap);

		this.dataPetriNet = dataPetriNet;
	}

	public DataPetriNetsWithMarkings getDataPetriNet() {
		return dataPetriNet;
	}


	@Override
	public void initializeAutomaton(PropositionData propositionData) {
		//Allows to execute transitions and manipulate the state of the underlying Petri Net
		PetrinetSemantics petrinetSemantics = PetrinetSemanticsFactory.regularPetrinetSemantics(Petrinet.class); //The class argument is actually unused
		petrinetSemantics.initialize(dataPetriNet.getTransitions(), dataPetriNet.getInitialMarking());

		//Data structures and factory for automata creation
		DefaultAutomatonFactory automatonFactory = new DefaultAutomatonFactory();
		List<DpnState> visitedDpnStates = new ArrayList<DpnState>();
		Map<Marking, List<State>> dpnMarkingToAutomatonStates = new HashMap<Marking, List<State>>(); //For setting the accepting states of the automaton
		Stack<DpnState> currentDpnStatePath = new Stack<DpnState>();

		//Processing the initial state
		Marking initialDpnMarking = petrinetSemantics.getCurrentState();
		DpnState initialDpnState = new DpnState(initialDpnMarking, generateInitialWrittenPropositions(dataPetriNet, propositionData));
		processVisitedState(initialDpnState, automatonFactory, visitedDpnStates, dpnMarkingToAutomatonStates, currentDpnStatePath);
		automatonFactory.initialState(initialDpnState.getAutomatonState());

		//Petri Net traversal and automata construction
		visitNextState(petrinetSemantics, currentDpnStatePath, automatonFactory, visitedDpnStates, dpnMarkingToAutomatonStates, propositionData);

		//Setting the accepting states of the automaton
		for (Marking marking : dataPetriNet.getFinalMarkings()) {
			for (State state : dpnMarkingToAutomatonStates.get(marking)) {
				automatonFactory.updateState(state, state.getId(), true);
				//automatonFactory.addAllTransition(state, state); //This would mean that DPN is considered permanently satisfied once the final marking has been reached
			}
		}

		//Creating fail state and self loops
		State failState = new State(automatonFactory.getAutomaton().getStateCount());
		automatonFactory.addState(failState);
		automatonFactory.addAllTransition(failState, failState);
		Set<String> modelPropositions = new HashSet<String>();
		for (String activityName : getActivityNames()) {
			modelPropositions.addAll(propositionData.getAllActivityPropositions(activityName));
		}
		for (DpnState dpnState : visitedDpnStates) {
			//Adding a transition from current state to fail state for each proposition that is unhandled by existing outgoing transitions 
			Set<String> outgoingPropositions = new HashSet<String>();
			for (org.processmining.ltl2automaton.plugins.automaton.Transition transition : dpnState.getAutomatonState().getOutput()) {
				outgoingPropositions.add(transition.getPositiveLabel());
			}
			Set<String> unhandeledPropositions = new HashSet<String>();
			unhandeledPropositions.addAll(modelPropositions);
			unhandeledPropositions.removeAll(outgoingPropositions);
			for (String unhandeledProposition : unhandeledPropositions) {
				automatonFactory.addPropositionTransition(dpnState.getAutomatonState(), failState, unhandeledProposition);
			}
			
			//Adding a self-loop to each state of the automaton (matches all propositions that are not part of the alphabet of this DPN)
			automatonFactory.addNegativePropositionsTransition(dpnState.getAutomatonState(), dpnState.getAutomatonState(), modelPropositions);
		}

		//Setting the automaton state ids and creating the executable automaton
		automaton = automatonFactory.getAutomaton().op.determinize().op.complete().op.renumber().op.minimize();
		executableAutomaton = new ExecutableAutomaton(automaton);
		System.out.println("\tAutomata created for model: " + getModelName());
	}

	private void visitNextState(PetrinetSemantics petrinetSemantics, Stack<DpnState> currentDpnStatePath, DefaultAutomatonFactory automatonFactory, List<DpnState> visitedDpnStates, Map<Marking, List<State>> dpnMarkingToAutomatonStates, PropositionData propositionData) {
		DpnState currentDpnState = currentDpnStatePath.peek();
		
		for (Transition transition : petrinetSemantics.getExecutableTransitions()) {
			if (transition.isInvisible()) { //TODO: Silent transitions
				continue;
			}
			
			Set<String> allActivityPropositions = null;
			PNWDTransition pnwdTransition = (PNWDTransition) transition;
			
			if (!pnwdTransition.hasGuardExpression()) { //Transition without guards
				allActivityPropositions = propositionData.getAllActivityPropositions(transition.getLabel());
				//Fire the transition and, process the resulting marking and visit the next state
				try {
					petrinetSemantics.setCurrentState(currentDpnState.getDpnMarking());
					petrinetSemantics.executeExecutableTransition(transition);
					DpnState newDpnState = new DpnState(petrinetSemantics.getCurrentState(), currentDpnState.getWrittenPropositions());
					
					if (!visitedDpnStates.contains(newDpnState)) {
						processVisitedState(newDpnState, automatonFactory, visitedDpnStates, dpnMarkingToAutomatonStates, currentDpnStatePath);
						for (String activityProposition : allActivityPropositions) {
							automatonFactory.addPropositionTransition(currentDpnState.getAutomatonState(), newDpnState.getAutomatonState(), activityProposition);
						}
						visitNextState(petrinetSemantics, currentDpnStatePath, automatonFactory, visitedDpnStates, dpnMarkingToAutomatonStates, propositionData);
						
					} else {
						newDpnState = visitedDpnStates.get(visitedDpnStates.indexOf(newDpnState));
						for (String activityProposition : allActivityPropositions) {
							automatonFactory.addPropositionTransition(currentDpnState.getAutomatonState(), newDpnState.getAutomatonState(), activityProposition);
						}
					}
					
				} catch (IllegalTransitionException e) {
					//This should never happen because the loop is over executable transitions
					System.err.println(e.getMessage());
				}
			} else if (!pnwdTransition.getWriteOperations().isEmpty()) { //Transition at least one write operation
				String dataCondition = formatGuardExpression(pnwdTransition.getGuardAsString());
				Set<String> writeVariables = new HashSet<String>();
				for (DataElement writeOperation : pnwdTransition.getWriteOperations()) {
					writeVariables.add(writeOperation.getVarName());
				}
				
				String[] orOperatorSplit = dataCondition.split(" or "); //TODO: Should add code for handling parenthesis
				Set<Map<String, String>> orResultWrites = new HashSet<Map<String, String>>();
				
				for (int i = 0; i < orOperatorSplit.length; i++) {
					Map<String, Set<String>> andResultWrites = new HashMap<String, Set<String>>();
					
					String[] andOperatorSplit = orOperatorSplit[i].split(" and "); //TODO: Should add code for handling parenthesis
					for (int j = 0; j < andOperatorSplit.length; j++) {
						String[] splitCondition = andOperatorSplit[j].split("<=|!=|>=|<|=|>| is not | is | not in | in ");
						String attributeName = splitCondition[0];
						Set<String> attributePropositions = propositionData.getMatchingAttributePropositions(attributeName, andOperatorSplit[j]);
						
						if (writeVariables.contains(attributeName)) { //Write condition
							if (andResultWrites.containsKey(attributeName)) {
								andResultWrites.get(attributeName).retainAll(attributePropositions);
							} else {
								andResultWrites.put(attributeName, new HashSet<String>());
								andResultWrites.get(attributeName).addAll(attributePropositions);
							}
						} else { //Read condition
							if (attributePropositions.contains(currentDpnState.getWrittenPropositions().get(attributeName))) {
								//If a read condition does not match the currently written proposition then this orOperatorSplit can not be satisfied on the current traversal path
								andResultWrites.clear();
								break;
							}
						}
					}
					
					
					Set<Map<String, String>> possibleWritePropositions = new HashSet<Map<String,String>>();
					List<String> attributeNames = new ArrayList<String>(andResultWrites.keySet());
					
					for (String attributeValue : andResultWrites.get(attributeNames.get(0))) {
						Map<String, String> possibleWriteProposition = new HashMap<String, String>();
						possibleWriteProposition.put(attributeNames.get(0), attributeValue);
						possibleWritePropositions.add(possibleWriteProposition);
					}
					
					for (int j = 1; j < attributeNames.size(); j++) {
						Set<Map<String, String>> possibleWritePropositionsNew = new HashSet<Map<String,String>>();
						
						for (String attributeValue : andResultWrites.get(attributeNames.get(j))) {
							for (Map<String, String> possibleWriteProposition : possibleWritePropositions) {
								Map<String, String> possibleWritePropositionNew = new HashMap<String, String>();
								possibleWritePropositionNew.put(attributeNames.get(j), attributeValue);
								possibleWriteProposition.forEach((key, value) -> {
									possibleWritePropositionNew.put(key, value);
								});
								possibleWritePropositionsNew.add(possibleWritePropositionNew);
							}
						}
						possibleWritePropositions=possibleWritePropositionsNew;
					}
					orResultWrites.addAll(possibleWritePropositions);
				}
				
				if (!orResultWrites.isEmpty()) { //Empty if the guard can not be satisfied on the current traversal path
					for (Map<String, String> orResultWrite : orResultWrites) {
						allActivityPropositions = propositionData.getAllActivityPropositions(transition.getLabel());
						Set<String> validActivityPropositions = new HashSet<String>();
						for (String activityProposition : allActivityPropositions) {
							boolean isValidForData = true;
							for (String attributeProposition : orResultWrite.values()) {
								if (!activityProposition.contains(attributeProposition)) {
									isValidForData = false;
									break;
								}
							}
							if (isValidForData) {
								validActivityPropositions.add(activityProposition);
							}
						}
						
						Map<String, String> writtenPropositions = new HashMap<String, String>();
						for (DataElement dataElement : dataPetriNet.getVariables()) {
							if (orResultWrite.containsKey(dataElement.getVarName())) {
								writtenPropositions.put(dataElement.getVarName(), orResultWrite.get(dataElement.getVarName()));
							} else {
								writtenPropositions.put(dataElement.getVarName(), currentDpnState.getWrittenPropositions().get(dataElement.getVarName()));
							}
						}
						
						//Fire the transition and, process the resulting marking and visit the next state
						try {
							petrinetSemantics.setCurrentState(currentDpnState.getDpnMarking());
							petrinetSemantics.executeExecutableTransition(transition);
							DpnState newDpnState = new DpnState(petrinetSemantics.getCurrentState(), writtenPropositions);
							
							if (!visitedDpnStates.contains(newDpnState)) {
								processVisitedState(newDpnState, automatonFactory, visitedDpnStates, dpnMarkingToAutomatonStates, currentDpnStatePath);
								for (String activityProposition : validActivityPropositions) {
									automatonFactory.addPropositionTransition(currentDpnState.getAutomatonState(), newDpnState.getAutomatonState(), activityProposition);
								}
								visitNextState(petrinetSemantics, currentDpnStatePath, automatonFactory, visitedDpnStates, dpnMarkingToAutomatonStates, propositionData);
								
							} else {
								newDpnState = visitedDpnStates.get(visitedDpnStates.indexOf(newDpnState));
								for (String activityProposition : validActivityPropositions) {
									automatonFactory.addPropositionTransition(currentDpnState.getAutomatonState(), newDpnState.getAutomatonState(), activityProposition);
								}
							}
							
						} catch (IllegalTransitionException e) {
							//This should never happen because the loop is over executable transitions
							System.err.println(e.getMessage());
						}
						
					}
				}
				
			} else { //Transition with only read guards
				String dataCondition = formatGuardExpression(pnwdTransition.getGuardAsString());
				//Automata arcs are created for all propositions because read conditions do not care about event payloads
				allActivityPropositions = propositionData.getAllActivityPropositions(transition.getLabel());
				//However, an arc can be created only if the last written propositions match the read condition
				Set<String> matchingActivityPropositions = propositionData.getMatchingActivityPropositions(transition.getLabel(), dataCondition);
				
				boolean isValidForData = true;
				for (String attributeName : currentDpnState.getWrittenPropositions().keySet()) {
					String attributeId = propositionData.getAttributeId(attributeName);
					boolean isValidForAttribute = false;
					for (String activityProposition : matchingActivityPropositions) {
						if ((!activityProposition.contains(attributeId)) || (activityProposition.contains(attributeId) && activityProposition.contains(currentDpnState.getWrittenPropositions().get(attributeName)))) {
							isValidForAttribute = true;
							break;
						}
					}
					if (!isValidForAttribute) {
						isValidForData = false;
						break;
					}
				}
				
				
//				for (String activityProposition : matchingActivityPropositions) {
//					
//					boolean isValidForAttribute = true;
//					for (String attributeName : currentDpnState.getWrittenPropositions().keySet()) {
//						String attributeId = propositionData.getAttributeId(attributeName);
//						if (activityProposition.contains(attributeId) && !activityProposition.contains(currentDpnState.getWrittenPropositions().get(attributeName))) {
//							isValidForAttribute = false;
//							break;
//						}
//					}
//				}
				if (isValidForData) {
					try {
						petrinetSemantics.setCurrentState(currentDpnState.getDpnMarking());
						petrinetSemantics.executeExecutableTransition(transition);
						DpnState newDpnState = new DpnState(petrinetSemantics.getCurrentState(), currentDpnState.getWrittenPropositions());
						
						if (!visitedDpnStates.contains(newDpnState)) {
							processVisitedState(newDpnState, automatonFactory, visitedDpnStates, dpnMarkingToAutomatonStates, currentDpnStatePath);
							for (String activityProposition : allActivityPropositions) {
								automatonFactory.addPropositionTransition(currentDpnState.getAutomatonState(), newDpnState.getAutomatonState(), activityProposition);
							}
							visitNextState(petrinetSemantics, currentDpnStatePath, automatonFactory, visitedDpnStates, dpnMarkingToAutomatonStates, propositionData);
							
						} else {
							newDpnState = visitedDpnStates.get(visitedDpnStates.indexOf(newDpnState));
							for (String activityProposition : allActivityPropositions) {
								automatonFactory.addPropositionTransition(currentDpnState.getAutomatonState(), newDpnState.getAutomatonState(), activityProposition);
							}
						}
						
					} catch (IllegalTransitionException e) {
						//This should never happen because the loop is over executable transitions
						System.err.println(e.getMessage());
					}
				}
			}
			
		}

		//Backtracking
		currentDpnStatePath.pop();
	}

	//Old implementation that traverses the underlying petri net without considering guard expressions (keeping it just in case)
	//	private void visitNextState_old(PetrinetSemantics petrinetSemantics, PropositionData propositionData, Map<Marking, State> dpnStateToAutomatonState, Stack<Marking> currentDpnStatePath, DefaultAutomatonFactory automatonFactory) {
	//		for (Transition transition : petrinetSemantics.getExecutableTransitions()) {
	//			if (transition.isInvisible()) { //TODO: Silent transitions
	//				continue;
	//			}
	//			try {
	//				Marking currentDpnState = petrinetSemantics.getCurrentState(); //TODO: Handle writes
	//				currentDpnStatePath.add(currentDpnState);
	//				petrinetSemantics.executeExecutableTransition(transition);
	//				Marking newDpnState = petrinetSemantics.getCurrentState();
	//
	//				if (!dpnStateToAutomatonState.containsKey(newDpnState)) {
	//					//Creating a new automaton state if a new Petri Net state is reached
	//					State newAutomatonState = new State();
	//					automatonFactory.addState(newAutomatonState);
	//					dpnStateToAutomatonState.put(newDpnState, newAutomatonState);
	//					visitNextState_old(petrinetSemantics, propositionData, dpnStateToAutomatonState, currentDpnStatePath, automatonFactory);
	//				}
	//
	//				//Creating an arc in the automaton according to the fired transition
	//				Set<String> activityPropositions;
	//				PNWDTransition pnwdTransition = (PNWDTransition) transition;
	//				if (pnwdTransition.hasGuardExpression()) {
	//					String guardExpression = formatGuardExpression(pnwdTransition.getGuardAsString());
	//					activityPropositions = propositionData.getMatchingActivityPropositions(transition.getLabel(), guardExpression);
	//				} else {
	//					activityPropositions = propositionData.getAllActivityPropositions(transition.getLabel());
	//				}
	//				for (String activityProposition : activityPropositions) {
	//					automatonFactory.addPropositionTransition(dpnStateToAutomatonState.get(currentDpnState), dpnStateToAutomatonState.get(newDpnState), activityProposition);
	//				}
	//
	//			} catch (IllegalTransitionException e) {
	//				//This method should never be called with a transition that can not be fired
	//				System.err.println(e.getMessage());
	//			}
	//
	//			//Backtracking
	//			petrinetSemantics.setCurrentState(currentDpnStatePath.pop());
	//		}
	//	}

	private void processVisitedState(DpnState newDpnState, DefaultAutomatonFactory automatonFactory, List<DpnState> visitedDpnStates, Map<Marking, List<State>> dpnMarkingToAutomatonStates, Stack<DpnState> currentDpnStatePath) {
		State automatonState = new State(automatonFactory.getAutomaton().getStateCount()); //State class refers to the automaton state
		automatonFactory.addState(automatonState);
		newDpnState.setAutomatonState(automatonState);
		visitedDpnStates.add(newDpnState);
		currentDpnStatePath.add(newDpnState);
		if (!dpnMarkingToAutomatonStates.containsKey(newDpnState.getDpnMarking())) {
			dpnMarkingToAutomatonStates.put(newDpnState.getDpnMarking(), new ArrayList<State>());
		}
		dpnMarkingToAutomatonStates.get(newDpnState.getDpnMarking()).add(automatonState);
	}

	private String formatGuardExpression(String guardExpression) {
		String dataCondition = guardExpression.replaceAll("\\(", "").replaceAll("\\)", ""); //TODO: Should add code for handling parenthesis
		dataCondition = dataCondition.replaceAll("==\"", " is ").replaceAll("!=\"", " is not ").replaceAll("\"", ""); //String constants
		dataCondition = dataCondition.replaceAll("&&", " and ").replaceAll("\\|\\|", " or ");
		return dataCondition;
	}

	private Map<String, String> generateInitialWrittenPropositions(DataPetriNetsWithMarkings dataPetriNet, PropositionData propositionData) {
		//Currently assuming that all DPN variables start out as unassigned
		Map<String, String> writtenPropositions = new HashMap<String, String>();
		for (DataElement dataElement : dataPetriNet.getVariables()) {
			writtenPropositions.put(dataElement.getVarName(), propositionData.getAttributeId(dataElement.getVarName()) + "px");
		}
		return writtenPropositions;
	}

	public class DpnState { //Used only for traversing the DPN
		private Marking dpnMarking;
		private Map<String, String> writtenPropositions;
		private State automatonState;

		public DpnState(Marking dpnMarking, Map<String, String> writtenPropositions) {
			this.dpnMarking = dpnMarking;
			this.writtenPropositions = writtenPropositions;
		}

		public Marking getDpnMarking() {
			return dpnMarking;
		}

		public Map<String, String> getWrittenPropositions() {
			return writtenPropositions;
		}

		public void setAutomatonState(State automatonState) {
			this.automatonState = automatonState;
		}
		public State getAutomatonState() {
			return automatonState;
		}

		@Override
		public String toString() {
			return "DpnState [marking=" + dpnMarking + ", writtenPropositions=" + writtenPropositions + "]";
		}
		
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			DpnState other = (DpnState) obj;
			
			if (this.dpnMarking.equals(other.getDpnMarking()) && this.writtenPropositions.equals(other.getWrittenPropositions())) {
				return true;
			} else {
				return false;
			}
		}
	}
}
