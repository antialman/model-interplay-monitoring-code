package data;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import data.proposition.AbstractAttribute;
import data.proposition.Activity;
import data.proposition.AttributeFloat;
import data.proposition.AttributeInteger;
import data.proposition.AttributeString;
import data.proposition.AttributeType;

public class PropositionData {
	private Map<String, Activity> activityMap = new HashMap<String, Activity>();
	
	
	
	//Should be called for each activity in the models
	public void addActivity(String activityName) {
		if (!activityMap.containsKey(activityName)) {
			activityMap.put(activityName, new Activity(activityName, activityMap.values().size()));
		}
	}
	
	
	
	//Should be called for each attribute in the models
	//Creates activity object if needed
	public void addAttribute(String activityName, String attributeName, AttributeType attributeType) {
		if (!activityMap.containsKey(activityName)) {
			activityMap.put(activityName, new Activity(activityName, activityMap.values().size()));
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
			activityMap.put(activityName, new Activity(activityName, activityMap.values().size()));
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
			activityMap.put(activityName, new Activity(activityName, activityMap.values().size()));
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
			activityMap.put(activityName, new Activity(activityName, activityMap.values().size()));
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
	
	
}
