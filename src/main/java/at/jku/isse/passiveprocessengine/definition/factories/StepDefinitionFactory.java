package at.jku.isse.passiveprocessengine.definition.factories;

import at.jku.isse.passiveprocessengine.Context;
import at.jku.isse.passiveprocessengine.core.Instance;
import at.jku.isse.passiveprocessengine.core.InstanceRepository;
import at.jku.isse.passiveprocessengine.core.ProcessDomainTypesRegistry;
import at.jku.isse.passiveprocessengine.definition.activeobjects.StepDefinition;

public class StepDefinitionFactory {
	InstanceRepository repository;
	Context wrapperCache;
	ProcessDomainTypesRegistry typesFactory;
	
	public StepDefinitionFactory(InstanceRepository repository, Context wrapperCache, ProcessDomainTypesRegistry typesFactory) {
		this.repository = repository;
		this.wrapperCache = wrapperCache;
		this.typesFactory = typesFactory;
	}
	
	public StepDefinition createInstance(String stepId) {
		Instance instance = repository.createInstance(stepId, typesFactory.getType(StepDefinition.class));
		return wrapperCache.getWrappedInstance(StepDefinition.class, instance);
	}
}
