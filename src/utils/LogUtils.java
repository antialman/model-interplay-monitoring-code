package utils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.extension.std.XSemanticExtension;
import org.deckfour.xes.in.XMxmlParser;
import org.deckfour.xes.in.XesXmlParser;
import org.deckfour.xes.model.XAttribute;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.impl.XAttributeContinuousImpl;
import org.deckfour.xes.model.impl.XAttributeDiscreteImpl;
import org.deckfour.xes.model.impl.XAttributeLiteralImpl;

import proposition.Activity;
import proposition.PropositionData;
import proposition.ScopeType;
import proposition.attribute.AbstractVariable;
import proposition.attribute.FloatVariable;
import proposition.attribute.IntegerVariable;
import proposition.attribute.StringVariable;

public class LogUtils {

	private LogUtils() {
		//Private constructor to avoid unnecessary instantiation of the class
	}


	public static XLog convertToXlog(String logPath) {
		XLog xlog = null;
		File logFile = new File(logPath);

		if (logFile.getName().toLowerCase().endsWith(".mxml")){
			XMxmlParser parser = new XMxmlParser();
			if(parser.canParse(logFile)){
				try {
					xlog = parser.parse(logFile).get(0);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		} else if (logFile.getName().toLowerCase().endsWith(".xes")){
			XesXmlParser parser = new XesXmlParser();
			if(parser.canParse(logFile)){
				try {
					xlog = parser.parse(logFile).get(0);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		return xlog;
	}

	public static String getEventProposition(XEvent xevent, PropositionData propositionData) {

		String activityName = XConceptExtension.instance().extractName(xevent);
		System.out.println("eventToProposition: " + activityName);

		String modelName = null;
		if (!XSemanticExtension.instance().extractModelReferences(xevent).isEmpty()) {
			modelName = XSemanticExtension.instance().extractModelReferences(xevent).get(0);
		} else {
			System.out.println("\tNo model reference");
		}

		if (propositionData.getActivity(activityName, modelName) == null) {
			System.out.println("\tActivity not found in proposition data, adding as global activity: " + activityName);
			propositionData.addMissingGlobalActivity(activityName);
		}

		List<String> propositionList = new ArrayList<String>();
		Activity activity = propositionData.getActivity(activityName, modelName);
		if (activity.getScopeType() == ScopeType.LOCAL) {
			propositionList.add(propositionData.getModelId(modelName));
		}
		propositionList.add(activity.getId());

		Map<String, AbstractVariable<?>> attributes = activity.getAttributes();

		if (attributes.isEmpty()) {
			System.out.println("\tNo related attributes based on the model");
		}

		for (String attributeId : attributes.keySet()) {
			AbstractVariable<?> attribute = attributes.get(attributeId);
			System.out.println("\tProposition attribute: " + attribute.toString());

			XAttribute xattribute = xevent.getAttributes().get(attribute.getName());
			String proposition = "";

			switch (attribute.getAttributeType()) {
			case FLOAT:
				if (xattribute instanceof XAttributeContinuousImpl) {
					FloatVariable attributeFloat = (FloatVariable) attribute;
					XAttributeContinuousImpl xAttributeContinuousImpl = (XAttributeContinuousImpl) xattribute;
					System.out.println("\tLog attribute: " + xAttributeContinuousImpl.getKey() + "=" + xAttributeContinuousImpl.getValue());
					proposition = attributeFloat.getPropositionName((float) xAttributeContinuousImpl.getValue());
				}
				break;
			case INTEGER:
				if (xattribute instanceof XAttributeDiscreteImpl) {
					IntegerVariable attributeInteger = (IntegerVariable) attribute;
					XAttributeDiscreteImpl xAttributeDiscreteImpl = (XAttributeDiscreteImpl) xattribute;
					System.out.println("\tLog attribute: " + xAttributeDiscreteImpl.getKey() + "=" + xAttributeDiscreteImpl.getValue());
					proposition = attributeInteger.getPropositionName((int) xAttributeDiscreteImpl.getValue());
				}
				break;
			case STRING:
				if (xattribute instanceof XAttributeLiteralImpl) {
					StringVariable attributeString = (StringVariable) attribute;
					XAttributeLiteralImpl xAttributeLiteralImpl = (XAttributeLiteralImpl) xattribute;
					System.out.println("\tLog attribute: " + xAttributeLiteralImpl.getKey() + "=" + xAttributeLiteralImpl.getValue());
					proposition = attributeString.getPropositionName(xAttributeLiteralImpl.getValue());
				}
				break;
			default:
				System.err.println("Possible attribute type mismatch between model and log, using px as the proposition: " + activityName);
				proposition = attribute.getId() + "px";
				break;

			}

			if (proposition.isBlank()) {
				//If the activity-attribute connection is a result of a read guard of a DPN, then proposition will be blank here
				proposition = attribute.getId() + "px";
			}

			propositionList.add(proposition);
			System.out.println("\tAttribute proposition: " + proposition);
		}

		String eventProposition = String.join("", propositionList);
		System.out.println("Event proposition: " + eventProposition);
		System.out.println("");
		return eventProposition;
	}


	public static String getEventProposition(XEvent xevent, PropositionData propositionData, Map<String, String> storedGlobalVariableValues) {
		String activityName = XConceptExtension.instance().extractName(xevent);
		System.out.println("eventToProposition: " + activityName);

		String modelName = null;
		if (!XSemanticExtension.instance().extractModelReferences(xevent).isEmpty()) {
			modelName = XSemanticExtension.instance().extractModelReferences(xevent).get(0);
		} else {
			System.out.println("\tNo model reference");
		}

		if (propositionData.getActivity(activityName, modelName) == null) {
			System.out.println("\tActivity not found in proposition data, adding as global activity: " + activityName);
			propositionData.addMissingGlobalActivity(activityName);
		}

		List<String> propositionList = new ArrayList<String>();
		Activity activity = propositionData.getActivity(activityName, modelName);
		if (activity.getScopeType() == ScopeType.LOCAL) {
			propositionList.add(propositionData.getModelId(modelName));
		}
		propositionList.add(activity.getId());

		Map<String, AbstractVariable<?>> attributes = activity.getAttributes();

		if (attributes.isEmpty()) {
			System.out.println("\tNo related attributes based on the model");
		}

		for (String attributeId : attributes.keySet()) {
			AbstractVariable<?> attribute = attributes.get(attributeId);
			System.out.println("\tProposition attribute: " + attribute.toString());

			XAttribute xattribute = xevent.getAttributes().get(attribute.getName());
			String proposition = "";

			switch (attribute.getAttributeType()) {
			case FLOAT:
				if (xattribute instanceof XAttributeContinuousImpl) {
					FloatVariable attributeFloat = (FloatVariable) attribute;
					XAttributeContinuousImpl xAttributeContinuousImpl = (XAttributeContinuousImpl) xattribute;
					System.out.println("\tLog attribute: " + xAttributeContinuousImpl.getKey() + "=" + xAttributeContinuousImpl.getValue());
					proposition = attributeFloat.getPropositionName((float) xAttributeContinuousImpl.getValue());
				}
				break;
			case INTEGER:
				if (xattribute instanceof XAttributeDiscreteImpl) {
					IntegerVariable attributeInteger = (IntegerVariable) attribute;
					XAttributeDiscreteImpl xAttributeDiscreteImpl = (XAttributeDiscreteImpl) xattribute;
					System.out.println("\tLog attribute: " + xAttributeDiscreteImpl.getKey() + "=" + xAttributeDiscreteImpl.getValue());
					proposition = attributeInteger.getPropositionName((int) xAttributeDiscreteImpl.getValue());
				}
				break;
			case STRING:
				if (xattribute instanceof XAttributeLiteralImpl) {
					StringVariable attributeString = (StringVariable) attribute;
					XAttributeLiteralImpl xAttributeLiteralImpl = (XAttributeLiteralImpl) xattribute;
					System.out.println("\tLog attribute: " + xAttributeLiteralImpl.getKey() + "=" + xAttributeLiteralImpl.getValue());
					proposition = attributeString.getPropositionName(xAttributeLiteralImpl.getValue());
				}
				break;
			default:
				System.err.println("Possible attribute type mismatch between model and log, using px as the proposition: " + activityName);
				proposition = attribute.getId() + "px";
				break;

			}

			if (proposition.isBlank()) {
				//If the activity-attribute connection is a result of a read guard of a DPN, then proposition will be blank here
				proposition = attribute.getId() + "px";
			}


			propositionList.add(proposition);

			if (storedGlobalVariableValues.containsKey(attributeId)) {
				System.out.println("\tAdding global variable value");
				propositionList.add("s" + storedGlobalVariableValues.get(attributeId));
			}

			System.out.println("\tAttribute proposition: " + proposition);


		}

		String eventProposition = String.join("", propositionList);
		System.out.println("Event proposition: " + eventProposition);
		System.out.println("");
		return eventProposition;
	}
}
