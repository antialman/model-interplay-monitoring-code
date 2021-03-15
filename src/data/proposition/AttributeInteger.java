package data.proposition;

public class AttributeInteger extends AbstractAttribute<Integer> {

	public AttributeInteger(String name, int id) {
		super(name, id, AttributeType.INTEGER);
	}

	@Override
	public int getPropositionId(Integer value) {
		//Condition values are processed in ascending order
		//Once we reach a point where the given value is smaller than of equal to the condition value we know the proposition index
		//Note that each individual conditionValues is considered a separate proposition
		//So if the model has conditions x>5 and X<=7, then this gives propositions (inf,5) [5,5] (5,7) [7,7] (7,inf)
		//The first condition would match propositions 2, 3, 4 and the second would match 0, 1, 2, 3
		
		int index=-1;
		if (conditionValues.isEmpty()) {
			//This should never happen because the attribute object should not be created with no conditionValues
			return index;
		}
		
		for (Integer conditionValue : conditionValues) {
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
