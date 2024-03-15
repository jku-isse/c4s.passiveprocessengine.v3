package at.jku.isse.passiveprocessengine.definition.factories;

import at.jku.isse.passiveprocessengine.core.FactoryIndex.DomainFactory;
import at.jku.isse.passiveprocessengine.core.ProcessContext;
import at.jku.isse.passiveprocessengine.core.PPEInstance;
import at.jku.isse.passiveprocessengine.definition.activeobjects.StepDefinition;

public class StepDefinitionFactory extends DomainFactory {
	
	public StepDefinitionFactory(ProcessContext context) {
		super(context);
	}
	
	public StepDefinition createInstance(String stepId) {
		PPEInstance instance = getContext().getInstanceRepository().createInstance(stepId, getContext().getSchemaRegistry().getType(StepDefinition.class));
		return getContext().getWrappedInstance(StepDefinition.class, instance);
	}
}
