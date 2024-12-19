package at.jku.isse.passiveprocessengine.persistence;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import at.jku.isse.passiveprocessengine.core.PPEInstance;
import at.jku.isse.passiveprocessengine.core.PPEInstanceType;
import at.jku.isse.passiveprocessengine.core.ProcessEngineConfigurationBuilder;
import at.jku.isse.passiveprocessengine.core.ProcessInstanceChangeListener;
import at.jku.isse.passiveprocessengine.core.RepairTreeProvider;
import at.jku.isse.passiveprocessengine.core.RuleAnalysisService;
import at.jku.isse.passiveprocessengine.core.RuleEvaluationService;
import at.jku.isse.passiveprocessengine.core.SchemaRegistry;
import at.jku.isse.artifacteventstreaming.api.Branch;
import at.jku.isse.artifacteventstreaming.api.BranchStateUpdater;
import at.jku.isse.passiveprocessengine.core.ChangeEventTransformer;
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
import at.jku.isse.passiveprocessengine.rdfwrapper.RDFWrapperSetup;
import lombok.NonNull;

public
class ProcessPersistenceTests {

	protected RDFWrapperSetup dsSetup;
	protected InstanceRepository instanceRepository;
	protected SchemaRegistry schemaReg;
	protected RepairTreeProvider ruleServiceWrapper;
	protected ProcessEngineConfigurationBuilder configBuilder;
	TestDTOProcesses procFactory;	
	TestArtifacts artifactFactory;
		
	PPEInstanceType typeJira;
	ProcessInstanceChangeListener picp;
	ProcessQAStatsMonitor monitor;
	Branch branch;
	
	
	public
	void setup() throws Exception {
		dsSetup = new RDFWrapperSetup();
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
				, new RewriterFactory(designspaceAbstractionMapper, false, ((RDFWrapperSetup) dsSetup).getRuleSchemaProvider())
				, ruleEvaluationFactory
				, dsSetup.getCoreTypeFactory()
				, (RuleAnalysisService) ruleServiceWrapper);
		System.out.println("Size after process engine build: "+branch.getModel().size());
		EventDistributor eventDistrib = new EventDistributor();
		monitor = new ProcessQAStatsMonitor(new CurrentSystemTimeProvider());
		eventDistrib.registerHandler(monitor);
		ProcessInstanceChangeListener picp = new ProcessInstanceChangeProcessor(configBuilder.getContext(), eventDistrib);
		ChangeEventTransformer picpWrapper = dsSetup.getChangeEventTransformer();
		picpWrapper.registerWithWorkspace(picp);
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
	
	protected ProcessInstance instantiateDefaultProcess(DTOs.Process procDTO, PPEInstance... inputs) {
		ProcessDefinition procDef = getDefinition(procDTO);				
		instanceRepository.concludeTransaction();
		instanceRepository.startWriteTransaction();

		ProcessInstance procInstance = configBuilder.getContext().getFactoryIndex().getProcessInstanceFactory().getInstance(procDef, "TEST");
		assert(procInstance != null);
		configBuilder.getContext().getInstanceRepository().concludeTransaction();
		configBuilder.getContext().getInstanceRepository().startWriteTransaction();
		for (PPEInstance input : inputs) {
			IOResponse resp = procInstance.addInput(TestDTOProcesses.JIRA_IN, input);
			assert(resp.getError() == null);
		}
		return procInstance;
	}
	
	protected ProcessInstance instantiateDefaultProcess(@NonNull ProcessDefinition procDef,  PPEInstance... inputs) {		
		ProcessInstance procInstance = configBuilder.getContext().getFactoryIndex().getProcessInstanceFactory().getInstance(procDef, "TEST");
		assert(procInstance != null);
		for (PPEInstance input : inputs) {
			IOResponse resp = procInstance.addInput(TestDTOProcesses.JIRA_IN, input);
			assert(resp.getError() == null);
		}
		return procInstance;
	}

	@Test
	void testCreatingProcess() throws Exception {		
		RDFWrapperSetup.resetPersistence(); // also ensure to delete model from filesystem
		setup(); // manual as we otherwise cant reset data
		instanceRepository.startWriteTransaction();
		PPEInstance jiraB =  artifactFactory.getJiraInstance("jiraB");
		PPEInstance jiraC = artifactFactory.getJiraInstance("jiraC");		
		PPEInstance jiraA = artifactFactory.getJiraInstance("jiraA", jiraB, jiraC);
						
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
		var process = configBuilder.getContext().getWrappedInstance(ProcessInstance.class, optProc.get());
		assertNotNull(process);
		instanceRepository.concludeTransaction();
		
		
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
