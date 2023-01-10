package at.jku.isse.passiveprocessengine;

import java.util.Optional;

import at.jku.isse.designspace.core.model.Cardinality;
import at.jku.isse.designspace.core.model.Instance;
import at.jku.isse.designspace.core.model.InstanceType;
import at.jku.isse.designspace.core.model.Workspace;
import at.jku.isse.passiveprocessengine.instance.ProcessInstance;

public abstract class ProcessInstanceScopedElement extends InstanceWrapper {

	static enum CoreProperties {process};
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
	
	public static InstanceType getOrCreateDesignSpaceCoreSchema(Workspace ws) {
		Optional<InstanceType> thisType = ws.debugInstanceTypes().stream()
			.filter(it -> it.name().equals(designspaceTypeId))
			.findAny();
		if (thisType.isPresent())
			return thisType.get();
		else {
			InstanceType typeStep = ws.createInstanceType(designspaceTypeId, ws.TYPES_FOLDER);
			typeStep.createPropertyType(CoreProperties.process.toString(), Cardinality.SINGLE, typeStep);
			return typeStep;
		}
	}
}
