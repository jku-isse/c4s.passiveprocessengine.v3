package at.jku.isse.passiveprocessengine.core;

import at.jku.isse.passiveprocessengine.Context;
import at.jku.isse.passiveprocessengine.definition.factories.ConstraintSpecFactory;
import at.jku.isse.passiveprocessengine.definition.factories.DecisionNodeDefinitionFactory;
import at.jku.isse.passiveprocessengine.definition.factories.DefinitionTransformerFactory;
import at.jku.isse.passiveprocessengine.definition.factories.MappingDefinitionFactory;
import at.jku.isse.passiveprocessengine.definition.factories.ProcessDefinitionFactory;
import at.jku.isse.passiveprocessengine.definition.factories.StepDefinitionFactory;
import at.jku.isse.passiveprocessengine.instance.factories.ConstraintResultWrapperFactory;
import at.jku.isse.passiveprocessengine.instance.factories.DecisionNodeInstanceFactory;
import at.jku.isse.passiveprocessengine.instance.factories.ProcessInstanceFactory;
import at.jku.isse.passiveprocessengine.instance.factories.ProcessStepInstanceFactory;
import lombok.Data;

@Data
public class FactoryIndex {

	final ConstraintResultWrapperFactory constraintResultFactory;
	final DecisionNodeInstanceFactory decisionNodeInstanceFactory;
	final ProcessInstanceFactory processInstanceFactory;
	final ProcessStepInstanceFactory processStepFactory;
	
	final ConstraintSpecFactory constraintFactory;
	final DecisionNodeDefinitionFactory decisionNodeDefinitionFactory;
	final MappingDefinitionFactory mappingDefinitionFactory;
	final ProcessDefinitionFactory processDefinitionFactory;
	final StepDefinitionFactory stepDefinitionFactory;	
	final DefinitionTransformerFactory definitionTransformerFactory;
	final RuleDefinitionFactory ruleDefinitionFactory;
	
	
	@Data
	public static abstract class DomainFactory {
		protected FactoryIndex factoryIndex;
		
		final InstanceRepository repository;
		final Context context;
		final DomainTypesRegistry typesFactory;				
		
		public void inject(FactoryIndex index) {
			this.factoryIndex = index;
		}
	}
}
