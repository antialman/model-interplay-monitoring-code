package data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.model.XAttribute;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.impl.XAttributeContinuousImpl;
import org.deckfour.xes.model.impl.XAttributeDiscreteImpl;
import org.deckfour.xes.model.impl.XAttributeLiteralImpl;

import data.activity.Activity;
import data.attribute.AbstractAttribute;
import data.attribute.AttributeFloat;
import data.attribute.AttributeInteger;
import data.attribute.AttributeString;
import data.attribute.AttributeType;

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
	
	public String eventToProposition(XEvent xevent) {
		
		String eventName = XConceptExtension.instance().extractName(xevent);
		System.out.println("eventToProposition: " + eventName);
		
		if (!activityMap.containsKey(eventName)) {
			System.out.println("\tActivity not found in proposition data, adding: " + eventName);
			addActivity(eventName);
		}
		
		List<String> propositionList = new ArrayList<String>();
		Activity activity = activityMap.get(eventName);
		propositionList.add("act");
		propositionList.add(Integer.toString(activity.getId()));
		
		Map<String, AbstractAttribute<?>> attributes = activity.getAttributes();
		
		if (attributes.isEmpty()) {
			System.out.println("\tNo related attributes based on the models");
		}
		
		for (String attributeName : attributes.keySet()) {
			propositionList.add("_");
			propositionList.add("att");
			AbstractAttribute<?> attribute = attributes.get(attributeName);
			propositionList.add(Integer.toString(attribute.getId()));
			propositionList.add("p");
			
			XAttribute xattribute = xevent.getAttributes().get(attributeName);
			
			System.out.println("\tProposition attribute: " + attribute.toString());
			
			String proposition = "";
			
			switch (attribute.getAttributeType()) {
			case FLOAT:
				if (xattribute instanceof XAttributeContinuousImpl) {
					AttributeFloat attributeFloat = (AttributeFloat) attribute;
					XAttributeContinuousImpl xAttributeContinuousImpl = (XAttributeContinuousImpl) xattribute;
					System.out.println("\tLog attribute: " + xAttributeContinuousImpl.getKey() + "=" + xAttributeContinuousImpl.getValue());
					proposition = "p" + Integer.toString(attributeFloat.getPropositionId((float) xAttributeContinuousImpl.getValue()));
				}
				break;
			case INTEGER:
				if (xattribute instanceof XAttributeDiscreteImpl) {
					AttributeInteger attributeInteger = (AttributeInteger) attribute;
					XAttributeDiscreteImpl xAttributeDiscreteImpl = (XAttributeDiscreteImpl) xattribute;
					System.out.println("\tLog attribute: " + xAttributeDiscreteImpl.getKey() + "=" + xAttributeDiscreteImpl.getValue());
					proposition = "p" + Integer.toString(attributeInteger.getPropositionId((int) xAttributeDiscreteImpl.getValue()));
				}
				break;
			case STRING:
				if (xattribute instanceof XAttributeLiteralImpl) {
					AttributeString attributeString = (AttributeString) attribute;
					XAttributeLiteralImpl xAttributeLiteralImpl = (XAttributeLiteralImpl) xattribute;
					System.out.println("\tLog attribute: " + xAttributeLiteralImpl.getKey() + "=" + xAttributeLiteralImpl.getValue());
					proposition = "p" + Integer.toString(attributeString.getPropositionId(xAttributeLiteralImpl.getValue()));
				}
				break;
			default:
				break;
			
			}
			
			if (proposition.isBlank()) {
				System.err.println("Possible attribute type mismatch between model and log, using p-1 as the proposition");
				proposition = "p-1";
			}
			
			propositionList.add(proposition);
			System.out.println("\tAttribute proposition: " + proposition);
		}
		
		String eventProposition = String.join("", propositionList);
		System.out.println("Event proposition: " + eventProposition);
		System.out.println("");
		return eventProposition;
	}
	
}
