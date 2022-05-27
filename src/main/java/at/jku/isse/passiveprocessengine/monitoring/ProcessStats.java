package at.jku.isse.passiveprocessengine.monitoring;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import at.jku.isse.passiveprocessengine.instance.ProcessStep;
import at.jku.isse.passiveprocessengine.instance.StepLifecycle.State;
import lombok.Data;

@Data
public class ProcessStats {
	String processId;
	boolean processCompleted;
	State lastExpectedState;
	State lastActualState;
	transient Map<ProcessStep, ProcessStepStats> perStepStats = new HashMap<>();
	List<ProcessStepStats> stepStats; 	// only used for simpler serialization into json
	
	public ProcessStats(String processId) {
		this.processId = processId;
	
	}
}
