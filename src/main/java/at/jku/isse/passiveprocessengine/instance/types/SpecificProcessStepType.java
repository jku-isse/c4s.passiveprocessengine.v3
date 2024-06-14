package at.jku.isse.passiveprocessengine.instance.types;

import java.util.Optional;

import at.jku.isse.passiveprocessengine.core.PPEInstanceType;
import at.jku.isse.passiveprocessengine.core.SchemaRegistry;
import at.jku.isse.passiveprocessengine.core.TypeProviderBase;
import at.jku.isse.passiveprocessengine.definition.activeobjects.StepDefinition;
import at.jku.isse.passiveprocessengine.instance.activeobjects.ProcessInstance;
import at.jku.isse.passiveprocessengine.instance.activeobjects.ProcessStep;
import lombok.NonNull;

public class SpecificProcessStepType extends TypeProviderBase {

//	public static enum CoreProperties {actualLifecycleState, expectedLifecycleState, stepDefinition, inDNI, outDNI, qaState,
//	preconditions, postconditions, cancelconditions, activationconditions,
//	processedPreCondFulfilled, processedPostCondFulfilled, processedCancelCondFulfilled, processedActivationCondFulfilled, isWorkExpected}

	private final StepDefinition stepDef;
	private final PPEInstanceType processType;

	public static final String PREFIX_OUT = "out_";
	public static final String PREFIX_IN = "in_";

		
	public SpecificProcessStepType(SchemaRegistry schemaRegistry, StepDefinition stepDef, @NonNull PPEInstanceType processType) {
		super(schemaRegistry);
		this.stepDef = stepDef;
		this.processType = processType;
	}
	
	public SpecificProcessStepType(SchemaRegistry schemaRegistry, StepDefinition stepDef) {
		super(schemaRegistry);
		this.stepDef = stepDef;	
		this.processType = null;
	}
	
	@Override
	public void produceTypeProperties() {
		String stepName = SpecificProcessStepType.getProcessStepName(stepDef);
		Optional<PPEInstanceType> thisType = schemaRegistry.findNonDeletedInstanceTypeByFQN(stepName);
		if (thisType.isPresent())
			schemaRegistry.registerTypeByName(thisType.get());	
		else {
			PPEInstanceType type = processType == null ? 
				schemaRegistry.createNewInstanceType(stepName, schemaRegistry.getType(ProcessInstance.class)) :			
				schemaRegistry.createNewInstanceType(stepName, schemaRegistry.getType(ProcessStep.class));			
			schemaRegistry.registerTypeByName(type);		

			stepDef.getExpectedInput().entrySet().stream()
			.forEach(entry -> {
				type.createSetPropertyType(PREFIX_IN+entry.getKey(), entry.getValue());
			});
			stepDef.getExpectedOutput().entrySet().stream()
			.forEach(entry -> {
				type.createSetPropertyType(PREFIX_OUT+entry.getKey(), entry.getValue());
			});
			// DONE IN ProcessDefinitionschemaRegistry
			//			stepDef.getInputToOutputMappingRules().entrySet().stream()
//			.forEach(entry -> {
//				if (entry.getValue() != null) {
//					RuleDefinition crt = this. RuleDefinition.create(type, getDataMappingId(entry, stepDef), completeDatamappingRule(entry.getKey(), entry.getValue()));
//					// do we really need to create those properties? or is creating just the rules ok? probably not
//					//type.createPropertyType(CRD_DATAMAPPING_PREFIX+entry.getKey(), Cardinality.SINGLE, crt);
//				}
//			});
			// override process property type to actual process so we can access its config when needed
			if (processType != null) {
				if (type.getPropertyType(ProcessInstanceScopeType.CoreProperties.process.toString()) == null)
					type.createSinglePropertyType(ProcessInstanceScopeType.CoreProperties.process.toString(), processType);
				//else
				//typeStep.getPropertyType(ProcessInstanceScopedElement.CoreProperties.process.toString()).setInstanceType(processType);
			} else {
				ProcessInstanceScopeType.addGenericProcessProperty(type, schemaRegistry);
			}
		}

	}
	
	public static String getProcessStepName(StepDefinition sd) {
		String procName = sd.getProcess() != null ? sd.getProcess().getName() : "ROOTPROCESS";
		return AbstractProcessStepType.typeId+"_"+sd.getName()+"_"+procName;
	}
}
