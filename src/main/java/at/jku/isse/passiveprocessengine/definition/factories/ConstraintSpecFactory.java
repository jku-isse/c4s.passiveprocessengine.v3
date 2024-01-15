package at.jku.isse.passiveprocessengine.definition.factories;

import at.jku.isse.passiveprocessengine.Context;
import at.jku.isse.passiveprocessengine.core.DomainTypesRegistry;
import at.jku.isse.passiveprocessengine.core.FactoryIndex.DomainFactory;
import at.jku.isse.passiveprocessengine.core.Instance;
import at.jku.isse.passiveprocessengine.core.InstanceRepository;
import at.jku.isse.passiveprocessengine.definition.activeobjects.ConstraintSpec;
import at.jku.isse.passiveprocessengine.definition.serialization.DTOs;
import at.jku.isse.passiveprocessengine.definition.types.ConstraintSpecType;
import at.jku.isse.passiveprocessengine.instance.StepLifecycle.Conditions;

public class ConstraintSpecFactory extends DomainFactory{
		
	public ConstraintSpecFactory(Context context) {
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
		Instance instance = getContext().getInstanceRepository().createInstance(constraintId, getContext().getSchemaRegistry().getType(ConstraintSpec.class));
		instance.setSingleProperty(ConstraintSpecType.CoreProperties.constraintSpec.toString(),constraintSpec);
		instance.setSingleProperty(ConstraintSpecType.CoreProperties.humanReadableDescription.toString(), humanReadableDescription == null ? "" : humanReadableDescription);
		instance.setSingleProperty(ConstraintSpecType.CoreProperties.specOrderIndex.toString(), specOrderIndex);
		instance.setSingleProperty(ConstraintSpecType.CoreProperties.isOverridable.toString(), isOverridable);
		instance.setSingleProperty(ConstraintSpecType.CoreProperties.conditionsType.toString(), condition.toString());
		return getContext().getWrappedInstance(ConstraintSpec.class, instance);
	}

}
