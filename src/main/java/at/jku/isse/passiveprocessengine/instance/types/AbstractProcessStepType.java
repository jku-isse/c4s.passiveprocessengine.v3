package at.jku.isse.passiveprocessengine.instance.types;

import java.util.Optional;

import at.jku.isse.passiveprocessengine.core.AbstractTypeProvider;
import at.jku.isse.passiveprocessengine.rdfwrapper.NodeToDomainResolver;
import at.jku.isse.passiveprocessengine.rdfwrapper.RDFInstanceType;
import at.jku.isse.passiveprocessengine.rdfwrapper.rule.RuleEnabledResolver;
import at.jku.isse.passiveprocessengine.definition.types.StepDefinitionTypeFactory;
import at.jku.isse.passiveprocessengine.instance.activeobjects.ProcessStep;

public class AbstractProcessStepType extends AbstractTypeProvider {

	private static final String NS = ProcessInstanceScopeTypeFactory.NS+"/abstractstep";
	
	public enum CoreProperties {actualLifecycleState, expectedLifecycleState, stepDefinition, inDNI, outDNI, qaState,
		preconditions, postconditions, cancelconditions, activationconditions,
		processedPreCondFulfilled, processedPostCondFulfilled, processedCancelCondFulfilled, processedActivationCondFulfilled, 
		isWorkExpected
		;
		
		@Override
		public String toString() {
			return NS+name();
		}
		
		public String getURI() {
			return NS+name();
		}
	}

	public static final String typeId = NS+"#ProcessStep";

	public AbstractProcessStepType(RuleEnabledResolver schemaRegistry) {
		super(schemaRegistry);
		Optional<RDFInstanceType> thisType = schemaRegistry.findNonDeletedInstanceTypeByFQN(typeId);
		if (thisType.isPresent()) {	
			this.type = thisType.get();
		} else {
			this.type = schemaRegistry.createNewInstanceType(typeId, schemaRegistry.findNonDeletedInstanceTypeByFQN(ProcessInstanceScopeTypeFactory.typeId).orElseThrow());			
		}		

	}

	public void produceTypeProperties() {
		type.cacheSuperProperties();
		type.createSinglePropertyType(CoreProperties.stepDefinition.toString(),
				schemaRegistry.findNonDeletedInstanceTypeByFQN(StepDefinitionTypeFactory.typeId)
				.map(vtype->vtype.getAsPropertyType())
				.orElseThrow());
		type.createSinglePropertyType(CoreProperties.inDNI.toString(), 
				schemaRegistry.findNonDeletedInstanceTypeByFQN(DecisionNodeInstanceTypeFactory.typeId)
				.map(vtype->vtype.getAsPropertyType())
				.orElse(null));
		type.createSinglePropertyType(CoreProperties.outDNI.toString(), 
				schemaRegistry.findNonDeletedInstanceTypeByFQN(DecisionNodeInstanceTypeFactory.typeId)
				.map(vtype->vtype.getAsPropertyType())
				.orElseThrow());
		type.createSinglePropertyType(CoreProperties.actualLifecycleState.toString(),  primitives.getStringType());
		type.createSinglePropertyType(CoreProperties.expectedLifecycleState.toString(), primitives.getStringType());

		type.createSinglePropertyType(CoreProperties.processedPreCondFulfilled.toString(),  primitives.getBooleanType());
		type.createSinglePropertyType(CoreProperties.processedPostCondFulfilled.toString(),  primitives.getBooleanType());
		type.createSinglePropertyType(CoreProperties.processedCancelCondFulfilled.toString(),  primitives.getBooleanType());
		type.createSinglePropertyType(CoreProperties.processedActivationCondFulfilled.toString(), primitives.getBooleanType());
		type.createSinglePropertyType(CoreProperties.isWorkExpected.toString(), primitives.getBooleanType());
		// opposable no longer possible as, we cant then set it for pre/post, etc
		type.createMapPropertyType(CoreProperties.qaState.toString(),  
				schemaRegistry.findNonDeletedInstanceTypeByFQN(ConstraintResultWrapperTypeFactory.typeId)
				.map(vtype->vtype.getAsPropertyType())
				.orElseThrow());
		//check if we need to set step parent on opposite end --> we do now set it upon instantiation
		type.createMapPropertyType(CoreProperties.preconditions.toString(),  
				schemaRegistry.findNonDeletedInstanceTypeByFQN(ConstraintResultWrapperTypeFactory.typeId)
				.map(vtype->vtype.getAsPropertyType())
				.orElseThrow());
		type.createMapPropertyType(CoreProperties.postconditions.toString(), 
				schemaRegistry.findNonDeletedInstanceTypeByFQN(ConstraintResultWrapperTypeFactory.typeId)
				.map(vtype->vtype.getAsPropertyType())
				.orElseThrow());
		type.createMapPropertyType(CoreProperties.cancelconditions.toString(),  
				schemaRegistry.findNonDeletedInstanceTypeByFQN(ConstraintResultWrapperTypeFactory.typeId)
				.map(vtype->vtype.getAsPropertyType())
				.orElseThrow());
		type.createMapPropertyType(CoreProperties.activationconditions.toString(),  
				schemaRegistry.findNonDeletedInstanceTypeByFQN(ConstraintResultWrapperTypeFactory.typeId)
				.map(vtype->vtype.getAsPropertyType())
				.orElseThrow());

	}
}
