package at.jku.isse.passiveprocessengine.definition.types;

import java.util.Optional;

import at.jku.isse.passiveprocessengine.rdfwrapper.NodeToDomainResolver;
import at.jku.isse.passiveprocessengine.rdfwrapper.RDFInstanceType;

public class ProcessDefinitionType {

	public enum CoreProperties {decisionNodeDefinitions, stepDefinitions, prematureTriggers, prematureTriggerMappings,
	isImmediateDataPropagationEnabled,
	isImmediateInstantiateAllSteps, isWithoutBlockingErrors}

	private NodeToDomainResolver schemaRegistry;
	public static final String typeId = ProcessDefinitionType.class.getSimpleName();
	private final RDFInstanceType type;
	
	public ProcessDefinitionType(NodeToDomainResolver schemaRegistry) {
		this.schemaRegistry = schemaRegistry;
		Optional<RDFInstanceType> thisType = schemaRegistry.findNonDeletedInstanceTypeByFQN(typeId);
		if (thisType.isPresent()) {
		//	schemaRegistry.registerType(ProcessDefinition.class, thisType.get());
			this.type = thisType.get();
		} else {
			RDFInstanceType type = schemaRegistry.createNewInstanceType(typeId, schemaRegistry.getTypeByName(ProcessStepDefinitionType.typeId));
		//	schemaRegistry.registerType(ProcessDefinition.class, type);
			this.type = type;
		}
	}
	
	public void produceTypeProperties() {
		type.cacheSuperProperties();
		type.createListPropertyType(CoreProperties.stepDefinitions.toString(), schemaRegistry.getTypeByName(ProcessStepDefinitionType.typeId));
		type.createSetPropertyType(CoreProperties.decisionNodeDefinitions.toString(),  schemaRegistry.getTypeByName(DecisionNodeDefinitionType.typeId));
		type.createMapPropertyType(CoreProperties.prematureTriggers.toString(),  BuildInType.STRING, BuildInType.STRING);
		type.createMapPropertyType(CoreProperties.prematureTriggerMappings.toString(),  BuildInType.STRING, BuildInType.STRING);
		type.createSinglePropertyType(CoreProperties.isImmediateDataPropagationEnabled.toString(),  BuildInType.BOOLEAN);
		type.createSinglePropertyType(CoreProperties.isImmediateInstantiateAllSteps.toString(),  BuildInType.BOOLEAN);
		type.createSinglePropertyType(CoreProperties.isWithoutBlockingErrors.toString(),  BuildInType.BOOLEAN);		

	}
}
