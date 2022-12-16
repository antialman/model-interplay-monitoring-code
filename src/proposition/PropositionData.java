package proposition;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import controller.ScopeSelection;
import model.AbstractModel;
import model.DeclareModel;
import model.DpnModel;
import model.LtlModel;
import model.ModelType;
import proposition.attribute.AbstractVariable;
import proposition.attribute.VariableType;
import proposition.attribute.FloatVariable;
import proposition.attribute.IntegerVariable;
import proposition.attribute.StringVariable;
import utils.PropositionUtils;

public class PropositionData {

	//For looking up models based on model name
	private Map<String, AbstractModel> modelNameToModel = new HashMap<String, AbstractModel>();
	//For looking up models based on model id
	private Map<String, AbstractModel> modelIdToModel = new HashMap<String, AbstractModel>();

	//For looking up global activities based on activity name
	private Map<String, Activity> globalActivityNameToActivity = new HashMap<String, Activity>();
	//For looking up global activities based on activity id
	private Map<String, Activity> globalActivityIdToActivity = new HashMap<String, Activity>();
	//For looking up constraint activities based on activity name
	private Map<String, Activity> constraintActivityNameToActivity = new HashMap<String, Activity>();
	//For looking up constraint activities based on activity id
	private Map<String, Activity> constraintActivityIdToActivity = new HashMap<String, Activity>();

	//For looking up global variables based on variable name
	private Map<String, AbstractVariable<?>> globalVariableNameToVariable = new HashMap<String, AbstractVariable<?>>();
	//For looking up global variables based on activity id
	private Map<String, AbstractVariable<?>> globalVariableIdToVariable = new HashMap<String, AbstractVariable<?>>();
	//For looking up constraint variables based on variable name
	private Map<String, AbstractVariable<?>> constraintVariableNameToVariable = new HashMap<String, AbstractVariable<?>>();
	//For looking up constraint variables based on activity id
	private Map<String, AbstractVariable<?>> constraintVariableIdToVariable = new HashMap<String, AbstractVariable<?>>();

	//For looking up global variable write operations of a given local activity
	private Map<Activity, Set<String>> localActivityToGlobalWriteIDs = new HashMap<Activity, Set<String>>();

	//For looking up global variable write operations of a given local activity
	private Map<Activity, Map<AbstractModel, Set<String>>> globalActivityToGlobalWriteIDs = new HashMap<Activity, Map<AbstractModel,Set<String>>>();

	//For looking up local activities based on activity id
	private Map<String, Activity> localActivityIdToActivity = new HashMap<String, Activity>();

	//For looking up local variables based on variable id
	private Map<String, AbstractVariable<?>> localVariableIdToVariable = new HashMap<String, AbstractVariable<?>>();


	//For looking up variable types
	private Map<String, VariableType> fullVariableTypeMap = new HashMap<String, VariableType>();



	//For looking up activities based on activity name
	private Map<String, Activity> activityNameToActivity = new HashMap<String, Activity>();
	//For looking up activities based on activity id
	private Map<String, Activity> activityIdToActivity = new HashMap<String, Activity>();

	//For looking up attributes based on activity name
	private Map<String, AbstractVariable<?>> attributeNameToAttribute = new HashMap<String, AbstractVariable<?>>();
	//For looking up attributes based on activity id
	private Map<String, AbstractVariable<?>> attributeIdToAttribute = new HashMap<String, AbstractVariable<?>>();


	public void setModels(List<AbstractModel> abstractModels) {
		for (AbstractModel abstractModel : abstractModels) {
			abstractModel.setModelId("m"+modelNameToModel.size());
			modelNameToModel.put(abstractModel.getModelName(), abstractModel);
			modelIdToModel.put(abstractModel.getModelId(), abstractModel);
			fullVariableTypeMap.putAll(abstractModel.getVariableTypeMap());
		}
	}

	public void setDpnGlobalActivities(List<ScopeSelection> activityScopeSelections) {
		for (ScopeSelection scopeSelection : activityScopeSelections) {
			if (scopeSelection.getIsGlobalScope()) {
				Activity activity = new Activity(scopeSelection.getItemName(), "ga" + globalActivityNameToActivity.size(), ScopeType.GLOBAL);
				globalActivityNameToActivity.put(activity.getName(), activity);
				globalActivityIdToActivity.put(activity.getId(), activity);
			}
		}
	}


	public void setDpnLocalActivities(List<AbstractModel> abstractModels) {
		for (AbstractModel abstractModel : abstractModels) {
			if (abstractModel.getModelType() == ModelType.DPN) {
				for (String activityName : abstractModel.getActivityNames()) {
					if (!globalActivityNameToActivity.containsKey(activityName)) {
						Activity activity = new Activity(activityName, "la" + localActivityIdToActivity.size(), ScopeType.LOCAL);
						localActivityIdToActivity.put(activity.getId(), activity);
						abstractModel.addLocalActivity(activity);
					}
				}
			}
		}
	}

	public void setConstraintActivities(List<AbstractModel> abstractModels) {
		for (AbstractModel abstractModel : abstractModels) {
			if (abstractModel.getModelType() == ModelType.DECLARE || abstractModel.getModelType() == ModelType.LTL) {
				for (String activityName : abstractModel.getActivityNames()) {
					if (!constraintActivityNameToActivity.containsKey(activityName)) {
						Activity activity = new Activity(activityName, "ca" + constraintActivityIdToActivity.size(), ScopeType.CONSTRAINT);
						constraintActivityNameToActivity.put(activityName, activity);
						constraintActivityIdToActivity.put(activity.getId(), activity);
					}
				}
			}
		}
	}


	public void setDpnGlobalVariables(List<ScopeSelection> attributeScopeSelections) {
		for (ScopeSelection scopeSelection : attributeScopeSelections) {
			if (scopeSelection.getIsGlobalScope()) {
				VariableType variableType = fullVariableTypeMap.get(scopeSelection.getItemName());
				String variableId = "gv" + globalVariableNameToVariable.size();
				AbstractVariable<?> abstractVariable = null;
				if (variableType ==  VariableType.INTEGER) {
					abstractVariable = new IntegerVariable(scopeSelection.getItemName(), variableId, ScopeType.GLOBAL);
				} else if (variableType ==  VariableType.FLOAT) {
					abstractVariable = new FloatVariable(scopeSelection.getItemName(), variableId, ScopeType.GLOBAL);
				} else if (variableType ==  VariableType.STRING) {
					abstractVariable = new StringVariable(scopeSelection.getItemName(), variableId, ScopeType.GLOBAL);
				} else {
					System.out.println("Skipping unhandeled attribute type: " + variableType);
				}

				if (abstractVariable != null) {
					globalVariableNameToVariable.put(scopeSelection.getItemName(), abstractVariable);
					globalVariableIdToVariable.put(variableId, abstractVariable);
				}
			}
		}
	}

	public void setDpnLocalVariables(List<AbstractModel> abstractModels) {
		for (AbstractModel abstractModel : abstractModels) {
			if (abstractModel.getModelType() == ModelType.DPN) {
				for (String variableName : abstractModel.getVariableTypeMap().keySet()) {
					if (!globalVariableNameToVariable.containsKey(variableName)) {
						VariableType variableType = fullVariableTypeMap.get(variableName);
						String variableId = "lv" + localVariableIdToVariable.size();
						AbstractVariable<?> abstractVariable = null;
						if (variableType ==  VariableType.INTEGER) {
							abstractVariable = new IntegerVariable(variableName, variableId, ScopeType.LOCAL);
						} else if (variableType ==  VariableType.FLOAT) {
							abstractVariable = new FloatVariable(variableName, variableId, ScopeType.LOCAL);
						} else if (variableType ==  VariableType.STRING) {
							abstractVariable = new StringVariable(variableName, variableId, ScopeType.LOCAL);
						} else {
							System.out.println("Skipping unhandeled attribute type: " + variableType);
						}

						if (abstractVariable != null) {
							localVariableIdToVariable.put(abstractVariable.getId(), abstractVariable);
							abstractModel.addLocalVariable(abstractVariable);
						}
					}
				}	
			}
		}
	}

	public void setConstraintVariables(List<AbstractModel> abstractModels) {
		for (AbstractModel abstractModel : abstractModels) {
			if (abstractModel.getModelType() == ModelType.DECLARE || abstractModel.getModelType() == ModelType.LTL) {
				for (String variableName : abstractModel.getVariableTypeMap().keySet()) {
					if (!constraintVariableNameToVariable.containsKey(variableName)) {
						VariableType variableType = fullVariableTypeMap.get(variableName);
						String variableId = "cv" + constraintVariableIdToVariable.size();
						AbstractVariable<?> abstractVariable = null;
						if (variableType ==  VariableType.INTEGER) {
							abstractVariable = new IntegerVariable(variableName, variableId, ScopeType.CONSTRAINT);
						} else if (variableType ==  VariableType.FLOAT) {
							abstractVariable = new FloatVariable(variableName, variableId, ScopeType.CONSTRAINT);
						} else if (variableType ==  VariableType.STRING) {
							abstractVariable = new StringVariable(variableName, variableId, ScopeType.CONSTRAINT);
						} else {
							System.out.println("Skipping unhandeled attribute type: " + variableType);
						}

						if (abstractVariable != null) {
							constraintVariableNameToVariable.put(variableName, abstractVariable);
							constraintVariableIdToVariable.put(abstractVariable.getId(), abstractVariable);
							abstractModel.addLocalVariable(abstractVariable);
						}
					}
				}
			}

		}
	}



	public void setVariableConnectionsAndPropositions(List<AbstractModel> abstractModels) {
		for (AbstractModel abstractModel : abstractModels) {
			if (abstractModel.getModelType() == ModelType.DECLARE) {
				PropositionUtils.setVariableConnectionsPropositions((DeclareModel) abstractModel, this);
			} else if (abstractModel.getModelType() == ModelType.LTL) {
				PropositionUtils.setVariableConnectionsPropositions((LtlModel) abstractModel, this);
			} else if (abstractModel.getModelType() == ModelType.DPN) {
				PropositionUtils.setVariableConnectionsPropositions((DpnModel) abstractModel, this);
			} else {
				System.err.println("Skipping model of unhandled type: " + abstractModel.getModelType());
			}
		}
	}















	//Encodes and adds new activities from the process model
	public void addActivities(AbstractModel processModel) {
		for (String activityName : processModel.getActivityNames()) {
			if (!activityNameToActivity.containsKey(activityName)) {
				String activityId = "a" + activityNameToActivity.size();
				Activity activity = new Activity(activityName, activityId, ScopeType.GLOBAL);
				activityNameToActivity.put(activityName, activity);
				activityIdToActivity.put(activityId, activity);
			}
		}

	}

	//Encodes and adds both new attributes and corresponding constants from the process model  //TODO: Update also for cmd version
	public void addAttributePropositions(AbstractModel processModel) {
		if (processModel.getModelType() == ModelType.DECLARE) {
			PropositionUtils.addAttributePropositions((DeclareModel) processModel, this);
		} else if (processModel.getModelType() == ModelType.LTL) {
			PropositionUtils.addAttributePropositions((LtlModel) processModel, this);
		} else if (processModel.getModelType() == ModelType.DPN) {
			PropositionUtils.addAttributePropositions((DpnModel) processModel, this);
		} else {
			System.err.println("Skipping model of unhandled type: " + processModel.getModelType());
		}
	}

	//Adds attribute constant to integer attribute, creates the attribute and updates attribute-activity relations if needed
	public void addAttributeConstant(String activityName, String attributeName, Integer constant) {
		IntegerVariable integerAttribute;
		if (attributeNameToAttribute.containsKey(attributeName)) {
			//If an attribute of the same name already exists and the type is correct then using the existing attribute
			if (attributeNameToAttribute.get(attributeName).getAttributeType() == VariableType.INTEGER) {
				integerAttribute = (IntegerVariable) attributeNameToAttribute.get(attributeName);
			} else {
				System.err.println("Skipping invalid constant for integer attribute: " + constant);
				return;
			}
		} else {
			//If an attribute of the same name does not exist then create it
			String attributeId = "att" + attributeNameToAttribute.size();
			integerAttribute = new IntegerVariable(attributeName, attributeId, null);
			attributeNameToAttribute.put(attributeName, integerAttribute);
			attributeIdToAttribute.put(attributeId, integerAttribute);
		}

		//Adds attribute to the activity
		activityNameToActivity.get(activityName).addVariable(integerAttribute);
		//Adds constant to the attribute
		integerAttribute.addConstant(constant.toString());
	}

	//Adds attribute constant to float attribute, creates the attribute and updates attribute-activity relations if needed
	public void addAttributeConstant(String activityName, String attributeName, Float constant) {
		FloatVariable floatAttribute;
		if (attributeNameToAttribute.containsKey(attributeName)) {
			//If an attribute of the same name already exists and the type is correct then using the existing attribute
			if (attributeNameToAttribute.get(attributeName).getAttributeType() == VariableType.FLOAT) {
				floatAttribute = (FloatVariable) attributeNameToAttribute.get(attributeName);
			} else {
				System.err.println("Skipping invalid constant for float attribute: " + constant);
				return;
			}
		} else {
			//If an attribute of the same name does not exist then create it
			String attributeId = "att" + attributeNameToAttribute.size();
			floatAttribute = new FloatVariable(attributeName, attributeId, null);
			attributeNameToAttribute.put(attributeName, floatAttribute);
			attributeIdToAttribute.put(attributeId, floatAttribute);
		}

		//Adds attribute to the activity
		activityNameToActivity.get(activityName).addVariable(floatAttribute);
		//Adds constant to the attribute
		floatAttribute.addConstant(constant.toString());
	}

	//Adds attribute constant to string attribute, creates the attribute and updates attribute-activity relations if needed
	public void addAttributeConstant(String activityName, String attributeName, String constant) {
		StringVariable stringAttribute;
		if (attributeNameToAttribute.containsKey(attributeName)) {
			//If an attribute of the same name already exists and the type is correct then using the existing attribute
			if (attributeNameToAttribute.get(attributeName).getAttributeType() == VariableType.STRING) {
				stringAttribute = (StringVariable) attributeNameToAttribute.get(attributeName);
			} else {
				System.err.println("Skipping invalid constant for string attribute: " + constant);
				return;
			}
		} else {
			//If an attribute of the same name does not exist then create it
			String attributeId = "att" + attributeNameToAttribute.size();
			stringAttribute = new StringVariable(attributeName, attributeId, null);
			attributeNameToAttribute.put(attributeName, stringAttribute);
			attributeIdToAttribute.put(attributeId, stringAttribute);
		}

		//Adds attribute to the activity
		activityNameToActivity.get(activityName).addVariable(stringAttribute);
		//Adds constant to the attribute
		stringAttribute.addConstant(constant);
	}









	public Set<Activity> getActivityInstancesByName(String activityName) {
		Set<Activity> activityInstances = new HashSet<Activity>();
		if (globalActivityNameToActivity.containsKey(activityName)) {
			activityInstances.add(globalActivityNameToActivity.get(activityName));
		} else {
			for (AbstractModel abstractModel : modelIdToModel.values()) {
				if (abstractModel.hasLocalActivityByName(activityName)) {
					activityInstances.add(abstractModel.getLocalActivityByName(activityName));
				}
			}
		}
		if (constraintActivityNameToActivity.containsKey(activityName)) {
			activityInstances.add(constraintActivityNameToActivity.get(activityName));
		}
		return activityInstances;
	}

	public Set<AbstractVariable<?>> getVariableInstancesByName(String variableName) {
		Set<AbstractVariable<?>> variableInstances = new HashSet<AbstractVariable<?>>();
		if (globalVariableNameToVariable.containsKey(variableName)) {
			variableInstances.add(globalVariableNameToVariable.get(variableName));
		} else {
			for (AbstractModel abstractModel : modelIdToModel.values()) {
				if (abstractModel.hasLocalVariableByName(variableName)) {
					variableInstances.add(abstractModel.getLocalVariableByName(variableName));
				}
			}
		}
		if (constraintVariableNameToVariable.containsKey(variableName)) {
			variableInstances.add(constraintVariableNameToVariable.get(variableName));
		}
		return variableInstances;
	}

	public boolean hasGlobalVariableByName(String variableName) {
		return globalVariableNameToVariable.containsKey(variableName);
	}

	public AbstractVariable<?> getGlobalVariableByName(String variableName) {
		return globalVariableNameToVariable.get(variableName);
	}


	public Set<String> getAllActivityPropositions(String activityName) {
		Set<String> allActivityPropositions = new HashSet<String>();
		if (globalActivityNameToActivity.containsKey(activityName)) {
			allActivityPropositions.addAll(globalActivityNameToActivity.get(activityName).getAllPropositionNames());
		} else {
			allActivityPropositions = new HashSet<String>();
			for (AbstractModel abstractModel : modelIdToModel.values()) {
				if (abstractModel.hasLocalActivityByName(activityName)) {
					for (String activityProposition : abstractModel.getLocalActivityByName(activityName).getAllPropositionNames()) {
						allActivityPropositions.add(abstractModel.getModelId()+activityProposition);
					}
				}
			}
		}
		if (constraintActivityNameToActivity.containsKey(activityName)) {
			allActivityPropositions.addAll(constraintActivityNameToActivity.get(activityName).getAllPropositionNames());
		}
		return allActivityPropositions;
	}

	public Set<String> getAllActivityPropositionsForDpn(String activityName, String modelId) {
		Set<String> allActivityPropositions = new HashSet<String>();
		if (globalActivityNameToActivity.containsKey(activityName)) {
			allActivityPropositions.addAll(globalActivityNameToActivity.get(activityName).getAllPropositionNames());
		} else {
			allActivityPropositions = new HashSet<String>();
			AbstractModel abstractModel = modelIdToModel.get(modelId);
			if (abstractModel.hasLocalActivityByName(activityName)) {
				for (String activityProposition : abstractModel.getLocalActivityByName(activityName).getAllPropositionNames()) {
					allActivityPropositions.add(abstractModel.getModelId()+activityProposition);
				}
			}
		}
		return allActivityPropositions;
	}

	public Set<String> getMatchingActivityPropositions(String activityName, String activityCondition) {
		Set<String> allActivityPropositions = new HashSet<String>();
		if (globalActivityNameToActivity.containsKey(activityName)) {
			allActivityPropositions.addAll(globalActivityNameToActivity.get(activityName).getMatchingPropositionNames(activityCondition));
		} else {
			for (AbstractModel abstractModel : modelIdToModel.values()) {
				if (abstractModel.hasLocalActivityByName(activityName)) {
					for (String activityProposition : abstractModel.getLocalActivityByName(activityName).getMatchingPropositionNames(activityCondition)) {
						allActivityPropositions.add(abstractModel.getModelId()+activityProposition);
					}
				}
			}
		}
		if (constraintActivityNameToActivity.containsKey(activityName)) {
			allActivityPropositions.addAll(constraintActivityNameToActivity.get(activityName).getMatchingPropositionNames(activityCondition));
		}
		return allActivityPropositions;
	}


	public Set<String> getMatchingActivityPropositionsForDpnRead(String activityName, String activityCondition, String modelId) {
		Set<String> allActivityPropositions = new HashSet<String>();
		if (globalActivityNameToActivity.containsKey(activityName)) {
			allActivityPropositions.addAll(globalActivityNameToActivity.get(activityName).getMatchingPropositionNames(activityCondition));
		} else {
			AbstractModel abstractModel = modelIdToModel.get(modelId);
			if (abstractModel.hasLocalActivityByName(activityName)) {
				for (String activityProposition : abstractModel.getLocalActivityByName(activityName).getMatchingPropositionNames(activityCondition)) {
					allActivityPropositions.add(abstractModel.getModelId()+activityProposition);
				}
			}
		}
		return allActivityPropositions;
	}











	public String getAttributeId(String attributeName) {
		return attributeNameToAttribute.get(attributeName).getId();
	}


	public String getVariableId(String modelId, String variableName) {
		if (globalVariableNameToVariable.containsKey(variableName)) {
			return globalVariableNameToVariable.get(variableName).getId();
		} else {
			return modelIdToModel.get(modelId).getLocalVariableByName(variableName).getId();
		}
	}





	public Set<String> getAllAttributePropositions(String attributeName) {
		return attributeNameToAttribute.get(attributeName).getAllPropositionNames();
	}

	public Set<String> getMatchingAttributePropositions(String attributeName, String atomicCondition) {
		return attributeNameToAttribute.get(attributeName).getMatchingPropositionNames(atomicCondition, false);
	}


	public Set<String> getMatchingVariablePropositions(String modelId, String variableName, String atomicCondition) {
		if (globalVariableNameToVariable.containsKey(variableName)) {
			return globalVariableNameToVariable.get(variableName).getMatchingPropositionNames(atomicCondition, false);
		} else {
			return modelIdToModel.get(modelId).getLocalVariableByName(variableName).getMatchingPropositionNames(atomicCondition, false);
		}
	}

	public Set<String> getAllVariablePropositions(String modelId, String variableName) {
		if (globalVariableNameToVariable.containsKey(variableName)) {
			return globalVariableNameToVariable.get(variableName).getAllPropositionNames();
		} else {
			return modelIdToModel.get(modelId).getLocalVariableByName(variableName).getAllPropositionNames();
		}
	}




	//Methods used for log replay
	public Activity getActivity(String activityName, String modelName) {
		if (globalActivityNameToActivity.containsKey(activityName)) {
			return globalActivityNameToActivity.get(activityName);
		} else if (modelName != null) {
			if (modelNameToModel.containsKey(modelName)) {
				return modelNameToModel.get(modelName).getLocalActivityByName(activityName);
			} else {
				System.out.println("\tModel not in input specifications: " + modelName);
				if (constraintActivityNameToActivity.containsKey(activityName)) {
					System.out.println("\t... but matches a constraint activity");
					return constraintActivityNameToActivity.get(activityName);
				}
			}
		} else if (constraintActivityNameToActivity.containsKey(activityName)) {
			return constraintActivityNameToActivity.get(activityName);
		}
		return null;
	}

	public void addMissingGlobalActivity(String activityName) {
		Activity activity = new Activity(activityName, "ma" + globalActivityNameToActivity.size(), ScopeType.GLOBAL);
		globalActivityNameToActivity.put(activity.getName(), activity);
		globalActivityIdToActivity.put(activity.getId(), activity);
	}

	public String getModelId(String modelName) {
		if (modelNameToModel.containsKey(modelName)) {
			return modelNameToModel.get(modelName).getModelId();
		} else {
			return null;
		}
	}











	public String propositionToActivityString(String proposition, boolean ignorePx) {
		Activity activity;
		String modelName;
		boolean containsVariables = proposition.contains("v");
		int vIndex;

		if (proposition.contains("ga")) {
			if (containsVariables) {
				vIndex = proposition.indexOf("v");
				activity = globalActivityIdToActivity.get(proposition.substring(0, vIndex-1));
			} else {
				return globalActivityIdToActivity.get(proposition).getName() + " (global)";
			}
		} else if (proposition.contains("la")) {
			if (containsVariables) {
				vIndex = proposition.indexOf("v");
				activity = localActivityIdToActivity.get(proposition.substring(proposition.indexOf("la"), vIndex-1));
			} else {
				modelName = modelIdToModel.get(proposition.substring(0, proposition.indexOf("la"))).getModelName();
				return localActivityIdToActivity.get(proposition.substring(proposition.indexOf("la"))).getName() + " [" + modelName + "]";
			}
		} else if (proposition.contains("ca")) {
			if (containsVariables) {
				vIndex = proposition.indexOf("v");
				activity = constraintActivityIdToActivity.get(proposition.substring(0, vIndex-1));
			} else {
				return constraintActivityIdToActivity.get(proposition).getName() + " (constraint)";
			}
		} else if (proposition.contains("ma")) {
			return globalActivityIdToActivity.get(proposition).getName() + " (not in specifications)";
		} else {
			return proposition;
		}

		Map<String, Integer> attributePropositionMap = new HashMap<String, Integer>();

		while (vIndex != -1) {
			int pIndex = proposition.indexOf("p", vIndex);
			String attributeId = proposition.substring(vIndex-1, pIndex);
			vIndex = proposition.indexOf("v", pIndex);

			String propositionIdString;
			Integer propositionId;
			if (vIndex == -1) {
				propositionIdString = proposition.substring(pIndex+1);
			} else {
				propositionIdString = proposition.substring(pIndex+1, vIndex-1);
			}

			if (propositionIdString.contains("sp")) { //TODO: I should keep only the recommendations that match with currently written global variables
				propositionIdString = propositionIdString.substring(0, propositionIdString.indexOf("sp"));
			}

			if (propositionIdString.equals("x")) {
				propositionId = Integer.valueOf(-1);
			} else {
				propositionId = Integer.valueOf(propositionIdString);
			}

			if (propositionId != -1 || !ignorePx) {
				attributePropositionMap.put(attributeId, propositionId);
			}
		}

		StringBuilder sb = new StringBuilder(activity.getName());
		if (!attributePropositionMap.isEmpty()) {
			sb.append(" [");
			Iterator<AbstractVariable<?>> it = activity.getAttributes().values().iterator();
			while (it.hasNext()) {
				AbstractVariable<?> attribute = it.next();
				if (attributePropositionMap.containsKey(attribute.getId())) {
					sb.append(attribute.getName()).append("=");
					String propositionValue = attribute.getPropositionValue(attributePropositionMap.get(attribute.getId()));
					sb.append(propositionValue);
					if (it.hasNext()) {
						sb.append(";");
					}
				}
			}
			sb.append("]");
		}
		return sb.toString();
	}

	public Set<String> getActivityPropositionsForDpnRead(String activityName, String activityCondition, String modelId) {
		Set<String> allActivityPropositions = new HashSet<String>();
		if (globalActivityNameToActivity.containsKey(activityName)) {
			allActivityPropositions.addAll(globalActivityNameToActivity.get(activityName).getMatchingPropositionNamesForDpnRead(activityCondition));
		} else {
			AbstractModel abstractModel = modelIdToModel.get(modelId);
			if (abstractModel.hasLocalActivityByName(activityName)) {
				for (String activityProposition : abstractModel.getLocalActivityByName(activityName).getMatchingPropositionNamesForDpnRead(activityCondition)) {
					allActivityPropositions.add(abstractModel.getModelId()+activityProposition);
				}
			}
		}
		return allActivityPropositions;
	}

	public Set<String> getGlobalVariableIds() {
		return globalVariableIdToVariable.keySet();
	}


	public void addLocalActivityToGolbalWriteIDs(Activity activity, String variebleId) {
		if (!localActivityToGlobalWriteIDs.containsKey(activity)) {
			localActivityToGlobalWriteIDs.put(activity, new HashSet<String>());
		}
		localActivityToGlobalWriteIDs.get(activity).add(variebleId);
	}

	public Set<String> getLocalActivityToGolbalWriteIDs(Activity activity) {
		return localActivityToGlobalWriteIDs.get(activity);
	}

	public void addGlobalActivityToGolbalWriteIDs(AbstractModel abstractModel, Activity activity, String variableId) {
		if (!globalActivityToGlobalWriteIDs.containsKey(activity)) {
			globalActivityToGlobalWriteIDs.put(activity, new HashMap<AbstractModel, Set<String>>());
		}
		if (!globalActivityToGlobalWriteIDs.get(activity).containsKey(abstractModel)) {
			globalActivityToGlobalWriteIDs.get(activity).put(abstractModel, new HashSet<String>());
		}
		globalActivityToGlobalWriteIDs.get(activity).get(abstractModel).add(variableId);
	}

	public Object getModelByName(String modelName) {
		return modelNameToModel.get(modelName);
	}

	public Map<AbstractModel, Set<String>> getGlobalActivityToGolbalWriteIDs(Activity activity) {
		return globalActivityToGlobalWriteIDs.get(activity);
	}

}
