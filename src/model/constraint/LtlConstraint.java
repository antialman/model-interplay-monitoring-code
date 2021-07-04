package model.constraint;

import java.util.List;

public class LtlConstraint {
	String formulaString;
	List<String> dataConditions;
	
	public LtlConstraint(String formulaString, List<String> dataConditions) {
		this.formulaString = formulaString;
		this.dataConditions = dataConditions;
	}
	
	public String getFormulaString() {
		return formulaString;
	}
	
	public List<String> getDataConditions() {
		return dataConditions;
	}

	@Override
	public String toString() {
		return "LtlConstraint [formulaString=" + formulaString + ", dataConditions=" + dataConditions + "]";
	}
	
}
