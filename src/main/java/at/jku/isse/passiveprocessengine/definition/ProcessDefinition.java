package at.jku.isse.passiveprocessengine.definition;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import at.jku.isse.designspace.core.model.Cardinality;
import at.jku.isse.designspace.core.model.Instance;
import at.jku.isse.designspace.core.model.InstanceType;
import at.jku.isse.designspace.core.model.ListProperty;
import at.jku.isse.designspace.core.model.MapProperty;
import at.jku.isse.designspace.core.model.SetProperty;
import at.jku.isse.designspace.core.model.Workspace;
import at.jku.isse.designspace.rule.model.ConsistencyRuleType;
import at.jku.isse.passiveprocessengine.InstanceWrapper;
import at.jku.isse.passiveprocessengine.WrapperCache;
import at.jku.isse.passiveprocessengine.analysis.PrematureTriggerGenerator;
import at.jku.isse.passiveprocessengine.analysis.RuleAugmentation;
import at.jku.isse.passiveprocessengine.configurability.ProcessConfigBaseElementFactory;
import at.jku.isse.passiveprocessengine.instance.DecisionNodeInstance;
import at.jku.isse.passiveprocessengine.instance.ProcessInstance;
import at.jku.isse.passiveprocessengine.instance.ProcessStep;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ProcessDefinition extends StepDefinition{

	public static enum CoreProperties {decisionNodeDefinitions, stepDefinitions, prematureTriggers, prematureTriggerMappings, 
		isImmediateDataPropagationEnabled,
		isImmediateInstantiateAllSteps, isWithoutBlockingErrors}
	
	public static final String designspaceTypeId = ProcessDefinition.class.getSimpleName();
	
	public ProcessDefinition(Instance instance) {
		super(instance);
	}

	public List<StepDefinition> getStepDefinitions() {
		ListProperty<?> stepList = instance.getPropertyAsList(CoreProperties.stepDefinitions.toString());
		if (stepList != null && stepList.get() != null) {
			return stepList.get().stream()
					.map(inst -> WrapperCache.getWrappedInstance(getMostSpecializedClass((Instance)inst), (Instance) inst))
					.filter(StepDefinition.class::isInstance) 
					.map(StepDefinition.class::cast)
					.collect(Collectors.toList());	
		} else return Collections.emptyList();
	}
	
	@SuppressWarnings("unchecked")
	public void addStepDefinition(StepDefinition step) {
		instance.getPropertyAsList(CoreProperties.stepDefinitions.toString()).add(step.getInstance());
	}
	
	@SuppressWarnings("unchecked")
	public Set<DecisionNodeDefinition> getDecisionNodeDefinitions() {
		SetProperty<?> dnSet = instance.getPropertyAsSet(CoreProperties.decisionNodeDefinitions.toString());
		if (dnSet != null && dnSet.get() != null) {
			return (Set<DecisionNodeDefinition>) dnSet.get().stream()
					.map(inst -> WrapperCache.getWrappedInstance(DecisionNodeDefinition.class, (Instance) inst))
					.collect(Collectors.toSet());	
		} else return Collections.emptySet();
	}
	
	@SuppressWarnings("unchecked")
	public void addDecisionNodeDefinition(DecisionNodeDefinition dnd) {
		instance.getPropertyAsSet(CoreProperties.decisionNodeDefinitions.toString()).add(dnd.getInstance());
	}
	
	public DecisionNodeDefinition getDecisionNodeDefinitionByName(String name) {
		return getDecisionNodeDefinitions().stream()
		.filter(dnd -> dnd.getName().equals(name))
		.findAny().orElse(null);
	}
	
	public StepDefinition getStepDefinitionByName(String name) {
		return getStepDefinitions().stream()
				.filter(step -> step.getName().equals(name))
				.findAny().orElse(null);
	}
	
	@SuppressWarnings("unchecked")
	public void addPrematureTrigger(String stepName, String trigger) {
		instance.getPropertyAsMap(CoreProperties.prematureTriggers.toString()).put(stepName, trigger);
	}
	
	@SuppressWarnings("unchecked")
	public Map<String, String> getPrematureTriggers() {
		MapProperty<?> triggers = instance.getPropertyAsMap(CoreProperties.prematureTriggers.toString());
		if (triggers != null && triggers.get() != null) {
			return ( Map<String, String>) triggers.get();
		} else return Collections.emptyMap();
	}
	
	public StepDefinition getStepDefinitionForPrematureConstraint(String constraintName) {
		MapProperty<?> triggers = instance.getPropertyAsMap(CoreProperties.prematureTriggerMappings.toString());
		if (triggers != null && triggers.get() != null) {
			String stepDefName =  (( Map<String, String>) triggers.get()).get(constraintName);
			return getStepDefinitionByName(stepDefName);
		} else return null;
	}
	
	@SuppressWarnings("unchecked")
	public void setPrematureConstraintNameStepDefinition(String constraintName, String stepDefinitionName) {
		instance.getPropertyAsMap(CoreProperties.prematureTriggerMappings.toString()).put(constraintName, stepDefinitionName);
	}
	
	public void setDepthIndexRecursive(int indexToSet) {
		super.setDepthIndexRecursive(indexToSet);
		// make sure we also update the child process steps		
		// find first DNI
		DecisionNodeDefinition startDND = this.getDecisionNodeDefinitions().stream().filter(dnd -> dnd.getInSteps().isEmpty()).findFirst().get();
		startDND.setDepthIndexRecursive(indexToSet+1);
	}
	
	@Override
	public void deleteCascading(ProcessConfigBaseElementFactory configFactory) {
		getDecisionNodeDefinitions().forEach(dnd -> dnd.deleteCascading(configFactory));
		getStepDefinitions().forEach(sd -> sd.deleteCascading(configFactory));
		InstanceType thisType = ProcessInstance.getOrCreateDesignSpaceInstanceType(ws, this);
		this.getPrematureTriggers().entrySet().stream()
		.forEach(entry -> {
			String name = ProcessInstance.generatePrematureRuleName(entry.getKey(), this);
			ConsistencyRuleType crt = getRuleByNameAndContext(name, thisType);//ConsistencyRuleType.consistencyRuleTypeExists(ws,  name, thisType, entry.getValue());
			if (crt != null) crt.delete();
		});
		// delete configtype
		this.getExpectedInput().entrySet().stream()
		.filter(entry -> entry.getValue().isKindOf(configFactory.getBaseType()))
		.forEach(configEntry -> {
			InstanceType procConfig = configFactory.getOrCreateProcessSpecificSubtype(configEntry.getKey(), this);
			procConfig.delete();
		});
		super.deleteCascading(configFactory);
		thisType.delete();
	}
	
	public List<ProcessDefinitionError> initializeInstanceTypes(boolean doGeneratePrematureDetectionConstraints) {
		List<ProcessDefinitionError> errors = new LinkedList<>();
		InstanceType processInstanceType = ProcessInstance.getOrCreateDesignSpaceInstanceType(instance.workspace, this);
		DecisionNodeInstance.getOrCreateDesignSpaceCoreSchema(instance.workspace);
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
		errors.addAll(new RuleAugmentation(ws, this, processInstanceType).augmentAndCreateConditions()); 
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
				ConsistencyRuleType crt = getRuleByNameAndContext(ruleId, processInstType); //.consistencyRuleTypeExists(ws,  ruleId, instType, entry.getValue());
				if (crt == null) {
					log.error("Expected Rule for existing process not found: "+ruleId);
					overallStatus.add(new ProcessDefinitionError(this, "Expected Premature Trigger Rule Not Found - Internal Data Corruption", ruleId));
				} else
					if (crt.hasRuleError())
						overallStatus.add(new ProcessDefinitionError(this, String.format("Premature Trigger Rule % has an error", ruleId), crt.ruleError()));
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
	
	public static InstanceType getOrCreateDesignSpaceCoreSchema(Workspace ws) {
		Optional<InstanceType> thisType = Optional.ofNullable(ws.TYPES_FOLDER.instanceTypeWithName(designspaceTypeId)); 
//		Optional<InstanceType> thisType = ws.debugInstanceTypes().stream()
//				.filter(it -> !it.isDeleted)
//				.filter(it -> it.name().contentEquals(designspaceTypeId))
//				.findAny();
			if (thisType.isPresent())
				return thisType.get();
			else {
				InstanceType typeStep = ws.createInstanceType(designspaceTypeId, ws.TYPES_FOLDER, StepDefinition.getOrCreateDesignSpaceCoreSchema(ws));
				typeStep.createPropertyType(CoreProperties.stepDefinitions.toString(), Cardinality.LIST, StepDefinition.getOrCreateDesignSpaceCoreSchema(ws));
				typeStep.createPropertyType(CoreProperties.decisionNodeDefinitions.toString(), Cardinality.SET, DecisionNodeDefinition.getOrCreateDesignSpaceCoreSchema(ws));
				typeStep.createPropertyType(CoreProperties.prematureTriggers.toString(), Cardinality.MAP, Workspace.STRING);
				typeStep.createPropertyType(CoreProperties.prematureTriggerMappings.toString(), Cardinality.MAP, Workspace.STRING);
				typeStep.createPropertyType(CoreProperties.isImmediateDataPropagationEnabled.toString(), Cardinality.SINGLE, Workspace.BOOLEAN);
				typeStep.createPropertyType(CoreProperties.isImmediateInstantiateAllSteps.toString(), Cardinality.SINGLE, Workspace.BOOLEAN);
				typeStep.createPropertyType(CoreProperties.isWithoutBlockingErrors.toString(), Cardinality.SINGLE, Workspace.BOOLEAN);
				return typeStep;
			}
	}
	
	protected static Class<? extends InstanceWrapper> getMostSpecializedClass(Instance inst) {
		// we have the problem, that the WrapperCache will only return a type we ask for (which might be a general type) rather than the most specialized one, hence we need to obtain that type here
		// we assume that this is used only in here within, and thus that inst is only ProcessDefinition or StepDefinition
		if (inst.getInstanceType().name().startsWith(designspaceTypeId.toString())) // its a process
			return ProcessDefinition.class;
		else 
			return StepDefinition.class; // for now only those two types
	}

	public static ProcessDefinition getInstance(String stepId, Workspace ws) {
		Instance instance = ws.createInstance(getOrCreateDesignSpaceCoreSchema(ws), stepId);
		instance.getPropertyAsSingle(CoreProperties.isWithoutBlockingErrors.toString()).set(false);
		return WrapperCache.getWrappedInstance(ProcessDefinition.class, instance);
	}
	
	public StepDefinition createStepDefinition(String stepId, Workspace ws) {
		StepDefinition sd =  StepDefinition.getInstance(stepId, ws); // any other initialization there
		sd.setProcess(this);
		this.addStepDefinition(sd);
		return sd;
	}
	
	public DecisionNodeDefinition createDecisionNodeDefinition(String dndId, Workspace ws) {
		DecisionNodeDefinition dnd = DecisionNodeDefinition.getInstance(dndId, ws); // any other initialization there
		dnd.setProcess(this);
		this.addDecisionNodeDefinition(dnd);
		return dnd;
	}

	public boolean isImmediateInstantiateAllStepsEnabled() {
		Object value = instance.getPropertyAsValueOrElse(CoreProperties.isImmediateInstantiateAllSteps.toString(), () -> false);
		if (value == null) return false;
		else return (boolean) value; 
	}

	public void setImmediateInstantiateAllStepsEnabled(boolean isImmediateInstantiateAllStepsEnabled) {
		instance.getPropertyAsSingle(CoreProperties.isImmediateInstantiateAllSteps.toString()).set(isImmediateInstantiateAllStepsEnabled);
	}
	
	public boolean isImmediateDataPropagationEnabled() {
		Object value = instance.getPropertyAsValueOrElse(CoreProperties.isImmediateDataPropagationEnabled.toString(), () -> false);
		if (value == null) return false;
		else return (boolean) value; 
	}

	public void setImmediateDataPropagationEnabled(boolean isImmediateDataPropagationEnabled) {
		instance.getPropertyAsSingle(CoreProperties.isImmediateDataPropagationEnabled.toString()).set(isImmediateDataPropagationEnabled);
	}
	
	public void setIsWithoutBlockingErrors(boolean isWithoutBlockingErrors) {
		instance.getPropertyAsSingle(CoreProperties.isWithoutBlockingErrors.toString()).set(isWithoutBlockingErrors);
	}

	@Override
	public void setProcOrderIndex(int index) {
		super.setProcOrderIndex(index);
		setElementOrder(); // continue within this (sub)process
	}
	
	public void setElementOrder() {
		int offset = this.getSpecOrderIndex();
		// 		init dnd index		
		this.getDecisionNodeDefinitions().stream().forEach(dnd -> dnd.setProcOrderIndex(this.getSpecOrderIndex()));
		// determine order index
		this.getStepDefinitions().stream()
			.sorted(new Comparator<StepDefinition>() {
				@Override
				public int compare(StepDefinition o1, StepDefinition o2) {
					return Integer.compare(o1.getSpecOrderIndex(), o2.getSpecOrderIndex());
				}})
			.forEach(step -> {
			step.setProcOrderIndex(step.getSpecOrderIndex()+offset);
			step.getOutDND().setProcOrderIndex(step.getSpecOrderIndex()+offset); // every dnd has as order index the largest spec order index of its inSteps
		});
	}


}
