package at.jku.isse.passiveprocessengine.instance;

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
import at.jku.isse.passiveprocessengine.instance.commands.Commands.TrackableCmd;
import lombok.extern.slf4j.Slf4j;

@Slf4j
//@Service
public class ProcessInstanceChangeProcessor implements WorkspaceListener {

	Workspace ws;
	// refactor this out later into a schema cache
	//Map<Id, String> typeIndex = new HashMap<>();
	Map<Id, String> instanceIndex = new HashMap<>();
	
	
//	public ProcessInstanceChangeProcessor(WorkspaceService workspaceService) {
//		//ws.addListener(this);
//		this.ws = WorkspaceService.PUBLIC_WORKSPACE;
//		
//		// init typeIndex
//		ws.debugInstanceTypes().stream()
//			.forEach(it -> typeIndex.put(it.id(), it.name()));
//		//Workspace.serviceListeners.add(this);
//		WorkspaceService.subscribeToWorkspace(ws, this);
//	}
	
	public ProcessInstanceChangeProcessor(Workspace ws) {
		//ws.addListener(this);
		this.ws = ws;
		
		// init typeIndex
		//ws.debugInstanceTypes().stream()
		//	.forEach(it -> typeIndex.put(it.id(), it.name()));
		//Workspace.serviceListeners.add(this);
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
			
			
//			if (typeId != null && typeIndex.get(typeId) != null)
//				instanceIndex.put(element.id(), typeIndex.get(typeId));
//			else {
//				// unknown type
//				
//				InstanceType it = element.getInstanceType();
//				Set<InstanceType> types = ws.debugInstanceTypes();
//				element.id();
//			}
		}
	}
	
	private void processPropertyUpdateAdd(PropertyUpdateAdd op, Element element) {
		// check if this is about an instance
		if (!instanceIndex.containsKey(op.elementId()))
			return;
		// now lets check if this is about a step, and if so about input
		if (isOfStepType(element.id())) {
			//if (op.name().startsWith("in_")) {
				Id addedId = (Id) op.value();
				Element added = ws.findElement(addedId);
				log.info(String.format("%s %s now also contains %s", element.name(),
																op.name(),
																added != null ? added.name() : "NULL"
																));		
			//}
		} //else {
			//log.info(op.toString());
		//}
			
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
				log.info(String.format("%s %s removed %s", element.name(),
																op.name(),
																op.indexOrKey()
																));		
	//		}
		}//else {
//			log.info(op.toString());
//		}
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
				log.info(String.format("CRD of type %s for step %s updated %s to %s", element.name(), context.name(),
						op.name(),
						op.value().toString()
						));	
				return Optional.ofNullable(effect);
			}
		}
		return Optional.empty();
	}

	@Override
	public void handleUpdated(List<Operation> operations) {
		@SuppressWarnings("unchecked")
		List<TrackableCmd> effects = (List<TrackableCmd>) operations.stream().map(operation -> {
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
			effects.stream()
				.peek(cmd -> log.debug(String.format("Executing: %s", cmd.toString())))
				.forEach(cmd -> cmd.execute());
			ws.concludeTransaction();
		}
	}
}
