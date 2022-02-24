package at.jku.isse.passiveprocessengine.definition;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import at.jku.isse.designspace.core.model.Cardinality;
import at.jku.isse.designspace.core.model.Instance;
import at.jku.isse.designspace.core.model.InstanceType;
import at.jku.isse.designspace.core.model.MapProperty;
import at.jku.isse.designspace.core.model.SetProperty;
import at.jku.isse.designspace.core.model.Workspace;
import at.jku.isse.passiveprocessengine.DecisionNodeDefinition;
import at.jku.isse.passiveprocessengine.InstanceWrapper;
import at.jku.isse.passiveprocessengine.WrapperCache;
import at.jku.isse.passiveprocessengine.instance.StepLifecycle.Conditions;

public class StepDefinition extends InstanceWrapper implements IStepDefinition {

	public static enum CoreProperties {expectedInput, expectedOutput, ioMappingRules, 
							conditions,
							qaConstraints};
	
	public static final String designspaceTypeId = StepDefinition.class.getSimpleName();
	
	
	public StepDefinition(Instance instance) {
		super(instance);
	}

	@SuppressWarnings("unchecked")
	public Map<String, InstanceType> getExpectedInput() {
		MapProperty<?> inMap = instance.getPropertyAsMap(CoreProperties.expectedInput.toString());
		if (inMap != null && inMap.get() != null) {
			return ( Map<String, InstanceType>) inMap.get();
		} else return Collections.emptyMap();
	}

	@SuppressWarnings("unchecked")
	public void addExpectedInput(String paramName, InstanceType type) {
		instance.getPropertyAsMap(CoreProperties.expectedInput.toString()).put(paramName, type);
	}
	
	public Map<String, InstanceType> getExpectedOutput() {
		// TODO Auto-generated method stub
		return Collections.emptyMap();
	}

	public Map<String, String> getInputToOutputMappingRules() {
		// TODO Auto-generated method stub
		return Collections.emptyMap();
	}

	public Optional<String> getCondition(Conditions condition) {
		String rule = (String)instance.getPropertyAsMap(CoreProperties.conditions.toString()).get(condition.toString());
		return Optional.ofNullable(rule);
	}
	
	public void setCondition(Conditions condition, String ruleAsString) {
		instance.getPropertyAsMap(CoreProperties.conditions.toString()).put(condition.toString(), ruleAsString);
	}

	public Optional<String> getPostconditionRule() {
		// TODO Auto-generated method stub
		return Optional.empty();
	}

	public Optional<String> getActivationRule() {
		// TODO Auto-generated method stub
		return Optional.empty();
	}

	public Optional<String> getCancelationRule() {
		// TODO Auto-generated method stub
		return Optional.empty();
	}

	@SuppressWarnings("unchecked")
	public Set<QAConstraintSpec> getQAConstraints() {
		SetProperty<?> qaSet = instance.getPropertyAsSet(CoreProperties.qaConstraints.toString());
		if (qaSet != null && qaSet.get() != null) {
			return (Set<QAConstraintSpec>) qaSet.get().stream()
					.map(inst -> WrapperCache.getWrappedInstance(QAConstraintSpec.class, (Instance) inst))
					.collect(Collectors.toSet());
		} else return Collections.emptySet();
	}
	
	@SuppressWarnings("unchecked")
	public void addQAConstraint(QAConstraintSpec spec) {
		instance.getPropertyAsSet(CoreProperties.qaConstraints.toString()).add(spec.getInstance());
	}
	
	public DecisionNodeDefinition getOutDND() {
		// TODO Auto-generated method stub
		return null;
	}

	public DecisionNodeDefinition getInDND() {
		// TODO Auto-generated method stub
		return null;
	}

	public static InstanceType getOrCreateDesignSpaceCoreSchema(Workspace ws) {
		Optional<InstanceType> thisType = ws.debugInstanceTypes().stream()
				.filter(it -> it.name().contentEquals(designspaceTypeId))
				.findAny();
			if (thisType.isPresent())
				return thisType.get();
			else {
				InstanceType typeStep = ws.createInstanceType(designspaceTypeId, ws.TYPES_FOLDER);
				typeStep.createPropertyType(CoreProperties.qaConstraints.toString(), Cardinality.SET, QAConstraintSpec.getOrCreateDesignSpaceCoreSchema(ws));
				typeStep.createPropertyType(CoreProperties.expectedInput.toString(), Cardinality.MAP, ws.META_INSTANCE_TYPE);
				typeStep.createPropertyType(CoreProperties.conditions.toString(), Cardinality.MAP, Workspace.STRING);
				return typeStep;
			}
	}

	public static StepDefinition getInstance(String stepId, Workspace ws) {
		Instance instance = ws.createInstance(getOrCreateDesignSpaceCoreSchema(ws), stepId);
		return WrapperCache.getWrappedInstance(StepDefinition.class, instance);
	}

}
