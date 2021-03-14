package utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import data.PropositionData;
import data.attribute.AttributeType;

public class DeclareModelUtils {

	private DeclareModelUtils() {
		//Private constructor to avoid unnecessary instantiation of the class
	}

	public static void readPropositionData(String declareModelPath, PropositionData propositionData) throws FileNotFoundException {
		List<String> constraintStrings = new ArrayList<String>();
		List<String> attributeDefStrings = new ArrayList<String>();

		Scanner sc = new Scanner(new File(declareModelPath));
		Pattern constraintPattern = Pattern.compile("\\w+(\\[.*\\]) \\|");
		Pattern attributeDefPattern =  Pattern.compile("(.+): (.+)");
		while(sc.hasNextLine()) {
			String line = sc.nextLine();
			if(line.startsWith("activity") && line.length() > 9) { //Activities
				//Using constraints to detect the activities
			} else if(line.startsWith("bind") && line.length() > 7 && line.substring(6).contains(":")) {
				//Using constraints to detect activity-attribute relations
			} else {
				Matcher constraintMatcher = constraintPattern.matcher(line);
				Matcher attributeDefMatcher = attributeDefPattern.matcher(line);
				if (constraintMatcher.find()) { //Constraints
					constraintStrings.add(line);
				} else if (attributeDefMatcher.find()) {
					//Attribute definitions are used for detecting attribute types
					attributeDefStrings.add(line);
				}
			}
		}

		//Detecting attribute types
		Map<String, AttributeType> attributeTypeMap = new HashMap<String, AttributeType>();
		for (String attributeDefString : attributeDefStrings) {
			String[] attributeDefSplit = attributeDefString.split(": ");
			if (attributeDefSplit[1].startsWith("integer between ")) {
				attributeTypeMap.put(attributeDefSplit[0], AttributeType.INTEGER);
			} else if (attributeDefSplit[1].startsWith("float between ")) {
				attributeTypeMap.put(attributeDefSplit[0], AttributeType.FLOAT);
			} else {
				attributeTypeMap.put(attributeDefSplit[0], AttributeType.STRING);
			}
		}
		
		
		//Reading activities, attributes and attribute values from constraints
		for (String constraintString : constraintStrings) {
			DeclareTemplate template = null;
			String activationActivity = "";
			String activationCondition = "";
			String targetActivity = "";
			String targetCondition = "";
			String timeCondition = "";
			
			Matcher mBinary = Pattern.compile("(.*)\\[(.*), (.*)\\] \\|(.*) \\|(.*) \\|(.*)").matcher(constraintString);
			Matcher mUnary = Pattern.compile(".*\\[(.*)\\] \\|(.*) \\|(.*)").matcher(constraintString);
						
			//Processing the constraint
			if(mBinary.find()) { //Binary constraints
				template = DeclareTemplate.getByTemplateName(mBinary.group(1));
				if(template.getReverseActivationTarget()) {
					targetActivity = mBinary.group(2);
					targetCondition = mBinary.group(5);
					activationActivity = mBinary.group(3);
					activationCondition = mBinary.group(4);
					timeCondition = mBinary.group(6);
				}
				else {
					activationActivity = mBinary.group(2);
					activationCondition = mBinary.group(4);
					targetActivity = mBinary.group(3);
					targetCondition = mBinary.group(5);
					timeCondition = mBinary.group(6);
				}
			} else if(mUnary.find()) { //Unary constraints
				template = DeclareTemplate.getByTemplateName(mUnary.group(0).substring(0, mUnary.group(0).indexOf("["))); //TODO: Should be done more intelligently
				activationActivity = mUnary.group(1);
				activationCondition = mUnary.group(2);
				timeCondition = mUnary.group(3);
				
				
			}
			
			
			//Adding activities to propositionData
			propositionData.addActivity(activationActivity);
			if (!targetActivity.equals("")) {
				propositionData.addActivity(activationActivity);
			}
			
			//Processing activation condition
			if (!activationCondition.isBlank()) {
				processConstraintCondition(activationCondition, activationActivity, targetActivity, constraintString, attributeTypeMap, propositionData);
			}
			//Processing target condition
			if (!targetCondition.isBlank()) {
				processConstraintCondition(targetCondition, activationActivity, targetActivity, constraintString, attributeTypeMap, propositionData);
			}
		}
	}
	
	private static void processConstraintCondition(String conditionString, String activationActivity, String targetActivity, String constraintString, Map<String, AttributeType> attributeTypeMap, PropositionData propositionData) {
		String[] individualConditions = conditionString.split(" and | or ");
		String activityName = "";
		for (int i = 0; i < individualConditions.length; i++) {
			//Detecting the related activity
			if (individualConditions[i].startsWith("A.")) {
				activityName = activationActivity;
			} else if (individualConditions[i].startsWith("T.") && !targetActivity.equals("")) {
				activityName = targetActivity;
			} else {
				System.out.println("Skipping unhandeled condition: " + individualConditions[i] + " in constraint: " + constraintString);
				continue;
			}
			
			String[] splitCondition = individualConditions[i].split("<|<=|=|>|>=| is | is not | in | not in ");
			String attributeName = splitCondition[0].substring(2);
			
			if (attributeTypeMap.get(attributeName) ==  AttributeType.INTEGER) {
				propositionData.addAttributeValue(activityName, attributeName, Integer.valueOf(splitCondition[1].stripTrailing()));
			} else if (attributeTypeMap.get(attributeName) ==  AttributeType.FLOAT) {
				propositionData.addAttributeValue(activityName, attributeName, Float.valueOf(splitCondition[1].stripTrailing()));
			} else if (attributeTypeMap.get(attributeName) ==  AttributeType.STRING) {
				if (splitCondition[1].startsWith("(") && splitCondition[1].endsWith(")")) {
					String[] values = splitCondition[1].substring(1, splitCondition[1].length()-1).split(",");
					for (int j = 0; j < values.length; j++) {
						propositionData.addAttributeValue(activityName, attributeName, values[j]);
					}
				} else {
					propositionData.addAttributeValue(activityName, attributeName, splitCondition[1].stripTrailing());
				}
			}
		}
	}
	
	
}
