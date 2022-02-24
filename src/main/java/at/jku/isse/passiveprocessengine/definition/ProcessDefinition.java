package at.jku.isse.passiveprocessengine.definition;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import at.jku.isse.designspace.core.model.Cardinality;
import at.jku.isse.designspace.core.model.Instance;
import at.jku.isse.designspace.core.model.InstanceType;
import at.jku.isse.designspace.core.model.ListProperty;
import at.jku.isse.designspace.core.model.SetProperty;
import at.jku.isse.designspace.core.model.Workspace;
import at.jku.isse.passiveprocessengine.WrapperCache;

public class ProcessDefinition extends StepDefinition{

	public static enum CoreProperties {decisionNodeDefinitions, stepDefinitions}
	
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
	
	public static InstanceType getOrCreateDesignSpaceCoreSchema(Workspace ws) {
		Optional<InstanceType> thisType = ws.debugInstanceTypes().stream()
				.filter(it -> it.name().contentEquals(designspaceTypeId))
				.findAny();
			if (thisType.isPresent())
				return thisType.get();
			else {
				InstanceType typeStep = ws.createInstanceType(designspaceTypeId, ws.TYPES_FOLDER, StepDefinition.getOrCreateDesignSpaceCoreSchema(ws));
				typeStep.createPropertyType(CoreProperties.stepDefinitions.toString(), Cardinality.LIST, StepDefinition.getOrCreateDesignSpaceCoreSchema(ws));
				typeStep.createPropertyType(CoreProperties.decisionNodeDefinitions.toString(), Cardinality.SET, DecisionNodeDefinition.getOrCreateDesignSpaceCoreSchema(ws));
				return typeStep;
			}
	}

	public static ProcessDefinition getInstance(String stepId, Workspace ws) {
		Instance instance = ws.createInstance(getOrCreateDesignSpaceCoreSchema(ws), stepId);
		return WrapperCache.getWrappedInstance(ProcessDefinition.class, instance);
	}
	
}
