package at.jku.isse.passiveprocessengine.definition.factories;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import at.jku.isse.passiveprocessengine.Context;
import at.jku.isse.passiveprocessengine.analysis.PrematureTriggerGenerator;
import at.jku.isse.passiveprocessengine.core.Instance;
import at.jku.isse.passiveprocessengine.core.InstanceRepository;
import at.jku.isse.passiveprocessengine.core.InstanceType;
import at.jku.isse.passiveprocessengine.core.ProcessDomainTypesRegistry;
import at.jku.isse.passiveprocessengine.core.RuleDefinition;
import at.jku.isse.passiveprocessengine.definition.ProcessDefinitionError;
import at.jku.isse.passiveprocessengine.definition.activeobjects.ConstraintSpec;
import at.jku.isse.passiveprocessengine.definition.activeobjects.ProcessDefinition;
import at.jku.isse.passiveprocessengine.definition.activeobjects.StepDefinition;
import at.jku.isse.passiveprocessengine.definition.types.ProcessDefinitionType;
import at.jku.isse.passiveprocessengine.instance.StepLifecycle.Conditions;
import at.jku.isse.passiveprocessengine.instance.activeobjects.DecisionNodeInstance;
import at.jku.isse.passiveprocessengine.instance.activeobjects.ProcessInstance;
import at.jku.isse.passiveprocessengine.instance.activeobjects.ProcessStep;
import at.jku.isse.passiveprocessengine.instance.types.SpecificProcessInstanceType;
import at.jku.isse.passiveprocessengine.instance.types.SpecificProcessStepType;

public class ProcessDefinitionFactory {
	InstanceRepository repository;
	Context context;
	ProcessDomainTypesRegistry typesFactory;

	public static final String CRD_PREFIX = "crd_";
	public static final String CRD_DATAMAPPING_PREFIX = "crd_datamapping_";
	public static final String CRD_QASPEC_PREFIX = "crd_qaspec_";


	public ProcessDefinitionFactory(InstanceRepository repository, Context context, ProcessDomainTypesRegistry typesFactory) {
		this.repository = repository;
		this.context = context;
		this.typesFactory = typesFactory;
	}

	/**
	 * 
	 * @param processId
	 * @return Basic, empty process definition structure. Creation of rules and specific process instance types requires calling 'initializeInstanceTypes'
	 */
	public ProcessDefinition createInstance(String processId) {
		Instance instance = repository.createInstance(processId, typesFactory.getType(ProcessDefinition.class));
		instance.setSingleProperty(ProcessDefinitionType.CoreProperties.isWithoutBlockingErrors.toString(), false);
		return context.getWrappedInstance(ProcessDefinition.class, instance);				
	}

	public List<ProcessDefinitionError> initializeInstanceTypes(ProcessDefinition processDef, boolean doGeneratePrematureDetectionConstraints) {
		List<ProcessDefinitionError> errors = new LinkedList<>();
		SpecificProcessInstanceType typeProvider = new SpecificProcessInstanceType(context.getSchemaRegistry(), processDef);
		typeProvider.registerTypeInFactory(typesFactory);				
		InstanceType processInstanceType = typesFactory.getTypeByName(SpecificProcessInstanceType.getProcessName(processDef)); //) ProcessInstance.getOrCreateDesignSpaceInstanceType(instance.workspace, this);
		//DecisionNodeInstance.getOrCreateCoreType(instance.workspace);
		//List<ProcessException> subProcessExceptions = new ArrayList<>();
		processDef.getStepDefinitions().stream().forEach(stepDef -> {
			if (stepDef instanceof ProcessDefinition) {
				errors.addAll(initializeInstanceTypes((ProcessDefinition)stepDef, doGeneratePrematureDetectionConstraints));
			} else {
				// create the specific step type
				SpecificProcessStepType stepTypeProvider = new SpecificProcessStepType(context.getSchemaRegistry(), stepDef, processInstanceType);
				stepTypeProvider.registerTypeInFactory(typesFactory);	
				InstanceType stepInstanceType = typesFactory.getTypeByName(SpecificProcessStepType.getProcessStepName(stepDef));
				stepDef.getInputToOutputMappingRules().entrySet().stream()
				.forEach(entry -> {
					if (entry.getValue() != null) {
						RuleDefinition crt = context.getFactoryIndex().getRuleDefinitionFactory().createInstance(stepInstanceType, getDataMappingId(entry, stepDef), completeDatamappingRule(entry.getKey(), entry.getValue()));
						// do we really need to create those properties? or is creating just the rules ok? probably not
						//type.createPropertyType(CRD_DATAMAPPING_PREFIX+entry.getKey(), Cardinality.SINGLE, crt);
					}
				});				 					
			}
		});
		errors.addAll(checkProcessStructure(processDef));
		context.getInstanceRepository().concludeTransaction();
		//				List<String> augmentationErrors = new LinkedList<>();
		errors.addAll(new RuleAugmentation(processDef, processInstanceType, context.getFactoryIndex().getRuleDefinitionFactory()).augmentAndCreateConditions());
		processDef.getStepDefinitions().stream().forEach(stepDef -> {
			errors.addAll(new RuleAugmentation(stepDef, typesFactory.getTypeByName(SpecificProcessStepType.getProcessStepName(stepDef)), context.getFactoryIndex().getRuleDefinitionFactory()).augmentAndCreateConditions());
		});
		errors.addAll(checkConstraintValidity(processDef, processInstanceType));
		if (errors.isEmpty() ) {
			// now lets also create premature rules here, as we need the process to exist first
			if (doGeneratePrematureDetectionConstraints) {
				new PrematureTriggerGenerator(ws, this).generatePrematureConstraints();
				context.getInstanceRepository().concludeTransaction();
			}
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
		context.getInstanceRepository().concludeTransaction(); // persisting the blocking errors flag
		return errors;
	}

	public List<ProcessDefinitionError> checkConstraintValidity(ProcessDefinition processDef, InstanceType processInstanceType) {
		List<ProcessDefinitionError> overallStatus = new LinkedList<>();				
		//premature constraints:
		processDef.getPrematureTriggers().entrySet().stream()
		.forEach(entry -> {
			String ruleId = SpecificProcessInstanceType.generatePrematureRuleName(entry.getKey(), processDef);
			RuleDefinition crt = context.getSchemaRegistry().getRuleByNameAndContext(ruleId, processInstanceType); //.consistencyRuleTypeExists(ws,  ruleId, instType, entry.getValue());
			if (crt == null) {
				log.error("Expected Rule for existing process not found: "+ruleId);
				overallStatus.add(new ProcessDefinitionError(processDef, "Expected Premature Trigger Rule Not Found - Internal Data Corruption", ruleId));
			} else
				if (crt.hasRuleError())
					overallStatus.add(new ProcessDefinitionError(processDef, String.format("Premature Trigger Rule % has an error", ruleId), crt.getRuleError()));
		});
		processDef.getStepDefinitions().forEach(sd -> overallStatus.addAll( sd.checkConstraintValidity(processInstanceType)));
		return overallStatus;
	}

	public List<ProcessDefinitionError> checkProcessStructure(ProcessDefinition processDef) {
		List<ProcessDefinitionError> status = new LinkedList<>();
		if (processDef.getDecisionNodeDefinitions().stream().filter(dnd -> dnd.getInSteps().isEmpty()).count() > 1)
			status.add(new ProcessDefinitionError(processDef, "Invalid Process Structure", "More than one entry decision node found"));
		if (processDef.getDecisionNodeDefinitions().stream().filter(dnd -> dnd.getOutSteps().isEmpty()).count() > 1)
			status.add(new ProcessDefinitionError(processDef, "Invalid Process Structure", "More than one exit decision node found"));
		if (processDef.getExpectedInput().isEmpty()) {
			status.add(new ProcessDefinitionError(processDef, "No Input Defined", "Step needs at least one input."));
		}
		processDef.getExpectedInput().forEach((in, type) -> {
			if (type == null)
				status.add(new ProcessDefinitionError(processDef, "Unavailable Type", "Artifact type of input '"+in+"' could not be resolved"));
		});
		processDef.getExpectedOutput().forEach((out, type) -> {
			if (type == null)
				status.add(new ProcessDefinitionError(processDef, "Unavailable Type", "Artifact type of output '"+out+"' could not be resolved"));
		});

		processDef.getStepDefinitions().stream()
		.filter(sd -> !(sd instanceof ProcessDefinition))
		.forEach(sd -> status.addAll( sd.checkStepStructureValidity()));
		processDef.getDecisionNodeDefinitions().forEach(dnd -> status.addAll(dnd.checkDecisionNodeStructureValidity()));
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


	public static String getConstraintName(Conditions condition, InstanceType stepType) {
		return getConstraintName(condition, 0, stepType);
	}

	public static String getConstraintName(Conditions condition, int specOrderIndex, InstanceType stepType) {
		return CRD_PREFIX+condition+specOrderIndex+"_"+stepType.getName();
	}

	public static String getQASpecId(ConstraintSpec spec, ProcessDefinition context) {
		return CRD_QASPEC_PREFIX+spec.getConstraintId()+"_"+context.getName();
	}
}
