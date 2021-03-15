package data.proposition;

import java.util.Map;
import java.util.TreeMap;

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
}
