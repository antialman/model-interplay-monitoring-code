package utils;

import java.util.Set;

import data.DeclareConstraint;
import data.PropositionData;

public class LtlUtils {

	private LtlUtils() {
		//Private constructor to avoid unnecessary instantiation of the class
	}
	
	public static String getPropositionalizedLtlFormula(DeclareConstraint declareConstraint, PropositionData propositionData) {
		String ltlFormula=getGeneralLtlFormula(declareConstraint.getTemplate());
		
		Set<String> activationPropositions = propositionData.getAllActivityPropositions(declareConstraint.getActivationActivity());
		ltlFormula = ltlFormula.replace("\"A\"", "( " + String.join(" \\/ ", activationPropositions) + " )");
		
		if (declareConstraint.getTemplate().getIsBinary()) {
			Set<String> targetPropositions = propositionData.getAllActivityPropositions(declareConstraint.getTargetActivity());
			ltlFormula = ltlFormula.replace("\"B\"", "( " + String.join(" \\/ ", targetPropositions) + " )");
		}
		
		return ltlFormula;
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
