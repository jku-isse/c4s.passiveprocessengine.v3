package at.jku.isse.passiveprocessengine;

import java.util.Optional;

import at.jku.isse.designspace.core.model.Cardinality;
import at.jku.isse.designspace.core.model.Instance;
import at.jku.isse.designspace.core.model.InstanceType;
import at.jku.isse.designspace.core.model.Workspace;
import at.jku.isse.passiveprocessengine.definition.ProcessDefinition;

public abstract class ProcessDefinitionScopedElement extends InstanceWrapper {

	static enum CoreProperties {process, orderIndex};
	public static final String designspaceTypeId = ProcessDefinitionScopedElement.class.getSimpleName();
	
	
	
	public ProcessDefinitionScopedElement(Instance instance) {
		super(instance);
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
			return WrapperCache.getWrappedInstance(ProcessDefinition.class, pi);
		else return null;
	}
	
	public static InstanceType getOrCreateDesignSpaceCoreSchema(Workspace ws) {
//		Optional<InstanceType> thisType = ws.debugInstanceTypes().stream()
//			.filter(it -> it.name().equals(designspaceTypeId))
//			.findAny();
		Optional<InstanceType> thisType = Optional.ofNullable(ws.TYPES_FOLDER.instanceTypeWithName(designspaceTypeId)); 
		if (thisType.isPresent())
			return thisType.get();
		else {
			InstanceType typeStep = ws.createInstanceType(designspaceTypeId, ws.TYPES_FOLDER);
			typeStep.createPropertyType(CoreProperties.process.toString(), Cardinality.SINGLE, typeStep);
			typeStep.createPropertyType((CoreProperties.orderIndex.toString()), Cardinality.SINGLE, Workspace.INTEGER);
			return typeStep;
		}
	}
	
	public String toString() {
		return instance.name(); 
	}
}
