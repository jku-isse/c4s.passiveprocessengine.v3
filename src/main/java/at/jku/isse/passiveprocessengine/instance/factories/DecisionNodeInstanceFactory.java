package at.jku.isse.passiveprocessengine.instance.factories;

import java.util.UUID;

import at.jku.isse.passiveprocessengine.core.FactoryIndex.DomainFactory;
import at.jku.isse.passiveprocessengine.rdfwrapper.RDFInstance;
import at.jku.isse.passiveprocessengine.rdfwrapper.rule.RuleEnabledResolver;
import at.jku.isse.passiveprocessengine.definition.activeobjects.DecisionNodeDefinition;
import at.jku.isse.passiveprocessengine.instance.activeobjects.DecisionNodeInstance;
import at.jku.isse.passiveprocessengine.instance.types.DecisionNodeInstanceType;

public class DecisionNodeInstanceFactory extends DomainFactory {

	
	public DecisionNodeInstanceFactory(RuleEnabledResolver context) {
		super(context);		
	}

	public DecisionNodeInstance getInstance(DecisionNodeDefinition dnd) {				
		RDFInstance instance = getContext().createInstance(dnd.getName()+"_"+UUID.randomUUID()
			, getContext().findNonDeletedInstanceTypeByFQN(DecisionNodeInstanceType.typeId).get());
		DecisionNodeInstance dni = (DecisionNodeInstance) instance;

		instance.setSingleProperty(DecisionNodeInstanceType.CoreProperties.dnd.toString(),dnd.getInstance());
		instance.setSingleProperty(DecisionNodeInstanceType.CoreProperties.hasPropagated.toString(),false);
		instance.setSingleProperty(DecisionNodeInstanceType.CoreProperties.isInflowFulfilled.toString(), false);
		return dni;
	}

}
