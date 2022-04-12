package at.jku.isse.passiveprocessengine.instance.messages;

import artifactapi.ArtifactIdentifier;
import artifactapi.IArtifact;
import artifactapi.ResourceLink;
import at.jku.isse.designspace.rule.model.ConsistencyRule;
import at.jku.isse.passiveprocessengine.instance.InputToOutputMapper;
import at.jku.isse.passiveprocessengine.instance.ProcessInstance;
import at.jku.isse.passiveprocessengine.instance.ProcessStep;
import at.jku.isse.passiveprocessengine.instance.StepLifecycle;
import at.jku.isse.passiveprocessengine.instance.StepLifecycle.Conditions;
import lombok.Data;


import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

public class Commands {

	public interface IdentifiableCmd {
       // String getId();
        
    }
	
	public static abstract class ProcessScopedCmd implements IdentifiableCmd{
		
//		String parentCauseRef;
//		public String getParentCauseRef() {
//			return parentCauseRef;
//		}
//		public ProcessScopedCmd setParentCauseRef(String parentCauseRef) {
//			this.parentCauseRef = parentCauseRef;
//			return this;
//		}
		
		public abstract ProcessInstance getScope();
		
		public abstract List<Events.ProcessChangedEvent> execute();
	}
	
//	@Data
//	public static class CompositeCmd extends TrackableCmd {
//        private final String id;
//		private final List<TrackableCmd> commandList;
//	}
	
//    @Data
//    public static class CreateWorkflowCmd extends TrackableCmd{
//        private final String id;
//        private final Map<ArtifactIdentifier, String> input;
//        private final String definitionName;
//    }
//    
//    @Data
//    public static class CreateSubWorkflowCmd extends TrackableCmd {
//        @TargetAggregateIdentifier
//        private final String id;
//        private final String parentWfiId;
//        private final String parentWftId;
//        private final String definitionName;
//        private final Map<ArtifactIdentifier, String> input;
//    }
//
//    @Data
//    public static class CompleteDataflowCmd extends TrackableCmd {
//        @TargetAggregateIdentifier
//        private final String id;
//        private final String dniId;
//        private final ResourceLink res;
//    }
//
//    @Data
//    public static class DeleteCmd extends TrackableCmd {
//        @TargetAggregateIdentifier
//        private final String id;
//    }
//    @Data
//    public static class AddConstraintsCmd extends TrackableCmd {
//        @TargetAggregateIdentifier
//        private final String id;
//        private final String wftId;
//        private final Map<String, String> rules;
//    }
//    @Data
//    public static class AddEvaluationResultToConstraintCmd extends TrackableCmd {
//        @TargetAggregateIdentifier
//        private final String id;
//        private final String wftId;
//        private final String qacId;
//        private final Map<ResourceLink, Boolean> res;
//        private final CorrelationTuple corr;
//        private final Instant time;
//    }
//    @Data
//    public static class CheckConstraintCmd extends TrackableCmd {
//        @TargetAggregateIdentifier
//        private final String id;
//        private final String constrId;
//    }
//    @Data
//    public static class CheckAllConstraintsCmd extends TrackableCmd {
//        @TargetAggregateIdentifier
//        private final String id;
//    }
//    @Data
//    public static class AddInputCmd extends TrackableCmd {
//        @TargetAggregateIdentifier
//        private final String id;
//        private final String wftId;
//        private final String artifactId;
//        private final String role;
//        private final String type;
//    }
//    @Data
//    public static class AddOutputCmd extends TrackableCmd {
//        @TargetAggregateIdentifier
//        private final String id;
//        private final String wftId;
//        private final String artifactId;
//        private final String role;
//        private final String type;
//    }
//    @Data
//    public static class AddInputToWorkflowCmd extends TrackableCmd {
//        @TargetAggregateIdentifier
//        private final String id;
//        private final String artifactId;
//        private final String role;
//        private final String type;
//    }
//    @Data
//    public static class AddOutputToWorkflowCmd extends TrackableCmd {
//        @TargetAggregateIdentifier
//        private final String id;
//        private final String artifactId;
//        private final String role;
//        private final String type;
//    }
//    @Data
//    public static class RemoveOutputCmd extends TrackableCmd {
//        @TargetAggregateIdentifier
//        private final String id;
//        private final String wftId;
//        private final ArtifactIdentifier artifactId;
//        private final String role;
//    }
//    @Data
//    public static class RemoveInputCmd extends TrackableCmd {
//        @TargetAggregateIdentifier
//        private final String id;
//        private final String wftId;
//        private final ArtifactIdentifier artifactId;
//        private final String role;
//    }
//    @Data
//    public static class UpdateArtifactsCmd extends TrackableCmd {
//        @TargetAggregateIdentifier
//        private final String id;
//        private final List<IArtifact> artifacts;
//    }
//    @Data
//    public static class SetPreConditionsFulfillmentCmd extends TrackableCmd {
//        private final String id;
//        private final String wftId;
//        private final boolean isFulfilled;
//    }
    
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
	
	@Data
	public static class IOMappingInconsistentCmd extends ProcessScopedCmd {
		private final ProcessStep step;
		private final ConsistencyRule crule;
	
		public List<Events.ProcessChangedEvent> execute() {
			return InputToOutputMapper.mapInputToOutputInStepScope(step, crule);
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
    
    @Data
    public static class ConditionChangedCmd extends ProcessScopedCmd {
        private final ProcessStep step;
        private final Conditions condition;
        private final boolean isFulfilled;
		@Override
		public List<Events.ProcessChangedEvent> execute() {
			switch(condition) {
			case ACTIVATION:
				return step.setActivationConditionsFulfilled();
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

