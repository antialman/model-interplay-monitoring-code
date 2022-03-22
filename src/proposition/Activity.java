package proposition;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import proposition.attribute.AbstractAttribute;

public class Activity {
	private String name;
	private String id;

	//For looking up attributes based on attribute id (TreeMap is used to fix the order of attributes in proposition strings)
	private Map<String, AbstractAttribute<?>> attributeIdToAttribute = new TreeMap<String, AbstractAttribute<?>>();

	public Activity(String name, String id) {
		this.name=name;
		this.id=id;
	}

	public String getName() {
		return name;
	}

	public String getId() {
		return id;
	}

	//Adds attribute to the activity if it is not already added
	public void addAttribute(AbstractAttribute<?> abstractAttribute) {
		if (!attributeIdToAttribute.containsKey(abstractAttribute.getId())) {
			attributeIdToAttribute.put(abstractAttribute.getId(), abstractAttribute);
		}
	}

	//Returns all proposition combinations for this activity, each combination string is prefixed with activity id
	public Set<String> getAllPropositionNames() {
		Set<String> allPropositions = new TreeSet<String>();
		allPropositions.add(getId());
		
		for (String attributeId : attributeIdToAttribute.keySet()) {
			Set<String> attributePropositionNames = attributeIdToAttribute.get(attributeId).getAllPropositionNames();
			Set<String> allPropositionsNew = new TreeSet<String>();
			
			for (String existingProposition : allPropositions) {
				for (String attributePropositionName : attributePropositionNames) {
					allPropositionsNew.add(existingProposition + attributePropositionName);
				}
			}
			allPropositions = allPropositionsNew;
		}
		return allPropositions;
	}
	
	//Returns all proposition combinations for this activity that match the given activity condition
	public Set<String> getMatchingPropositionNames(String activityCondition) {
		String[] orOperatorSplit = activityCondition.split(" or "); //TODO: Should add code for handling parenthesis
		Set<String> orResultPropositions = new HashSet<String>();
		
		for (int i = 0; i < orOperatorSplit.length; i++) {
			String[] andOperatorSplit = orOperatorSplit[i].split(" and "); //TODO: Should add code for handling parenthesis
			Set<String> andResultPropositions = new HashSet<String>();
			andResultPropositions.addAll(processAtomicCondition(andOperatorSplit[0]));
			
			for (int j = 1; j < andOperatorSplit.length; j++) {
				andResultPropositions.retainAll(processAtomicCondition(andOperatorSplit[j]));
			}
			orResultPropositions.addAll(andResultPropositions);
		}
		return orResultPropositions;
	}
	
	//Returns all proposition combinations for this activity that match the given atomic condition, each combination string is prefixed with activity id
	private Set<String> processAtomicCondition(String atomicCondition) {
		Set<String> allPropositions = new TreeSet<String>();
		allPropositions.add(getId());
		
		String[] splitCondition = atomicCondition.split("<=|!=|>=|<|=|>| is not | is | not in | in ");
		String conditionAttributeName = splitCondition[0].strip();
		
		for (String attributeId : attributeIdToAttribute.keySet()) {
			Set<String> attributePropositionNames;
			if (attributeIdToAttribute.get(attributeId).getName().equals(conditionAttributeName)) {
				attributePropositionNames = attributeIdToAttribute.get(attributeId).getMatchingPropositionNames(atomicCondition);
			} else {
				attributePropositionNames = attributeIdToAttribute.get(attributeId).getAllPropositionNames();
			}
			
			Set<String> allPropositionsNew = new TreeSet<String>();
			
			for (String existingProposition : allPropositions) {
				for (String attributePropositionName : attributePropositionNames) {
					allPropositionsNew.add(existingProposition + attributePropositionName);
				}
			}
			allPropositions = allPropositionsNew;
		}
		
		return allPropositions;
	}
	
	public Map<String, AbstractAttribute<?>> getAttributes() {
		return attributeIdToAttribute;
	}

	@Override
	public String toString() {
		return "Activity [name=" + name + ", id=" + id + ", attributeIdToAttribute=" + attributeIdToAttribute + "]";
	}

	
	
}
