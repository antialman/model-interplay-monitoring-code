package utils.enums;

public enum ConstraintState {
	INIT, //Initial state of the constraint automata("init"), could probably be removed 
	SAT, //Permanently satisfied ("sat")
	VIOL, //Permanently violated ("viol")
	POSS_SAT, //Possibly satisfied ("poss.sat")
	POSS_VIOL; //Possibly violated ("poss.viol")
}
