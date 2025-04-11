package at.jku.isse.passiveprocessengine.core;

import at.jku.isse.passiveprocessengine.definition.factories.ConstraintSpecFactory;
import at.jku.isse.passiveprocessengine.definition.factories.DecisionNodeDefinitionFactory;
import at.jku.isse.passiveprocessengine.definition.factories.MappingDefinitionFactory;
import at.jku.isse.passiveprocessengine.definition.factories.SpecificProcessInstanceTypesFactory;
import at.jku.isse.passiveprocessengine.definition.factories.StepDefinitionFactory;
import at.jku.isse.passiveprocessengine.designspace.RewriterFactory;
import at.jku.isse.passiveprocessengine.instance.factories.ConstraintResultWrapperFactory;
import at.jku.isse.passiveprocessengine.instance.factories.DecisionNodeInstanceFactory;
import at.jku.isse.passiveprocessengine.instance.factories.ProcessConfigFactory;
import at.jku.isse.passiveprocessengine.instance.factories.ProcessInstanceFactory;
import at.jku.isse.passiveprocessengine.instance.factories.ProcessStepInstanceFactory;
import at.jku.isse.passiveprocessengine.rdfwrapper.rule.RuleDefinitionService;
import at.jku.isse.passiveprocessengine.rdfwrapper.rule.RuleEnabledResolver;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class FactoryIndex {

	final ConstraintResultWrapperFactory constraintResultFactory;
	final DecisionNodeInstanceFactory decisionNodeInstanceFactory;
	final ProcessInstanceFactory processInstanceFactory;
	final ProcessStepInstanceFactory processStepFactory;
	
	final ConstraintSpecFactory constraintFactory;
	final DecisionNodeDefinitionFactory decisionNodeDefinitionFactory;
	final MappingDefinitionFactory mappingDefinitionFactory;
	final SpecificProcessInstanceTypesFactory processDefinitionFactory;
	final StepDefinitionFactory stepDefinitionFactory;	
	
	final RuleDefinitionService ruleDefinitionFactory;
	final ProcessConfigFactory processConfigFactory;
	
	
	/**
	 * 
	 * @param context
	 * @return FactoryIndex with new set of factory instances, 
	 * if possible, ensure to build only once, to reuse Factories (multiple instances of same factory are ok, but not efficient)
	 */
	public static FactoryIndex build(RuleEnabledResolver context, RewriterFactory ruleRewriterFactory, RuleDefinitionService ruleDefinitionService) {
		return new FactoryIndex(
				new ConstraintResultWrapperFactory(context),
				new DecisionNodeInstanceFactory(context),
				new ProcessInstanceFactory(context),
				new ProcessStepInstanceFactory(context),
				
				new ConstraintSpecFactory(context),
				new DecisionNodeDefinitionFactory(context),
				new MappingDefinitionFactory(context),
				new SpecificProcessInstanceTypesFactory(context, ruleRewriterFactory),
				new StepDefinitionFactory(context),
				
				ruleDefinitionService,
				new ProcessConfigFactory(context)
				);
	}
	
	@RequiredArgsConstructor
	@Getter
	public abstract static class DomainFactory {
		
		final RuleEnabledResolver context;			

	}
}
