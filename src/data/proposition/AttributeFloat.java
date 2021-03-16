package data.proposition;

import java.util.Set;
import java.util.TreeSet;

public class AttributeFloat extends AbstractAttribute<Float> {

	public AttributeFloat(String name, int id) {
		super(name, id, AttributeType.FLOAT);
	}

	@Override
	public String getPropositionName(Float value) {
		int propositionId = getPropositionId(value);
		return "att" + Integer.toString(getId()) + "p" + Integer.toString(propositionId);
	}

	private int getPropositionId(Float value) {
		//Condition values are processed in ascending order
		//Once we reach a point where the given value is smaller than of equal to the condition value we know the proposition index
		//Note that each individual conditionValues is considered a separate proposition
		//So if the model has conditions x>5.1 and X<=7.5, then this gives propositions (inf,5.1) [5.1,5.1] (5.1,7.5) [7.5,7.5] (7.5,inf)
		//The first condition would match propositions 2, 3, 4 and the second would match 0, 1, 2, 3

		int index=-1;
		if (conditionValues.isEmpty()) {
			return index;
		}

		for (Float conditionValue : conditionValues) {
			index++;
			if (conditionValue.floatValue() > value.floatValue()) {
				return index;
			}
			index++;
			if (conditionValue.floatValue() == value.floatValue()) {
				return index;
			}
		}
		index++;
		return index;
	}

	@Override
	public Set<String> getAllPropositionNames() {
		Set<String> propositionNames = new TreeSet<String>();
		propositionNames.add("att" + Integer.toString(getId()) + "p-1");

		for (int i = 0; i < conditionValues.size()*2+1; i++) {
			propositionNames.add("att" + Integer.toString(getId()) + "p" + Integer.toString(i));
		}
		return propositionNames;
	}

	@Override
	public Set<String> getMatchingPropositionNames(String singleEquality) {
		Set<String> propositionNames = new TreeSet<String>();

		//TODO: Should add code for handling parenthesis
		String[] splitEquality = singleEquality.split("<=|!=|>=|<|=|>");

		if (!splitEquality[0].substring(2).equals(getName()) ) {
			System.err.println("Condition attribute (" + singleEquality + ") does not match. Using p-1 as proposition");
			propositionNames.add("att" + Integer.toString(getId()) + "p-1");
		} else if (!splitEquality[1].matches("^[+-]?((\\d+\\.?\\d*)|(\\.\\d+))$")) {
			System.err.println("Condition (" + singleEquality + ") value " + splitEquality[1] + " is not a float. Using p-1 as proposition");
			propositionNames.add("att" + Integer.toString(getId()) + "p-1");
		} else {
			int propositionId = getPropositionId(Float.valueOf(splitEquality[1]));

			if (singleEquality.contains("!=")) {
				for (int i = 0; i < conditionValues.size()*2+1; i++) {
					if (i != propositionId) {
						propositionNames.add("att" + Integer.toString(getId()) + "p" + Integer.toString(i));
					}
				}
			} else { 
				if (singleEquality.contains("=")) {
					propositionNames.add("att" + Integer.toString(getId()) + "p" + Integer.toString(propositionId));
				}
				if (singleEquality.contains("<")) {
					for (int i = 0; i < conditionValues.size()*2+1; i++) {
						if (i == propositionId) {
							break;
						} else {
							propositionNames.add("att" + Integer.toString(getId()) + "p" + Integer.toString(i));
						}
					}
				} else if (singleEquality.contains(">")) {
					for (int i = propositionId+1; i < conditionValues.size()*2+1; i++) {
						propositionNames.add("att" + Integer.toString(getId()) + "p" + Integer.toString(i));
					}
				}
			}
		}

		if (propositionNames.isEmpty()) {
			System.err.println("Likely invalid equality (" + singleEquality + ") for string attribute. Using p-1 as proposition");
			propositionNames.add("att" + Integer.toString(getId()) + "p-1");
		}

		return propositionNames;
	}

}
