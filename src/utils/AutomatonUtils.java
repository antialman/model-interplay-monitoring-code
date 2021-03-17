package utils;

import java.util.HashMap;
import java.util.List;
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
import data.PropositionData;

public class AutomatonUtils {
	
	private AutomatonUtils() {
		//Private constructor to avoid unnecessary instantiation of the class
	}
	
	public static Map<ExecutableAutomaton, String> createConstraintAutomatons(List<DeclareConstraint> declareConstraints, PropositionData propositionData) {
		Map<ExecutableAutomaton, String> constraintAutomatons = new HashMap<ExecutableAutomaton, String>();
		TreeFactory<ConjunctionTreeNode, ConjunctionTreeLeaf> treeFactory = DefaultTreeFactory.getInstance();
		ConjunctionFactory<? extends GroupedTreeConjunction> conjunctionFactory = GroupedTreeConjunction.getFactory(treeFactory);
		
		for (DeclareConstraint declareConstraint : declareConstraints) {
			String ltlFormula = LtlUtils.getPropositionalizedLtlFormula(declareConstraint, propositionData);
			System.out.println("Declare constraint: " + declareConstraint.toString());
			System.out.println("Propositionalized formula: " + ltlFormula);
			
			//Parsing the ltl formula
			try {
				Formula parsedFormula = new DefaultParser(ltlFormula).parse();
				System.out.println("Parsed formula: " + parsedFormula);
				GroupedTreeConjunction conjunction = conjunctionFactory.instance(parsedFormula);
				Automaton aut = conjunction.getAutomaton().op.reduce();
				ExecutableAutomaton execAut = new ExecutableAutomaton(aut);
				constraintAutomatons.put(execAut, declareConstraint.getConstraintString());
			} catch (SyntaxParserException e) {
				System.out.println("Unable to parse formula: " + ltlFormula);
				e.printStackTrace();
			}
			System.out.println("");
		}
		
		return constraintAutomatons;
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
