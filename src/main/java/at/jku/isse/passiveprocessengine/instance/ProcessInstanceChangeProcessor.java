package at.jku.isse.passiveprocessengine.instance;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import at.jku.isse.passiveprocessengine.rdfwrapper.RDFInstance;
import at.jku.isse.passiveprocessengine.rdfwrapper.RDFInstanceType;
import at.jku.isse.passiveprocessengine.core.ProcessContext;
import at.jku.isse.passiveprocessengine.core.RuleResult;
import at.jku.isse.passiveprocessengine.instance.StepLifecycle.Conditions;
import at.jku.isse.passiveprocessengine.instance.activeobjects.ProcessInstance;
import at.jku.isse.passiveprocessengine.instance.activeobjects.ProcessStep;
import at.jku.isse.passiveprocessengine.instance.messages.Commands;
import at.jku.isse.passiveprocessengine.instance.messages.Commands.ConditionChangedCmd;
import at.jku.isse.passiveprocessengine.instance.messages.Commands.IOMappingConsistencyCmd;
import at.jku.isse.passiveprocessengine.instance.messages.Commands.OutputChangedCmd;
import at.jku.isse.passiveprocessengine.instance.messages.Commands.PrematureStepTriggerCmd;
import at.jku.isse.passiveprocessengine.instance.messages.Commands.ProcessScopedCmd;
import at.jku.isse.passiveprocessengine.instance.messages.Commands.QAConstraintChangedCmd;
import at.jku.isse.passiveprocessengine.instance.messages.EventDistributor;
import at.jku.isse.passiveprocessengine.instance.messages.Events;
import at.jku.isse.passiveprocessengine.instance.types.AbstractProcessStepType;
import at.jku.isse.passiveprocessengine.instance.types.SpecificProcessInstanceType;
import at.jku.isse.passiveprocessengine.rdfwrapper.events.ChangeListener;
import at.jku.isse.passiveprocessengine.rdfwrapper.events.PropertyChange;
import at.jku.isse.passiveprocessengine.rdfwrapper.events.PropertyChange.Update;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ProcessInstanceChangeProcessor implements ChangeListener {

	final EventDistributor distributor;
	final ProcessContext context;
	final RDFInstanceType stepType;
	
	//we queue commands and remove some that are undone when lazy fetching of artifacts results in different outcome
	protected Map<String, Commands.ProcessScopedCmd> cmdQueue = Collections.synchronizedMap(new HashMap<>());

	private EventStats stats = new EventStats();

	public ProcessInstanceChangeProcessor(ProcessContext context, EventDistributor distributor) {		
		this.distributor = distributor;
		this.context = context;
		stepType = context.getSchemaRegistry().getTypeByName(AbstractProcessStepType.typeId);
		assert(stepType != null);
	}

	public EventStats getEventStatistics() {
		return stats;
	}

	@Override
	public void handleUpdates(Collection<Update> operations) {
		processProcessUpdates(operations);
	}

	protected Set<ProcessInstance> processProcessUpdates(Collection<Update> operations) {
		@SuppressWarnings("unchecked")

		List<ProcessScopedCmd> queuedEffects = (List<ProcessScopedCmd>) operations.stream()
		 .map(operation -> { 			
				if (operation instanceof PropertyChange.Add op) {
					 return processPropertyUpdateAdd(op);
				} else
					if (operation instanceof PropertyChange.Remove op) {
						 return processPropertyUpdateRemove(op);
					} else
						if (operation instanceof PropertyChange.Set op) {
							return processPropertyUpdateSet(op);
						}
			return Optional.empty();
		})
		.filter(Optional::isPresent)
		.map(Optional::get)
		.toList();

		prepareQueueExecution(queuedEffects); // here we can subclass and manage commands, lazy loading etc (no longer is use at the moment)
		// we process this in a service loop, if there is lazy loading then we would need to ensure there are no more changes to rule outcomes before executing commands
		return executeCommands(); // here we execute, or in subclass, when we are still lazy loading, skip execution until later
	}

	private Optional<ProcessScopedCmd> processPropertyUpdateAdd(PropertyChange.Add op) {		
		RDFInstance element = op.getInstance();
		if(	(op.getName().startsWith("in_") || op.getName().startsWith("out_"))
				&& isOfStepType(op.getInstance()) ) {
			ProcessStep step = context.getWrappedInstance(ProcessStep.class, element);	
			RDFInstance added = (RDFInstance) op.getValue();
			log.debug(String.format("%s %s now also contains %s", step.getName(),
					op.getName(),																
					added != null ? added.getName() : "NULL"
					));
			stats.incrementIoAddEventCount();
			return Optional.ofNullable(step.prepareIOAddEvent(op));
		}		
		return Optional.empty();
	}

	private Optional<ProcessScopedCmd> processPropertyUpdateRemove(PropertyChange.Remove op) {
		RDFInstance element = op.getInstance();
			if(	(op.getName().startsWith("in_") || op.getName().startsWith("out_"))
				&& isOfStepType(element) ) {
				ProcessStep step = context.getWrappedInstance(ProcessStep.class, element);
				log.info(String.format("%s %s removed %s", step.getName(),
																op.getName(),
																op.getValue()
																));
				stats.incrementIoRemoveEventCount();
				return Optional.ofNullable(step.prepareIORemoveEvent(op));
			}
		return Optional.empty();
	}

	private Optional<ProcessScopedCmd> processPropertyUpdateSet(PropertyChange.Set op) {
		RDFInstance element = op.getInstance();
//		if (isOfStepType(element)) {
//			if (!op.getName().startsWith("@")) {
//				log.info(String.format("Step %s updated %s to %s", element.getName(),
//					op.getName(),
//					String.valueOf(op.getValue())
//					));
//			}
//		} else 
		if (op.getName().equals("ruleHasConsistentResult") && element instanceof RuleResult cr) {
			RDFInstance ruleContext = cr.getContextInstance();
			if (isOfStepType(ruleContext)) { // rule belonging to a step, or process
				ProcessStep step = getAsStepOrClass(ruleContext);
				stats.incrementRuleUpdateEventCount();
				ProcessScopedCmd effect = step.prepareRuleEvaluationChange(cr, op);
				return Optional.ofNullable(effect);
			}
		}
		return Optional.empty();
	}

	private boolean isOfStepType(RDFInstance instance) {
		if (instance == null) 
			return false;
		else {
			var type = instance.getInstanceType();
			return type != null && type.isOfTypeOrAnySubtype(stepType);		
		}
	}
	
	private ProcessStep getAsStepOrClass(RDFInstance instance) {
		if (instance.getInstanceType().hasPropertyType(SpecificProcessInstanceType.CoreProperties.processDefinition.toString())) 
			return context.getWrappedInstance(ProcessInstance.class, instance);
		else
			return context.getWrappedInstance(ProcessStep.class, instance);
	}

	
	protected void prepareQueueExecution(List<ProcessScopedCmd> mostRecentQueuedEffects) {
		// check for each queued effect if it undos a previous one: e.g., a constraint fufillment now is unfulfilled, datamapping fulfilled is no unfulfilled
		mostRecentQueuedEffects.forEach(cmd -> {
			ProcessScopedCmd overriddenCmd = cmdQueue.put(cmd.getId(), cmd);
			if (overriddenCmd != null) {
				log.trace("Overridden Command: "+overriddenCmd.toString());
			}
		});

	}

	protected Set<ProcessInstance> executeCommands() {
		Collection<ProcessScopedCmd> relevantEffects = cmdQueue.values();
		// now lets just execute all commands
		// but first we should check if we need to load lazy fetched artifacts that might override/undo the queuedEffects
		if (!relevantEffects.isEmpty()) {
			List<Events.ProcessChangedEvent> cmdEvents = new LinkedList<>();
			// qa not fulfilled
			cmdEvents.addAll(relevantEffects.stream()
			.filter(QAConstraintChangedCmd.class::isInstance)
			.map(QAConstraintChangedCmd.class::cast)
			.filter(cmd -> !cmd.isFulfilled())
			.map(cmd -> {
				stats.incrementNegQAConstraintChangedCmdCount();
				log.debug(String.format("Executing: %s", cmd.toString()));
				return cmd;})
			.flatMap(cmd -> cmd.execute().stream())
			.collect(Collectors.toList()));

			// first sort them to apply them in a sensible order, e.g., preconditions fulfilled before postconditions
			cmdEvents.addAll(relevantEffects.stream()
				.filter(ConditionChangedCmd.class::isInstance)
				.map(ConditionChangedCmd.class::cast)
				.sorted(new CommandComparator())
				.map(cmd -> {
					stats.incrementConditionChangedCmdCount();
					log.debug(String.format("Executing: %s", cmd.toString()));
					return cmd;})
				.flatMap(cmd -> cmd.execute().stream())
				.collect(Collectors.toList()));

			// if QA is fulfilled
			cmdEvents.addAll(relevantEffects.stream()
			.filter(QAConstraintChangedCmd.class::isInstance)
			.map(QAConstraintChangedCmd.class::cast)
			.filter(cmd -> cmd.isFulfilled())
			.map(cmd -> {
				stats.incrementPosQAConstraintChangedCmdCount();
				log.debug(String.format("Executing: %s", cmd.toString()));
				return cmd;})
			.flatMap(cmd -> cmd.execute().stream())
			.collect(Collectors.toList()));

			// then execute datamappings
			cmdEvents.addAll(relevantEffects.stream()
				.filter(IOMappingConsistencyCmd.class::isInstance)
				.map(IOMappingConsistencyCmd.class::cast)
				.map(cmd -> {
					stats.incrementIOMappingConsistencyCmdCount();
					log.debug(String.format("Executing: %s", cmd.toString()));
					return cmd;})
				.flatMap(cmd -> cmd.execute().stream())
				.collect(Collectors.toList()));

			// then execute output change events
			cmdEvents.addAll(relevantEffects.stream()
					.filter(OutputChangedCmd.class::isInstance)
					.map(OutputChangedCmd.class::cast)
					.map(cmd -> {
						stats.incrementOutputChangedCmdCount();
						log.debug(String.format("Executing: %s", cmd.toString()));
						return cmd;})
					.flatMap(cmd -> cmd.execute().stream())
					.collect(Collectors.toList()));

			// then execute premature step triggers
			cmdEvents.addAll(relevantEffects.stream()
					.filter(PrematureStepTriggerCmd.class::isInstance)
					.map(PrematureStepTriggerCmd.class::cast)
					.map(cmd -> {
						stats.incrementPrematureStepTriggerCmdCount();
						log.debug(String.format("Executing: %s", cmd.toString()));
						return cmd;})
					.flatMap(cmd -> cmd.execute().stream())
					.collect(Collectors.toList()));

			stats.incrementProcessEventCountBy(cmdEvents.size());

			Set<ProcessInstance> procs = new HashSet<>();
			//procs.addAll(relevantEffects.stream().map(cmd -> cmd.getScope()).collect(Collectors.toSet()));
			procs.addAll(cmdEvents.stream().map(event -> event.getProcScope()).filter(Objects::nonNull).collect(Collectors.toSet()));
			relevantEffects.clear();
			cmdQueue.clear();
			if (distributor  != null)
				distributor.handleEvents(cmdEvents); //do something with the change events from command based effects, if needed, e.g., for LTE-based checking
	//		context.getInstanceRepository().concludeTransaction();
			return procs;
		} else
			return Collections.emptySet();
	}

	public static class CommandComparator implements Comparator<ConditionChangedCmd> {

		@Override
		public int compare(ConditionChangedCmd o1, ConditionChangedCmd o2) {
			if (o1.getStep().getProcess() == null ) { // we have a process
				if (o2.getStep().getProcess() == null) // we have a second process
					return o1.getStep().getName().compareTo(o2.getStep().getName()); // we sort processes by their id
				else {// other is a step,
					return 1; // we put steps first, do process updates at the end
				}
			} else if (o2.getStep().getProcess() == null) { // second one is a process, but not first
				return -1; // we put steps first, process updates at the very end
			} else { // neither are processes
				int procBased = o1.getStep().getProcess().getName().compareTo(o2.getStep().getProcess().getName()); // compare via process
				if (procBased == 0) {
					// now compare via steps
					int stepBased = o1.getStep().getDefinition().getSpecOrderIndex().compareTo(o2.getStep().getDefinition().getSpecOrderIndex());
					if (stepBased == 0) { // same step
						return Integer.compare(getAssignedValue(o1.getCondition(), o1.isFulfilled()), getAssignedValue(o2.getCondition(), o2.isFulfilled()));
					} else return stepBased; // compare/sort by step order in proc definition
				} else return procBased; // sort steps from different processes by process first
			}
		}

		private int getAssignedValue(Conditions cond, boolean fulfilled) {
			// we need to ensure that we first signal a failure of conditions, before a fulfillment to avoid activation propagation that needs to be undone thereafter
			//TODO: support also no work expected
			// now sort by state transition: 0 canceled -> 1 noWorkExpected
			// 										-> 3 preFulfilled(false) 							-> 5 active -> 6 postFulfilled(true)
			//	but		-> 2 postFulfilled(false) 							->  4 prefulfilled(true) 	--> 5 active
			switch(cond) {
			case CANCELATION:
				return 0;
			case ACTIVATION:
				return 5;
			case POSTCONDITION:
				if (fulfilled) return 6;
				else return 2;
			case PRECONDITION:
				if (fulfilled) return 4;
				else return 3;
			default:
				return 10;
			}
		}

	}

	@Data
	public static class EventStats {
		private long ruleUpdateEventCount;
		private long ioAddEventCount;
		private long ioRemoveEventCount;

		private long negQAConstraintChangedCmdCount;
		private long posQAConstraintChangedCmdCount;
		private long conditionChangedCmdCount;
		private long ioMappingConsistencyCmdCount;
		private long outputChangedCmdCount;
		private long prematureStepTriggerCmdCount;

		private long processEventCount;

		protected void incrementIoAddEventCount() {
			ioAddEventCount++;
		}

		protected void incrementIoRemoveEventCount() {
			ioRemoveEventCount++;
		}

		protected void incrementRuleUpdateEventCount() {
			ruleUpdateEventCount++;
		}

		protected void incrementNegQAConstraintChangedCmdCount() {
			negQAConstraintChangedCmdCount++;
		}

		protected void incrementConditionChangedCmdCount() {
			conditionChangedCmdCount++;
		}

		protected void incrementPosQAConstraintChangedCmdCount() {
			posQAConstraintChangedCmdCount++;
		}

		protected void incrementIOMappingConsistencyCmdCount() {
			ioMappingConsistencyCmdCount++;
		}

		protected void incrementOutputChangedCmdCount() {
			outputChangedCmdCount++;
		}

		protected void incrementPrematureStepTriggerCmdCount() {
			prematureStepTriggerCmdCount++;
		}

		protected void incrementProcessEventCountBy(long inc) {
			processEventCount = processEventCount + inc;
		}
	}


}
