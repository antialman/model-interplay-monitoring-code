package data;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import data.proposition.AbstractAttribute;
import data.proposition.Activity;
import data.proposition.AttributeFloat;
import data.proposition.AttributeInteger;
import data.proposition.AttributeString;
import data.proposition.AttributeType;

public class PropositionData {
	//For looking up activities based on activity name
	private Map<String, Activity> activityMap = new HashMap<String, Activity>();

	//For looking up activities based on activity id
	private Map<Integer, Activity> activityIdMap = new HashMap<Integer, Activity>();


	//Should be called for each activity in the models
	public void addActivity(String activityName) {
		if (!activityMap.containsKey(activityName)) {
			Activity activity = new Activity(activityName, activityMap.values().size());
			activityMap.put(activityName, activity);
			activityIdMap.put(Integer.valueOf(activity.getId()), activity);
		}
	}


	//Should be called for each attribute in the models
	//Creates activity object if needed
	public void addAttribute(String activityName, String attributeName, AttributeType attributeType) {
		if (!activityMap.containsKey(activityName)) {
			addActivity(activityName);
		}
		Map<String, AbstractAttribute<?>> attributes = activityMap.get(activityName).getAttributes();
		if (!attributes.containsKey(attributeName)) {
			switch (attributeType) {
			case STRING:
				attributes.put(attributeName, new AttributeString(attributeName, attributes.size()));
				break;
			case FLOAT:
				attributes.put(attributeName, new AttributeFloat(attributeName, attributes.size()));
				break;
			case INTEGER:
				attributes.put(attributeName, new AttributeInteger(attributeName, attributes.size()));
				break;
			default:
				break;
			}
		}
	}



	//Should be called for each string value that is used in attribute conditions (mixing of types is not allowed for the same attribute)
	//Creates activity object and attribute object if needed
	public void addAttributeValue(String activityName, String attributeName, String attributeValue) {
		if (!activityMap.containsKey(activityName)) {
			addActivity(activityName);
		}
		Map<String, AbstractAttribute<?>> attributes = activityMap.get(activityName).getAttributes();
		if (!attributes.containsKey(attributeName)) {
			attributes.put(attributeName, new AttributeString(attributeName, attributes.size()));
		} else if (attributes.get(attributeName).getAttributeType() != AttributeType.STRING) {
			System.out.println("mixing of types is not allowed for the same attribute");
			System.out.println("skipping attribute: " + activityName + "." + attributeName + " - value: " + attributeValue);
			return;
		}

		AttributeString attribute = (AttributeString) activityMap.get(activityName).getAttributes().get(attributeName);
		attribute.addConditionValue(attributeValue);
	}


	//Should be called for each string value that is used in attribute conditions (mixing of types is not allowed for the same attribute)
	//Creates activity object and attribute object if needed
	public void addAttributeValue(String activityName, String attributeName, Integer attributeValue) {
		if (!activityMap.containsKey(activityName)) {
			addActivity(activityName);
		}
		Map<String, AbstractAttribute<?>> attributes = activityMap.get(activityName).getAttributes();
		if (!attributes.containsKey(attributeName)) {
			attributes.put(attributeName, new AttributeInteger(attributeName, attributes.size()));
		} else if (attributes.get(attributeName).getAttributeType() != AttributeType.INTEGER) {
			System.out.println("mixing of types is not allowed for the same attribute");
			System.out.println("skipping attribute: " + activityName + "." + attributeName + " - value: " + attributeValue);
			return;
		}

		AttributeInteger attribute = (AttributeInteger) activityMap.get(activityName).getAttributes().get(attributeName);
		attribute.addConditionValue(attributeValue);
	}


	//Should be called for each string value that is used in attribute conditions (mixing of types is not allowed for the same attribute)
	//Creates activity object and attribute object if needed
	public void addAttributeValue(String activityName, String attributeName, Float attributeValue) {
		if (!activityMap.containsKey(activityName)) {
			addActivity(activityName);
		}
		Map<String, AbstractAttribute<?>> attributes = activityMap.get(activityName).getAttributes();
		if (!attributes.containsKey(attributeName)) {
			attributes.put(attributeName, new AttributeFloat(attributeName, attributes.size()));
		} else if (attributes.get(attributeName).getAttributeType() != AttributeType.FLOAT) {
			System.out.println("mixing of types is not allowed for the same attribute");
			System.out.println("skipping attribute: " + activityName + "." + attributeName + " - value: " + attributeValue);
			return;
		}

		AttributeFloat attribute = (AttributeFloat) activityMap.get(activityName).getAttributes().get(attributeName);
		attribute.addConditionValue(attributeValue);
	}

	public Activity getActivity(String activityName) {
		return activityMap.get(activityName);
	}

	public Set<String> getAllActivityPropositions(String activityName) {
		return activityMap.get(activityName).getAllPropositions();
	}

	public Set<String> getMatchingPropositions(String activityName, String activityCondition) {
		return activityMap.get(activityName).getMatchingPropositionNames(activityCondition);
	}
	
	public Set<String> getActivityNames() {
		return activityMap.keySet();
	}

	
	public String propositionToActivityString(String proposition) {
		int attIndex = proposition.indexOf("att");
		if (attIndex == -1) {
			Integer activityId = Integer.valueOf(proposition.substring(3));
			return activityIdMap.get(activityId).getName();
		} else {
			Integer activityId = Integer.valueOf(proposition.substring(3, attIndex));
			Activity recommendedActivity = activityIdMap.get(activityId);
			
			Map<Integer, Integer> attributePropositionMap = new HashMap<Integer, Integer>();
			while (attIndex != -1) {
				int pIndex = proposition.indexOf("p", attIndex);
				
				Integer attributeId = Integer.valueOf(proposition.substring(attIndex+3, pIndex));
				
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
				attributePropositionMap.put(attributeId, propositionId);
			}
			
			StringBuilder sb = new StringBuilder(recommendedActivity.getName());
			sb.append("[");
			Iterator<AbstractAttribute<?>> it = recommendedActivity.getAttributes().values().iterator();
			while (it.hasNext()) {
				AbstractAttribute<?> attribute = it.next();
				sb.append(attribute.getName()).append("=");
				String propositionValue = attribute.getPropositionValue(attributePropositionMap.get(attribute.getId()));
				sb.append(propositionValue);
				if (it.hasNext()) {
					sb.append(";");
				}
			}
			sb.append("]");
			return sb.toString();
		}
	}
}
