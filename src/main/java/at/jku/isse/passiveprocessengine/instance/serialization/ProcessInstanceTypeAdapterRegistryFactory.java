package at.jku.isse.passiveprocessengine.instance.serialization;

import static java.util.stream.Collectors.toSet;
import static java.util.stream.Stream.concat;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import at.jku.isse.passiveprocessengine.definition.types.ConstraintSpecTypeFactory;
import at.jku.isse.passiveprocessengine.instance.types.AbstractProcessInstanceType;
import at.jku.isse.passiveprocessengine.instance.types.AbstractProcessStepType;
import at.jku.isse.passiveprocessengine.instance.types.ConstraintResultWrapperTypeFactory;
import at.jku.isse.passiveprocessengine.instance.types.DecisionNodeInstanceTypeFactory;
import at.jku.isse.passiveprocessengine.instance.types.ProcessInstanceScopeTypeFactory;
import at.jku.isse.passiveprocessengine.instance.types.SpecificProcessInstanceType;
import at.jku.isse.passiveprocessengine.rdfwrapper.CoreTypeFactory;

public class ProcessInstanceTypeAdapterRegistryFactory {

	
	public static TypeAdapterRegistry buildRegistry(RuleEnabledResolver context) {
		// we need the context as only then can we be sure to have the process definition and instance base types initialized
		NodeToDomainResolver schemaReg = context.getSchemaRegistry();
		
		TypeAdapterRegistry typeAdapterRegistry = new TypeAdapterRegistry();
		
		// process instance
		typeAdapterRegistry.registerTypeAdapter(
				new StepTypeAdapter(typeAdapterRegistry 
						, concat(getShallowProcessInstanceProperties().stream(), getShallowProcessStepProperties().stream()).collect(toSet())
						, concat(getDeepProcessInstanceProperties().stream(), getDeepProcessStepProperties().stream()).collect(toSet())
						, context)
				, schemaReg.findNonDeletedInstanceTypeByFQN(AbstractProcessInstanceType.typeId));
		// process step
		typeAdapterRegistry.registerTypeAdapter(
				new StepTypeAdapter(typeAdapterRegistry 
						, getShallowProcessStepProperties()
						, getDeepProcessStepProperties()
						, context)
				, schemaReg.findNonDeletedInstanceTypeByFQN(AbstractProcessStepType.typeId));
		// decision node - only shallow
		typeAdapterRegistry.registerTypeAdapter(
				new ConfigurablePropertyTypeAdapter(typeAdapterRegistry 
						, getShallowDecisionNodeProperties()
						, Collections.emptySet())
				, schemaReg.findNonDeletedInstanceTypeByFQN(DecisionNodeInstanceTypeFactory.typeId));
		// constraint wrapper - only deep
		typeAdapterRegistry.registerTypeAdapter(
				new ConfigurablePropertyTypeAdapter(typeAdapterRegistry 
						, Collections.emptySet()
						, getDeepProcessWrapperProperties() )
				, schemaReg.findNonDeletedInstanceTypeByFQN(ConstraintResultWrapperTypeFactory.typeId));
		// constraint spec, wrapper needs spec as there the actual constraint is described, wrapper could be also shallow
		typeAdapterRegistry.registerTypeAdapter(
				new ConfigurablePropertyTypeAdapter(typeAdapterRegistry 
						, getShallowConstraintSpecProperties()
						, Collections.emptySet())
				, schemaReg.findNonDeletedInstanceTypeByFQN(ConstraintSpecTypeFactory.typeId));
		// Artifacts minimal info - only shallow
		typeAdapterRegistry.registerTypeAdapter(
				new ConfigurablePropertyTypeAdapter(typeAdapterRegistry 
						, getShallowArtifactProperties()
						, Collections.emptySet())
				, schemaReg.findNonDeletedInstanceTypeByFQN(CoreTypeFactory.BASE_TYPE_URI));
		
		return typeAdapterRegistry;
	}
	
	private static Set<String> getDeepProcessInstanceProperties() {
		return Set.of(SpecificProcessInstanceType.CoreProperties.createdAt.toString() 
				,SpecificProcessInstanceType.CoreProperties.stepInstances.toString()
				,SpecificProcessInstanceType.CoreProperties.decisionNodeInstances.toString()
				);
	}
	
	private static Set<String> getShallowProcessInstanceProperties() {
		return Set.of(SpecificProcessInstanceType.CoreProperties.processDefinition.toString()
				);
	}
	
	private static Set<String> getDeepProcessStepProperties() {
		return Set.of(
				AbstractProcessStepType.CoreProperties.actualLifecycleState.toString()
				,AbstractProcessStepType.CoreProperties.expectedLifecycleState.toString()
				,AbstractProcessStepType.CoreProperties.isWorkExpected.toString()
				,AbstractProcessStepType.CoreProperties.processedPreCondFulfilled.toString()
				,AbstractProcessStepType.CoreProperties.preconditions.toString()
				,AbstractProcessStepType.CoreProperties.processedPostCondFulfilled.toString()
				,AbstractProcessStepType.CoreProperties.postconditions.toString()
				,AbstractProcessStepType.CoreProperties.processedCancelCondFulfilled.toString()
				,AbstractProcessStepType.CoreProperties.cancelconditions.toString()
				,AbstractProcessStepType.CoreProperties.processedActivationCondFulfilled.toString()
				,AbstractProcessStepType.CoreProperties.activationconditions.toString()
				,AbstractProcessStepType.CoreProperties.qaState.toString()			
				);
		//TODO: unsure how to handle input and output?!
	}
	
	private static Set<String> getShallowProcessStepProperties() {
		return Set.of(AbstractProcessStepType.CoreProperties.stepDefinition.toString()
				,AbstractProcessStepType.CoreProperties.inDNI.toString()
				,AbstractProcessStepType.CoreProperties.outDNI.toString()
				);
	}
	
	/* only needed when used standalone outside of process json*/
	private static Set<String> getShallowProcessScopeProperties() {
		return Set.of(ProcessInstanceScopeTypeFactory.CoreProperties.process.toString()
				);
	}
	
	private static Set<String> getShallowDecisionNodeProperties() {
		return Arrays.asList(DecisionNodeInstanceTypeFactory.CoreProperties.values()).stream()
				.map(prop -> prop.toString())
				.collect(toSet());
	}
	
	private static Set<String> getDeepProcessWrapperProperties() {
		return Set.of(ConstraintResultWrapperTypeFactory.CoreProperties.constraintSpec.toString()
				,ConstraintResultWrapperTypeFactory.CoreProperties.lastChanged.toString()
				,ConstraintResultWrapperTypeFactory.CoreProperties.isOverriden.toString()
				,ConstraintResultWrapperTypeFactory.CoreProperties.overrideValue.toString()
				,ConstraintResultWrapperTypeFactory.CoreProperties.overrideReason.toString()
				);
	}
	
	/* only needed when used standalone outside of process json*/
	private static Set<String> getShallowProcessWrapperProperties() {
		return Set.of(ConstraintResultWrapperTypeFactory.CoreProperties.parentStep.toString());
	}
	
	private static Set<String> getShallowConstraintSpecProperties() {
		return Set.of(ConstraintSpecTypeFactory.CoreProperties.constraintSpec.toString()
				, ConstraintSpecTypeFactory.CoreProperties.humanReadableDescription.toString()
				, ConstraintSpecTypeFactory.CoreProperties.constraintSpecOrderIndex.toString()
				, ConstraintSpecTypeFactory.CoreProperties.isOverridable.toString()
				, ConstraintSpecTypeFactory.CoreProperties.ruleType.toString()
				, ConstraintSpecTypeFactory.CoreProperties.conditionsType.toString()
				);
	}
	
	private static Set<String> getShallowArtifactProperties() {
		return Set.of(CoreTypeFactory.EXTERNAL_DEFAULT_ID_URI
				, CoreTypeFactory.EXTERNAL_TYPE_URI
				, CoreTypeFactory.URL_URI
				, CoreTypeFactory.LAST_UPDATE_URI);
	}
}
