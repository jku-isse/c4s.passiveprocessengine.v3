package at.jku.isse.designspace.passiveprocessengine.instance;

import static org.junit.Assert.assertTrue;

import java.util.Optional;

import org.junit.Ignore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import at.jku.isse.designspace.core.controlflow.ControlEventEngine;
import at.jku.isse.designspace.core.model.Element;
import at.jku.isse.designspace.core.model.Id;
import at.jku.isse.designspace.core.model.Instance;
import at.jku.isse.designspace.core.model.InstanceType;
import at.jku.isse.designspace.core.model.Workspace;
import at.jku.isse.designspace.core.service.WorkspaceService;
import at.jku.isse.designspace.rule.arl.repair.RepairNode;
import at.jku.isse.designspace.rule.checker.ArlRuleEvaluator;
import at.jku.isse.designspace.rule.model.ConsistencyRule;
import at.jku.isse.designspace.rule.service.RuleService;
import at.jku.isse.passiveprocessengine.definition.ProcessDefinition;
import at.jku.isse.passiveprocessengine.definition.serialization.JsonDefinitionSerializer;
import at.jku.isse.passiveprocessengine.demo.TestArtifacts;
import at.jku.isse.passiveprocessengine.demo.TestArtifacts.JiraStates;
import at.jku.isse.passiveprocessengine.demo.TestProcesses;
import at.jku.isse.passiveprocessengine.instance.DecisionNodeInstance;
import at.jku.isse.passiveprocessengine.instance.ProcessException;
import at.jku.isse.passiveprocessengine.instance.ProcessInstance;
import at.jku.isse.passiveprocessengine.instance.ProcessInstanceChangeProcessor;
import at.jku.isse.passiveprocessengine.instance.ProcessStep;
import at.jku.isse.passiveprocessengine.instance.StepLifecycle.Conditions;
import at.jku.isse.passiveprocessengine.instance.StepLifecycle.State;
import at.jku.isse.passiveprocessengine.instance.messages.EventDistributor;
import at.jku.isse.passiveprocessengine.instance.messages.WorkspaceListenerSequencer;
import at.jku.isse.passiveprocessengine.monitoring.CurrentSystemTimeProvider;
import at.jku.isse.passiveprocessengine.monitoring.ProcessQAStatsMonitor;
import at.jku.isse.passiveprocessengine.monitoring.ProcessStats;

@ExtendWith(SpringExtension.class)
@SpringBootTest
public
class InstanceTests {

	static Workspace ws;
	static InstanceType typeJira;
	ProcessInstanceChangeProcessor picp;
	static JsonDefinitionSerializer json = new JsonDefinitionSerializer();
	static ProcessQAStatsMonitor monitor;

	@Autowired
	ControlEventEngine controlEventEngine;
	
	@BeforeEach
	void setup() throws Exception {
		RuleService.setEvaluator(new ArlRuleEvaluator());
		//ws = WorkspaceService.createWorkspace("test", WorkspaceService.PUBLIC_WORKSPACE, WorkspaceService.ANY_USER, null, true, false);
		ws = WorkspaceService.PUBLIC_WORKSPACE;
		RuleService.currentWorkspace = ws;
		EventDistributor eventDistrib = new EventDistributor();
		monitor = new ProcessQAStatsMonitor(new CurrentSystemTimeProvider());
		eventDistrib.registerHandler(monitor);
		picp = new ProcessInstanceChangeProcessor(ws, eventDistrib);
		WorkspaceListenerSequencer wsls = new WorkspaceListenerSequencer(ws);
	//	wsls.registerListener(repAnalyzer);
		wsls.registerListener(picp);
		typeJira = TestArtifacts.getJiraInstanceType(ws);
	}


	@Test
	void opposableProps() {
		Instance jiraB =  TestArtifacts.getJiraInstance(ws, "jiraB");
		Instance jiraC = TestArtifacts.getJiraInstance(ws, "jiraC");
		
	}
	
	@Test
	void testComplexDataMapping() throws ProcessException {
		Instance jiraB =  TestArtifacts.getJiraInstance(ws, "jiraB");
		Instance jiraC = TestArtifacts.getJiraInstance(ws, "jiraC");
		Instance jiraD = TestArtifacts.getJiraInstance(ws, "jiraD");
		Instance jiraA = TestArtifacts.getJiraInstance(ws, "jiraA");//, "jiraB", "jiraC");
		TestArtifacts.addJiraToJira(jiraA, jiraB);
		TestArtifacts.addJiraToJira(jiraA, jiraC);		
		
		ProcessDefinition procDef = TestProcesses.getSimple2StepProcessDefinition(ws);
		ProcessInstance proc = ProcessInstance.getInstance(ws, procDef);
		proc.addInput("jiraIn", jiraA);
		ws.concludeTransaction();

		System.out.println(proc);
		proc.getProcessSteps().stream().forEach(step -> System.out.println(step));

		assert(proc.getProcessSteps().stream()
				.filter(step -> step.getDefinition().getName().equals("sd1") )
				.allMatch(step -> step.getOutput("jiraOut").size() == 2));
	}
	
	@Test
	void testComplexDataMappingUpdateToProperty() throws ProcessException {
		Instance jiraB =  TestArtifacts.getJiraInstance(ws, "jiraB");
		Instance jiraC = TestArtifacts.getJiraInstance(ws, "jiraC");
		Instance jiraD = TestArtifacts.getJiraInstance(ws, "jiraD");
		Instance jiraA = TestArtifacts.getJiraInstance(ws, "jiraA");//, "jiraB", "jiraC");
		TestArtifacts.addJiraToJira(jiraA, jiraB);
		TestArtifacts.addJiraToJira(jiraA, jiraC);	
		
		ProcessDefinition procDef = TestProcesses.getSimple2StepProcessDefinition(ws);
		ProcessInstance proc = ProcessInstance.getInstance(ws, procDef);
		proc.addInput("jiraIn", jiraA);
		ws.concludeTransaction();
		System.out.println(proc);
		proc.getProcessSteps().stream().forEach(step -> System.out.println(step));
		assert(proc.getProcessSteps().stream()
				.filter(step -> step.getDefinition().getName().equals("sd1") )
				.allMatch(step -> (step.getOutput("jiraOut").size() == 2) && step.getActualLifecycleState().equals(State.COMPLETED) ));
		assert(proc.getProcessSteps().stream()
				.filter(step -> step.getDefinition().getName().equals("sd2") )
				.allMatch(step -> (step.getInput("jiraIn").size() == 2) && step.getActualLifecycleState().equals(State.ACTIVE) ) );
		
		jiraA.getPropertyAsSet(TestArtifacts.CoreProperties.requirements.toString()).remove(jiraC);
		TestArtifacts.setStateToJiraInstance(jiraB, JiraStates.Closed);
		// we close, thus keep SD1 in active state, thus no output propagation yet, 
		ws.concludeTransaction();
		assert(jiraA.getPropertyAsSet(TestArtifacts.CoreProperties.requirements.toString()).size() == 1);
		proc.getProcessSteps().stream().forEach(step -> System.out.println(step));
		
		TestArtifacts.setStateToJiraInstance(jiraB, JiraStates.Open);
		//now that we open again the jira issue, we fulfill SD1, and the output should be mapped, removing jiraC from SD2 input, and subsequently also from its output
		ws.concludeTransaction();
		
		System.out.println(proc);
		proc.getProcessSteps().stream().forEach(step -> System.out.println(step));
		proc.getDecisionNodeInstances().stream().forEach(dni -> System.out.println(dni));
		assertAllConstraintsAreValid(proc);
		assert(proc.getProcessSteps().stream()
			.filter(step -> step.getDefinition().getName().equals("sd1") )
			.allMatch(step -> (step.getOutput("jiraOut").iterator().next().name().equals("jiraB")) && step.getExpectedLifecycleState().equals(State.COMPLETED) ) );
		
		ProcessStep step2 = proc.getProcessSteps().stream()
				.filter(step -> step.getDefinition().getName().equals("sd2") ).findAny().get();
		assert(step2.getInput("jiraIn").iterator().next().name().equals("jiraB")) ;
		assert(step2.getInput("jiraIn").size()==1) ;
		assert(step2.getOutput("jiraOut").size()==1) ;
		assert(step2.getOutput("jiraOut").iterator().next().name().equals("jiraB")) ;
		assert(step2.getActualLifecycleState().equals(State.ACTIVE) );
		
		monitor.calcFinalStats();
		ProcessStats stats = monitor.stats.get(proc);
		assert(stats.isProcessCompleted() == false);

		Element element = ws.findElement(Id.of(244l));
		System.out.println(element);

		Element element1 = ws.findElement(Id.of(252l));
		System.out.println(element1);
	}
	
	@Test
	void testComplexDataMappingRemoveInput() throws ProcessException {
		Instance jiraB =  TestArtifacts.getJiraInstance(ws, "jiraB");
		Instance jiraC = TestArtifacts.getJiraInstance(ws, "jiraC");
		Instance jiraD = TestArtifacts.getJiraInstance(ws, "jiraD");
		Instance jiraA = TestArtifacts.getJiraInstance(ws, "jiraA");//, "jiraB", "jiraC");
		TestArtifacts.addJiraToJira(jiraA, jiraB);
		TestArtifacts.addJiraToJira(jiraA, jiraC);	
		
		ProcessDefinition procDef = TestProcesses.getSimple2StepProcessDefinition(ws);
		ProcessInstance proc = ProcessInstance.getInstance(ws, procDef);
		proc.addInput("jiraIn", jiraA);
		ws.concludeTransaction();
		System.out.println(proc);
		proc.getProcessSteps().stream().forEach(step -> System.out.println(step));
		assert(proc.getExpectedLifecycleState().equals(State.ACTIVE)); 
		
		proc.addInput("jiraIn", jiraD);
		proc.removeInput("jiraIn", jiraA);
		assert(proc.getProcessSteps().stream()
				.filter(step -> step.getDefinition().getName().equals("sd1") )
				.allMatch(step -> step.getInput("jiraIn").size() == 1));
		assert(proc.getProcessSteps().stream()
				.filter(step -> step.getDefinition().getName().equals("sd1") )
				.allMatch(step -> step.getOutput("jiraOut").size() == 2));
		ws.concludeTransaction();
		
		assert(proc.getProcessSteps().stream()
			.filter(step -> step.getDefinition().getName().equals("sd1") )
			.allMatch(step -> step.getOutput("jiraOut").size() == 0));
		
		jiraD.getPropertyAsSet(TestArtifacts.CoreProperties.requirements.toString()).add(jiraB);
		//TestArtifacts.setStateToJiraInstance(jiraB, JiraStates.Closed);
		ws.concludeTransaction();
		System.out.println(proc);
		proc.getProcessSteps().stream().forEach(step -> System.out.println(step));
		assert(proc.getActualLifecycleState().equals(State.ACTIVE));
		assert(proc.getProcessSteps().stream()
				.filter(step -> step.getDefinition().getName().equals("sd2") )
				.allMatch(step -> (step.getOutput("jiraOut").iterator().next().name().equals("jiraB"))) );
		
		jiraD.getPropertyAsSet(TestArtifacts.CoreProperties.requirements.toString()).remove(jiraB);
//		ws.concludeTransaction();
//		assert(proc.getProcessSteps().stream()
//				.filter(step -> step.getDefinition().getName().equals("sd2") )
//				.allMatch(step -> (step.getOutput("jiraOut").size() == 0)) );
		
		jiraD.getPropertyAsSet(TestArtifacts.CoreProperties.requirements.toString()).add(jiraC);
		ws.concludeTransaction();
		
		assertAllConstraintsAreValid(proc);
		System.out.println(proc);
		proc.getProcessSteps().stream().forEach(step -> System.out.println(step));
		assert(proc.getProcessSteps().stream()
				.filter(step -> step.getDefinition().getName().equals("sd1") )
				.allMatch(step -> (step.getOutput("jiraOut").iterator().next().name().equals("jiraC")) && step.getExpectedLifecycleState().equals(State.COMPLETED) ));
		assert(proc.getProcessSteps().stream()
				.filter(step -> step.getDefinition().getName().equals("sd2") )
				.allMatch(step -> (step.getOutput("jiraOut").iterator().next().name().equals("jiraC")) && step.getActualLifecycleState().equals(State.ACTIVE) ));
	}
	
	@Test
	void testComplexDataMappingImmediateRemoveInput() throws ProcessException {
		Instance jiraB =  TestArtifacts.getJiraInstance(ws, "jiraB");
		Instance jiraC = TestArtifacts.getJiraInstance(ws, "jiraC");
		Instance jiraD = TestArtifacts.getJiraInstance(ws, "jiraD");
		Instance jiraA = TestArtifacts.getJiraInstance(ws, "jiraA");//, "jiraB", "jiraC");
		TestArtifacts.addJiraToJira(jiraA, jiraB);
		TestArtifacts.addJiraToJira(jiraA, jiraC);	
		
		ProcessDefinition procDef = TestProcesses.getSimple2StepProcessDefinition(ws);
		procDef.setImmediateInstantiateAllStepsEnabled(true);
		procDef.setImmediateDataPropagationEnabled(true);
		
		ProcessInstance proc = ProcessInstance.getInstance(ws, procDef);
		proc.addInput("jiraIn", jiraA);
		ws.concludeTransaction();
		System.out.println(proc);
		proc.getProcessSteps().stream().forEach(step -> System.out.println(step));
		assert(proc.getExpectedLifecycleState().equals(State.ACTIVE)); 
		
		proc.addInput("jiraIn", jiraD);
		proc.removeInput("jiraIn", jiraA);
		assert(proc.getProcessSteps().stream()
				.filter(step -> step.getDefinition().getName().equals("sd1") )
				.allMatch(step -> step.getInput("jiraIn").size() == 1));
		assert(proc.getProcessSteps().stream()
				.filter(step -> step.getDefinition().getName().equals("sd1") )
				.allMatch(step -> step.getOutput("jiraOut").size() == 2));
		ws.concludeTransaction();
		
		assert(proc.getProcessSteps().stream()
			.filter(step -> step.getDefinition().getName().equals("sd1") )
			.allMatch(step -> step.getOutput("jiraOut").size() == 0));				
	}
	
	@Test
	void testRules() throws ProcessException {
		Instance jiraB =  TestArtifacts.getJiraInstance(ws, "jiraB");
		Instance jiraC = TestArtifacts.getJiraInstance(ws, "jiraC");
		Instance jiraD = TestArtifacts.getJiraInstance(ws, "jiraD");
		Instance jiraA = TestArtifacts.getJiraInstance(ws, "jiraA");//, "jiraB", "jiraC");
		TestArtifacts.addJiraToJira(jiraA, jiraB);
		TestArtifacts.addJiraToJira(jiraA, jiraC);	
		
		ProcessDefinition procDef = TestProcesses.getSimple2StepProcessDefinition(ws);
		ProcessInstance proc = ProcessInstance.getInstance(ws, procDef);
		proc.addInput("jiraIn", jiraA);
		ws.concludeTransaction();
		assertTrue(jiraA.getPropertyAsSet(TestArtifacts.CoreProperties.requirements.toString()).get().size()==2);
			
		TestArtifacts.setStateToJiraInstance(jiraB, JiraStates.Closed);
		TestArtifacts.setStateToJiraInstance(jiraC, JiraStates.Closed);
		ws.concludeTransaction();
		
		assertAllConstraintsAreValid(proc);
		System.out.println(proc);
		proc.getProcessSteps().stream().forEach(step -> System.out.println(step));
		assert(proc.getProcessSteps().stream()
				.filter(step -> step.getDefinition().getName().equals("sd2") )
				.allMatch(step -> step.getActualLifecycleState().equals(State.COMPLETED) ));
	
	}

	@Test
	void testSimpleSubprocess() throws ProcessException {
		Instance jiraE =  TestArtifacts.getJiraInstance(ws, "jiraE");
		ProcessDefinition procDef = TestProcesses.getSimpleSubprocessDefinition(ws, true);
		ProcessInstance proc = ProcessInstance.getInstance(ws, procDef);
		proc.addInput("jiraIn", jiraE);
		ws.concludeTransaction();
		printFullProcessToLog(proc);
		assert(proc.getExpectedLifecycleState().equals(State.ACTIVE));
		
		TestArtifacts.setStateToJiraInstance(jiraE, JiraStates.Closed);
		ws.concludeTransaction();
		
		printFullProcessToLog(proc);
		assert(proc.getExpectedLifecycleState().equals(State.COMPLETED));
		assert(proc.getActualLifecycleState().equals(State.COMPLETED));
		assert(proc.getOutput("jiraOut").size() == 1);
	}
	
	@Ignore // needs some fixing,!!
	@Test
	void testSimpleParentprocess() throws ProcessException {
//		Instance jiraF =  TestArtifacts.getJiraInstance(ws, "jiraF");
//		ProcessDefinition procDef = TestProcesses.getSimpleSuperProcessDefinition(ws);
//		ProcessInstance proc = ProcessInstance.getInstance(ws, procDef, "SimpleParentprocess");
//
//		proc.addInput("jiraIn", jiraF);
//		ws.concludeTransaction();
//		TestArtifacts.setStateToJiraInstance(jiraF, JiraStates.Closed);
//		ws.concludeTransaction();
//		
//		printFullProcessToLog(proc); 
//		assert(proc.getExpectedLifecycleState().equals(State.COMPLETED));
//		assert(proc.getActualLifecycleState().equals(State.COMPLETED));
//		assert(proc.getOutput("jiraOut").size() == 1);
	}

//	@Test
//	void testSimpleParentProcessFromSerializationForm() {
//		Instance jiraF =  TestArtifacts.getJiraInstance(ws, "jiraF");
//		DTOs.Process procD = TestProcesses.getSimpleSuperDTOProcessDefinition(ws);
//		String jsonProc = json.toJson(procD);
//		DTOs.Process deSer = json.fromJson(jsonProc);
//		ProcessDefinition procDef = DefinitionTransformer.fromDTO(deSer, ws);
//		ProcessInstance proc = ProcessInstance.getInstance(ws, procDef);
//		proc.addInput("jiraIn", jiraF);
//		ws.concludeTransaction();
//		TestArtifacts.setStateToJiraInstance(jiraF, JiraStates.Closed);
//		ws.concludeTransaction();
//		
//		InstanceTests.printFullProcessToLog(proc); 
//		assert(proc.getExpectedLifecycleState().equals(State.COMPLETED));
//		assert(proc.getActualLifecycleState().equals(State.COMPLETED));
//		assert(proc.getOutput("jiraOut").size() == 1);
//	}

	@Test
	void testSymmetricDifferenceDatamapping() throws ProcessException {
		Instance jiraB =  TestArtifacts.getJiraInstance(ws, "jiraB");
		Instance jiraC = TestArtifacts.getJiraInstance(ws, "jiraC");
		Instance jiraD = TestArtifacts.getJiraInstance(ws, "jiraD");
		Instance jiraA = TestArtifacts.getJiraInstance(ws, "jiraA", "jiraB", "jiraC");
		TestArtifacts.addJiraToJira(jiraA, jiraB);
		TestArtifacts.addJiraToJira(jiraA, jiraC);
		
		ProcessDefinition procDef = TestProcesses.get2StepProcessDefinitionWithSymmetricDiffMapping(ws);
		ProcessInstance proc = ProcessInstance.getInstance(ws, procDef);
		proc.addInput("jiraIn", jiraA);
		ws.concludeTransaction();
	//	assertAllConstraintsAreValid(proc);
	//	printFullProcessToLog(proc);
		assert(proc.getProcessSteps().stream()
				.filter(step -> step.getDefinition().getName().equals("sd1") )
				.allMatch(step -> step.getOutput("jiraOut").size() == 2 ));

		
		TestArtifacts.removeJiraFromJira(jiraA,  jiraB);
		TestArtifacts.addJiraToJira(jiraA,  jiraD);
		ws.concludeTransaction();
		printFullProcessToLog(proc);
		assert(proc.getProcessSteps().stream()
				.filter(step -> step.getDefinition().getName().equals("sd1") )
				.allMatch(step -> step.getOutput("jiraOut").size() == 2 ));

	}
	
	@Test
	void testUnionSymmetricDifferenceDatamapping() throws ProcessException {
		Instance jiraB =  TestArtifacts.getJiraInstance(ws, "jiraB");
		Instance jiraC = TestArtifacts.getJiraInstance(ws, "jiraC");
		Instance jiraD = TestArtifacts.getJiraInstance(ws, "jiraD");
		Instance jiraA = TestArtifacts.getJiraInstance(ws, "jiraA", "jiraB", "jiraC");
		TestArtifacts.addJiraToJira(jiraA, jiraB);
		//TestArtifacts.addJiraToJira(jiraA, jiraC);
		
		ProcessDefinition procDef = TestProcesses.get2StepProcessDefinitionWithUnionMapping(ws);
		ProcessInstance proc = ProcessInstance.getInstance(ws, procDef);
		proc.addInput("jiraIn", jiraA);
		proc.addInput("jiraIn2", jiraD);
		
		ws.concludeTransaction();
		assertAllConstraintsAreValid(proc);
		printFullProcessToLog(proc);
		assert(proc.getProcessSteps().stream()
				.filter(step -> step.getDefinition().getName().equals("sd1") )
				.allMatch(step -> step.getOutput("jiraOut").size() == 2 ));

		
		TestArtifacts.removeJiraFromJira(jiraA,  jiraB);
		ws.concludeTransaction();
		printFullProcessToLog(proc);
		assert(proc.getProcessSteps().stream()
				.filter(step -> step.getDefinition().getName().equals("sd1") )
				.allMatch(step -> step.getOutput("jiraOut").size() == 1 ));
		// while this works for this usecase, the repair suggestion for union picks the first collection found, and not the right collection,
		// i.e., JiraD is suggested to be removed from jiraA.requirements and not (as would be correct) from in_jiraIn2

	}
	
	@Test
	void testExistsCompletion() throws ProcessException {
		Instance jiraB =  TestArtifacts.getJiraInstance(ws, "jiraB");
		Instance jiraC = TestArtifacts.getJiraInstance(ws, "jiraC");
		Instance jiraD = TestArtifacts.getJiraInstance(ws, "jiraD");
		Instance jiraA = TestArtifacts.getJiraInstance(ws, "jiraA", "jiraB", "jiraC");
		TestArtifacts.addJiraToJira(jiraA, jiraB);
		TestArtifacts.addParentToJira(jiraB, jiraC);
		TestArtifacts.addParentToJira(jiraD, jiraC);
		//TestArtifacts.addJiraToJira(jiraA, jiraC);
		
		ProcessDefinition procDef = TestProcesses.get2StepProcessDefinitionWithExistsCheck(ws);
		ProcessInstance proc = ProcessInstance.getInstance(ws, procDef);
		proc.addInput("jiraIn", jiraA);
		proc.addInput("jiraIn2", jiraD);
		
		ws.concludeTransaction();
		assertAllConstraintsAreValid(proc);
		printFullProcessToLog(proc);
		assert(proc.getProcessSteps().stream()
				.filter(step -> step.getDefinition().getName().equals("sd1") )
				.allMatch(step -> step.getActualLifecycleState().equals(State.ENABLED) ));
		
		ProcessStep step1 = proc.getProcessSteps().stream()
				.filter(step -> step.getDefinition().getName().equals("sd1") ).findAny().get();
		Optional<ConsistencyRule> crOpt = step1.getConditionStatus(Conditions.POSTCONDITION);
		RepairNode repairTree = RuleService.repairTree(crOpt.get());
		assert(repairTree != null);
		
		TestArtifacts.setStateToJiraInstance(jiraC, JiraStates.Closed);
		ws.concludeTransaction();
		assertAllConstraintsAreValid(proc);
		printFullProcessToLog(proc);
		assert(proc.getProcessSteps().stream()
				.filter(step -> step.getDefinition().getName().equals("sd1") )
				.allMatch(step -> step.getActualLifecycleState().equals(State.COMPLETED) ));
//		TestArtifacts.removeJiraFromJira(jiraA,  jiraB);
//		ws.concludeTransaction();
//		printFullProcessToLog(proc);
//		assert(proc.getProcessSteps().stream()
//				.filter(step -> step.getDefinition().getName().equals("sd1") )
//				.allMatch(step -> step.getOutput("jiraOut").size() == 1 ));
		
	}


	
	
	public static void assertAllConstraintsAreValid(ProcessInstance proc) {
//		proc.getProcessSteps().stream()
//		.peek(td -> System.out.println("Visiting Step: "+td.getName()))
//		.forEach(td -> {
//			td.getDefinition().getInputToOutputMappingRules().entrySet().stream().forEach(entry -> {
//				InstanceType type = td.getInstance().getProperty("crd_datamapping_"+entry.getKey()).propertyType().referencedInstanceType();
//				ConsistencyRuleType crt = (ConsistencyRuleType)type;
//				assertTrue(ConsistencyUtils.crdValid(crt));
//				String eval = (String) crt.ruleEvaluations().get().stream()
//						.map(rule -> ((Rule)rule).result()+"" )
//						.collect(Collectors.joining(",","[","]"));
//				System.out.println("Checking "+crt.name() +" Result: "+ eval);
//			});
//			ProcessDefinition pd = td.getProcess() !=null ? td.getProcess().getDefinition() : (ProcessDefinition)td.getDefinition();
//			td.getDefinition().getQAConstraints().stream().forEach(entry -> {
//				//InstanceType type = td.getInstance().getProperty(ProcessStep.getQASpecId(entry, ProcessStep.getOrCreateDesignSpaceInstanceType(ws, td.getDefinition()))).propertyType().referencedInstanceType();
//				String id = ProcessStep.getQASpecId(entry, pd);
//				ConstraintWrapper cw = WrapperCache.getWrappedInstance(ConstraintWrapper.class, (Instance) td.getInstance().getPropertyAsMap(ProcessStep.CoreProperties.qaState.toString()).get(id));
//				ConsistencyRuleType crt = (ConsistencyRuleType)cw.getCr().getInstanceType();
//				assertTrue(ConsistencyUtils.crdValid(crt));
//				String eval = (String) crt.ruleEvaluations().get().stream()
//								.map(rule -> ((Rule)rule).result()+"" )
//								.collect(Collectors.joining(",","[","]"));
//				System.out.println("Checking "+crt.name() +" Result: "+ eval);
//				
//			});
//			for (Conditions condition : Conditions.values()) {
//				if (td.getDefinition().getCondition(condition).isPresent()) {
//					InstanceType type = td.getInstance().getProperty(condition.toString()).propertyType().referencedInstanceType();
//					ConsistencyRuleType crt = (ConsistencyRuleType)type;
//					assertTrue(ConsistencyUtils.crdValid(crt));
//					String eval = (String) crt.ruleEvaluations().get().stream()
//							.map(rule -> ((Rule)rule).result()+"" )
//							.collect(Collectors.joining(",","[","]"));
//					System.out.println("Checking "+crt.name() +" Result: "+ eval);
//				}	
//			}
//	});
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
