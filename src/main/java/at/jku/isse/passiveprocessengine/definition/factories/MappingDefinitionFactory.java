package at.jku.isse.passiveprocessengine.definition.factories;

import at.jku.isse.passiveprocessengine.core.FactoryIndex.DomainFactory;
import at.jku.isse.passiveprocessengine.core.ProcessContext;
import at.jku.isse.passiveprocessengine.core.PPEInstance;
import at.jku.isse.passiveprocessengine.definition.activeobjects.MappingDefinition;
import at.jku.isse.passiveprocessengine.definition.types.MappingDefinitionType;

public class MappingDefinitionFactory extends DomainFactory{
	
	public MappingDefinitionFactory(ProcessContext context) {
		super(context);
	}
	
	public MappingDefinition getInstance(String fromStepType, String fromParameter, String toStepType, String toParameter) {
		PPEInstance instance = getContext().getInstanceRepository().createInstance(fromStepType+fromParameter+toStepType+toParameter
				, getContext().getSchemaRegistry().getTypeByName(MappingDefinitionType.typeId));
		MappingDefinition md = getContext().getWrappedInstance(MappingDefinition.class, instance);
		md.setFromStepType(fromStepType);
		md.setFromParameter(fromParameter);
		md.setToStepType(toStepType);
		md.setToParameter(toParameter);
		return md;
	}
}
