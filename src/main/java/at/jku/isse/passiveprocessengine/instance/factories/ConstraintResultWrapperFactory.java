package at.jku.isse.passiveprocessengine.instance.factories;

import java.time.ZonedDateTime;
import java.util.UUID;

import at.jku.isse.passiveprocessengine.Context;
import at.jku.isse.passiveprocessengine.core.FactoryIndex.DomainFactory;
import at.jku.isse.passiveprocessengine.core.Instance;
import at.jku.isse.passiveprocessengine.definition.activeobjects.ConstraintSpec;
import at.jku.isse.passiveprocessengine.instance.activeobjects.ConstraintResultWrapper;
import at.jku.isse.passiveprocessengine.instance.activeobjects.ProcessInstance;
import at.jku.isse.passiveprocessengine.instance.activeobjects.ProcessStep;
import at.jku.isse.passiveprocessengine.instance.types.ConstraintWrapperType;

public class ConstraintResultWrapperFactory extends DomainFactory {
	
	public ConstraintResultWrapperFactory(Context context) {
		super(context);		
	}

	public ConstraintResultWrapper createInstance(ConstraintSpec qaSpec, ZonedDateTime lastChanged, ProcessStep owningStep, ProcessInstance proc) {
		Instance inst = getContext().getInstanceRepository().createInstance(qaSpec.getName()+proc.getName()+"_"+UUID.randomUUID()
			, getContext().getSchemaRegistry().getType(ConstraintResultWrapper.class));
		ConstraintResultWrapper cw = getContext().getWrappedInstance(ConstraintResultWrapper.class, inst);
		cw.getInstance().setSingleProperty(ConstraintWrapperType.CoreProperties.parentStep.toString(), owningStep.getInstance());
		cw.setSpec(qaSpec);
		cw.setLastChanged(lastChanged);
		cw.setProcess(proc);
		cw.setOverrideReason("");
		cw.setIsOverriden(false);
		return cw;
	}
}
