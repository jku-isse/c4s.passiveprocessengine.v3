package at.jku.isse.passiveprocessengine.definition.factories;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import at.jku.isse.passiveprocessengine.core.FactoryIndex.DomainFactory;
import at.jku.isse.passiveprocessengine.core.ProcessContext;
import at.jku.isse.passiveprocessengine.core.PPEInstance;
import at.jku.isse.passiveprocessengine.core.PPEInstanceType;
import at.jku.isse.passiveprocessengine.core.RuleDefinition;
import at.jku.isse.passiveprocessengine.definition.ProcessDefinitionError;
import at.jku.isse.passiveprocessengine.definition.activeobjects.ConstraintSpec;
import at.jku.isse.passiveprocessengine.definition.activeobjects.ProcessDefinition;
import at.jku.isse.passiveprocessengine.definition.activeobjects.StepDefinition;
import at.jku.isse.passiveprocessengine.definition.types.ProcessDefinitionType;
import at.jku.isse.passiveprocessengine.designspace.RewriterFactory;
import at.jku.isse.passiveprocessengine.instance.StepLifecycle.Conditions;
import at.jku.isse.passiveprocessengine.instance.types.SpecificProcessInstanceType;
import at.jku.isse.passiveprocessengine.instance.types.SpecificProcessStepType;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ProcessDefinitionFactory extends DomainFactory {

	public static final String CRD_PREFIX = "crd_";
	public static final String CRD_DATAMAPPING_PREFIX = "crd_datamapping_";
	public static final String CRD_QASPEC_PREFIX = "crd_qaspec_";

	final RewriterFactory ruleService;

	public ProcessDefinitionFactory(ProcessContext context, RewriterFactory ruleService) {
		super(context);
		this.ruleService = ruleService;
	}

	/**
	 * 
	 * @param processId
	 * @return Basic, empty process definition structure. Creation of rules and specific process instance types requires calling 'initializeInstanceTypes'
	 */
	public ProcessDefinition createInstance(String processId) {
		PPEInstance instance = getContext().getInstanceRepository().createInstance(processId, getContext().getSchemaRegistry().getType(ProcessDefinition.class));
		instance.setSingleProperty(ProcessDefinitionType.CoreProperties.isWithoutBlockingErrors.toString(), false);
		return getContext().getWrappedInstance(ProcessDefinition.class, instance);				
	}

	/**
	 * 
	 * @param processDef
	 * @param doGeneratePrematureDetectionConstraints (ignored for now)
	 * @return any errors that occurred while instantiating all specific StepTypes and their respective RuleDefinitions, includes rule augmentation
	 * includes checking of process wellformedness constraints
	 */
	public List<ProcessDefinitionError> initializeInstanceTypes(ProcessDefinition processDef, boolean doGeneratePrematureDetectionConstraints) {
		List<ProcessDefinitionError> errors = new LinkedList<>();
		SpecificProcessStepType processAsStepTypeProvider = new SpecificProcessStepType(getContext().getSchemaRegistry(), processDef);
		processAsStepTypeProvider.produceTypeProperties();
		SpecificProcessInstanceType typeProvider = new SpecificProcessInstanceType(getContext().getSchemaRegistry(), processDef);
		typeProvider.produceTypeProperties();				
		PPEInstanceType processInstanceType = getContext().getSchemaRegistry().getTypeByName(SpecificProcessInstanceType.getProcessName(processDef)); //) ProcessInstance.getOrCreateDesignSpaceInstanceType(instance.workspace, this);
		//DecisionNodeInstance.getOrCreateCoreType(instance.workspace);
		//List<ProcessException> subProcessExceptions = new ArrayList<>();
		processDef.getStepDefinitions().stream().forEach(stepDef -> {
			if (stepDef instanceof ProcessDefinition) {
				log.debug("Skipping creation of Datamapping Rule for Subprocess Step: "+stepDef.getName());
				errors.addAll(initializeInstanceTypes((ProcessDefinition)stepDef, doGeneratePrematureDetectionConstraints));
			} else {
				// create the specific step type
				SpecificProcessStepType stepTypeProvider = new SpecificProcessStepType(getContext().getSchemaRegistry(), stepDef, processInstanceType);
				stepTypeProvider.produceTypeProperties();	
				PPEInstanceType stepInstanceType = getContext().getSchemaRegistry().getTypeByName(SpecificProcessStepType.getProcessStepName(stepDef));
				stepDef.getInputToOutputMappingRules().entrySet().stream()
				.forEach(entry -> {
					if (entry.getValue() != null) {
						String name = getDataMappingId(entry, stepDef);
						String expression = completeDatamappingRule(entry.getKey(), entry.getValue());
						RuleDefinition crt = getContext().getFactoryIndex().getRuleDefinitionFactory().createInstance(stepInstanceType, name, expression);
						if (crt == null) {
							log.warn("Unknown reason for rule creation failure");
						} else {
							System.out.println("Created "+name);
						}
						// do we really need to create those properties? or is creating just the rules ok? probably not
						//type.createPropertyType(CRD_DATAMAPPING_PREFIX+entry.getKey(), Cardinality.SINGLE, crt);
					}
				});				 					
			}
		});
		errors.addAll(checkProcessStructure(processDef));
		getContext().getInstanceRepository().concludeTransaction();
		//				List<String> augmentationErrors = new LinkedList<>();
		errors.addAll(new RuleAugmentation(processDef, processInstanceType, getContext().getFactoryIndex().getRuleDefinitionFactory(), ruleService).augmentAndCreateConditions());
		processDef.getStepDefinitions().stream().forEach(stepDef -> {
			errors.addAll(new RuleAugmentation(stepDef, 
												getContext().getSchemaRegistry().getTypeByName(SpecificProcessStepType.getProcessStepName(stepDef)), 
												getContext().getFactoryIndex().getRuleDefinitionFactory(), 
												ruleService)
								.augmentAndCreateConditions());
		});
		errors.addAll(checkConstraintValidity(processDef, processInstanceType));
		
		errors.addAll(ruleService.checkOverriding(processDef, getContext()));
		
		if (errors.isEmpty() || errors.stream().allMatch(error -> !error.getSeverity().equals(ProcessDefinitionError.Severity.ERROR))) {
			// now lets also create premature rules here, as we need the process to exist first
	//		if (doGeneratePrematureDetectionConstraints) {
	//			new PrematureTriggerGenerator(ws, this).generatePrematureConstraints();
	//			context.getInstanceRepository().concludeTransaction();
	//		}
			// even if there are augementation errors, these were due to unsupported constructs in the constraint during augmentation, but the constraints are ok,
			// thus we store and run the process, but report back that augmentation didn;t work.
			//					if (!augmentationErrors.isEmpty()) {
			//						ProcessException pex = new ProcessException("Constraint Augmentation unsuccessful, but continuing nevertheless without augmentation.");
			//						pex.setErrorMessages(augmentationErrors);
			//						throw pex;
			//					}
			processDef.setIsWithoutBlockingErrors(true);
		} else {
			log.info("Blocking newly added process due to constraint errors: "+processDef.getName());
			processDef.setIsWithoutBlockingErrors(false);
		}
		getContext().getInstanceRepository().concludeTransaction(); // persisting the blocking errors flag
		return errors;
	}

	public List<ProcessDefinitionError> checkConstraintValidity(ProcessDefinition processDef, PPEInstanceType processInstanceType) {
		List<ProcessDefinitionError> overallStatus = new LinkedList<>();				
		//premature constraints:
		processDef.getPrematureTriggers().entrySet().stream()
		.forEach(entry -> {
			String ruleId = SpecificProcessInstanceType.generatePrematureRuleName(entry.getKey(), processDef);
			RuleDefinition crt = getContext().getSchemaRegistry().getRuleByNameAndContext(ruleId, processInstanceType); //.consistencyRuleTypeExists(ws,  ruleId, instType, entry.getValue());
			if (crt == null) {
				log.error("Expected Rule for existing process not found: "+ruleId);
				overallStatus.add(new ProcessDefinitionError(processDef, "Expected Premature Trigger Rule Not Found - Internal Data Corruption", ruleId, ProcessDefinitionError.Severity.ERROR));
			} else
				if (crt.hasRuleError())
					overallStatus.add(new ProcessDefinitionError(processDef, String.format("Premature Trigger Rule % has an error", ruleId), crt.getRuleError(), ProcessDefinitionError.Severity.ERROR));
		});
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

	private String completeDatamappingRule(String param, String rule) {
		return rule
				+"\r\n"
				+"->asSet()\r\n"
				+"->symmetricDifference(self.out_"+param+")\r\n"
				+"->size() = 0";
	}

	public static String getDataMappingId(Map.Entry<String,String> ioMapping, StepDefinition sd) {
		String procId = sd.getProcess() != null ? sd.getProcess().getName() : "";
		return CRD_DATAMAPPING_PREFIX+ioMapping.getKey()+"_"+sd.getName()+"_"+procId;
	}


	public static String getConstraintName(Conditions condition, PPEInstanceType stepType) {
		return getConstraintName(condition, 0, stepType);
	}

	public static String getConstraintName(Conditions condition, int specOrderIndex, PPEInstanceType stepType) {
		return CRD_PREFIX+condition+specOrderIndex+"_"+stepType.getName();
	}

	public static String getQASpecId(ConstraintSpec spec, ProcessDefinition processContext) {
		return CRD_QASPEC_PREFIX+spec.getConstraintId()+"_"+processContext.getName();
	}
}
