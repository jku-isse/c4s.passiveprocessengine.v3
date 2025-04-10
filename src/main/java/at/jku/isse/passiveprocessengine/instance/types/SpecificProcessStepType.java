package at.jku.isse.passiveprocessengine.instance.types;

import java.util.Optional;

import at.jku.isse.passiveprocessengine.rdfwrapper.RDFInstanceType;
import at.jku.isse.passiveprocessengine.core.SchemaRegistry;
import at.jku.isse.passiveprocessengine.core.TypeProviderBase;
import at.jku.isse.passiveprocessengine.definition.activeobjects.StepDefinition;
import at.jku.isse.passiveprocessengine.rdfwrapper.RDFInstanceType;
import lombok.NonNull;

public class SpecificProcessStepType extends TypeProviderBase {

//	public static enum CoreProperties {actualLifecycleState, expectedLifecycleState, stepDefinition, inDNI, outDNI, qaState,
//	preconditions, postconditions, cancelconditions, activationconditions,
//	processedPreCondFulfilled, processedPostCondFulfilled, processedCancelCondFulfilled, processedActivationCondFulfilled, isWorkExpected}

	private final StepDefinition stepDef;
	private final RDFInstanceType processType;

	public static final String PREFIX_OUT = "out_";
	public static final String PREFIX_IN = "in_";

		
	public SpecificProcessStepType(SchemaRegistry schemaRegistry, StepDefinition stepDef, @NonNull RDFInstanceType processType) {
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
		Optional<RDFInstanceType> thisType = schemaRegistry.findNonDeletedInstanceTypeByFQN(stepName);
		if (thisType.isPresent()) {
			((RDFInstanceType) thisType.get()).cacheSuperProperties();
			type = thisType.get();	
		} else {
			RDFInstanceType type = processType == null ? 
				schemaRegistry.createNewInstanceType(stepName, schemaRegistry.getTypeByName(AbstractProcessInstanceType.typeId)) :			
				schemaRegistry.createNewInstanceType(stepName, schemaRegistry.getTypeByName(AbstractProcessStepType.typeId));			

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
			this.type = type;
		}

	}
	
	public static String getProcessStepName(StepDefinition sd) {
//		if (sd instanceof ProcessDefinition procDef) {
//			return SpecificProcessInstanceType.getProcessName(procDef);
//		}
		String procName = sd.getProcess() != null ? sd.getProcess().getName() : "ROOTPROCESS";
		return AbstractProcessStepType.typeId+"-"+sd.getName()+"-"+procName;
	}
}
