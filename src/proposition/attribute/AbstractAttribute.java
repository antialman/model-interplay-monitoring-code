package proposition.attribute;

import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

public abstract class AbstractAttribute<T> {
	private String name;
	private String id;
	private AttributeType attributeType;
	
	protected SortedSet<T> constants = new TreeSet<T>(); //Using a sorted set because index of the constant is used in the proposition name
	
	public AbstractAttribute(String name, String id, AttributeType attributeType) {
		this.name=name;
		this.id=id;
		this.attributeType=attributeType;
	}
	
	public String getName() {
		return name;
	}
	
	public String getId() {
		return id;
	}
	
	public AttributeType getAttributeType() {
		return attributeType;
	}
	
	public void addConstant(T value) {
		constants.add(value);
	};
	
	public abstract String getPropositionName(T value); //Gets the attribute proposition that matches the given value
	public abstract Set<String> getAllPropositionNames(); //Gets all propositions used for this attribute
	public abstract Set<String> getMatchingPropositionNames(String atomicCondition); //Gets the attribute propositions that match the given atomic condition
	public abstract String getPropositionValue(int propositionId); //Gets the value (range) that matches the given propositionId
	

	@Override
	public String toString() {
		return "AbstractAttribute [name=" + name + ", id=" + id + ", attributeType=" + attributeType
				+ ", constants=" + constants + "]";
	}
}
