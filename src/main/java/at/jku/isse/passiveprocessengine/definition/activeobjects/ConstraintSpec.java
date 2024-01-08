package at.jku.isse.passiveprocessengine.definition.activeobjects;

import at.jku.isse.passiveprocessengine.WrapperCache;
import at.jku.isse.passiveprocessengine.configurability.ProcessConfigBaseElementFactory;
import at.jku.isse.passiveprocessengine.core.Instance;
import at.jku.isse.passiveprocessengine.definition.types.ConstraintSpecType;
import at.jku.isse.passiveprocessengine.instance.StepLifecycle.Conditions;

public class ConstraintSpec extends  ProcessDefinitionScopedElement{


	public ConstraintSpec(Instance instance, WrapperCache wrapperCache) {
		super(instance, wrapperCache);
	}

	public String getConstraintId() {
		return instance.getId();
	}

	public String getConstraintSpec() {
		return instance.getTypedProperty(ConstraintSpecType.CoreProperties.constraintSpec.toString(), String.class);
	}

	public String getHumanReadableDescription() {
		return instance.getTypedProperty(ConstraintSpecType.CoreProperties.humanReadableDescription.toString(), String.class);
	}

	public Integer getOrderIndex() {
		return instance.getTypedProperty(ConstraintSpecType.CoreProperties.specOrderIndex.toString(), Integer.class);
	}

	public boolean isOverridable() {
		return instance.getTypedProperty(ConstraintSpecType.CoreProperties.isOverridable.toString(), Boolean.class);
	}

	public Conditions getConditionType() {
		return Conditions.valueOf((String)instance.getTypedProperty(ConstraintSpecType.CoreProperties.conditionsType.toString(), String.class));
	}

	@Override
	public void deleteCascading(ProcessConfigBaseElementFactory configFactory) {
		instance.markAsDeleted();
	}
}
