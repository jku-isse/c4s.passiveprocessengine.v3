package at.jku.isse.passiveprocessengine.instance;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import at.jku.isse.designspace.core.model.Cardinality;
import at.jku.isse.designspace.core.model.Instance;
import at.jku.isse.designspace.core.model.InstanceType;
import at.jku.isse.designspace.core.model.Property;
import at.jku.isse.designspace.core.model.SetProperty;
import at.jku.isse.designspace.core.model.Workspace;
import at.jku.isse.passiveprocessengine.WrapperCache;
import at.jku.isse.passiveprocessengine.definition.IStepDefinition;
import at.jku.isse.passiveprocessengine.definition.ProcessDefinition;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ProcessInstance extends ProcessStep {

	static enum CoreProperties {stepInstances, decisionNodeInstances};
	
	public static final String designspaceTypeId = ProcessInstance.class.getSimpleName();
	
	transient protected ProcessDefinition pd;
	
	public ProcessInstance(Instance instance) {
		super(instance);
	}

	@SuppressWarnings("unchecked")
	public void addInput(String inParam, Instance artifact) {
		if (pd.getExpectedInput().containsKey(inParam)) {
			Property<?> prop = instance.getProperty("in_"+inParam);
			if (prop.propertyType.isAssignable(artifact)) {
				instance.getPropertyAsList("in_"+inParam).add(artifact);
			} else {
				log.warn(String.format("Cannot add input %s to process instance %s with nonmatching artifact type %s of id % %s", inParam, this.getName(), artifact.getInstanceType().toString(), artifact.id(), artifact.name()));
			}
		} else {
			// additionally Somehow notify about wrong param access
			log.warn(String.format("Ignoring attempt to add unexpected input %s to process instance %s", inParam, this.getName()));
		}
	}
	
	
	@SuppressWarnings("unchecked")
	public Set<ProcessStep> getProcessSteps() {
		SetProperty<?> stepList = instance.getPropertyAsSet(CoreProperties.stepInstances.toString());
		if (stepList != null && stepList.get() != null) {
			return (Set<ProcessStep>) stepList.get().stream()
					.map(inst -> WrapperCache.getWrappedInstance(ProcessStep.class, (Instance) inst))
					.collect(Collectors.toSet());	
		} else return Collections.emptySet();
		
	}
	
	@SuppressWarnings("unchecked")
	public Set<DecisionNodeInstance> getDecisionNodeInstances() {
		SetProperty<?> stepList = instance.getPropertyAsSet(CoreProperties.decisionNodeInstances.toString());
		if (stepList != null && stepList.get() != null) {
			return (Set<DecisionNodeInstance>) stepList.get().stream()
					.map(inst -> WrapperCache.getWrappedInstance(DecisionNodeInstance.class, (Instance) inst))
					.collect(Collectors.toSet());	
		} else return Collections.emptySet();
	}
	
	public static InstanceType getOrCreateDesignSpaceInstanceType(Workspace ws, IStepDefinition td) {
		Optional<InstanceType> thisType = ws.debugInstanceTypes().stream()
				.filter(it -> it.name().equals(designspaceTypeId+td.getId()))
				.findAny();
		if (thisType.isPresent())
			return thisType.get();
		else {
			InstanceType typeStep = ws.createInstanceType(designspaceTypeId+td.getName(), ws.TYPES_FOLDER, ProcessStep.getOrCreateDesignSpaceInstanceType(ws, td));
			typeStep.createPropertyType(CoreProperties.stepInstances.toString(), Cardinality.LIST, getOrCreateDesignSpaceCoreSchema(ws));
			typeStep.createPropertyType(CoreProperties.decisionNodeInstances.toString(), Cardinality.LIST, DecisionNodeInstance.getOrCreateDesignSpaceCoreSchema(ws));
			return typeStep;
		}
	}
		
	public static ProcessStep getInstance(Workspace ws, IStepDefinition sd) {
		Instance instance = ws.createInstance(getOrCreateDesignSpaceInstanceType(ws, sd), sd.getName()+UUID.randomUUID());
		return WrapperCache.getWrappedInstance(ProcessInstance.class, instance);
	}
	
}
