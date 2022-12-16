package utils;

import java.util.HashSet;
import java.util.Set;

import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.graphbased.directed.petrinetwithdata.newImpl.DataElement;
import org.processmining.models.graphbased.directed.petrinetwithdata.newImpl.PNWDTransition;

import model.DeclareModel;
import model.DpnModel;
import model.LtlModel;
import model.constraint.DeclareConstraint;
import model.constraint.LtlConstraint;
import proposition.Activity;
import proposition.PropositionData;
import proposition.ScopeType;
import proposition.attribute.AbstractVariable;
import proposition.attribute.VariableType;

public class PropositionUtils {

	private PropositionUtils() {
		//Private constructor to avoid unnecessary instantiation of the class
	}


	public static void setVariableConnectionsPropositions(DeclareModel declareModel, PropositionData propositionData) {
		for (DeclareConstraint declareConstraint : declareModel.getDeclareConstraints()) {
			if (declareConstraint.getActivationCondition() != null) {
				String[] atomicConditions = declareConstraint.getActivationCondition().split(" and | or "); //TODO: Should add code for handling parenthesis
				String activityName = declareConstraint.getActivationActivity();
				for (int i = 0; i < atomicConditions.length; i++) {
					//Detecting attribute name, type and the constant used in the atomic condition
					String[] splitCondition = atomicConditions[i].split("<=|!=|>=|<|=|>| is not | is | not in | in ");
					String variableName = splitCondition[0].stripLeading();
					if (variableName.startsWith("A.")) {
						variableName = variableName.substring(2);
					}
					String attributeConstant = splitCondition[1].stripTrailing();
					
					for (Activity activity : propositionData.getActivityInstancesByName(activityName)) {
						for (AbstractVariable<?> abstractVariable : propositionData.getVariableInstancesByName(variableName)) {
							if (activity.getScopeType() == ScopeType.CONSTRAINT && abstractVariable.getScopeType() == ScopeType.CONSTRAINT) {
								activity.addVariable(abstractVariable);
								abstractVariable.addConstant(attributeConstant);
							}
						}
					}
				}
			}
			if (declareConstraint.getTargetCondition() != null) { //Same code as above, but modified for target condition
				String[] atomicConditions = declareConstraint.getTargetCondition().split(" and | or "); //TODO: Should add code for handling parenthesis
				String activityName = declareConstraint.getTargetActivity();
				for (int i = 0; i < atomicConditions.length; i++) {
					//Detecting attribute name, type and the constant used in the atomic condition
					String[] splitCondition = atomicConditions[i].split("<=|!=|>=|<|=|>| is not | is | not in | in ");
					String variableName = splitCondition[0].stripLeading();
					if (variableName.startsWith("T.")) {
						variableName = variableName.substring(2);
					}
					String attributeConstant = splitCondition[1].stripTrailing();
					
					for (Activity activity : propositionData.getActivityInstancesByName(activityName)) {
						for (AbstractVariable<?> abstractVariable : propositionData.getVariableInstancesByName(variableName)) {
							if (activity.getScopeType() == ScopeType.CONSTRAINT && abstractVariable.getScopeType() == ScopeType.CONSTRAINT) {
								activity.addVariable(abstractVariable);
								abstractVariable.addConstant(attributeConstant);	
							}
						}
					}
				}
			}
		}
	}

	public static void setVariableConnectionsPropositions(LtlModel ltlModel, PropositionData propositionData) {
		for (LtlConstraint ltlConstraint : ltlModel.getLtlFormulas()) {
			for (String dataCondition : ltlConstraint.getDataConditions()) {
				//Processing each atomic condition individually
				if (dataCondition != null) {
					String[] atomicConditions = dataCondition.split(" and | or "); //TODO: Should add code for handling parenthesis
					for (int i = 0; i < atomicConditions.length; i++) {
						String[] splitCondition = atomicConditions[i].split("<=|!=|>=|<|=|>| is not | is | not in | in "); //TODO: Should add code for handling parenthesis
						int dotIndex = splitCondition[0].indexOf(".");
						String activityName = splitCondition[0].substring(0, dotIndex);
						String variableName = splitCondition[0].substring(dotIndex+1).strip();
						String attributeConstant = splitCondition[1].stripTrailing();
						
						
						for (Activity activity : propositionData.getActivityInstancesByName(activityName)) {
							for (AbstractVariable<?> abstractVariable : propositionData.getVariableInstancesByName(variableName)) {
								if (activity.getScopeType() == ScopeType.CONSTRAINT && abstractVariable.getScopeType() == ScopeType.CONSTRAINT) {
									activity.addVariable(abstractVariable);
									abstractVariable.addConstant(attributeConstant);
								}
							}
						}
					}
				}
			}
		}
	}

	public static void setVariableConnectionsPropositions(DpnModel dpnModel, PropositionData propositionData) {
		for (Transition transition : dpnModel.getDataPetriNet().getTransitions()) {
			PNWDTransition pnwdTransition = (PNWDTransition) transition;
			if (pnwdTransition.hasGuardExpression() && !pnwdTransition.isInvisible()) { //TODO: Silent transitions

				Set<String> writeVariables = new HashSet<String>();
				for (DataElement writeOperation : pnwdTransition.getWriteOperations()) {
					writeVariables.add(writeOperation.getVarName());
				}
				
				String guardCondition = pnwdTransition.getGuardAsString();
				String[] atomicConditions = guardCondition.split("&&|\\|\\|");
				for (String atomicCondition : atomicConditions) {
					atomicCondition = atomicCondition.replaceAll("^\\(+", "").replaceAll("\\)+$", "").replaceAll("\"", "");
					String[] splitCondition = atomicCondition.split("==|!=|<=|>=|<|>");

					String activityName = transition.getLabel();
					String variableName = splitCondition[0];
					String attributeConstant = splitCondition[1].stripTrailing();
					
					for (Activity activity : propositionData.getActivityInstancesByName(activityName)) {
						for (AbstractVariable<?> abstractVariable : propositionData.getVariableInstancesByName(variableName)) {
							activity.addVariable(abstractVariable); 
							abstractVariable.addConstant(attributeConstant);
							
							if (writeVariables.contains(variableName) && abstractVariable.getScopeType() == ScopeType.GLOBAL) {
								if (activity.getScopeType() != ScopeType.GLOBAL && dpnModel.hasLocalActivity(activity)) {
									propositionData.addLocalActivityToGolbalWriteIDs(activity, abstractVariable.getId());
								} else {
									propositionData.addGlobalActivityToGolbalWriteIDs(dpnModel, activity, abstractVariable.getId());
								}
							}
							
						}
					}
				}
			}
		}
	}







	//Finds all attribute constants from the Declare model and updates the propositionalization data structure
	public static void addAttributePropositions(DeclareModel declareModel, PropositionData propositionData) {
		for (DeclareConstraint declareConstraint : declareModel.getDeclareConstraints()) {
			if (declareConstraint.getActivationCondition() != null) {
				String[] atomicConditions = declareConstraint.getActivationCondition().split(" and | or "); //TODO: Should add code for handling parenthesis
				String activityName = declareConstraint.getActivationActivity();
				for (int i = 0; i < atomicConditions.length; i++) {
					//Detecting attribute name, type and the constant used in the atomic condition
					String[] splitCondition = atomicConditions[i].split("<=|!=|>=|<|=|>| is not | is | not in | in ");
					String attributeName = splitCondition[0];
					if (attributeName.startsWith("A.")) {
						attributeName = attributeName.substring(2);
					}
					VariableType attributeType = declareModel.getVariableTypeMap().get(attributeName);
					String attributeConstant = splitCondition[1].stripTrailing();
					updatePropositionData(activityName, attributeName, attributeType, attributeConstant, propositionData);
				}
			} 
			if (declareConstraint.getTargetCondition() != null) { //Same code as above, but modified for target condition
				String[] atomicConditions = declareConstraint.getTargetCondition().split(" and | or "); //TODO: Should add code for handling parenthesis
				String activityName = declareConstraint.getTargetActivity();
				for (int i = 0; i < atomicConditions.length; i++) {
					//Detecting attribute name, type and the constant used in the atomic condition
					String[] splitCondition = atomicConditions[i].split("<=|!=|>=|<|=|>| is not | is | not in | in ");
					String attributeName = splitCondition[0];
					if (attributeName.startsWith("T.")) {
						attributeName = attributeName.substring(2);
					}
					VariableType attributeType = declareModel.getVariableTypeMap().get(attributeName);
					String attributeConstant = splitCondition[1].stripTrailing();
					updatePropositionData(activityName, attributeName, attributeType, attributeConstant, propositionData);
				}
			}
		}
	}


	//Finds all attribute constants from the LTL model and updates the propositionalization data structure
	public static void addAttributePropositions(LtlModel ltlModel, PropositionData propositionData) {
		for (LtlConstraint ltlConstraint : ltlModel.getLtlFormulas()) {
			for (String dataCondition : ltlConstraint.getDataConditions()) {
				//Processing each atomic condition individually
				if (dataCondition != null) {
					String[] atomicConditions = dataCondition.split(" and | or "); //TODO: Should add code for handling parenthesis
					for (int i = 0; i < atomicConditions.length; i++) {
						String[] splitCondition = atomicConditions[i].split("<=|!=|>=|<|=|>| is not | is | not in | in "); //TODO: Should add code for handling parenthesis
						int dotIndex = splitCondition[0].indexOf(".");
						String activityName = splitCondition[0].substring(0, dotIndex);
						String attributeName = splitCondition[0].substring(dotIndex+1).strip();
						VariableType attributeType = ltlModel.getVariableTypeMap().get(attributeName);
						String attributeConstant = splitCondition[1].stripTrailing();

						//Updating proposition data
						updatePropositionData(activityName, attributeName, attributeType, attributeConstant, propositionData);
					}
				}
			}
		}
	}


	//Finds all attribute constants from the DPN model and updates the propositionalization data structure
	public static void addAttributePropositions(DpnModel dpnModel, PropositionData propositionData) {
		for (Transition transition : dpnModel.getDataPetriNet().getTransitions()) {
			PNWDTransition pnwdTransition = (PNWDTransition) transition;
			if (pnwdTransition.hasGuardExpression() && !pnwdTransition.isInvisible()) { //TODO: Silent transitions
				String guardCondition = pnwdTransition.getGuardAsString();
				String[] atomicConditions = guardCondition.split("&&|\\|\\|");
				for (String atomicCondition : atomicConditions) {
					atomicCondition = atomicCondition.replaceAll("^\\(+", "").replaceAll("\\)+$", "").replaceAll("\"", "");
					String[] splitCondition = atomicCondition.split("==|!=|<=|>=|<|>");

					String activityName = transition.getLabel();
					String attributeName = splitCondition[0];
					VariableType attributeType = dpnModel.getVariableTypeMap().get(attributeName);
					String attributeConstant = splitCondition[1].stripTrailing();

					//Updating proposition data
					updatePropositionData(activityName, attributeName, attributeType, attributeConstant, propositionData);
				}
			}
		}
	}

	//Helper method to update the propositionalization data structure with an attribute constant
	private static void updatePropositionData(String activityName, String attributeName, VariableType attributeType, String attributeConstant, PropositionData propositionData) {
		if (attributeType ==  VariableType.INTEGER) {
			propositionData.addAttributeConstant(activityName, attributeName, Integer.valueOf(attributeConstant));
		} else if (attributeType ==  VariableType.FLOAT) {
			propositionData.addAttributeConstant(activityName, attributeName, Float.valueOf(attributeConstant));
		} else if (attributeType ==  VariableType.STRING) {
			if (attributeConstant.startsWith("(") && attributeConstant.endsWith(")")) {
				String[] values = attributeConstant.substring(1, attributeConstant.length()-1).split(",");
				for (int j = 0; j < values.length; j++) {
					propositionData.addAttributeConstant(activityName, attributeName, values[j]);
				}
			} else {
				propositionData.addAttributeConstant(activityName, attributeName, attributeConstant);
			}
		} else {
			System.out.println("Skipping unhandeled attribute type: " + attributeType);
		}
	}

}
