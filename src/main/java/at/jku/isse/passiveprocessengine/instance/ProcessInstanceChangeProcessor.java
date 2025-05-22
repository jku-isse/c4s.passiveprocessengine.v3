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
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import at.jku.isse.passiveprocessengine.core.ProcessContext;
import at.jku.isse.passiveprocessengine.core.ProcessInstanceChangeListener;
import at.jku.isse.designspace.artifactconnector.core.repository.CoreTypeFactory;
import at.jku.isse.passiveprocessengine.core.PPEInstance;
import at.jku.isse.passiveprocessengine.core.PPEInstanceType;
import at.jku.isse.passiveprocessengine.core.PropertyChange;
import at.jku.isse.passiveprocessengine.core.PropertyChange.Update;
import at.jku.isse.passiveprocessengine.core.RuleResult;
import at.jku.isse.passiveprocessengine.definition.factories.ProcessDefinitionFactory;
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
import at.jku.isse.passiveprocessengine.instance.types.SpecificProcessInstanceType;
import at.jku.isse.passiveprocessengine.monitoring.UsageMonitor;
import at.jku.isse.passiveprocessengine.instance.messages.EventDistributor;
import at.jku.isse.passiveprocessengine.instance.messages.Events;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ProcessInstanceChangeProcessor implements ProcessInstanceChangeListener {

	final EventDistributor distributor;
	final ProcessContext context;
	final PPEInstanceType stepType;
	final PPEInstanceType artType;
	final UsageMonitor usageMonitor;
	
	final Map<PPEInstance, ProcessStep> art2StepIndex = new ConcurrentHashMap<>();
	
	
	//we queue commands and remove some that are undone when lazy fetching of artifacts results in different outcome
	protected Map<String, Commands.ProcessScopedCmd> cmdQueue = Collections.synchronizedMap(new HashMap<>());

	private EventStats stats = new EventStats();

	public ProcessInstanceChangeProcessor(ProcessContext context, EventDistributor distributor, UsageMonitor usageMonitor) {		
		this.distributor = distributor;
		this.context = context;
		this.usageMonitor = usageMonitor;
		stepType = context.getSchemaRegistry().getType(ProcessStep.class);
		assert(stepType != null);
		artType = context.getSchemaRegistry().getTypeByName(CoreTypeFactory.BASE_TYPE_NAME);
		assert(artType != null);
	}

	public EventStats getEventStatistics() {
		return stats;
	}

	private boolean isOfStepType(PPEInstance instance) {
		if (instance == null) 
			return false;
		else
			return instance.getInstanceType().isOfTypeOrAnySubtype(stepType);		
	}

	private boolean isOfCRDType(PPEInstance instance) {
		if (instance == null) 
			return false;
		else
			return (instance.getName().startsWith(ProcessDefinitionFactory.CRD_PREFIX) && instance instanceof RuleResult);
		//FIXME better approach than naming
	}

	private boolean isOfArtifactType(PPEInstance instance) {
		if (instance == null) 
			return false;
		else
			return instance.getInstanceType().isOfTypeOrAnySubtype(artType);			
	}

	
	
	private Optional<ProcessScopedCmd> processPropertyUpdateAdd(PropertyChange.Add op) {		
		PPEInstance element = op.getInstance();
		if(!op.getName().contains("/@")
				&& (op.getName().startsWith("in_") || op.getName().startsWith("out_"))
				&& isOfStepType(op.getInstance()) ) {
			ProcessStep step = context.getWrappedInstance(ProcessStep.class, element);	
			PPEInstance added = (PPEInstance) op.getValue();
			log.debug(String.format("%s %s now also contains %s", step.getName(),
					op.getName(),																
					added != null ? added.getName() : "NULL"
					));
			art2StepIndex.put(added, step);
			stats.incrementIoAddEventCount();
			return Optional.ofNullable(step.prepareIOAddEvent(op));
		}		
		if (art2StepIndex.containsKey(op.getInstance())) {
			// change to a process related artifact
			var step = art2StepIndex.get(op.getInstance());
			var proc = step instanceof ProcessInstance ? (ProcessInstance)step : step.getProcess();
			var value = op.getValue() instanceof PPEInstance ? ((PPEInstance)op.getValue()).getName() : String.valueOf(op.getValue());
			usageMonitor.processArtifactChanged(proc, op.getInstance().getName(), op.getName(), "ADD", value);
		}
		return Optional.empty();
	}

	private Optional<ProcessScopedCmd> processPropertyUpdateRemove(PropertyChange.Remove op) {
		PPEInstance element = op.getInstance();
		if(!op.getName().contains("/@")  // ignore special properties  (e.g., usage in consistency rules, etc)
				&& (op.getName().startsWith("in_") || op.getName().startsWith("out_"))
				&& isOfStepType(element) ) {
			ProcessStep step = context.getWrappedInstance(ProcessStep.class, element);
			log.info(String.format("%s %s removed something", step.getName(),
					op.getName()
					//	,op.indexOrKey()
					));
			stats.incrementIoRemoveEventCount();
			return Optional.ofNullable(step.prepareIORemoveEvent(op));
		}
		if (art2StepIndex.containsKey(op.getInstance())) {
			// change to a process related artifact
			var step = art2StepIndex.get(op.getInstance());
			var proc = step instanceof ProcessInstance ? (ProcessInstance)step : step.getProcess();
			var value = op.getValue() instanceof PPEInstance ? ((PPEInstance)op.getValue()).getName() : String.valueOf(op.getValue());
			usageMonitor.processArtifactChanged(proc, op.getInstance().getName(), op.getName(), "REMOVE", value);
		}
		//	}
		return Optional.empty();
	}

	private Optional<ProcessScopedCmd> processPropertyUpdateSet(PropertyChange.Set op) {
		PPEInstance element = op.getInstance();
		if (isOfStepType(element)) {
			if (!op.getName().startsWith("@")) {
				log.info(String.format("Step %s updated %s to %s", element.getName(),
					op.getName(),
					String.valueOf(op.getValue())
					));
			}
		} else if (op.getName().equals("result") && isOfCRDType(element)) {
			RuleResult cr = (RuleResult)element;
			PPEInstance ruleContext = cr.getContextInstance();
			if (isOfStepType(ruleContext)) { // rule belonging to a step, or TODO: process!				
				ProcessStep step = getAsStepOrClass(ruleContext);
				stats.incrementRuleUpdateEventCount();
				ProcessScopedCmd effect = step.prepareRuleEvaluationChange(cr, op);
				return Optional.ofNullable(effect);
			}
		} else if (art2StepIndex.containsKey(op.getInstance())) {
			// change to a process related artifact
			var step = art2StepIndex.get(op.getInstance());
			var proc = step instanceof ProcessInstance ? (ProcessInstance)step : step.getProcess();
			var value = op.getValue() instanceof PPEInstance ? ((PPEInstance)op.getValue()).getName() : String.valueOf(op.getValue());
			usageMonitor.processArtifactChanged(proc, op.getInstance().getName(), op.getName(), "SET", value);
		}
		return Optional.empty();
	}

	private ProcessStep getAsStepOrClass(PPEInstance instance) {
		if (instance.getInstanceType().hasPropertyType(SpecificProcessInstanceType.CoreProperties.processDefinition.toString())) 
			return context.getWrappedInstance(ProcessInstance.class, instance);
		else
			return context.getWrappedInstance(ProcessStep.class, instance);
	}

	@Override
	public void handleUpdates(Collection<Update> operations) {
		processProcessUpdates(operations);
	}

	protected Set<ProcessInstance> processProcessUpdates(Collection<Update> operations) {
		@SuppressWarnings("unchecked")

		List<ProcessScopedCmd> queuedEffects = (List<ProcessScopedCmd>) operations.stream()
		 .map(operation -> { 			
				if (operation instanceof PropertyChange.Add) {
					 return processPropertyUpdateAdd((PropertyChange.Add) operation);
				} else
					if (operation instanceof PropertyChange.Remove) {
						 return processPropertyUpdateRemove((PropertyChange.Remove) operation);
					} else
						if (operation instanceof PropertyChange.Set) {
							return processPropertyUpdateSet((PropertyChange.Set) operation);
						}
			return Optional.empty();
		})
		.filter(Optional::isPresent)
		.map(opt -> opt.get())
		.collect(Collectors.toList());

		prepareQueueExecution(queuedEffects); // here we can subclass and manage commands, lazy loading etc
		return executeCommands(); // here we execute, or in subclass, when we are still lazy loading, skip execution until later
	}
	
	protected void prepareQueueExecution(List<ProcessScopedCmd> mostRecentQueuedEffects) {
		mostRecentQueuedEffects.forEach(cmd -> {
			cmdQueue.put(cmd.getId(), cmd);
		});

	}

	protected Set<ProcessInstance> executeCommands() {
		Collection<ProcessScopedCmd> relevantEffects = cmdQueue.values();
		// now lets just execute all commands
		// but first we should check if we need to load lazy fetched artifacts that might override/undo the queuedEffects
		if (relevantEffects.size() > 0) {
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
			context.getInstanceRepository().concludeTransaction();
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
