package at.jku.isse.passiveprocessengine.instance.types;

import java.time.ZonedDateTime;
import java.util.Optional;

import at.jku.isse.passiveprocessengine.core.AbstractTypeProvider;
import at.jku.isse.passiveprocessengine.rdfwrapper.RDFInstance;
import at.jku.isse.passiveprocessengine.rdfwrapper.RDFInstanceType;
import at.jku.isse.passiveprocessengine.rdfwrapper.rule.RuleEnabledResolver;
import at.jku.isse.passiveprocessengine.definition.activeobjects.ConstraintSpec;
import at.jku.isse.passiveprocessengine.definition.types.ConstraintSpecTypeFactory;
import at.jku.isse.passiveprocessengine.instance.activeobjects.ConstraintResultWrapper;
import at.jku.isse.passiveprocessengine.instance.activeobjects.ProcessInstance;
import at.jku.isse.passiveprocessengine.instance.activeobjects.ProcessStep;

public class ConstraintResultWrapperTypeFactory extends AbstractTypeProvider {
	
	private static final String NS = ProcessInstanceScopeTypeFactory.NS+"/constraint";	
	
	public enum CoreProperties {constraintSpec, lastChanged, ruleResult, parentStep, isOverriden, overrideValue, overrideReason
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

	public ConstraintResultWrapperTypeFactory(RuleEnabledResolver schemaRegistry) {
		super(schemaRegistry);
		Optional<RDFInstanceType> thisType = schemaRegistry.findNonDeletedInstanceTypeByFQN(typeId);
		if (thisType.isPresent()) {
			this.type = thisType.get();
		} else {
			type = schemaRegistry.createNewInstanceType(typeId, schemaRegistry.findNonDeletedInstanceTypeByFQN(ProcessInstanceScopeTypeFactory.typeId).orElse(null));
		}
		metaElements.registerInstanceSpecificClass(typeId, ConstraintResultWrapper.class);
	}

	public void produceTypeProperties(ProcessInstanceScopeTypeFactory processInstanceScopeType) {
		type.cacheSuperProperties();
		type.createSinglePropertyType(CoreProperties.constraintSpec.toString(), 
				schemaRegistry.findNonDeletedInstanceTypeByFQN(ConstraintSpecTypeFactory.typeId)
				.map(vtype->vtype.getAsPropertyType())
				.orElse(null));
		type.createSinglePropertyType(CoreProperties.parentStep.toString(), 
				schemaRegistry.findNonDeletedInstanceTypeByFQN(AbstractProcessStepType.typeId)
				.map(vtype->vtype.getAsPropertyType())
				.orElse(null));
		type.createSinglePropertyType(CoreProperties.lastChanged.toString(), primitives.getStringType());
		type.createSinglePropertyType(CoreProperties.ruleResult.toString(), 
				schemaRegistry.findNonDeletedInstanceTypeByFQN(
						schemaRegistry.getRuleSchema().getResultBaseType().getURI())
				.map(vtype->vtype.getAsPropertyType())
				.orElseThrow());
		type.createSinglePropertyType(CoreProperties.isOverriden.toString(), primitives.getBooleanType());
		type.createSinglePropertyType(CoreProperties.overrideValue.toString(), primitives.getBooleanType());
		type.createSinglePropertyType(CoreProperties.overrideReason.toString(), primitives.getStringType());

		// so ugly:
		processInstanceScopeType.addGenericProcessProperty(type);
	}

	/**
	 * assuming that process has unique name across all processes and qaSpec has unique name within that process' definition
	 * */
	public static String generateId(ConstraintSpec qaSpec, ProcessInstance proc) {
		return qaSpec.getName()+"-"+proc.getName(); 
	}
	
	public ConstraintResultWrapper createInstance(ConstraintSpec qaSpec, ZonedDateTime lastChanged, ProcessStep owningStep, ProcessInstance proc) {
		var id = generateId(qaSpec, proc);
		RDFInstance inst = schemaRegistry.createInstance(id, schemaRegistry.findNonDeletedInstanceTypeByFQN(ConstraintResultWrapperTypeFactory.typeId)
				.orElseThrow());
		ConstraintResultWrapper cw = (ConstraintResultWrapper)inst;
		cw.setSingleProperty(ConstraintResultWrapperTypeFactory.CoreProperties.parentStep.toString(), owningStep.getInstance());
		cw.setSpec(qaSpec);
		cw.setLastChanged(lastChanged);
		cw.setProcess(proc);
		cw.setOverrideReason("");
		cw.setIsOverriden(false);
		return cw;
	}
}
