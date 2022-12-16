package proposition.attribute;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import proposition.ScopeType;

public class StringVariable extends AbstractVariable<String> {

	public StringVariable(String name, String id, ScopeType scopeType) {
		super(name, id, VariableType.STRING, scopeType);
	}
	
	@Override
	public void addConstant(String attributeConstant) {
		if (attributeConstant.startsWith("(") && attributeConstant.endsWith(")")) {
			String[] values = attributeConstant.substring(1, attributeConstant.length()-1).split(",");
			for (int j = 0; j < values.length; j++) {
				constants.add(values[j]);
			}
		} else {
			constants.add(attributeConstant);
		}
	}

	@Override
	public String getPropositionName(String value) {
		boolean matchFound = false;

		int index=-1;
		for (String constant : constants) {
			index++;
			if (constant.equals(value)) {
				matchFound = true;
				break;
			}
		}

		if (matchFound) {
			//Using index of the value in constants as the proposition id 
			return getId() + "p" + Integer.toString(index);
		} else {
			//If value is not found in constants then using px (note that "is not a" would be satisfied by c even if c is not used in the model at all)
			return getId() + "px";
		}
	}

	@Override
	public Set<String> getAllPropositionNames() {
		Set<String> propositionNames = new TreeSet<String>();
		
		if (this.getScopeType() == ScopeType.GLOBAL) {
			propositionNames.add(getId() + "pxspx");
			
			for (int i = 0; i < constants.size(); i++) {
				propositionNames.addAll(getComplementaryPropositions("p" + Integer.toString(i), true));
				propositionNames.addAll(getComplementaryPropositions("p" + Integer.toString(i), false));
			}
		} else {
			propositionNames.add(getId() + "px"); //Used for invalid or missing values
			
			for (int i = 0; i < constants.size(); i++) {
				propositionNames.add(getId() + "p" + Integer.toString(i));
			}
		}
		return propositionNames;
	}

	@Override
	public Set<String> getMatchingPropositionNames(String atomicCondition, boolean matchGlobalVariableValue) {
		Set<String> propositionNames = new TreeSet<String>();
		String[] splitEquality = atomicCondition.split(" is not | is | not in | in ");

		if (!splitEquality[0].equals(getName())) {
			//Using px for this case might not be the best choice, but if we reach here then something is already wrong
			System.err.println("Condition attribute (" + atomicCondition + ") does not match. Using px as proposition");
			if (this.getScopeType() == ScopeType.GLOBAL) {
				propositionNames.addAll(getComplementaryPropositions("px", !matchGlobalVariableValue));
			} else {
				propositionNames.add(getId() + "px");
			}
		} else {
			if (atomicCondition.contains(" is not ")) {
				if (this.getScopeType() == ScopeType.GLOBAL) {
					propositionNames.addAll(getComplementaryPropositions("px", !matchGlobalVariableValue));
				} else {
					propositionNames.add(getId() + "px");
				}
				
				int index=-1;
				for (String constant : constants) {
					index++;
					if (!constant.equals(splitEquality[1])) {
						if (this.getScopeType() == ScopeType.GLOBAL) {
							propositionNames.addAll(getComplementaryPropositions("p" + Integer.toString(index), !matchGlobalVariableValue));
						} else {
							propositionNames.add(getId() + "p" + Integer.toString(index));
						}
					}
				}

			} else if (atomicCondition.contains(" is ")) {
				if (constants.contains(splitEquality[1])) {
					if (this.getScopeType() == ScopeType.GLOBAL) {
						String propositionName = getPropositionName(splitEquality[1]);
						propositionNames.addAll(getComplementaryPropositions(propositionName.substring(this.getId().length()), !matchGlobalVariableValue));
					} else {
						propositionNames.add(getPropositionName(splitEquality[1]));
					}
				} else {
					System.err.println("Condition constant (" + atomicCondition + ") not in attribute constants. Skipping");
				}

			} else if (atomicCondition.contains(" not in ")) {
				List<String> equalityValues = Arrays.asList(splitEquality[1].substring(1, splitEquality[1].length()-1).split(","));
				if (this.getScopeType() == ScopeType.GLOBAL) {
					propositionNames.addAll(getComplementaryPropositions("px", !matchGlobalVariableValue));
				} else {
					propositionNames.add(getId() + "px");
				}
				int index=-1;
				for (String constant : constants) {
					index++;
					if (!equalityValues.contains(constant)) {
						if (this.getScopeType() == ScopeType.GLOBAL) {
							propositionNames.addAll(getComplementaryPropositions("p" + Integer.toString(index), !matchGlobalVariableValue));
						} else {
							propositionNames.add(getId() + "p" + Integer.toString(index));
						}
					}
				}

			} else if (atomicCondition.contains(" in ")) {
				List<String> equalityValues = Arrays.asList(splitEquality[1].substring(1, splitEquality[1].length()-1).split(","));
				for (String equalityValue : equalityValues) {
					if (constants.contains(equalityValue.strip())) {
						if (this.getScopeType() == ScopeType.GLOBAL) {
							String propositionName = getPropositionName(equalityValue.strip());
							propositionNames.addAll(getComplementaryPropositions(propositionName.substring(this.getId().length()), !matchGlobalVariableValue));
						} else {
							propositionNames.add(getPropositionName(equalityValue.strip()));
						}
					} else {
						System.err.println("Condition constant (" + atomicCondition + ") " + equalityValue.strip() + " not in attribute constants. Skipping");
					}
				}

			} else {
				System.err.println("Likely invalid condition (" + atomicCondition + ") for string attribute. Using px as proposition");
				if (this.getScopeType() == ScopeType.GLOBAL) {
					propositionNames.addAll(getComplementaryPropositions("px", !matchGlobalVariableValue));
				} else {
					propositionNames.add(getId() + "px");
				}
			}
		}
		return propositionNames;
	}
	
	private Set<String> getComplementaryPropositions(String proposition, boolean addGlobalVariableValues) {
		Set<String> complementaryPropositions = new HashSet<String>();
		
		if (addGlobalVariableValues) {
			complementaryPropositions.add(getId() + proposition + "spx");
			for (int i = 0; i < constants.size(); i++) {
				complementaryPropositions.add(getId() + proposition + "sp" + Integer.toString(i));
			}
		} else {
			complementaryPropositions.add(getId() + "pxs" + proposition);
			for (int i = 0; i < constants.size(); i++) {
				complementaryPropositions.add(getId() + "p" + Integer.toString(i) + "s" + proposition);
			}
		}
		
		return complementaryPropositions;
	}

	@Override
	public String getPropositionValue(int propositionId) {
		if (propositionId == -1) {
			return "!(" + String.join(",", constants) + ")";
		} else {
			int index = -1;
			for (String conditionValue : constants) {
				index++;
				if (index == propositionId) {
					return conditionValue;
				}
			}
		}
		//propositionId should never be greater than the number of constants so the code should never reach here
		return null;
	}

}
