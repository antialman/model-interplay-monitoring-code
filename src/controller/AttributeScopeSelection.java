package controller;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;

public class AttributeScopeSelection {
	private String attributeName;
	private IntegerProperty attributeOverlapsCount = new SimpleIntegerProperty();
	private BooleanProperty isGlobalScopeProperty = new SimpleBooleanProperty();
	
	public AttributeScopeSelection (String attributeName) {
		this.attributeName = attributeName;
		this.attributeOverlapsCount.set(0);
		isGlobalScopeProperty.set(true); //We assume global scope by default
	}
	
	public String getAttributeName() {
		return attributeName;
	}
	
	public IntegerProperty attributeOverlapsCountProperty() {
		return this.attributeOverlapsCount;
	}
	public Integer getAttributeOverlapsCount() {
		return attributeOverlapsCount.get();
	}
	public void setAttributeOverlapsCount(Integer attributeOverlapsCount) {
		this.attributeOverlapsCount.set(attributeOverlapsCount);
	}
	
	public BooleanProperty isGlobalScopeProperty() {
		return this.isGlobalScopeProperty;
	}
	public Boolean getIsGlobalScope() {
		return isGlobalScopeProperty.get();
	}
	public void setIsGlobalScope(boolean isGlobalScope) {
		this.isGlobalScopeProperty.set(isGlobalScope);
	}
}
