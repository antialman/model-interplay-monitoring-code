package data.proposition;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public class Activity {
	private String name;
	private int id;
	
	private Map<String, AbstractAttribute<?>> attributes = new TreeMap<String, AbstractAttribute<?>>();

	public Activity(String name, int id) {
		this.name=name;
		this.id=id;
	}
	
	public String getName() {
		return name;
	}
	
	public int getId() {
		return id;
	}
	
	public Map<String, AbstractAttribute<?>> getAttributes() {
		return attributes;
	}
	
	public Set<String> getAllPropositions() {
		Set<String> allPropositions = new TreeSet<String>();
		allPropositions.add("act" + Integer.toString(getId()));
		
		for (String attributeName : attributes.keySet()) {
			Set<String> attributePropositionNames = attributes.get(attributeName).getAllPropositionNames();
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
	
	private Set<String> getAllPropositionsOfSingleCondition(String singleCondition) {
		Set<String> allPropositions = new TreeSet<String>();
		allPropositions.add("act" + Integer.toString(getId()));
		
		String[] splitCondition = singleCondition.split("<|<=|=|!=|>|>=| is | is not | in | not in "); //TODO: Should add code for handling parenthesis
		String conditionAttributeName = splitCondition[0].substring(2);
		
		for (String attributeName : attributes.keySet()) {
			Set<String> attributePropositionNames;
			if (attributeName.equals(conditionAttributeName)) {
				attributePropositionNames = attributes.get(attributeName).getMatchingPropositionNames(singleCondition);
			} else {
				attributePropositionNames = attributes.get(attributeName).getAllPropositionNames();
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
	
	public Set<String> getMatchingPropositionNames(String activityCondition) {
		String[] orOperatorSplit = activityCondition.split(" or "); //TODO: Should add code for handling parenthesis
		Set<String> orResultPropositions = new HashSet<String>();
		
		for (int i = 0; i < orOperatorSplit.length; i++) {
			String[] andOperatorSplit = orOperatorSplit[i].split(" and "); //TODO: Should add code for handling parenthesis
			Set<String> andResultPropositions = new HashSet<String>();
			andResultPropositions.addAll(getAllPropositionsOfSingleCondition(andOperatorSplit[0]));
			
			for (int j = 1; j < andOperatorSplit.length; j++) {
				andResultPropositions.retainAll(getAllPropositionsOfSingleCondition(andOperatorSplit[j]));
			}
			orResultPropositions.addAll(andResultPropositions);
		}
		return orResultPropositions;
	}

	@Override
	public String toString() {
		return "Activity [name=" + name + ", id=" + id + ", attributes=" + attributes + "]";
	}
}
