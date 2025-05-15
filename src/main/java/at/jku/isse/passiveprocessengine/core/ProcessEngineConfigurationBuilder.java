package at.jku.isse.passiveprocessengine.core;

import at.jku.isse.passiveprocessengine.definition.types.ConstraintSpecTypeFactory;
import at.jku.isse.passiveprocessengine.definition.types.DecisionNodeDefinitionTypeFactory;
import at.jku.isse.passiveprocessengine.definition.types.MappingDefinitionTypeFactory;
import at.jku.isse.passiveprocessengine.definition.types.ProcessDefinitionScopeType;
import at.jku.isse.passiveprocessengine.definition.types.ProcessDefinitionType;
import at.jku.isse.passiveprocessengine.definition.types.StepDefinitionTypeFactory;
import at.jku.isse.passiveprocessengine.designspace.RewriterFactory;
import at.jku.isse.passiveprocessengine.instance.InputToOutputMapper;
import at.jku.isse.passiveprocessengine.instance.types.AbstractProcessInstanceType;
import at.jku.isse.passiveprocessengine.instance.types.AbstractProcessStepType;
import at.jku.isse.passiveprocessengine.instance.types.ConstraintWrapperType;
import at.jku.isse.passiveprocessengine.instance.types.DecisionNodeInstanceType;
import at.jku.isse.passiveprocessengine.instance.types.ProcessConfigBaseElementType;
import at.jku.isse.passiveprocessengine.instance.types.ProcessInstanceScopeType;
import at.jku.isse.passiveprocessengine.rdfwrapper.CoreTypeFactory;
import at.jku.isse.passiveprocessengine.rdfwrapper.rule.RuleDefinitionService;
import at.jku.isse.passiveprocessengine.rdfwrapper.rule.RuleEnabledResolver;
import lombok.Getter;

@Getter
public class ProcessEngineConfigurationBuilder {

	final InstanceRepository instanceRepository;
	final RuleEnabledResolver schemaRegistry;
	final CoreTypeFactory coreTypeFactory;
	
	protected RuleEnabledResolver context;
	protected InputToOutputMapper ioMapper;
	
	public ProcessEngineConfigurationBuilder(RuleEnabledResolver schemaRegistry
			, InstanceRepository instanceRepository
			, RepairTreeProvider ruleService
			, RewriterFactory rewriterFactory
			, RuleDefinitionService ruleFactory
			, CoreTypeFactory coreTypeFactory
			, RuleAnalysisService ruleAnalysisService) {
		this.instanceRepository = instanceRepository;
		this.schemaRegistry = schemaRegistry;
		this.coreTypeFactory = coreTypeFactory;
		initSchemaRegistry();
		ioMapper = new InputToOutputMapper(ruleService);
		initContext(rewriterFactory, ruleFactory, ruleService, ruleAnalysisService);
	}
	
	private void initSchemaRegistry() {
		registerAllDefinitionTypes();
		registerAllInstanceBaseTypes();
	}
	
	private void registerAllDefinitionTypes() {
		ProcessDefinitionScopeType scopeTypeProvider = new ProcessDefinitionScopeType(schemaRegistry);		
		ConstraintSpecTypeFactory specTypeProvider = new ConstraintSpecTypeFactory(schemaRegistry);		
		MappingDefinitionTypeFactory mapTypeProvider = new MappingDefinitionTypeFactory(schemaRegistry);		
		DecisionNodeDefinitionTypeFactory dndTypeProvider = new DecisionNodeDefinitionTypeFactory(schemaRegistry);		
		StepDefinitionTypeFactory stepTypeProvider = new StepDefinitionTypeFactory(schemaRegistry);		
		ProcessDefinitionType processTypeProvider = new ProcessDefinitionType(schemaRegistry);
		scopeTypeProvider.produceTypeProperties();
		specTypeProvider.produceTypeProperties();
		mapTypeProvider.produceTypeProperties();
		dndTypeProvider.produceTypeProperties();
		stepTypeProvider.produceTypeProperties();
		processTypeProvider.produceTypeProperties();
				
		coreTypeFactory.getBaseArtifactType(); // ensure base type exists
	}
	
	private void registerAllInstanceBaseTypes() {
		ProcessInstanceScopeType scopeTypeProvider = new ProcessInstanceScopeType(schemaRegistry);
		ProcessConfigBaseElementType configTypeProvider = new ProcessConfigBaseElementType(schemaRegistry);
		ConstraintWrapperType constraintWrapperType = new ConstraintWrapperType(schemaRegistry);
		DecisionNodeInstanceType dniType = new DecisionNodeInstanceType(schemaRegistry);
		AbstractProcessStepType stepType = new AbstractProcessStepType(schemaRegistry);
		AbstractProcessInstanceType processType = new AbstractProcessInstanceType(schemaRegistry);
		scopeTypeProvider.produceTypeProperties();
		constraintWrapperType.produceTypeProperties();
		dniType.produceTypeProperties();
		stepType.produceTypeProperties();
		processType.produceTypeProperties();
		configTypeProvider.produceTypeProperties();
	}
	
	private void initContext(RewriterFactory rewriterFactory, RuleDefinitionService ruleFactory, RepairTreeProvider repairTreeProvider, RuleAnalysisService ruleAnalysisService) {
		context = new RuleEnabledResolver(instanceRepository, schemaRegistry, ioMapper, repairTreeProvider, ruleAnalysisService);
		context.inject(FactoryIndex.build(context, rewriterFactory, ruleFactory));
	}
	
}
