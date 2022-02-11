package at.jku.isse.passiveprocessengine.definition;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import at.jku.isse.designspace.sdk.core.model.Cardinality;
import at.jku.isse.designspace.sdk.core.model.Instance;
import at.jku.isse.designspace.sdk.core.model.InstanceType;
import at.jku.isse.designspace.sdk.core.model.MapProperty;
import at.jku.isse.designspace.sdk.core.model.SetProperty;
import at.jku.isse.designspace.sdk.core.model.Workspace;
import at.jku.isse.passiveprocessengine.InstanceWrapper;
import at.jku.isse.passiveprocessengine.WrapperCache;
import at.jku.isse.passiveprocessengine.instance.ProcessStep;

public class StepDefinition implements IStepDefinition, InstanceWrapper {

	public static enum CoreProperties {expectedInput, expectedOutput, ioMappingRules, 
							preCondition, postCondition, activationCondition, cancelationCondition,
							qaConstraints};
	
	public static final String designspaceTypeId = StepDefinition.class.getSimpleName();
	
	private transient Instance instance;
	
	public StepDefinition(Instance instance) {
		this.instance = instance;
	}
	
	@Override
	public String getId() {
		return instance.name();
	}

	@SuppressWarnings("unchecked")
	@Override
	public Map<String, InstanceType> getExpectedInput() {
		MapProperty<?> inMap = instance.propertyAsMap(CoreProperties.expectedInput.toString());
		if (inMap != null && inMap.get() != null) {
			return ( Map<String, InstanceType>) inMap.get();
		} else return Collections.emptyMap();
	}

	@SuppressWarnings("unchecked")
	public void addExpectedInput(String paramName, InstanceType type) {
		instance.propertyAsMap(CoreProperties.expectedInput.toString()).put(paramName, type);
	}
	
	@Override
	public Map<String, InstanceType> getExpectedOutput() {
		// TODO Auto-generated method stub
		return Collections.emptyMap();
	}

	@Override
	public Map<String, String> getInputToOutputMappingRules() {
		// TODO Auto-generated method stub
		return Collections.emptyMap();
	}

	@Override
	public Optional<String> getPreconditionRule() {
		// TODO Auto-generated method stub
		return Optional.empty();
	}

	@Override
	public Optional<String> getPostconditionRule() {
		// TODO Auto-generated method stub
		return Optional.empty();
	}

	@Override
	public Optional<String> getActivationRule() {
		// TODO Auto-generated method stub
		return Optional.empty();
	}

	@Override
	public Optional<String> getCancelationRule() {
		// TODO Auto-generated method stub
		return Optional.empty();
	}

	@SuppressWarnings("unchecked")
	@Override
	public Set<QAConstraintSpec> getQAConstraints() {
		SetProperty<?> qaSet = instance.propertyAsSet(CoreProperties.qaConstraints.toString());
		if (qaSet != null && qaSet.get() != null) {
			return (Set<QAConstraintSpec>) qaSet.get().stream()
					.map(inst -> WrapperCache.getWrappedInstance(QAConstraintSpec.class, (Instance) inst))
					.collect(Collectors.toSet());
		} else return Collections.emptySet();
	}
	
	@SuppressWarnings("unchecked")
	public void addQAConstraint(QAConstraintSpec spec) {
		instance.propertyAsSet(CoreProperties.qaConstraints.toString()).add(spec.getInstance());
	}
	
	@Override
	public DecisionNodeDefinition getOutDND() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DecisionNodeDefinition getInDND() {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public Instance getInstance() {
		return instance;
	}

	public static InstanceType getOrCreateDesignSpaceCoreSchema(Workspace ws) {
		Optional<InstanceType> thisType = ws.debugInstanceTypes().stream()
				.filter(it -> it.name().contentEquals(designspaceTypeId))
				.findAny();
			if (thisType.isPresent())
				return thisType.get();
			else {
				InstanceType typeStep = ws.createInstanceType(designspaceTypeId);
				typeStep.createPropertyType(CoreProperties.qaConstraints.toString(), Cardinality.SET, QAConstraintSpec.getOrCreateDesignSpaceCoreSchema(ws));
				typeStep.createPropertyType(CoreProperties.expectedInput.toString(), Cardinality.MAP, ws.META_INSTANCE_TYPE);
				return typeStep;
			}
	}

	public static StepDefinition getInstance(String stepId, Workspace ws) {
		Instance instance = ws.createInstance(stepId, getOrCreateDesignSpaceCoreSchema(ws));
		return WrapperCache.getWrappedInstance(StepDefinition.class, instance);
	}

}
