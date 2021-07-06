package model;

import java.util.Map;
import java.util.Set;

import org.processmining.ltl2automaton.plugins.automaton.DeterministicAutomaton;
import org.processmining.plugins.declareminer.ExecutableAutomaton;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import proposition.PropositionData;
import proposition.attribute.AttributeType;

public abstract class AbstractModel {
	private String modelName;
	private ModelType modelType;
	private IntegerProperty violationCost = new SimpleIntegerProperty();
	private Set<String> activityNames;
	private Map<String, AttributeType> attributeTypeMap;
	protected DeterministicAutomaton automaton;
	protected ExecutableAutomaton executableAutomaton;

	public AbstractModel(String modelName, ModelType modelType, int violationCost, Set<String> activityNames, Map<String, AttributeType> attributeTypeMap) {
		this.modelName = modelName;
		this.modelType = modelType;
		this.setViolationCost(violationCost);
		this.activityNames = activityNames;
		this.attributeTypeMap = attributeTypeMap;
	}
	
	public String getModelName() {
		return modelName;
	}
	
	public ModelType getModelType() {
		return modelType;
	}
	
	public int getViolationCost() {
		return this.violationCostProperty().get();
	}
	
	public void setViolationCost(int violationCost) {
		 this.violationCostProperty().set(violationCost);
	}
	
	public IntegerProperty violationCostProperty() { //For modifying the violation cost trough GUI
		return this.violationCost;
	}
	
	public Set<String> getActivityNames() {
		return activityNames;
	}
	
	public Map<String, AttributeType> getAttributeTypeMap() {
		return attributeTypeMap;
	}

	public DeterministicAutomaton getAutomaton() {
		return automaton;
	}
	
	public ExecutableAutomaton getExecutableAutomaton() {
		return executableAutomaton;
	}
	
	public abstract void initializeAutomaton(PropositionData propositionData);
	
	
	@Override
	public String toString() {
		return "AbstractModel [modelName=" + modelName + ", modelType=" + modelType + ", violationCost=" + violationCost
				+ "]";
	}

	
	
}
