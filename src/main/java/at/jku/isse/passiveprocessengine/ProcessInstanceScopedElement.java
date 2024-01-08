package at.jku.isse.passiveprocessengine;

import java.util.Optional;

import at.jku.isse.designspace.core.model.Cardinality;
import at.jku.isse.designspace.core.model.Instance;
import at.jku.isse.designspace.core.model.InstanceType;
import at.jku.isse.designspace.core.model.Workspace;
import at.jku.isse.passiveprocessengine.instance.ProcessInstance;

public abstract class ProcessInstanceScopedElement extends InstanceWrapper {

	public static enum CoreProperties {process};
	public static final String designspaceTypeId = ProcessInstanceScopedElement.class.getSimpleName();
	
	
	
	public ProcessInstanceScopedElement(Instance instance) {
		super(instance);
	}

	public void setProcess(ProcessInstance pi) {
		instance.getPropertyAsSingle(CoreProperties.process.toString()).set(pi.getInstance());
	}

	public ProcessInstance getProcess() {
		Instance pi = instance.getPropertyAsInstance(CoreProperties.process.toString());
		if (pi != null)
			return WrapperCache.getWrappedInstance(ProcessInstance.class, pi);
		else return null;
	}
	
	public abstract ProcessDefinitionScopedElement getDefinition();
	
	public static InstanceType getOrCreateDesignSpaceCoreSchema(Workspace ws) {
//		Optional<InstanceType> thisType = ws.debugInstanceTypes().stream()
//			.filter(it -> it.name().equals(designspaceTypeId))
//			.findAny();
		Optional<InstanceType> thisType = Optional.ofNullable(ws.TYPES_FOLDER.instanceTypeWithName(designspaceTypeId)); 
		if (thisType.isPresent())
			return thisType.get();
		else {
			InstanceType typeStep = ws.createInstanceType(designspaceTypeId, ws.TYPES_FOLDER);
			//typeStep.createPropertyType(CoreProperties.process.toString(), Cardinality.SINGLE, typeStep); needs to be add in individual subclasses in order to be able to refine it 
			return typeStep;
		}
	}
	
	public static void addGenericProcessProperty(InstanceType instType) {
		if (instType.getPropertyType(CoreProperties.process.toString()) == null) {
			instType.createPropertyType(CoreProperties.process.toString(), Cardinality.SINGLE, getOrCreateDesignSpaceCoreSchema(instType.workspace));
		}
	}
	
}
