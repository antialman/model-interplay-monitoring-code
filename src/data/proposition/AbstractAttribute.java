package data.proposition;

import java.util.SortedSet;
import java.util.TreeSet;

public abstract class AbstractAttribute<T> {
	private String name;
	private int id;
	private AttributeType attributeType;
	
	protected SortedSet<T> conditionValues = new TreeSet<T>();
	
	public AbstractAttribute(String name, int id, AttributeType attributeType) {
		this.name=name;
		this.id=id;
		this.attributeType=attributeType;
	}
	
	public String getName() {
		return name;
	}
	
	public int getId() {
		return id;
	}
	
	public AttributeType getAttributeType() {
		return attributeType;
	}
	
	public void addConditionValue(T value) {
		conditionValues.add(value);
	};
	
	public abstract int getPropositionId(T value);

	@Override
	public String toString() {
		return "AbstractAttribute [name=" + name + ", id=" + id + ", attributeType=" + attributeType
				+ ", conditionValues=" + conditionValues + "]";
	}
}
