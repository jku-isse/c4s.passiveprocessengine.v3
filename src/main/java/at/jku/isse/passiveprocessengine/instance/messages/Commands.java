package at.jku.isse.passiveprocessengine.instance.messages;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import at.jku.isse.designspace.core.events.PropertyUpdate;
import at.jku.isse.designspace.rule.model.ConsistencyRule;
import at.jku.isse.passiveprocessengine.definition.StepDefinition;
import at.jku.isse.passiveprocessengine.instance.InputToOutputMapper;
import at.jku.isse.passiveprocessengine.instance.ProcessInstance;
import at.jku.isse.passiveprocessengine.instance.ProcessStep;
import at.jku.isse.passiveprocessengine.instance.StepLifecycle.Conditions;
import at.jku.isse.passiveprocessengine.instance.StepLifecycle.State;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

public class Commands {

	public interface IdentifiableCmd {       
    }
	
	public static abstract class ProcessScopedCmd implements IdentifiableCmd{
		
		
		public abstract ProcessInstance getScope();
		
		public abstract List<Events.ProcessChangedEvent> execute();
	}

    @Slf4j
	@Data
	public static class PrematureStepTriggerCmd extends ProcessScopedCmd {
		private final StepDefinition sd;
		private final ProcessInstance procInst;
		private final boolean isFulfilled;
		
		public List<Events.ProcessChangedEvent> execute() {
			if (isFulfilled) {
				if (getScope().getProcessSteps().stream().noneMatch(t -> t.getDefinition().getId().equals(sd.getId()))) {
					ProcessStep step = getScope().createAndWireTask(sd);
					if (step != null) {
						//TODO: set more precise inputs (or from further distance)
						//TODO: trigger correct step transitions
						List<Events.ProcessChangedEvent> events = new LinkedList<>();
						events.addAll(step.getInDNI().tryDataPropagationToPrematurelyTriggeredTask());
						events.addAll(step.setActivationConditionsFulfilled());
						return events;
					} else {
						log.warn(String.format("No step created while trying to execute premature invocation of %s in %s ", sd.getName(), procInst.getName()));
						return Collections.emptyList();
					}
				} else { // nothing to do
					log.debug(String.format("No need to execute premature invocation of %s in %s as step exists in the mean time", sd.getName(), procInst.getName()));
					return Collections.emptyList();
				}
			} // nothing to do
				return Collections.emptyList();
		}
		
		@Override
		public ProcessInstance getScope() {
			return procInst;
		}

		@Override
		public String toString() {
			return "PrematureStepTriggerCmd [" + sd.getName() + " in "+procInst.getName()+" premature triggered: " + isFulfilled
					+ "]";
		}
	}
	
	@EqualsAndHashCode(callSuper=false)
	@Data 
	public static class QAConstraintChangedCmd extends ProcessScopedCmd {
		private final ProcessStep step;
		private final ConsistencyRule crule;
		 private final boolean isFulfilled;
	
		public List<Events.ProcessChangedEvent> execute() {
			return step.processQAEvent(crule, isFulfilled);
		}

		@Override
		public String toString() {
			return "QAConstraintChangedCmd [" + step.getDefinition().getName() + " " + crule.getInstanceType().name() +":"+ isFulfilled + "]";
		}

		@Override
		public ProcessInstance getScope() {
			return step.getProcess();
		}
	}
	
	@EqualsAndHashCode(callSuper=false)
	@Data
	public static class IOMappingConsistencyCmd extends ProcessScopedCmd {
		private final ProcessStep step;
		private final ConsistencyRule crule;
		private final boolean isInconsistent;
	
		public List<Events.ProcessChangedEvent> execute() {
			if (isInconsistent)
				return InputToOutputMapper.mapInputToOutputInStepScope(step, crule);
			else // nothing to do
				return Collections.emptyList();
		}

		@Override
		public String toString() {
			return "IOMappingInconsistentCmd [" + step.getDefinition().getName() + " " + crule.getInstanceType().name() + "]";
		}
		
		@Override
		public ProcessInstance getScope() {
			return step.getProcess();
		}
		
		
	}
    
	@EqualsAndHashCode(callSuper=false)
    @Data
    public static class ConditionChangedCmd extends ProcessScopedCmd {
        private final ProcessStep step;
        private final Conditions condition;
        private final boolean isFulfilled;
		@Override
		public List<Events.ProcessChangedEvent> execute() {
			switch(condition) {
			case ACTIVATION:
				if (isFulfilled)
					return step.setActivationConditionsFulfilled();
				else return Collections.emptyList();
			case CANCELATION:
				return step.setCancelConditionsFulfilled(isFulfilled);
			case POSTCONDITION:
				return step.setPostConditionsFulfilled(isFulfilled);
			case PRECONDITION:
				return step.setPreConditionsFulfilled(isFulfilled);				
			default:
				return Collections.emptyList();
			}
			
		}
		@Override
		public String toString() {
			return "ConditionChangedCmd [" + step.getDefinition().getName() + " " + condition + " : " + isFulfilled
					+ "]";
		}
        
		@Override
		public ProcessInstance getScope() {
			return step.getProcess();
		}
    }
    
	@Data
	public static class OutputChangedCmd extends ProcessScopedCmd {
		private final ProcessStep step;
		private final PropertyUpdate change;
		
		public List<Events.ProcessChangedEvent> execute() {
			if (step.getOutDNI() != null && step.getActualLifecycleState().equals(State.COMPLETED) ) // to avoid NPE in case this is a ProcessInstance AND still some unexpected late output change
				return step.getOutDNI().signalPrevTaskDataChanged(step);
			else { 
				List<Events.ProcessChangedEvent> events = new LinkedList<>();
				// whenever there is output added or removed, it means someone was active regardless of otherstate (except for COMPLETED)
				if (!step.getActualLifecycleState().equals(State.ACTIVE) && !step.getActualLifecycleState().equals(State.COMPLETED))
					events.addAll(step.setActivationConditionsFulfilled());
				if (step.getOutDNI() != null && step.isImmediateDataPropagationEnabled()) {
					events.addAll(step.getOutDNI().tryDataPropagationToPrematurelyTriggeredTask());			
					//FIXME: there might be instances used in steps further down, thus we also need to trigger their inDNIs as otherwise there wont be any instances added/removed)
				}
				return events;
			}
		}

		@Override
		public String toString() {
			return "OutputChangedCmd [" + step.getDefinition().getName() + " " + change.name() + "]";
		}
		
		@Override
		public ProcessInstance getScope() {
			return step.getProcess();
		}
		
		
	}
    
    
//    @Data
//    public static class SetPostConditionsFulfillmentCmd extends TrackableCmd {
//        @TargetAggregateIdentifier
//        private final String id;
//        private final String wftId;
//        private final boolean isFulfilled;
//    }
//    @Data
//    public static class ActivateTaskCmd extends TrackableCmd {
//        @TargetAggregateIdentifier
//        private final String id;
//        private final String wftId;
//    }
//    
//    @Data
//    public static class ChangeCanceledStateOfTaskCmd extends TrackableCmd {
//    	@TargetAggregateIdentifier
//        private final String id;
//        private final String wftId; // or wfi Id if whole process is to be canceled or uncanceled
//        private final boolean isCanceled;
//    }
//    
//    @Data
//    public static class SetPropertiesCmd extends TrackableCmd {
//        @TargetAggregateIdentifier
//        private final String id;
//        private final String iwftId;
//        private final Map<String, String> properties;
//    }
//    @Data
//    public static class InstantiateTaskCmd extends TrackableCmd {
//        @TargetAggregateIdentifier
//        private final String id;
//        private final String taskDefinitionId;
//        private final List<ArtifactInput> optionalInputs;
//        private final List<ArtifactOutput> optionalOutputs;
//    }
//    

}

