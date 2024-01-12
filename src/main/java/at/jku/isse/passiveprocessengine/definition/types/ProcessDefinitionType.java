package at.jku.isse.passiveprocessengine.definition.types;

import java.util.Optional;

import at.jku.isse.passiveprocessengine.core.BuildInType;
import at.jku.isse.passiveprocessengine.core.InstanceType;
import at.jku.isse.passiveprocessengine.core.SchemaRegistry;
import at.jku.isse.passiveprocessengine.core.TypeProvider;
import at.jku.isse.passiveprocessengine.definition.activeobjects.DecisionNodeDefinition;
import at.jku.isse.passiveprocessengine.definition.activeobjects.ProcessDefinition;
import at.jku.isse.passiveprocessengine.definition.activeobjects.StepDefinition;

public class ProcessDefinitionType implements TypeProvider{

	public static enum CoreProperties {decisionNodeDefinitions, stepDefinitions, prematureTriggers, prematureTriggerMappings,
	isImmediateDataPropagationEnabled,
	isImmediateInstantiateAllSteps, isWithoutBlockingErrors}

	private SchemaRegistry schemaRegistry;
	public static final String typeId = ProcessDefinitionType.class.getSimpleName();
	private final InstanceType type;
	
	public ProcessDefinitionType(SchemaRegistry schemaRegistry) {
		this.schemaRegistry = schemaRegistry;
		Optional<InstanceType> thisType = schemaRegistry.findNonDeletedInstanceTypeById(typeId);
		if (thisType.isPresent()) {
			schemaRegistry.registerType(ProcessDefinition.class, thisType.get());
			this.type = thisType.get();
		} else {
			InstanceType type = schemaRegistry.createNewInstanceType(typeId, schemaRegistry.getType(StepDefinition.class));
			schemaRegistry.registerType(ProcessDefinition.class, type);
			this.type = type;
		}
	}
	
	@Override
	public void produceTypeProperties() {
			type.createListPropertyType(CoreProperties.stepDefinitions.toString(), schemaRegistry.getType(StepDefinition.class));
			type.createSetPropertyType(CoreProperties.decisionNodeDefinitions.toString(),  schemaRegistry.getType(DecisionNodeDefinition.class));
			type.createMapPropertyType(CoreProperties.prematureTriggers.toString(),  BuildInType.STRING, BuildInType.STRING);
			type.createMapPropertyType(CoreProperties.prematureTriggerMappings.toString(),  BuildInType.STRING, BuildInType.STRING);
			type.createSinglePropertyType(CoreProperties.isImmediateDataPropagationEnabled.toString(),  BuildInType.BOOLEAN);
			type.createSinglePropertyType(CoreProperties.isImmediateInstantiateAllSteps.toString(),  BuildInType.BOOLEAN);
			type.createSinglePropertyType(CoreProperties.isWithoutBlockingErrors.toString(),  BuildInType.BOOLEAN);			
	}
}
