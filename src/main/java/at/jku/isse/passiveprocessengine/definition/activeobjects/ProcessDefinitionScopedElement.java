package at.jku.isse.passiveprocessengine.definition.activeobjects;

import at.jku.isse.passiveprocessengine.InstanceWrapper;
import at.jku.isse.passiveprocessengine.WrapperCache;
import at.jku.isse.passiveprocessengine.core.Instance;
import at.jku.isse.passiveprocessengine.definition.ProcessDefinition;
import at.jku.isse.passiveprocessengine.definition.types.ProcessDefinitionCoreType;

public abstract class ProcessDefinitionScopedElement extends InstanceWrapper {

	public ProcessDefinitionScopedElement(Instance instance, WrapperCache wrapperCache) {
		super(instance, wrapperCache);
	}

	public void setProcess(ProcessDefinition pi) {
		instance.setSingleProperty(ProcessDefinitionCoreType.CoreProperties.process.toString(), pi.getInstance());
	}

	public void setProcOrderIndex(int index) {
		instance.setSingleProperty(ProcessDefinitionCoreType.CoreProperties.orderIndex.toString(), index);
	}

	public Integer getProcOrderIndex() {
		return instance.getTypedProperty(ProcessDefinitionCoreType.CoreProperties.orderIndex.toString(), Integer.class, -1);
	}

	public ProcessDefinition getProcess() {
		Instance pi = instance.getTypedProperty(ProcessDefinitionCoreType.CoreProperties.process.toString(), Instance.class);
		if (pi != null)
			return wrapperCache.getWrappedInstance(ProcessDefinition.class, pi);
		else return null;
	}

	@Override
	public String toString() {
		return instance.getId();
	}
}
