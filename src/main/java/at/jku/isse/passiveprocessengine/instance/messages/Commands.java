package at.jku.isse.passiveprocessengine.instance.messages;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import at.jku.isse.passiveprocessengine.core.RuleResult;
import at.jku.isse.passiveprocessengine.definition.activeobjects.StepDefinition;
import at.jku.isse.passiveprocessengine.instance.InputToOutputMapper;
import at.jku.isse.passiveprocessengine.instance.StepLifecycle.Conditions;
import at.jku.isse.passiveprocessengine.instance.activeobjects.ProcessInstance;
import at.jku.isse.passiveprocessengine.instance.activeobjects.ProcessStep;
import at.jku.isse.passiveprocessengine.rdfwrapper.events.PropertyChange;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

public class Commands {

	public interface IdentifiableCmd {
    }

	public static abstract class ProcessScopedCmd implements IdentifiableCmd{


		public abstract ProcessInstance getScope();

		public abstract List<Events.ProcessChangedEvent> execute();

		public abstract String getId();
	}

    @Slf4j
	@Data
	public static class PrematureStepTriggerCmd extends ProcessScopedCmd {
		private final StepDefinition sd;
		private final ProcessInstance procInst;
		private final boolean isFulfilled;

		@Override
		public List<Events.ProcessChangedEvent> execute() {
			if (isFulfilled) {
				if (getScope().getProcessSteps().stream().noneMatch(t -> t.getDefinition().getName().equals(sd.getName()))) {
					ProcessStep step = getScope().createAndWireTask(sd);
					if (step != null) {
						// set more precise inputs (or from further distance) --> DNI will use input from further away if defined so in the inter-step data mappings
						//: trigger correct step transitions --> adding input will result in proper constraint evaluation and hence step status
						List<Events.ProcessChangedEvent> events = new LinkedList<>();
						events.addAll(step.getInDNI().doDataPropagationToPrematurelyTriggeredTask());
						events.addAll(step.setActivationConditionsFulfilled(true));
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

		@Override
		public String getId() {
			return "PrematureStepTriggerCmd [" +procInst.getName()+sd.getName();
		}
	}

	@EqualsAndHashCode(callSuper=false)
	@Data
	public static class QAConstraintChangedCmd extends ProcessScopedCmd {
		private final ProcessStep step;
		private final RuleResult crule;
		 private final boolean isFulfilled;

		@Override
		public List<Events.ProcessChangedEvent> execute() {
			return step.processQAEvent(crule, isFulfilled);
		}

		@Override
		public String toString() {
			return "QAConstraintChangedCmd [" + step.getDefinition().getName() + " " + crule.getInstanceType().getName() +":"+ isFulfilled + "]";
		}

		@Override
		public ProcessInstance getScope() {
			return step.getProcess();
		}

		@Override
		public String getId() {
			return "QAConstraintChangedCmd [" +step.getName()+crule.getInstanceType().getName();
		}
	}

	@EqualsAndHashCode(callSuper=false)
	@Data
	public static class IOMappingConsistencyCmd extends ProcessScopedCmd {
		private final ProcessStep step;
		private final RuleResult crule;
		private final boolean isInconsistent;
		private final InputToOutputMapper ioMapper;

		@Override
		public List<Events.ProcessChangedEvent> execute() {
			if (isInconsistent)
				return ioMapper.mapInputToOutputInStepScope(step, crule);
			else // nothing to do
				return Collections.emptyList();
		}

		@Override
		public String toString() {
			return "IOMappingConsistencyCmd [" + step.getDefinition().getName() + " " + crule.getInstanceType().getName() + "]";
		}

		@Override
		public ProcessInstance getScope() {
			return step.getProcess();
		}

		@Override
		public String getId() {
			return "IOMappingConsistencyCmd [" +step.getName()+crule.getInstanceType().getName();
		}
	}

	@EqualsAndHashCode(callSuper=false)
    @Data
    public static class ConditionChangedCmd extends ProcessScopedCmd {
        private final ProcessStep step;
        private final RuleResult ruleResult;
        private final Conditions condition;
        private final boolean isFulfilled;
		@Override
		public List<Events.ProcessChangedEvent> execute() {
			switch(condition) {
			case ACTIVATION:
				return step.processActivationConditionsChange(ruleResult, isFulfilled);
			case CANCELATION:
				return step.processCancelConditionsChange(ruleResult, isFulfilled);
			case POSTCONDITION:
				return step.processPostConditionsChange(ruleResult, isFulfilled);
			case PRECONDITION:
				return step.processPreConditionsChange(ruleResult, isFulfilled);
			default:
				return Collections.emptyList();
			}

		}
		@Override
		public String toString() {
			return "ConditionChangedCmd [" + step.getDefinition().getName() + " " + ruleResult.getName() + " : " + isFulfilled
					+ "]";
		}

		@Override
		public ProcessInstance getScope() {
			return step.getProcess();
		}

		@Override
		public String getId() {
			return "ConditionChangedCmd ["+step.getName()+ruleResult.getName();
		}
    }

	@Data
	public static class OutputChangedCmd extends ProcessScopedCmd {
		private final ProcessStep step;
		private final PropertyChange.Update change;

		@Override
		public List<Events.ProcessChangedEvent> execute() {
			return step.processOutputChangedCmd(change.getName().substring(4));
		}

		@Override
		public String toString() {
			return "OutputChangedCmd [" + step.getDefinition().getName() + " " + change.getName() + "]";
		}

		@Override
		public ProcessInstance getScope() {
			return step.getProcess();
		}

		@Override
		public String getId() {
			return "OutputChangedCmd [" +step.getName()+change.getName();
		}
	}

}

