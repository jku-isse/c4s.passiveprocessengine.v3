package at.jku.isse.passiveprocessengine.core;

import at.jku.isse.passiveprocessengine.definition.factories.SpecificProcessInstanceTypesFactory;
import at.jku.isse.passiveprocessengine.definition.types.ConstraintSpecTypeFactory;
import at.jku.isse.passiveprocessengine.definition.types.DecisionNodeDefinitionTypeFactory;
import at.jku.isse.passiveprocessengine.definition.types.MappingDefinitionTypeFactory;
import at.jku.isse.passiveprocessengine.definition.types.ProcessDefinitionTypeFactory;
import at.jku.isse.passiveprocessengine.definition.types.StepDefinitionTypeFactory;
import at.jku.isse.passiveprocessengine.instance.types.DecisionNodeInstanceTypeFactory;
import at.jku.isse.passiveprocessengine.instance.factories.ProcessInstanceFactory;
import at.jku.isse.passiveprocessengine.instance.types.ConstraintResultWrapperTypeFactory;
import at.jku.isse.passiveprocessengine.instance.types.ProcessConfigBaseElementTypeFactory;
import at.jku.isse.passiveprocessengine.instance.types.ProcessInstanceScopeTypeFactory;
import at.jku.isse.passiveprocessengine.rdfwrapper.rule.RuleEnabledResolver;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class FactoryIndex {

	final ConstraintSpecTypeFactory constraintFactory;
	final DecisionNodeDefinitionTypeFactory decisionNodeDefinitionFactory;
	final MappingDefinitionTypeFactory mappingDefinitionFactory;
	final StepDefinitionTypeFactory stepDefinitionFactory;	
	final ProcessDefinitionTypeFactory processDefinitionFactory;
	
	final ProcessConfigBaseElementTypeFactory processConfigFactory;
	final SpecificProcessInstanceTypesFactory specificProcessInstanceFactory;
	final ConstraintResultWrapperTypeFactory constraintResultFactory;
	final DecisionNodeInstanceTypeFactory decisionNodeInstanceFactory;
	final ProcessInstanceFactory processInstanceFactory;	
	final ProcessInstanceScopeTypeFactory instanceScopeFactory;
	
	@RequiredArgsConstructor
	@Getter
	public abstract static class DomainFactory {
		
		final RuleEnabledResolver context;			

	}
}
