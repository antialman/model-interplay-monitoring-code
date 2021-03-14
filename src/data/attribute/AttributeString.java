package data.attribute;

public class AttributeString extends AbstractAttribute<String> {

	public AttributeString(String name, int id) {
		super(name, id, AttributeType.STRING);
	}

	@Override
	public int getPropositionId(String value) {
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
			return index;
		} else {
			//If value is not found in conditionValues then using -1 as the proposition id
			return -1;
		}
	}

}
