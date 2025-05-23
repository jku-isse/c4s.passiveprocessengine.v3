package at.jku.isse.passiveprocessengine.instance;

import static org.junit.Assert.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import at.jku.isse.passiveprocessengine.rdfwrapper.RDFInstance;
import at.jku.isse.passiveprocessengine.rdfwrapper.RDFInstanceType;
import at.jku.isse.passiveprocessengine.definition.activeobjects.ProcessDefinition;
import at.jku.isse.passiveprocessengine.definition.registry.DTOs;
import at.jku.isse.passiveprocessengine.definition.registry.DefinitionTransformer;
import at.jku.isse.passiveprocessengine.demo.TestArtifacts;
import at.jku.isse.passiveprocessengine.demo.TestArtifacts.JiraStates;
import at.jku.isse.passiveprocessengine.demo.TestDTOProcesses;
import at.jku.isse.passiveprocessengine.instance.StepLifecycle.State;
import at.jku.isse.passiveprocessengine.instance.activeobjects.DecisionNodeInstance;
import at.jku.isse.passiveprocessengine.instance.activeobjects.ProcessInstance;
import at.jku.isse.passiveprocessengine.instance.activeobjects.ProcessStep;
import at.jku.isse.passiveprocessengine.instance.messages.EventDistributor;
import at.jku.isse.passiveprocessengine.instance.messages.Responses.IOResponse;
import at.jku.isse.passiveprocessengine.monitoring.CurrentSystemTimeProvider;
import at.jku.isse.passiveprocessengine.monitoring.ProcessQAStatsMonitor;
import at.jku.isse.passiveprocessengine.monitoring.ProcessStats;
import at.jku.isse.passiveprocessengine.rdfwrapper.events.ChangeEventTransformer;
import at.jku.isse.passiveprocessengine.rdfwrapper.events.ChangeListener;
import at.jku.isse.passiveprocessengine.wrappers.DefinitionWrapperTests;
import lombok.NonNull;

//@ExtendWith(SpringExtension.class)
//@SpringBootTest
public
class InstanceTests extends DefinitionWrapperTests {

	
	TestDTOProcesses procFactory;	
	TestArtifacts artifactFactory;
		
	RDFInstanceType typeJira;
	ChangeListener picp;
	ProcessQAStatsMonitor monitor;
	
	
	@Override
	@BeforeEach
	public
	void setup() {
		super.setup();
		EventDistributor eventDistrib = new EventDistributor();
		monitor = new ProcessQAStatsMonitor(new CurrentSystemTimeProvider());
		eventDistrib.registerHandler(monitor);
		ChangeListener picp = new ProcessInstanceChangeProcessor(configBuilder.getContext(), eventDistrib);
		ChangeEventTransformer picpWrapper = super.dsSetup.getChangeEventTransformer();
		picpWrapper.registerWithBranch(picp);
		
		artifactFactory = new TestArtifacts(super.instanceRepository, schemaReg);
		procFactory = new TestDTOProcesses(artifactFactory);
		typeJira = artifactFactory.getJiraInstanceType();
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
		super.instanceRepository.concludeTransaction();
		super.instanceRepository.startWriteTransaction();

		ProcessInstance procInstance = configBuilder.getContext().getFactoryIndex().getProcessInstanceFactory().getStepInstance(procDef, "TEST");
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
		ProcessInstance procInstance = configBuilder.getContext().getFactoryIndex().getProcessInstanceFactory().getStepInstance(procDef, "TEST");
		assert(procInstance != null);
		for (RDFInstance input : inputs) {
			IOResponse resp = procInstance.addInput(TestDTOProcesses.JIRA_IN, input);
			assert(resp.getError() == null);
		}
		return procInstance;
	}

	@Test
	void testComplexDataMapping() throws ProcessException {		
		RDFInstance jiraB =  artifactFactory.getJiraInstance("jiraB");
		RDFInstance jiraC = artifactFactory.getJiraInstance("jiraC");		
		RDFInstance jiraA = artifactFactory.getJiraInstance("jiraA", jiraB, jiraC);
						
		ProcessInstance proc =  instantiateDefaultProcess(procFactory.getSimple2StepProcessDefinition(), jiraA);		
		super.instanceRepository.concludeTransaction();
		proc.printProcessToConsole(" ");
		assertTrue(proc.getProcessSteps().stream()
				.filter(step -> step.getDefinition().getName().equals(TestDTOProcesses.SD1) )
				.allMatch(step -> step.getOutput(TestDTOProcesses.JIRA_OUT).size() == 2));
	}
	
	@Test
	void testComplexDataMappingUpdateToProperty() throws ProcessException {
		
		RDFInstance jiraB =  artifactFactory.getJiraInstance("jiraB");
		RDFInstance jiraC = artifactFactory.getJiraInstance("jiraC");
		RDFInstance jiraA = artifactFactory.getJiraInstance("jiraA", jiraB, jiraC);
		
		ProcessInstance proc =  instantiateDefaultProcess(procFactory.getSimple2StepProcessDefinition(), jiraA);		
		instanceRepository.concludeTransaction();
		proc.printProcessToConsole(" ");
		System.out.println(TestArtifacts.printProperties(jiraA));
		
		assert(proc.getProcessSteps().stream()
				.filter(step -> step.getDefinition().getName().equals(TestDTOProcesses.SD1) )
				.allMatch(step -> (step.getOutput(TestDTOProcesses.JIRA_OUT).size() == 2) && step.getActualLifecycleState().equals(State.COMPLETED) ));
		assert(proc.getProcessSteps().stream()
				.filter(step -> step.getDefinition().getName().equals(TestDTOProcesses.SD2) )
				.allMatch(step -> (step.getInput(TestDTOProcesses.JIRA_IN).size() == 2) && step.getActualLifecycleState().equals(State.ACTIVE) ) );
		
		instanceRepository.startWriteTransaction();
		artifactFactory.removeJiraFromReqs(jiraA, jiraC);		
		artifactFactory.setStateToJiraInstance(jiraB, JiraStates.Closed);
		// we close, thus keep SD1 in active state, thus no output propagation yet, 
		instanceRepository.concludeTransaction();		
		
		instanceRepository.startWriteTransaction();
		artifactFactory.setStateToJiraInstance(jiraB, JiraStates.Open);
		//now that we open again the jira issue, we fulfill SD1, and the output should be mapped, removing jiraC from SD2 input, and subsequently also from its output
		instanceRepository.concludeTransaction();
		
		proc.printProcessToConsole(" ");		
		System.out.println(TestArtifacts.printProperties(jiraA));
		
		assert(proc.getProcessSteps().stream()
			.filter(step -> step.getDefinition().getName().equals(TestDTOProcesses.SD1) )
			.allMatch(step -> (step.getOutput(TestDTOProcesses.JIRA_OUT).stream().findAny().get().getName().equals("jiraB")) && step.getExpectedLifecycleState().equals(State.COMPLETED) ) );
		
		ProcessStep step2 = proc.getProcessSteps().stream()
				.filter(step -> step.getDefinition().getName().equals(TestDTOProcesses.SD2) ).findAny().get();
		assert(step2.getInput(TestDTOProcesses.JIRA_IN).stream().findAny().get().getName().equals("jiraB")) ;
		assert(step2.getInput(TestDTOProcesses.JIRA_IN).size()==1) ;
		assert(step2.getOutput(TestDTOProcesses.JIRA_OUT).size()==1) ;
		assert(step2.getOutput(TestDTOProcesses.JIRA_OUT).stream().findAny().get().getName().equals("jiraB")) ;
		assert(step2.getActualLifecycleState().equals(State.ACTIVE) );
		
		monitor.calcFinalStats();
		ProcessStats stats = monitor.stats.get(proc);
		assert(stats.isProcessCompleted() == false);
	}
	
	@Test
	void testComplexDataMappingRemoveInput() throws ProcessException {
		RDFInstance jiraB =  artifactFactory.getJiraInstance("jiraB");
		RDFInstance jiraC = artifactFactory.getJiraInstance("jiraC");
		RDFInstance jiraD = artifactFactory.getJiraInstance("jiraD");
		RDFInstance jiraA = artifactFactory.getJiraInstance("jiraA", jiraB, jiraC);
		
		ProcessInstance proc =  instantiateDefaultProcess(procFactory.getSimple2StepProcessDefinition(), jiraA);		
		instanceRepository.concludeTransaction();
		proc.printProcessToConsole(" ");			
		assert(proc.getExpectedLifecycleState().equals(State.ACTIVE)); 
		
			
		assert(proc.getProcessSteps().stream()
				.filter(step -> step.getDefinition().getName().equals(TestDTOProcesses.SD1) )
				.allMatch(step -> step.getInput(TestDTOProcesses.JIRA_IN).size() == 1));
		assert(proc.getProcessSteps().stream()
				.filter(step -> step.getDefinition().getName().equals(TestDTOProcesses.SD1) )
				.allMatch(step -> step.getOutput("jiraOut").size() == 2));				
		
		instanceRepository.startWriteTransaction();
		proc.addInput("jiraIn", jiraD);
		proc.removeInput("jiraIn", jiraA);	
		instanceRepository.concludeTransaction();
		proc.printProcessToConsole(" ");
		assert(proc.getProcessSteps().stream()
			.filter(step -> step.getDefinition().getName().equals(TestDTOProcesses.SD1) )
			.allMatch(step -> step.getOutput("jiraOut").size() == 0));		
		
		instanceRepository.startWriteTransaction();
		artifactFactory.addJiraToRequirements(jiraD, jiraB);		
		instanceRepository.concludeTransaction();
		proc.printProcessToConsole(" ");					
		assert(proc.getActualLifecycleState().equals(State.ACTIVE));
		assert(proc.getProcessSteps().stream()
				.filter(step -> step.getDefinition().getName().equals(TestDTOProcesses.SD2) )
				.allMatch(step -> (step.getOutput(TestDTOProcesses.JIRA_OUT).stream().findAny().get().getName().equals("jiraB"))) );
		
		instanceRepository.startWriteTransaction();
		artifactFactory.removeJiraFromReqs(jiraD, jiraB);
		artifactFactory.addJiraToRequirements(jiraD, jiraC);		
		instanceRepository.concludeTransaction();		
		proc.printProcessToConsole(" ");
				assert(proc.getProcessSteps().stream()
				.filter(step -> step.getDefinition().getName().equals(TestDTOProcesses.SD1) )
				.allMatch(step -> (step.getOutput(TestDTOProcesses.JIRA_OUT).stream().findAny().get().getName().equals("jiraC")) && step.getExpectedLifecycleState().equals(State.COMPLETED) ));
		assert(proc.getProcessSteps().stream()
				.filter(step -> step.getDefinition().getName().equals(TestDTOProcesses.SD2) )
				.allMatch(step -> (step.getOutput(TestDTOProcesses.JIRA_OUT).stream().findAny().get().getName().equals("jiraC")) && (step.getActualLifecycleState().equals(State.ACTIVE) || step.getActualLifecycleState().equals(State.ENABLED)) ));
	}
	

	@Test
	void testProcessComplete() throws ProcessException {
		RDFInstance jiraB =  artifactFactory.getJiraInstance("jiraB");
		RDFInstance jiraC = artifactFactory.getJiraInstance("jiraC");		
		RDFInstance jiraA = artifactFactory.getJiraInstance("jiraA", jiraB, jiraC);
		
		var def = procFactory.getSimple2StepProcessDefinition();
		
		ProcessInstance proc =  instantiateDefaultProcess(def, jiraA);		
		instanceRepository.concludeTransaction();
		proc.printProcessToConsole(" ");			
		assert(proc.getExpectedLifecycleState().equals(State.ACTIVE)); 
			
		artifactFactory.setStateToJiraInstance(jiraA, JiraStates.Closed);
		artifactFactory.setStateToJiraInstance(jiraB, JiraStates.Closed);
		artifactFactory.setStateToJiraInstance(jiraC, JiraStates.Closed);
		artifactFactory.removeJiraFromReqs(jiraA, jiraC);
		instanceRepository.concludeTransaction();
		
		proc.printProcessToConsole(" ");
		assert(proc.getProcessSteps().stream()
				.filter(step -> step.getDefinition().getName().equals(TestDTOProcesses.SD2) )
				.allMatch(step -> step.getActualLifecycleState().equals(State.COMPLETED) ));
	
	}
//
//	@Test
//	void testSimpleSubprocess() throws ProcessException {
//		Instance jiraE =  artifactFactory.getJiraInstance("jiraE");
//		ProcessDefinition procDef = TestProcesses.getSimpleSubprocessDefinition(ws, true);
//		ProcessInstance proc = ProcessInstance.getInstance(ws, procDef);
//		proc.addInput("jiraIn", jiraE);
//		instanceRepository.concludeTransaction();
//		printFullProcessToLog(proc);
//		assert(proc.getExpectedLifecycleState().equals(State.ACTIVE));
//		
//		artifactFactory.setStateToJiraInstance(jiraE, JiraStates.Closed);
//		instanceRepository.concludeTransaction();
//		
//		printFullProcessToLog(proc);
//		assert(proc.getExpectedLifecycleState().equals(State.COMPLETED));
//		assert(proc.getActualLifecycleState().equals(State.COMPLETED));
//		assert(proc.getOutput("jiraOut").size() == 1);
//	}
//	
//	@Test
//	void testSimpleParentprocess() throws ProcessException {
//		Instance jiraF =  artifactFactory.getJiraInstance("jiraF");
//		ProcessDefinition procDef = TestProcesses.getSimpleSuperProcessDefinition(ws);
//		ProcessInstance proc = ProcessInstance.getInstance(ws, procDef, "SimpleParentprocess");
//
//		proc.addInput("jiraIn", jiraF);
//		instanceRepository.concludeTransaction();
//		artifactFactory.setStateToJiraInstance(jiraF, JiraStates.Closed);
//		instanceRepository.concludeTransaction();
//		
//		printFullProcessToLog(proc); 
//		assert(proc.getExpectedLifecycleState().equals(State.COMPLETED));
//		assert(proc.getActualLifecycleState().equals(State.COMPLETED));
//		assert(proc.getOutput("jiraOut").size() == 1);
//	}
//
//
//	@Test
//	void testSymmetricDifferenceDatamapping() throws ProcessException {
//		Instance jiraB =  artifactFactory.getJiraInstance("jiraB");
//		Instance jiraC = artifactFactory.getJiraInstance("jiraC");
//		Instance jiraD = artifactFactory.getJiraInstance("jiraD");
//		Instance jiraA = artifactFactory.getJiraInstance("jiraA", "jiraB", "jiraC");
//		artifactFactory.addJiraToJira(jiraA, jiraB);
//		artifactFactory.addJiraToJira(jiraA, jiraC);
//		
//		ProcessDefinition procDef = TestProcesses.get2StepProcessDefinitionWithSymmetricDiffMapping(ws);
//		ProcessInstance proc = ProcessInstance.getInstance(ws, procDef);
//		proc.addInput("jiraIn", jiraA);
//		instanceRepository.concludeTransaction();
//	//	assertAllConstraintsAreValid(proc);
//	//	printFullProcessToLog(proc);
//		assert(proc.getProcessSteps().stream()
//				.filter(step -> step.getDefinition().getName().equals("sd1") )
//				.allMatch(step -> step.getOutput("jiraOut").size() == 2 ));
//
//		
//		artifactFactory.removeJiraFromJira(jiraA,  jiraB);
//		artifactFactory.addJiraToJira(jiraA,  jiraD);
//		instanceRepository.concludeTransaction();
//		printFullProcessToLog(proc);
//		assert(proc.getProcessSteps().stream()
//				.filter(step -> step.getDefinition().getName().equals("sd1") )
//				.allMatch(step -> step.getOutput("jiraOut").size() == 2 ));
//
//	}
//	
//	@Test
//	void testUnionSymmetricDifferenceDatamapping() throws ProcessException {
//		Instance jiraB =  artifactFactory.getJiraInstance("jiraB");
//		Instance jiraC = artifactFactory.getJiraInstance("jiraC");
//		Instance jiraD = artifactFactory.getJiraInstance("jiraD");
//		Instance jiraA = artifactFactory.getJiraInstance("jiraA", "jiraB", "jiraC");
//		artifactFactory.addJiraToJira(jiraA, jiraB);
//		//artifactFactory.addJiraToJira(jiraA, jiraC);
//		
//		ProcessDefinition procDef = TestProcesses.get2StepProcessDefinitionWithUnionMapping(ws);
//		ProcessInstance proc = ProcessInstance.getInstance(ws, procDef);
//		proc.addInput("jiraIn", jiraA);
//		proc.addInput("jiraIn2", jiraD);
//		
//		instanceRepository.concludeTransaction();
//		printFullProcessToLog(proc);
//		assert(proc.getProcessSteps().stream()
//				.filter(step -> step.getDefinition().getName().equals("sd1") )
//				.allMatch(step -> step.getOutput("jiraOut").size() == 2 ));
//
//		
//		artifactFactory.removeJiraFromJira(jiraA,  jiraB);
//		instanceRepository.concludeTransaction();
//		printFullProcessToLog(proc);
//		assert(proc.getProcessSteps().stream()
//				.filter(step -> step.getDefinition().getName().equals("sd1") )
//				.allMatch(step -> step.getOutput("jiraOut").size() == 1 ));
//		// while this works for this usecase, the repair suggestion for union picks the first collection found, and not the right collection,
//		// i.e., JiraD is suggested to be removed from jiraA.requirements and not (as would be correct) from in_jiraIn2
//
//	}
//	
//	@Test
//	void testExistsCompletion() throws ProcessException {
//		Instance jiraB =  artifactFactory.getJiraInstance("jiraB");
//		Instance jiraC = artifactFactory.getJiraInstance("jiraC");
//		Instance jiraD = artifactFactory.getJiraInstance("jiraD");
//		Instance jiraA = artifactFactory.getJiraInstance("jiraA", jiraB, jiraC);
//		artifactFactory.addJiraToJira(jiraA, jiraB);
//		artifactFactory.addParentToJira(jiraB, jiraC);
//		artifactFactory.addParentToJira(jiraD, jiraC);
//		//artifactFactory.addJiraToJira(jiraA, jiraC);
//		
//		ProcessDefinition procDef = TestProcesses.get2StepProcessDefinitionWithExistsCheck(ws);
//		ProcessInstance proc = ProcessInstance.getInstance(ws, procDef);
//		proc.addInput("jiraIn", jiraA);
//		proc.addInput("jiraIn2", jiraD);
//		
//		instanceRepository.concludeTransaction();
//		printFullProcessToLog(proc);
//		assert(proc.getProcessSteps().stream()
//				.filter(step -> step.getDefinition().getName().equals("sd1") )
//				.allMatch(step -> step.getActualLifecycleState().equals(State.ENABLED) ));
//		
//		ProcessStep step1 = proc.getProcessSteps().stream()
//				.filter(step -> step.getDefinition().getName().equals("sd1") ).findAny().get();
//		Optional<ConsistencyRule> crOpt = step1.getConditionStatus(Conditions.POSTCONDITION);
//		RepairNode repairTree = RuleService.repairTree(crOpt.get());
//		assert(repairTree != null);
//		
//		artifactFactory.setStateToJiraInstance(jiraC, JiraStates.Closed);
//		instanceRepository.concludeTransaction();
//		printFullProcessToLog(proc);
//		assert(proc.getProcessSteps().stream()
//				.filter(step -> step.getDefinition().getName().equals("sd1") )
//				.allMatch(step -> step.getActualLifecycleState().equals(State.COMPLETED) ));
////		artifactFactory.removeJiraFromJira(jiraA,  jiraB);
////		instanceRepository.concludeTransaction();
////		printFullProcessToLog(proc);
////		assert(proc.getProcessSteps().stream()
////				.filter(step -> step.getDefinition().getName().equals("sd1") )
////				.allMatch(step -> step.getOutput("jiraOut").size() == 1 ));
//		
//	}			
	
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
