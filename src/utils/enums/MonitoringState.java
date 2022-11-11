package utils.enums;

public enum MonitoringState {
	INIT("ini"), //Initial state of the constraint automata, could probably be removed
	SAT("PS"), //Permanently satisfied
	VIOL("PV"), //Permanently violated
	POSS_SAT("TS"), //Possibly satisfied
	POSS_VIOL("TV"); //Possibly violated
	
	
	private final String mobuconltlName;
	
	private MonitoringState(String mobuconltlName) {
		this.mobuconltlName=mobuconltlName;
	}
	
	public String getMobuconltlName() {
		return mobuconltlName;
	}
	
	@Override
	public String toString() {
		return mobuconltlName;
	}
}
