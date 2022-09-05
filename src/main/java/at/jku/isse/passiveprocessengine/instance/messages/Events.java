package at.jku.isse.passiveprocessengine.instance.messages;

import java.time.OffsetDateTime;

import at.jku.isse.passiveprocessengine.instance.ConstraintWrapper;
import at.jku.isse.passiveprocessengine.instance.ProcessInstance;
import at.jku.isse.passiveprocessengine.instance.ProcessStep;
import at.jku.isse.passiveprocessengine.instance.StepLifecycle.State;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

public class Events {

	@Data
	@AllArgsConstructor
	public abstract static class ProcessChangedEvent {		
		ProcessInstance procScope;
		OffsetDateTime timestamp;
	}
	
	@EqualsAndHashCode(callSuper = true)
	@Data
	public static class DataMappingChangedEvent extends ProcessChangedEvent {
		
		public DataMappingChangedEvent(ProcessInstance proc) {
			super(proc, null);
		}
	}
	
	@EqualsAndHashCode(callSuper = true)
	@Data
	public static class QAConstraintFulfillmentChanged extends ProcessChangedEvent {
		final ProcessStep step;
		final ConstraintWrapper qacWrapper;
		
		public QAConstraintFulfillmentChanged(ProcessInstance proc, ProcessStep step, ConstraintWrapper qacWrapper) {
			super(proc, null);
			this.step = step;
			this.qacWrapper = qacWrapper;
		}
	}
	
	@EqualsAndHashCode(callSuper = true)
	@Data
	public static class QAFulfillmentChanged extends ProcessChangedEvent {
		final ProcessStep step;
		final boolean fulfilled;
		
		public QAFulfillmentChanged(ProcessInstance proc, ProcessStep step, boolean fulfilled) {
			super(proc, null);
			this.step = step;
			this.fulfilled = fulfilled;
		}
	}
	
	@EqualsAndHashCode(callSuper = true)
	@Data
	public static class PostconditionFulfillmentChanged extends ProcessChangedEvent {
		final ProcessStep step;
		final boolean fulfilled;
		
		public PostconditionFulfillmentChanged(ProcessInstance proc, ProcessStep step, boolean fulfilled) {
			super(proc, null);
			this.step = step;
			this.fulfilled = fulfilled;
		}
	}
	
	@EqualsAndHashCode(callSuper = true)
	@Data
	public static class StepStateTransitionEvent extends ProcessChangedEvent {
		
		ProcessStep step;
		State oldState;
		State newState;
		boolean isActualState;
		
		public StepStateTransitionEvent(ProcessInstance proc, ProcessStep step, State oldState, State newState, boolean isActualState) {
			super(proc, null);
			this.step = step;
			this.oldState = oldState;
			this.newState = newState;
			this.isActualState = isActualState;
		}
	}
}
