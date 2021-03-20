package utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.processmining.ltl2automaton.plugins.automaton.Automaton;
import org.processmining.ltl2automaton.plugins.automaton.State;
import org.processmining.ltl2automaton.plugins.automaton.Transition;
import org.processmining.ltl2automaton.plugins.formula.DefaultParser;
import org.processmining.ltl2automaton.plugins.formula.Formula;
import org.processmining.ltl2automaton.plugins.formula.conjunction.ConjunctionFactory;
import org.processmining.ltl2automaton.plugins.formula.conjunction.ConjunctionTreeLeaf;
import org.processmining.ltl2automaton.plugins.formula.conjunction.ConjunctionTreeNode;
import org.processmining.ltl2automaton.plugins.formula.conjunction.DefaultTreeFactory;
import org.processmining.ltl2automaton.plugins.formula.conjunction.GroupedTreeConjunction;
import org.processmining.ltl2automaton.plugins.formula.conjunction.TreeFactory;
import org.processmining.ltl2automaton.plugins.ltl.SyntaxParserException;
import org.processmining.plugins.declareminer.ExecutableAutomaton;
import org.processmining.plugins.declareminer.PossibleNodes;

import data.DeclareConstraint;
import utils.enums.ConstraintState;

public class AutomatonUtils {
	
	private static TreeFactory<ConjunctionTreeNode, ConjunctionTreeLeaf> treeFactory = DefaultTreeFactory.getInstance();
	private static ConjunctionFactory<? extends GroupedTreeConjunction> conjunctionFactory = GroupedTreeConjunction.getFactory(treeFactory);
	
	private AutomatonUtils() {
		//Private constructor to avoid unnecessary instantiation of the class
	}
	
	public static Map<ExecutableAutomaton, DeclareConstraint> createConstraintAutomata(Map<DeclareConstraint, String> ltlFormulaMap) {
		Map<ExecutableAutomaton, DeclareConstraint> constraintAutomatons = new HashMap<ExecutableAutomaton, DeclareConstraint>();
		for (DeclareConstraint declareConstraint : ltlFormulaMap.keySet()) {
			
			Automaton aut = createAutomatonForLTLFormula(ltlFormulaMap.get(declareConstraint));
			constraintAutomatons.put(new ExecutableAutomaton(aut), declareConstraint);
		}
		return constraintAutomatons;
	}
	
	public static ExecutableAutomaton createGlobalAutomaton(Map<DeclareConstraint, String> ltlFormulaMap) {
		Automaton globalAutomaton = null;
		
		for (String ltlFormula : ltlFormulaMap.values()) {
			//Creating new automata just in case because op.intersect modifies the automata being intersected 
			Automaton individualAutomaton = createAutomatonForLTLFormula(ltlFormula);
			if (globalAutomaton==null && individualAutomaton != null) {
				globalAutomaton = individualAutomaton;
			} else if (individualAutomaton != null) {
				globalAutomaton = globalAutomaton.op.intersect(individualAutomaton);
			}			
		}
		if (globalAutomaton != null) {
			globalAutomaton = globalAutomaton.op.determinize().op.complete();
			return new ExecutableAutomaton(globalAutomaton);
		} else {
			return null;
		}
	}
	
	private static Automaton createAutomatonForLTLFormula(String ltlFormula) {
		Automaton aut = null;
		
		//Parsing the ltl formula
		try {
			Formula parsedFormula = new DefaultParser(ltlFormula).parse();
			System.out.println("Parsed formula: " + parsedFormula);
			GroupedTreeConjunction conjunction = conjunctionFactory.instance(parsedFormula);
			aut = conjunction.getAutomaton();
		} catch (SyntaxParserException e) {
			System.out.println("Unable to parse formula: " + ltlFormula);
			e.printStackTrace();
		}
		System.out.println("");
		
		return aut;
	}
	
	

	public static ConstraintState execPropositionOnAutomaton(String eventProposition, ExecutableAutomaton executableAutomaton) {
		//Each event must be executed at least on the global automata during event log replay
		PossibleNodes currentState = executableAutomaton.next(eventProposition);
		return checkTruthValue(currentState);
	}
	
	private static ConstraintState checkTruthValue(PossibleNodes currentState) {
		ConstraintState newTruthValue;
		
		if (currentState.isAccepting()) {
			newTruthValue = ConstraintState.POSS_SAT;
			for (State state : currentState) {
				for (Transition t : state.getOutput()) {
					if (t.isAll()) {
						newTruthValue = ConstraintState.SAT;
					}
				}
			}
		} else {
			newTruthValue = ConstraintState.POSS_VIOL;
			for (State state : currentState) {
				if (!currentState.acceptingReachable(state)) {
					newTruthValue = ConstraintState.VIOL;
				}
			}
		}
		
		return newTruthValue;
	}

	public static Map<State, Map<ExecutableAutomaton, ConstraintState>> getGlobalAutomatonColours(ExecutableAutomaton globalAutomaton, Map<ExecutableAutomaton, DeclareConstraint> constraintAutomata) {
		Map<State, Map<ExecutableAutomaton, ConstraintState>> globalAutomatonColours = new HashMap<State, Map<ExecutableAutomaton, ConstraintState>>();
		boolean visited[] = new boolean[globalAutomaton.stateCount()];
		
		//Just to make sure the initial states are correct
		for (ExecutableAutomaton executableAutomaton : constraintAutomata.keySet()) {
			executableAutomaton.ini();
		}
		globalAutomaton.ini();
		
		for (State state : globalAutomaton.currentState()) {
			Stack<String> executedPropositions = new Stack<String>();
			colourGlobalAutomatonState(state, visited, constraintAutomata, globalAutomatonColours, executedPropositions);
		}
		
		return globalAutomatonColours;
	}

	private static void colourGlobalAutomatonState(State state, boolean[] visited, Map<ExecutableAutomaton, DeclareConstraint> constraintAutomata, Map<State, Map<ExecutableAutomaton, ConstraintState>> globalAutomatonColours, Stack<String> executedPropositions) {
		visited[state.getId()] = true;
		System.out.println("Global state colours " + state.toString());
		HashMap<ExecutableAutomaton, ConstraintState> globalStateColours = new HashMap<ExecutableAutomaton, ConstraintState>();
		
		for (ExecutableAutomaton executableAutomaton : constraintAutomata.keySet()) {
			if (!executedPropositions.empty()) {
				executableAutomaton.ini();
				for (String executedProposition : executedPropositions) {
					executableAutomaton.next(executedProposition);
				}
			}
			ConstraintState truthValue = checkTruthValue(executableAutomaton.currentState());
			globalStateColours.put(executableAutomaton, truthValue);
			System.out.println("\t" + truthValue + ": " + constraintAutomata.get(executableAutomaton).getConstraintString());
		}
		globalAutomatonColours.put(state, globalStateColours);
		
		
		for (Transition t : state.getOutput()) {
			if (!visited[t.getTarget().getId()]) {
				executedPropositions.push(t.getPositiveLabel()); //Returns a label even if the transition is negative which is useful in this case
				colourGlobalAutomatonState(t.getTarget(), visited, constraintAutomata, globalAutomatonColours, executedPropositions);
				executedPropositions.pop();
			}
		}
	}

	public static Map<State, Integer> getCostCurrMap(ExecutableAutomaton globalAutomaton, Map<State, Map<ExecutableAutomaton, ConstraintState>> globalAutomatonColours, Map<ExecutableAutomaton, DeclareConstraint> constraintAutomata) {
		Map<State, Integer> costCurrMap = new HashMap<State, Integer>();
		boolean visited[] = new boolean[globalAutomaton.stateCount()];
		
		//Just to make sure the initial state is correct
		globalAutomaton.ini();
		
		for (State state : globalAutomaton.currentState()) {
			calculateCostCurrForState(state, visited, costCurrMap, globalAutomaton, globalAutomatonColours, constraintAutomata);
		}
		
		return costCurrMap;
	}

	private static void calculateCostCurrForState(State state, boolean[] visited, Map<State, Integer> costCurrMap, ExecutableAutomaton globalAutomaton, Map<State, Map<ExecutableAutomaton, ConstraintState>> globalAutomatonColours, Map<ExecutableAutomaton, DeclareConstraint> constraintAutomata) {
		visited[state.getId()] = true;
		int costCurr = 0;
		
		for (ExecutableAutomaton executableAutomaton : globalAutomatonColours.get(state).keySet()) {
			ConstraintState constraintState = globalAutomatonColours.get(state).get(executableAutomaton);
			if (constraintState == ConstraintState.VIOL || constraintState == ConstraintState.POSS_VIOL) {
				costCurr = costCurr + constraintAutomata.get(executableAutomaton).getViolationCost();
			}
		}
		System.out.println("cost_curr for state " + state.toString() + ": " + costCurr);
		costCurrMap.put(state, Integer.valueOf(costCurr));
		
		for (Transition t : state.getOutput()) {
			if (!visited[t.getTarget().getId()]) {
				calculateCostCurrForState(t.getTarget(), visited, costCurrMap, globalAutomaton, globalAutomatonColours, constraintAutomata);
			}
		}
		
	}
}