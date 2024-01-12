package at.jku.isse.passiveprocessengine.definition.types;

import java.util.Optional;

import at.jku.isse.passiveprocessengine.core.BuildInType;
import at.jku.isse.passiveprocessengine.core.InstanceType;
import at.jku.isse.passiveprocessengine.core.SchemaRegistry;
import at.jku.isse.passiveprocessengine.core.TypeProvider;
import at.jku.isse.passiveprocessengine.definition.activeobjects.DecisionNodeDefinition;
import at.jku.isse.passiveprocessengine.definition.activeobjects.MappingDefinition;
import at.jku.isse.passiveprocessengine.definition.activeobjects.ProcessDefinitionScopedElement;
import at.jku.isse.passiveprocessengine.definition.activeobjects.StepDefinition;

public class DecisionNodeDefinitionType implements TypeProvider {

	public static enum CoreProperties {inFlowType, dataMappingDefinitions, inSteps, outSteps, hierarchyDepth, closingDN}
	public static final String typeId = DecisionNodeDefinition.class.getSimpleName();
	private SchemaRegistry schemaRegistry;
	private final InstanceType type;

	public DecisionNodeDefinitionType(SchemaRegistry schemaRegistry) {
		this.schemaRegistry = schemaRegistry;
		Optional<InstanceType> thisType = schemaRegistry.findNonDeletedInstanceTypeById(typeId);
		if (thisType.isPresent()) {
			schemaRegistry.registerType(DecisionNodeDefinition.class, thisType.get());
			this.type = thisType.get();
		} else {
			InstanceType type = schemaRegistry.createNewInstanceType(typeId,  schemaRegistry.getType(ProcessDefinitionScopedElement.class));
			schemaRegistry.registerType(DecisionNodeDefinition.class, type);
			this.type = type;
		}
	}
	
	
	@Override
	public void produceTypeProperties() {
		type.createSinglePropertyType(DecisionNodeDefinitionType.CoreProperties.inFlowType.toString(), BuildInType.STRING);
		type.createSetPropertyType(DecisionNodeDefinitionType.CoreProperties.inSteps.toString(), schemaRegistry.getType(StepDefinition.class));
		type.createSetPropertyType(DecisionNodeDefinitionType.CoreProperties.outSteps.toString(),  schemaRegistry.getType(StepDefinition.class));
		type.createSetPropertyType(DecisionNodeDefinitionType.CoreProperties.dataMappingDefinitions.toString(),  schemaRegistry.getType(MappingDefinition.class));
		type.createSinglePropertyType((DecisionNodeDefinitionType.CoreProperties.hierarchyDepth.toString()),  BuildInType.INTEGER);
		type.createSinglePropertyType(DecisionNodeDefinitionType.CoreProperties.closingDN.toString(), type);

	}	

}
