package proposition;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import model.AbstractModel;
import model.DeclareModel;
import model.DpnModel;
import model.LtlModel;
import model.ModelType;
import proposition.attribute.AbstractAttribute;
import proposition.attribute.AttributeType;
import proposition.attribute.FloatAttribute;
import proposition.attribute.IntegerAttribute;
import proposition.attribute.StringAttribute;
import utils.PropositionUtils;

public class PropositionData {

	//For looking up activities based on activity name
	private Map<String, Activity> activityNameToActivity = new HashMap<String, Activity>();
	//For looking up activities based on activity id
	private Map<String, Activity> activityIdToActivity = new HashMap<String, Activity>();

	//For looking up attributes based on activity name
	private Map<String, AbstractAttribute<?>> attributeNameToAttribute = new HashMap<String, AbstractAttribute<?>>();
	//For looking up attributes based on activity id
	private Map<String, AbstractAttribute<?>> attributeIdToAttribute = new HashMap<String, AbstractAttribute<?>>();



	//Encodes and adds new activities from the process model
	public void addActivities(AbstractModel processModel) {
		for (String activityName : processModel.getActivityNames()) {
			if (!activityNameToActivity.containsKey(activityName)) {
				String activityId = "act" + activityNameToActivity.size();
				Activity activity = new Activity(activityName, activityId);
				activityNameToActivity.put(activityName, activity);
				activityIdToActivity.put(activityId, activity);
			}
		}

	}

	//Encodes and adds both new attributes and corresponding constants from the process model 
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
		IntegerAttribute integerAttribute;
		if (attributeNameToAttribute.containsKey(attributeName)) {
			//If an attribute of the same name already exists and the type is correct then using the existing attribute
			if (attributeNameToAttribute.get(attributeName).getAttributeType() == AttributeType.INTEGER) {
				integerAttribute = (IntegerAttribute) attributeNameToAttribute.get(attributeName);
			} else {
				System.err.println("Skipping invalid constant for integer attribute: " + constant);
				return;
			}
		} else {
			//If an attribute of the same name does not exist then create it
			String attributeId = "att" + attributeNameToAttribute.size();
			integerAttribute = new IntegerAttribute(attributeName, attributeId);
			attributeNameToAttribute.put(attributeName, integerAttribute);
			attributeIdToAttribute.put(attributeId, integerAttribute);
		}

		//Adds attribute to the activity
		activityNameToActivity.get(activityName).addAttribute(integerAttribute);
		//Adds constant to the attribute
		integerAttribute.addConstant(constant);
	}

	//Adds attribute constant to float attribute, creates the attribute and updates attribute-activity relations if needed
	public void addAttributeConstant(String activityName, String attributeName, Float constant) {
		FloatAttribute floatAttribute;
		if (attributeNameToAttribute.containsKey(attributeName)) {
			//If an attribute of the same name already exists and the type is correct then using the existing attribute
			if (attributeNameToAttribute.get(attributeName).getAttributeType() == AttributeType.FLOAT) {
				floatAttribute = (FloatAttribute) attributeNameToAttribute.get(attributeName);
			} else {
				System.err.println("Skipping invalid constant for float attribute: " + constant);
				return;
			}
		} else {
			//If an attribute of the same name does not exist then create it
			String attributeId = "att" + attributeNameToAttribute.size();
			floatAttribute = new FloatAttribute(attributeName, attributeId);
			attributeNameToAttribute.put(attributeName, floatAttribute);
			attributeIdToAttribute.put(attributeId, floatAttribute);
		}

		//Adds attribute to the activity
		activityNameToActivity.get(activityName).addAttribute(floatAttribute);
		//Adds constant to the attribute
		floatAttribute.addConstant(constant);
	}

	//Adds attribute constant to string attribute, creates the attribute and updates attribute-activity relations if needed
	public void addAttributeConstant(String activityName, String attributeName, String constant) {
		StringAttribute stringAttribute;
		if (attributeNameToAttribute.containsKey(attributeName)) {
			//If an attribute of the same name already exists and the type is correct then using the existing attribute
			if (attributeNameToAttribute.get(attributeName).getAttributeType() == AttributeType.STRING) {
				stringAttribute = (StringAttribute) attributeNameToAttribute.get(attributeName);
			} else {
				System.err.println("Skipping invalid constant for string attribute: " + constant);
				return;
			}
		} else {
			//If an attribute of the same name does not exist then create it
			String attributeId = "att" + attributeNameToAttribute.size();
			stringAttribute = new StringAttribute(attributeName, attributeId);
			attributeNameToAttribute.put(attributeName, stringAttribute);
			attributeIdToAttribute.put(attributeId, stringAttribute);
		}

		//Adds attribute to the activity
		activityNameToActivity.get(activityName).addAttribute(stringAttribute);
		//Adds constant to the attribute
		stringAttribute.addConstant(constant);
	}

	public Set<String> getAllActivityPropositions(String activityName) {
		return activityNameToActivity.get(activityName).getAllPropositionNames();
	}

	public Set<String> getMatchingActivityPropositions(String activityName, String activityCondition) { 
		return activityNameToActivity.get(activityName).getMatchingPropositionNames(activityCondition);
	}

	public String getAttributeId(String attributeName) {
		return attributeNameToAttribute.get(attributeName).getId();
	}

	public Set<String> getAllAttributePropositions(String attributeName) {
		return attributeNameToAttribute.get(attributeName).getAllPropositionNames();
	}
	
	public Set<String> getMatchingAttributePropositions(String attributeName, String atomicCondition) {
		return attributeNameToAttribute.get(attributeName).getMatchingPropositionNames(atomicCondition);
	}
	
	//Methods used for log replay
	public Activity getActivity(String activityName) {
		return activityNameToActivity.get(activityName);
	}
	public void addActivity(String activityName) {
		if (!activityNameToActivity.containsKey(activityName)) {
			String activityId = "act" + activityNameToActivity.size();
			Activity activity = new Activity(activityName, activityId);
			activityNameToActivity.put(activityName, activity);
			activityIdToActivity.put(activityId, activity);
		}
	}
	public String propositionToActivityString(String proposition, boolean ignorePx) {
		int attIndex = proposition.indexOf("att");
		if (attIndex == -1) {
			return activityIdToActivity.get(proposition).getName();
		} else {
			String activityId = proposition.substring(0, attIndex);
			Activity recommendedActivity = activityIdToActivity.get(activityId);

			Map<String, Integer> attributePropositionMap = new HashMap<String, Integer>();
			while (attIndex != -1) {
				int pIndex = proposition.indexOf("p", attIndex);

				String attributeId = proposition.substring(attIndex, pIndex);

				attIndex = proposition.indexOf("att", pIndex);

				String propositionIdString;
				Integer propositionId;
				if (attIndex == -1) {
					propositionIdString = proposition.substring(pIndex+1);
				} else {
					propositionIdString = proposition.substring(pIndex+1, attIndex);
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

			StringBuilder sb = new StringBuilder(recommendedActivity.getName());
			if (!attributePropositionMap.isEmpty()) {
				sb.append(" [");
				Iterator<AbstractAttribute<?>> it = recommendedActivity.getAttributes().values().iterator();
				while (it.hasNext()) {
					AbstractAttribute<?> attribute = it.next();
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
	}
}
