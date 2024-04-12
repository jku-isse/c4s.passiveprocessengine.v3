package at.jku.isse.passiveprocessengine.instance;

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

import at.jku.isse.designspace.core.model.Cardinality;
import at.jku.isse.designspace.core.model.Instance;
import at.jku.isse.designspace.core.model.InstanceType;
import at.jku.isse.designspace.core.model.SingleProperty;
import at.jku.isse.designspace.core.model.Workspace;
import at.jku.isse.designspace.rule.model.ConsistencyRule;
import at.jku.isse.designspace.rule.model.ConsistencyRuleType;
import at.jku.isse.passiveprocessengine.ProcessDefinitionScopedElement;
import at.jku.isse.passiveprocessengine.ProcessInstanceScopedElement;
import at.jku.isse.passiveprocessengine.WrapperCache;
import at.jku.isse.passiveprocessengine.definition.ConstraintSpec;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ConstraintWrapper extends ProcessInstanceScopedElement {

	
	public static final String designspaceTypeId = ConstraintWrapper.class.getSimpleName();
	static enum CoreProperties {qaSpec, lastChanged, crule, parentStep, isOverriden, overrideValue, overrideReason};
	
	ZonedDateTime lastChanged;
	
	public ConstraintWrapper(Instance instance) {
		super(instance);
		
	}
	

	
	public ConsistencyRule getCr() {
		return (ConsistencyRule) instance.getPropertyAsValueOrNull(CoreProperties.crule.toString());
	}

	protected void setCrIfEmpty(ConsistencyRule cr) {
		SingleProperty prop = instance.getPropertyAsSingle(CoreProperties.crule.toString());
		if (prop.get() == null)
			prop.set(cr);
	}

	public Boolean getEvalResult() {		
		if (getIsOverriden()) {
			return getOverrideValue();
		} else {
			return getTrueResult();
		}
	}
	
	private Boolean getTrueResult() {
		ConsistencyRule cr = getCr();
		if (cr == null)
			return false;
		else {
			return cr.isConsistent();
		}
	}

	public ZonedDateTime getLastChanged() {
		if (lastChanged == null) { // load from DS
			String last = (String) instance.getPropertyAsValue(CoreProperties.lastChanged.toString());
			lastChanged = ZonedDateTime.parse(last);
		}
		return lastChanged;
	}

	protected void setLastChanged(ZonedDateTime lastChanged) {
		instance.getPropertyAsSingle(CoreProperties.lastChanged.toString()).set(lastChanged.toString());
		this.lastChanged = lastChanged;
	}

	public ConstraintSpec getSpec() {
		Instance qainst = instance.getPropertyAsInstance(CoreProperties.qaSpec.toString());
		return WrapperCache.getWrappedInstance(ConstraintSpec.class, qainst);
	}

	private void setSpec(ConstraintSpec qaSpec) {
		instance.getPropertyAsSingle(CoreProperties.qaSpec.toString()).set(qaSpec.getInstance());
	}
	
	public ProcessStep getParentStep() {
		Instance step = instance.getPropertyAsInstance(CoreProperties.parentStep.toString());
		if (step != null)
			return WrapperCache.getWrappedInstance(ProcessStep.class, step);
		else 
			return null;
	}
	
	@Override
	public ProcessDefinitionScopedElement getDefinition() {
		return getSpec();
	}	
	
	public boolean getIsOverriden() {
		return (boolean) instance.getPropertyAsValue(CoreProperties.isOverriden.toString());
	}
	
	public boolean getOverrideValue() {
		return (boolean) instance.getPropertyAsValue(CoreProperties.overrideValue.toString());
	}
	
	public boolean isOverrideDiffFromConstraintResult() {
		// if no override is there, then no effect
		if (getIsOverriden()) {
			return getOverrideValue() != getTrueResult();
		} else return false;
	}
	
	public String getOverrideReasonOrNull() {
		return (String) instance.getPropertyAsValueOrNull(CoreProperties.overrideReason.toString());
	}
	
	public void removeOverride() {
		setIsOverriden(false);
		setOverrideReason("");
		this.instance.workspace.concludeTransaction();
	}
	
	public String setOverrideWithReason(String reason) {
		if (getSpec().isOverridable()) {
			if (reason == null || reason.length() == 0) {
				String msg = String.format("Attempt to override value of constraint %s without providing a reason", getSpec().getName());
				log.warn(msg);
				return msg;
			} else {
				setIsOverriden(true);				
				setOverrideReason(reason);
				setOverrideValue(!getTrueResult());		
				this.instance.workspace.concludeTransaction();
				return "";
			}						
		} else {		
			String msg = String.format("Attempt to override value of non-overridable constraint %s ", getSpec().getName());
			log.warn(msg);
			return msg;
		}		
	}		
	
	private void setIsOverriden(boolean isOverriden) {
		instance.getPropertyAsSingle(CoreProperties.isOverriden.toString()).set(isOverriden);
	}
	
	private void setOverrideValue(boolean overrideTo) {
		instance.getPropertyAsSingle(CoreProperties.overrideValue.toString()).set(overrideTo);
	}
	
	private void setOverrideReason(String reason) {
		instance.getPropertyAsSingle(CoreProperties.overrideReason.toString()).set(reason);
	}
	
	public void deleteCascading() {
		super.deleteCascading();
	}
	
	public static InstanceType getOrCreateDesignSpaceInstanceType(Workspace ws){
		Optional<InstanceType> thisType = Optional.ofNullable(ws.TYPES_FOLDER.instanceTypeWithName(designspaceTypeId));
			if (thisType.isPresent())
				return thisType.get();
			else {
				InstanceType typeStep = ws.createInstanceType(designspaceTypeId, ws.TYPES_FOLDER, ProcessInstanceScopedElement.getOrCreateDesignSpaceCoreSchema(ws));
				ProcessInstanceScopedElement.addGenericProcessProperty(typeStep);
				typeStep.createPropertyType(CoreProperties.qaSpec.toString(), Cardinality.SINGLE, ConstraintSpec.getOrCreateDesignSpaceCoreSchema(ws));
				typeStep.createPropertyType(CoreProperties.parentStep.toString(), Cardinality.SINGLE, ProcessStep.getOrCreateDesignSpaceCoreSchema(ws));
				typeStep.createPropertyType(CoreProperties.lastChanged.toString(), Cardinality.SINGLE, Workspace.STRING);
				typeStep.createPropertyType(CoreProperties.crule.toString(), Cardinality.SINGLE, ws.its(ConsistencyRuleType.CONSISTENCY_RULE_TYPE));
				typeStep.createPropertyType(CoreProperties.isOverriden.toString(), Cardinality.SINGLE, Workspace.BOOLEAN);
				typeStep.createPropertyType(CoreProperties.overrideValue.toString(), Cardinality.SINGLE, Workspace.BOOLEAN);
				typeStep.createPropertyType(CoreProperties.overrideReason.toString(), Cardinality.SINGLE, Workspace.STRING);
				return typeStep;
			}
	}
	
	public static ConstraintWrapper getInstance(Workspace ws, ConstraintSpec qaSpec, ZonedDateTime lastChanged, ProcessStep owningStep, ProcessInstance proc) {
		Instance inst = ws.createInstance(getOrCreateDesignSpaceInstanceType(ws), qaSpec.getId()+proc.getName()+"_"+UUID.randomUUID());
		ConstraintWrapper cw = WrapperCache.getWrappedInstance(ConstraintWrapper.class, inst);
		cw.instance.getPropertyAsSingle(CoreProperties.parentStep.toString()).set(owningStep.getInstance());
		cw.setSpec(qaSpec);
		cw.setLastChanged(lastChanged);
		cw.setProcess(proc);
		cw.setOverrideReason("");
		cw.setIsOverriden(false);
		return cw;
	}

	
}
