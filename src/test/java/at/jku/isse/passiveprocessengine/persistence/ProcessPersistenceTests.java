package at.jku.isse.passiveprocessengine.persistence;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import at.jku.isse.passiveprocessengine.rdfwrapper.RDFInstance;
import at.jku.isse.passiveprocessengine.rdfwrapper.RDFInstanceType;
import at.jku.isse.passiveprocessengine.core.ProcessContext;
import at.jku.isse.passiveprocessengine.core.ProcessEngineConfigurationBuilder;
import at.jku.isse.passiveprocessengine.core.RepairTreeProvider;
import at.jku.isse.passiveprocessengine.core.RuleAnalysisService;
import at.jku.isse.passiveprocessengine.core.SchemaRegistry;
import at.jku.isse.artifacteventstreaming.api.Branch;
import at.jku.isse.artifacteventstreaming.api.BranchStateUpdater;
import at.jku.isse.passiveprocessengine.core.InstanceRepository;
import at.jku.isse.passiveprocessengine.definition.activeobjects.ProcessDefinition;
import at.jku.isse.passiveprocessengine.definition.serialization.DTOs;
import at.jku.isse.passiveprocessengine.definition.serialization.DefinitionTransformer;
import at.jku.isse.passiveprocessengine.demo.TestArtifacts;
import at.jku.isse.passiveprocessengine.designspace.RewriterFactory;
import at.jku.isse.passiveprocessengine.demo.TestDTOProcesses;
import at.jku.isse.passiveprocessengine.instance.ProcessInstanceChangeProcessor;
import at.jku.isse.passiveprocessengine.instance.activeobjects.DecisionNodeInstance;
import at.jku.isse.passiveprocessengine.instance.activeobjects.ProcessInstance;
import at.jku.isse.passiveprocessengine.instance.messages.EventDistributor;
import at.jku.isse.passiveprocessengine.instance.messages.Responses.IOResponse;
import at.jku.isse.passiveprocessengine.monitoring.CurrentSystemTimeProvider;
import at.jku.isse.passiveprocessengine.monitoring.ProcessQAStatsMonitor;
import at.jku.isse.passiveprocessengine.rdfwrapper.AbstractionMapper;
import at.jku.isse.passiveprocessengine.rdfwrapper.NodeToDomainResolver;
import at.jku.isse.passiveprocessengine.rdfwrapper.config.RDFWrapperTestSetup;
import at.jku.isse.passiveprocessengine.rdfwrapper.events.ChangeEventTransformer;
import at.jku.isse.passiveprocessengine.rdfwrapper.events.ChangeListener;
import at.jku.isse.passiveprocessengine.rdfwrapper.rule.RuleEvaluationService;
import lombok.NonNull;

public class ProcessPersistenceTests {

	protected RDFWrapperTestSetup dsSetup;
	protected InstanceRepository instanceRepository;
	protected SchemaRegistry schemaReg;
	protected RepairTreeProvider ruleServiceWrapper;
	protected ProcessEngineConfigurationBuilder configBuilder;
	TestDTOProcesses procFactory;	
	TestArtifacts artifactFactory;
		
	RDFInstanceType typeJira;
	ChangeListener picp;
	ProcessQAStatsMonitor monitor;
	Branch branch;
	protected ProcessContext context;
	
	protected void startRead() {
		instanceRepository.startReadTransaction();
	}
	
	protected void startWrite() {
		instanceRepository.startWriteTransaction();
	}
	
	protected void conclude() {
		instanceRepository.concludeTransaction();
	}
	
	public void setup() throws Exception {
		dsSetup = new RDFWrapperTestSetup();
		dsSetup.setupPersistedBranch();
		branch = dsSetup.getBranch();
		this.schemaReg = dsSetup.getSchemaRegistry();
		this.instanceRepository = dsSetup.getInstanceRepository();
		this.ruleServiceWrapper = dsSetup.getRepairTreeProvider();			
		AbstractionMapper designspaceAbstractionMapper = (AbstractionMapper) schemaReg; // ugly as we know this is a DesignSpace in the background
		RuleEvaluationService ruleEvaluationFactory = dsSetup.getRuleEvaluationService(); 
		configBuilder = new ProcessEngineConfigurationBuilder(schemaReg
				, instanceRepository
				, ruleServiceWrapper
				, new RewriterFactory(designspaceAbstractionMapper, false, ((RDFWrapperTestSetup) dsSetup).getRuleSchemaProvider())
				, ruleEvaluationFactory
				, dsSetup.getCoreTypeFactory()
				, (RuleAnalysisService) ruleServiceWrapper);
		System.out.println("Size after process engine configurator build: "+branch.getModel().size());
		EventDistributor eventDistrib = new EventDistributor();
		monitor = new ProcessQAStatsMonitor(new CurrentSystemTimeProvider());
		eventDistrib.registerHandler(monitor);
		context = configBuilder.getContext();
		ChangeListener picp = new ProcessInstanceChangeProcessor(context, eventDistrib);
		ChangeEventTransformer picpWrapper = dsSetup.getChangeEventTransformer();
		picpWrapper.registerWithBranch(picp);
		System.out.println("Size after process engine listeners build: "+branch.getModel().size());
		artifactFactory = new TestArtifacts(instanceRepository, schemaReg);
		procFactory = new TestDTOProcesses(artifactFactory);
		typeJira = artifactFactory.getJiraInstanceType();
		
		
		System.out.println("Size after test artifacts build: "+branch.getModel().size());
		branch.getDataset().commit(); // just to store all default created elements that we dont want to see in a commit FIXME: see if that works
		var stateKeeper = branch.getStateKeeper();
		var unfinishedCommit = ((BranchStateUpdater) stateKeeper).loadState();
		branch.startCommitHandlers(unfinishedCommit);
		//UsageMonitor usageMonitor = new UsageMonitor(new CurrentSystemTimeProvider(), ruleServiceWrapper);
		//ExecutedRepairListenerImpl repairListener = new ExecutedRepairListenerImpl(usageMonitor, configBuilder.getContext());
		//ruleServiceWrapper.register(repairListener);					
	}
	
	protected ProcessDefinition getDefinition(DTOs.Process procDTO) {		
		procDTO.calculateDecisionNodeDepthIndex(1);
		DefinitionTransformer transformer = new DefinitionTransformer(procDTO, configBuilder.getContext().getFactoryIndex(), schemaReg);
		ProcessDefinition procDef = transformer.fromDTO(false);
		assert(procDef != null);
		transformer.getErrors().stream().forEach(err -> System.out.println(err.toString()));		
		assert(transformer.getErrors().isEmpty());		
		return procDef;
	}
	
	protected ProcessInstance instantiateDefaultProcess(DTOs.Process procDTO, RDFInstance... inputs) {
		ProcessDefinition procDef = getDefinition(procDTO);				
		instanceRepository.concludeTransaction();
		instanceRepository.startWriteTransaction();

		ProcessInstance procInstance = configBuilder.getContext().getFactoryIndex().getProcessInstanceFactory().getInstance(procDef, "TEST");
		assert(procInstance != null);
		configBuilder.getContext().getInstanceRepository().concludeTransaction();
		configBuilder.getContext().getInstanceRepository().startWriteTransaction();
		for (RDFInstance input : inputs) {
			IOResponse resp = procInstance.addInput(TestDTOProcesses.JIRA_IN, input);
			assert(resp.getError() == null);
		}
		return procInstance;
	}
	
	protected ProcessInstance instantiateDefaultProcess(@NonNull ProcessDefinition procDef,  RDFInstance... inputs) {		
		ProcessInstance procInstance = configBuilder.getContext().getFactoryIndex().getProcessInstanceFactory().getInstance(procDef, "TEST");
		assert(procInstance != null);
		for (RDFInstance input : inputs) {
			IOResponse resp = procInstance.addInput(TestDTOProcesses.JIRA_IN, input);
			assert(resp.getError() == null);
		}
		return procInstance;
	}

	@Test
	void testCreatingProcess() throws Exception {		
		RDFWrapperTestSetup.resetPersistence(); 
		setup(); // manual as we otherwise cant reset data
		instanceRepository.startWriteTransaction();
		RDFInstance jiraB =  artifactFactory.getJiraInstance("jiraB");
		RDFInstance jiraC = artifactFactory.getJiraInstance("jiraC");		
		RDFInstance jiraA = artifactFactory.getJiraInstance("jiraA", jiraB, jiraC);
						
		ProcessInstance proc =  instantiateDefaultProcess(procFactory.getSimple2StepProcessDefinition(), jiraA);		
		instanceRepository.concludeTransaction();
		instanceRepository.startWriteTransaction();
		proc.printProcessToConsole(" ");
		System.out.println("BranchModel has size: "+branch.getModel().size());
		assertTrue(proc.getProcessSteps().stream()
				.filter(step -> step.getDefinition().getName().equals(TestDTOProcesses.SD1) )
				.allMatch(step -> step.getOutput(TestDTOProcesses.JIRA_OUT).size() == 2));
	}
	
	@Test
	void testLoadingProcess() throws Exception {
		setup(); // manual as we otherwise cant reset data
		instanceRepository.startReadTransaction();
		var types = configBuilder.getSchemaRegistry().getAllNonDeletedInstanceTypes();
		//types.forEach(type -> System.out.println(type.getName()));
		assertFalse(types.isEmpty());		
		
		Model diff = branch.getModel().difference(dsSetup.getLoadedModel());
		RDFDataMgr.write(System.out, diff, Lang.TURTLE) ;
		System.out.println("BranchModel has size: "+branch.getModel().size());
		//RDFDataMgr.write(System.out, branch.getModel(), Lang.TURTLE) ;
		var procRDF = branch.getModel().getIndividual("http://isse.jku.at/artifactstreaming/rdfwrapper#SimpleProc_TEST");
		assertNotNull(procRDF);
		var optProc = instanceRepository.findInstanceById(procRDF.getURI());
		assertTrue(optProc.isPresent());
		var process = (ProcessInstance)configBuilder.getContext().getWrappedInstance(ProcessInstance.class, optProc.get());
		assertNotNull(process);
		process.printProcessToConsole(" ");
		instanceRepository.concludeTransaction();
	}
	
	@Test
	void testLoadingAndChangeProcess() throws Exception {
		setup(); // manual as we otherwise cant reset data
		instanceRepository.startWriteTransaction();
		var types = configBuilder.getSchemaRegistry().getAllNonDeletedInstanceTypes();
		//types.forEach(type -> System.out.println(type.getName()));
		assertFalse(types.isEmpty());		
		
		Model diff = branch.getModel().difference(dsSetup.getLoadedModel());
		RDFDataMgr.write(System.out, diff, Lang.TURTLE) ;
		System.out.println("BranchModel has size: "+branch.getModel().size());
		//RDFDataMgr.write(System.out, branch.getModel(), Lang.TURTLE) ;
		var procRDF = branch.getModel().getIndividual(NodeToDomainResolver.BASE_NS+"SimpleProc_TEST");
		assertNotNull(procRDF);
		var optProc = instanceRepository.findInstanceById(procRDF.getURI());
		assertTrue(optProc.isPresent());
		var process = (ProcessInstance)configBuilder.getContext().getWrappedInstance(ProcessInstance.class, optProc.get());
		assertNotNull(process);
		process.printProcessToConsole(" ");
		var jiraARDF = branch.getModel().getIndividual(NodeToDomainResolver.BASE_NS+"jiraA");
		var jiraA = instanceRepository.findInstanceById(jiraARDF.getURI()).get();
		var jiraBRDF = branch.getModel().getIndividual(NodeToDomainResolver.BASE_NS+"jiraB");
		var jiraB = instanceRepository.findInstanceById(jiraBRDF.getURI()).get();
		process.addInput("jiraIn", jiraB);
		process.removeInput("jiraIn", jiraA);	
		instanceRepository.concludeTransaction();
		instanceRepository.startReadTransaction();
		process.printProcessToConsole(" ");
		
	}
	
	
	
	public static void printFullProcessToLog(ProcessInstance proc) {
		printProcessToLog(proc, " ");
	}
	
	private static void printProcessToLog(ProcessInstance proc, String prefix) {
		
		System.out.println(prefix+proc.toString());
		String nextIndent = "  "+prefix;
		proc.getProcessSteps().stream().forEach(step -> {
			if (step instanceof ProcessInstance) {
				printProcessToLog((ProcessInstance) step, nextIndent);
			} else {
				
				System.out.println(nextIndent+step.toString());
			}
		});
		proc.getDecisionNodeInstances().stream().sorted(DecisionNodeInstance.comparator).forEach(dni -> System.out.println(nextIndent+dni.toString()));
	}
	
	
	
}
