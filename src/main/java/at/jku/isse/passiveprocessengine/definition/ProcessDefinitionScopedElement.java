package at.jku.isse.passiveprocessengine.definition;

import java.util.Optional;

import at.jku.isse.passiveprocessengine.InstanceWrapper;
import at.jku.isse.passiveprocessengine.WrapperCache;
import at.jku.isse.passiveprocessengine.core.Instance;
import at.jku.isse.passiveprocessengine.core.InstanceType;

public abstract class ProcessDefinitionScopedElement extends InstanceWrapper {

	public static enum CoreProperties {process, orderIndex}
	public static final String typeId = ProcessDefinitionScopedElement.class.getSimpleName();	

	public ProcessDefinitionScopedElement(Instance instance, WrapperCache wrapperCache) {
		super(instance, wrapperCache);
	}

	public void setProcess(ProcessDefinition pi) {
		instance.getPropertyAsSingle(CoreProperties.process.toString()).set(pi.getInstance());
	}

	public void setProcOrderIndex(int index) {
		instance.getPropertyAsSingle(CoreProperties.orderIndex.toString()).set(index);
	}

	public Integer getProcOrderIndex() {
		return (Integer) instance.getPropertyAsValueOrElse(CoreProperties.orderIndex.toString(), () -> -1);
	}

	public ProcessDefinition getProcess() {
		Instance pi = instance.getPropertyAsInstance(CoreProperties.process.toString());
		if (pi != null)
			return wrapperCache.getWrappedInstance(ProcessDefinition.class, pi);
		else return null;
	}

//	public static InstanceType getOrCreateCoreType(Workspace ws) {
////		Optional<InstanceType> thisType = ws.debugInstanceTypes().stream()
////			.filter(it -> it.name().equals(designspaceTypeId))
////			.findAny();
//		Optional<InstanceType> thisType = Optional.ofNullable(ws.TYPES_FOLDER.instanceTypeWithName(typeId));
//		if (thisType.isPresent())
//			return thisType.get();
//		else {
//			InstanceType typeStep = ws.createInstanceType(typeId, ws.TYPES_FOLDER);
//			typeStep.createPropertyType(CoreProperties.process.toString(), Cardinality.SINGLE, typeStep);
//			typeStep.createPropertyType((CoreProperties.orderIndex.toString()), Cardinality.SINGLE, Workspace.INTEGER);
//			return typeStep;
//		}
//	}

	@Override
	public String toString() {
		return instance.getId();
	}
}
