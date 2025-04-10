package at.jku.isse.passiveprocessengine.definition.activeobjects;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import at.jku.isse.designspace.rule.arl.evaluator.RuleDefinition;
import at.jku.isse.passiveprocessengine.core.InstanceWrapper;
import at.jku.isse.passiveprocessengine.rdfwrapper.RDFInstance;
import at.jku.isse.passiveprocessengine.rdfwrapper.RDFInstanceType;
import at.jku.isse.passiveprocessengine.core.ProcessContext;
import at.jku.isse.passiveprocessengine.definition.factories.ProcessDefinitionFactory;
import at.jku.isse.passiveprocessengine.definition.types.ProcessDefinitionType;
import at.jku.isse.passiveprocessengine.instance.StepLifecycle.Conditions;
import at.jku.isse.passiveprocessengine.instance.types.ProcessConfigBaseElementType;
import at.jku.isse.passiveprocessengine.instance.types.SpecificProcessInstanceType;
import at.jku.isse.passiveprocessengine.instance.types.SpecificProcessStepType;

public class ProcessDefinition extends StepDefinition{

	public ProcessDefinition(RDFInstance instance, ProcessContext context) {
		super(instance, context);
	}

	public List<StepDefinition> getStepDefinitions() {
		List<?> stepList = instance.getTypedProperty(ProcessDefinitionType.CoreProperties.stepDefinitions.toString(), List.class);
		if (stepList != null) {
			return stepList.stream()
					.map(inst -> context.getWrappedInstance(getMostSpecializedClass((RDFInstance)inst), (RDFInstance) inst))
					.filter(StepDefinition.class::isInstance)
					.map(StepDefinition.class::cast)
					.collect(Collectors.toList());
		} else return Collections.emptyList();
	}

	@SuppressWarnings("unchecked")
	public void addStepDefinition(StepDefinition step) {
		instance.getTypedProperty(ProcessDefinitionType.CoreProperties.stepDefinitions.toString(), List.class).add(step.getInstance());
	}

	@SuppressWarnings("unchecked")
	public Set<DecisionNodeDefinition> getDecisionNodeDefinitions() {
		Set<?> dnSet = instance.getTypedProperty(ProcessDefinitionType.CoreProperties.decisionNodeDefinitions.toString(), Set.class);
		if (dnSet != null) {
			return dnSet.stream()
					.map(inst -> context.getWrappedInstance(DecisionNodeDefinition.class, (RDFInstance) inst))
					.map(obj ->(DecisionNodeDefinition)obj)
					.collect(Collectors.toSet());
		} else return Collections.emptySet();
	}

	@SuppressWarnings("unchecked")
	public void addDecisionNodeDefinition(DecisionNodeDefinition dnd) {
		instance.getTypedProperty(ProcessDefinitionType.CoreProperties.decisionNodeDefinitions.toString(), Set.class).add(dnd.getInstance());
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
		instance.getTypedProperty(ProcessDefinitionType.CoreProperties.prematureTriggers.toString(), Map.class).put(stepName, trigger);
	}

	@SuppressWarnings("unchecked")
	public Map<String, String> getPrematureTriggers() {
		Map<?,?> triggers = instance.getTypedProperty(ProcessDefinitionType.CoreProperties.prematureTriggers.toString(), Map.class);
		if (triggers != null) {
			return (Map<String,String>)triggers;
		} else return Collections.emptyMap();
	}

	public StepDefinition getStepDefinitionForPrematureConstraint(String constraintName) {
		Map<?,?> triggers = instance.getTypedProperty(ProcessDefinitionType.CoreProperties.prematureTriggerMappings.toString(), Map.class);
		if (triggers != null) {
			@SuppressWarnings("unchecked")
			String stepDefName =  (( Map<String, String>) triggers).get(constraintName);
			return getStepDefinitionByName(stepDefName);
		} else return null;
	}

	@SuppressWarnings("unchecked")
	public void setPrematureConstraintNameStepDefinition(String constraintName, String stepDefinitionName) {
		instance.getTypedProperty(ProcessDefinitionType.CoreProperties.prematureTriggerMappings.toString(), Map.class).put(constraintName, stepDefinitionName);
	}

	@Override
	public void setDepthIndexRecursive(int indexToSet) {
		super.setDepthIndexRecursive(indexToSet);
		// make sure we also update the child process steps
		// find first DNI
		DecisionNodeDefinition startDND = this.getDecisionNodeDefinitions().stream().filter(dnd -> dnd.getInSteps().isEmpty()).findFirst().get();
		startDND.setDepthIndexRecursive(indexToSet+1);
	}

	@Override
	public void deleteCascading() {
		getDecisionNodeDefinitions().forEach(dnd -> dnd.deleteCascading());
		getStepDefinitions().forEach(sd -> sd.deleteCascading());
		
		// delete configtype
		this.getExpectedInput().entrySet().stream()
		.filter(entry -> entry.getValue().isOfTypeOrAnySubtype(context.getSchemaRegistry().getTypeByName(ProcessConfigBaseElementType.typeId)))
		.forEach(configEntry -> {
			RDFInstanceType procConfig = configEntry.getValue().getInstanceType(); // context.getConfigFactory().getOrCreateProcessSpecificSubtype(configEntry.getKey(), this);
			procConfig.delete();
		});
		// wring instanceType: we need to get the dynamically generate Instance (the one that is used for the ProcessInstance)
		String processDefName = SpecificProcessInstanceType.getProcessName(this);
		RDFInstanceType thisType = this.context.getSchemaRegistry().getTypeByName(processDefName);
		if (thisType != null) {
			this.getPrematureTriggers().entrySet().stream()
			.forEach(entry -> {
				String name = SpecificProcessInstanceType.generatePrematureRuleName(entry.getKey(), this);
				RuleDefinition crt = context.getSchemaRegistry().getRuleByNameAndContext(name, thisType);//RuleDefinition.consistencyRuleTypeExists(ws,  name, thisType, entry.getValue());
				if (crt != null) 
					crt.delete();
			});			
			thisType.delete();
		}
		// some code duplication with StepDefiniton.deleteCascading() due to awkward naming, needs major engine overhaul
		String overrideName = SpecificProcessInstanceType.getProcessName(this);
		String stepDefName = SpecificProcessStepType.getProcessStepName(this);
		RDFInstanceType instType = this.context.getSchemaRegistry().getTypeByName(stepDefName);
		if (instType != null) {	
			this.getActivationconditions().stream().forEach(spec -> { 
				deleteRuleIfExists(instType, spec, Conditions.ACTIVATION, overrideName); //delete the rule 
			});
			this.getCancelconditions().stream().forEach(spec -> { 
				deleteRuleIfExists(instType, spec, Conditions.CANCELATION, overrideName); //delete the rule 
			});
			this.getPostconditions().stream().forEach(spec -> { 
				deleteRuleIfExists(instType, spec, Conditions.POSTCONDITION, overrideName); //delete the rule 
			});
			this.getPreconditions().stream().forEach(spec -> { 
				deleteRuleIfExists(instType, spec, Conditions.PRECONDITION, overrideName); //delete the rule 
			});
		}	
		super.deleteCascading();
	}
	
	protected void deleteRuleIfExists(RDFInstanceType instType, ConstraintSpec spec, Conditions condition, String overrideName ) {
		String name = ProcessDefinitionFactory.CRD_PREFIX+condition+spec.getOrderIndex()+"_"+overrideName;
		RuleDefinition crt = context.getSchemaRegistry().getRuleByNameAndContext(name, instType);
		if (crt != null) 
			crt.delete();
	}

	
	

	protected static Class<? extends InstanceWrapper> getMostSpecializedClass(RDFInstance inst) {
		// we have the problem, that the WrapperCache will only return a type we ask for (which might be a general type) rather than the most specialized one, hence we need to obtain that type here
		// we assume that this is used only in here within, and thus that inst is only ProcessDefinition or StepDefinition
		if (inst.getInstanceType().getId().startsWith(ProcessDefinitionType.typeId)) // its a process
			return ProcessDefinition.class;
		else
			return StepDefinition.class; // for now only those two types
	}



	public StepDefinition createAndAddStepDefinition(String stepId) {
		StepDefinition sd = getProcessContext().getFactoryIndex().getStepDefinitionFactory().createInstance(stepId);
				//StepDefinition.getInstance(stepId, ws); // any other initialization there
		sd.setProcess(this);
		this.addStepDefinition(sd);
		return sd;
	}

	public DecisionNodeDefinition createDecisionNodeDefinition(String dndId) {
		DecisionNodeDefinition dnd =  getProcessContext().getFactoryIndex().getDecisionNodeDefinitionFactory().createInstance(dndId); // any other initialization there
		dnd.setProcess(this);
		this.addDecisionNodeDefinition(dnd);
		return dnd;
	}

	public boolean isImmediateInstantiateAllStepsEnabled() {
		return instance.getTypedProperty(ProcessDefinitionType.CoreProperties.isImmediateInstantiateAllSteps.toString(), Boolean.class, false);		
	}

	public void isImmediateInstantiateAllStepsEnabled(boolean isImmediateInstantiateAllStepsEnabled) {
		instance.setSingleProperty(ProcessDefinitionType.CoreProperties.isImmediateInstantiateAllSteps.toString(), isImmediateInstantiateAllStepsEnabled);
	}

	public boolean isImmediateDataPropagationEnabled() {
		return instance.getTypedProperty(ProcessDefinitionType.CoreProperties.isImmediateDataPropagationEnabled.toString(), Boolean.class, false);		
	}

	public void setImmediateDataPropagationEnabled(boolean isImmediateDataPropagationEnabled) {
		instance.setSingleProperty(ProcessDefinitionType.CoreProperties.isImmediateDataPropagationEnabled.toString(), isImmediateDataPropagationEnabled);
	}

	public void setIsWithoutBlockingErrors(boolean isWithoutBlockingErrors) {
		instance.setSingleProperty(ProcessDefinitionType.CoreProperties.isWithoutBlockingErrors.toString(), isWithoutBlockingErrors);
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
