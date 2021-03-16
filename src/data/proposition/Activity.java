package data.proposition;

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

	@Override
	public String toString() {
		return "Activity [name=" + name + ", id=" + id + ", attributes=" + attributes + "]";
	}
}
