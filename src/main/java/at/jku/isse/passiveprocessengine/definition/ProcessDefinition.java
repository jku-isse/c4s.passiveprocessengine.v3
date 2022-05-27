package at.jku.isse.passiveprocessengine.definition;

import java.util.Collections;
import java.util.HashMap;
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
import at.jku.isse.passiveprocessengine.WrapperCache;
import at.jku.isse.passiveprocessengine.analysis.PrematureTriggerGenerator;
import at.jku.isse.passiveprocessengine.analysis.RuleAugmentation;
import at.jku.isse.passiveprocessengine.definition.StepDefinition.CoreProperties;
import at.jku.isse.passiveprocessengine.instance.DecisionNodeInstance;
import at.jku.isse.passiveprocessengine.instance.ProcessException;
import at.jku.isse.passiveprocessengine.instance.ProcessInstance;
import at.jku.isse.passiveprocessengine.instance.ProcessStep;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ProcessDefinition extends StepDefinition{

	public static enum CoreProperties {decisionNodeDefinitions, stepDefinitions, prematureTriggers, prematureTriggerMappings}
	
	public static final String designspaceTypeId = ProcessDefinition.class.getSimpleName();
	
	public ProcessDefinition(Instance instance) {
		super(instance);
	}

	public List<StepDefinition> getStepDefinitions() {
		ListProperty<?> stepList = instance.getPropertyAsList(CoreProperties.stepDefinitions.toString());
		if (stepList != null && stepList.get() != null) {
			return stepList.get().stream()
					.map(inst -> WrapperCache.getWrappedInstance(StepDefinition.class, (Instance) inst))
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
	
	@Override
	public void deleteCascading() {
		getDecisionNodeDefinitions().forEach(dnd -> dnd.deleteCascading());
		getStepDefinitions().forEach(sd -> sd.deleteCascading());
		InstanceType thisType = ProcessInstance.getOrCreateDesignSpaceInstanceType(ws, this);
		this.getPrematureTriggers().entrySet().stream()
		.forEach(entry -> {
			String name = ProcessInstance.generatePrematureRuleName(entry.getKey(), this);
			ConsistencyRuleType crt = ConsistencyRuleType.consistencyRuleTypeExists(ws,  name, thisType, entry.getValue());
			if (crt != null) crt.delete();
		});
		super.deleteCascading();
	}
	
	public void initializeInstanceTypes() throws ProcessException{
		ProcessInstance.getOrCreateDesignSpaceInstanceType(instance.workspace, this);
		DecisionNodeInstance.getOrCreateDesignSpaceCoreSchema(instance.workspace);
		this.getStepDefinitions().stream().forEach(sd -> ProcessStep.getOrCreateDesignSpaceInstanceType(instance.workspace, sd));
		ws.concludeTransaction();
		List<String> augmentationErrors = new LinkedList<>();
		try {
			new RuleAugmentation(ws, this, ProcessStep.getOrCreateDesignSpaceInstanceType(ws, this)).augmentConditions(); 
		} catch(ProcessException pex) {
			augmentationErrors.addAll(pex.getErrorMessages());
		}
		this.getStepDefinitions().stream().forEach(sd -> {
			try {
				new RuleAugmentation(ws, sd, ProcessStep.getOrCreateDesignSpaceInstanceType(ws, sd)).augmentConditions(); 
			} catch(ProcessException pex) {
				augmentationErrors.addAll(pex.getErrorMessages());
			}
		});
		Map<String, Map<String, String>> validity = checkConstraintValidity();
		if (validity.values().stream().flatMap(vmap -> vmap.values().stream()).allMatch(val -> val.equals("valid")) ) {
			// now lets also create premature rules here, as we need the process to exist first
			//new PrematureTriggerGenerator(ws, this).generatePrematureConstraints();
			//ws.concludeTransaction();
			
			// even if there are augementation errors, these were due to unsupported constructs in the constraint during augmentation, but the constraints are ok,
			// thus we store and run the process, but report back that augmentation didn;t work.
			if (!augmentationErrors.isEmpty()) {
				ProcessException pex = new ProcessException("Constraint Augmentation unsuccessful, but continuing nevertheless without augmentation.");
				pex.setErrorMessages(augmentationErrors);
				throw pex;
			}
		} else {
			log.info("Removing newly added process again due to constraint errors: "+getName());
			ProcessException pex = new ProcessException("Constraints contain at least one error");
			// here we dont care if there were augmentation errors, as these are most likely due to the constraint errors anyway.
			validity.values().stream()
				.flatMap(vmap -> vmap.entrySet().stream())
				.filter(entry -> !entry.getValue().equals("valid"))
				.forEach(entry -> pex.getErrorMessages().add(entry.getKey()+": "+entry.getValue()));
			deleteCascading();
			ws.concludeTransaction();
			throw pex;
		}
	}
	
	public Map<String, Map<String, String>> checkConstraintValidity() {
		Map<String, Map<String, String>> overallStatus = new HashMap<>();
		overallStatus.put(this.getName(), ProcessInstance.getConstraintValidityStatus(ws, this));
		getStepDefinitions().forEach(sd -> overallStatus.put(sd.getName(), ProcessStep.getConstraintValidityStatus(ws, sd)));
		return overallStatus;
	}
	
	public static InstanceType getOrCreateDesignSpaceCoreSchema(Workspace ws) {
		Optional<InstanceType> thisType = ws.debugInstanceTypes().stream()
				.filter(it -> !it.isDeleted)
				.filter(it -> it.name().contentEquals(designspaceTypeId))
				.findAny();
			if (thisType.isPresent())
				return thisType.get();
			else {
				InstanceType typeStep = ws.createInstanceType(designspaceTypeId, ws.TYPES_FOLDER, StepDefinition.getOrCreateDesignSpaceCoreSchema(ws));
				typeStep.createPropertyType(CoreProperties.stepDefinitions.toString(), Cardinality.LIST, StepDefinition.getOrCreateDesignSpaceCoreSchema(ws));
				typeStep.createPropertyType(CoreProperties.decisionNodeDefinitions.toString(), Cardinality.SET, DecisionNodeDefinition.getOrCreateDesignSpaceCoreSchema(ws));
				typeStep.createPropertyType(CoreProperties.prematureTriggers.toString(), Cardinality.MAP, Workspace.STRING);
				typeStep.createPropertyType(CoreProperties.prematureTriggerMappings.toString(), Cardinality.MAP, Workspace.STRING);
				return typeStep;
			}
	}

	public static ProcessDefinition getInstance(String stepId, Workspace ws) {
		Instance instance = ws.createInstance(getOrCreateDesignSpaceCoreSchema(ws), stepId);
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
}
