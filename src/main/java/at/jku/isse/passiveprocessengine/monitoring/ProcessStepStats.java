package at.jku.isse.passiveprocessengine.monitoring;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import at.jku.isse.passiveprocessengine.instance.ProcessStep;
import at.jku.isse.passiveprocessengine.instance.StepLifecycle.State;
import lombok.Data;

@Data
public class ProcessStepStats {

	String stepId; // actually the step type
	String exp;
	String act;
	public int countCancelled = 0; // cancelled from any state
	public int countReactivated = 0; // completed but reopened
	public int countRevoked = 0; // activate or enabled but preconditions revoked
	OffsetDateTime prematurelyStarted = null; //when started
	//transient OffsetDateTime prematureStartRepaired = null; //when have all the preconditions (rules and QA) been fulfilled that this task can actually start	
	OffsetDateTime unsafeStarted = null; //when started
	//transient OffsetDateTime unsafeStartRepaired = null; //when have all the preconditions (rules and QA) been fulfilled that this task can actually start	
	Set<String> qaConstraintsFulfilledAtSomeTime = new HashSet<>();
	Set<String> qaConstraintsUnfulfilledAtSomeTime = new HashSet<>();
	Set<String> qaConstraintsUnevaluatedAtSomeTime = new HashSet<>();
	Set<String> qaConstraintsFulfilledAtStepCompletion = new HashSet<>();
	Set<String> qaConstraintsUnfulfilledAtStepCompletion = new HashSet<>();
	Set<String> qaConstraintsUnevaluatedAtStepCompletion = new HashSet<>();
	
	Set<String> qaConstraintsFulfilledAtEnd = new HashSet<>();
	Set<String> qaConstraintsUnfulfilledAtEnd = new HashSet<>();
	Set<String> qaConstraintsUnevaluatedAtEnd = new HashSet<>();
	
	Map<OffsetDateTime, OffsetDateTime> prematureIntervals = new HashMap<>();
	Map<OffsetDateTime, OffsetDateTime> unsafeIntervals = new HashMap<>();
	transient ProcessStep step;
	
	public ProcessStepStats(ProcessStep step) {
		this.step = step;
		this.stepId = step.getDefinition().getName();
	}
	
	public void calcMidQaFulfillment() { 
		
		step.getQAstatus().stream()
		.forEach(check -> {
			if (check.getCr() == null) 				
				qaConstraintsUnevaluatedAtSomeTime.add(check.getSpec().getConstraintId());			
			if (!check.getCr().isConsistent()) 
				qaConstraintsUnfulfilledAtSomeTime.add(check.getSpec().getConstraintId());
			else
				qaConstraintsFulfilledAtSomeTime.add(check.getSpec().getConstraintId());
		});
		// check for a completion ready step whether any QA constraint is fulfilled or not
		if (!step.arePostCondFulfilled() || step.getActualLifecycleState().equals(State.CANCELED) || step.getActualLifecycleState().equals(State.NO_WORK_EXPECTED)) {
			return; // but ignore if canceled or no work expected
		}
		step.getQAstatus().stream()
		.forEach(check -> {
			if (check.getCr() == null) 				
				qaConstraintsUnevaluatedAtStepCompletion.add(check.getSpec().getConstraintId());			
			if (!check.getCr().isConsistent()) 
				qaConstraintsUnfulfilledAtStepCompletion.add(check.getSpec().getConstraintId());
			else
				qaConstraintsFulfilledAtStepCompletion.add(check.getSpec().getConstraintId());
		});
	}
	
	
	
	protected void setUnsafeStarted(OffsetDateTime odt) {
		if (unsafeStarted == null)
			unsafeStarted = odt;
	}
	
	protected void setUnsafeStartRepaired(OffsetDateTime odt) {
		if (unsafeStarted != null) { // we have an interval
			unsafeIntervals.put(unsafeStarted, odt);
			unsafeStarted = null;
		} else {
			// occurs in rare cases when two prior steps are fixed at the same time, both with signal the repair, we only process the first signal
		}
	}
	
	protected void setPrematurelyStarted(OffsetDateTime odt) {
		if (prematurelyStarted == null)
			prematurelyStarted = odt;
	}
	
	protected void setPrematureStartRepaired(OffsetDateTime odt) {
		if (prematurelyStarted != null) { // we have an interval
			prematureIntervals.put(prematurelyStarted, odt);
			prematurelyStarted = null;
		} else {
			// occurs in rare cases when two prior steps are fixed at the same time, both with signal the repair, we only process the first signal
		}
			
	}
	
	public void calcEOPqaFulfillment() {
		//clear any prior/premature calculations first to always reflect the last state
		qaConstraintsUnevaluatedAtEnd.clear();
		qaConstraintsUnfulfilledAtEnd.clear();
		qaConstraintsFulfilledAtEnd.clear();
		step.getQAstatus().stream()
		.forEach(check -> {
			if (check.getCr() == null) 
					qaConstraintsUnevaluatedAtEnd.add(check.getSpec().getConstraintId());
			else if (!check.getCr().isConsistent()) 
					qaConstraintsUnfulfilledAtEnd.add(check.getSpec().getConstraintId());
				else
					qaConstraintsFulfilledAtEnd.add(check.getSpec().getConstraintId());
			});		
		
		exp = step.getExpectedLifecycleState().toString();
		act = step.getActualLifecycleState().toString();
	}
	
}
