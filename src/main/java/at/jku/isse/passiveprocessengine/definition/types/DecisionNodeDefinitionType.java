package at.jku.isse.passiveprocessengine.definition.types;

import java.util.Optional;

import at.jku.isse.passiveprocessengine.core.BuildInType;
import at.jku.isse.passiveprocessengine.core.InstanceType;
import at.jku.isse.passiveprocessengine.core.ProcessDomainTypesRegistry;
import at.jku.isse.passiveprocessengine.core.SchemaRegistry;
import at.jku.isse.passiveprocessengine.core.ProcessDomainTypesRegistry.TypeProvider;
import at.jku.isse.passiveprocessengine.definition.activeobjects.DecisionNodeDefinition;
import at.jku.isse.passiveprocessengine.definition.activeobjects.MappingDefinition;
import at.jku.isse.passiveprocessengine.definition.activeobjects.ProcessDefinitionScopedElement;
import at.jku.isse.passiveprocessengine.definition.activeobjects.StepDefinition;

public class DecisionNodeDefinitionType implements TypeProvider {

	public static enum CoreProperties {inFlowType, dataMappingDefinitions, inSteps, outSteps, hierarchyDepth, closingDN}
	public static final String typeId = DecisionNodeDefinition.class.getSimpleName();
	private SchemaRegistry schemaRegistry;

	public DecisionNodeDefinitionType(SchemaRegistry schemaRegistry) {
		this.schemaRegistry = schemaRegistry;
	}
	
	
	@Override
	public void registerTypeInFactory(ProcessDomainTypesRegistry factory) {
		Optional<InstanceType> thisType = schemaRegistry.findNonDeletedInstanceTypeById(typeId);
			if (thisType.isPresent())
				factory.registerType(DecisionNodeDefinition.class, thisType.get());
			else {
				InstanceType type = schemaRegistry.createNewInstanceType(typeId,  factory.getType(ProcessDefinitionScopedElement.class));
				factory.registerType(DecisionNodeDefinition.class, type);
				type.createSinglePropertyType(DecisionNodeDefinitionType.CoreProperties.inFlowType.toString(), BuildInType.STRING);
				type.createSetPropertyType(DecisionNodeDefinitionType.CoreProperties.inSteps.toString(), factory.getType(StepDefinition.class));
				type.createSetPropertyType(DecisionNodeDefinitionType.CoreProperties.outSteps.toString(),  factory.getType(StepDefinition.class));
				type.createSetPropertyType(DecisionNodeDefinitionType.CoreProperties.dataMappingDefinitions.toString(),  factory.getType(MappingDefinition.class));
				type.createSinglePropertyType((DecisionNodeDefinitionType.CoreProperties.hierarchyDepth.toString()),  BuildInType.INTEGER);
				type.createSinglePropertyType(DecisionNodeDefinitionType.CoreProperties.closingDN.toString(), type);
				
			}
	}

}
