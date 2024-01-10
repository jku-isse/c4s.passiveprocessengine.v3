package at.jku.isse.passiveprocessengine.instance.factories;

import java.util.UUID;

import at.jku.isse.passiveprocessengine.Context;
import at.jku.isse.passiveprocessengine.core.Instance;
import at.jku.isse.passiveprocessengine.core.InstanceRepository;
import at.jku.isse.passiveprocessengine.definition.activeobjects.DecisionNodeDefinition;
import at.jku.isse.passiveprocessengine.definition.types.ProcessDomainTypesRegistry;
import at.jku.isse.passiveprocessengine.instance.activeobjects.DecisionNodeInstance;
import at.jku.isse.passiveprocessengine.instance.factories.FactoryIndex.DomainInstanceFactory;
import at.jku.isse.passiveprocessengine.instance.types.DecisionNodeInstanceType;

public class DecisionNodeInstanceFactory extends DomainInstanceFactory {

	
	public DecisionNodeInstanceFactory(InstanceRepository repository, Context context,
			ProcessDomainTypesRegistry typesFactory) {
		super(repository, context, typesFactory);		
	}

	public DecisionNodeInstance getInstance(DecisionNodeDefinition dnd) {				
		Instance instance = repository.createInstance(dnd.getName()+"_"+UUID.randomUUID(), typesFactory.getType(DecisionNodeInstance.class));
		DecisionNodeInstance dni = context.getWrappedInstance(DecisionNodeInstance.class, instance);
		//dni.init(dnd);
		instance.setSingleProperty(DecisionNodeInstanceType.CoreProperties.dnd.toString(),dnd.getInstance());
		instance.setSingleProperty(DecisionNodeInstanceType.CoreProperties.hasPropagated.toString(),false);
		instance.setSingleProperty(DecisionNodeInstanceType.CoreProperties.isInflowFulfilled.toString(), false);
		return dni;
	}

//	protected void init(DecisionNodeDefinition dnd) {
//		instance.getPropertyAsSingle(DecisionNodeInstanceType.CoreProperties.dnd.toString()).set(dnd.getInstance());
//		instance.getPropertyAsSingle(DecisionNodeInstanceType.CoreProperties.hasPropagated.toString()).set(false);
//		// if kickoff DN, then set inflow fulfillment to true
//		instance.getPropertyAsSingle(DecisionNodeInstanceType.CoreProperties.isInflowFulfilled.toString()).set(/*dnd.getInSteps().size() == 0 ? true :*/ false);
//
//	}
}
