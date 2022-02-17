package at.jku.isse.passiveprocessengine.instance;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import at.jku.isse.designspace.core.events.ElementCreate;
import at.jku.isse.designspace.core.events.Operation;
import at.jku.isse.designspace.core.events.PropertyCreate;
import at.jku.isse.designspace.core.events.PropertyUpdateAdd;
import at.jku.isse.designspace.core.events.PropertyUpdateRemove;
import at.jku.isse.designspace.core.events.PropertyUpdateSet;
import at.jku.isse.designspace.core.model.Element;
import at.jku.isse.designspace.core.model.Id;
import at.jku.isse.designspace.core.model.ServiceListener;
import at.jku.isse.designspace.core.model.Workspace;
import at.jku.isse.designspace.core.model.WorkspaceListener;
import at.jku.isse.designspace.core.service.WorkspaceService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ProcessInstanceChangeProcessor implements WorkspaceListener {// ServiceListener {

	Workspace ws;
	// refactor this out later into a schema cache
	Map<Id, String> typeIndex = new HashMap<>();
	Map<Id, String> instanceIndex = new HashMap<>();
	
	
	public ProcessInstanceChangeProcessor(WorkspaceService workspaceService) {
		//ws.addListener(this);
		this.ws = WorkspaceService.PUBLIC_WORKSPACE;
		// init typeIndex
		ws.debugInstanceTypes().stream()
			.forEach(it -> typeIndex.put(it.id(), it.name()));
		//Workspace.serviceListeners.add(this);
		WorkspaceService.subscribeToWorkspace(ws, this);
	}

	private boolean isOfStepType(Id id) {
		return instanceIndex.getOrDefault(id, "NOTFOUND").startsWith(ProcessStep.designspaceTypeId);
	}
	
	private void processElementCreate(ElementCreate op, Element element) {
		if (element.getInstanceType() == null)
			return;
		// check if new type, if so update typeIndex
		if (element.getInstanceType().id().value() == 2l) {
			// new type
			typeIndex.put(element.id(), element.getPropertyAsSingle("name").value.toString());
		} else if (element.getInstanceType().id().value() == 3l) { 
			// ignore creation of a property
			return;
		} else {
			// new instance
			Id typeId = element.getInstanceType().id();
			instanceIndex.put(element.id(), typeIndex.get(typeId));
		}
	}
	
	private void processPropertyUpdateAdd(PropertyUpdateAdd op, Element element) {
		// check if this is about an instance
	//	if (!instanceIndex.containsKey(op.elementId()))
	//		return;
		// now lets check if this is about a step, and if so about input
		if (isOfStepType(element.id())) {
			if (op.name().startsWith("in_")) {
				Id addedId = (Id) op.value();
				Element added = ws.findElement(addedId);
				log.info(String.format("%s %s now also contains %s", element.name(),
																op.name(),
																added != null ? added.name() : "NULL"
																));		
			}
		} else {
			log.info(op.toString());
		}
			
	}
	
	private void processPropertyUpdateRemove(PropertyUpdateRemove op, Element element) {
		// check if this is about an instance
	//	if (!instanceIndex.containsKey(op.elementId()))
	//		return;
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
		} else {
			log.info(op.toString());
		}
	}
	
	private void processPropertyUpdateSet(PropertyUpdateSet op, Element element) {
		log.info(String.format("%s updated %s to %s", element.name(),
				op.name(),
				op.value().toString()
				));	
	}
	
//	@Override
//	public void handleServiceRequest(Workspace workspace, List<Operation> operations) {
//		operations.stream().forEach(operation -> {
// 			Element element = workspace.findElement(operation.elementId());
//			if (operation instanceof ElementCreate) {
//				// update type and instance index
//				processElementCreate((ElementCreate) operation, element);
//			} else
//				if (operation instanceof PropertyUpdateAdd) {
//					processPropertyUpdateAdd((PropertyUpdateAdd) operation, element);
//				} else 
//					if (operation instanceof PropertyUpdateRemove) {
//						processPropertyUpdateRemove((PropertyUpdateRemove) operation, element);
//					} else
//						if (operation instanceof PropertyUpdateSet) {
//							processPropertyUpdateSet((PropertyUpdateSet) operation, element);
//						} else
//							if (operation instanceof PropertyCreate) { 
//								// no logging
//							} else 
//			//if (element.getInstanceType() != null && element.getInstanceType().id().value() == 137) {
//			log.debug(String.format("Element %s %s <%s> changed: %s", 
//					element.id(), 
//					element.name(),
//					element.getInstanceType() != null ? element.getInstanceType().id() : "NoInstanceType",
//							operation.getClass().getSimpleName()));
//		//	}
//		});
//	}

	@Override
	public void handleUpdated(List<Operation> operations) {
		operations.stream().forEach(operation -> {
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
							processPropertyUpdateSet((PropertyUpdateSet) operation, element);
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
		});
		
	}
}
