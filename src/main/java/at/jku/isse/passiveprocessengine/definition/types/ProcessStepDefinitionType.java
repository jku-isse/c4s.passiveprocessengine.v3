package at.jku.isse.passiveprocessengine.definition.types;

import java.util.Optional;

import at.jku.isse.passiveprocessengine.rdfwrapper.NodeToDomainResolver;
import at.jku.isse.passiveprocessengine.rdfwrapper.RDFInstanceType;
import at.jku.isse.passiveprocessengine.definition.activeobjects.StepDefinition;

public class ProcessStepDefinitionType  {

	public enum CoreProperties {expectedInput, expectedOutput, ioMappingRules,
	preconditions, postconditions, cancelconditions, activationconditions,
	qaConstraints,
	inDND, outDND, specOrderIndex,html_url,description,
	stepHierarchyDepth}
	public static final String typeId = StepDefinition.class.getSimpleName();
	private NodeToDomainResolver schemaRegistry;
	private final RDFInstanceType type;

	public ProcessStepDefinitionType(NodeToDomainResolver schemaRegistry) {
		this.schemaRegistry = schemaRegistry;
		Optional<RDFInstanceType> thisType = schemaRegistry.findNonDeletedInstanceTypeByFQN(typeId);
		if (thisType.isPresent()) {
			this.type = thisType.get();
		} else {
			RDFInstanceType type = schemaRegistry.createNewInstanceType(typeId,  schemaRegistry.getTypeByName(ProcessDefinitionScopeType.typeId));
			this.type = type;
		}
	}
	
	
	public void produceTypeProperties() {
		type.cacheSuperProperties();
		type.createSetPropertyType(ProcessStepDefinitionType.CoreProperties.qaConstraints.toString(), schemaRegistry.getTypeByName(ConstraintSpecType.typeId));
		type.createMapPropertyType(ProcessStepDefinitionType.CoreProperties.expectedInput.toString(), BuildInType.STRING, BuildInType.METATYPE);
		type.createMapPropertyType(ProcessStepDefinitionType.CoreProperties.expectedOutput.toString(), BuildInType.STRING, BuildInType.METATYPE);
		type.createSetPropertyType(ProcessStepDefinitionType.CoreProperties.preconditions.toString(), schemaRegistry.getTypeByName(ConstraintSpecType.typeId));
		type.createSetPropertyType(ProcessStepDefinitionType.CoreProperties.postconditions.toString(), schemaRegistry.getTypeByName(ConstraintSpecType.typeId));
		type.createSetPropertyType(ProcessStepDefinitionType.CoreProperties.cancelconditions.toString(), schemaRegistry.getTypeByName(ConstraintSpecType.typeId));
		type.createSetPropertyType(ProcessStepDefinitionType.CoreProperties.activationconditions.toString(), schemaRegistry.getTypeByName(ConstraintSpecType.typeId));
		type.createSinglePropertyType(ProcessStepDefinitionType.CoreProperties.inDND.toString(), BuildInType.STRING);
		type.createSinglePropertyType(ProcessStepDefinitionType.CoreProperties.outDND.toString(), BuildInType.STRING);
		type.createMapPropertyType((ProcessStepDefinitionType.CoreProperties.ioMappingRules.toString()), BuildInType.STRING, BuildInType.STRING);
		type.createSinglePropertyType((ProcessStepDefinitionType.CoreProperties.specOrderIndex.toString()), BuildInType.INTEGER);
		type.createSinglePropertyType((ProcessStepDefinitionType.CoreProperties.stepHierarchyDepth.toString()), BuildInType.INTEGER);
		type.createSinglePropertyType((ProcessStepDefinitionType.CoreProperties.html_url.toString()), BuildInType.STRING);
		type.createSinglePropertyType((ProcessStepDefinitionType.CoreProperties.description.toString()), BuildInType.STRING);

	}

}