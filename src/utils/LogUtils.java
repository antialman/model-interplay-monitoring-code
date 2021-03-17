package utils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.in.XMxmlParser;
import org.deckfour.xes.in.XesXmlParser;
import org.deckfour.xes.model.XAttribute;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.impl.XAttributeContinuousImpl;
import org.deckfour.xes.model.impl.XAttributeDiscreteImpl;
import org.deckfour.xes.model.impl.XAttributeLiteralImpl;

import data.PropositionData;
import data.proposition.AbstractAttribute;
import data.proposition.Activity;
import data.proposition.AttributeFloat;
import data.proposition.AttributeInteger;
import data.proposition.AttributeString;

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

		if (propositionData.getActivity(activityName) == null) {
			System.out.println("\tActivity not found in proposition data, adding: " + activityName);
			propositionData.addActivity(activityName);
		}

		List<String> propositionList = new ArrayList<String>();
		Activity activity = propositionData.getActivity(activityName);
		propositionList.add("act");
		propositionList.add(Integer.toString(activity.getId()));

		Map<String, AbstractAttribute<?>> attributes = activity.getAttributes();

		if (attributes.isEmpty()) {
			System.out.println("\tNo related attributes based on the models");
		}

		for (String attributeName : attributes.keySet()) {
			propositionList.add("_");
			AbstractAttribute<?> attribute = attributes.get(attributeName);
			System.out.println("\tProposition attribute: " + attribute.toString());

			XAttribute xattribute = xevent.getAttributes().get(attributeName);
			String proposition = "";

			switch (attribute.getAttributeType()) {
			case FLOAT:
				if (xattribute instanceof XAttributeContinuousImpl) {
					AttributeFloat attributeFloat = (AttributeFloat) attribute;
					XAttributeContinuousImpl xAttributeContinuousImpl = (XAttributeContinuousImpl) xattribute;
					System.out.println("\tLog attribute: " + xAttributeContinuousImpl.getKey() + "=" + xAttributeContinuousImpl.getValue());
					proposition = attributeFloat.getPropositionName((float) xAttributeContinuousImpl.getValue());
				}
				break;
			case INTEGER:
				if (xattribute instanceof XAttributeDiscreteImpl) {
					AttributeInteger attributeInteger = (AttributeInteger) attribute;
					XAttributeDiscreteImpl xAttributeDiscreteImpl = (XAttributeDiscreteImpl) xattribute;
					System.out.println("\tLog attribute: " + xAttributeDiscreteImpl.getKey() + "=" + xAttributeDiscreteImpl.getValue());
					proposition = attributeInteger.getPropositionName((int) xAttributeDiscreteImpl.getValue());
				}
				break;
			case STRING:
				if (xattribute instanceof XAttributeLiteralImpl) {
					AttributeString attributeString = (AttributeString) attribute;
					XAttributeLiteralImpl xAttributeLiteralImpl = (XAttributeLiteralImpl) xattribute;
					System.out.println("\tLog attribute: " + xAttributeLiteralImpl.getKey() + "=" + xAttributeLiteralImpl.getValue());
					proposition = attributeString.getPropositionName(xAttributeLiteralImpl.getValue());
				}
				break;
			default:
				break;

			}

			if (proposition.isBlank()) {
				System.err.println("Possible attribute type mismatch between model and log, using p-1 as the proposition");
				proposition = "att" + Integer.toString(attribute.getId()) + "p-1";
			}

			
			proposition = proposition.replace("p-1", "px"); //Minus character causes issues with ltl formula parser - doing a simple workaround here
			
			propositionList.add(proposition);
			System.out.println("\tAttribute proposition: " + proposition);
		}

		String eventProposition = String.join("", propositionList);
		System.out.println("Event proposition: " + eventProposition);
		System.out.println("");
		return eventProposition;
	}
}
