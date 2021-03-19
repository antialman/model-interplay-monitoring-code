package utils;

import java.util.HashMap;
import java.util.Map;

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
	
	public static Map<ExecutableAutomaton, String> createConstraintAutomatons(Map<DeclareConstraint, String> ltlFormulaMap) {
		for (DeclareConstraint declareConstraint : ltlFormulaMap.keySet()) {
			
			ExecutableAutomaton execAut = createAutomatonForLTLFormula(ltlFormulaMap.get(declareConstraint), true);
			constraintAutomatons.put(execAut, declareConstraint.getConstraintString());
		}
		return constraintAutomatons;
	}
	
	public static ExecutableAutomaton createAutomatonForLTLFormula(String ltlFormula, boolean reduce) {
		ExecutableAutomaton execAut = null;
		
		//Parsing the ltl formula
		try {
			Formula parsedFormula = new DefaultParser(ltlFormula).parse();
			System.out.println("Parsed formula: " + parsedFormula);
			GroupedTreeConjunction conjunction = conjunctionFactory.instance(parsedFormula);
			Automaton aut;
			if (reduce) {
				aut = conjunction.getAutomaton().op.reduce();
			} else {
				aut = conjunction.getAutomaton();
			}
			execAut = new ExecutableAutomaton(aut);
		} catch (SyntaxParserException e) {
			System.out.println("Unable to parse formula: " + ltlFormula);
			e.printStackTrace();
		}
		System.out.println("");
		
		return execAut;
		
	}

	public static String execPropositionOnAutomaton(String eventProposition, ExecutableAutomaton executableAutomaton, String currentTruthValue) {
		PossibleNodes current = executableAutomaton.currentState();
		boolean violated = true;
		String newTruthValue;
		if(currentTruthValue.equals("sat") || currentTruthValue.equals("viol")){
			return currentTruthValue;
		}
		if(current!=null&& !(current.get(0)==null)){
			for (Transition out : current.output()) {
				if (out.parses(eventProposition)) {
					violated = false;
					break;
				}
			}
		}

		if (!violated){
			ExecutableAutomaton exec = executableAutomaton;
			exec.next(eventProposition);
			
			current = executableAutomaton.currentState();
			if (current.isAccepting()) {
				newTruthValue = "poss.sat";
				for (State state : current) {
					if (state.isAccepting()) {
						for (Transition t : state.getOutput()) {
							if (t.isAll() && t.getTarget().equals(state)) {
								newTruthValue = "sat";
							}
						}
					}
				}
			} else {
				newTruthValue = "poss.viol";
			}
		} else {
			newTruthValue = "viol";
		}
		
		return newTruthValue;
	}
	
}
