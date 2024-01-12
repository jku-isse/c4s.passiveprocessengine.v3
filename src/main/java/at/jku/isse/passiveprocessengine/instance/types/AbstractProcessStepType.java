package at.jku.isse.passiveprocessengine.instance.types;

import java.util.Optional;

import at.jku.isse.passiveprocessengine.core.BuildInType;
import at.jku.isse.passiveprocessengine.core.InstanceType;
import at.jku.isse.passiveprocessengine.core.SchemaRegistry;
import at.jku.isse.passiveprocessengine.core.TypeProvider;
import at.jku.isse.passiveprocessengine.definition.activeobjects.StepDefinition;
import at.jku.isse.passiveprocessengine.instance.activeobjects.ConstraintResultWrapper;
import at.jku.isse.passiveprocessengine.instance.activeobjects.DecisionNodeInstance;
import at.jku.isse.passiveprocessengine.instance.activeobjects.ProcessStep;

public class AbstractProcessStepType implements TypeProvider {

	public static enum CoreProperties {actualLifecycleState, expectedLifecycleState, stepDefinition, inDNI, outDNI, qaState,
	preconditions, postconditions, cancelconditions, activationconditions,
	processedPreCondFulfilled, processedPostCondFulfilled, processedCancelCondFulfilled, processedActivationCondFulfilled, isWorkExpected}

	private SchemaRegistry schemaRegistry;
	public static final String typeId = ProcessStep.class.getSimpleName();
		
	public AbstractProcessStepType(SchemaRegistry schemaRegistry) {
		this.schemaRegistry = schemaRegistry;
	}
	
	@Override
	public void produceTypeProperties() {
		Optional<InstanceType> thisType = schemaRegistry.findNonDeletedInstanceTypeById(typeId);
		if (thisType.isPresent())
			factory.registerType(ProcessStep.class, thisType.get());
		else {
			InstanceType type = schemaRegistry.createNewInstanceType(typeId);
			factory.registerType(ProcessStep.class, type);		
			
			type.createSinglePropertyType(CoreProperties.stepDefinition.toString(),factory.getType(StepDefinition.class));
			type.createSinglePropertyType(CoreProperties.inDNI.toString(),  factory.getType(DecisionNodeInstance.class));
			type.createSinglePropertyType(CoreProperties.outDNI.toString(),  factory.getType(DecisionNodeInstance.class));
			
			type.createSinglePropertyType(CoreProperties.actualLifecycleState.toString(),  BuildInType.STRING);
			type.createSinglePropertyType(CoreProperties.expectedLifecycleState.toString(), BuildInType.STRING);

			type.createSinglePropertyType(CoreProperties.processedPreCondFulfilled.toString(),  BuildInType.BOOLEAN);
			type.createSinglePropertyType(CoreProperties.processedPostCondFulfilled.toString(),  BuildInType.BOOLEAN);
			type.createSinglePropertyType(CoreProperties.processedCancelCondFulfilled.toString(),  BuildInType.BOOLEAN);
			type.createSinglePropertyType(CoreProperties.processedActivationCondFulfilled.toString(),  BuildInType.BOOLEAN);
			type.createSinglePropertyType(CoreProperties.isWorkExpected.toString(), BuildInType.BOOLEAN);
			// opposable no longer possible as, we cant then set it for pre/post, etc
			type.createMapPropertyType(CoreProperties.qaState.toString(), BuildInType.STRING, factory.getType(ConstraintResultWrapper.class));
			//check if we need to set step parent on opposite end --> we do now set it upon instantiation
			type.createMapPropertyType(CoreProperties.preconditions.toString(), BuildInType.STRING, factory.getType(ConstraintResultWrapper.class));
			type.createMapPropertyType(CoreProperties.postconditions.toString(),BuildInType.STRING, factory.getType(ConstraintResultWrapper.class));
			type.createMapPropertyType(CoreProperties.cancelconditions.toString(), BuildInType.STRING, factory.getType(ConstraintResultWrapper.class));
			type.createMapPropertyType(CoreProperties.activationconditions.toString(), BuildInType.STRING, factory.getType(ConstraintResultWrapper.class));

		}
		
	}
}
