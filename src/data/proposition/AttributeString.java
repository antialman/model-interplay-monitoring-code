package data.proposition;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class AttributeString extends AbstractAttribute<String> {

	public AttributeString(String name, int id) {
		super(name, id, AttributeType.STRING);
	}

	@Override
	public String getPropositionName(String value) {
		boolean matchFound = false;
		
		int index=-1;
		for (String conditionValue : conditionValues) {
			index++;
			if (conditionValue.equals(value)) {
				matchFound = true;
				break;
			}
		}
		
		if (matchFound) {
			//Using index of the value in conditionValues as the proposition id 
			return "att" + Integer.toString(getId()) + "p" + Integer.toString(index);
		} else {
			//If value is not found in conditionValues then using -1 as the proposition id
			return "att" + Integer.toString(getId()) + "p-1";
		}
	}
	
	@Override
	public Set<String> getAllPropositionNames() {
		Set<String> propositionNames = new TreeSet<String>();
		propositionNames.add("att" + Integer.toString(getId()) + "p-1");
		
		for (int i = 0; i < conditionValues.size(); i++) {
			propositionNames.add("att" + Integer.toString(getId()) + "p" + Integer.toString(i));
		}
		return propositionNames;
	}

	@Override
	public Set<String> getMatchingPropositionNames(String singleEquality) {
		Set<String> propositionNames = new TreeSet<String>();
		
		//TODO: Should add code for handling parenthesis
		String[] splitEquality = singleEquality.split(" is not | is | not in | in ");
		
		if (!splitEquality[0].substring(2).equals(getName())) {
			System.err.println("Condition attribute (" + singleEquality + ") does not match. Using p-1 as proposition");
			propositionNames.add("att" + Integer.toString(getId()) + "p-1");
		} else {
			if (singleEquality.contains(" is not ")) {
				propositionNames.add("att" + Integer.toString(getId()) + "p-1");
				int index=-1;
				for (String conditionValue : conditionValues) {
					index++;
					if (!conditionValue.equals(splitEquality[1])) {
						propositionNames.add("att" + Integer.toString(getId()) + "p" + Integer.toString(index));
					}
				}
				
			} else if (singleEquality.contains(" is ")) {
				if (conditionValues.contains(splitEquality[1])) {
					propositionNames.add(getPropositionName(splitEquality[1]));
				} else {
					System.err.println("Equality (" + singleEquality + ") value not in conditionValues. Skipping");
				}
				
			} else if (singleEquality.contains(" not in ")) {
				List<String> equalityValues = Arrays.asList(splitEquality[1].substring(1, splitEquality[1].length()-1).split(","));
				propositionNames.add("att" + Integer.toString(getId()) + "p-1");
				int index=-1;
				for (String conditionValue : conditionValues) {
					index++;
					if (!equalityValues.contains(conditionValue)) {
						propositionNames.add("att" + Integer.toString(getId()) + "p" + Integer.toString(index));
					}
				}
				
			} else if (singleEquality.contains(" in ")) {
				List<String> equalityValues = Arrays.asList(splitEquality[1].substring(1, splitEquality[1].length()-1).split(","));
				for (String equalityValue : equalityValues) {
					if (conditionValues.contains(equalityValue.strip())) {
						propositionNames.add(getPropositionName(equalityValue.strip()));
					} else {
						System.err.println("Equality (" + singleEquality + ") value " + equalityValue.strip() + " not in conditionValues. Skipping");
					}
				}
				
			} else {
				System.err.println("Likely invalid equality (" + singleEquality + ") for string attribute. Using p-1 as proposition");
				propositionNames.add("att" + Integer.toString(getId()) + "p-1");
			}
		}
		
		return propositionNames;
	}

}
