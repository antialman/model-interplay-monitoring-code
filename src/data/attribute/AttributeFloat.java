package data.attribute;

public class AttributeFloat extends AbstractAttribute<Float> {

	public AttributeFloat(String name, int id) {
		super(name, id, AttributeType.FLOAT);
	}

	@Override
	public int getPropositionId(Float value) {
		//Condition values are processed in ascending order
		//Once we reach a point where the given value is smaller than of equal to the condition value we know the proposition index
		//Note that each individual conditionValues is considered a separate proposition
		//So if the model has conditions x>5.1 and X<=7.5, then this gives propositions (inf,5.1) [5.1,5.1] (5.1,7.5) [7.5,7.5] (7.5,inf)
		//The first condition would match propositions 2, 3, 4 and the second would match 0, 1, 2, 3

		int index=-1;
		if (conditionValues.isEmpty()) {
			//This should never happen because the attribute object should not be created with no conditionValues
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

}
