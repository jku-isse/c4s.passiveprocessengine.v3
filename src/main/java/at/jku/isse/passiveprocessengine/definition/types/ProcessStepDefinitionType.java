package at.jku.isse.passiveprocessengine.definition.types;

import java.util.Optional;

import at.jku.isse.passiveprocessengine.core.BuildInType;
import at.jku.isse.passiveprocessengine.core.PPEInstanceType;
import at.jku.isse.passiveprocessengine.core.SchemaRegistry;
import at.jku.isse.passiveprocessengine.core.TypeProvider;
import at.jku.isse.passiveprocessengine.definition.activeobjects.ConstraintSpec;
import at.jku.isse.passiveprocessengine.definition.activeobjects.ProcessDefinitionScopedElement;
import at.jku.isse.passiveprocessengine.definition.activeobjects.StepDefinition;

public class ProcessStepDefinitionType implements TypeProvider {

	public static enum CoreProperties {expectedInput, expectedOutput, ioMappingRules,
	//conditions,
	preconditions, postconditions, cancelconditions, activationconditions,
	qaConstraints,
	inDND, outDND, specOrderIndex,html_url,description,
	hierarchyDepth}
	public static final String typeId = StepDefinition.class.getSimpleName();
	private SchemaRegistry schemaRegistry;
	private final PPEInstanceType type;

	public ProcessStepDefinitionType(SchemaRegistry schemaRegistry) {
		this.schemaRegistry = schemaRegistry;
		Optional<PPEInstanceType> thisType = schemaRegistry.findNonDeletedInstanceTypeByFQN(typeId);
		if (thisType.isPresent()) {
			schemaRegistry.registerType(StepDefinition.class, thisType.get());
			this.type = thisType.get();
		} else {
			PPEInstanceType type = schemaRegistry.createNewInstanceType(typeId,  schemaRegistry.getType(ProcessDefinitionScopedElement.class));
			schemaRegistry.registerType(StepDefinition.class, type);
			this.type = type;
		}
	}
	
	
	@Override
	public void produceTypeProperties() {
		
				type.createSetPropertyType(ProcessStepDefinitionType.CoreProperties.qaConstraints.toString(), schemaRegistry.getType(ConstraintSpec.class));
				type.createMapPropertyType(ProcessStepDefinitionType.CoreProperties.expectedInput.toString(), BuildInType.STRING, BuildInType.METATYPE);
				type.createMapPropertyType(ProcessStepDefinitionType.CoreProperties.expectedOutput.toString(), BuildInType.STRING, BuildInType.METATYPE);
				type.createSetPropertyType(ProcessStepDefinitionType.CoreProperties.preconditions.toString(), schemaRegistry.getType(ConstraintSpec.class));
				type.createSetPropertyType(ProcessStepDefinitionType.CoreProperties.postconditions.toString(), schemaRegistry.getType(ConstraintSpec.class));
				type.createSetPropertyType(ProcessStepDefinitionType.CoreProperties.cancelconditions.toString(), schemaRegistry.getType(ConstraintSpec.class));
				type.createSetPropertyType(ProcessStepDefinitionType.CoreProperties.activationconditions.toString(), schemaRegistry.getType(ConstraintSpec.class));
				type.createSinglePropertyType(ProcessStepDefinitionType.CoreProperties.inDND.toString(), BuildInType.STRING);
				type.createSinglePropertyType(ProcessStepDefinitionType.CoreProperties.outDND.toString(), BuildInType.STRING);
				type.createMapPropertyType((ProcessStepDefinitionType.CoreProperties.ioMappingRules.toString()), BuildInType.STRING, BuildInType.STRING);
				type.createSinglePropertyType((ProcessStepDefinitionType.CoreProperties.specOrderIndex.toString()), BuildInType.INTEGER);
				type.createSinglePropertyType((ProcessStepDefinitionType.CoreProperties.hierarchyDepth.toString()), BuildInType.INTEGER);
				type.createSinglePropertyType((ProcessStepDefinitionType.CoreProperties.html_url.toString()), BuildInType.STRING);
				type.createSinglePropertyType((ProcessStepDefinitionType.CoreProperties.description.toString()), BuildInType.STRING);
	}

}