package at.jku.isse.passiveprocessengine.definition.factories;

import at.jku.isse.passiveprocessengine.WrapperCache;
import at.jku.isse.passiveprocessengine.core.Instance;
import at.jku.isse.passiveprocessengine.core.InstanceRepository;
import at.jku.isse.passiveprocessengine.definition.activeobjects.MappingDefinition;
import at.jku.isse.passiveprocessengine.definition.types.ProcessDomainTypesFactory;

public class MappingDefinitionFactory {

	InstanceRepository repository;
	WrapperCache wrapperCache;
	ProcessDomainTypesFactory typesFactory;
	
	public MappingDefinitionFactory(InstanceRepository repository, WrapperCache wrapperCache, ProcessDomainTypesFactory typesFactory) {
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
