package utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

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

import model.AbstractModel;
import utils.enums.DeclareTemplate;
import utils.enums.MonitoringState;

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
			MonitoringState truthValue = checkTruthValue(executableAutomaton.currentState(), null);
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
	
	private static MonitoringState checkTruthValue(PossibleNodes currentState, Map<State, Integer> costBestMap) {
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
				if (costBestMap == null) {
					if (!currentState.acceptingReachable(state)) {
						newTruthValue = MonitoringState.VIOL;
					}
				} else { //TODO: This can give incorrect results if any input process model has a violation cost of 0
					if (costBestMap.get(state) > 0) {
						newTruthValue = MonitoringState.VIOL;
					}
				}
			}
		}

		return newTruthValue;
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

	public static MonitoringState execPropositionOnAutomaton(String eventProposition, ExecutableAutomaton executableAutomaton, Map<State, Integer> costBestMap) {
		//Each event must be executed at least on the global automata during event log replay
		PossibleNodes currentState = executableAutomaton.next(eventProposition);
		return checkTruthValue(currentState, costBestMap);
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



	

	
	
}
