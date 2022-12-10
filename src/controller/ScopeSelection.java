package controller;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;

public class ScopeSelection {
	private String itemName;
	private IntegerProperty overlapsCount = new SimpleIntegerProperty();
	private BooleanProperty isGlobalScopeProperty = new SimpleBooleanProperty();
	
	public ScopeSelection (String itemName) {
		this.itemName = itemName;
		this.overlapsCount.set(0);
		isGlobalScopeProperty.set(true); //We assume global scope by default
	}
	
	public String getAttributeName() {
		return itemName;
	}
	
	public IntegerProperty verlapsCountProperty() {
		return this.overlapsCount;
	}
	public Integer getOverlapsCount() {
		return overlapsCount.get();
	}
	public void setOverlapsCount(Integer overlapsCount) {
		this.overlapsCount.set(overlapsCount);
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
