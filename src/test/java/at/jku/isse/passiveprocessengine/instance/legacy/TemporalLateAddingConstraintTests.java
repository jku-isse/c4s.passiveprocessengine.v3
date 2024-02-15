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
import at.jku.isse.passiveprocessengine.demo.TestProcesses;
import at.jku.isse.passiveprocessengine.instance.InstanceTests;
import at.jku.isse.passiveprocessengine.instance.ProcessInstanceChangeProcessor;
import at.jku.isse.passiveprocessengine.instance.StepLifecycle.State;
import at.jku.isse.passiveprocessengine.instance.activeobjects.ProcessInstance;
import at.jku.isse.passiveprocessengine.instance.activeobjects.ProcessStep;
import at.jku.isse.passiveprocessengine.instance.messages.EventDistributor;
import at.jku.isse.passiveprocessengine.instance.messages.WorkspaceListenerSequencer;
import at.jku.isse.passiveprocessengine.monitoring.CurrentSystemTimeProvider;
import at.jku.isse.passiveprocessengine.monitoring.ProcessQAStatsMonitor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ExtendWith(SpringExtension.class)
@SpringBootTest
class TemporalLateAddingConstraintTests {

	static Workspace ws;
	static InstanceType typeJira;
	ProcessInstanceChangeProcessor picp;
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
	
	/*
	 * ALL THESE TEST ARE EXPECTED TO FAIL AS WE DONT CONSIDER THE HISTORY YET WHEN AN INSTANCE IS ADDED LATE TO THE RULE CONTEXT
	 */
	
	
	@Test
	void testTemporalConstraintLateAdding2() throws Exception {
		Instance jiraB =  TestArtifacts.getJiraInstance(ws, "jiraB");
		Instance jiraC = TestArtifacts.getJiraInstance(ws, "jiraC");		
		Instance jiraA = TestArtifacts.getJiraInstance(ws, "jiraA");
		TestArtifacts.addJiraToJira(jiraA, jiraB);
				
		
		ProcessDefinition procDef = TestProcesses.getSimpleTemporalProcessDefinitionWithoutQA(ws);
		ProcessInstance proc = ProcessInstance.getInstance(ws, procDef);
		proc.addInput("jiraIn", jiraA);
		TestArtifacts.setStateToJiraInstance(jiraB, JiraStates.ReadyForReview);
		
		ws.concludeTransaction();
		ProcessStep sd1 = proc.getProcessSteps().stream()
				.filter(step -> step.getDefinition().getName().equals("step1") ).findAny().get(); 
		InstanceTests.printFullProcessToLog(proc);
		// now we are enabled
		assert(sd1.getActualLifecycleState() == State.ENABLED);
		
		// now lets add C		
		TestArtifacts.setStateToJiraInstance(jiraC, JiraStates.ReadyForReview);
		ws.concludeTransaction();
		TestArtifacts.setStateToJiraInstance(jiraC, JiraStates.Released);
		ws.concludeTransaction();
		InstanceTests.printFullProcessToLog(proc);
		// now we are still enabled
		assert(sd1.getActualLifecycleState() == State.ENABLED);
				
		
		// now lets complete step1:		
		TestArtifacts.addJiraToJira(jiraA, jiraC);
		TestArtifacts.setStateToJiraInstance(jiraB, JiraStates.Released);		
		ws.concludeTransaction();
		InstanceTests.printFullProcessToLog(proc);
		assert(sd1.getActualLifecycleState() == State.COMPLETED);
	}

	@Test
	void testTemporalConstraintLateAdding1() throws Exception {
		Instance jiraB =  TestArtifacts.getJiraInstance(ws, "jiraB");
		Instance jiraC = TestArtifacts.getJiraInstance(ws, "jiraC");		
		Instance jiraA = TestArtifacts.getJiraInstance(ws, "jiraA");
		TestArtifacts.addJiraToJira(jiraA, jiraB);
		
		ProcessDefinition procDef = TestProcesses.getSimpleTemporalProcessDefinitionWithoutQA(ws);
		ProcessInstance proc = ProcessInstance.getInstance(ws, procDef);
		proc.addInput("jiraIn", jiraA);
		TestArtifacts.setStateToJiraInstance(jiraB, JiraStates.ReadyForReview);
		ws.concludeTransaction();
		ProcessStep sd1 = proc.getProcessSteps().stream()
				.filter(step -> step.getDefinition().getName().equals("step1") ).findAny().get(); 
		InstanceTests.printFullProcessToLog(proc);
		// now we are enabled
		assert(sd1.getActualLifecycleState() == State.ENABLED);
		
		// now lets update jiraC		
		TestArtifacts.setStateToJiraInstance(jiraC, JiraStates.Closed);		
		ws.concludeTransaction();
		TestArtifacts.setStateToJiraInstance(jiraC, JiraStates.Released);
		ws.concludeTransaction();
		InstanceTests.printFullProcessToLog(proc);
		// now we are still enabled
		assert(sd1.getActualLifecycleState() == State.ENABLED);
				
		
		// due to incorrect state transitions of jiraC we are still ENABLED		
		// we directly detect jiraC currently as incorrect as we dont observe the ready for review state at all
		TestArtifacts.addJiraToJira(jiraA, jiraC);
		TestArtifacts.setStateToJiraInstance(jiraB, JiraStates.Released);		
		ws.concludeTransaction();
		InstanceTests.printFullProcessToLog(proc);
		assert(sd1.getActualLifecycleState() == State.ENABLED);
	}
	
	@Test
	void testTemporalConstraintComplexLateAdding1() throws Exception {
		Instance jiraB =  TestArtifacts.getJiraInstance(ws, "jiraB");
		Instance jiraC = TestArtifacts.getJiraInstance(ws, "jiraC");		
		Instance jiraA = TestArtifacts.getJiraInstance(ws, "jiraA");
		Instance jiraD =  TestArtifacts.getJiraInstance(ws, "jiraD");
		Instance jiraE =  TestArtifacts.getJiraInstance(ws, "jiraE");
		TestArtifacts.addJiraToJira(jiraA, jiraB);
		TestArtifacts.addJiraToJira(jiraD, jiraE);
		
		ProcessDefinition procDef = TestProcesses.getSimpleTemporalProcessDefinitionWithoutQA(ws);
		ProcessInstance proc = ProcessInstance.getInstance(ws, procDef);
		proc.addInput("jiraIn", jiraA);
		proc.addInput("jiraIn", jiraD);
		TestArtifacts.setStateToJiraInstance(jiraB, JiraStates.ReadyForReview);
		TestArtifacts.setStateToJiraInstance(jiraE, JiraStates.ReadyForReview);
		
		ws.concludeTransaction();
		ProcessStep sd1 = proc.getProcessSteps().stream()
				.filter(step -> step.getDefinition().getName().equals("step1") ).findAny().get(); 
		InstanceTests.printFullProcessToLog(proc);
		// now we are enabled
		assert(sd1.getActualLifecycleState() == State.ENABLED);
		
		// now lets update jiraC		
		TestArtifacts.setStateToJiraInstance(jiraC, JiraStates.Closed);		
		ws.concludeTransaction();
		TestArtifacts.setStateToJiraInstance(jiraC, JiraStates.Released);
		TestArtifacts.setStateToJiraInstance(jiraE, JiraStates.Released);
		ws.concludeTransaction();
		InstanceTests.printFullProcessToLog(proc);
		// now we are still enabled
		assert(sd1.getActualLifecycleState() == State.ENABLED);
				
		
		// due to incorrect state transitions of jiraC we are still ENABLED		
		TestArtifacts.addJiraToJira(jiraA, jiraC);
		TestArtifacts.setStateToJiraInstance(jiraB, JiraStates.Released);		
		ws.concludeTransaction();
		InstanceTests.printFullProcessToLog(proc);
		assert(sd1.getActualLifecycleState() == State.ENABLED);
	}
	
	
	@Test
	void testTemporalConstraintLateAddingSequenceAbsence() throws Exception {
		Instance jiraB =  TestArtifacts.getJiraInstance(ws, "jiraB");
		Instance jiraC = TestArtifacts.getJiraInstance(ws, "jiraC");		
		Instance jiraA = TestArtifacts.getJiraInstance(ws, "jiraA");
		
		TestArtifacts.addJiraToJira(jiraA, jiraC);	
		
		ProcessDefinition procDef = TestProcesses.getSimpleTemporalProcessDefinitionWithSequenceAbsence(ws);
		ProcessInstance proc = ProcessInstance.getInstance(ws, procDef);
		proc.addInput("jiraIn", jiraA);
		TestArtifacts.setStateToJiraInstance(jiraB, JiraStates.ReadyForReview);
		
		ws.concludeTransaction();
		ProcessStep sd1 = proc.getProcessSteps().stream()
				.filter(step -> step.getDefinition().getName().equals("step1") ).findAny().get(); 
		InstanceTests.printFullProcessToLog(proc);
		// now we are enabled
		assert(sd1.getActualLifecycleState() == State.ENABLED);
		
		// now lets progress toward fulfillment but
		TestArtifacts.setStateToJiraInstance(jiraB, JiraStates.Released);
		TestArtifacts.setStateToJiraInstance(jiraC, JiraStates.ReadyForReview);
		ws.concludeTransaction();
		InstanceTests.printFullProcessToLog(proc);
		// now we are still enabled
		assert(sd1.getActualLifecycleState() == State.ENABLED);
				
		// now lets deviate with B by setting to something else again and back to released
		TestArtifacts.setStateToJiraInstance(jiraB, JiraStates.Closed);
		ws.concludeTransaction();
		TestArtifacts.setStateToJiraInstance(jiraB, JiraStates.Released);
		ws.concludeTransaction();
		assert(sd1.getActualLifecycleState() == State.ENABLED);
		
		// now lets add B, complete for C, but we should still be ENABLED as B violated the constraint
		TestArtifacts.addJiraToJira(jiraA, jiraB);		
		TestArtifacts.setStateToJiraInstance(jiraC, JiraStates.Released);
		ws.concludeTransaction();
		InstanceTests.printFullProcessToLog(proc);
		assert(sd1.getActualLifecycleState() == State.ENABLED);
	}
	
	
}
