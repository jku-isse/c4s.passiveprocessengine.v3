package at.jku.isse.passiveprocessengine.instance.legacy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import at.jku.isse.designspace.core.model.Instance;
import at.jku.isse.designspace.core.model.InstanceType;
import at.jku.isse.designspace.core.model.Workspace;
import at.jku.isse.designspace.core.service.WorkspaceService;
import at.jku.isse.designspace.rule.checker.ArlRuleEvaluator;
import at.jku.isse.designspace.rule.service.RuleService;
import at.jku.isse.passiveprocessengine.definition.activeobjects.ProcessDefinition;
import at.jku.isse.passiveprocessengine.definition.serialization.JsonDefinitionSerializer;
import at.jku.isse.passiveprocessengine.demo.TestArtifacts;
import at.jku.isse.passiveprocessengine.demo.TestArtifacts.JiraStates;
import at.jku.isse.passiveprocessengine.instance.InstanceTests;
import at.jku.isse.passiveprocessengine.instance.ProcessInstanceChangeListener;
import at.jku.isse.passiveprocessengine.instance.ProcessInstanceChangeProcessor;
import at.jku.isse.passiveprocessengine.instance.StepLifecycle.State;
import at.jku.isse.passiveprocessengine.instance.activeobjects.ProcessInstance;
import at.jku.isse.passiveprocessengine.instance.activeobjects.ProcessStep;
import at.jku.isse.passiveprocessengine.instance.messages.EventDistributor;
import at.jku.isse.passiveprocessengine.instance.messages.WorkspaceListenerSequencer;
import at.jku.isse.passiveprocessengine.monitoring.CurrentSystemTimeProvider;
import at.jku.isse.passiveprocessengine.monitoring.ProcessQAStatsMonitor;

@ExtendWith(SpringExtension.class)
@SpringBootTest
class DeviationTest {

	static Workspace ws;
	static InstanceType typeJira;
	ProcessInstanceChangeListener picp;
	static JsonDefinitionSerializer json = new JsonDefinitionSerializer();
	static ProcessQAStatsMonitor monitor;
	
	@BeforeEach
	void setup() throws Exception {
		RuleService.setEvaluator(new ArlRuleEvaluator());
		ws = WorkspaceService.createWorkspace("test", WorkspaceService.PUBLIC_WORKSPACE, WorkspaceService.ANY_USER, null, true, false);
		//ws = WorkspaceService.PUBLIC_WORKSPACE;
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
	void testReactivationLeadsToInflowViolation() throws Exception {
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
		
		// this should be sufficient to complete step1
		TestArtifacts.setStateToJiraInstance(jiraB, JiraStates.InProgress);
		ws.concludeTransaction();
		// now step1 should be back in Active and the second one not ready:
		System.out.println(proc);
		proc.getProcessSteps().stream().forEach(step -> System.out.println(step));
		proc.getDecisionNodeInstances().stream().forEach(dni -> System.out.println(dni));
		
		assert(proc.getProcessSteps().stream()
				.filter(step -> step.getDefinition().getName().equals("sd1") )
				.allMatch(step -> (step.getOutput("jiraOut").size() == 2) && step.getActualLifecycleState().equals(State.ACTIVE) ));
		assert(proc.getProcessSteps().stream()
				.filter(step -> step.getDefinition().getName().equals("sd2") )
				.allMatch(step -> (step.getInput("jiraIn").size() == 2) && step.getActualLifecycleState().equals(State.ACTIVE) ) );
		
		assert(proc.getDecisionNodeInstances().stream()
				.filter(dni -> dni.getDefinition().getName().equals("dnd2"))
				.allMatch(dni -> dni.isInflowFulfilled()==false)
				);
		
		// now lets complete also step 2:
		TestArtifacts.setStateToJiraInstance(jiraB, JiraStates.Closed);
		TestArtifacts.setStateToJiraInstance(jiraC, JiraStates.Closed);
		ws.concludeTransaction();
		System.out.println(proc);
		proc.getProcessSteps().stream().forEach(step -> System.out.println(step));
		proc.getDecisionNodeInstances().stream().forEach(dni -> System.out.println(dni));
	}

	@Test
	void testPrematureOutput() throws Exception {
		
		ProcessDefinition procDef = TestProcesses.getSingleStepProcessDefinitionWithOutput(ws);
		Instance jiraA = TestArtifacts.getJiraInstance(ws, "jiraA");
		Instance jiraB =  TestArtifacts.getJiraInstance(ws, "jiraB");		
		ProcessInstance proc = ProcessInstance.getInstance(ws, procDef);
		proc.addInput("jiraIn", jiraA);
		
		ws.concludeTransaction();
		InstanceTests.printFullProcessToLog(proc);
		assert(proc.getProcessSteps().stream()
				.filter(step -> step.getDefinition().getName().equals("sd1") )
				.allMatch(step -> (step.getOutput("jiraOut").size() == 0) && step.getActualLifecycleState().equals(State.AVAILABLE) ));
		
		TestArtifacts.addParentToJira(jiraA, jiraB);
		ws.concludeTransaction();
		InstanceTests.printFullProcessToLog(proc);
		assert(proc.getProcessSteps().stream()
				.filter(step -> step.getDefinition().getName().equals("sd1") )
				.allMatch(step -> (step.getOutput("jiraOut").size() == 1) && step.getActualLifecycleState().equals(State.ACTIVE) ));
		
	}
	
	@Test
	void testDirectCancel() throws Exception {
		
		ProcessDefinition procDef = TestProcesses.getSingleStepProcessDefinitionWithOutput(ws);
		Instance jiraA = TestArtifacts.getJiraInstance(ws, "jiraA");
		Instance jiraB =  TestArtifacts.getJiraInstance(ws, "jiraB");		
		ProcessInstance proc = ProcessInstance.getInstance(ws, procDef);
		proc.addInput("jiraIn", jiraA);
		
		ws.concludeTransaction();
		ProcessStep sd1 = proc.getProcessSteps().stream()
				.filter(step -> step.getDefinition().getName().equals("sd1") ).findAny().get(); 
		// now we directly manipulate the step
		sd1.setCancelConditionsFulfilled(true);
		assert(sd1.getActualLifecycleState() == State.CANCELED);
		assert(sd1.getExpectedLifecycleState() == State.CANCELED);
		
		sd1.setPostConditionsFulfilled(true);
		assert(sd1.getActualLifecycleState() == State.COMPLETED);
		assert(sd1.getExpectedLifecycleState() == State.CANCELED);
		
		sd1.setPostConditionsFulfilled(false);
		assert(sd1.getActualLifecycleState() == State.AVAILABLE);
		assert(sd1.getExpectedLifecycleState() == State.CANCELED);
		
		sd1.setCancelConditionsFulfilled(false);
		assert(sd1.getActualLifecycleState() == State.AVAILABLE);
		assert(sd1.getExpectedLifecycleState() == State.AVAILABLE);
	}
	
	@Test
	void testDirectHalt() throws Exception {
		
		ProcessDefinition procDef = TestProcesses.getSingleStepProcessDefinitionWithOutput(ws);
		Instance jiraA = TestArtifacts.getJiraInstance(ws, "jiraA");
		Instance jiraB =  TestArtifacts.getJiraInstance(ws, "jiraB");		
		ProcessInstance proc = ProcessInstance.getInstance(ws, procDef);
		proc.addInput("jiraIn", jiraA);
		
		ws.concludeTransaction();
		ProcessStep sd1 = proc.getProcessSteps().stream()
				.filter(step -> step.getDefinition().getName().equals("sd1") ).findAny().get(); 
		// now we directly manipulate the step
		sd1.setWorkExpected(false);
		assert(sd1.getActualLifecycleState() == State.NO_WORK_EXPECTED);
		assert(sd1.getExpectedLifecycleState() == State.NO_WORK_EXPECTED);
		
		sd1.setCancelConditionsFulfilled(true);
		assert(sd1.getActualLifecycleState() == State.CANCELED);
		assert(sd1.getExpectedLifecycleState() == State.NO_WORK_EXPECTED);
		
		sd1.setWorkExpected(true);
		assert(sd1.getActualLifecycleState() == State.CANCELED);
		assert(sd1.getExpectedLifecycleState() == State.CANCELED);					
	}
	
	@Test
	void testXOR() throws Exception {
		ProcessDefinition procDef = TestProcesses.getSimpleXORDefinition(ws);
		Instance jiraA = TestArtifacts.getJiraInstance(ws, "jiraA");
		Instance jiraB =  TestArtifacts.getJiraInstance(ws, "jiraB");
		Instance jiraC =  TestArtifacts.getJiraInstance(ws, "jiraC");
		ProcessInstance proc = ProcessInstance.getInstance(ws, procDef);
		proc.addInput("jiraIn", jiraA);
		
		ws.concludeTransaction();
		InstanceTests.printFullProcessToLog(proc);
		ProcessStep alt1 = proc.getProcessSteps().stream()
				.filter(step -> step.getDefinition().getName().equals("alt1") ).findAny().get(); 
		ProcessStep alt2 = proc.getProcessSteps().stream()
				.filter(step -> step.getDefinition().getName().equals("alt2") ).findAny().get();
		assert(alt1.getActualLifecycleState() == State.ENABLED);
		assert(alt1.getExpectedLifecycleState() == State.ENABLED);
		assert(alt2.getActualLifecycleState() == State.ENABLED);
		assert(alt2.getExpectedLifecycleState() == State.ENABLED);
		
		TestArtifacts.addJiraToJira(jiraA, jiraB);		
		ws.concludeTransaction();
		InstanceTests.printFullProcessToLog(proc);
		assert(alt1.getActualLifecycleState() == State.ENABLED);
		assert(alt1.getExpectedLifecycleState() == State.ENABLED);	
		assert(alt2.getExpectedLifecycleState() == State.ACTIVE);
		
		
		TestArtifacts.setStateToJiraInstance(jiraB, JiraStates.InProgress);
		ws.concludeTransaction();
		InstanceTests.printFullProcessToLog(proc);		
		assert(alt1.getExpectedLifecycleState() == State.NO_WORK_EXPECTED);	
		assert(alt2.getExpectedLifecycleState() == State.COMPLETED);
		
		TestArtifacts.addJiraToJiraBug(jiraA, jiraC);
		ws.concludeTransaction();
		InstanceTests.printFullProcessToLog(proc);		
		assert(alt1.getActualLifecycleState() == State.ACTIVE);	
		assert(alt2.getExpectedLifecycleState() == State.COMPLETED);
		
		TestArtifacts.setStateToJiraInstance(jiraC, JiraStates.Closed);
		ws.concludeTransaction();
		InstanceTests.printFullProcessToLog(proc);	
		assert(alt1.getActualLifecycleState() == State.COMPLETED);	
		assert(alt1.getExpectedLifecycleState() == State.NO_WORK_EXPECTED);	
		assert(alt2.getExpectedLifecycleState() == State.COMPLETED);
		
		TestArtifacts.setStateToJiraInstance(jiraB, JiraStates.Released);
		ws.concludeTransaction();
		InstanceTests.printFullProcessToLog(proc);
		assert(alt1.getActualLifecycleState() == State.COMPLETED);	
		assert(alt1.getExpectedLifecycleState() == State.COMPLETED);	
		assert(alt2.getExpectedLifecycleState() == State.CANCELED);
		
	}
	
}
