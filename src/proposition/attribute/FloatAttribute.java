package proposition.attribute;

import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

public class FloatAttribute extends AbstractAttribute<Float> {

	public FloatAttribute(String name, String id) {
		super(name, id, AttributeType.FLOAT);
	}

	@Override
	public String getPropositionName(Float value) {
		int propositionId = getPropositionIndex(value);
		return getId() + "p" + Integer.toString(propositionId);
	}

	@Override
	public Set<String> getAllPropositionNames() {
		Set<String> propositionNames = new TreeSet<String>();
		propositionNames.add(getId() + "px"); //Used for invalid or missing values

		for (int i = 0; i < constants.size()*2+1; i++) {
			propositionNames.add(getId() + "p" + Integer.toString(i));
		}
		return propositionNames;
	}

	@Override
	public Set<String> getMatchingPropositionNames(String atomicCondition) {
		Set<String> propositionNames = new TreeSet<String>();
		String[] splitEquality = atomicCondition.split("<=|!=|>=|<|=|>");

		if (!splitEquality[0].equals(getName()) ) {
			//Using px for this case might not be the best choice, but if we reach here then something is already wrong
			System.err.println("Condition attribute (" + atomicCondition + ") does not match. Using px as proposition");
			propositionNames.add(getId() + "px");
		} else if (!splitEquality[1].matches("^[+-]?((\\d+\\.?\\d*)|(\\.\\d+))$")) {
			//Using px for this case might not be the best choice, but if we reach here then something is already wrong
			System.err.println("Condition constant (" + atomicCondition + ") is not a float. Using px as proposition");
			propositionNames.add(getId() + "px");
		} else {
			int propositionId = getPropositionIndex(Float.valueOf(splitEquality[1])); //Proposition that matches the constant used in the atomic condition
			
			if (atomicCondition.contains("!=")) { //All propositions except the one used in the atomic condition
				for (int i = 0; i < constants.size()*2+1; i++) {
					if (i != propositionId) {
						propositionNames.add(getId() + "p" + Integer.toString(i));
					}
				}
			} else {
				//Equals code block can occur together with greater/less than code blocks
				if (atomicCondition.contains("=")) { //The proposition used in the atomic condition
					propositionNames.add(getId() + "p" + Integer.toString(propositionId));
				}
				if (atomicCondition.contains("<")) { //The propositions lesser than the constant in the atomic condition
					for (int i = 0; i < constants.size()*2+1; i++) {
						if (i == propositionId) {
							break;
						} else {
							propositionNames.add(getId() + "p" + Integer.toString(i));
						}
					}
				} else if (atomicCondition.contains(">")) { //The propositions greater than the constant in the atomic condition
					for (int i = propositionId+1; i < constants.size()*2+1; i++) {
						propositionNames.add(getId() + "p" + Integer.toString(i));
					}
				}
			}
		}

		if (propositionNames.isEmpty()) {
			System.err.println("Likely invalid condition (" + atomicCondition + ") for string attribute. Using px as proposition");
			propositionNames.add(getId() + "px");
		}

		return propositionNames;
	}

	@Override
	public String getPropositionValue(int propositionId) { //From old implementation, can probably be improved
		if (propositionId == -1) {
			return "!Q";
		}
		
		if (propositionId == 0) {
			return "(-inf," + constants.first() + ")";
		} else if (propositionId == constants.size()*2) {
			return "(" + constants.last() + ",inf)";
		} else {
			boolean singleValueInterval = propositionId%2 != 0;
			
			if (singleValueInterval) {
				int index = 1;
				for (Float conditionCalue : constants) {
					if (index == propositionId) {
						return Float.toString(conditionCalue);
					}
					index=index+2;
				}
			} else {
				Iterator<Float> it = constants.iterator();
				int index = 2;
				while (it.hasNext()) {
					Float conditionValue = it.next();
					if (index == propositionId) {
						return "(" + conditionValue + "," + it.next() + ")";
					}
					index=index+2;
				}
			}
		}
		//propositionId should never be greater than constants.size()*2 so the code should never reach here
		return null;
	}

	private int getPropositionIndex(Float value) {
		//Condition values are processed in ascending order
		//Once we reach a point where the given value is smaller than of equal to the condition value we know the proposition index
		//Each constant, intervals between the constants, and intervals to infinities are considered separate propositions
		//So if the model has conditions x>5.1 and X<=7.5, then this gives propositions (inf,5.1) [5.1,5.1] (5.1,7.5) [7.5,7.5] (7.5,inf)
		//The first condition would match propositions p2, p3, p4 and the second would match p0, p1, p2, p3

		int index=-1;
		if (constants.isEmpty()) {
			//This should never happen because the attribute object should not be created with no constants
			return index;
		}

		for (Float conditionValue : constants) {
			index++;
			if (conditionValue.intValue() > value.intValue()) {
				return index;
			}
			index++;
			if (conditionValue.intValue() == value.intValue()) {
				return index;
			}
		}
		index++;
		return index;
	}

}
