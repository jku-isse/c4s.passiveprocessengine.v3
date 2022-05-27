package at.jku.isse.passiveprocessengine.monitoring;

import java.time.OffsetDateTime;
import java.util.HashSet;
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
	OffsetDateTime prematureStartRepaired = null; //when have all the preconditions (rules and QA) been fulfilled that this task can actually start	
	Set<String> qaConstraintsFulfilledAtSomeTime = new HashSet<>();
	Set<String> qaConstraintsUnfulfilledAtSomeTime = new HashSet<>();
	Set<String> qaConstraintsUnevaluatedAtSomeTime = new HashSet<>();
	
	Set<String> qaConstraintsFulfilledAtEnd = new HashSet<>();
	Set<String> qaConstraintsUnfulfilledAtEnd = new HashSet<>();
	Set<String> qaConstraintsUnevaluatedAtEnd = new HashSet<>();
	
	transient ProcessStep step;
	
	public ProcessStepStats(ProcessStep step) {
		this.step = step;
		this.stepId = step.getDefinition().getName();
	}
	
	public void calcMidQaFulfillment() { // check for a completion ready step whether any QA constraint is fulfilled or not
		if (!step.arePostCondFulfilled() || step.getActualLifecycleState().equals(State.CANCELED) || step.getActualLifecycleState().equals(State.NO_WORK_EXPECTED))
			return; // but ignore if cancled or no work expected
		
		step.getQAstatus().stream()
		.forEach(check -> {
			if (check.getCr() == null) 				
				qaConstraintsUnevaluatedAtSomeTime.add(check.getQaSpec().getQaConstraintId());			
			if (check.getCr() == null) 
				qaConstraintsUnfulfilledAtSomeTime.add(check.getQaSpec().getQaConstraintId());
			else
				qaConstraintsFulfilledAtSomeTime.add(check.getQaSpec().getQaConstraintId());
		});
	}
	
	public void calcEOPqaFulfillment() {
		//clear any prior/premature calculations first to always reflect the last state
		qaConstraintsUnevaluatedAtEnd.clear();
		qaConstraintsUnfulfilledAtEnd.clear();
		qaConstraintsFulfilledAtEnd.clear();
		step.getQAstatus().stream()
		.forEach(check -> {
			if (check.getCr() == null) 
					qaConstraintsUnevaluatedAtEnd.add(check.getQaSpec().getQaConstraintId());
			if (check.getCr() == null) 
					qaConstraintsUnfulfilledAtEnd.add(check.getQaSpec().getQaConstraintId());
				else
					qaConstraintsFulfilledAtEnd.add(check.getQaSpec().getQaConstraintId());
			});		
		
		exp = step.getExpectedLifecycleState().toString();
		act = step.getActualLifecycleState().toString();
	}
	
}
