package at.jku.isse.passiveprocessengine.core;

import at.jku.isse.passiveprocessengine.definition.factories.SpecificProcessInstanceTypesFactory;
import at.jku.isse.passiveprocessengine.definition.types.ConstraintSpecTypeFactory;
import at.jku.isse.passiveprocessengine.definition.types.DecisionNodeDefinitionTypeFactory;
import at.jku.isse.passiveprocessengine.definition.types.MappingDefinitionTypeFactory;
import at.jku.isse.passiveprocessengine.definition.types.ProcessDefinitionScopeTypeFactory;
import at.jku.isse.passiveprocessengine.definition.types.ProcessDefinitionTypeFactory;
import at.jku.isse.passiveprocessengine.definition.types.StepDefinitionTypeFactory;
import at.jku.isse.passiveprocessengine.instance.factories.ProcessInstanceFactory;
import at.jku.isse.passiveprocessengine.instance.types.AbstractProcessInstanceType;
import at.jku.isse.passiveprocessengine.instance.types.AbstractProcessStepType;
import at.jku.isse.passiveprocessengine.instance.types.ConstraintResultWrapperTypeFactory;
import at.jku.isse.passiveprocessengine.instance.types.DecisionNodeInstanceTypeFactory;
import at.jku.isse.passiveprocessengine.instance.types.ProcessConfigBaseElementTypeFactory;
import at.jku.isse.passiveprocessengine.instance.types.ProcessInstanceScopeTypeFactory;
import at.jku.isse.passiveprocessengine.rdfwrapper.CoreTypeFactory;
import at.jku.isse.passiveprocessengine.rdfwrapper.rule.RuleEnabledResolver;
import at.jku.isse.passiveprocessengine.rules.RewriterFactory;
import lombok.Getter;

@Getter
public class ProcessEngineConfigurationBuilder {

	final RuleEnabledResolver schemaRegistry;
	final CoreTypeFactory coreTypeFactory;
	FactoryIndex factoryIndex;
	
	public ProcessEngineConfigurationBuilder(RuleEnabledResolver schemaRegistry
			, CoreTypeFactory coreTypeFactory) {
		this.coreTypeFactory = coreTypeFactory;
		this.schemaRegistry = schemaRegistry;
		initSchemaRegistry();
	}
	
	private void initSchemaRegistry() {
		
		ProcessDefinitionScopeTypeFactory scopeTypeProvider = new ProcessDefinitionScopeTypeFactory(schemaRegistry);		
		ConstraintSpecTypeFactory specTypeProvider = new ConstraintSpecTypeFactory(schemaRegistry);		
		MappingDefinitionTypeFactory mapTypeProvider = new MappingDefinitionTypeFactory(schemaRegistry);		
		DecisionNodeDefinitionTypeFactory dndTypeProvider = new DecisionNodeDefinitionTypeFactory(schemaRegistry);		
		StepDefinitionTypeFactory stepTypeProvider = new StepDefinitionTypeFactory(schemaRegistry);		
		ProcessDefinitionTypeFactory processTypeProvider = new ProcessDefinitionTypeFactory(schemaRegistry, stepTypeProvider, dndTypeProvider);		
		scopeTypeProvider.produceTypeProperties();
		specTypeProvider.produceTypeProperties();
		mapTypeProvider.produceTypeProperties();
		dndTypeProvider.produceTypeProperties();
		stepTypeProvider.produceTypeProperties();
		processTypeProvider.produceTypeProperties();
		coreTypeFactory.getBaseArtifactType(); // ensure base type exists
	
		ProcessInstanceScopeTypeFactory scopeInstTypeProvider = new ProcessInstanceScopeTypeFactory(schemaRegistry);
		ProcessConfigBaseElementTypeFactory configTypeProvider = new ProcessConfigBaseElementTypeFactory(schemaRegistry);
		ConstraintResultWrapperTypeFactory constraintWrapperType = new ConstraintResultWrapperTypeFactory(schemaRegistry);
		DecisionNodeInstanceTypeFactory dniType = new DecisionNodeInstanceTypeFactory(schemaRegistry);
		AbstractProcessStepType stepType = new AbstractProcessStepType(schemaRegistry);
		AbstractProcessInstanceType processType = new AbstractProcessInstanceType(schemaRegistry);
		scopeInstTypeProvider.produceTypeProperties();
		constraintWrapperType.produceTypeProperties(scopeInstTypeProvider);
		dniType.produceTypeProperties(scopeInstTypeProvider);
		stepType.produceTypeProperties();
		processType.produceTypeProperties();
		configTypeProvider.produceTypeProperties(scopeInstTypeProvider);
		
		var processInstanceFactory = new ProcessInstanceFactory(schemaRegistry, dniType, constraintWrapperType);
		var processFactory = new SpecificProcessInstanceTypesFactory(schemaRegistry, new RewriterFactory(schemaRegistry, schemaRegistry.getRuleSchema()), scopeInstTypeProvider);
		
		factoryIndex = new FactoryIndex(specTypeProvider, dndTypeProvider, mapTypeProvider, stepTypeProvider, processTypeProvider
				, configTypeProvider, processFactory, constraintWrapperType, dniType, processInstanceFactory, scopeInstTypeProvider);
	}
	
	
}
