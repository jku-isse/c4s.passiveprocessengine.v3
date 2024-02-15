package at.jku.isse.passiveprocessengine.definition.factories;

import at.jku.isse.passiveprocessengine.Context;
import at.jku.isse.passiveprocessengine.core.FactoryIndex.DomainFactory;
import at.jku.isse.passiveprocessengine.core.Instance;
import at.jku.isse.passiveprocessengine.definition.activeobjects.DecisionNodeDefinition;
import at.jku.isse.passiveprocessengine.definition.activeobjects.DecisionNodeDefinition.InFlowType;
import at.jku.isse.passiveprocessengine.definition.types.DecisionNodeDefinitionType;

public class DecisionNodeDefinitionFactory extends DomainFactory {
	
	public DecisionNodeDefinitionFactory(Context context) {
		super(context);
	}

	public DecisionNodeDefinition createInstance(String dndId) {
		Instance instance = getContext().getInstanceRepository().createInstance(dndId, getContext().getSchemaRegistry().getType(DecisionNodeDefinition.class));
		// default SEQ
		instance.setSingleProperty(DecisionNodeDefinitionType.CoreProperties.inFlowType.toString(), InFlowType.SEQ.toString());
		instance.setSingleProperty(DecisionNodeDefinitionType.CoreProperties.hierarchyDepth.toString(), -1);
		return getContext().getWrappedInstance(DecisionNodeDefinition.class, instance);
	}
	
	
}
