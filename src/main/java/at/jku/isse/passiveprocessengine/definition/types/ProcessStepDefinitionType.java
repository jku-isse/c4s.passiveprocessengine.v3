package at.jku.isse.passiveprocessengine.definition.types;

import java.util.Optional;

import at.jku.isse.passiveprocessengine.rdfwrapper.NodeToDomainResolver;
import at.jku.isse.passiveprocessengine.rdfwrapper.RDFInstanceType;
import at.jku.isse.passiveprocessengine.rdfwrapper.RDFPropertyType.PrimitiveOrClassType;
import at.jku.isse.passiveprocessengine.core.BaseNamespace;
import at.jku.isse.passiveprocessengine.definition.activeobjects.StepDefinition;

public class ProcessStepDefinitionType  {

	public enum CoreProperties {expectedInput, expectedOutput, ioMappingRules,
	preconditions, postconditions, cancelconditions, activationconditions,
	qaConstraints,
	inDND, outDND, specOrderIndex,html_url,description,
	stepHierarchyDepth}
	
	public static final String typeId = ProcessDefinitionScopeType.NS+"#"+StepDefinition.class.getSimpleName();
	private NodeToDomainResolver schemaRegistry;
	private final RDFInstanceType type;

	public ProcessStepDefinitionType(NodeToDomainResolver schemaRegistry) {
		this.schemaRegistry = schemaRegistry;
		Optional<RDFInstanceType> thisType = schemaRegistry.findNonDeletedInstanceTypeByFQN(typeId);
		if (thisType.isPresent()) {
			this.type = thisType.get();
		} else {
			this.type = schemaRegistry.createNewInstanceType(typeId,  schemaRegistry.findNonDeletedInstanceTypeByFQN(ProcessDefinitionScopeType.typeId).orElse(null));
		}
		schemaRegistry.getMetaschemata().getMetaElements().registerInstanceSpecificClass(typeId, StepDefinition.class);
	}
	
	
	public void produceTypeProperties() {
		type.cacheSuperProperties();
		var primitives = schemaRegistry.getMetaschemata().getPrimitiveTypesFactory();
		type.createSetPropertyType(ProcessStepDefinitionType.CoreProperties.qaConstraints.toString(), schemaRegistry.findNonDeletedInstanceTypeByFQN(ConstraintSpecType.typeId).map(vtype->vtype.getAsPropertyType()).orElse(null));
		type.createMapPropertyType(ProcessStepDefinitionType.CoreProperties.expectedInput.toString(),  PrimitiveOrClassType.fromRDFNode(schemaRegistry.getMetaschemata().getMetaElements().getMetaClass()));
		type.createMapPropertyType(ProcessStepDefinitionType.CoreProperties.expectedOutput.toString(),  PrimitiveOrClassType.fromRDFNode(schemaRegistry.getMetaschemata().getMetaElements().getMetaClass()));
		type.createSetPropertyType(ProcessStepDefinitionType.CoreProperties.preconditions.toString(), schemaRegistry.findNonDeletedInstanceTypeByFQN(ConstraintSpecType.typeId).map(vtype->vtype.getAsPropertyType()).orElse(null));
		type.createSetPropertyType(ProcessStepDefinitionType.CoreProperties.postconditions.toString(), schemaRegistry.findNonDeletedInstanceTypeByFQN(ConstraintSpecType.typeId).map(vtype->vtype.getAsPropertyType()).orElse(null));
		type.createSetPropertyType(ProcessStepDefinitionType.CoreProperties.cancelconditions.toString(), schemaRegistry.findNonDeletedInstanceTypeByFQN(ConstraintSpecType.typeId).map(vtype->vtype.getAsPropertyType()).orElse(null));
		type.createSetPropertyType(ProcessStepDefinitionType.CoreProperties.activationconditions.toString(), schemaRegistry.findNonDeletedInstanceTypeByFQN(ConstraintSpecType.typeId).map(vtype->vtype.getAsPropertyType()).orElse(null));
		type.createSinglePropertyType(ProcessStepDefinitionType.CoreProperties.inDND.toString(), primitives.getStringType());
		type.createSinglePropertyType(ProcessStepDefinitionType.CoreProperties.outDND.toString(), primitives.getStringType());
		type.createMapPropertyType((ProcessStepDefinitionType.CoreProperties.ioMappingRules.toString()), primitives.getStringType());
		type.createSinglePropertyType((ProcessStepDefinitionType.CoreProperties.specOrderIndex.toString()), primitives.getIntType());
		type.createSinglePropertyType((ProcessStepDefinitionType.CoreProperties.stepHierarchyDepth.toString()), primitives.getIntType());
		type.createSinglePropertyType((ProcessStepDefinitionType.CoreProperties.html_url.toString()), primitives.getStringType());
		type.createSinglePropertyType((ProcessStepDefinitionType.CoreProperties.description.toString()), primitives.getStringType());

	}

}