package at.jku.isse.passiveprocessengine.definition.types;

import java.util.Optional;

import at.jku.isse.passiveprocessengine.core.BuildInType;
import at.jku.isse.passiveprocessengine.core.PPEInstanceType;
import at.jku.isse.passiveprocessengine.core.SchemaRegistry;
import at.jku.isse.passiveprocessengine.core.TypeProvider;
import at.jku.isse.passiveprocessengine.definition.activeobjects.DecisionNodeDefinition;
import at.jku.isse.passiveprocessengine.definition.activeobjects.MappingDefinition;
import at.jku.isse.passiveprocessengine.definition.activeobjects.ProcessDefinitionScopedElement;
import at.jku.isse.passiveprocessengine.definition.activeobjects.StepDefinition;
import at.jku.isse.passiveprocessengine.rdfwrapper.RDFInstanceType;

public class DecisionNodeDefinitionType implements TypeProvider {

	public static enum CoreProperties {inFlowType, dataMappingDefinitions, inSteps, outSteps, hierarchyDepth, closingDN}
	public static final String typeId = DecisionNodeDefinition.class.getSimpleName();
	private SchemaRegistry schemaRegistry;
	private final PPEInstanceType type;

	public DecisionNodeDefinitionType(SchemaRegistry schemaRegistry) {
		this.schemaRegistry = schemaRegistry;
		Optional<PPEInstanceType> thisType = schemaRegistry.findNonDeletedInstanceTypeByFQN(typeId);
		if (thisType.isPresent()) {
			//schemaRegistry.registerType(DecisionNodeDefinition.class, thisType.get());
			this.type = thisType.get();
		} else {
			type = schemaRegistry.createNewInstanceType(typeId,  schemaRegistry.getTypeByName(ProcessDefinitionScopeType.typeId));
		//	schemaRegistry.registerType(DecisionNodeDefinition.class, type);
		}
	}
	
	
	@Override
	public void produceTypeProperties() {
		((RDFInstanceType) type).cacheSuperProperties();
		type.createSinglePropertyType(DecisionNodeDefinitionType.CoreProperties.inFlowType.toString(), BuildInType.STRING);
		type.createSetPropertyType(DecisionNodeDefinitionType.CoreProperties.inSteps.toString(), schemaRegistry.getTypeByName(ProcessStepDefinitionType.typeId));
		type.createSetPropertyType(DecisionNodeDefinitionType.CoreProperties.outSteps.toString(),  schemaRegistry.getTypeByName(ProcessStepDefinitionType.typeId));
		type.createSetPropertyType(DecisionNodeDefinitionType.CoreProperties.dataMappingDefinitions.toString(),  schemaRegistry.getTypeByName(MappingDefinitionType.typeId));
		type.createSinglePropertyType((DecisionNodeDefinitionType.CoreProperties.hierarchyDepth.toString()),  BuildInType.INTEGER);
		type.createSinglePropertyType(DecisionNodeDefinitionType.CoreProperties.closingDN.toString(), type);
		
	}	

}
