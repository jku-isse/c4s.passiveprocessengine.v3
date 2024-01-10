package at.jku.isse.passiveprocessengine.definition.factories;

import java.util.LinkedList;
import java.util.List;

import at.jku.isse.passiveprocessengine.Context;
import at.jku.isse.passiveprocessengine.analysis.PrematureTriggerGenerator;
import at.jku.isse.passiveprocessengine.analysis.RuleAugmentation;
import at.jku.isse.passiveprocessengine.core.Instance;
import at.jku.isse.passiveprocessengine.core.InstanceRepository;
import at.jku.isse.passiveprocessengine.core.InstanceType;
import at.jku.isse.passiveprocessengine.core.RuleDefinition;
import at.jku.isse.passiveprocessengine.definition.ProcessDefinitionError;
import at.jku.isse.passiveprocessengine.definition.activeobjects.ProcessDefinition;
import at.jku.isse.passiveprocessengine.definition.types.ProcessDefinitionType;
import at.jku.isse.passiveprocessengine.definition.types.ProcessDomainTypesRegistry;
import at.jku.isse.passiveprocessengine.instance.activeobjects.DecisionNodeInstance;
import at.jku.isse.passiveprocessengine.instance.activeobjects.ProcessInstance;
import at.jku.isse.passiveprocessengine.instance.activeobjects.ProcessStep;

public class ProcessDefinitionFactory {
	InstanceRepository repository;
	Context wrapperCache;
	ProcessDomainTypesRegistry typesFactory;
	
	public ProcessDefinitionFactory(InstanceRepository repository, Context wrapperCache, ProcessDomainTypesRegistry typesFactory) {
		this.repository = repository;
		this.wrapperCache = wrapperCache;
		this.typesFactory = typesFactory;
	}
	
	public ProcessDefinition createInstance(String stepId) {
		Instance instance = repository.createInstance(stepId, typesFactory.getType(ProcessDefinition.class));
		instance.setSingleProperty(ProcessDefinitionType.CoreProperties.isWithoutBlockingErrors.toString(), false);
		return wrapperCache.getWrappedInstance(ProcessDefinition.class, instance);
		
		//TODO: not only create the definition,
		// but also initialize all instance type
		// and also ensure rule augmentation
	}
	
	//TODO: do we really need to create those properties? or is creating just the rules ok? 
	//probably not: TODO: move the rule creation for iomapping into Factory of StepDefinition
	//RuleDefinition crt =  RuleDefinition.create(ws, typeStep, getDataMappingId(entry, td), completeDatamappingRule(entry.getKey(), entry.getValue()));
	//type.createPropertyType(CRD_DATAMAPPING_PREFIX+entry.getKey(), Cardinality.SINGLE, crt);
	
	
	//TODO: move to instance factory
	public List<ProcessDefinitionError> initializeInstanceTypes(boolean doGeneratePrematureDetectionConstraints) {
		List<ProcessDefinitionError> errors = new LinkedList<>();
		InstanceType processInstanceType = ProcessInstance.getOrCreateDesignSpaceInstanceType(instance.workspace, this);
		DecisionNodeInstance.getOrCreateCoreType(instance.workspace);
		//List<ProcessException> subProcessExceptions = new ArrayList<>();
		this.getStepDefinitions().stream().forEach(sd -> {
			if (sd instanceof ProcessDefinition) {
					errors.addAll(((ProcessDefinition)sd).initializeInstanceTypes(doGeneratePrematureDetectionConstraints));
			} else
				ProcessStep.getOrCreateDesignSpaceInstanceType(instance.workspace, sd, processInstanceType);
		});
		errors.addAll(checkProcessStructure());
//		if (!subProcessExceptions.isEmpty())
//			return errors;
		ws.concludeTransaction();
//		List<String> augmentationErrors = new LinkedList<>();
		errors.addAll(new RuleAugmentation(ws, this, ProcessStep.getOrCreateDesignSpaceInstanceType(ws, this, processInstanceType)).augmentAndCreateConditions());
		this.getStepDefinitions().stream().forEach(sd -> {
				errors.addAll(new RuleAugmentation(ws, sd, ProcessStep.getOrCreateDesignSpaceInstanceType(ws, sd, processInstanceType)).augmentAndCreateConditions());
		});
		errors.addAll(checkConstraintValidity());
		if (errors.isEmpty() ) {
			// now lets also create premature rules here, as we need the process to exist first
			if (doGeneratePrematureDetectionConstraints) {
				new PrematureTriggerGenerator(ws, this).generatePrematureConstraints();
				ws.concludeTransaction();
			}
			// even if there are augementation errors, these were due to unsupported constructs in the constraint during augmentation, but the constraints are ok,
			// thus we store and run the process, but report back that augmentation didn;t work.
//			if (!augmentationErrors.isEmpty()) {
//				ProcessException pex = new ProcessException("Constraint Augmentation unsuccessful, but continuing nevertheless without augmentation.");
//				pex.setErrorMessages(augmentationErrors);
//				throw pex;
//			}
			this.setIsWithoutBlockingErrors(true);
		} else {
			log.info("Blocking newly added process due to constraint errors: "+getName());
//			ProcessException pex = new ProcessException("Constraints contain at least one error");
//			// here we dont care if there were augmentation errors, as these are most likely due to the constraint errors anyway.
//			validity.values().stream()
//				.flatMap(vmap -> vmap.entrySet().stream())
//				.filter(entry -> !entry.getValue().equals("valid"))
//				.forEach(entry -> pex.getErrorMessages().add(entry.getKey()+": "+entry.getValue()));
//			deleteCascading();
//			ws.concludeTransaction();
//			throw pex;
			this.setIsWithoutBlockingErrors(false);
		}
		ws.concludeTransaction(); // persisting the blocking errors flag
		return errors;
	}

	public List<ProcessDefinitionError> checkConstraintValidity() {
		List<ProcessDefinitionError> overallStatus = new LinkedList<>();
		InstanceType processInstType = ProcessInstance.getOrCreateDesignSpaceInstanceType(ws, this);
		//premature constraints:
		this.getPrematureTriggers().entrySet().stream()
			.forEach(entry -> {
				String ruleId = ProcessInstance.generatePrematureRuleName(entry.getKey(), this);
				RuleDefinition crt = context.getSchemaRegistry().getRuleByNameAndContext(ruleId, processInstType); //.consistencyRuleTypeExists(ws,  ruleId, instType, entry.getValue());
				if (crt == null) {
					log.error("Expected Rule for existing process not found: "+ruleId);
					overallStatus.add(new ProcessDefinitionError(this, "Expected Premature Trigger Rule Not Found - Internal Data Corruption", ruleId));
				} else
					if (crt.hasRuleError())
						overallStatus.add(new ProcessDefinitionError(this, String.format("Premature Trigger Rule % has an error", ruleId), crt.getRuleError()));
			});
		getStepDefinitions().forEach(sd -> overallStatus.addAll( sd.checkConstraintValidity(processInstType)));
		return overallStatus;
	}

	public List<ProcessDefinitionError> checkProcessStructure() {
		List<ProcessDefinitionError> status = new LinkedList<>();
		if (this.getDecisionNodeDefinitions().stream().filter(dnd -> dnd.getInSteps().isEmpty()).count() > 1)
			status.add(new ProcessDefinitionError(this, "Invalid Process Structure", "More than one entry decision node found"));
		if (this.getDecisionNodeDefinitions().stream().filter(dnd -> dnd.getOutSteps().isEmpty()).count() > 1)
			status.add(new ProcessDefinitionError(this, "Invalid Process Structure", "More than one exit decision node found"));
		if (getExpectedInput().isEmpty()) {
			status.add(new ProcessDefinitionError(this, "No Input Defined", "Step needs at least one input."));
		}
		getExpectedInput().forEach((in, type) -> {
			if (type == null)
				status.add(new ProcessDefinitionError(this, "Unavailable Type", "Artifact type of input '"+in+"' could not be resolved"));
		});
		getExpectedOutput().forEach((out, type) -> {
			if (type == null)
				status.add(new ProcessDefinitionError(this, "Unavailable Type", "Artifact type of output '"+out+"' could not be resolved"));
		});

		getStepDefinitions().stream()
			.filter(sd -> !(sd instanceof ProcessDefinition))
			.forEach(sd -> status.addAll( sd.checkStepStructureValidity()));
		getDecisionNodeDefinitions().forEach(dnd -> status.addAll(dnd.checkDecisionNodeStructureValidity()));
		return status;
	}
}
