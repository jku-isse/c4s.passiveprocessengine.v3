package at.jku.isse.passiveprocessengine.instance.types;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import at.jku.isse.passiveprocessengine.core.BuildInType;
import at.jku.isse.passiveprocessengine.core.InstanceType;
import at.jku.isse.passiveprocessengine.core.RuleDefinition;
import at.jku.isse.passiveprocessengine.core.SchemaRegistry;
import at.jku.isse.passiveprocessengine.definition.activeobjects.ConstraintSpec;
import at.jku.isse.passiveprocessengine.definition.activeobjects.ProcessDefinition;
import at.jku.isse.passiveprocessengine.definition.activeobjects.StepDefinition;
import at.jku.isse.passiveprocessengine.definition.types.ProcessDomainTypesRegistry;
import at.jku.isse.passiveprocessengine.definition.types.ProcessDomainTypesRegistry.TypeProvider;
import at.jku.isse.passiveprocessengine.instance.StepLifecycle.Conditions;
import at.jku.isse.passiveprocessengine.instance.activeobjects.ConstraintWrapper;
import at.jku.isse.passiveprocessengine.instance.activeobjects.DecisionNodeInstance;
import at.jku.isse.passiveprocessengine.instance.activeobjects.ProcessInstanceScopedElement;
import at.jku.isse.passiveprocessengine.instance.activeobjects.ProcessStep;

public class SpecificProcessStepType implements TypeProvider {

	public static enum CoreProperties {actualLifecycleState, expectedLifecycleState, stepDefinition, inDNI, outDNI, qaState,
	preconditions, postconditions, cancelconditions, activationconditions,
	processedPreCondFulfilled, processedPostCondFulfilled, processedCancelCondFulfilled, processedActivationCondFulfilled, isWorkExpected}

	private SchemaRegistry schemaRegistry;
	
	private final StepDefinition stepDef;
	private final InstanceType processType;

	public static final String CRD_PREFIX = "crd_";

	public static final String PREFIX_OUT = "out_";
	public static final String PREFIX_IN = "in_";
	public static final String CRD_DATAMAPPING_PREFIX = "crd_datamapping_";
	public static final String CRD_QASPEC_PREFIX = "crd_qaspec_";
		
	public SpecificProcessStepType(SchemaRegistry schemaRegistry, StepDefinition stepDef, InstanceType processType) {
		this.schemaRegistry = schemaRegistry;
		this.stepDef = stepDef;
		this.processType = processType;
	}
	
	@Override
	public void registerTypeInFactory(ProcessDomainTypesRegistry factory) {
		String stepName = SpecificProcessStepType.getProcessStepName(stepDef);
		Optional<InstanceType> thisType = schemaRegistry.findNonDeletedInstanceTypeById(stepName);
		if (thisType.isPresent())
			factory.registerTypeByName(thisType.get());	
		else {
			InstanceType type = schemaRegistry.createNewInstanceType(stepName, factory.getType(ProcessStep.class));
			factory.registerTypeByName(type);		

			stepDef.getExpectedInput().entrySet().stream()
			.forEach(entry -> {
				type.createSinglePropertyType(PREFIX_IN+entry.getKey(), entry.getValue());
			});
			stepDef.getExpectedOutput().entrySet().stream()
			.forEach(entry -> {
				type.createSinglePropertyType(PREFIX_OUT+entry.getKey(), entry.getValue());
			});
			stepDef.getInputToOutputMappingRules().entrySet().stream()
			.forEach(entry -> {
				if (entry.getValue() != null) {
					//TODO: do we really need to create those properties? or is creating just the rules ok? 
					//probably not: TODO: move the rule creation for iomapping into Factory of StepDefinition
					RuleDefinition crt =  RuleDefinition.create(ws, typeStep, getDataMappingId(entry, td), completeDatamappingRule(entry.getKey(), entry.getValue()));
					type.createPropertyType(CRD_DATAMAPPING_PREFIX+entry.getKey(), Cardinality.SINGLE, crt);
				}//assert ConsistencyUtils.crdValid(crt); as no workspace.concludeTransaction is called here, no need to assert this here, as will never be false here
			});
			// override process property type to actual process so we can access its config when needed
			if (processType != null) {
				if (type.getPropertyType(ProcessInstanceScopeType.CoreProperties.process.toString()) == null)
					type.createSinglePropertyType(ProcessInstanceScopeType.CoreProperties.process.toString(), processType);
				//else
				//typeStep.getPropertyType(ProcessInstanceScopedElement.CoreProperties.process.toString()).setInstanceType(processType);
			} else {
				ProcessInstanceScopeType.addGenericProcessProperty(type, factory);
			}
		}

	}
	
	private String completeDatamappingRule(String param, String rule) {
		return rule
				+"\r\n"
				+"->asSet()\r\n"
				+"->symmetricDifference(self.out_"+param+")\r\n"
				+"->size() = 0";
	}
	
	public static String getConstraintName(Conditions condition, InstanceType stepType) {
		return getConstraintName(condition, 0, stepType);
	}

	public static String getConstraintName(Conditions condition, int specOrderIndex, InstanceType stepType) {
		return CRD_PREFIX+condition+specOrderIndex+"_"+stepType.getName();
	}

	public static String getDataMappingId(Map.Entry<String,String> ioMapping, StepDefinition sd) {
		String procId = sd.getProcess() != null ? sd.getProcess().getName() : "";
		return CRD_DATAMAPPING_PREFIX+ioMapping.getKey()+"_"+sd.getName()+"_"+procId;
	}

	public static String getQASpecId(ConstraintSpec spec, ProcessDefinition context) {
		return CRD_QASPEC_PREFIX+spec.getConstraintId()+"_"+context.getName();
	}

	
	
	public static String getProcessStepName(StepDefinition sd) {
		String procName = sd.getProcess() != null ? sd.getProcess().getName() : "ROOTPROCESS";
		return AbstractProcessStepType.typeId+"_"+sd.getName()+"_"+procName;
	}
}
