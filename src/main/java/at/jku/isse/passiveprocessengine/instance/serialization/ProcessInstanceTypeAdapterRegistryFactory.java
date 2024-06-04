package at.jku.isse.passiveprocessengine.instance.serialization;

import java.util.Set;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.Stream.concat;

import java.util.Arrays;
import java.util.Collections;

import at.jku.isse.designspace.artifactconnector.core.repository.CoreTypeFactory;
import at.jku.isse.passiveprocessengine.core.ProcessContext;
import at.jku.isse.passiveprocessengine.core.SchemaRegistry;
import at.jku.isse.passiveprocessengine.core.serialization.ConfigurablePropertyTypeAdapter;
import at.jku.isse.passiveprocessengine.core.serialization.TypeAdapterRegistry;
import at.jku.isse.passiveprocessengine.definition.activeobjects.ConstraintSpec;
import at.jku.isse.passiveprocessengine.definition.types.ConstraintSpecType;
import at.jku.isse.passiveprocessengine.instance.activeobjects.ConstraintResultWrapper;
import at.jku.isse.passiveprocessengine.instance.activeobjects.DecisionNodeInstance;
import at.jku.isse.passiveprocessengine.instance.activeobjects.ProcessInstance;
import at.jku.isse.passiveprocessengine.instance.activeobjects.ProcessStep;
import at.jku.isse.passiveprocessengine.instance.types.AbstractProcessStepType;
import at.jku.isse.passiveprocessengine.instance.types.ConstraintWrapperType;
import at.jku.isse.passiveprocessengine.instance.types.DecisionNodeInstanceType;
import at.jku.isse.passiveprocessengine.instance.types.ProcessInstanceScopeType;
import at.jku.isse.passiveprocessengine.instance.types.SpecificProcessInstanceType;

public class ProcessInstanceTypeAdapterRegistryFactory {

	
	public static TypeAdapterRegistry buildRegistry(ProcessContext context) {
		// we need the context as only then can we be sure to have the process definition and instance base types initialized
		SchemaRegistry schemaReg = context.getSchemaRegistry();
		
		TypeAdapterRegistry typeAdapterRegistry = new TypeAdapterRegistry();
		
		// process instance
		typeAdapterRegistry.registerTypeAdapter(
				new StepTypeAdapter(typeAdapterRegistry 
						, concat(getShallowProcessInstanceProperties().stream(), getShallowProcessStepProperties().stream()).collect(toSet())
						, concat(getDeepProcessInstanceProperties().stream(), getDeepProcessStepProperties().stream()).collect(toSet())
						, context)
				, schemaReg.getType(ProcessInstance.class));
		// process step
		typeAdapterRegistry.registerTypeAdapter(
				new StepTypeAdapter(typeAdapterRegistry 
						, getShallowProcessStepProperties()
						, getDeepProcessStepProperties()
						, context)
				, schemaReg.getType(ProcessStep.class));
		// decision node - only shallow
		typeAdapterRegistry.registerTypeAdapter(
				new ConfigurablePropertyTypeAdapter(typeAdapterRegistry 
						, getShallowDecisionNodeProperties()
						, Collections.emptySet())
				, schemaReg.getType(DecisionNodeInstance.class));
		// constraint wrapper - only deep
		typeAdapterRegistry.registerTypeAdapter(
				new ConfigurablePropertyTypeAdapter(typeAdapterRegistry 
						, Collections.emptySet()
						, getDeepProcessWrapperProperties() )
				, schemaReg.getType(ConstraintResultWrapper.class));
		// constraint spec, wrapper needs spec as there the actual constraint is described, wrapper could be also shallow
		typeAdapterRegistry.registerTypeAdapter(
				new ConfigurablePropertyTypeAdapter(typeAdapterRegistry 
						, getShallowConstraintSpecProperties()
						, Collections.emptySet())
				, schemaReg.getType(ConstraintSpec.class));
		// Artifacts minimal info - only shallow
		typeAdapterRegistry.registerTypeAdapter(
				new ConfigurablePropertyTypeAdapter(typeAdapterRegistry 
						, getShallowArtifactProperties()
						, Collections.emptySet())
				, schemaReg.getTypeByName(CoreTypeFactory.BASE_TYPE_NAME));
		
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
		return Set.of(ProcessInstanceScopeType.CoreProperties.process.toString()
				);
	}
	
	private static Set<String> getShallowDecisionNodeProperties() {
		return Arrays.asList(DecisionNodeInstanceType.CoreProperties.values()).stream()
				.map(prop -> prop.toString())
				.collect(toSet());
	}
	
	private static Set<String> getDeepProcessWrapperProperties() {
		return Set.of(ConstraintWrapperType.CoreProperties.qaSpec.toString()
				,ConstraintWrapperType.CoreProperties.lastChanged.toString()
				,ConstraintWrapperType.CoreProperties.isOverriden.toString()
				,ConstraintWrapperType.CoreProperties.overrideValue.toString()
				,ConstraintWrapperType.CoreProperties.overrideReason.toString()
				);
	}
	
	/* only needed when used standalone outside of process json*/
	private static Set<String> getShallowProcessWrapperProperties() {
		return Set.of(ConstraintWrapperType.CoreProperties.parentStep.toString());
	}
	
	private static Set<String> getShallowConstraintSpecProperties() {
		return Set.of(ConstraintSpecType.CoreProperties.constraintSpec.toString()
				, ConstraintSpecType.CoreProperties.humanReadableDescription.toString()
				, ConstraintSpecType.CoreProperties.specOrderIndex.toString()
				, ConstraintSpecType.CoreProperties.isOverridable.toString()
				, ConstraintSpecType.CoreProperties.ruleType.toString()
				, ConstraintSpecType.CoreProperties.conditionsType.toString()
				);
	}
	
	private static Set<String> getShallowArtifactProperties() {
		return Set.of(CoreTypeFactory.EXTERNAL_DEFAULT_ID
				, CoreTypeFactory.EXTERNAL_TYPE
				, CoreTypeFactory.URL
				, CoreTypeFactory.LAST_UPDATE);
	}
}
