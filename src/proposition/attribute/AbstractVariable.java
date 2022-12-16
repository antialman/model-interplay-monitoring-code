package proposition.attribute;

import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import proposition.ScopeType;

public abstract class AbstractVariable<T> {
	private String name;
	private String id;
	private VariableType attributeType;
	private ScopeType scopeType;
	
	protected SortedSet<T> constants = new TreeSet<T>(); //Using a sorted set because index of the constant is used in the proposition name
	
	public AbstractVariable(String name, String id, VariableType attributeType, ScopeType scopeType) {
		this.name=name;
		this.id=id;
		this.attributeType=attributeType;
		this.scopeType = scopeType;
	}
	
	public String getName() {
		return name;
	}
	
	public String getId() {
		return id;
	}
	
	public VariableType getAttributeType() {
		return attributeType;
	}
	
	public ScopeType getScopeType() {
		return scopeType;
	}
	
	public abstract void addConstant(String attributeConstant); //Converts and adds the given value to the list constants 
	
	public abstract String getPropositionName(T value); //Gets the attribute proposition that matches the given value
	public abstract Set<String> getAllPropositionNames(); //Gets all propositions used for this attribute
	public abstract Set<String> getMatchingPropositionNames(String atomicCondition, boolean matchGlobalVariableValue); //Gets the attribute propositions that match the given atomic condition
	public abstract String getPropositionValue(int propositionId); //Gets the value (range) that matches the given propositionId

	@Override
	public String toString() {
		return "AbstractAttribute [name=" + name + ", id=" + id + ", attributeType=" + attributeType
				+ ", constants=" + constants + "]";
	}
}
