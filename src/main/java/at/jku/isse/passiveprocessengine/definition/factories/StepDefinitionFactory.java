package at.jku.isse.passiveprocessengine.definition.factories;

import at.jku.isse.passiveprocessengine.core.FactoryIndex.DomainFactory;
import at.jku.isse.passiveprocessengine.rdfwrapper.RDFInstance;
import at.jku.isse.passiveprocessengine.core.ProcessContext;
import at.jku.isse.passiveprocessengine.definition.activeobjects.StepDefinition;
import at.jku.isse.passiveprocessengine.definition.types.ProcessStepDefinitionType;

public class StepDefinitionFactory extends DomainFactory {
	
	public StepDefinitionFactory(ProcessContext context) {
		super(context);
	}
	
	public StepDefinition createInstance(String stepId) {
		RDFInstance instance = getContext().getInstanceRepository().createInstance(stepId, getContext().getSchemaRegistry().getTypeByName(ProcessStepDefinitionType.typeId));
		return getContext().getWrappedInstance(StepDefinition.class, instance);
	}
}
