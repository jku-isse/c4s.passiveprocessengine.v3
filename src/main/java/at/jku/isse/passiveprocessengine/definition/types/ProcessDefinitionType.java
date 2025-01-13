package at.jku.isse.passiveprocessengine.definition.types;

import java.util.Optional;

import at.jku.isse.passiveprocessengine.core.BuildInType;
import at.jku.isse.passiveprocessengine.core.PPEInstanceType;
import at.jku.isse.passiveprocessengine.core.SchemaRegistry;
import at.jku.isse.passiveprocessengine.core.TypeProvider;
import at.jku.isse.passiveprocessengine.definition.activeobjects.DecisionNodeDefinition;
import at.jku.isse.passiveprocessengine.definition.activeobjects.ProcessDefinition;
import at.jku.isse.passiveprocessengine.definition.activeobjects.StepDefinition;
import at.jku.isse.passiveprocessengine.rdfwrapper.RDFInstanceType;

public class ProcessDefinitionType implements TypeProvider{

	public static enum CoreProperties {decisionNodeDefinitions, stepDefinitions, prematureTriggers, prematureTriggerMappings,
	isImmediateDataPropagationEnabled,
	isImmediateInstantiateAllSteps, isWithoutBlockingErrors}

	private SchemaRegistry schemaRegistry;
	public static final String typeId = ProcessDefinitionType.class.getSimpleName();
	private final PPEInstanceType type;
	
	public ProcessDefinitionType(SchemaRegistry schemaRegistry) {
		this.schemaRegistry = schemaRegistry;
		Optional<PPEInstanceType> thisType = schemaRegistry.findNonDeletedInstanceTypeByFQN(typeId);
		if (thisType.isPresent()) {
		//	schemaRegistry.registerType(ProcessDefinition.class, thisType.get());
			this.type = thisType.get();
		} else {
			PPEInstanceType type = schemaRegistry.createNewInstanceType(typeId, schemaRegistry.getTypeByName(ProcessStepDefinitionType.typeId));
		//	schemaRegistry.registerType(ProcessDefinition.class, type);
			this.type = type;
		}
	}
	
	@Override
	public void produceTypeProperties() {
		((RDFInstanceType) type).cacheSuperProperties();
			type.createListPropertyType(CoreProperties.stepDefinitions.toString(), schemaRegistry.getTypeByName(ProcessStepDefinitionType.typeId));
			type.createSetPropertyType(CoreProperties.decisionNodeDefinitions.toString(),  schemaRegistry.getTypeByName(DecisionNodeDefinitionType.typeId));
			type.createMapPropertyType(CoreProperties.prematureTriggers.toString(),  BuildInType.STRING, BuildInType.STRING);
			type.createMapPropertyType(CoreProperties.prematureTriggerMappings.toString(),  BuildInType.STRING, BuildInType.STRING);
			type.createSinglePropertyType(CoreProperties.isImmediateDataPropagationEnabled.toString(),  BuildInType.BOOLEAN);
			type.createSinglePropertyType(CoreProperties.isImmediateInstantiateAllSteps.toString(),  BuildInType.BOOLEAN);
			type.createSinglePropertyType(CoreProperties.isWithoutBlockingErrors.toString(),  BuildInType.BOOLEAN);		
			
	}
}
