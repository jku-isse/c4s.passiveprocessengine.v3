package at.jku.isse.passiveprocessengine.definition.factories;

import at.jku.isse.passiveprocessengine.configurability.ProcessConfigBaseElementFactory;
import lombok.Data;

@Data
public class DefinitionFactoryIndex {

	final ConstraintSpecFactory constraintFactory;
	final DecisionNodeDefinitionFactory decisionNodeDefinitionFactory;
	final MappingDefinitionFactory mappingDefinitionFactory;
	final ProcessDefinitionFactory processDefinitionFactory;
	final StepDefinitionFactory stepDefinitionFactory;
	final ProcessConfigBaseElementFactory processConfigFactory;
	final DefinitionTransformerFactory definitionTransformerFactory;
	
}
