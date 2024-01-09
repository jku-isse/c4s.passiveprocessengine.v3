package at.jku.isse.passiveprocessengine.definition.factories;

import at.jku.isse.passiveprocessengine.Context;
import at.jku.isse.passiveprocessengine.core.Instance;
import at.jku.isse.passiveprocessengine.core.InstanceRepository;
import at.jku.isse.passiveprocessengine.definition.activeobjects.ProcessDefinition;
import at.jku.isse.passiveprocessengine.definition.types.ProcessDefinitionType;
import at.jku.isse.passiveprocessengine.definition.types.ProcessDomainTypesFactory;

public class ProcessDefinitionFactory {
	InstanceRepository repository;
	Context wrapperCache;
	ProcessDomainTypesFactory typesFactory;
	
	public ProcessDefinitionFactory(InstanceRepository repository, Context wrapperCache, ProcessDomainTypesFactory typesFactory) {
		this.repository = repository;
		this.wrapperCache = wrapperCache;
		this.typesFactory = typesFactory;
	}
	
	public ProcessDefinition createInstance(String stepId) {
		Instance instance = repository.createInstance(stepId, typesFactory.getType(ProcessDefinition.class));
		instance.setSingleProperty(ProcessDefinitionType.CoreProperties.isWithoutBlockingErrors.toString(), false);
		return wrapperCache.getWrappedInstance(ProcessDefinition.class, instance);
	}
	
	
	
}
