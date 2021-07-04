package model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.processmining.ltl2automaton.plugins.automaton.DeterministicAutomaton;
import org.processmining.ltl2automaton.plugins.ltl.SyntaxParserException;
import org.processmining.plugins.declareminer.ExecutableAutomaton;

import model.constraint.LtlConstraint;
import proposition.PropositionData;
import proposition.attribute.AttributeType;
import utils.AutomatonUtils;

public class LtlModel extends AbstractModel {
	
	private List<LtlConstraint> ltlFormulas = new ArrayList<LtlConstraint>();
	
	public LtlModel(String modelName, int violationCost, Set<String> activityNames, Map<String, AttributeType> attributeTypeMap, List<LtlConstraint> ltlFormulas) {
		super(modelName, ModelType.LTL, violationCost, activityNames, attributeTypeMap);
		
		this.ltlFormulas = ltlFormulas;
	}
	
	public List<LtlConstraint> getLtlFormulas() {
		return ltlFormulas;
	}

	@Override
	public void initializeAutomaton(PropositionData propositionData) {
		List<DeterministicAutomaton> automata = new ArrayList<DeterministicAutomaton>();
		for (LtlConstraint ltlConstraint : ltlFormulas) {
			String ltlFormula = ltlConstraint.getFormulaString();
			
			//Replacing activities that have data conditions with propositions
			for (String dataCondition : ltlConstraint.getDataConditions()) {
				String activityName = dataCondition.substring(0, dataCondition.indexOf("."));
				dataCondition = dataCondition.replace(activityName+".", ""); //TODO: Works incorrectly if attribute name contains the activity name followed by a dot
				Set<String> activityPropositions = propositionData.getMatchingActivityPropositions(activityName, dataCondition);
				
				ltlFormula = ltlFormula.replace(activityName, "( " + String.join(" \\/ ", activityPropositions) + " )");
			}
			
			//Replacing activities that do not have data conditions with propositions
			for (String activityName : getActivityNames()) {
				Set<String> activityPropositions = propositionData.getAllActivityPropositions(activityName);
				ltlFormula = ltlFormula.replace(activityName, "( " + String.join(" \\/ ", activityPropositions) + " )");
			}
			
			//Creating automata for the propositionalized formula
			try {
				automata.add(AutomatonUtils.createAutomatonForLtlFormula(ltlFormula));
			} catch (SyntaxParserException e) {
				System.err.println("Unable to create automaton for formula: " + ltlConstraint.getFormulaString());
				e.printStackTrace();
			}
		}
		
		automaton = AutomatonUtils.createMinimizedIntersecrtion(automata);
		executableAutomaton = new ExecutableAutomaton(automaton);
		System.out.println("\tAutomata created for model: " + getModelName());
	}
}
