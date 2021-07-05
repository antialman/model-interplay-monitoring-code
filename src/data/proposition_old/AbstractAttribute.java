package data.proposition_old;

import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import proposition.attribute.AttributeType;

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
	
	public abstract String getPropositionName(T value);
	public abstract Set<String> getAllPropositionNames();
	public abstract Set<String> getMatchingPropositionNames(String singleEquality);
	public abstract String getPropositionValue(int propositionId);
	

	@Override
	public String toString() {
		return "AbstractAttribute [name=" + name + ", id=" + id + ", attributeType=" + attributeType
				+ ", conditionValues=" + conditionValues + "]";
	}
}