package utils;

import java.util.HashMap;
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

public class AutomatonUtils {
	
	private static Map<ExecutableAutomaton, String> constraintAutomatons = new HashMap<ExecutableAutomaton, String>();
	private static TreeFactory<ConjunctionTreeNode, ConjunctionTreeLeaf> treeFactory = DefaultTreeFactory.getInstance();
	private static ConjunctionFactory<? extends GroupedTreeConjunction> conjunctionFactory = GroupedTreeConjunction.getFactory(treeFactory);
	
	private AutomatonUtils() {
		//Private constructor to avoid unnecessary instantiation of the class
	}
	
	public static Map<ExecutableAutomaton, String> createConstraintAutomata(Map<DeclareConstraint, String> ltlFormulaMap) {
		for (DeclareConstraint declareConstraint : ltlFormulaMap.keySet()) {
			
			Automaton aut = createAutomatonForLTLFormula(ltlFormulaMap.get(declareConstraint));
			constraintAutomatons.put(new ExecutableAutomaton(aut), declareConstraint.getConstraintString());
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
	
	

	public static String execPropositionOnAutomaton(String eventProposition, ExecutableAutomaton executableAutomaton, String constraintString) {
		//Each event must be executed at least on the global automata during event log replay
		PossibleNodes currentState = executableAutomaton.next(eventProposition);
		return checkTruthValue(currentState);
	}
	
	private static String checkTruthValue(PossibleNodes currentState) {
		String newTruthValue;
		
		if (currentState.isAccepting()) {
			newTruthValue = "poss.sat";
			for (State state : currentState) {
				for (Transition t : state.getOutput()) {
					if (t.isAll()) {
						newTruthValue = "sat";
					}
				}
			}
		} else {
			newTruthValue = "poss.viol";
			for (State state : currentState) {
				if (!currentState.acceptingReachable(state)) {
					newTruthValue = "viol";
				}
			}
		}
		
		return newTruthValue;
	}

	public static Map<String, Map<ExecutableAutomaton, String>> getGlobalAutomatonColours(ExecutableAutomaton globalAutomaton, Map<ExecutableAutomaton, String> constraintAutomata) {
		Map<String, Map<ExecutableAutomaton, String>> globalAutomatonColours = new HashMap<String, Map<ExecutableAutomaton,String>>();
		boolean visited[] = new boolean[globalAutomaton.stateCount()];
		
		//Just to make sure the initial states are correct
		for (ExecutableAutomaton executableAutomaton : constraintAutomata.keySet()) {
			executableAutomaton.ini();
		}
		globalAutomaton.ini();
		
		for (State state : globalAutomaton.currentState()) {
			Stack<String> executedPropositions = new Stack<String>();
			processGlobalAutomatonState(state, visited, constraintAutomata, globalAutomatonColours, executedPropositions);
		}
		
		return globalAutomatonColours;
	}

	private static void processGlobalAutomatonState(State state, boolean[] visited, Map<ExecutableAutomaton, String> constraintAutomata, Map<String, Map<ExecutableAutomaton, String>> globalAutomatonColours, Stack<String> executedPropositions) {
		visited[state.getId()] = true;
		System.out.println("Global state colours " + state.toString());
		HashMap<ExecutableAutomaton, String> globalStateColours = new HashMap<ExecutableAutomaton, String>();
		
		for (ExecutableAutomaton executableAutomaton : constraintAutomata.keySet()) {
			if (!executedPropositions.empty()) {
				executableAutomaton.ini();
				for (String executedProposition : executedPropositions) {
					executableAutomaton.next(executedProposition);
				}
			}
			String truthValue = checkTruthValue(executableAutomaton.currentState());
			globalStateColours.put(executableAutomaton, truthValue);
			System.out.println("\t" + truthValue + ": " + constraintAutomata.get(executableAutomaton));
		}
		globalAutomatonColours.put(state.toString(), globalStateColours);
		
		
		for (Transition t : state.getOutput()) {
			if (!visited[t.getTarget().getId()]) {
				executedPropositions.push(t.getPositiveLabel()); //Returns a label even if the transition is negative which is useful in this case
				processGlobalAutomatonState(t.getTarget(), visited, constraintAutomata, globalAutomatonColours, executedPropositions);
				executedPropositions.pop();
			}
		}
	}
}
