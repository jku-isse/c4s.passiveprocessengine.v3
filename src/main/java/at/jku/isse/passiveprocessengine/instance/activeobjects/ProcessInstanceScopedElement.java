package at.jku.isse.passiveprocessengine.instance.activeobjects;

import at.jku.isse.passiveprocessengine.Context;
import at.jku.isse.passiveprocessengine.InstanceWrapper;
import at.jku.isse.passiveprocessengine.core.PPEInstance;
import at.jku.isse.passiveprocessengine.definition.activeobjects.ProcessDefinitionScopedElement;
import at.jku.isse.passiveprocessengine.instance.types.ProcessInstanceScopeType;

public abstract class ProcessInstanceScopedElement extends InstanceWrapper {

	public ProcessInstanceScopedElement(PPEInstance instance, Context context) {
		super(instance, context);
	}

	public void setProcess(ProcessInstance pi) {
		instance.setSingleProperty(ProcessInstanceScopeType.CoreProperties.process.toString(), pi.getInstance());
	}

	public ProcessInstance getProcess() {
		PPEInstance pi = instance.getTypedProperty(ProcessInstanceScopeType.CoreProperties.process.toString(), PPEInstance.class);
		if (pi != null)
			return context.getWrappedInstance(ProcessInstance.class, pi);
		else return null;
	}

	public abstract ProcessDefinitionScopedElement getDefinition();

//	public static InstanceType getOrCreateDesignSpaceCoreSchema(Workspace ws) {
////		Optional<InstanceType> thisType = ws.debugInstanceTypes().stream()
////			.filter(it -> it.name().equals(designspaceTypeId))
////			.findAny();
//		Optional<InstanceType> thisType = Optional.ofNullable(ws.TYPES_FOLDER.instanceTypeWithName(ProcessInstanceScopeType.designspaceTypeId));
//		if (thisType.isPresent())
//			return thisType.get();
//		else {
//			InstanceType typeStep = ws.createInstanceType(ProcessInstanceScopeType.designspaceTypeId, ws.TYPES_FOLDER);
//			//typeStep.createPropertyType(CoreProperties.process.toString(), Cardinality.SINGLE, typeStep); needs to be add in individual subclasses in order to be able to refine it
//			return typeStep;
//		}
//	}


}
