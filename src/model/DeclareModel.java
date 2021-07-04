package model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.processmining.ltl2automaton.plugins.automaton.DeterministicAutomaton;
import org.processmining.ltl2automaton.plugins.ltl.SyntaxParserException;
import org.processmining.plugins.declareminer.ExecutableAutomaton;

import model.constraint.DeclareConstraint;
import proposition.PropositionData;
import proposition.attribute.AttributeType;
import utils.AutomatonUtils;

public class DeclareModel extends AbstractModel {
	
	private List<DeclareConstraint> declareConstraints = new ArrayList<DeclareConstraint>();
	
	public DeclareModel(String modelName, int violationCost, Set<String> activityNames, Map<String, AttributeType> attributeTypeMap, List<DeclareConstraint> declareConstraints) {
		super(modelName, ModelType.DECLARE, violationCost, activityNames, attributeTypeMap);
		
		this.declareConstraints = declareConstraints;
	}
	
	public List<DeclareConstraint> getDeclareConstraints() {
		return declareConstraints;
	}
	
	@Override
	public void initializeAutomaton(PropositionData propositionData) {
		List<DeterministicAutomaton> automata = new ArrayList<DeterministicAutomaton>();
		for (DeclareConstraint declareConstraint : declareConstraints) {
			String ltlFormula = AutomatonUtils.getGenericLtlFormula(declareConstraint.getTemplate());
			Set<String> activityPropositions;
			
			//Replacing activation activity with propositions
			if (declareConstraint.getActivationCondition() != null) {
				String dataCondition = declareConstraint.getActivationCondition().replace("A.", ""); //TODO: Works incorrectly if attribute name contains the string "A."
				activityPropositions = propositionData.getMatchingActivityPropositions(declareConstraint.getActivationActivity(), dataCondition);
			} else {
				activityPropositions = propositionData.getAllActivityPropositions(declareConstraint.getActivationActivity());
			}
			ltlFormula = ltlFormula.replace("\"A\"", "( " + String.join(" \\/ ", activityPropositions) + " )");
			
			//Replacing target activity with propositions
			if (declareConstraint.getTemplate().getIsBinary()) {
				if (declareConstraint.getTargetCondition() != null) {
					String dataCondition = declareConstraint.getTargetCondition().replace("T.", ""); //TODO: Works incorrectly if attribute name contains the string "T."
					activityPropositions = propositionData.getMatchingActivityPropositions(declareConstraint.getTargetActivity(), dataCondition);
				} else {
					activityPropositions = propositionData.getAllActivityPropositions(declareConstraint.getTargetActivity());
				}
				ltlFormula = ltlFormula.replace("\"B\"", "( " + String.join(" \\/ ", activityPropositions) + " )");
			}
			
			//Creating automata for the propositionalized formula
			try {
				automata.add(AutomatonUtils.createAutomatonForLtlFormula(ltlFormula));
			} catch (SyntaxParserException e) {
				System.err.println("Unable to create automaton for declare constraint: " + declareConstraint.toString());
				e.printStackTrace();
			}
		}
		
		automaton = AutomatonUtils.createMinimizedIntersecrtion(automata);
		executableAutomaton = new ExecutableAutomaton(automaton);
		System.out.println("\tAutomata created for model: " + getModelName());
	}

}
