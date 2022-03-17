package at.jku.isse.passiveprocessengine.instance;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import at.jku.isse.designspace.core.events.ElementCreate;
import at.jku.isse.designspace.core.events.Operation;
import at.jku.isse.designspace.core.events.PropertyCreate;
import at.jku.isse.designspace.core.events.PropertyUpdateAdd;
import at.jku.isse.designspace.core.events.PropertyUpdateRemove;
import at.jku.isse.designspace.core.events.PropertyUpdateSet;
import at.jku.isse.designspace.core.model.Element;
import at.jku.isse.designspace.core.model.Id;
import at.jku.isse.designspace.core.model.Instance;
import at.jku.isse.designspace.core.model.InstanceType;
import at.jku.isse.designspace.core.model.ServiceListener;
import at.jku.isse.designspace.core.model.Workspace;
import at.jku.isse.designspace.core.model.WorkspaceListener;
import at.jku.isse.designspace.core.service.WorkspaceService;
import at.jku.isse.designspace.rule.model.ConsistencyRule;
import at.jku.isse.passiveprocessengine.WrapperCache;
import at.jku.isse.passiveprocessengine.instance.StepLifecycle.Conditions;
import at.jku.isse.passiveprocessengine.instance.commands.Commands.ConditionChangedCmd;
import at.jku.isse.passiveprocessengine.instance.commands.Commands.IOMappingInconsistentCmd;
import at.jku.isse.passiveprocessengine.instance.commands.Commands.QAConstraintChangedCmd;
import at.jku.isse.passiveprocessengine.instance.commands.Commands.TrackableCmd;
import lombok.extern.slf4j.Slf4j;

@Slf4j
//@Service
public class ProcessInstanceChangeProcessor implements WorkspaceListener {

	Workspace ws;
	// refactor this out later into a schema cache
	Map<Id, String> instanceIndex = new HashMap<>();
	

	
	public ProcessInstanceChangeProcessor(Workspace ws) {
		this.ws = ws;
		ws.workspaceListeners.add( this);
	}

	private boolean isOfStepType(Id id) {
		if (id == null) return false;
		return instanceIndex.getOrDefault(id, "NOTFOUND").startsWith(ProcessStep.designspaceTypeId);
	}
	
	private boolean isOfCRDType(Id id) {
		if (id == null) return false;
		return instanceIndex.getOrDefault(id, "NOTFOUND").startsWith("crd"); //FIXME better approach than naming
	}
	
	private void processElementCreate(ElementCreate op, Element element) {
		if (element.getInstanceType() == null)
			return;
		// check if new type, if so update typeIndex
		if (element.getInstanceType().id().value() == 2l) {
			// new type
			//typeIndex.put(element.id(), element.getPropertyAsSingle("name").value.toString());
		} else if (element.getInstanceType().id().value() == 3l) { 
			// ignore creation of a property
			return;
		} else {
			// new instance
			Id typeId = element.getInstanceType().id();
			 ws.debugInstanceTypes().stream()
			 	.filter(type -> type.id().equals(typeId))
			 	.forEach(type -> instanceIndex.put(element.id(), type.name()));
		}
	}
	
	private void processPropertyUpdateAdd(PropertyUpdateAdd op, Element element) {
		// check if this is about an instance
		if (!instanceIndex.containsKey(op.elementId()))
			return;
		// now lets check if this is about a step, 
		if (isOfStepType(element.id())) {
				Id addedId = (Id) op.value();
				Element added = ws.findElement(addedId);
				if(!op.name().contains("/@") 
						&& (op.name().startsWith("in_") 
						      || op.name().startsWith("out_") )) {
					ProcessStep step = WrapperCache.getWrappedInstance(ProcessStep.class, (Instance)element);
					step.processIOAddEvent(op);
					log.debug(String.format("%s %s now also contains %s", element.name(),
																op.name(),
																added != null ? added.name() : "NULL"
																));	
				}
		}		
	}
	
	private void processPropertyUpdateRemove(PropertyUpdateRemove op, Element element) {
		// check if this is about an instance
		if (!instanceIndex.containsKey(op.elementId()))
			return;
		// now lets check if this is about a step, and if so about input
		if (isOfStepType(element.id())) {
	//		if (op.name().startsWith("in_")) {
	//			Id remId = (Id) op.value(); // IS NOT SET, returns NULL if remove is called via index
	//			Element rem = ws.findElement(remId);
			if(!op.name().contains("/@")  // ignore special properties  (e.g., usage in consistency rules, etc)
					&& (op.name().startsWith("in_") 
					      || op.name().startsWith("out_") )) {
				ProcessStep step = WrapperCache.getWrappedInstance(ProcessStep.class, (Instance)element);
				step.processIORemoveEvent(op);
				log.info(String.format("%s %s removed %s", element.name(),
																op.name(),
																op.indexOrKey()
																));	
			}
		}
	}
	
	private Optional<TrackableCmd> processPropertyUpdateSet(PropertyUpdateSet op, Element element) {
		if (isOfStepType(element.id())) {
			log.info(String.format("Step %s updated %s to %s", element.name(),
					op.name(),
					op.value().toString()
					));	
		} else if (isOfCRDType(element.id()) && op.name().equals("result")) {
			ConsistencyRule cr = (ConsistencyRule)element;
			Instance context = cr.contextInstance();
			if (isOfStepType(context.id())) { // rule belonging to a step,
				ProcessStep step = WrapperCache.getWrappedInstance(ProcessStep.class, context);
				TrackableCmd effect = step.processRuleEvaluationChange(cr, op);
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
	public void handleUpdated(List<Operation> operations) {
		@SuppressWarnings("unchecked")
		List<TrackableCmd> effects = (List<TrackableCmd>) operations.stream()
		 .map(operation -> {
 			Element element = ws.findElement(operation.elementId());
			if (operation instanceof ElementCreate) {
				// update type and instance index
				processElementCreate((ElementCreate) operation, element);
			} else
				if (operation instanceof PropertyUpdateAdd) {
					processPropertyUpdateAdd((PropertyUpdateAdd) operation, element);
				} else 
					if (operation instanceof PropertyUpdateRemove) {
						processPropertyUpdateRemove((PropertyUpdateRemove) operation, element);
					} else
						if (operation instanceof PropertyUpdateSet) {
							return processPropertyUpdateSet((PropertyUpdateSet) operation, element);
							
						} else
							if (operation instanceof PropertyCreate) { 
								// no logging
							} else 
			//if (element.getInstanceType() != null && element.getInstanceType().id().value() == 137) {
			log.debug(String.format("Element %s %s <%s> changed: %s", 
					element.id(), 
					element.name(),
					element.getInstanceType() != null ? element.getInstanceType().id() : "NoInstanceType",
							operation.getClass().getSimpleName()));
		//	}
			return Optional.empty();
		})
		.filter(Optional::isPresent)
		.map(opt -> opt.get())
		.collect(Collectors.toList());
		
		// now lets just execute all commands
		if (effects.size() > 0) {
			// qa not fulfilled
			effects.stream()
			.filter(QAConstraintChangedCmd.class::isInstance)
			.map(QAConstraintChangedCmd.class::cast)
			.filter(cmd -> !cmd.isFulfilled())
			.peek(cmd -> log.debug(String.format("Executing: %s", cmd.toString())))
			.forEach(cmd -> cmd.execute());
			
			// first sort them to apply them in a sensible order, e.g., preconditions fulfilled before postconditions
			effects.stream()
				.filter(ConditionChangedCmd.class::isInstance)
				.map(ConditionChangedCmd.class::cast)
				.sorted(new CommandComparator())
				.peek(cmd -> log.debug(String.format("Executing: %s", cmd.toString())))
				.forEach(cmd -> cmd.execute());
			
			// if QA is fulfilled
			effects.stream()
			.filter(QAConstraintChangedCmd.class::isInstance)
			.map(QAConstraintChangedCmd.class::cast)
			.filter(cmd -> cmd.isFulfilled())
			.peek(cmd -> log.debug(String.format("Executing: %s", cmd.toString())))
			.forEach(cmd -> cmd.execute());
			
			// then execute datamappings
			effects.stream()
				.filter(IOMappingInconsistentCmd.class::isInstance)
				.map(IOMappingInconsistentCmd.class::cast)
				.peek(cmd -> log.debug(String.format("Executing: %s", cmd.toString())))
				.forEach(cmd -> cmd.execute());
			
			ws.concludeTransaction();
		}
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
					int stepBased = o1.getStep().getName().compareTo(o2.getStep().getName()); 
					if (stepBased == 0) { // same step
						return Integer.compare(getAssignedValue(o1.getCondition(), o1.isFulfilled()), getAssignedValue(o2.getCondition(), o2.isFulfilled()));
					} else return stepBased; // TODO: compare/sort not by step name but by step order in proc definition 
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
}
