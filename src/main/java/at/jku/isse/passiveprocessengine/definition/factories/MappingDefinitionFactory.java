package at.jku.isse.passiveprocessengine.definition.factories;

import at.jku.isse.passiveprocessengine.Context;
import at.jku.isse.passiveprocessengine.core.DomainTypesRegistry;
import at.jku.isse.passiveprocessengine.core.Instance;
import at.jku.isse.passiveprocessengine.core.InstanceRepository;
import at.jku.isse.passiveprocessengine.core.FactoryIndex.DomainFactory;
import at.jku.isse.passiveprocessengine.definition.activeobjects.MappingDefinition;

public class MappingDefinitionFactory extends DomainFactory{
	
	public MappingDefinitionFactory(Context context) {
		super(context);
	}
	
	public MappingDefinition getInstance(String fromStepType, String fromParameter, String toStepType, String toParameter) {
		Instance instance = getContext().getInstanceRepository().createInstance(fromStepType+fromParameter+toStepType+toParameter
				, getContext().getSchemaRegistry().getType(MappingDefinition.class));
		MappingDefinition md = getContext().getWrappedInstance(MappingDefinition.class, instance);
		md.setFromStepType(fromStepType);
		md.setFromParameter(fromParameter);
		md.setToStepType(toStepType);
		md.setToParameter(toParameter);
		return md;
	}
}
