package at.jku.isse.passiveprocessengine.definition.factories;

import at.jku.isse.passiveprocessengine.Context;
import at.jku.isse.passiveprocessengine.core.DomainTypesRegistry;
import at.jku.isse.passiveprocessengine.core.Instance;
import at.jku.isse.passiveprocessengine.core.InstanceRepository;
import at.jku.isse.passiveprocessengine.definition.activeobjects.MappingDefinition;

public class MappingDefinitionFactory {

	InstanceRepository repository;
	Context wrapperCache;
	DomainTypesRegistry typesFactory;
	
	public MappingDefinitionFactory(InstanceRepository repository, Context wrapperCache, DomainTypesRegistry typesFactory) {
		this.repository = repository;
		this.wrapperCache = wrapperCache;
		this.typesFactory = typesFactory;
	}
	
	public MappingDefinition getInstance(String fromStepType, String fromParameter, String toStepType, String toParameter) {
		Instance instance = repository.createInstance(fromStepType+fromParameter+toStepType+toParameter, typesFactory.getType(MappingDefinition.class));
		MappingDefinition md = wrapperCache.getWrappedInstance(MappingDefinition.class, instance);
		md.setFromStepType(fromStepType);
		md.setFromParameter(fromParameter);
		md.setToStepType(toStepType);
		md.setToParameter(toParameter);
		return md;
	}
}
