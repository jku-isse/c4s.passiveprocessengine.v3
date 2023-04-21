package at.jku.isse.designspace.passiveprocessengine.instance;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import at.jku.isse.designspace.core.model.Instance;
import at.jku.isse.designspace.core.model.InstanceType;
import at.jku.isse.designspace.core.model.Workspace;
import at.jku.isse.designspace.core.service.WorkspaceService;
import at.jku.isse.designspace.rule.arl.repair.Operator;
import at.jku.isse.designspace.rule.arl.repair.RepairNode;
import at.jku.isse.designspace.rule.arl.repair.RestrictionNode;
import at.jku.isse.designspace.rule.checker.ArlRuleEvaluator;
import at.jku.isse.designspace.rule.model.ConsistencyRuleType;
import at.jku.isse.designspace.rule.service.RuleService;
import at.jku.isse.passiveprocessengine.definition.ProcessDefinition;
import at.jku.isse.passiveprocessengine.definition.serialization.JsonDefinitionSerializer;
import at.jku.isse.passiveprocessengine.demo.TestArtifacts;
import at.jku.isse.passiveprocessengine.demo.TestProcesses;
import at.jku.isse.passiveprocessengine.demo.TestArtifacts.JiraStates;
import at.jku.isse.passiveprocessengine.instance.ProcessInstance;
import at.jku.isse.passiveprocessengine.instance.ProcessInstanceChangeProcessor;
import at.jku.isse.passiveprocessengine.instance.ProcessStep;
import at.jku.isse.passiveprocessengine.instance.StepLifecycle.State;
import at.jku.isse.passiveprocessengine.instance.messages.EventDistributor;
import at.jku.isse.passiveprocessengine.instance.messages.WorkspaceListenerSequencer;
import at.jku.isse.passiveprocessengine.monitoring.CurrentSystemTimeProvider;
import at.jku.isse.passiveprocessengine.monitoring.ProcessQAStatsMonitor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ExtendWith(SpringExtension.class)
@SpringBootTest
class TemporalConstraintTest {

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
	
	@Test
	void testTemporalConstraintEarlyAdding() throws Exception {
		Instance jiraB =  TestArtifacts.getJiraInstance(ws, "jiraB");
		Instance jiraC = TestArtifacts.getJiraInstance(ws, "jiraC");		
		Instance jiraA = TestArtifacts.getJiraInstance(ws, "jiraA");
		TestArtifacts.addJiraToJira(jiraA, jiraB);
		TestArtifacts.addJiraToJira(jiraA, jiraC);	
		
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
		
		// now lets progress toward fulfillment but
		
		TestArtifacts.setStateToJiraInstance(jiraB, JiraStates.Released);
		TestArtifacts.setStateToJiraInstance(jiraC, JiraStates.ReadyForReview);
		ws.concludeTransaction();
		InstanceTests.printFullProcessToLog(proc);
		// now we are still enabled
		assert(sd1.getActualLifecycleState() == State.ENABLED);
				
		
		// now lets complete step1:		
		TestArtifacts.setStateToJiraInstance(jiraC, JiraStates.Released);
		ws.concludeTransaction();
		InstanceTests.printFullProcessToLog(proc);
		assert(sd1.getActualLifecycleState() == State.COMPLETED);
	}
	
	@Test
	void testTemporalConstraintDeviatingAndRepairingEarlyAdding() throws Exception {
		//Instance jiraB =  TestArtifacts.getJiraInstance(ws, "jiraB");
		Instance jiraC = TestArtifacts.getJiraInstance(ws, "jiraC");		
		Instance jiraA = TestArtifacts.getJiraInstance(ws, "jiraA");
		//TestArtifacts.addJiraToJira(jiraA, jiraB);
		TestArtifacts.addJiraToJira(jiraA, jiraC);	
		
		ProcessDefinition procDef = TestProcesses.getSimpleTemporalProcessDefinitionWithoutQA(ws);
		ProcessInstance proc = ProcessInstance.getInstance(ws, procDef);
		proc.addInput("jiraIn", jiraA);
		//TestArtifacts.setStateToJiraInstance(jiraB, JiraStates.ReadyForReview);
		
		ws.concludeTransaction();
		ProcessStep sd1 = proc.getProcessSteps().stream()
				.filter(step -> step.getDefinition().getName().equals("step1") ).findAny().get(); 		
		// now we are enabled
		assert(sd1.getActualLifecycleState() == State.ENABLED);
		
		// now lets progress toward fulfillment but
		
		//TestArtifacts.setStateToJiraInstance(jiraB, JiraStates.Released);
		TestArtifacts.setStateToJiraInstance(jiraC, JiraStates.ReadyForReview);
		ws.concludeTransaction();
		InstanceTests.printFullProcessToLog(proc);
		// now we are still enabled
		assert(sd1.getActualLifecycleState() == State.ENABLED);
				
		
		// now lets progress away from fulfillment
		TestArtifacts.setStateToJiraInstance(jiraC, JiraStates.Closed);
		ws.concludeTransaction();
		InstanceTests.printFullProcessToLog(proc);
		assert(sd1.getActualLifecycleState() == State.ENABLED);
				
		TestArtifacts.setStateToJiraInstance(jiraC, JiraStates.Open);
		ws.concludeTransaction();
		assert(sd1.getActualLifecycleState() == State.ENABLED);
		
		TestArtifacts.setStateToJiraInstance(jiraC, JiraStates.ReadyForReview);
		ws.concludeTransaction();
		assert(sd1.getActualLifecycleState() == State.ENABLED);
		
		// now lets complete step1:		
		TestArtifacts.setStateToJiraInstance(jiraC, JiraStates.Released);
		ws.concludeTransaction();
		InstanceTests.printFullProcessToLog(proc);
		assert(sd1.getActualLifecycleState() == State.COMPLETED);
	}
	
	@Test
	void testPureConstraint() {
		//Instance jiraA = TestArtifacts.getJiraInstance(ws, "jiraA");		
		Instance jiraC = TestArtifacts.getJiraInstance(ws, "jiraC");		
		//TestArtifacts.addJiraToJira(jiraA, jiraC);	
		
		ConsistencyRuleType crt = ConsistencyRuleType.create(ws, typeJira, "TempTest", "eventually(self.state = 'ReadyForReview') and eventually( always( self.state = 'ReadyForReview' ,  eventually(self.state = 'Released')) )");
		ws.concludeTransaction();
		TestArtifacts.setStateToJiraInstance(jiraC, JiraStates.ReadyForReview);
		ws.concludeTransaction();
						
		TestArtifacts.setStateToJiraInstance(jiraC, JiraStates.Closed);
		ws.concludeTransaction();
				
		TestArtifacts.setStateToJiraInstance(jiraC, JiraStates.Open);
		ws.concludeTransaction();
		
		TestArtifacts.setStateToJiraInstance(jiraC, JiraStates.ReadyForReview);
		ws.concludeTransaction();
		
		// now lets complete step1:		
		TestArtifacts.setStateToJiraInstance(jiraC, JiraStates.Released);
		ws.concludeTransaction();
		
	}
	
	@Test
	void testTemporalConstraintComplexEarlyAdding() throws Exception {
		Instance jiraB =  TestArtifacts.getJiraInstance(ws, "jiraB");
		Instance jiraC = TestArtifacts.getJiraInstance(ws, "jiraC");		
		Instance jiraA = TestArtifacts.getJiraInstance(ws, "jiraA");
		TestArtifacts.addJiraToJira(jiraA, jiraB);
		TestArtifacts.addJiraToJira(jiraA, jiraC);	
		Instance jiraD =  TestArtifacts.getJiraInstance(ws, "jiraD");
		Instance jiraE =  TestArtifacts.getJiraInstance(ws, "jiraE");		
		TestArtifacts.addJiraToJira(jiraD, jiraE);
		
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
		
		// now lets progress toward fulfillment but
		TestArtifacts.setStateToJiraInstance(jiraE, JiraStates.ReadyForReview);
		ws.concludeTransaction();
		assert(sd1.getActualLifecycleState() == State.ENABLED);
		
		TestArtifacts.setStateToJiraInstance(jiraE, JiraStates.Released);
		ws.concludeTransaction();
		assert(sd1.getActualLifecycleState() == State.ENABLED);
		
		TestArtifacts.setStateToJiraInstance(jiraB, JiraStates.Released);
		TestArtifacts.setStateToJiraInstance(jiraC, JiraStates.ReadyForReview);
		ws.concludeTransaction();
		InstanceTests.printFullProcessToLog(proc);
		// now we are still enabled
		assert(sd1.getActualLifecycleState() == State.ENABLED);
				
		
		// now lets complete step1:		
		TestArtifacts.setStateToJiraInstance(jiraC, JiraStates.Released);
		ws.concludeTransaction();
		InstanceTests.printFullProcessToLog(proc);
		assert(sd1.getActualLifecycleState() == State.COMPLETED);
	}
	
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
	void testTemporalConstraintEarlyAddingSequenceAbsence() throws Exception {
		Instance jiraB =  TestArtifacts.getJiraInstance(ws, "jiraB");
		Instance jiraC = TestArtifacts.getJiraInstance(ws, "jiraC");		
		Instance jiraA = TestArtifacts.getJiraInstance(ws, "jiraA");
		TestArtifacts.addJiraToJira(jiraA, jiraB);
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
				
		
		// now lets complete step1:		
		TestArtifacts.setStateToJiraInstance(jiraC, JiraStates.Released);
		ws.concludeTransaction();
		InstanceTests.printFullProcessToLog(proc);
		assert(sd1.getActualLifecycleState() == State.COMPLETED);
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
	
	@Test
	void testTemporalConstraintEarlyAddingDeviatingFromSequenceAbsence() throws Exception {
		Instance jiraB =  TestArtifacts.getJiraInstance(ws, "jiraB");
		Instance jiraC = TestArtifacts.getJiraInstance(ws, "jiraC");		
		Instance jiraA = TestArtifacts.getJiraInstance(ws, "jiraA");
		TestArtifacts.addJiraToJira(jiraA, jiraB);	
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
		
		//complete for C, but we should still be ENABLED as B violated the constraint
			
		TestArtifacts.setStateToJiraInstance(jiraC, JiraStates.Released);
		ws.concludeTransaction();
		InstanceTests.printFullProcessToLog(proc);
		assert(sd1.getActualLifecycleState() == State.ENABLED);
	}
	
	@Test
	public void testDeviationFromSequenceAbsence() {		
		Instance jiraC = TestArtifacts.getJiraInstance(ws, "jiraC", "jiraA");	
		TestArtifacts.setStateToJiraInstance(jiraC, JiraStates.Released);
		ConsistencyRuleType crt = ConsistencyRuleType.create(ws, typeJira, "TempTest2", "self.requirementIDs.size() >= 0 and eventually(self.state = 'Released', always(self.state = 'Released') or not ( eventually(self.state <> 'Released' , self.state = 'Released'))) ");
		ws.concludeTransaction();
		
		assert(crt.consistencyRuleEvaluation(jiraC).isConsistent()==true);
		
		TestArtifacts.setStateToJiraInstance(jiraC, JiraStates.ReadyForReview);
		ws.concludeTransaction();
		assert(crt.consistencyRuleEvaluation(jiraC).isConsistent()==true);
		
		TestArtifacts.setStateToJiraInstance(jiraC, JiraStates.Released);
		ws.concludeTransaction();
		
		TestArtifacts.addReqIdsToJira(jiraC, "jiraB");
		ws.concludeTransaction();
		
		TestArtifacts.setStateToJiraInstance(jiraC, JiraStates.Released);
		ws.concludeTransaction();
		assert(crt.consistencyRuleEvaluation(jiraC).isConsistent()==false);
	}
	
	@Test
	public void testDeviationFromSequenceAbsence1() {		
		Instance jiraC = TestArtifacts.getJiraInstance(ws, "jiraC");			
		TestArtifacts.setStateToJiraInstance(jiraC, JiraStates.Released);
		ConsistencyRuleType crt = ConsistencyRuleType.create(ws, typeJira, "TempTest2", " always(self.state = 'Released') or not ( eventually(self.state <> 'Released' , self.state = 'Released')) ");
		ws.concludeTransaction();
		
		assert(crt.consistencyRuleEvaluation(jiraC).isConsistent()==true);
		
		TestArtifacts.setStateToJiraInstance(jiraC, JiraStates.ReadyForReview);
		ws.concludeTransaction();
		assert(crt.consistencyRuleEvaluation(jiraC).isConsistent()==true);
		
		TestArtifacts.setStateToJiraInstance(jiraC, JiraStates.Released);
		ws.concludeTransaction();
		assert(crt.consistencyRuleEvaluation(jiraC).isConsistent()==false);
	}
	
	@Test
	public void testDeviationFromAlwaysSequence() {		
		ConsistencyRuleType crt = ConsistencyRuleType.create(ws, typeJira, "TempTest2", " always(self.state = 'Released')");
		ws.concludeTransaction();
		
		Instance jiraC = TestArtifacts.getJiraInstance(ws, "jiraC");
		TestArtifacts.setStateToJiraInstance(jiraC, JiraStates.Released);
		ws.concludeTransaction();
		assert(crt.consistencyRuleEvaluation(jiraC).isConsistent()==true);
		
		TestArtifacts.setStateToJiraInstance(jiraC, JiraStates.ReadyForReview);
		ws.concludeTransaction();
		assert(crt.consistencyRuleEvaluation(jiraC).isConsistent()==false);
		
		TestArtifacts.setStateToJiraInstance(jiraC, JiraStates.Released);
		ws.concludeTransaction();
		assert(crt.consistencyRuleEvaluation(jiraC).isConsistent()==false);
	}
	
	@Test
	public void testDeviationFromEventuallyAlwaysSequence() {		
		ConsistencyRuleType crt = ConsistencyRuleType.create(ws, typeJira, "TempTest2", " eventually(always(self.state = 'Released'))");
		ws.concludeTransaction();
		
		Instance jiraC = TestArtifacts.getJiraInstance(ws, "jiraC");
		TestArtifacts.setStateToJiraInstance(jiraC, JiraStates.Released);
		ws.concludeTransaction();
		assert(crt.consistencyRuleEvaluation(jiraC).isConsistent()==true);
		
		TestArtifacts.setStateToJiraInstance(jiraC, JiraStates.ReadyForReview);
		ws.concludeTransaction();
		assert(crt.consistencyRuleEvaluation(jiraC).isConsistent()==false);
		
		TestArtifacts.setStateToJiraInstance(jiraC, JiraStates.Released);
		ws.concludeTransaction();
		assert(crt.consistencyRuleEvaluation(jiraC).isConsistent()==true);
	}
	
	@Test
	public void testDeviationFromSequenceAbsence2() {		
		Instance jiraC = TestArtifacts.getJiraInstance(ws, "jiraC");			
		
		ConsistencyRuleType crt = ConsistencyRuleType.create(ws, typeJira, "TempTest3", "eventually(self.state = 'Released') and eventually(self.state = 'Released' , always(self.state = 'Released') or not (next( eventually(self.state <> 'Released' , self.state = 'Released') ) )) ");
		ws.concludeTransaction();
		TestArtifacts.setStateToJiraInstance(jiraC, JiraStates.ReadyForReview);
		ws.concludeTransaction();
		assert(crt.consistencyRuleEvaluation(jiraC).isConsistent()==false);
						
		TestArtifacts.setStateToJiraInstance(jiraC, JiraStates.Open);
		ws.concludeTransaction();
		assert(crt.consistencyRuleEvaluation(jiraC).isConsistent()==false);
		
		TestArtifacts.setStateToJiraInstance(jiraC, JiraStates.Released);
		ws.concludeTransaction();
		assert(crt.consistencyRuleEvaluation(jiraC).isConsistent()==true);
		
		TestArtifacts.setStateToJiraInstance(jiraC, JiraStates.ReadyForReview);
		ws.concludeTransaction();
		assert(crt.consistencyRuleEvaluation(jiraC).isConsistent()==true);
		
		TestArtifacts.setStateToJiraInstance(jiraC, JiraStates.Released);
		ws.concludeTransaction();
		assert(crt.consistencyRuleEvaluation(jiraC).isConsistent()==false);
	}
}
