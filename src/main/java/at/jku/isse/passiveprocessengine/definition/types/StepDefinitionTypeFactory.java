package at.jku.isse.passiveprocessengine.definition.types;

import java.util.Optional;

import at.jku.isse.passiveprocessengine.rdfwrapper.NodeToDomainResolver;
import at.jku.isse.passiveprocessengine.rdfwrapper.RDFInstance;
import at.jku.isse.passiveprocessengine.rdfwrapper.RDFInstanceType;
import at.jku.isse.passiveprocessengine.rdfwrapper.RDFPropertyType.PrimitiveOrClassType;
import at.jku.isse.passiveprocessengine.rdfwrapper.rule.RuleEnabledResolver;
import at.jku.isse.passiveprocessengine.core.AbstractTypeProvider;
import at.jku.isse.passiveprocessengine.definition.activeobjects.StepDefinition;

public class StepDefinitionTypeFactory extends AbstractTypeProvider {

	private static final String NS = ProcessDefinitionScopeType.NS+"/stepdefinition#";
	
	public enum CoreProperties {		
		expectedInput,
		expectedOutput, 
		ioMappingRules,
		preconditions, postconditions, cancelconditions, activationconditions,
		qaConstraints,
		inDND, outDND, specOrderIndex, html_url, description,
		stepHierarchyDepth
		;
	
		@Override
		public String toString() {
			return NS+name();
		}
		
		public String getURI() {
			return NS+name();
		}
	}
	
	public static final String typeId = ProcessDefinitionScopeType.NS+"#"+StepDefinition.class.getSimpleName();

	public StepDefinitionTypeFactory(RuleEnabledResolver schemaRegistry) {
		super(schemaRegistry);
		Optional<RDFInstanceType> thisType = schemaRegistry.findNonDeletedInstanceTypeByFQN(typeId);
		if (thisType.isPresent()) {
			this.type = thisType.get();
		} else {
			this.type = schemaRegistry.createNewInstanceType(typeId,  schemaRegistry.findNonDeletedInstanceTypeByFQN(ProcessDefinitionScopeType.typeId).orElse(null));
		}
		metaElements.registerInstanceSpecificClass(typeId, StepDefinition.class);
	}
	
	
	public void produceTypeProperties() {
		type.cacheSuperProperties();		
		type.createSetPropertyType(CoreProperties.qaConstraints.toString(), 
				schemaRegistry.findNonDeletedInstanceTypeByFQN(ConstraintSpecTypeFactory.typeId)
				.map(vtype->vtype.getAsPropertyType())
				.orElse(null));
		type.createMapPropertyType(CoreProperties.expectedInput.toString(),  PrimitiveOrClassType.fromRDFNode(metaElements.getMetaClass()));
		type.createMapPropertyType(CoreProperties.expectedOutput.toString(),  PrimitiveOrClassType.fromRDFNode(metaElements.getMetaClass()));
		type.createSetPropertyType(CoreProperties.preconditions.toString(), 
				schemaRegistry.findNonDeletedInstanceTypeByFQN(ConstraintSpecTypeFactory.typeId)
				.map(vtype->vtype.getAsPropertyType())
				.orElse(null));
		type.createSetPropertyType(CoreProperties.postconditions.toString(), 
				schemaRegistry.findNonDeletedInstanceTypeByFQN(ConstraintSpecTypeFactory.typeId)
				.map(vtype->vtype.getAsPropertyType())
				.orElse(null));
		type.createSetPropertyType(CoreProperties.cancelconditions.toString(), 
				schemaRegistry.findNonDeletedInstanceTypeByFQN(ConstraintSpecTypeFactory.typeId)
				.map(vtype->vtype.getAsPropertyType())
				.orElse(null));
		type.createSetPropertyType(CoreProperties.activationconditions.toString(), 
				schemaRegistry.findNonDeletedInstanceTypeByFQN(ConstraintSpecTypeFactory.typeId)
				.map(vtype->vtype.getAsPropertyType())
				.orElse(null));
		type.createSinglePropertyType(CoreProperties.inDND.toString(), primitives.getStringType());
		type.createSinglePropertyType(CoreProperties.outDND.toString(), primitives.getStringType());
		type.createMapPropertyType((CoreProperties.ioMappingRules.toString()), primitives.getStringType());
		type.createSinglePropertyType((CoreProperties.specOrderIndex.toString()), primitives.getIntType());
		type.createSinglePropertyType((CoreProperties.stepHierarchyDepth.toString()), primitives.getIntType());
		type.createSinglePropertyType((CoreProperties.html_url.toString()), primitives.getStringType());
		type.createSinglePropertyType((CoreProperties.description.toString()), primitives.getStringType());

	}
	
	public StepDefinition createInstance(String stepId) {
		return (StepDefinition) schemaRegistry.createInstance(stepId, type);
		// no further default properties to set here
	}

}