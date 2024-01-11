package at.jku.isse.passiveprocessengine.instance.activeobjects;

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

import at.jku.isse.passiveprocessengine.Context;
import at.jku.isse.passiveprocessengine.core.Instance;
import at.jku.isse.passiveprocessengine.core.RuleResult;
import at.jku.isse.passiveprocessengine.definition.activeobjects.ConstraintSpec;
import at.jku.isse.passiveprocessengine.definition.activeobjects.ProcessDefinitionScopedElement;
import at.jku.isse.passiveprocessengine.instance.types.ConstraintWrapperType;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ConstraintResultWrapper extends ProcessInstanceScopedElement {

	private volatile ZonedDateTime lastChanged;

	public ConstraintResultWrapper(Instance instance, Context context) {
		super(instance, context);

	}

	public RuleResult getRuleResult() {
		return (RuleResult) instance.getTypedProperty(ConstraintWrapperType.CoreProperties.crule.toString(), RuleResult.class);
	}

	protected void setCrIfEmpty(RuleResult cr) {
		Object prevRuleResult = instance.getTypedProperty(ConstraintWrapperType.CoreProperties.crule.toString(), RuleResult.class);
		if (prevRuleResult == null) {
			instance.setSingleProperty(ConstraintWrapperType.CoreProperties.crule.toString(), cr);
		}
	}

	public Boolean getEvalResult() {
		if (getIsOverriden()) {
			return getOverrideValue();
		} else {
			return getTrueResult();
		}
	}

	private Boolean getTrueResult() {
		RuleResult cr = getRuleResult();
		if (cr == null)
			return false;
		else {
			return cr.isConsistent();
		}
	}

	public ZonedDateTime getLastChanged() {
		if (lastChanged == null) { // then load
			String last = instance.getTypedProperty(ConstraintWrapperType.CoreProperties.lastChanged.toString(), String.class);
			lastChanged = ZonedDateTime.parse(last);
		}
		return lastChanged;
	}

	//Not to be called directly, only public for factory access
	public void setLastChanged(ZonedDateTime lastChanged) {
		instance.setSingleProperty(ConstraintWrapperType.CoreProperties.lastChanged.toString(), lastChanged.toString());
		this.lastChanged = lastChanged;
	}

	public ConstraintSpec getConstraintSpec() {
		Instance qainst = instance.getTypedProperty(ConstraintWrapperType.CoreProperties.qaSpec.toString(), Instance.class);
		return context.getWrappedInstance(ConstraintSpec.class, qainst);
	}

	//Not to be called directly, only public for factory access
	public void setSpec(ConstraintSpec qaSpec) {
		instance.setSingleProperty(ConstraintWrapperType.CoreProperties.qaSpec.toString(), qaSpec.getInstance());
	}

	public ProcessStep getParentStep() {
		Instance step = instance.getTypedProperty(ConstraintWrapperType.CoreProperties.parentStep.toString(), Instance.class);
		if (step != null)
			return context.getWrappedInstance(ProcessStep.class, step);
		else
			return null;
	}

	@Override
	public ProcessDefinitionScopedElement getDefinition() {
		return getConstraintSpec();
	}

	public boolean getIsOverriden() {
		return instance.getTypedProperty(ConstraintWrapperType.CoreProperties.isOverriden.toString(), Boolean.class);
	}

	public boolean getOverrideValue() {
		return instance.getTypedProperty(ConstraintWrapperType.CoreProperties.overrideValue.toString(), Boolean.class);
	}

	public boolean isOverrideDiffFromConstraintResult() {
		// if no override is there, then no effect
		if (getIsOverriden()) {
			return getOverrideValue() != getTrueResult();
		} else return false;
	}

	public String getOverrideReasonOrNull() {
		return instance.getTypedProperty(ConstraintWrapperType.CoreProperties.overrideReason.toString(), String.class);
	}

	public void removeOverride() {
		setIsOverriden(false);
		setOverrideReason("");
		context.getInstanceRepository().concludeTransaction();
	}

	public String setOverrideWithReason(String reason) {
		if (getConstraintSpec().isOverridable()) {
			if (reason == null || reason.length() == 0) {
				String msg = String.format("Attempt to override value of constraint %s without providing a reason", getConstraintSpec().getName());
				log.warn(msg);
				return msg;
			} else {
				setIsOverriden(true);
				setOverrideReason(reason);
				setOverrideValue(!getTrueResult());
				context.getInstanceRepository().concludeTransaction();				
				return "";
			}
		} else {
			String msg = String.format("Attempt to override value of non-overridable constraint %s ", getConstraintSpec().getName());
			log.warn(msg);
			return msg;
		}
	}

	//Not to be called directly, only public for factory access
	public void setIsOverriden(boolean isOverriden) {
		instance.setSingleProperty(ConstraintWrapperType.CoreProperties.isOverriden.toString(), isOverriden);
	}

	private void setOverrideValue(boolean overrideTo) {
		instance.setSingleProperty(ConstraintWrapperType.CoreProperties.overrideValue.toString(), overrideTo);
	}

	//Not to be called directly, only public for factory access
	public void setOverrideReason(String reason) {
		instance.setSingleProperty(ConstraintWrapperType.CoreProperties.overrideReason.toString(), reason);
	}

	@Override
	public void deleteCascading() {
		super.deleteCascading();
	}

//	public static InstanceType getOrCreateDesignSpaceInstanceType(Workspace ws){
//		Optional<InstanceType> thisType = Optional.ofNullable(ws.TYPES_FOLDER.instanceTypeWithName(ConstraintWrapperType.designspaceTypeId));
//			if (thisType.isPresent())
//				return thisType.get();
//			else {
//				InstanceType typeStep = ws.createInstanceType(ConstraintWrapperType.designspaceTypeId, ws.TYPES_FOLDER, ProcessInstanceScopedElement.getOrCreateDesignSpaceCoreSchema(ws));
//				ProcessInstanceScopedElement.addGenericProcessProperty(typeStep);
//				typeStep.createPropertyType(ConstraintWrapperType.CoreProperties.qaSpec.toString(), Cardinality.SINGLE, ConstraintSpec.getOrCreateDesignSpaceCoreSchema(ws));
//				typeStep.createPropertyType(ConstraintWrapperType.CoreProperties.parentStep.toString(), Cardinality.SINGLE, ProcessStep.getOrCreateDesignSpaceCoreSchema(ws));
//				typeStep.createPropertyType(ConstraintWrapperType.CoreProperties.lastChanged.toString(), Cardinality.SINGLE, Workspace.STRING);
//				typeStep.createPropertyType(ConstraintWrapperType.CoreProperties.crule.toString(), Cardinality.SINGLE, ws.its(ConsistencyRuleType.CONSISTENCY_RULE_TYPE));
//				typeStep.createPropertyType(ConstraintWrapperType.CoreProperties.isOverriden.toString(), Cardinality.SINGLE, Workspace.BOOLEAN);
//				typeStep.createPropertyType(ConstraintWrapperType.CoreProperties.overrideValue.toString(), Cardinality.SINGLE, Workspace.BOOLEAN);
//				typeStep.createPropertyType(ConstraintWrapperType.CoreProperties.overrideReason.toString(), Cardinality.SINGLE, Workspace.STRING);
//				return typeStep;
//			}
//	}

//	public static ConstraintWrapper getInstance(Workspace ws, ConstraintSpec qaSpec, ZonedDateTime lastChanged, ProcessStep owningStep, ProcessInstance proc) {
//		Instance inst = ws.createInstance(getOrCreateDesignSpaceInstanceType(ws), qaSpec.getName()+proc.getName()+"_"+UUID.randomUUID());
//		ConstraintWrapper cw = context.getWrappedInstance(ConstraintWrapper.class, inst);
//		cw.instance.setSingleProperty(ConstraintWrapperType.CoreProperties.parentStep.toString(), owningStep.getInstance());
//		cw.setSpec(qaSpec);
//		cw.setLastChanged(lastChanged);
//		cw.setProcess(proc);
//		cw.setOverrideReason("");
//		cw.setIsOverriden(false);
//		return cw;
//	}

}
