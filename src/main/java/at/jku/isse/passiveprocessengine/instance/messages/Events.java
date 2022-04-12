package at.jku.isse.passiveprocessengine.instance.messages;

import at.jku.isse.passiveprocessengine.instance.ProcessInstance;
import at.jku.isse.passiveprocessengine.instance.ProcessStep;
import at.jku.isse.passiveprocessengine.instance.StepLifecycle.State;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

public class Events {

	@Data
	@AllArgsConstructor
	public abstract static class ProcessChangedEvent {
		ProcessInstance procScope;
	}
	
	@EqualsAndHashCode(callSuper = true)
	@Data
	public static class DataMappingChangedEvent extends ProcessChangedEvent {
		
		public DataMappingChangedEvent(ProcessInstance proc) {
			super(proc);
		}
	}
	
	@EqualsAndHashCode(callSuper = true)
	@Data
	public static class StepStateTransitionEvent extends ProcessChangedEvent {
		
		ProcessStep step;
		State newState;
		boolean isActualState;
		
		public StepStateTransitionEvent(ProcessInstance proc, ProcessStep step, State newState, boolean isActualState) {
			super(proc);
			this.step = step;
			this.newState = newState;
			this.isActualState = isActualState;
		}
	}
}
