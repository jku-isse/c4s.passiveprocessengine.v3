package at.jku.isse.passiveprocessengine.instance.activeobjects;

import java.time.ZonedDateTime;

import org.apache.jena.ontapi.model.OntIndividual;

import at.jku.isse.passiveprocessengine.rdfwrapper.NodeToDomainResolver;
import at.jku.isse.passiveprocessengine.rdfwrapper.RDFInstanceType;
import at.jku.isse.passiveprocessengine.rdfwrapper.rule.RDFRuleResultWrapper;
import at.jku.isse.passiveprocessengine.definition.activeobjects.ConstraintSpec;
import at.jku.isse.passiveprocessengine.definition.activeobjects.ProcessDefinitionScopedElement;
import at.jku.isse.passiveprocessengine.instance.types.ConstraintResultWrapperTypeFactory.CoreProperties;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ConstraintResultWrapper extends ProcessInstanceScopedElement {

	private volatile ZonedDateTime lastChanged;

	public ConstraintResultWrapper(@NonNull OntIndividual element, RDFInstanceType type, @NonNull NodeToDomainResolver resolver) {
		super(element, type, resolver);
	}

	public  RDFRuleResultWrapper getRuleResult() {
		return getTypedProperty(CoreProperties.ruleResult.toString(), RDFRuleResultWrapper.class);
	}

	protected void setRuleResultIfEmpty(RDFRuleResultWrapper cr) {
		var prevRuleResult = getRuleResult();
		if (prevRuleResult == null) {
			setSingleProperty(CoreProperties.ruleResult.toString(), cr);
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
		var cr = getRuleResult();
		if (cr == null)
			return false;
		else {
			return cr.isConsistent();
		}
	}

	public ZonedDateTime getLastChanged() {
		if (lastChanged == null) { // then load
			String last = getTypedProperty(CoreProperties.lastChanged.toString(), String.class);
			lastChanged = ZonedDateTime.parse(last);
		}
		return lastChanged;
	}

	//Not to be called directly, only public for factory access
	public void setLastChanged(ZonedDateTime lastChanged) {
		setSingleProperty(CoreProperties.lastChanged.toString(), lastChanged.toString());
		this.lastChanged = lastChanged;
	}

	public ConstraintSpec getConstraintSpec() {
		return getTypedProperty(CoreProperties.constraintSpec.toString(), ConstraintSpec.class);		
	}

	//Not to be called directly, only public for factory access
	public void setSpec(ConstraintSpec qaSpec) {
		setSingleProperty(CoreProperties.constraintSpec.toString(), qaSpec.getInstance());
	}

	public ProcessStep getParentStep() {
		return getTypedProperty(CoreProperties.parentStep.toString(), ProcessStep.class);		
	}

	@Override
	public ProcessDefinitionScopedElement getDefinition() {
		return getConstraintSpec();
	}

	public boolean getIsOverriden() {
		return getTypedProperty(CoreProperties.isOverriden.toString(), Boolean.class, Boolean.FALSE);
	}

	public boolean getOverrideValue() {
		return getTypedProperty(CoreProperties.overrideValue.toString(), Boolean.class);
	}

	public boolean isOverrideDiffFromConstraintResult() {
		// if no override is there, then no effect
		if (getIsOverriden()) {
			return getOverrideValue() != getTrueResult();
		} else return false;
	}

	public String getOverrideReasonOrNull() {
		return getTypedProperty(CoreProperties.overrideReason.toString(), String.class);
	}

	public void removeOverride() {
		setIsOverriden(false);
		setOverrideReason("");
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
		setSingleProperty(CoreProperties.isOverriden.toString(), isOverriden);
	}

	private void setOverrideValue(boolean overrideTo) {
		setSingleProperty(CoreProperties.overrideValue.toString(), overrideTo);
	}

	//Not to be called directly, only public for factory access
	public void setOverrideReason(String reason) {
		setSingleProperty(CoreProperties.overrideReason.toString(), reason);
	}

	@Override
	public void deleteCascading() {
		var ruleResult = this.getRuleResult();
		if (ruleResult != null) {
			ruleResult.delete();
		}
		super.deleteCascading();
		
	}

//	public static InstanceType getOrCreateDesignSpaceInstanceType(Workspace ws){
//		Optional<InstanceType> thisType = Optional.ofNullable(ws.TYPES_FOLDER.instanceTypeWithName(designspaceTypeId));
//			if (thisType.isPresent())
//				return thisType.get();
//			else {
//				InstanceType typeStep = ws.createInstanceType(designspaceTypeId, ws.TYPES_FOLDER, ProcessInstanceScopedElement.getOrCreateDesignSpaceCoreSchema(ws));
//				ProcessInstanceScopedElement.addGenericProcessProperty(typeStep);
//				typeStep.createPropertyType(CoreProperties.qaSpec.toString(), Cardinality.SINGLE, ConstraintSpec.getOrCreateDesignSpaceCoreSchema(ws));
//				typeStep.createPropertyType(CoreProperties.parentStep.toString(), Cardinality.SINGLE, ProcessStep.getOrCreateDesignSpaceCoreSchema(ws));
//				typeStep.createPropertyType(CoreProperties.lastChanged.toString(), Cardinality.SINGLE, Workspace.STRING);
//				typeStep.createPropertyType(CoreProperties.crule.toString(), Cardinality.SINGLE, ws.its(ConsistencyRuleType.CONSISTENCY_RULE_TYPE));
//				typeStep.createPropertyType(CoreProperties.isOverriden.toString(), Cardinality.SINGLE, Workspace.BOOLEAN);
//				typeStep.createPropertyType(CoreProperties.overrideValue.toString(), Cardinality.SINGLE, Workspace.BOOLEAN);
//				typeStep.createPropertyType(CoreProperties.overrideReason.toString(), Cardinality.SINGLE, Workspace.STRING);
//				return typeStep;
//			}
//	}

//	public static ConstraintWrapper getInstance(Workspace ws, ConstraintSpec qaSpec, ZonedDateTime lastChanged, ProcessStep owningStep, ProcessInstance proc) {
//		Instance inst = ws.createInstance(getOrCreateDesignSpaceInstanceType(ws), qaSpec.getName()+proc.getName()+"_"+UUID.randomUUID());
//		ConstraintWrapper cw = context.getWrappedInstance(ConstraintWrapper.class, inst);
//		cw.setSingleProperty(CoreProperties.parentStep.toString(), owningStep.getInstance());
//		cw.setSpec(qaSpec);
//		cw.setLastChanged(lastChanged);
//		cw.setProcess(proc);
//		cw.setOverrideReason("");
//		cw.setIsOverriden(false);
//		return cw;
//	}

}
