package at.jku.isse.passiveprocessengine.definition.factories;

import at.jku.isse.passiveprocessengine.core.FactoryIndex.DomainFactory;
import at.jku.isse.passiveprocessengine.rdfwrapper.RDFInstance;
import at.jku.isse.passiveprocessengine.core.ProcessContext;
import at.jku.isse.passiveprocessengine.definition.activeobjects.DecisionNodeDefinition;
import at.jku.isse.passiveprocessengine.definition.activeobjects.DecisionNodeDefinition.InFlowType;
import at.jku.isse.passiveprocessengine.definition.types.DecisionNodeDefinitionType;

public class DecisionNodeDefinitionFactory extends DomainFactory {
	
	public DecisionNodeDefinitionFactory(ProcessContext context) {
		super(context);
	}

	public DecisionNodeDefinition createInstance(String dndId) {
		RDFInstance instance = getContext().getInstanceRepository().createInstance(dndId, getContext().getSchemaRegistry().getTypeByName(DecisionNodeDefinitionType.typeId));
		// default SEQ
		instance.setSingleProperty(DecisionNodeDefinitionType.CoreProperties.inFlowType.toString(), InFlowType.SEQ.toString());
		instance.setSingleProperty(DecisionNodeDefinitionType.CoreProperties.hierarchyDepth.toString(), -1);
		return getContext().getWrappedInstance(DecisionNodeDefinition.class, instance);
	}
	
	
}
