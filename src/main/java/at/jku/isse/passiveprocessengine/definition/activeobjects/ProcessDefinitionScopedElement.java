package at.jku.isse.passiveprocessengine.definition.activeobjects;

import at.jku.isse.passiveprocessengine.core.InstanceWrapper;
import at.jku.isse.passiveprocessengine.core.ProcessContext;
import at.jku.isse.passiveprocessengine.core.PPEInstance;
import at.jku.isse.passiveprocessengine.definition.types.ProcessDefinitionScopeType;

public abstract class ProcessDefinitionScopedElement extends InstanceWrapper {

	public ProcessDefinitionScopedElement(PPEInstance instance, ProcessContext context) {
		super(instance, context);
	}

	public void setProcess(ProcessDefinition pi) {
		instance.setSingleProperty(ProcessDefinitionScopeType.CoreProperties.processDefinition.toString(), pi.getInstance());
	}

	public void setProcOrderIndex(int index) {
		instance.setSingleProperty(ProcessDefinitionScopeType.CoreProperties.orderIndex.toString(), index);
	}

	public Integer getProcOrderIndex() {
		return instance.getTypedProperty(ProcessDefinitionScopeType.CoreProperties.orderIndex.toString(), Integer.class, -1);
	}

	public ProcessDefinition getProcess() {
		PPEInstance pi = instance.getTypedProperty(ProcessDefinitionScopeType.CoreProperties.processDefinition.toString(), PPEInstance.class);
		if (pi != null)
			return context.getWrappedInstance(ProcessDefinition.class, pi);
		else return null;
	}
	
	protected ProcessContext getProcessContext() {
		return (ProcessContext) context;
	}

	@Override
	public String toString() {
		return instance.getName();
	}
}
