package at.jku.isse.passiveprocessengine.definition.activeobjects;

import java.util.Comparator;

import at.jku.isse.passiveprocessengine.core.ProcessContext;
import at.jku.isse.passiveprocessengine.core.PPEInstance;
import at.jku.isse.passiveprocessengine.definition.types.ConstraintSpecType;
import at.jku.isse.passiveprocessengine.instance.StepLifecycle.Conditions;
import at.jku.isse.passiveprocessengine.instance.types.ProcessConfigBaseElementType;

public class ConstraintSpec extends  ProcessDefinitionScopedElement{




	public ConstraintSpec(PPEInstance instance, ProcessContext context) {
		super(instance, context);
	}

	public String getConstraintId() {
		return instance.getName();
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
		return Conditions.valueOf(instance.getTypedProperty(ConstraintSpecType.CoreProperties.conditionsType.toString(), String.class));
	}

	@Override
	public void deleteCascading() {
		super.deleteCascading();
	}
	
	public static Comparator<ConstraintSpec> COMPARATOR_BY_ORDERINDEX = new Comparator<>() {
	@Override
	public int compare(ConstraintSpec o1, ConstraintSpec o2) {
		return o1.getOrderIndex().compareTo(o2.getOrderIndex()) ;
	}};
}
