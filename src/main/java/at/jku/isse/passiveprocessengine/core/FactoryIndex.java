package at.jku.isse.passiveprocessengine.core;

import at.jku.isse.passiveprocessengine.definition.factories.ConstraintSpecFactory;
import at.jku.isse.passiveprocessengine.definition.factories.DecisionNodeDefinitionFactory;
import at.jku.isse.passiveprocessengine.definition.factories.MappingDefinitionFactory;
import at.jku.isse.passiveprocessengine.definition.factories.ProcessDefinitionFactory;
import at.jku.isse.passiveprocessengine.definition.factories.StepDefinitionFactory;
import at.jku.isse.passiveprocessengine.designspace.RuleServiceWrapper;
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
	final RuleDefinitionFactory ruleDefinitionFactory;
	
	
	/**
	 * 
	 * @param context
	 * @return FactoryIndex with new set of factory instances, 
	 * if possible, ensure to build only once, to reuse Factories (multiple instances of same factory are ok, but not efficient)
	 */
	public static FactoryIndex build(ProcessContext context, RuleServiceWrapper ruleService, RuleDefinitionFactory ruleDefinitionFactory) {
		FactoryIndex index = new FactoryIndex(
				new ConstraintResultWrapperFactory(context),
				new DecisionNodeInstanceFactory(context),
				new ProcessInstanceFactory(context),
				new ProcessStepInstanceFactory(context),
				
				new ConstraintSpecFactory(context),
				new DecisionNodeDefinitionFactory(context),
				new MappingDefinitionFactory(context),
				new ProcessDefinitionFactory(context, ruleService),
				new StepDefinitionFactory(context),
				ruleDefinitionFactory
				);
		return index;
	}
	
	@Data
	public static abstract class DomainFactory {
		
		final ProcessContext context;			

	}
}
