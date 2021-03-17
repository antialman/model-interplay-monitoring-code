package utils;

import java.util.Set;

import data.DeclareConstraint;
import data.PropositionData;

public class LtlUtils {

	private LtlUtils() {
		//Private constructor to avoid unnecessary instantiation of the class
	}

	public static String getPropositionalizedLtlFormula(DeclareConstraint declareConstraint, PropositionData propositionData) {
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
}
