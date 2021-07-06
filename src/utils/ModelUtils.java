package utils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.processmining.datapetrinets.DataPetriNetsWithMarkings;
import org.processmining.datapetrinets.io.DPNIOException;
import org.processmining.datapetrinets.io.DataPetriNetImporter;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.graphbased.directed.petrinetwithdata.newImpl.DataElement;
import org.processmining.models.graphbased.directed.petrinetwithdata.newImpl.PNWDTransition;

import model.AbstractModel;
import model.DeclareModel;
import model.DpnModel;
import model.LtlModel;
import model.constraint.DeclareConstraint;
import model.constraint.LtlConstraint;
import proposition.attribute.AttributeType;
import utils.enums.DeclareTemplate;

public class ModelUtils {

	private ModelUtils() {
		//Private constructor to avoid unnecessary instantiation of the class
	}


	//Uses the costs file to load the input process models
	public static List<AbstractModel> loadProcessModels(String costsFilePath) {
		List<AbstractModel> processModels = new ArrayList<AbstractModel>();
		Scanner sc = null;
		try {
			File costsFile = new File(costsFilePath);
			String parentPath = costsFile.getAbsoluteFile().getParent();
			sc = new Scanner(costsFile);

			while(sc.hasNextLine()) {
				//Should be made more reliable for filenames that contain dots and/or commas
				String line = sc.nextLine();
				String[] splitLine = line.split(",");
				String modelName = splitLine[0];
				Integer violationCost = Integer.valueOf(splitLine[1]);
				Path modelPath = Paths.get(parentPath, modelName);
				try {
					String modelExtension = modelName.substring(modelName.lastIndexOf(".")+1);
					if ("decl".equalsIgnoreCase(modelExtension)) {
						processModels.add(loadDeclareModel(modelPath, modelName, violationCost));
					} else if ("ltl".equalsIgnoreCase(modelExtension)) {
						processModels.add(loadLtlModel(modelPath, modelName, violationCost));
					} else if ("pnml".equalsIgnoreCase(modelExtension)) {
						processModels.add(loadDpnModel(modelPath, modelName, violationCost));
					} else {
						System.err.println("Skipping model of unknown type: " + modelExtension);
					}
				} catch (DPNIOException | IOException | IndexOutOfBoundsException e) {
					System.err.println("Unable to load the model specified on line: " + line);
					e.printStackTrace();
				}
			}

		} catch (Exception e) {
			System.err.println("Error parsing the costs file");
			e.printStackTrace();
		} finally {
			if (sc != null) {
				sc.close();
			}
		}

		return processModels;
	}




	//Handles loading of a Declare model
	public static DeclareModel loadDeclareModel(Path modelPath, String modelName, int violationCost) throws IOException {
		Set<String> activityNames = createActivityNamesSet(modelPath);
		List<DeclareConstraint> declareConstrains = readConstraints(modelPath);		
		Map<String, AttributeType> attributeTypeMap = createAttributeTypeMap(modelPath);

		DeclareModel declareModel = new DeclareModel(modelName, violationCost, activityNames, attributeTypeMap, declareConstrains);
		return declareModel;
	}

	//Finds constraint strings in the Declare model and creates a list of Declare constraint objects
	private static List<DeclareConstraint> readConstraints(Path declareModelPath) throws IOException {
		List<DeclareConstraint> declareConstraints = new ArrayList<DeclareConstraint>();

		Scanner sc = new Scanner(declareModelPath);
		Pattern constraintPattern = Pattern.compile("\\w+(\\[.*\\]) \\|");

		while(sc.hasNextLine()) {
			String line = sc.nextLine();

			if(line.startsWith("activity") && line.length() > 9) {
				//Skipping activity definitions
			} else if(line.startsWith("bind") && line.length() > 7 && line.substring(6).contains(":")) {
				//Skipping activity-attribute bindings
			} else {
				Matcher constraintMatcher = constraintPattern.matcher(line);
				if (constraintMatcher.find()) { //Constraints
					DeclareConstraint declareConstraint = readConstraintString(line);
					declareConstraints.add(declareConstraint);
				}
			}
		}
		sc.close();

		return declareConstraints;
	}

	//Creates a single Declare constraint object from Declare constraint string
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



	//Handles loading of an LTL model
	public static LtlModel loadLtlModel(Path modelPath, String modelName, Integer violationCost) throws IOException {
		Set<String> activityNames = createActivityNamesSet(modelPath);
		List<LtlConstraint> ltlFormulas = readLtlFormulas(modelPath);
		Map<String, AttributeType> attributeTypeMap = createAttributeTypeMap(modelPath);
		LtlModel ltlModel = new LtlModel(modelName, violationCost, activityNames, attributeTypeMap, ltlFormulas);

		return ltlModel;
	}

	//Creates a single LTL constraint object from LTL constraint string (note that LTL formula and conditions are separated by | symbol)
	private static List<LtlConstraint> readLtlFormulas(Path ltlModelPath) throws IOException {
		List<LtlConstraint> ltlFormulas = new ArrayList<LtlConstraint>();

		Scanner sc = new Scanner(ltlModelPath);

		while(sc.hasNextLine()) {
			String line = sc.nextLine();

			if(line.startsWith("activity") && line.length() > 9) {
				//Skipping activity definitions
			} else if(line.startsWith("bind") && line.length() > 7 && line.substring(6).contains(":")) {
				//Skipping activity-attribute bindings
			} else {
				if (line.contains("|")) { //Constraints
					String[] splitLine = line.split("\\|");
					String formulaString = splitLine[0].strip();
					List<String> dataConditions = new ArrayList<String>();
					if (splitLine.length>1) {
						for (int i = 1; i < splitLine.length; i++) {
							if (!splitLine[i].isBlank()) {
								dataConditions.add(splitLine[i].strip());
							}
						}
					}
					LtlConstraint ltlFormula = new LtlConstraint(formulaString, dataConditions);
					ltlFormulas.add(ltlFormula);
				}
			}
		}
		sc.close();

		return ltlFormulas;
	}


	//Creates attribute type map based on Declare or LTL model
	private static Map<String, AttributeType> createAttributeTypeMap(Path modelPath) throws IOException {
		Map<String, AttributeType> attributeTypeMap = new HashMap<String, AttributeType>();
		Scanner sc = new Scanner(modelPath);
		Pattern constraintPattern = Pattern.compile("\\w+(\\[.*\\]) \\|");
		Pattern attributeDefPattern =  Pattern.compile("(.+): (.+)");

		while(sc.hasNextLine()) {
			String line = sc.nextLine();
			Matcher constraintMatcher = constraintPattern.matcher(line);
			Matcher attributeDefMatcher = attributeDefPattern.matcher(line);
			if ((line.startsWith("activity") && line.length() > 9) || 
					(line.startsWith("bind") && line.length() > 7 && line.substring(6).contains(":")) ||
					constraintMatcher.find()) {
				//Skipping activity definitions, bindings and constraints
				continue;
			} else if (attributeDefMatcher.find()) {
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
		sc.close();
		return attributeTypeMap;
	}

	//Creates activity names set based on Declare or LTL model
	private static Set<String> createActivityNamesSet(Path modelPath) throws IOException {
		Set<String> activityNames = new HashSet<String>();

		Scanner sc = new Scanner(modelPath);
		while(sc.hasNextLine()) {
			String line = sc.nextLine();
			if(line.startsWith("activity ") && line.length() > 9) {
				activityNames.add(line.substring(9));
			}
		}
		sc.close();

		return activityNames;
	}



	//Handles loading of a DPN model
	public static DpnModel loadDpnModel(Path modelPath, String modelName, Integer violationCost) throws FileNotFoundException, DPNIOException {
		DataPetriNetImporter dataPetriNetImporter = new DataPetriNetImporter();
		InputStream inputStream = new BufferedInputStream(new FileInputStream(modelPath.toString()));
		DataPetriNetsWithMarkings dataPetriNet = dataPetriNetImporter.importFromStream(inputStream).getDPN();
		Set<String> activityNames = createActivityNamesSet(dataPetriNet);
		Map<String, AttributeType> attributeTypeMap = createAttributeTypeMap(dataPetriNet);

		DpnModel dpnModel = new DpnModel(modelName, violationCost, activityNames, attributeTypeMap, dataPetriNet);
		return dpnModel;
	}

	//Creates attribute type map based on DPN model
	private static Map<String, AttributeType> createAttributeTypeMap(DataPetriNetsWithMarkings dataPetriNet) {
		Map<String, AttributeType> attributeTypeMap = new HashMap<String, AttributeType>();
		for (DataElement dataElement : dataPetriNet.getVariables()) {
			String varName = dataElement.getVarName();
			Class<?> typeClass = dataElement.getType();
			if (typeClass == Long.class) {
				attributeTypeMap.put(varName, AttributeType.INTEGER);
			} else if (typeClass == Double.class) {
				attributeTypeMap.put(varName, AttributeType.FLOAT);
			} else {
				attributeTypeMap.put(varName, AttributeType.STRING);
			}

		}
		return attributeTypeMap;
	}

	//Creates activity names set based on DPN model
	private static Set<String> createActivityNamesSet(DataPetriNetsWithMarkings dataPetriNet) {
		Set<String> activityNames = new HashSet<String>();
		for (Transition transition : dataPetriNet.getTransitions()) {
			if (!transition.isInvisible()) {
				activityNames.add(transition.getLabel());
			}
		}
		return activityNames;
	}


	/*
	 * 
	 */















	//To be removed

	public static Map<String, List<String>> getAttributeConstants(AbstractModel processModel) {
		Map<String, List<String>> attributeConstantsMap;

		if (processModel instanceof DeclareModel) {
			attributeConstantsMap = getAttributeConstants((DeclareModel)processModel);
		} else if (processModel instanceof LtlModel) {
			attributeConstantsMap = getAttributeConstants((LtlModel)processModel);
		} else if (processModel instanceof DpnModel) {
			attributeConstantsMap = getAttributeConstants((DpnModel)processModel);
		} else {
			attributeConstantsMap = new HashMap<String, List<String>>();
			System.err.println("Skipping unhandled model type: " + processModel.getClass().toGenericString());
		}
		return attributeConstantsMap;
	}

	private static Map<String, List<String>> getAttributeConstants(DeclareModel processModel) {
		Map<String, List<String>> attributeConstantsMap = new HashMap<String, List<String>>();
		for (DeclareConstraint declareConstraint : processModel.getDeclareConstraints()) {
			if (declareConstraint.getActivationCondition() != null) {
				getAttributeConstantsDeclare(declareConstraint.getActivationCondition(), attributeConstantsMap);
			}
			if (declareConstraint.getTargetCondition() != null) {
				getAttributeConstantsDeclare(declareConstraint.getTargetCondition(), attributeConstantsMap);
			}
		}
		return attributeConstantsMap;
	}

	//Modifies attributeConstantsMap
	private static void getAttributeConstantsDeclare(String conditionString, Map<String, List<String>> attributeConstantsMap) {
		String[] individualConditions = conditionString.split(" and | or "); //TODO: Should add code for handling parenthesis
		for (String individualCondition : individualConditions) {
			if (individualCondition.startsWith("A.") || individualCondition.startsWith("T.")) {
				individualCondition = individualCondition.substring(2);
			}
			String[] splitCondition = individualCondition.split("<=|!=|>=|<|=|>| is not | is | not in | in "); //TODO: Should add code for handling parenthesis'
			if (!attributeConstantsMap.containsKey(splitCondition[0])) {
				attributeConstantsMap.put(splitCondition[0], new ArrayList<String>());
			}
			if (splitCondition[1].startsWith("(") && splitCondition[1].endsWith(")")) {
				for (String constant : splitCondition[1].substring(1, splitCondition[1].length()-1).split(",")) {
					attributeConstantsMap.get(splitCondition[0]).add(constant);
				}
			} else {
				attributeConstantsMap.get(splitCondition[0]).add(splitCondition[1]);
			}
		}
	}

	private static Map<String, List<String>> getAttributeConstants(LtlModel processModel) {
		Map<String, List<String>> attributeConstantsMap = new HashMap<String, List<String>>();
		for (LtlConstraint ltlFormula : processModel.getLtlFormulas()) {
				getAttributeConstantsLtl(ltlFormula.getDataConditions(), attributeConstantsMap);
		}
		return attributeConstantsMap;
	}

	//Modifies attributeConstantsMap
	private static void getAttributeConstantsLtl(List<String> dataConditions, Map<String, List<String>> attributeConstantsMap) {
		for (String dataCondition : dataConditions) {
			String[] individualConditions = dataCondition.split(" and | or "); //TODO: Should add code for handling parenthesis
			for (String individualCondition : individualConditions) {
				individualCondition = individualCondition.substring(individualCondition.indexOf(".")+1); //Attributes should always be preceded by "activity."
				
				String[] splitCondition = individualCondition.split("<=|!=|>=|<|=|>| is not | is | not in | in "); //TODO: Should add code for handling parenthesis
				if (!attributeConstantsMap.containsKey(splitCondition[0])) {
					attributeConstantsMap.put(splitCondition[0], new ArrayList<String>());
				}
				if (splitCondition[1].startsWith("(") && splitCondition[1].endsWith(")")) {
					for (String constant : splitCondition[1].substring(1, splitCondition[1].length()-1).split(",")) {
						attributeConstantsMap.get(splitCondition[0]).add(constant);
					}
				} else {
					attributeConstantsMap.get(splitCondition[0]).add(splitCondition[1]);
				}
			}
		}
	}

	private static Map<String, List<String>> getAttributeConstants(DpnModel processModel) {
		Map<String, List<String>> attributeConstantsMap = new HashMap<String, List<String>>();
		for (Transition transition : processModel.getDataPetriNet().getTransitions()) {
			PNWDTransition pnwdTransition = (PNWDTransition) transition;
			if (pnwdTransition.hasGuardExpression()) {
				String guardCondition = pnwdTransition.getGuardAsString();
				String[] individualConditions = guardCondition.split("&&|\\|\\|"); //TODO: Should add code for handling parenthesis
				for (String individualCondition : individualConditions) {
					individualCondition = individualCondition.replaceAll("^\\(+", "").replaceAll("\\)+$", "");
					String[] splitCondition = individualCondition.split("==|!=|<=|>=|<|>"); //TODO: Should add code for handling parenthesis
					if (!attributeConstantsMap.containsKey(splitCondition[0])) {
						attributeConstantsMap.put(splitCondition[0], new ArrayList<String>());
					}
					attributeConstantsMap.get(splitCondition[0]).add(splitCondition[1]);
				}
			}
		}
		return attributeConstantsMap;
	}
}
