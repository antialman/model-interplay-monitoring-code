package data;

import java.util.HashMap;
import java.util.Map;

public class LtlModel {

	private String name;
	private int violationCost;
	
	private Map<String, String> formulas = new HashMap<String, String>();
	
	public LtlModel(String name, int violationCost) {
		super();
		this.name = name;
		this.violationCost = violationCost;
	}
	
	public String getName() {
		return name;
	}
	
	public int getViolationCost() {
		return violationCost;
	}
	
	public void addformula(String formula, String formulaConditions) {
		formulas.put(formula, formulaConditions);
	}
	public Map<String, String> getFormulas() {
		return formulas;
	}

	@Override
	public String toString() {
		return "LtlModel [name=" + name + ", violationCost=" + violationCost + ", formulas=" + formulas + "]";
	}
}
