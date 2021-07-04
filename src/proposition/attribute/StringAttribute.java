package proposition.attribute;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class StringAttribute extends AbstractAttribute<String> {

	public StringAttribute(String name, String id) {
		super(name, id, AttributeType.STRING);
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
		propositionNames.add(getId() + "px"); //Used for invalid or missing values

		for (int i = 0; i < constants.size(); i++) {
			propositionNames.add(getId() + "p" + Integer.toString(i));
		}
		return propositionNames;
	}

	@Override
	public Set<String> getMatchingPropositionNames(String atomicCondition) {
		Set<String> propositionNames = new TreeSet<String>();
		String[] splitEquality = atomicCondition.split(" is not | is | not in | in ");

		if (!splitEquality[0].equals(getName())) {
			//Using px for this case might not be the best choice, but if we reach here then something is already wrong
			System.err.println("Condition attribute (" + atomicCondition + ") does not match. Using px as proposition");
			propositionNames.add(getId() + "px");
		} else {
			if (atomicCondition.contains(" is not ")) {
				propositionNames.add(getId() + "px");
				int index=-1;
				for (String constant : constants) {
					index++;
					if (!constant.equals(splitEquality[1])) {
						propositionNames.add(getId() + "p" + Integer.toString(index));
					}
				}

			} else if (atomicCondition.contains(" is ")) {
				if (constants.contains(splitEquality[1])) {
					propositionNames.add(getPropositionName(splitEquality[1]));
				} else {
					System.err.println("Condition constant (" + atomicCondition + ") not in attribute constants. Skipping");
				}

			} else if (atomicCondition.contains(" not in ")) {
				List<String> equalityValues = Arrays.asList(splitEquality[1].substring(1, splitEquality[1].length()-1).split(","));
				propositionNames.add(getId() + "px");
				int index=-1;
				for (String constant : constants) {
					index++;
					if (!equalityValues.contains(constant)) {
						propositionNames.add(getId() + "p" + Integer.toString(index));
					}
				}

			} else if (atomicCondition.contains(" in ")) {
				List<String> equalityValues = Arrays.asList(splitEquality[1].substring(1, splitEquality[1].length()-1).split(","));
				for (String equalityValue : equalityValues) {
					if (constants.contains(equalityValue.strip())) {
						propositionNames.add(getPropositionName(equalityValue.strip()));
					} else {
						System.err.println("Condition constant (" + atomicCondition + ") " + equalityValue.strip() + " not in attribute constants. Skipping");
					}
				}

			} else {
				System.err.println("Likely invalid condition (" + atomicCondition + ") for string attribute. Using px as proposition");
				propositionNames.add(getId() + "px");
			}
		}
		return propositionNames;
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
