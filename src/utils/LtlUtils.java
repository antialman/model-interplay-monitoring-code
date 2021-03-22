package utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import org.processmining.ltl2automaton.plugins.automaton.Automaton;
import org.processmining.plugins.declareminer.ExecutableAutomaton;

import data.DeclareConstraint;
import data.LtlModel;
import data.PropositionData;
import data.proposition.AttributeType;
import utils.enums.DeclareTemplate;

public class LtlUtils {

	private LtlUtils() {
		//Private constructor to avoid unnecessary instantiation of the class
	}

	public static Map<DeclareConstraint, String> getPropositionalizedLtlFormulaMap(List<DeclareConstraint> declareConstraints, PropositionData propositionData) {
		Map<DeclareConstraint, String> propositionalizedLtlFormulas = new HashMap<DeclareConstraint, String>();
		for (DeclareConstraint declareConstraint : declareConstraints) {
			String ltlFormula = LtlUtils.getPropositionalizedLtlFormula(declareConstraint, propositionData);
			System.out.println("Declare constraint: " + declareConstraint.toString());
			System.out.println("Propositionalized formula: " + ltlFormula);

			propositionalizedLtlFormulas.put(declareConstraint, ltlFormula);
		}
		return propositionalizedLtlFormulas;
	}

	private static String getPropositionalizedLtlFormula(DeclareConstraint declareConstraint, PropositionData propositionData) {
		//This method can be simplified by a lot if it turns out that activation condition can not refer to target activity and vice versa
		String ltlFormula = getGeneralLtlFormula(declareConstraint.getTemplate());
		String condition = declareConstraint.getActivationCondition();

		if (declareConstraint.getTargetCondition() != null) {
			if (condition != null) {
				condition = condition + " and ";
			}
			condition = condition + declareConstraint.getTargetCondition();
		}

		String activationActivityCondition = null;
		if (condition != null) {
			activationActivityCondition = preprocessCondition(condition, "A.", "T.");
		}

		Set<String> activationPropositions;
		if (activationActivityCondition != null) {
			activationPropositions = propositionData.getMatchingPropositions(declareConstraint.getActivationActivity(), activationActivityCondition);
		} else {
			activationPropositions = propositionData.getAllActivityPropositions(declareConstraint.getActivationActivity());
		}

		ltlFormula = ltlFormula.replace("\"A\"", "( " + String.join(" \\/ ", activationPropositions) + " )");

		if (declareConstraint.getTemplate().getIsBinary()) {
			String targetActivityCondition = null;
			if (condition != null) {
				targetActivityCondition = preprocessCondition(condition, "T.", "A.");
			}

			Set<String> targetPropositions;
			if (targetActivityCondition != null) {
				targetPropositions = propositionData.getMatchingPropositions(declareConstraint.getTargetActivity(), targetActivityCondition);
			} else {
				targetPropositions = propositionData.getAllActivityPropositions(declareConstraint.getTargetActivity());
			}

			ltlFormula = ltlFormula.replace("\"B\"", "( " + String.join(" \\/ ", targetPropositions) + " )");
		}

		ltlFormula = ltlFormula.replace("p-1", "px"); //Minus character causes issues with ltl formula parser - doing a simple workaround here

		return ltlFormula;
	}

	private static String preprocessCondition(String condition, String refToKeep, String refToRemove) {
		int tDotIndex = condition.indexOf(refToRemove);

		while (tDotIndex != -1) {
			int aDotIndex = condition.indexOf(refToKeep, tDotIndex);

			if (aDotIndex != -1) {
				condition = condition.substring(0, tDotIndex) + condition.substring(aDotIndex);
			} else {
				condition = condition.substring(0, tDotIndex);
			}
			tDotIndex = condition.indexOf(refToRemove);
		}
		if (condition.endsWith(" and ")) {
			condition = condition.substring(0, condition.length()-5);
		} else if (condition.endsWith(" or ")) {
			condition = condition.substring(0, condition.length()-4);
		}

		if (condition.isBlank()) {
			return null;
		} else {
			return condition;
		}
	}

	private static String getGeneralLtlFormula(DeclareTemplate declareTemplate) {
		String formula = "";
		switch (declareTemplate) {
		case Absence:
			formula = "!( <> ( \"A\" ) )";
			break;
		case Absence2:
			formula = "! ( <> ( ( \"A\" /\\ X(<>(\"A\")) ) ) )";
			break;
		case Absence3:
			formula = "! ( <> ( ( \"A\" /\\  X ( <> ((\"A\" /\\  X ( <> ( \"A\" ) )) ) ) ) ))";
			break;
		case Alternate_Precedence:
			formula = "(((( !(\"B\") U \"A\") \\/ []( !(\"B\"))) /\\ []((\"B\" ->( (!(X(\"A\")) /\\ !(X(!(\"A\"))) ) \\/ X((( !(\"B\") U \"A\") \\/ []( !(\"B\")))))))) /\\ (  ! (\"B\" ) \\/ (!(X(\"A\")) /\\ !(X(!(\"A\"))) ) ))";
			break;
		case Alternate_Response:
			formula = "( []( ( \"A\" -> X(( (! ( \"A\" )) U \"B\" ) )) ) )";
			break;
		case Alternate_Succession:
			formula = "( []((\"A\" -> X(( !(\"A\") U \"B\")))) /\\ (((( !(\"B\") U \"A\") \\/ []( !(\"B\"))) /\\ []((\"B\" ->( (!(X(\"A\")) /\\ !(X(!(\"A\"))) ) \\/ X((( !(\"B\") U \"A\") \\/ []( !(\"B\")))))))) /\\ (  ! (\"B\" ) \\/ (!(X(\"A\")) /\\ !(X(!(\"A\"))) ) )))";
			break;
		case Chain_Precedence:
			formula = "[]( ( X( \"B\" ) -> \"A\") )/\\ (  ! (\"B\" ) \\/ (!(X(\"A\")) /\\ !(X(!(\"A\"))) ) )";
			break;
		case Chain_Response:
			formula = "[] ( ( \"A\" -> X( \"B\" ) ) )";
			break;
		case Chain_Succession:
			formula = "([]( ( \"A\" -> X( \"B\" ) ) )) /\\ ([]( ( X( \"B\" ) ->  \"A\") ) /\\ (  ! (\"B\" ) \\/ (!(X(\"A\")) /\\ !(X(!(\"A\"))) ) ))";
			break;
		case Choice:
			formula = "(  <> ( \"A\" ) \\/ <>( \"B\" )  )";
			break;
		case CoExistence:
			formula = "( ( <>(\"A\") -> <>( \"B\" ) ) /\\ ( <>(\"B\") -> <>( \"A\" ) )  )";
			break;
		case End:
			formula = "(\"A\") && !X (true)";
			break;
		case Exactly1:
			formula = "(  <> (\"A\") /\\ ! ( <> ( ( \"A\" /\\ X(<>(\"A\")) ) ) ) )";
			break;
		case Exactly2:
			formula = "( <> (\"A\" /\\ (\"A\" -> (X(<>(\"A\"))))) /\\  ! ( <>( \"A\" /\\ (\"A\" -> X( <>( \"A\" /\\ (\"A\" -> X ( <> ( \"A\" ) ))) ) ) ) ) )";
			break;
		case Exclusive_Choice:
			formula = "(  ( <>( \"A\" ) \\/ <>( \"B\" )  )  /\\ !( (  <>( \"A\" ) /\\ <>( \"B\" ) ) ) )";
			break;
		case Existence:
			formula = "( <> ( \"A\" ) )";
			break;
		case Existence2:
			formula = "<> ( ( \"A\" /\\ X(<>(\"A\")) ) )";
			break;
		case Existence3:
			formula = "<>( \"A\" /\\ X(  <>( \"A\" /\\ X( <> \"A\" )) ))";
			break;
		case Init:
			formula = "( \"A\" )";
			break;
		case Not_Chain_Precedence:
			formula = "[] ( \"A\" -> !( X ( \"B\" ) ) )";
			break;
		case Not_Chain_Response:
			formula = "[] ( \"A\" -> !( X ( \"B\" ) ) )";
			break;
		case Not_Chain_Succession:
			formula = "[]( ( \"A\" -> !(X( \"B\" ) ) ))";
			break;
		case Not_CoExistence:
			formula = "(<>( \"A\" )) -> (!(<>( \"B\" )))";
			break;
		case Not_Precedence:
			formula = "[] ( \"A\" -> !( <> ( \"B\" ) ) )";
			break;
		case Not_Responded_Existence:
			formula = "(<>( \"A\" )) -> (!(<>( \"B\" )))";
			break;
		case Not_Response:
			formula = "[] ( \"A\" -> !( <> ( \"B\" ) ) )";
			break;
		case Not_Succession:
			formula = "[]( ( \"A\" -> !(<>( \"B\" ) ) ))";
			break;
		case Precedence:
			formula = "( ! (\"B\" ) U \"A\" ) \\/ ([](!(\"B\"))) /\\ (  ! (\"B\" ) \\/ (!(X(\"A\")) /\\ !(X(!(\"A\"))) ) )";
			break;
		case Responded_Existence:
			formula = "(( ( <>( \"A\" ) -> (<>( \"B\" ) )) ))";
			break;
		case Response:
			formula = "( []( ( \"A\" -> <>( \"B\" ) ) ))";
			break;
		case Succession:
			formula = "(( []( ( \"A\" -> <>( \"B\" ) ) ))) /\\ (( ! (\"B\" ) U \"A\" ) \\/ ([](!(\"B\"))) /\\ (  ! (\"B\" ) \\/ (!(X(\"A\")) /\\ !(X(!(\"A\"))) ) )   )";
			break;
		default:
			break;
		}
		return formula;
	}

	public static Map<String, AttributeType> addAttributeTypes(String ltlModelPath, Map<String, AttributeType> attributeTypeMap) throws FileNotFoundException {

		return attributeTypeMap;
	}

	public static List<LtlModel> processLtlModelFile(String ltlModelPath, PropositionData propositionData) throws FileNotFoundException {
		Scanner sc = new Scanner(new File(ltlModelPath));

		Map<String, AttributeType> attributeTypeMap = new HashMap<String, AttributeType>();
		
		List<LtlModel> ltlModels = new ArrayList<LtlModel>();

		while(sc.hasNextLine()) {
			String line = sc.nextLine();
			if (line.startsWith("model ")) {
				String[] splitLine = line.substring(6).split(";");
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
				ltlModels.add(new LtlModel(splitLine[0], cost));
			} else if (line.startsWith("activity ")) {
				propositionData.addActivity(line.substring(9));
			} else if (line.startsWith("attribute ") && line.contains(":")) {
				String[] splitLine = line.substring(10).split(":");
				String[] activityAttribute = splitLine[0].split("\\.");
				if (activityAttribute.length > 1) {
					if (splitLine[1].equals("integer")) {
						propositionData.addAttribute(activityAttribute[0], activityAttribute[1], AttributeType.INTEGER);
						attributeTypeMap.put(activityAttribute[0], AttributeType.INTEGER);
					} else if(splitLine[1].equals("float")) {
						propositionData.addAttribute(activityAttribute[0], activityAttribute[1], AttributeType.FLOAT);
						attributeTypeMap.put(activityAttribute[0], AttributeType.FLOAT);
					} else {
						propositionData.addAttribute(activityAttribute[0], activityAttribute[1], AttributeType.STRING);
						attributeTypeMap.put(activityAttribute[0], AttributeType.STRING);
					}
				} else {
					System.out.println("Skipping attribute definition: " + line);
				}

			} else if (line.startsWith("formula ")) {
				String[] splitLine = line.substring(8).split(";");
				if (splitLine.length > 1) {
					ltlModels.get(ltlModels.size()-1).addformula(splitLine[0], splitLine[1]);

					for (String individualCondition : splitLine[1].split(" and | or ")) {
						String[] splitCondition = individualCondition.split("<|<=|=|!=|>|>=| is | is not | in | not in ");
						String[] activityAttribute = splitCondition[0].split("\\.");
						if (attributeTypeMap.get(activityAttribute[0]) == AttributeType.INTEGER) {
							propositionData.addAttributeValue(activityAttribute[0], activityAttribute[1], Integer.valueOf(splitCondition[1]));
						} else if (attributeTypeMap.get(activityAttribute[0]) == AttributeType.FLOAT) {
							propositionData.addAttributeValue(activityAttribute[0], activityAttribute[1], Float.valueOf(splitCondition[1]));
						} else {
							propositionData.addAttributeValue(activityAttribute[0], activityAttribute[1], splitCondition[1]);
						}
					}

				} else {
					ltlModels.get(ltlModels.size()-1).addformula(splitLine[0], null);
				}
			}
		}

		return ltlModels;
	}

	public static Map<LtlModel, String> getPropositionalizedLtlModelFormulaMap(List<LtlModel> ltlModels, PropositionData propositionData) {
		Map<LtlModel, String> ltlModelFormulaMap = new HashMap<LtlModel, String>();
		if (ltlModels == null) {
			return ltlModelFormulaMap;
		}
		for (LtlModel ltlModel : ltlModels) {
			List<String> propositionalizedFormulas = new ArrayList<String>();
			for (String ltlModelFormula : ltlModel.getFormulas().keySet()) {
				String propositionalizedFormula = ltlModelFormula;
				
				if (ltlModel.getFormulas().get(ltlModelFormula) != null) {
					String[] split = ltlModel.getFormulas().get(ltlModelFormula).split("\\.");
					String activity = split[0];
					String activityCondition = split[1]; //TODO: Support for operators 'and', 'or'
					if (activityCondition.contains(" and ") || activityCondition.contains(" or ")) {
						activityCondition =  activityCondition.substring(0, split[1].indexOf(" and | or "));
					}
					Set<String> activityPropositions = propositionData.getMatchingPropositions(split[0], "A." + activityCondition);
					
					propositionalizedFormula = propositionalizedFormula.replace(activity, "( " + String.join(" \\/ ", activityPropositions) + " )");
					
				}
				
				
				for (String activityName : propositionData.getActivityNames()) {
					propositionalizedFormula = propositionalizedFormula.replace(activityName, "( " + String.join(" \\/ ", propositionData.getAllActivityPropositions(activityName)) + " )");
				}
				
				propositionalizedFormulas.add(propositionalizedFormula.replace("p-1", "px"));
			}
			
			StringBuilder ltlModelFormula = new StringBuilder("(");
			Iterator<String> it = propositionalizedFormulas.iterator();
			while (it.hasNext()) {
				ltlModelFormula.append(it.next());
				if (it.hasNext()) {
					ltlModelFormula.append(")&&(");
				} else {
					ltlModelFormula.append(")");
				}
			}
			
			ltlModelFormulaMap.put(ltlModel, ltlModelFormula.toString());
			
		}
		return ltlModelFormulaMap;
	}
}
