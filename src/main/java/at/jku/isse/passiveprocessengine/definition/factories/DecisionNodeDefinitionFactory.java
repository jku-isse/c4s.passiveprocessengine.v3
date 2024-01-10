package at.jku.isse.passiveprocessengine.definition.factories;

import at.jku.isse.passiveprocessengine.Context;
import at.jku.isse.passiveprocessengine.core.Instance;
import at.jku.isse.passiveprocessengine.core.InstanceRepository;
import at.jku.isse.passiveprocessengine.definition.activeobjects.DecisionNodeDefinition;
import at.jku.isse.passiveprocessengine.definition.activeobjects.DecisionNodeDefinition.InFlowType;
import at.jku.isse.passiveprocessengine.definition.types.DecisionNodeDefinitionType;
import at.jku.isse.passiveprocessengine.definition.types.ProcessDomainTypesRegistry;

public class DecisionNodeDefinitionFactory {
	InstanceRepository repository;
	Context wrapperCache;
	ProcessDomainTypesRegistry typesFactory;
	
	public DecisionNodeDefinitionFactory(InstanceRepository repository, Context wrapperCache, ProcessDomainTypesRegistry typesFactory) {
		this.repository = repository;
		this.wrapperCache = wrapperCache;
		this.typesFactory = typesFactory;
	}

	public DecisionNodeDefinition createInstance(String dndId) {
		Instance instance = repository.createInstance(dndId, typesFactory.getType(DecisionNodeDefinition.class));
		// default SEQ
		instance.setSingleProperty(DecisionNodeDefinitionType.CoreProperties.inFlowType.toString(), InFlowType.SEQ.toString());
		instance.setSingleProperty(DecisionNodeDefinitionType.CoreProperties.hierarchyDepth.toString(), -1);
		return wrapperCache.getWrappedInstance(DecisionNodeDefinition.class, instance);
	}
	
	
}
