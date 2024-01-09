package at.jku.isse.passiveprocessengine.definition.factories;

import at.jku.isse.passiveprocessengine.Context;
import at.jku.isse.passiveprocessengine.core.Instance;
import at.jku.isse.passiveprocessengine.core.InstanceRepository;
import at.jku.isse.passiveprocessengine.definition.activeobjects.ConstraintSpec;
import at.jku.isse.passiveprocessengine.definition.serialization.DTOs;
import at.jku.isse.passiveprocessengine.definition.types.ConstraintSpecType;
import at.jku.isse.passiveprocessengine.definition.types.ProcessDomainTypesFactory;
import at.jku.isse.passiveprocessengine.instance.StepLifecycle.Conditions;

public class ConstraintSpecFactory {
	InstanceRepository repository;
	Context wrapperCache;
	ProcessDomainTypesFactory typesFactory;
	
	public ConstraintSpecFactory(InstanceRepository repository, Context wrapperCache, ProcessDomainTypesFactory typesFactory) {
		this.repository = repository;
		this.wrapperCache = wrapperCache;
		this.typesFactory = typesFactory;
	}
	
	public ConstraintSpec createInstance(Conditions condition, DTOs.Constraint constraintSpec) {
		return createInstance(condition, constraintSpec.getCode(), constraintSpec.getArlRule(), constraintSpec.getDescription(), constraintSpec.getSpecOrderIndex(), constraintSpec.isOverridable());
	}

	@Deprecated
	public ConstraintSpec createInstance(Conditions condition, String constraintId, String constraintSpec, String humanReadableDescription, int specOrderIndex) {
		return createInstance(condition, constraintId, constraintSpec, humanReadableDescription, specOrderIndex, false);
	}
	public ConstraintSpec createInstance(Conditions condition, String constraintId, String constraintSpec, String humanReadableDescription, int specOrderIndex, boolean isOverridable) {
		Instance instance = repository.createInstance(constraintId, typesFactory.getType(ConstraintSpec.class));
		instance.setSingleProperty(ConstraintSpecType.CoreProperties.constraintSpec.toString(),constraintSpec);
		instance.setSingleProperty(ConstraintSpecType.CoreProperties.humanReadableDescription.toString(), humanReadableDescription == null ? "" : humanReadableDescription);
		instance.setSingleProperty(ConstraintSpecType.CoreProperties.specOrderIndex.toString(), specOrderIndex);
		instance.setSingleProperty(ConstraintSpecType.CoreProperties.isOverridable.toString(), isOverridable);
		instance.setSingleProperty(ConstraintSpecType.CoreProperties.conditionsType.toString(), condition.toString());
		return wrapperCache.getWrappedInstance(ConstraintSpec.class, instance);
	}

}
