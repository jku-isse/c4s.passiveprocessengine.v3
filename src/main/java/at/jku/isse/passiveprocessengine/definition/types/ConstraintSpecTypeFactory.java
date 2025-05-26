package at.jku.isse.passiveprocessengine.definition.types;

import java.util.Optional;

import at.jku.isse.passiveprocessengine.core.AbstractTypeProvider;
import at.jku.isse.passiveprocessengine.definition.activeobjects.ConstraintSpec;
import at.jku.isse.passiveprocessengine.instance.StepLifecycle.Conditions;
import at.jku.isse.passiveprocessengine.rdfwrapper.RDFInstance;
import at.jku.isse.passiveprocessengine.rdfwrapper.RDFInstanceType;
import at.jku.isse.passiveprocessengine.rdfwrapper.rule.RuleEnabledResolver;

public class ConstraintSpecTypeFactory extends AbstractTypeProvider {

	private static final String NS = ProcessDefinitionScopeTypeFactory.NS+"/constraintdefinition#";
	
	public enum CoreProperties {constraintSpec, augmentedSpec, humanReadableDescription, constraintSpecOrderIndex, isOverridable, ruleType, conditionsType;
			
		@Override
		public String toString() {
			return NS+name();
		}
		
		public String getURI() {
			return NS+name();
		}
	}

	public static final String typeId = ProcessDefinitionScopeTypeFactory.NS+"#ConstraintSpec";
	
	public ConstraintSpecTypeFactory(RuleEnabledResolver schemaRegistry) {
		super(schemaRegistry);
		Optional<RDFInstanceType> thisType = schemaRegistry.findNonDeletedInstanceTypeByFQN(typeId);
		if (thisType.isPresent()) {
			this.type = thisType.get();
		} else {
			this.type = schemaRegistry.createNewInstanceType(typeId, schemaRegistry.findNonDeletedInstanceTypeByFQN(ProcessDefinitionScopeTypeFactory.typeId).orElse(null));						
		}
		metaElements.registerInstanceSpecificClass(typeId, ConstraintSpec.class);
	}
	

	public void produceTypeProperties() {
		type.cacheSuperProperties();
		// constraintId maps to Instance name property
		var primitives = schemaRegistry.getMetaschemata().getPrimitiveTypesFactory();
		type.createSinglePropertyType(CoreProperties.constraintSpec.toString(), primitives.getStringType());
		type.createSinglePropertyType(CoreProperties.augmentedSpec.toString(),  primitives.getStringType());
		type.createSinglePropertyType(CoreProperties.humanReadableDescription.toString(),  primitives.getStringType());
		type.createSinglePropertyType(CoreProperties.constraintSpecOrderIndex.toString(),  primitives.getIntType());
		type.createSinglePropertyType(CoreProperties.isOverridable.toString(),  primitives.getBooleanType());
		type.createSinglePropertyType(CoreProperties.ruleType.toString(),  
				schemaRegistry.findNonDeletedInstanceTypeByFQN(schemaRegistry.getRuleSchema().getDefinitionType().getURI())
								.map(vtype->vtype.getAsPropertyType())
								.orElseThrow());
		type.createSinglePropertyType(CoreProperties.conditionsType.toString(),  primitives.getStringType());
		
	}

	
	public ConstraintSpec createInstance(Conditions condition, String constraintId, String constraintSpec, String humanReadableDescription, int specOrderIndex, boolean isOverridable) {
		return createInstance(condition, constraintId, constraintSpec, constraintSpec, humanReadableDescription, specOrderIndex, isOverridable);
	}

	public ConstraintSpec createInstance(Conditions condition, String constraintURI, String augmentedSpec, String constraintSpec, String humanReadableDescription, int specOrderIndex, boolean isOverridable) {
		ConstraintSpec instance = (ConstraintSpec) schemaRegistry.createInstance(constraintURI, type);
		instance.setSingleProperty(ConstraintSpecTypeFactory.CoreProperties.constraintSpec.toString(),constraintSpec);
		instance.setSingleProperty(ConstraintSpecTypeFactory.CoreProperties.augmentedSpec.toString(),augmentedSpec);
		instance.setSingleProperty(ConstraintSpecTypeFactory.CoreProperties.humanReadableDescription.toString(), humanReadableDescription == null ? "" : humanReadableDescription);
		instance.setSingleProperty(ConstraintSpecTypeFactory.CoreProperties.constraintSpecOrderIndex.toString(), specOrderIndex);
		instance.setSingleProperty(ConstraintSpecTypeFactory.CoreProperties.isOverridable.toString(), isOverridable);
		instance.setSingleProperty(ConstraintSpecTypeFactory.CoreProperties.conditionsType.toString(), condition.toString());
		//TODO: create rule and reference rule? or is this done elsewhere
		
		return instance;
	}
}
