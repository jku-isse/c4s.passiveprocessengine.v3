package at.jku.isse.passiveprocessengine.definition.factories;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import at.jku.isse.designspace.rule.arl.evaluator.RuleDefinition;
import at.jku.isse.passiveprocessengine.core.FactoryIndex.DomainFactory;
import at.jku.isse.passiveprocessengine.rdfwrapper.RDFInstance;
import at.jku.isse.passiveprocessengine.rdfwrapper.RDFInstanceType;
import at.jku.isse.passiveprocessengine.rdfwrapper.rule.RDFRuleDefinitionWrapper;
import at.jku.isse.passiveprocessengine.rdfwrapper.rule.RuleEnabledResolver;
import at.jku.isse.passiveprocessengine.rules.RewriterFactory;
import at.jku.isse.passiveprocessengine.definition.ProcessDefinitionError;
import at.jku.isse.passiveprocessengine.definition.activeobjects.ConstraintSpec;
import at.jku.isse.passiveprocessengine.definition.activeobjects.ProcessDefinition;
import at.jku.isse.passiveprocessengine.definition.activeobjects.StepDefinition;
import at.jku.isse.passiveprocessengine.definition.types.ProcessDefinitionTypeFactory;
import at.jku.isse.passiveprocessengine.instance.StepLifecycle.Conditions;
import at.jku.isse.passiveprocessengine.instance.types.ProcessInstanceScopeTypeFactory;
import at.jku.isse.passiveprocessengine.instance.types.SpecificProcessInstanceType;
import at.jku.isse.passiveprocessengine.instance.types.SpecificProcessStepType;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * Creates for a process definition the specific instance types, i.e., process step and dni subtypes 
 * that are specific for this process definition, so that we can execute rules over them
 */
@Slf4j
public class SpecificProcessInstanceTypesFactory {

	public static final String CRD_PREFIX = "crd_";
	public static final String CRD_DATAMAPPING_PREFIX = "crd_datamapping_";
	public static final String CRD_QASPEC_PREFIX = "crd_qaspec_";

	final RewriterFactory ruleService;
	final ProcessInstanceScopeTypeFactory scopeFactory;
	@Getter final RuleEnabledResolver context;

	
	public SpecificProcessInstanceTypesFactory(@NonNull RuleEnabledResolver context, @NonNull RewriterFactory ruleService, @NonNull ProcessInstanceScopeTypeFactory scopeFactory) {
		this.context = context;
		this.ruleService = ruleService;
		this.scopeFactory = scopeFactory;
	}
		
	/**
	 * 
	 * @param processDef
	 * @return any errors that occurred while instantiating all specific StepTypes and their respective RuleDefinitions, includes rule augmentation
	 * includes checking of process wellformedness constraints
	 */
	public List<ProcessDefinitionError> initializeInstanceTypes(ProcessDefinition processDef) {
		List<ProcessDefinitionError> errors = new LinkedList<>();
		SpecificProcessStepType processAsStepTypeProvider = new SpecificProcessStepType(context, processDef, scopeFactory);
		processAsStepTypeProvider.produceTypeProperties();
		SpecificProcessInstanceType typeProvider = new SpecificProcessInstanceType(context, processDef);
		typeProvider.produceTypeProperties();				
		RDFInstanceType processInstanceType = context.findNonDeletedInstanceTypeByFQN(SpecificProcessInstanceType.getProcessName(processDef)).get(); //) ProcessInstance.getOrCreateDesignSpaceInstanceType(instance.workspace, this);

		processDef.getStepDefinitions().stream().forEach(stepDef -> {
			if (stepDef instanceof ProcessDefinition procDef) {
				log.debug("Skipping creation of Datamapping Rule for Subprocess Step: "+stepDef.getName());
				errors.addAll(initializeInstanceTypes(procDef));
			} else {
				// create the specific step type
				SpecificProcessStepType stepTypeProvider = new SpecificProcessStepType(context, stepDef, processInstanceType, scopeFactory);
				stepTypeProvider.produceTypeProperties();			
			}
		});
		errors.addAll(checkProcessStructure(processDef));
		errors.addAll(new RuleAugmentation(processDef, processInstanceType, context, ruleService).augmentAndCreateConditions());
		processDef.getStepDefinitions().stream().forEach(stepDef -> {
			errors.addAll(new RuleAugmentation(stepDef, 
												context.findNonDeletedInstanceTypeByFQN(SpecificProcessStepType.getProcessStepName(stepDef)).get(), 
												context, 
												ruleService)
								.augmentAndCreateConditions());
		});
		errors.addAll(checkConstraintValidity(processDef, processInstanceType));
		
		if (errors.isEmpty() || errors.stream().allMatch(error -> !error.getSeverity().equals(ProcessDefinitionError.Severity.ERROR))) {
			processDef.setIsWithoutBlockingErrors(true);
		} else {
			log.info("Blocking newly added process due to constraint errors: "+processDef.getName());
			processDef.setIsWithoutBlockingErrors(false);
		}
		return errors;
	}

	public List<ProcessDefinitionError> checkConstraintValidity(ProcessDefinition processDef, RDFInstanceType processInstanceType) {
		List<ProcessDefinitionError> overallStatus = new LinkedList<>();				
		processDef.getStepDefinitions().forEach(sd -> overallStatus.addAll( sd.checkConstraintValidity(processInstanceType)));
		return overallStatus;
	}

	public List<ProcessDefinitionError> checkProcessStructure(ProcessDefinition processDef) {
		List<ProcessDefinitionError> status = new LinkedList<>();
		if (processDef.getDecisionNodeDefinitions().stream().filter(dnd -> dnd.getInSteps().isEmpty()).count() > 1)
			status.add(new ProcessDefinitionError(processDef, "Invalid Process Structure", "More than one entry decision node found", ProcessDefinitionError.Severity.ERROR));
		if (processDef.getDecisionNodeDefinitions().stream().filter(dnd -> dnd.getOutSteps().isEmpty()).count() > 1)
			status.add(new ProcessDefinitionError(processDef, "Invalid Process Structure", "More than one exit decision node found", ProcessDefinitionError.Severity.ERROR));
		if (processDef.getExpectedInput().isEmpty()) {
			status.add(new ProcessDefinitionError(processDef, "No Input Defined", "Step needs at least one input.", ProcessDefinitionError.Severity.ERROR));
		}
		processDef.getExpectedInput().entrySet().stream().forEach(entry -> {
			if (entry.getValue() == null)
				status.add(new ProcessDefinitionError(processDef, "Unavailable Type", "Artifact type of input '"+entry.getKey()+"' could not be resolved", ProcessDefinitionError.Severity.ERROR));
		});
		processDef.getExpectedOutput().entrySet().stream().forEach(entry -> {
			if (entry.getValue() == null)
				status.add(new ProcessDefinitionError(processDef, "Unavailable Type", "Artifact type of output '"+entry.getKey()+"' could not be resolved", ProcessDefinitionError.Severity.ERROR));
		});

		processDef.getStepDefinitions().stream()
			.filter(sd -> !(sd instanceof ProcessDefinition))
			.forEach(sd -> status.addAll( sd.checkStepStructureValidity()));
		processDef.getDecisionNodeDefinitions().stream()
			.forEach(dnd -> status.addAll(dnd.checkDecisionNodeStructureValidity()));
		return status;
	}

	public static String getDataMappingId(Map.Entry<String,String> ioMapping, StepDefinition sd) {
		String procId = sd.getProcess() != null ? sd.getProcess().getName() : "";
		return CRD_DATAMAPPING_PREFIX+ioMapping.getKey()+"_"+sd.getName()+"_"+procId;
	}


	public static String getConstraintName(Conditions condition, RDFInstanceType stepType) {
		return getConstraintName(condition, 0, stepType);
	}

	public static String getConstraintName(Conditions condition, int specOrderIndex, RDFInstanceType stepType) {
		return CRD_PREFIX+condition+specOrderIndex+"_"+stepType.getName();
	}

	public static String getQASpecId(ConstraintSpec spec, ProcessDefinition processContext) {
		return CRD_QASPEC_PREFIX+spec.getConstraintId()+"_"+processContext.getName();
	}
}
