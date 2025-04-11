package at.jku.isse.passiveprocessengine.instance.types;

import java.util.Optional;

import at.jku.isse.passiveprocessengine.core.AbstractTypeProvider;
import at.jku.isse.passiveprocessengine.rdfwrapper.RDFInstanceType;
import at.jku.isse.passiveprocessengine.rdfwrapper.rule.RuleEnabledResolver;
import at.jku.isse.passiveprocessengine.definition.types.ConstraintSpecTypeFactory;
import at.jku.isse.passiveprocessengine.instance.activeobjects.ConstraintResultWrapper;

public class ConstraintWrapperType extends AbstractTypeProvider {
	
	private static final String NS = ProcessInstanceScopeType.NS+"/constraint";	
	
	public enum CoreProperties {qaSpec, lastChanged, crule, parentStep, isOverriden, overrideValue, overrideReason
		;
		
		@Override
		public String toString() {
			return NS+name();
		}
		
		public String getURI() {
			return NS+name();
		}	
	}
	public static final String typeId = NS+"#"+ConstraintResultWrapper.class.getSimpleName();

	
	private final RuleEnabledResolver schemaRegistry;
	public ConstraintWrapperType(RuleEnabledResolver schemaRegistry) {
		super(schemaRegistry);
		this.schemaRegistry = schemaRegistry;
		Optional<RDFInstanceType> thisType = schemaRegistry.findNonDeletedInstanceTypeByFQN(typeId);
		if (thisType.isPresent()) {
			this.type = thisType.get();
		} else {
			type = schemaRegistry.createNewInstanceType(typeId, schemaRegistry.findNonDeletedInstanceTypeByFQN(ProcessInstanceScopeType.typeId).orElse(null));
		}
		metaElements.registerInstanceSpecificClass(typeId, ConstraintResultWrapper.class);
	}

	public void produceTypeProperties(ProcessInstanceScopeType processInstanceScopeType) {
		type.cacheSuperProperties();
		type.createSinglePropertyType(CoreProperties.qaSpec.toString(), 
				schemaRegistry.findNonDeletedInstanceTypeByFQN(ConstraintSpecTypeFactory.typeId)
				.map(vtype->vtype.getAsPropertyType())
				.orElse(null));
		type.createSinglePropertyType(CoreProperties.parentStep.toString(), 
				schemaRegistry.findNonDeletedInstanceTypeByFQN(AbstractProcessStepType.typeId)
				.map(vtype->vtype.getAsPropertyType())
				.orElse(null));
		type.createSinglePropertyType(CoreProperties.lastChanged.toString(), primitives.getStringType());
		type.createSinglePropertyType(CoreProperties.crule.toString(), 
				schemaRegistry.findNonDeletedInstanceTypeByFQN(
						schemaRegistry.getRuleSchema().getResultBaseType().getURI())
				.map(vtype->vtype.getAsPropertyType())
				.orElse(null));
		type.createSinglePropertyType(CoreProperties.isOverriden.toString(), primitives.getBooleanType());
		type.createSinglePropertyType(CoreProperties.overrideValue.toString(), primitives.getBooleanType());
		type.createSinglePropertyType(CoreProperties.overrideReason.toString(), primitives.getStringType());

		// so ugly:
		processInstanceScopeType.addGenericProcessProperty(type);
	}


}
