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

import data.proposition_old.DeclareConstraint;
import data.proposition_old.PropositionData_old;
import proposition.attribute.AttributeType;
import utils.enums.DeclareTemplate;

public class ModelUtils_old {

	private ModelUtils_old() {
		//Private constructor to avoid unnecessary instantiation of the class
	}

	public static List<DeclareConstraint> readConstraints(String declareModelPath) throws FileNotFoundException {
		List<DeclareConstraint> declareConstraints = new ArrayList<DeclareConstraint>();
		
		Scanner sc = new Scanner(new File(declareModelPath));
		Pattern constraintPattern = Pattern.compile("\\w+(\\[.*\\]) \\|");
		
		while(sc.hasNextLine()) {
			String line = sc.nextLine();
			String[] splitLine = line.split(";");
			String constraintString = splitLine[0];
			
			if(constraintString.startsWith("activity") && constraintString.length() > 9) { //Activities
				//Using constraints to detect the activities
			} else if(constraintString.startsWith("bind") && constraintString.length() > 7 && constraintString.substring(6).contains(":")) {
				//Using constraints to detect activity-attribute relations
			} else {
				Matcher constraintMatcher = constraintPattern.matcher(constraintString);
				if (constraintMatcher.find()) { //Constraints
					Integer cost = Integer.valueOf(0);
					if (splitLine.length>1) {
						try {
							cost = Integer.valueOf(splitLine[1]);
						} catch (NumberFormatException e) {
							System.out.println("Unable to parse cost on line: " + line);
						}
					} else {
						System.out.println("Cost missing on line: " + line);
					}
					DeclareConstraint declareConstraint = readConstraintString(constraintString);
					declareConstraint.setViolationCost(cost);
					declareConstraints.add(declareConstraint);
				}
			}
		}
		
		return declareConstraints;
	}
	
	
	private static DeclareConstraint readConstraintString(String constraintString) {
		DeclareTemplate template = null;
		String activationActivity = "";
		String activationCondition = "";
		String targetActivity = "";
		String targetCondition = "";
		String timeCondition = ""; //Unimplemented at the moment, requires recording the activation activity time and comparing to target activity time on the fly

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
		
		//Removing possible blank conditions
		if (activationCondition.isBlank()) {
			activationCondition = null;
		}
		if (targetCondition.isBlank()) {
			targetCondition = null;
		}
		if (timeCondition.isBlank()) {
			timeCondition = null;
		}
		
		return new DeclareConstraint(constraintString, template, activationActivity, activationCondition, targetActivity, targetCondition, timeCondition);
	}
	
	

	public static Map<String, AttributeType> readAttributeTypes(String declareModelPath) throws FileNotFoundException {
		Map<String, AttributeType> attributeTypeMap = new HashMap<String, AttributeType>();
		
		Scanner sc = new Scanner(new File(declareModelPath));
		Pattern constraintPattern = Pattern.compile("\\w+(\\[.*\\]) \\|");
		Pattern attributeDefPattern =  Pattern.compile("(.+): (.+)");
		
		while(sc.hasNextLine()) {
			String line = sc.nextLine().split(";")[0];
			if(line.startsWith("activity") && line.length() > 9) { //Activities
				//Using constraints to detect the activities
			} else if(line.startsWith("bind") && line.length() > 7 && line.substring(6).contains(":")) {
				//Using constraints to detect activity-attribute relations
			} else {
				Matcher constraintMatcher = constraintPattern.matcher(line);
				Matcher attributeDefMatcher = attributeDefPattern.matcher(line);
				if (constraintMatcher.find()) { 
					//Constraints
				} else if (attributeDefMatcher.find()) {
					//Attribute definitions are used for detecting attribute types
					String[] attributeDefSplit = line.split(": ");
					if (attributeDefSplit[1].startsWith("integer between ")) {
						attributeTypeMap.put(attributeDefSplit[0], AttributeType.INTEGER);
					} else if (attributeDefSplit[1].startsWith("float between ")) {
						attributeTypeMap.put(attributeDefSplit[0], AttributeType.FLOAT);
					} else {
						attributeTypeMap.put(attributeDefSplit[0], AttributeType.STRING);
					}
				}
			}
		}
		return attributeTypeMap;
	}

	public static void updatePropositionData(List<DeclareConstraint> declareConstraints, Map<String, AttributeType> attributeTypeMap, PropositionData_old propositionData) {
		for (DeclareConstraint declareConstraint : declareConstraints) {
			
			propositionData.addActivity(declareConstraint.getActivationActivity());
			if (declareConstraint.getTemplate().getIsBinary()) {
				propositionData.addActivity(declareConstraint.getTargetActivity());
			}
			
			//Processing activation condition
			if (declareConstraint.getActivationCondition() != null) {
				processConstraintCondition(declareConstraint, declareConstraint.getActivationCondition(), attributeTypeMap, propositionData);
			}
			//Processing target condition
			if (declareConstraint.getTargetCondition() != null) {
				processConstraintCondition(declareConstraint, declareConstraint.getTargetCondition(), attributeTypeMap, propositionData);
			}
		}
	}

	private static void processConstraintCondition(DeclareConstraint declareConstraint, String conditionString, Map<String, AttributeType> attributeTypeMap, PropositionData_old propositionData) {
		String[] individualConditions = conditionString.split(" and | or "); //TODO: Should add code for handling parenthesis
		String activityName = "";
		for (int i = 0; i < individualConditions.length; i++) {
			//Detecting the related activity
			if (individualConditions[i].startsWith("A.")) {
				activityName = declareConstraint.getActivationActivity();
			} else if (individualConditions[i].startsWith("T.") && !declareConstraint.getTemplate().getIsBinary()) {
				activityName = declareConstraint.getTargetActivity();
			} else {
				System.out.println("Skipping unhandeled condition: " + individualConditions[i] + " in constraint: " + declareConstraint);
				continue;
			}

			String[] splitCondition = individualConditions[i].split("<|<=|=|!=|>|>=| is | is not | in | not in "); //TODO: Should add code for handling parenthesis
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
