package at.jku.isse.passiveprocessengine.definition;

import java.util.Optional;

import at.jku.isse.designspace.core.model.Cardinality;
import at.jku.isse.designspace.core.model.Instance;
import at.jku.isse.designspace.core.model.InstanceType;
import at.jku.isse.designspace.core.model.Workspace;
import at.jku.isse.designspace.rule.model.ConsistencyRuleType;
import at.jku.isse.passiveprocessengine.ProcessDefinitionScopedElement;
import at.jku.isse.passiveprocessengine.WrapperCache;
import at.jku.isse.passiveprocessengine.configurability.ProcessConfigBaseElementFactory;
import at.jku.isse.passiveprocessengine.definition.serialization.DTOs;
import at.jku.isse.passiveprocessengine.instance.StepLifecycle.Conditions;

public class ConstraintSpec extends /*InstanceWrapper*/ ProcessDefinitionScopedElement{

	
	public static enum CoreProperties {constraintSpec, humanReadableDescription, specOrderIndex, isOverridable, ruleType, conditionsType};
	public static final String designspaceTypeId = ConstraintSpec.class.getSimpleName();
	
	public ConstraintSpec(Instance instance) {
		super(instance);
	}

	public String getConstraintId() {
		return instance.name();
	}

	public String getConstraintSpec() {
		return (String) instance.getPropertyAsValue(CoreProperties.constraintSpec.toString());
	}

	public String getHumanReadableDescription() {
		return (String) instance.getPropertyAsValue(CoreProperties.humanReadableDescription.toString());
	}
	
	public Integer getOrderIndex() {
		return (Integer) instance.getPropertyAsValue(CoreProperties.specOrderIndex.toString());
	}
	
	public boolean isOverridable() {
		return (Boolean) instance.getPropertyAsValue(CoreProperties.isOverridable.toString());
	}
	
	public Conditions getConditionType() {
		return Conditions.valueOf((String)instance.getPropertyAsValue(CoreProperties.conditionsType.toString()));
	}
	
	@Override
	public void deleteCascading(ProcessConfigBaseElementFactory configFactory) {
		instance.delete();
	}
	
	public static InstanceType getOrCreateDesignSpaceCoreSchema(Workspace ws) {
		Optional<InstanceType> thisType = Optional.ofNullable(ws.TYPES_FOLDER.instanceTypeWithName(designspaceTypeId)); 
			if (thisType.isPresent())
				return thisType.get();
			else {
				InstanceType specType = ws.createInstanceType(designspaceTypeId, ws.TYPES_FOLDER, ProcessDefinitionScopedElement.getOrCreateDesignSpaceCoreSchema(ws));
				// constraintId maps to Instance name property
				specType.createPropertyType(CoreProperties.constraintSpec.toString(), Cardinality.SINGLE, Workspace.STRING);
				specType.createPropertyType(CoreProperties.humanReadableDescription.toString(), Cardinality.SINGLE, Workspace.STRING);
				specType.createPropertyType((CoreProperties.specOrderIndex.toString()), Cardinality.SINGLE, Workspace.INTEGER);
				specType.createPropertyType((CoreProperties.isOverridable.toString()), Cardinality.SINGLE, Workspace.BOOLEAN);
				specType.createPropertyType((CoreProperties.ruleType.toString()), Cardinality.SINGLE, ConsistencyRuleType.CONSISTENCY_RULE_TYPE);
				specType.createPropertyType(CoreProperties.conditionsType.toString(), Cardinality.SINGLE, Workspace.STRING);
				return specType;
			}
	}
	
	public static ConstraintSpec createInstance(Conditions condition, DTOs.Constraint constraintSpec, Workspace ws) {
		return createInstance(condition, constraintSpec.getCode(), constraintSpec.getArlRule(), constraintSpec.getDescription(), constraintSpec.getSpecOrderIndex(), constraintSpec.isOverridable(), ws );
	}
	
	@Deprecated
	public static ConstraintSpec createInstance(Conditions condition, String constraintId, String constraintSpec, String humanReadableDescription, int specOrderIndex, Workspace ws) {
		return createInstance(condition, constraintId, constraintSpec, humanReadableDescription, specOrderIndex, false, ws);
	}
	public static ConstraintSpec createInstance(Conditions condition, String constraintId, String constraintSpec, String humanReadableDescription, int specOrderIndex, boolean isOverridable, Workspace ws) {
		Instance instance = ws.createInstance(getOrCreateDesignSpaceCoreSchema(ws), constraintId);
		instance.getPropertyAsSingle(CoreProperties.constraintSpec.toString()).set(constraintSpec);
		instance.getPropertyAsSingle(CoreProperties.humanReadableDescription.toString()).set(humanReadableDescription == null ? "" : humanReadableDescription);
		instance.getPropertyAsSingle(CoreProperties.specOrderIndex.toString()).set(specOrderIndex);
		instance.getPropertyAsSingle(CoreProperties.isOverridable.toString()).set(isOverridable);		
		instance.getPropertyAsSingle(CoreProperties.conditionsType.toString()).set(condition.toString());
		return WrapperCache.getWrappedInstance(ConstraintSpec.class, instance);
	}



}
