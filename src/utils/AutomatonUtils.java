package utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.processmining.ltl2automaton.plugins.automaton.Automaton;
import org.processmining.ltl2automaton.plugins.automaton.DeterministicAutomaton;
import org.processmining.ltl2automaton.plugins.automaton.State;
import org.processmining.ltl2automaton.plugins.automaton.Transition;
import org.processmining.ltl2automaton.plugins.formula.DefaultParser;
import org.processmining.ltl2automaton.plugins.formula.Formula;
import org.processmining.ltl2automaton.plugins.formula.conjunction.ConjunctionFactory;
import org.processmining.ltl2automaton.plugins.formula.conjunction.ConjunctionTreeLeaf;
import org.processmining.ltl2automaton.plugins.formula.conjunction.ConjunctionTreeNode;
import org.processmining.ltl2automaton.plugins.formula.conjunction.DefaultTreeFactory;
import org.processmining.ltl2automaton.plugins.formula.conjunction.GroupedTreeConjunction;
import org.processmining.ltl2automaton.plugins.formula.conjunction.TreeFactory;
import org.processmining.ltl2automaton.plugins.ltl.SyntaxParserException;
import org.processmining.plugins.declareminer.ExecutableAutomaton;
import org.processmining.plugins.declareminer.PossibleNodes;

import data.proposition_old.DeclareConstraint;
import model.AbstractModel;
import utils.enums.MonitoringState;
import utils.enums.DeclareTemplate;

public class AutomatonUtils {

	private static TreeFactory<ConjunctionTreeNode, ConjunctionTreeLeaf> treeFactory = DefaultTreeFactory.getInstance();
	private static ConjunctionFactory<? extends GroupedTreeConjunction> conjunctionFactory = GroupedTreeConjunction.getFactory(treeFactory);

	private AutomatonUtils() {
		//Private constructor to avoid unnecessary instantiation of the class
	}


	//Creates an automaton for LTL formula
	public static DeterministicAutomaton createAutomatonForLtlFormula(String ltlFormula) throws SyntaxParserException {
		Formula parsedFormula = new DefaultParser(ltlFormula).parse();
		//System.out.println("Parsed formula: " + parsedFormula);
		GroupedTreeConjunction conjunction = conjunctionFactory.instance(parsedFormula);
		return conjunction.getAutomaton().op.determinize().op.complete();
	}

	//Creates a determinized and minimized(!) intersection of automata
	public static DeterministicAutomaton createMinimizedIntersecrtion(List<DeterministicAutomaton> automata) {
		//TODO: Check if the automata library allows to intersect multiple automata in one operation
		DeterministicAutomaton automataIntersection = null;
		for (DeterministicAutomaton automaton : automata) {
			if (automataIntersection == null) {
				automataIntersection = automaton;
			} else {
				automataIntersection = automataIntersection.op.intersect(automaton).op.determinize().op.minimize();
			}
		}
		automataIntersection = automataIntersection.op.determinize().op.complete();
		return automataIntersection;
	}

	public static ExecutableAutomaton createGlobalAutomaton(List<AbstractModel> processModels) {
		DeterministicAutomaton globalAutomaton = null;

		for (AbstractModel processModel : processModels) {
			if (processModel.getAutomaton() != null) {
				if (globalAutomaton == null) {
					globalAutomaton =  new DeterministicAutomaton(processModel.getAutomaton(), processModel.getAutomaton().isCompleted());
				} else {
					globalAutomaton = globalAutomaton.op.intersect(processModel.getAutomaton());
				}
			}
		}
		return new ExecutableAutomaton(globalAutomaton);
	}

	public static Map<State, Map<AbstractModel, MonitoringState>> getGlobalAutomatonColours(List<AbstractModel> processModels, ExecutableAutomaton globalAutomaton) {
		Map<State, Map<AbstractModel, MonitoringState>> globalAutomatonColours = new HashMap<State, Map<AbstractModel,MonitoringState>>();
		boolean visited[] = new boolean[globalAutomaton.stateCount()];

		//Just to make sure the initial states are correct
		for (AbstractModel processModel : processModels) {
			processModel.getExecutableAutomaton().ini();
		}
		globalAutomaton.ini();

		for (State state : globalAutomaton.currentState()) { //There should always be exactly one initial state
			Stack<String> executedPropositions = new Stack<String>();
			colourGlobalAutomatonState(state, visited, processModels, globalAutomatonColours, executedPropositions);
		}

		return globalAutomatonColours;
	}

	private static void colourGlobalAutomatonState(State state, boolean[] visited, List<AbstractModel> processModels, Map<State, Map<AbstractModel, MonitoringState>> globalAutomatonColours, Stack<String> executedPropositions) {
		visited[state.getId()] = true;
		//System.out.println("\tGlobal state colours " + state.toString());
		HashMap<AbstractModel, MonitoringState> globalStateColours = new HashMap<AbstractModel, MonitoringState>();

		for (AbstractModel processModel : processModels) {
			ExecutableAutomaton executableAutomaton = processModel.getExecutableAutomaton();
			if (!executedPropositions.empty()) {
				executableAutomaton.ini();
				for (String executedProposition : executedPropositions) {
					executableAutomaton.next(executedProposition);
				}
			}
			MonitoringState truthValue = checkTruthValue(executableAutomaton.currentState());
			globalStateColours.put(processModel, truthValue);
			//System.out.println("\t\t" + processModel.getModelName() + ": " + truthValue);
		}

		globalAutomatonColours.put(state, globalStateColours);

		for (Transition t : state.getOutput()) {
			if (!visited[t.getTarget().getId()]) {
				executedPropositions.push(t.getPositiveLabel()); //Returns a label even if the transition is negative which is useful in this case
				colourGlobalAutomatonState(t.getTarget(), visited, processModels, globalAutomatonColours, executedPropositions);
				executedPropositions.pop();
			}
		}
	}
	
	public static Map<State, Integer> getCostCurrMap(List<AbstractModel> processModels, ExecutableAutomaton globalAutomaton, Map<State, Map<AbstractModel, MonitoringState>> globalAutomatonColours) {
		Map<State, Integer> costCurrMap = new HashMap<State, Integer>();
		boolean visited[] = new boolean[globalAutomaton.stateCount()];
		globalAutomaton.ini(); //Just to make sure the initial state is correct

		for (State state : globalAutomaton.currentState()) {
			calculateCostCurrForState(state, visited, costCurrMap, globalAutomaton, globalAutomatonColours, processModels);
		}
		return costCurrMap;
	}
	
	private static void calculateCostCurrForState(State state, boolean[] visited, Map<State, Integer> costCurrMap, ExecutableAutomaton globalAutomaton, Map<State, Map<AbstractModel, MonitoringState>> globalAutomatonColours, List<AbstractModel> processModels) {
		visited[state.getId()] = true;
		int costCurr = 0;

		for (AbstractModel processModel : processModels) {
			MonitoringState constraintState = globalAutomatonColours.get(state).get(processModel);
			if (constraintState == MonitoringState.VIOL || constraintState == MonitoringState.POSS_VIOL) {
				costCurr = costCurr + processModel.getViolationCost();
			}
		}
		costCurrMap.put(state, Integer.valueOf(costCurr));
		
		for (Transition t : state.getOutput()) {
			if (!visited[t.getTarget().getId()]) {
				calculateCostCurrForState(t.getTarget(), visited, costCurrMap, globalAutomaton, globalAutomatonColours, processModels);
			}
		}
	}

	public static Map<State, Integer> getCostBestMap(ExecutableAutomaton globalAutomaton, Map<State, Integer> costCurrMap) {

		//Calculating cost_best value for each state
		Map<State, Integer> costBestMap = new HashMap<State, Integer>();
		for (State state : costCurrMap.keySet()) { //Initialising costBestMap
			costBestMap.put(state, costCurrMap.get(state));
		}
		boolean mapChanged = true;
		while (mapChanged) { //Loop until cost_best stabilises for all states
			mapChanged = iterateCostBestValues(costBestMap, globalAutomaton);
		}
		//for (State state : costBestMap.keySet()) { //costCurrMap and costBestMap should have exactly the same keys
		//	System.out.println("\tcost_cur and cost_best for state " + state.toString() + ": " + costCurrMap.get(state) + "->" + costBestMap.get(state));
		//}

		return costBestMap;
	}

	


	public static String getGenericLtlFormula(DeclareTemplate declareTemplate) {
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















	//Old methods

	public static Map<ExecutableAutomaton, DeclareConstraint> createConstraintAutomata(Map<DeclareConstraint, String> ltlFormulaMap) {
		Map<ExecutableAutomaton, DeclareConstraint> constraintAutomatons = new HashMap<ExecutableAutomaton, DeclareConstraint>();
		for (DeclareConstraint declareConstraint : ltlFormulaMap.keySet()) {

			Automaton aut = createAutomatonForLTLFormula_old(ltlFormulaMap.get(declareConstraint));
			constraintAutomatons.put(new ExecutableAutomaton(aut), declareConstraint);
		}
		return constraintAutomatons;
	}

	public static ExecutableAutomaton createGlobalAutomaton(Map<data.proposition_old.DeclareConstraint, String> ltlFormulaMap, Map<data.proposition_old.LtlModel, String> ltlModelFormulaMap) {
		Automaton globalAutomaton = null;

		for (String ltlFormula : ltlFormulaMap.values()) {
			//Creating new automata just in case because op.intersect modifies the automata being intersected 
			Automaton individualAutomaton = createAutomatonForLTLFormula_old(ltlFormula);
			if (globalAutomaton==null && individualAutomaton != null) {
				globalAutomaton = individualAutomaton;
			} else if (individualAutomaton != null) {
				globalAutomaton = globalAutomaton.op.intersect(individualAutomaton);
			}			
		}

		for (String ltlModelFormula : ltlModelFormulaMap.values()) {
			Automaton individualAutomaton = createAutomatonForLTLFormula_old(ltlModelFormula);
			if (globalAutomaton==null && individualAutomaton != null) {
				globalAutomaton = individualAutomaton;
			} else if (individualAutomaton != null) {
				globalAutomaton = globalAutomaton.op.intersect(individualAutomaton);
			}
		}

		if (globalAutomaton != null) {
			globalAutomaton = globalAutomaton.op.determinize().op.complete();
			return new ExecutableAutomaton(globalAutomaton);
		} else {
			return null;
		}
	}

	public static Automaton createAutomatonForLTLFormula_old(String ltlFormula) {
		Automaton aut = null;

		//Parsing the ltl formula
		try {
			Formula parsedFormula = new DefaultParser(ltlFormula).parse();
			System.out.println("Parsed formula: " + parsedFormula);
			GroupedTreeConjunction conjunction = conjunctionFactory.instance(parsedFormula);
			aut = conjunction.getAutomaton();
		} catch (SyntaxParserException e) {
			System.out.println("Unable to parse formula: " + ltlFormula);
			e.printStackTrace();
		}
		System.out.println();

		return aut;
	}



	public static MonitoringState execPropositionOnAutomaton(String eventProposition, ExecutableAutomaton executableAutomaton) {
		//Each event must be executed at least on the global automata during event log replay
		PossibleNodes currentState = executableAutomaton.next(eventProposition);
		return checkTruthValue(currentState);
	}

	private static MonitoringState checkTruthValue(PossibleNodes currentState) {
		MonitoringState newTruthValue;

		if (currentState.isAccepting()) {
			newTruthValue = MonitoringState.POSS_SAT;
			for (State state : currentState) {
				for (Transition t : state.getOutput()) {
					if (t.isAll()) {
						newTruthValue = MonitoringState.SAT;
					}
				}
			}
		} else {
			newTruthValue = MonitoringState.POSS_VIOL;
			for (State state : currentState) {
				if (!currentState.acceptingReachable(state)) {
					newTruthValue = MonitoringState.VIOL;
				}
			}
		}

		return newTruthValue;
	}

	public static Map<State, Map<ExecutableAutomaton, MonitoringState>> getGlobalAutomatonColours(ExecutableAutomaton globalAutomaton, Map<ExecutableAutomaton, data.proposition_old.DeclareConstraint> constraintAutomata, Map<ExecutableAutomaton, data.proposition_old.LtlModel> ltlModelAutomata) {
		Map<State, Map<ExecutableAutomaton, MonitoringState>> globalAutomatonColours = new HashMap<State, Map<ExecutableAutomaton, MonitoringState>>();
		boolean visited[] = new boolean[globalAutomaton.stateCount()];

		//Just to make sure the initial states are correct
		for (ExecutableAutomaton executableAutomaton : constraintAutomata.keySet()) {
			executableAutomaton.ini();
		}
		for (ExecutableAutomaton executableAutomaton : ltlModelAutomata.keySet()) {
			executableAutomaton.ini();
		}
		globalAutomaton.ini();

		for (State state : globalAutomaton.currentState()) {
			Stack<String> executedPropositions = new Stack<String>();
			colourGlobalAutomatonState(state, visited, constraintAutomata, ltlModelAutomata, globalAutomatonColours, executedPropositions);
		}

		return globalAutomatonColours;
	}

	private static void colourGlobalAutomatonState(State state, boolean[] visited, Map<ExecutableAutomaton, data.proposition_old.DeclareConstraint> constraintAutomata, Map<ExecutableAutomaton, data.proposition_old.LtlModel> ltlModelAutomata, Map<State, Map<ExecutableAutomaton, MonitoringState>> globalAutomatonColours, Stack<String> executedPropositions) {
		visited[state.getId()] = true;
		System.out.println("Global state colours " + state.toString());
		HashMap<ExecutableAutomaton, MonitoringState> globalStateColours = new HashMap<ExecutableAutomaton, MonitoringState>();

		for (ExecutableAutomaton executableAutomaton : constraintAutomata.keySet()) {
			if (!executedPropositions.empty()) {
				executableAutomaton.ini();
				for (String executedProposition : executedPropositions) {
					executableAutomaton.next(executedProposition);
				}
			}
			MonitoringState truthValue = checkTruthValue(executableAutomaton.currentState());
			globalStateColours.put(executableAutomaton, truthValue);
			System.out.println("\t" + truthValue + ": " + constraintAutomata.get(executableAutomaton).getConstraintString());
		}
		globalAutomatonColours.put(state, globalStateColours);

		for (ExecutableAutomaton executableAutomaton : ltlModelAutomata.keySet()) {
			if (!executedPropositions.empty()) {
				executableAutomaton.ini();
				for (String executedProposition : executedPropositions) {
					executableAutomaton.next(executedProposition);
				}
			}
			MonitoringState truthValue = checkTruthValue(executableAutomaton.currentState());
			globalStateColours.put(executableAutomaton, truthValue);
			System.out.println("\t" + truthValue + ": " + ltlModelAutomata.get(executableAutomaton).getName());
		}
		globalAutomatonColours.put(state, globalStateColours);


		for (Transition t : state.getOutput()) {
			if (!visited[t.getTarget().getId()]) {
				executedPropositions.push(t.getPositiveLabel()); //Returns a label even if the transition is negative which is useful in this case
				colourGlobalAutomatonState(t.getTarget(), visited, constraintAutomata, ltlModelAutomata, globalAutomatonColours, executedPropositions);
				executedPropositions.pop();
			}
		}
	}

	public static Map<State, Integer> getCostCurrMap(ExecutableAutomaton globalAutomaton, Map<State, Map<ExecutableAutomaton, MonitoringState>> globalAutomatonColours, Map<ExecutableAutomaton, data.proposition_old.DeclareConstraint> constraintAutomata, Map<ExecutableAutomaton, data.proposition_old.LtlModel> ltlModelAutomata) {
		Map<State, Integer> costCurrMap = new HashMap<State, Integer>();
		boolean visited[] = new boolean[globalAutomaton.stateCount()];

		//Just to make sure the initial state is correct
		globalAutomaton.ini();

		for (State state : globalAutomaton.currentState()) {
			calculateCostCurrForState(state, visited, costCurrMap, globalAutomaton, globalAutomatonColours, constraintAutomata, ltlModelAutomata);
		}
		for (State state : costCurrMap.keySet()) {
			System.out.println("cost_cur for state " + state.toString() + ": " + costCurrMap.get(state));
		}
		System.out.println();


		return costCurrMap;
	}

	private static void calculateCostCurrForState(State state, boolean[] visited, Map<State, Integer> costCurrMap, ExecutableAutomaton globalAutomaton, Map<State, Map<ExecutableAutomaton, MonitoringState>> globalAutomatonColours, Map<ExecutableAutomaton, data.proposition_old.DeclareConstraint> constraintAutomata, Map<ExecutableAutomaton, data.proposition_old.LtlModel> ltlModelAutomata) {
		visited[state.getId()] = true;
		int costCurr = 0;

		for (ExecutableAutomaton executableAutomaton : globalAutomatonColours.get(state).keySet()) {
			MonitoringState constraintState = globalAutomatonColours.get(state).get(executableAutomaton);
			if (constraintState == MonitoringState.VIOL || constraintState == MonitoringState.POSS_VIOL) {
				if (constraintAutomata.get(executableAutomaton) != null) {
					costCurr = costCurr + constraintAutomata.get(executableAutomaton).getViolationCost();
				} else {
					costCurr = costCurr + ltlModelAutomata.get(executableAutomaton).getViolationCost();
				}
			}
		}
		costCurrMap.put(state, Integer.valueOf(costCurr));

		for (Transition t : state.getOutput()) {
			if (!visited[t.getTarget().getId()]) {
				calculateCostCurrForState(t.getTarget(), visited, costCurrMap, globalAutomaton, globalAutomatonColours, constraintAutomata, ltlModelAutomata);
			}
		}

	}

	public static Map<State, Integer> getCostBestMap_old(ExecutableAutomaton globalAutomaton, Map<State, Integer> costCurrMap) {
		Map<State, Integer> costBestMap = new HashMap<State, Integer>();

		//Initialising costBestMap
		for (State state : costCurrMap.keySet()) {
			costBestMap.put(state, costCurrMap.get(state));
		}

		//Loop until cost_best stabilises for all states
		boolean mapChanged = true;
		while (mapChanged) {
			System.out.println("Iterating cost_best values");
			mapChanged = iterateCostBestValues(costBestMap, globalAutomaton);
		}

		for (State state : costBestMap.keySet()) {
			System.out.println("cost_best for state " + state.toString() + ": " + costBestMap.get(state));
		}
		System.out.println();

		return costBestMap;
	}

	private static boolean iterateCostBestValues(Map<State, Integer> costBestMap, ExecutableAutomaton globalAutomaton) {
		boolean visited[] = new boolean[globalAutomaton.stateCount()];
		boolean mapChanged = false;
		boolean valueChanged = false;

		//Just to make sure the initial state is correct
		globalAutomaton.ini();

		for (State state : globalAutomaton.currentState()) {
			valueChanged = updateCostBestForState(state, visited, costBestMap, globalAutomaton);
			if (valueChanged && !mapChanged) {
				mapChanged = true;
			}
		}

		return mapChanged;
	}

	private static boolean updateCostBestForState(State state, boolean[] visited, Map<State, Integer> costBestMap, ExecutableAutomaton globalAutomaton) {
		visited[state.getId()] = true;
		boolean valueChanged = false;
		boolean valueChanged2 = false;
		Integer costBest = costBestMap.get(state);

		for (Transition t : state.getOutput()) {
			//If we want to exclude self-loop then we should add a condition t.getTarget() != state
			if (costBest.intValue() > costBestMap.get(t.getTarget()).intValue()) {
				costBest = costBestMap.get(t.getTarget());
				valueChanged = true;
			}
		}
		costBestMap.put(state, costBest);


		for (Transition t : state.getOutput()) {
			if (!visited[t.getTarget().getId()]) {
				valueChanged2 = updateCostBestForState(t.getTarget(), visited, costBestMap, globalAutomaton);
				if (valueChanged2 && !valueChanged) {
					valueChanged = true;
				}
			}
		}


		return valueChanged;
	}

	public static Map<ExecutableAutomaton, data.proposition_old.LtlModel> createLtlModelAutomata(Map<data.proposition_old.LtlModel, String> ltlModelFormulaMap) {
		Map<ExecutableAutomaton, data.proposition_old.LtlModel> ltlModelAutomata = new HashMap<ExecutableAutomaton, data.proposition_old.LtlModel>();

		for (data.proposition_old.LtlModel ltlModel : ltlModelFormulaMap.keySet()) {
			Automaton aut = createAutomatonForLTLFormula_old(ltlModelFormulaMap.get(ltlModel));
			ltlModelAutomata.put(new ExecutableAutomaton(aut), ltlModel);
		}
		return ltlModelAutomata;
	}


	


}
