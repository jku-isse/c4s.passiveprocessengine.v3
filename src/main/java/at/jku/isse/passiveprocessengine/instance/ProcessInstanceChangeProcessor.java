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
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import at.jku.isse.designspace.core.events.Operation;
import at.jku.isse.designspace.core.events.PropertyUpdateAdd;
import at.jku.isse.designspace.core.events.PropertyUpdateRemove;
import at.jku.isse.designspace.core.events.PropertyUpdateSet;
import at.jku.isse.designspace.core.model.Element;
import at.jku.isse.designspace.core.model.Id;
import at.jku.isse.designspace.core.model.Instance;
import at.jku.isse.designspace.core.model.Workspace;
import at.jku.isse.designspace.core.model.WorkspaceListener;
import at.jku.isse.designspace.rule.model.ConsistencyRule;
import at.jku.isse.passiveprocessengine.WrapperCache;
import at.jku.isse.passiveprocessengine.instance.StepLifecycle.Conditions;
import at.jku.isse.passiveprocessengine.instance.messages.Commands;
import at.jku.isse.passiveprocessengine.instance.messages.Commands.ConditionChangedCmd;
import at.jku.isse.passiveprocessengine.instance.messages.Commands.IOMappingConsistencyCmd;
import at.jku.isse.passiveprocessengine.instance.messages.Commands.OutputChangedCmd;
import at.jku.isse.passiveprocessengine.instance.messages.Commands.PrematureStepTriggerCmd;
import at.jku.isse.passiveprocessengine.instance.messages.Commands.ProcessScopedCmd;
import at.jku.isse.passiveprocessengine.instance.messages.Commands.QAConstraintChangedCmd;
import at.jku.isse.passiveprocessengine.instance.messages.EventDistributor;
import at.jku.isse.passiveprocessengine.instance.messages.Events;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
//@Service
public class ProcessInstanceChangeProcessor implements WorkspaceListener {

	private AtomicLong latestTransaction = new AtomicLong(0);
	Workspace ws;
	EventDistributor distributor = null;
	//Map<Id, String> instanceIndex = Collections.synchronizedMap(new HashMap<>());
	//we queue commands and remove some that are undone when lazy fetching of artifacts results in different outcome 
	protected Map<String, Commands.ProcessScopedCmd> cmdQueue = Collections.synchronizedMap(new HashMap<>());
	
	private EventStats stats = new EventStats();
	
	public ProcessInstanceChangeProcessor(Workspace ws, EventDistributor distributor) {
		this.ws = ws;
		this.distributor = distributor;
	}

	public EventStats getEventStatistics() {
		return stats;
	}
	
	private boolean isOfStepType(Id id) {
		if (id == null) return false;
		Element el = ws.findElement(id);
		if (el instanceof Instance) {
			return el.getInstanceType().isKindOf(ProcessStep.getOrCreateDesignSpaceCoreSchema(ws));
		} 
		return false;
		//String type = instanceIndex.getOrDefault(id, "NOTFOUND");
		//return (type.startsWith(ProcessStep.designspaceTypeId) || type.startsWith(ProcessInstance.designspaceTypeId));
	}
	
	private boolean isOfCRDType(Id id) {
		if (id == null) return false;		
		Element el = ws.findElement(id);
		return (el instanceof ConsistencyRule && el.getInstanceType().name().startsWith("crd_"));
		//return instanceIndex.getOrDefault(id, "NOTFOUND").startsWith("crd"); //FIXME better approach than naming
	}
	
//	private void processElementCreate(ElementCreate op, Element element) {
//		if (element.getInstanceType() == null)
//			return;
//		// check if new type, if so update typeIndex
//		if (element.getInstanceType().id().value() == 2l) {
//			// new type
//			//typeIndex.put(element.id(), element.getPropertyAsSingle("name").value.toString());
//		} else if (element.getInstanceType().id().value() == 3l) { 
//			// ignore creation of a property
//			return;
//		} else {
//			// new instance
//			Id typeId = element.getInstanceType().id();
//			 ws.debugInstanceTypes().stream()
//			 	.filter(type -> !type.isDeleted)
//			 	.filter(type -> type.id().equals(typeId))
//			 	.forEach(type -> instanceIndex.put(element.id(), type.name()));
//		}
//	}
	
	private Optional<ProcessScopedCmd> processPropertyUpdateAdd(PropertyUpdateAdd op, Element element) {
		// check if this is about an instance
		//if (!instanceIndex.containsKey(op.elementId()))
		//	return Optional.empty();;
		// now lets check if this is about a step, 
		//if (isOfStepType(element.id())) {
				
				if(!op.name().contains("/@") 
					&& (op.name().startsWith("in_") 
					      || op.name().startsWith("out_"))
					&& isOfStepType(element.id()) ) {
					ProcessStep step = WrapperCache.getWrappedInstance(ProcessStep.class, (Instance)element);
					Id addedId = (Id) op.value();
					Element added = ws.findElement(addedId);
					log.debug(String.format("%s %s now also contains %s", element.name(),
																op.name(),
																added != null ? added.name() : "NULL"
																));
					stats.incrementIoAddEventCount();
					return Optional.ofNullable(step.prepareIOAddEvent(op));
				}
		//}		
		return Optional.empty();
	}
	
	private Optional<ProcessScopedCmd> processPropertyUpdateRemove(PropertyUpdateRemove op, Element element) {
		// check if this is about an instance
		//if (!instanceIndex.containsKey(op.elementId()))
		//	return Optional.empty();;
		// now lets check if this is about a step, and if so about input
	//	if (isOfStepType(element.id())) {
	//		if (op.name().startsWith("in_")) {
//				Id remId = (Id) op.value(); // IS NOT SET, returns NULL if remove is called via index
	//			Element rem = ws.findElement(remId);
			if(!op.name().contains("/@")  // ignore special properties  (e.g., usage in consistency rules, etc)
				&& (op.name().startsWith("in_") 
				      || op.name().startsWith("out_"))
				&& isOfStepType(element.id()) ) {
				ProcessStep step = WrapperCache.getWrappedInstance(ProcessStep.class, (Instance)element);
				log.info(String.format("%s %s removed %s", element.name(),
																op.name(),
																op.indexOrKey()
																));	
				stats.incrementIoRemoveEventCount();
				return Optional.ofNullable(step.prepareIORemoveEvent(op));
			}
	//	}
		return Optional.empty();
	}
	
	private Optional<ProcessScopedCmd> processPropertyUpdateSet(PropertyUpdateSet op, Element element) {
		if (isOfStepType(element.id())) {
			log.info(String.format("Step %s updated %s to %s", element.name(),
					op.name(),
					op.value().toString()
					));	
		} else if (isOfCRDType(element.id()) && op.name().equals("result")) {
			// FIXME: we also have to check if a rule no longer has any error, only then can we process it properly
			ConsistencyRule cr = (ConsistencyRule)element;
			Instance context = cr.contextInstance();
			if (isOfStepType(context.id())) { // rule belonging to a step,
				ProcessStep step = WrapperCache.getWrappedInstance(ProcessStep.class, context);
				stats.incrementRuleUpdateEventCount();
				ProcessScopedCmd effect = step.prepareRuleEvaluationChange(cr, op);
//				log.debug(String.format("CRD of type %s for step %s updated %s to %s", element.name(), context.name(),
//						op.name(),
//						op.value().toString()
//						));	
				return Optional.ofNullable(effect);
			}
		}
		return Optional.empty();
	}

	@Override
	public void handleUpdated(Collection<Operation> operations) {
		handleUpdates(operations);
	}
	
	private void checkTransactionId(long id) {
		long local = latestTransaction.getAcquire();
		if (local > id) { 
			log.error(String.format("Encountered Operation with an outdated transaction id: top current known id %s vs obtained id %s", local, id));
		}
		if (latestTransaction.getAcquire() < id) {
			latestTransaction.set(id);
		}
	}
	
	protected Set<ProcessInstance> handleUpdates(Collection<Operation> operations) {
		@SuppressWarnings("unchecked")
		
		List<ProcessScopedCmd> queuedEffects = (List<ProcessScopedCmd>) operations.stream()
		 .map(operation -> {
//			checkTransactionId(operation.getConclusionId());
 			Element element = ws.findElement(operation.elementId());
				if (operation instanceof PropertyUpdateAdd) {
					 return processPropertyUpdateAdd((PropertyUpdateAdd) operation, element);
				} else 
					if (operation instanceof PropertyUpdateRemove) {
						 return processPropertyUpdateRemove((PropertyUpdateRemove) operation, element);
					} else
						if (operation instanceof PropertyUpdateSet) {
							return processPropertyUpdateSet((PropertyUpdateSet) operation, element);
						}// else
			//				if (operation instanceof PropertyCreate) { 
								// no logging
			//				} //else 			
//			log.debug(String.format("Element %s %s <%s> changed: %s", 
//					element.id(), 
//					element.name(),
//					element.getInstanceType() != null ? element.getInstanceType().id() : "NoInstanceType",
//							operation.getClass().getSimpleName()));
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
			ws.concludeTransaction(); 
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
