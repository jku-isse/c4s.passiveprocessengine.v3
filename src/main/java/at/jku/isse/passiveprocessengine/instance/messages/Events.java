package at.jku.isse.passiveprocessengine.instance.messages;

import java.time.OffsetDateTime;

import at.jku.isse.passiveprocessengine.instance.ConstraintWrapper;
import at.jku.isse.passiveprocessengine.instance.ProcessInstance;
import at.jku.isse.passiveprocessengine.instance.ProcessStep;
import at.jku.isse.passiveprocessengine.instance.StepLifecycle.Conditions;
import at.jku.isse.passiveprocessengine.instance.StepLifecycle.State;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;

public class Events {

	@Data
	@AllArgsConstructor
	public abstract static class ProcessChangedEvent {		
		@NonNull ProcessInstance procScope;
		ProcessStep step;
		OffsetDateTime timestamp;
	}
	
	@EqualsAndHashCode(callSuper = true)
	@Data
	public static class DataMappingChangedEvent extends ProcessChangedEvent {
		
		public DataMappingChangedEvent(ProcessInstance proc) {
			super(proc, null, null);
		}
	}
	
	@EqualsAndHashCode(callSuper = true)
	@Data
	public static class QAConstraintFulfillmentChanged extends ProcessChangedEvent {

		final ConstraintWrapper qacWrapper;
		
		public QAConstraintFulfillmentChanged(ProcessInstance proc, ProcessStep step, ConstraintWrapper qacWrapper) {
			super(proc, step, null);
			this.qacWrapper = qacWrapper;
		}
	}
	
	@EqualsAndHashCode(callSuper = true)
	@Data
	public static class QAFulfillmentChanged extends ProcessChangedEvent {
		final boolean fulfilled;
		
		public QAFulfillmentChanged(ProcessInstance proc, ProcessStep step, boolean fulfilled) {
			super(proc, step, null);
			this.fulfilled = fulfilled;
		}
	}
	
	@EqualsAndHashCode(callSuper = true)
	@Data
	public static class ConditionFulfillmentChanged extends ProcessChangedEvent {
		final boolean fulfilled;
		final Conditions condition;
		
		public ConditionFulfillmentChanged(ProcessInstance proc, ProcessStep step, Conditions condition, boolean fulfilled) {
			super(proc, step, null);
			this.condition = condition;
			this.fulfilled = fulfilled;
		}
	}
	
	@EqualsAndHashCode(callSuper = true)
	@Data
	public static class PartialConditionFulfillmentChanged extends ConditionFulfillmentChanged {
		final String partialConditionName;
		
		public PartialConditionFulfillmentChanged(ProcessInstance proc, ProcessStep step, Conditions condition, boolean fulfilled, String partialConditionName) {
			super(proc, step, condition, fulfilled);			
			this.partialConditionName = partialConditionName;
		}
	}
	
	@EqualsAndHashCode(callSuper = true)
	@Data
	public static class StepStateTransitionEvent extends ProcessChangedEvent {
		
		State oldState;
		State newState;
		boolean isActualState;
		
		public StepStateTransitionEvent(ProcessInstance proc, ProcessStep step, State oldState, State newState, boolean isActualState) {
			super(proc, step, null);
			this.oldState = oldState;
			this.newState = newState;
			this.isActualState = isActualState;
		}
	}
	
	@EqualsAndHashCode(callSuper = true)
	@Data
	public static class ConstraintOverrideEvent extends QAConstraintFulfillmentChanged {
				
		final String reason;		
		final boolean isUndo;
		
		public ConstraintOverrideEvent(ProcessInstance proc, ProcessStep step, ConstraintWrapper qacWrapper, String reason, boolean isUndo) {
			super(proc, step, qacWrapper);
			this.reason = reason;			
			this.isUndo = isUndo;
			
		}
	}
	
	
}
