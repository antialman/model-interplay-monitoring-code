package model.constraint;

import utils.enums.DeclareTemplate;

public class DeclareConstraint {
	private String constraintString;
	private DeclareTemplate template;
	private String activationActivity;
	private String activationCondition;
	private String targetActivity;
	private String targetCondition;
	private String timeCondition;
	
	public DeclareConstraint(String constraintString, DeclareTemplate template, String activationActivity,
			String activationCondition, String targetActivity, String targetCondition, String timeCondition) {
		super();
		this.constraintString = constraintString;
		this.template = template;
		this.activationActivity = activationActivity;
		this.activationCondition = activationCondition;
		this.targetActivity = targetActivity;
		this.targetCondition = targetCondition;
		this.timeCondition = timeCondition;
	}

	public String getConstraintString() {
		return constraintString;
	}
	
	public DeclareTemplate getTemplate() {
		return template;
	}

	public String getActivationActivity() {
		return activationActivity;
	}

	public String getActivationCondition() {
		return activationCondition;
	}

	public String getTargetActivity() {
		return targetActivity;
	}

	public String getTargetCondition() {
		return targetCondition;
	}

	public String getTimeCondition() {
		return timeCondition;
	}

	@Override
	public String toString() {
		return "DeclareConstraint [constraintString=" + constraintString + ", template=" + template
				+ ", activationActivity=" + activationActivity + ", activationCondition=" + activationCondition
				+ ", targetActivity=" + targetActivity + ", targetCondition=" + targetCondition + ", timeCondition="
				+ timeCondition + "]";
	}
}
