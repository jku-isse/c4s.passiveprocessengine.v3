package at.jku.isse.passiveprocessengine.definition.factories;

import at.jku.isse.passiveprocessengine.core.FactoryIndex.DomainFactory;
import at.jku.isse.passiveprocessengine.core.ProcessContext;
import at.jku.isse.passiveprocessengine.core.PPEInstance;
import at.jku.isse.passiveprocessengine.definition.activeobjects.ConstraintSpec;
import at.jku.isse.passiveprocessengine.definition.serialization.DTOs;
import at.jku.isse.passiveprocessengine.definition.types.ConstraintSpecType;
import at.jku.isse.passiveprocessengine.instance.StepLifecycle.Conditions;

public class ConstraintSpecFactory extends DomainFactory{
		
	public ConstraintSpecFactory(ProcessContext context) {
		super(context);
	}
	
	public ConstraintSpec createInstance(Conditions condition, DTOs.Constraint constraintSpec) {
		return createInstance(condition, constraintSpec.getCode(), constraintSpec.getArlRule(), constraintSpec.getDescription(), constraintSpec.getSpecOrderIndex(), constraintSpec.isOverridable());
	}

	@Deprecated
	public ConstraintSpec createInstance(Conditions condition, String constraintId, String constraintSpec, String humanReadableDescription, int specOrderIndex) {
		return createInstance(condition, constraintId, constraintSpec, humanReadableDescription, specOrderIndex, false);
	}
	
	public ConstraintSpec createInstance(Conditions condition, String constraintId, String constraintSpec, String humanReadableDescription, int specOrderIndex, boolean isOverridable) {
		return createInstance(condition, constraintId, constraintSpec, constraintSpec, humanReadableDescription, specOrderIndex, isOverridable);
	}

	public ConstraintSpec createInstance(Conditions condition, String constraintId, String augmentedSpec, String constraintSpec, String humanReadableDescription, int specOrderIndex, boolean isOverridable) {
		PPEInstance instance = getContext().getInstanceRepository().createInstance(constraintId, getContext().getSchemaRegistry().getTypeByName(ConstraintSpecType.typeId));
		instance.setSingleProperty(ConstraintSpecType.CoreProperties.constraintSpec.toString(),constraintSpec);
		instance.setSingleProperty(ConstraintSpecType.CoreProperties.augmentedSpec.toString(),augmentedSpec);
		instance.setSingleProperty(ConstraintSpecType.CoreProperties.humanReadableDescription.toString(), humanReadableDescription == null ? "" : humanReadableDescription);
		instance.setSingleProperty(ConstraintSpecType.CoreProperties.constraintSpecOrderIndex.toString(), specOrderIndex);
		instance.setSingleProperty(ConstraintSpecType.CoreProperties.isOverridable.toString(), isOverridable);
		instance.setSingleProperty(ConstraintSpecType.CoreProperties.conditionsType.toString(), condition.toString());
		return getContext().getWrappedInstance(ConstraintSpec.class, instance);
	}
	
}
