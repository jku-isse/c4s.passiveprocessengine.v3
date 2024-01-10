package at.jku.isse.passiveprocessengine.instance.factories;

import java.time.ZonedDateTime;
import java.util.UUID;

import at.jku.isse.passiveprocessengine.Context;
import at.jku.isse.passiveprocessengine.core.Instance;
import at.jku.isse.passiveprocessengine.core.InstanceRepository;
import at.jku.isse.passiveprocessengine.definition.activeobjects.ConstraintSpec;
import at.jku.isse.passiveprocessengine.definition.types.ProcessDomainTypesRegistry;
import at.jku.isse.passiveprocessengine.instance.activeobjects.ConstraintWrapper;
import at.jku.isse.passiveprocessengine.instance.activeobjects.ProcessInstance;
import at.jku.isse.passiveprocessengine.instance.activeobjects.ProcessStep;
import at.jku.isse.passiveprocessengine.instance.factories.FactoryIndex.DomainInstanceFactory;
import at.jku.isse.passiveprocessengine.instance.types.ConstraintWrapperType;

public class ConstraintWrapperFactory extends DomainInstanceFactory {
	
	public ConstraintWrapperFactory(InstanceRepository repository, Context context,
			ProcessDomainTypesRegistry typesFactory) {
		super(repository, context, typesFactory);		
	}

	public ConstraintWrapper createInstance(ConstraintSpec qaSpec, ZonedDateTime lastChanged, ProcessStep owningStep, ProcessInstance proc) {
		Instance inst = repository.createInstance(qaSpec.getName()+proc.getName()+"_"+UUID.randomUUID(), typesFactory.getType(ConstraintWrapper.class));
		ConstraintWrapper cw = context.getWrappedInstance(ConstraintWrapper.class, inst);
		cw.getInstance().setSingleProperty(ConstraintWrapperType.CoreProperties.parentStep.toString(), owningStep.getInstance());
		cw.setSpec(qaSpec);
		cw.setLastChanged(lastChanged);
		cw.setProcess(proc);
		cw.setOverrideReason("");
		cw.setIsOverriden(false);
		return cw;
	}
}
