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
import at.jku.isse.passiveprocessengine.definition.QAConstraintSpec;

//@Data
public class ConstraintWrapper extends ProcessInstanceScopedElement {

	
	public static final String designspaceTypeId = ConstraintWrapper.class.getSimpleName();
	static enum CoreProperties {qaSpec, lastChanged, crule, parentStep};
	
	ZonedDateTime lastChanged;
	
//	ConsistencyRule cr;
//	Boolean evalResult = false;
//	QAConstraintSpec qaSpec;
//
//	protected void setCrIfEmpty(ConsistencyRule cr) {
//	this.cr = cr;
//	this.evalResult = cr.isConsistent();
// }
	
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
		ConsistencyRule cr = getCr();
		if (cr == null)
			return false;
		else 
			return cr.isConsistent();
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

	public QAConstraintSpec getQaSpec() {
		Instance qainst = instance.getPropertyAsInstance(CoreProperties.qaSpec.toString());
		return WrapperCache.getWrappedInstance(QAConstraintSpec.class, qainst);
	}

	private void setQaSpec(QAConstraintSpec qaSpec) {
		instance.getPropertyAsSingle(CoreProperties.qaSpec.toString()).set(qaSpec.getInstance());
	}
	
	public ProcessStep getParentStep() {
		Instance step = instance.getPropertyAsInstance(CoreProperties.parentStep.toString());
		if (step != null)
			return WrapperCache.getWrappedInstance(ProcessStep.class, step);
		else 
			return null;
	}
	
	public void deleteCascading() {
		super.deleteCascading();
	}
	
	public static InstanceType getOrCreateDesignSpaceInstanceType(Workspace ws){
//		Optional<InstanceType> thisType = ws.debugInstanceTypes().stream()
//				.filter(it -> it.name().equals(designspaceTypeId))
//				.findAny();
		Optional<InstanceType> thisType = Optional.ofNullable(ws.TYPES_FOLDER.instanceTypeWithName(designspaceTypeId));
			if (thisType.isPresent())
				return thisType.get();
			else {
				InstanceType typeStep = ws.createInstanceType(designspaceTypeId, ws.TYPES_FOLDER, ProcessInstanceScopedElement.getOrCreateDesignSpaceCoreSchema(ws));
				typeStep.createPropertyType(CoreProperties.qaSpec.toString(), Cardinality.SINGLE, QAConstraintSpec.getOrCreateDesignSpaceCoreSchema(ws));
				//typeStep.createPropertyType(CoreProperties.result.toString(), Cardinality.SINGLE, Workspace.BOOLEAN);
				typeStep.createPropertyType(CoreProperties.lastChanged.toString(), Cardinality.SINGLE, Workspace.STRING);
				typeStep.createPropertyType(CoreProperties.crule.toString(), Cardinality.SINGLE, ws.its(ConsistencyRuleType.CONSISTENCY_RULE_TYPE));
				return typeStep;
			}
	}
	
	public static ConstraintWrapper getInstance(Workspace ws, QAConstraintSpec qaSpec, ZonedDateTime lastChanged, ProcessInstance proc) {
		Instance inst = ws.createInstance(getOrCreateDesignSpaceInstanceType(ws), qaSpec.getId()+proc.getName()+"_"+UUID.randomUUID());
		ConstraintWrapper cw = WrapperCache.getWrappedInstance(ConstraintWrapper.class, inst);
		//cw.setCrIfEmpty(cr);
		cw.setQaSpec(qaSpec);
		//cw.setEvalResult(evalResult);
		cw.setLastChanged(lastChanged);
		cw.setProcess(proc);
		return cw;
	}



	@Override
	public ProcessDefinitionScopedElement getDefinition() {
		return getQaSpec();
	}




	
}
