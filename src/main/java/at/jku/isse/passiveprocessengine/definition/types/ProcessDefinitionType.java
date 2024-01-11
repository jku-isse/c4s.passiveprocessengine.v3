package at.jku.isse.passiveprocessengine.definition.types;

import java.util.Optional;

import at.jku.isse.passiveprocessengine.core.BuildInType;
import at.jku.isse.passiveprocessengine.core.InstanceType;
import at.jku.isse.passiveprocessengine.core.ProcessDomainTypesRegistry;
import at.jku.isse.passiveprocessengine.core.SchemaRegistry;
import at.jku.isse.passiveprocessengine.core.ProcessDomainTypesRegistry.TypeProvider;
import at.jku.isse.passiveprocessengine.definition.activeobjects.DecisionNodeDefinition;
import at.jku.isse.passiveprocessengine.definition.activeobjects.ProcessDefinition;
import at.jku.isse.passiveprocessengine.definition.activeobjects.StepDefinition;

public class ProcessDefinitionType implements TypeProvider{

	public static enum CoreProperties {decisionNodeDefinitions, stepDefinitions, prematureTriggers, prematureTriggerMappings,
	isImmediateDataPropagationEnabled,
	isImmediateInstantiateAllSteps, isWithoutBlockingErrors}

	private SchemaRegistry schemaRegistry;
	public static final String typeId = ProcessDefinitionType.class.getSimpleName();
	
	public ProcessDefinitionType(SchemaRegistry schemaRegistry) {
		this.schemaRegistry = schemaRegistry;
	}
	
	@Override
	public void registerTypeInFactory(ProcessDomainTypesRegistry factory) {
		Optional<InstanceType> thisType = schemaRegistry.findNonDeletedInstanceTypeById(typeId);
		if (thisType.isPresent())
			factory.registerType(ProcessDefinition.class, thisType.get());
		else {
			InstanceType type = schemaRegistry.createNewInstanceType(typeId, factory.getType(StepDefinition.class));
			factory.registerType(ProcessDefinition.class, type);			
			type.createListPropertyType(CoreProperties.stepDefinitions.toString(), factory.getType(StepDefinition.class));
			type.createSetPropertyType(CoreProperties.decisionNodeDefinitions.toString(),  factory.getType(DecisionNodeDefinition.class));
			type.createMapPropertyType(CoreProperties.prematureTriggers.toString(),  BuildInType.STRING, BuildInType.STRING);
			type.createMapPropertyType(CoreProperties.prematureTriggerMappings.toString(),  BuildInType.STRING, BuildInType.STRING);
			type.createSinglePropertyType(CoreProperties.isImmediateDataPropagationEnabled.toString(),  BuildInType.BOOLEAN);
			type.createSinglePropertyType(CoreProperties.isImmediateInstantiateAllSteps.toString(),  BuildInType.BOOLEAN);
			type.createSinglePropertyType(CoreProperties.isWithoutBlockingErrors.toString(),  BuildInType.BOOLEAN);			
		}
	}
}
