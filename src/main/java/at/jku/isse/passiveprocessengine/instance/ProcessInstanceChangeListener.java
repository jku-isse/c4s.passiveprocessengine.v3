package at.jku.isse.passiveprocessengine.instance;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import at.jku.isse.designspace.sdk.core.model.Element;
import at.jku.isse.designspace.sdk.core.model.Id;
import at.jku.isse.designspace.sdk.core.model.Workspace;
import at.jku.isse.designspace.sdk.core.model.WorkspaceListener;
import at.jku.isse.designspace.sdk.core.operations.ElementCreate;
import at.jku.isse.designspace.sdk.core.operations.Operation;
import at.jku.isse.designspace.sdk.core.operations.PropertyCreate;
import at.jku.isse.designspace.sdk.core.operations.PropertyUpdateAdd;
import at.jku.isse.designspace.sdk.core.operations.PropertyUpdateRemove;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ProcessInstanceChangeListener extends WorkspaceListener {

	Workspace ws;
	// refactor this out later into a schema cache
	Map<Id, String> typeIndex = new HashMap<>();
	Map<Id, String> instanceIndex = new HashMap<>();
	
	
	public ProcessInstanceChangeListener(Workspace ws) {
		ws.addListener(this);
		this.ws = ws;
		// init typeIndex
		ws.debugInstanceTypes().stream()
			.forEach(it -> typeIndex.put(it.id(), it.name()));
	}

	private boolean isOfStepType(Id id) {
		return instanceIndex.getOrDefault(id, "NOTFOUND").equals(ProcessStep.designspaceTypeId);
	}
	
	private void processElementCreate(ElementCreate op, Element element) {
		if (element.instanceType() == null)
			return;
		// check if new type, if so update typeIndex
		if (element.instanceType().id().value() == 2l) {
			// new type
			typeIndex.put(element.id(), element.propertyAsSingle("name").value.toString());
		} else if (element.instanceType().id().value() == 3l) { 
			// ignore creation of a property
			return;
		} else {
			// new instance
			Id typeId = element.instanceType().id();
			instanceIndex.put(element.id(), typeIndex.get(typeId));
		}
	}
	
	private void processPropertyUpdateAdd(PropertyUpdateAdd op, Element element) {
		// check if this is about an instance
		if (!instanceIndex.containsKey(op.elementId))
			return;
		// now lets check if this is about a step, and if so about input
		if (isOfStepType(element.id())) {
			if (op.name().startsWith("in.")) {
				Id addedId = (Id) op.value();
				Element added = ws.findElement(addedId);
				log.info(String.format("%s %s now also contains %s", element.name(),
																op.name(),
																added.name()
																));		
			}
		}
	}
	
	private void processPropertyUpdateRemove(PropertyUpdateRemove op, Element element) {
		// check if this is about an instance
		if (!instanceIndex.containsKey(op.elementId))
			return;
		// now lets check if this is about a step, and if so about input
		if (isOfStepType(element.id())) {
			if (op.name().startsWith("in.")) {
	//			Id remId = (Id) op.value(); // IS NOT SET, returns NULL if remove is called via index
	//			Element rem = ws.findElement(remId);
				log.info(String.format("%s %s removed %s", element.name(),
																op.name(),
																op.indexOrKey()
																));		
			}
		}
	}
	
	@Override
	public void elementChanged(Element element, Operation operation) {
		super.elementChanged(element, operation);
		
		
		if (operation instanceof ElementCreate) {
			// update type and instance index
			processElementCreate((ElementCreate) operation, element);
		} else
		if (operation instanceof PropertyUpdateAdd) {
			processPropertyUpdateAdd((PropertyUpdateAdd) operation, element);
		} else 
		if (operation instanceof PropertyUpdateRemove) {
				processPropertyUpdateRemove((PropertyUpdateRemove) operation, element);
		}
		log.debug(String.format("Element %s %s <%s> changed: %s", 
					element.id(), 
					element.name(),
					element.instanceType() != null ? element.instanceType().id() : "NoInstanceType",
					operation.getClass().getSimpleName()));
		
	}

	@Override
	public void workspaceUpdated(Workspace workspace, List<Operation> operations) {
		
		super.workspaceUpdated(workspace, operations);
		operations.stream()
			.filter(op -> !(op instanceof PropertyCreate))
			.count()
			;
			
		// we are interested in new output due to executed mapping rules --> we need to know that output element
		// we are interested in rule evaluation state change --> translated into pre/post/act/cancel commands
		
		
//		operations.forEach(op -> {
//			log.debug(String.format("Element %s changed: %s", op.elementId(), op.getClass().getSimpleName()));
//		});
	}
}
