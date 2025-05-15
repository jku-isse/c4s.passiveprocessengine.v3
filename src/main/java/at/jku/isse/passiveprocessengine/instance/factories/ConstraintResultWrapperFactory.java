package at.jku.isse.passiveprocessengine.instance.factories;

import java.time.ZonedDateTime;

import at.jku.isse.passiveprocessengine.core.FactoryIndex.DomainFactory;
import at.jku.isse.passiveprocessengine.rdfwrapper.RDFInstance;
import at.jku.isse.passiveprocessengine.rdfwrapper.rule.RuleEnabledResolver;
import at.jku.isse.passiveprocessengine.definition.activeobjects.ConstraintSpec;
import at.jku.isse.passiveprocessengine.instance.activeobjects.ConstraintResultWrapper;
import at.jku.isse.passiveprocessengine.instance.activeobjects.ProcessInstance;
import at.jku.isse.passiveprocessengine.instance.activeobjects.ProcessStep;
import at.jku.isse.passiveprocessengine.instance.types.ConstraintWrapperType;

public class ConstraintResultWrapperFactory extends DomainFactory {
	
	public ConstraintResultWrapperFactory(RuleEnabledResolver context) {
		super(context);		
	}

	/**
	 * assuming that process has unique name across all processes and qaSpec has unique name within that process' definition
	 * */
	public static String generateId(ConstraintSpec qaSpec, ProcessInstance proc) {
		return qaSpec.getName()+"-"+proc.getName(); 
	}
	
	public ConstraintResultWrapper createInstance(ConstraintSpec qaSpec, ZonedDateTime lastChanged, ProcessStep owningStep, ProcessInstance proc) {
		var id = generateId(qaSpec, proc);
		RDFInstance inst = getContext().createInstance(id, getContext().findNonDeletedInstanceTypeByFQN(ConstraintWrapperType.typeId).get());
		ConstraintResultWrapper cw = (ConstraintResultWrapper)inst;
		cw.setSingleProperty(ConstraintWrapperType.CoreProperties.parentStep.toString(), owningStep.getInstance());
		cw.setSpec(qaSpec);
		cw.setLastChanged(lastChanged);
		cw.setProcess(proc);
		cw.setOverrideReason("");
		cw.setIsOverriden(false);
		return cw;
	}
}
