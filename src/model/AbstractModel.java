package model;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.processmining.ltl2automaton.plugins.automaton.DeterministicAutomaton;
import org.processmining.plugins.declareminer.ExecutableAutomaton;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import proposition.Activity;
import proposition.PropositionData;
import proposition.attribute.AbstractVariable;
import proposition.attribute.VariableType;

public abstract class AbstractModel {
	private String modelName;
	private ModelType modelType;
	private IntegerProperty violationCost = new SimpleIntegerProperty();
	private Set<String> activityNames;
	private Map<String, VariableType> variableTypeMap;
	
	private String modelId;
	
	//For looking up local activities based on activity name
	private Map<String, Activity> localActivityNameToActivity = new HashMap<String, Activity>();
	//For looking up local activities based on activity id
	private Map<String, Activity> localActivityIdToActivity = new HashMap<String, Activity>();
	
	//For looking up local variables based on variable name
	private Map<String, AbstractVariable<?>> localVariableNameToVariable = new HashMap<String, AbstractVariable<?>>();
	//For looking up local variables based on variable id
	private Map<String, AbstractVariable<?>> localVariableIdToVariable = new HashMap<String, AbstractVariable<?>>();
	
	
	protected DeterministicAutomaton automaton;
	protected ExecutableAutomaton executableAutomaton;

	public AbstractModel(String modelName, ModelType modelType, int violationCost, Set<String> activityNames, Map<String, VariableType> variableTypeMap) {
		this.modelName = modelName;
		this.modelType = modelType;
		this.setViolationCost(violationCost);
		this.activityNames = activityNames;
		this.variableTypeMap = variableTypeMap;
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
	
	public Map<String, VariableType> getVariableTypeMap() {
		return variableTypeMap;
	}
	
	
	public void setModelId(String modelId) {
		this.modelId = modelId;
	}
	
	public String getModelId() {
		return modelId;
	}
	
	public boolean hasLocalActivityByName(String activityName) {
		return localActivityNameToActivity.containsKey(activityName);
	}
	public Activity getLocalActivityByName(String activityName) {
		return localActivityNameToActivity.get(activityName);
	}
	public boolean hasLocalVariableByName(String variableName) {
		return localVariableNameToVariable.containsKey(variableName);
	}
	public AbstractVariable<?> getLocalVariableByName(String variableName) {
		return localVariableNameToVariable.get(variableName);
	}
	


	public boolean hasLocalActivity(Activity activity) {
		return localActivityIdToActivity.containsValue(activity);
	}
	
	
	public void addLocalActivity(Activity activity) {
		localActivityNameToActivity.put(activity.getName(), activity);
		localActivityIdToActivity.put(activity.getId(), activity);
	}
	public void addLocalVariable(AbstractVariable<?> variable) {
		localVariableNameToVariable.put(variable.getName(), variable);
		localVariableIdToVariable.put(variable.getId(), variable);
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
				+ "modelId" + modelId + "]";
	}

}
