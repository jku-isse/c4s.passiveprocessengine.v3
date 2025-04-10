package at.jku.isse.passiveprocessengine.definition.types;

import java.util.Optional;

import at.jku.isse.passiveprocessengine.core.BuildInType;
import at.jku.isse.passiveprocessengine.rdfwrapper.RDFInstanceType;
import at.jku.isse.passiveprocessengine.core.SchemaRegistry;
import at.jku.isse.passiveprocessengine.core.TypeProvider;
import at.jku.isse.passiveprocessengine.rdfwrapper.RDFInstanceType;

public class MappingDefinitionType  implements TypeProvider {

	public static enum CoreProperties {fromStepType, fromParameter, toStepType, toParameter, flowDir};
	private SchemaRegistry schemaRegistry;
	public static final String typeId = MappingDefinitionType.class.getSimpleName();
	private final RDFInstanceType type;
	
	public MappingDefinitionType(SchemaRegistry schemaRegistry) {
		this.schemaRegistry = schemaRegistry;
		Optional<RDFInstanceType> thisType = schemaRegistry.findNonDeletedInstanceTypeByFQN(typeId);
		if (thisType.isPresent()) {
			//schemaRegistry.registerType(MappingDefinition.class, thisType.get());
			this.type = thisType.get();
		} else {
			RDFInstanceType type = schemaRegistry.createNewInstanceType(typeId, schemaRegistry.getTypeByName(ProcessDefinitionScopeType.typeId));
			//schemaRegistry.registerType(MappingDefinition.class, type);
			this.type = type;
		}
	}
	
	@Override
	public void produceTypeProperties() {
		((RDFInstanceType) type).cacheSuperProperties();
		type.createSinglePropertyType(MappingDefinitionType.CoreProperties.fromStepType.toString(), BuildInType.STRING);
		type.createSinglePropertyType(MappingDefinitionType.CoreProperties.fromParameter.toString(), BuildInType.STRING);
		type.createSinglePropertyType(MappingDefinitionType.CoreProperties.toStepType.toString(), BuildInType.STRING);
		type.createSinglePropertyType(MappingDefinitionType.CoreProperties.toParameter.toString(), BuildInType.STRING);
		type.createSinglePropertyType(MappingDefinitionType.CoreProperties.flowDir.toString(), BuildInType.STRING);
		
	}
}
