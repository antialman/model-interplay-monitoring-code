package model.dpn;

import java.util.Map;

import org.processmining.ltl2automaton.plugins.automaton.State;
import org.processmining.models.semantics.petrinet.Marking;

public class DpnState { //Used only for traversing the DPN
	private Marking dpnMarking;
	private Map<String, String> writtenPropositions;
	private State automatonState;

	public DpnState(Marking dpnMarking, Map<String, String> writtenPropositions) {
		this.dpnMarking = dpnMarking;
		this.writtenPropositions = writtenPropositions;
	}

	public Marking getDpnMarking() {
		return dpnMarking;
	}

	public Map<String, String> getWrittenPropositions() {
		return writtenPropositions;
	}

	public void setAutomatonState(State automatonState) {
		this.automatonState = automatonState;
	}
	public State getAutomatonState() {
		return automatonState;
	}

	@Override
	public String toString() {
		return "DpnState [marking=" + dpnMarking + ", writtenPropositions=" + writtenPropositions + "]";
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DpnState other = (DpnState) obj;
		
		if (this.dpnMarking.equals(other.getDpnMarking()) && this.writtenPropositions.equals(other.getWrittenPropositions())) {
			return true;
		} else {
			return false;
		}
	}
}
