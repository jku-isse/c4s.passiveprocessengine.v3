package at.jku.isse.passiveprocessengine.definition.types;

import java.util.Optional;

import at.jku.isse.passiveprocessengine.core.BuildInType;
import at.jku.isse.passiveprocessengine.core.InstanceType;
import at.jku.isse.passiveprocessengine.core.SchemaRegistry;
import at.jku.isse.passiveprocessengine.definition.activeobjects.MappingDefinition;
import at.jku.isse.passiveprocessengine.definition.activeobjects.ProcessDefinitionScopedElement;
import at.jku.isse.passiveprocessengine.definition.types.MappingDefinitionType.CoreProperties;
import at.jku.isse.passiveprocessengine.definition.types.ProcessDomainTypesFactory.TypeProvider;

public class MappingDefinitionType  implements TypeProvider {

	public static enum CoreProperties {fromStepType, fromParameter, toStepType, toParameter, flowDir};
	private SchemaRegistry schemaRegistry;
	public static final String typeId = MappingDefinitionType.class.getSimpleName();
	
	public MappingDefinitionType(SchemaRegistry schemaRegistry) {
		this.schemaRegistry = schemaRegistry;
	}
	
	@Override
	public void registerTypeInFactory(ProcessDomainTypesFactory factory) {
		Optional<InstanceType> thisType = schemaRegistry.findNonDeletedInstanceTypeById(typeId);
		if (thisType.isPresent())
			factory.registerType(MappingDefinition.class, thisType.get());
		else {
			InstanceType type = schemaRegistry.createNewInstanceType(typeId, factory.getType(ProcessDefinitionScopedElement.class));
			factory.registerType(MappingDefinition.class, type);
			type.createSinglePropertyType(MappingDefinitionType.CoreProperties.fromStepType.toString(), BuildInType.STRING);
			type.createSinglePropertyType(MappingDefinitionType.CoreProperties.fromParameter.toString(), BuildInType.STRING);
			type.createSinglePropertyType(MappingDefinitionType.CoreProperties.toStepType.toString(), BuildInType.STRING);
			type.createSinglePropertyType(MappingDefinitionType.CoreProperties.toParameter.toString(), BuildInType.STRING);
			
		}
	}
}
