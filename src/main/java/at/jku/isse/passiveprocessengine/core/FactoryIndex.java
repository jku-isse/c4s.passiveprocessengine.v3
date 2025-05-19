package at.jku.isse.passiveprocessengine.core;

import at.jku.isse.passiveprocessengine.definition.factories.SpecificProcessInstanceTypesFactory;
import at.jku.isse.passiveprocessengine.definition.types.ConstraintSpecTypeFactory;
import at.jku.isse.passiveprocessengine.definition.types.DecisionNodeDefinitionTypeFactory;
import at.jku.isse.passiveprocessengine.definition.types.MappingDefinitionTypeFactory;
import at.jku.isse.passiveprocessengine.definition.types.ProcessDefinitionTypeFactory;
import at.jku.isse.passiveprocessengine.definition.types.StepDefinitionTypeFactory;
import at.jku.isse.passiveprocessengine.designspace.RewriterFactory;
import at.jku.isse.passiveprocessengine.instance.factories.ConstraintResultWrapperFactory;
import at.jku.isse.passiveprocessengine.instance.factories.DecisionNodeInstanceFactory;
import at.jku.isse.passiveprocessengine.instance.factories.ProcessConfigFactory;
import at.jku.isse.passiveprocessengine.instance.factories.ProcessInstanceFactory;
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
	
	final ConstraintSpecTypeFactory constraintFactory;
	final DecisionNodeDefinitionTypeFactory decisionNodeDefinitionFactory;
	final MappingDefinitionTypeFactory mappingDefinitionFactory;
	final SpecificProcessInstanceTypesFactory specificProcessDefinitionFactory;
	final StepDefinitionTypeFactory stepDefinitionFactory;	
	final ProcessDefinitionTypeFactory processDefinitionFactory;
	final ProcessConfigFactory processConfigFactory;
	
	
	/**
	 * 
	 * @param context
	 * @return FactoryIndex with new set of factory instances, 
	 * if possible, ensure to build only once, to reuse Factories (multiple instances of same factory are ok, but not efficient)
	 */
	public static FactoryIndex build(RuleEnabledResolver context, RewriterFactory ruleRewriterFactory) {
		var crwFactory = new ConstraintResultWrapperFactory(context);
		var dndFactory = new DecisionNodeDefinitionTypeFactory(context);
		var dniFactory = new DecisionNodeInstanceFactory(context);
		return new FactoryIndex(
				crwFactory,
				dniFactory,
				new ProcessInstanceFactory(context, dniFactory, crwFactory),				
				new ConstraintSpecTypeFactory(context),
				dndFactory,
				new MappingDefinitionTypeFactory(context),
				new SpecificProcessInstanceTypesFactory(context, ruleRewriterFactory),
				new StepDefinitionTypeFactory(context),
				new ProcessDefinitionTypeFactory(context),
				new ProcessConfigFactory(context)
				);
	}
	
	@RequiredArgsConstructor
	@Getter
	public abstract static class DomainFactory {
		
		final RuleEnabledResolver context;			

	}
}
