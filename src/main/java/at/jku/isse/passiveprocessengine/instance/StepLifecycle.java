package at.jku.isse.passiveprocessengine.instance;

import com.github.oxo42.stateless4j.StateMachine;
import com.github.oxo42.stateless4j.StateMachineConfig;
import com.github.oxo42.stateless4j.delegates.Func2;
import com.github.oxo42.stateless4j.triggers.TriggerWithParameters1;

public class StepLifecycle {
	
	public static enum Conditions { PRECONDITION, POSTCONDITION, ACTIVATION, CANCELATION, DATAMAPPING, QA }
	
	// see as inspiration: https://docs.camunda.org/manual/7.4/reference/cmmn11/concepts/lifecycle/#task-stage-lifecycle
	public static enum State {
		AVAILABLE, // not used yet
		ENABLED, // input sufficient and preconditions fulfilled to be worked on, thus we would recommend this task, respectively expect to oberve work on it
		ACTIVE, // explicitly set by user after recommendation, or observed work output
		NO_WORK_EXPECTED, // user or rule/logic/etc set the the task to NOWORKEXPECTED, should not be executed/recommended, we might still see input for this
		CANCELED, //To signal that conditions say step not needed
		COMPLETED, // output and post conditions fulfilled
		SUPERSTATE_ENDED
	}
	
	public static enum InputState {
		INPUT_UNKNOWN, // not yet checked
		INPUT_MISSING, // no input available so far,
		INPUT_PARTIAL, // no enough input available so far
		INPUT_SUFFICIENT // all required input available
	}
	
	public static enum OutputState {
		OUTPUT_UNKNOWN, // not yet checked
		OUTPUT_MISSING, // no output available so far,
		OUTPUT_PARTIAL, // no enough output available so far
		OUTPUT_SUFFICIENT // all required output available
	}
	
	public static enum Trigger {
		ENABLE,
		ACTIVATE,
		CANCEL,
		UNCANCEL,
		HALT,
		UNHALT,
		RESET,
		MARK_COMPLETE,
	//	ACTIVATE_DEVIATING,
	//	MARK_COMPLETE_DEVIATING,
	//	ACTIVATE_REPAIR,
	//	MARK_COMPLETE_REPAIR
	}
	
	private static StateMachineConfig<State, Trigger> smcExpected;
	private static StateMachineConfig<State, Trigger> smcActual;
	
	public static TriggerWithParameters1<ProcessStep, Trigger> uncancel = new TriggerWithParameters1<>(Trigger.UNCANCEL, ProcessStep.class);
	public static TriggerWithParameters1<ProcessStep, Trigger> unhalt= new TriggerWithParameters1<>(Trigger.UNHALT, ProcessStep.class);
	
	
	public static StateMachineConfig<State, Trigger> getExpectedStateMachineConfig() {
		if (smcExpected == null) {
			// https://github.com/oxo42/stateless4j
			smcExpected = new StateMachineConfig<>();
			smcExpected.configure(State.AVAILABLE)
				.permit(Trigger.ENABLE, State.ENABLED)
				.permit(Trigger.HALT, State.NO_WORK_EXPECTED)
				.permit(Trigger.CANCEL, State.CANCELED);
			smcExpected.configure(State.ENABLED)
				.permit(Trigger.ACTIVATE, State.ACTIVE)
				.permit(Trigger.RESET, State.AVAILABLE)
				.permit(Trigger.MARK_COMPLETE, State.COMPLETED)
				.permit(Trigger.HALT, State.NO_WORK_EXPECTED)
				.permit(Trigger.CANCEL, State.CANCELED);
			smcExpected.configure(State.ACTIVE)
				.permit(Trigger.MARK_COMPLETE, State.COMPLETED)
//				.permit(Triggers.ENABLE, State.ENABLED) makes no sense to go back to ENABLED once we were active
//				.permit(Triggers.RESET, State.AVAILABLE)
				.permit(Trigger.HALT, State.NO_WORK_EXPECTED)
				.permit(Trigger.CANCEL, State.CANCELED);
			smcExpected.configure(State.COMPLETED)
			.permit(Trigger.CANCEL, State.CANCELED) // if we worked in good faith on an XOR branch that later needs to be ignored, we should cancel any completed step to signal that we explicitly wont use the results of that step
			.substateOf(State.SUPERSTATE_ENDED);
			
			smcExpected.configure(State.NO_WORK_EXPECTED)
				.permitDynamic(unhalt, new Func2<ProcessStep, State>() {
					@Override
					public State call(ProcessStep step) {						
						if (step.areCancelCondFulfilled())
							return State.CANCELED; 
						else if (!step.arePreCondFulfilled())
							return State.AVAILABLE;
						else if (step.arePostCondFulfilled() && step.arePreCondFulfilled())
							return State.COMPLETED;
						else if (step.getActualLifecycleState().equals(State.ACTIVE))
							return State.ACTIVE;
						else 
							return State.ENABLED;
					}
				})
//				.permit( Trigger.RESET, State.AVAILABLE)
//				.permit(Trigger.ENABLE, State.ENABLED)
//				.permit(Trigger.CANCEL, State.CANCELED)
//				.permit(Trigger.ACTIVATE_REPAIR, State.ACTIVE)
//				.permit(Trigger.MARK_COMPLETE_REPAIR, State.COMPLETED)
				.substateOf(State.SUPERSTATE_ENDED);
			
			smcExpected.configure(State.CANCELED)
			.permitDynamic(uncancel, new Func2<ProcessStep, State>() {
				@Override
				public State call(ProcessStep step) {
					if (!step.isWorkExpected())
						return State.NO_WORK_EXPECTED; 
					else if (!step.arePreCondFulfilled())
						return State.AVAILABLE;
					else if (step.arePostCondFulfilled() && step.arePreCondFulfilled())
						return State.COMPLETED;
					else if (step.getActualLifecycleState().equals(State.ACTIVE))
						return State.ACTIVE;
					else 
						return State.ENABLED;
				}
			})
			.permit(Trigger.HALT, State.NO_WORK_EXPECTED)
				//.permit(Trigger.RESET, State.AVAILABLE)
				//.permit(Trigger.ENABLE, State.ENABLED)				
				//.permit(Trigger.ACTIVATE_REPAIR, State.ACTIVE)
				//.permit(Trigger.MARK_COMPLETE_REPAIR, State.COMPLETED)
				.substateOf(State.SUPERSTATE_ENDED);
			

		}
		return smcExpected;
	}
	
	public static StateMachineConfig<State, Trigger> getActualStateMachineConfig() {
		if (smcActual == null) {
			// https://github.com/oxo42/stateless4j
			smcActual = new StateMachineConfig<>();
			smcActual.configure(State.AVAILABLE)
				.permit(Trigger.ACTIVATE, State.ACTIVE) //deviating trigger
				.permit(Trigger.MARK_COMPLETE, State.COMPLETED) // deviating trigger
				.permit(Trigger.ENABLE, State.ENABLED)
				.permit(Trigger.HALT, State.NO_WORK_EXPECTED)
				.permit(Trigger.CANCEL, State.CANCELED)
				;
			smcActual.configure(State.ENABLED)
				.permit(Trigger.ACTIVATE, State.ACTIVE)
				.permit(Trigger.RESET, State.AVAILABLE)
				.permit(Trigger.MARK_COMPLETE, State.COMPLETED)
				.permit(Trigger.HALT, State.NO_WORK_EXPECTED)
				.permit(Trigger.CANCEL, State.CANCELED)
				;
			smcActual.configure(State.ACTIVE)
				.permit(Trigger.MARK_COMPLETE/*_DEVIATING*/, State.COMPLETED) // deviating trigger	
		//		.ignore(Trigger.ACTIVATE_REPAIR) // repair / expected catching up with actual 
		//		.permit(Trigger.MARK_COMPLETE, State.COMPLETED)
				.permit(Trigger.ENABLE, State.ENABLED)  //FIXME: doesnt make sense, if we have already made some actions, why would we ever go back to enabled, perhaps when we made accidental changes and undo them? 
				// --> still some activeness, then question whether we rather cancel, deactivation needs special capability which we ignore for now
				.permit(Trigger.RESET, State.AVAILABLE)
				.permit(Trigger.HALT, State.NO_WORK_EXPECTED)
				.permit(Trigger.CANCEL, State.CANCELED)
				;
			smcActual.configure(State.NO_WORK_EXPECTED)
				.permitDynamic(unhalt, new Func2<ProcessStep, State>() {
					@Override
					public State call(ProcessStep step) {						
						if (step.areCancelCondFulfilled())
							return State.CANCELED; 
						else if (!step.arePreCondFulfilled())
							return State.AVAILABLE;
						else if (step.arePostCondFulfilled() && step.arePreCondFulfilled())
							return State.COMPLETED;
						else if (step.getActualLifecycleState().equals(State.ACTIVE))
							return State.ACTIVE;
						else 
							return State.ENABLED;
					}
				}) // repairing
//				.permit(Trigger.ACTIVATE/*_DEVIATING*/, State.ACTIVE) //deviating trigger
				.permit(Trigger.MARK_COMPLETE/*_DEVIATING*/, State.COMPLETED) // deviating trigger
//				.permit(Trigger.MARK_COMPLETE_REPAIR, State.COMPLETED) // repair trigger
//				.permit(Trigger.ACTIVATE_REPAIR, State.ACTIVE) //repair trigger 
				.permit(Trigger.ACTIVATE, State.ACTIVE)
				.permit(Trigger.RESET, State.AVAILABLE)
				.permit(Trigger.ENABLE, State.ENABLED)
				.permit(Trigger.CANCEL, State.CANCELED)
			//	.permit(Triggers.MARK_COMPLETE, State.COMPLETED)
				.substateOf(State.SUPERSTATE_ENDED);
			smcActual.configure(State.CANCELED)
				.permitDynamic(uncancel, new Func2<ProcessStep, State>() {
					@Override
					public State call(ProcessStep step) {
						if (!step.isWorkExpected())
							return State.NO_WORK_EXPECTED; 
						else if (!step.arePreCondFulfilled())
							return State.AVAILABLE;
						else if (step.arePostCondFulfilled() && step.arePreCondFulfilled())
							return State.COMPLETED;
						else if (step.getActualLifecycleState().equals(State.ACTIVE))
							return State.ACTIVE;
						else 
							return State.ENABLED;
					}
				})
//				.permit(Trigger.ACTIVATE/*_DEVIATING*/, State.ACTIVE) //deviating trigger
//				.permit(Trigger.MARK_COMPLETE/*_DEVIATING*/, State.COMPLETED) // deviating trigger
//				.permit(Trigger.MARK_COMPLETE_REPAIR, State.COMPLETED) // repair trigger
//				.permit(Trigger.ACTIVATE_REPAIR, State.ACTIVE) //repair trigger
				.permit(Trigger.ACTIVATE, State.ACTIVE)
				.permit(Trigger.RESET, State.AVAILABLE)
				.permit(Trigger.ENABLE, State.ENABLED)
				.permit(Trigger.HALT, State.NO_WORK_EXPECTED)
				.permit(Trigger.MARK_COMPLETE, State.COMPLETED)
				.substateOf(State.SUPERSTATE_ENDED);
			smcActual.configure(State.COMPLETED)
				.permit(Trigger.RESET, State.AVAILABLE) //deviating trigger	
				.permit(Trigger.ACTIVATE, State.ACTIVE) //deviating trigger
				.permit(Trigger.ENABLE, State.ENABLED)
				.permit(Trigger.CANCEL, State.CANCELED)
		//		.permit(Trigger.CANCEL, State.CANCELED) // if we worked in good faith on an XOR branch that later needs to be ignored, we should cancel any completed step to signal that we explicitly wont use the results of that step
		//		.permit(Trigger.HALT, State.NO_WORK_EXPECTED) // when a complete task is in an XOR branch that is not finally selected
		//		.ignore(Trigger.MARK_COMPLETE_REPAIR) // repair / expected catching up with actual 
				.substateOf(State.SUPERSTATE_ENDED);
		}
		return smcActual;
	}
	
	
	
	public static StateMachine<State, Trigger> buildExpectedStatemachine() {
		StateMachine<State, Trigger> sm = new StateMachine<>(State.AVAILABLE, getExpectedStateMachineConfig());
		return sm;
	}
	
	public static StateMachine<State, Trigger> buildActualStatemachine() {
		StateMachine<State, Trigger> sm = new StateMachine<>(State.AVAILABLE, getActualStateMachineConfig());
		return sm;
	}
	
	public static StateMachine<State, Trigger> buildExpectedStatemachineInState(State state) {
		StateMachine<State, Trigger> sm = new StateMachine<>(state, getExpectedStateMachineConfig());
		return sm;
	}
	
	public static StateMachine<State, Trigger> buildActualStatemachineInState(State state) {
		StateMachine<State, Trigger> sm = new StateMachine<>(state, getActualStateMachineConfig());
		return sm;
	}
}

/*

@startuml

title TaskLifecycle State Model
[*] --> AVAILABLE: createTask
AVAILABLE --> ENABLED : inputConditionsFulfilled
ENABLED --> ACTIVE : addOutput / activate
AVAILABLE --> ACTIVE : addOutput
ACTIVE --> ACTIVE : addOutput
ENABLED --> NOWORKEXPECTED : DONTWORKONTASK
ENABLED --> REVOKED : inputConditionsNoLongerHold
NOWORKEXPECTED --> ACTIVE : addOutput
NOWORKEXPECTED --> CANCELED : IGNORE_FOR_PROGRESS
REVOKED --> ENABLED : inputConditionsFulfilled
REVOKED --> CANCELED : IGNORE_FOR_PROGRESS
REVOKED --> PARTIALLY_COMPLETED : PARTIALLY_COMPLETE
ACTIVE --> REVOKED : inputConditionsNoLongerHold
ACTIVE --> COMPLETED : outputConditionsFulfilled
ACTIVE --> CANCELED : IGNORE_FOR_PROGRESS
ACTIVE --> PARTIALLY_COMPLETED : PARTIALLY_COMPLETE

COMPLETED --> [*] 
CANCELED --> [*] 
PARTIALLY_COMPLETED --> [*] 



note left of AVAILABLE
    whenever task is added to knowledge base
end note 

note right of ENABLED
    while input conditions are met, 
    but no work observed yet
end note

note left of ACTIVE
    some but insufficient user output 
    or after explicit activation by user
    might not imply fulfilled inputconditions
end note

note top of NOWORKEXPECTED
    e.g. when alternative task in XOR becomes active instead
    (but input conditions could still hold)
    user might still provide output
end note

note right of REVOKED
    when input conditions no longer hold 
    (similar to AVAILABLE but engine needs 
    to expect potential output from user)
end note

note top of COMPLETED
     when user provided sufficient output and conditions hold
     task and its output can now be used to trigger process progress
end note

note right of CANCELED
    explicitly set by user or 
    e.g. when two task were wrongfully worked on in an XOR split, 
    and one triggers completion, the other is canceled, 
    we still might see output 
    that should not be available to further tasks
    but that will be ignored
end note

note right of PARTIALLY_COMPLETED
    output conditions don't hold but rules 
    or user signal no more work,
    and output should be available to further tasks
end note 

note "once in COMPLETED, CANCELED, or PARTIALLY_COMPLETED, further user output will be recorded but not acted upon" as N1

note "External actions by the user include addOutput, activate, PARTIALLY_COMPLETE, and cancel; engine actionc include: createTask, inputConditionsFulfilled, inputConditionsNoLongerHold, outputConditionsFulfilled, DONTWORKONTASK" as N2

@enduml

 * 
 * */

