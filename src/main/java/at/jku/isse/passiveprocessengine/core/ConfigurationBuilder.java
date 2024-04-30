package at.jku.isse.passiveprocessengine.core;

import at.jku.isse.passiveprocessengine.definition.types.ConstraintSpecType;
import at.jku.isse.passiveprocessengine.definition.types.DecisionNodeDefinitionType;
import at.jku.isse.passiveprocessengine.definition.types.MappingDefinitionType;
import at.jku.isse.passiveprocessengine.definition.types.ProcessDefinitionScopeType;
import at.jku.isse.passiveprocessengine.definition.types.ProcessDefinitionType;
import at.jku.isse.passiveprocessengine.definition.types.ProcessStepDefinitionType;
import at.jku.isse.passiveprocessengine.designspace.RewriterFactory;
import at.jku.isse.passiveprocessengine.designspace.RuleServiceWrapper;
import at.jku.isse.passiveprocessengine.instance.InputToOutputMapper;
import at.jku.isse.passiveprocessengine.instance.types.AbstractProcessStepType;
import at.jku.isse.passiveprocessengine.instance.types.ConstraintWrapperType;
import at.jku.isse.passiveprocessengine.instance.types.DecisionNodeInstanceType;
import at.jku.isse.passiveprocessengine.instance.types.ProcessConfigBaseElementType;
import at.jku.isse.passiveprocessengine.instance.types.ProcessInstanceScopeType;
import lombok.Getter;

@Getter
public class ConfigurationBuilder {

	final InstanceRepository instanceRepository;
	final SchemaRegistry schemaRegistry;
	
	protected ProcessContext context;
	protected InputToOutputMapper ioMapper;
	
	public ConfigurationBuilder(SchemaRegistry schemaRegistry
			, InstanceRepository instanceRepository
			, RuleServiceWrapper ruleService
			, RewriterFactory rewriterFactory
			, RuleDefinitionFactory ruleFactory) {
		this.instanceRepository = instanceRepository;
		this.schemaRegistry = schemaRegistry;
		initSchemaRegistry();
		ioMapper = new InputToOutputMapper(ruleService);
		initContext(rewriterFactory, ruleFactory);
	}
	
	private void initSchemaRegistry() {
		registerAllDefinitionTypes();
		registerAllInstanceBaseTypes();
	}
	
	private void registerAllDefinitionTypes() {
		ProcessDefinitionScopeType scopeTypeProvider = new ProcessDefinitionScopeType(schemaRegistry);		
		ConstraintSpecType specTypeProvider = new ConstraintSpecType(schemaRegistry);		
		MappingDefinitionType mapTypeProvider = new MappingDefinitionType(schemaRegistry);		
		DecisionNodeDefinitionType dndTypeProvider = new DecisionNodeDefinitionType(schemaRegistry);		
		ProcessStepDefinitionType stepTypeProvider = new ProcessStepDefinitionType(schemaRegistry);		
		ProcessDefinitionType processTypeProvider = new ProcessDefinitionType(schemaRegistry);
		scopeTypeProvider.produceTypeProperties();
		specTypeProvider.produceTypeProperties();
		mapTypeProvider.produceTypeProperties();
		dndTypeProvider.produceTypeProperties();
		processTypeProvider.produceTypeProperties();
		stepTypeProvider.produceTypeProperties();		
		
	}
	
	private void registerAllInstanceBaseTypes() {
		ProcessInstanceScopeType scopeTypeProvider = new ProcessInstanceScopeType(schemaRegistry);
		ProcessConfigBaseElementType configTypeProvider = new ProcessConfigBaseElementType(schemaRegistry);
		ConstraintWrapperType constraintWrapperType = new ConstraintWrapperType(schemaRegistry);
		DecisionNodeInstanceType dniType = new DecisionNodeInstanceType(schemaRegistry);
		AbstractProcessStepType stepType = new AbstractProcessStepType(schemaRegistry);
		scopeTypeProvider.produceTypeProperties();
		constraintWrapperType.produceTypeProperties();
		dniType.produceTypeProperties();
		stepType.produceTypeProperties();
		configTypeProvider.produceTypeProperties();
	}
	
	private void initContext(RewriterFactory rewriterFactory, RuleDefinitionFactory ruleFactory) {
		context = new ProcessContext(instanceRepository, schemaRegistry, ioMapper);
		context.inject(FactoryIndex.build(context, rewriterFactory, ruleFactory));
	}
	
}
