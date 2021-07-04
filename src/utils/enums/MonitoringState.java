package utils.enums;

public enum MonitoringState {
	INIT("init"), //Initial state of the constraint automata, could probably be removed
	SAT("perm.sat"), //Permanently satisfied
	VIOL("perm.viol"), //Permanently violated
	POSS_SAT("temp.sat"), //Possibly satisfied
	POSS_VIOL("temp.viol"); //Possibly violated
	
	
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
