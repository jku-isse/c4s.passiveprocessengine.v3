package at.jku.isse.passiveprocessengine.instance.legacy;

import java.util.Optional;

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
import at.jku.isse.designspace.rule.model.ConsistencyRule;
import at.jku.isse.designspace.rule.model.ConsistencyRuleType;
import at.jku.isse.designspace.rule.service.RuleService;
import at.jku.isse.passiveprocessengine.definition.activeobjects.ProcessDefinition;
import at.jku.isse.passiveprocessengine.definition.serialization.JsonDefinitionSerializer;
import at.jku.isse.passiveprocessengine.demo.TestArtifacts;
import at.jku.isse.passiveprocessengine.demo.TestProcesses;
import at.jku.isse.passiveprocessengine.demo.TestArtifacts.JiraStates;
import at.jku.isse.passiveprocessengine.instance.InstanceTests;
import at.jku.isse.passiveprocessengine.instance.ProcessInstanceChangeProcessor;
import at.jku.isse.passiveprocessengine.instance.StepLifecycle.Conditions;
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
class TemporalConstraintInTimeRepairTests {

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
	void testRepairTemporalConstraintEarlyAdding() throws Exception {
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
		// lets checkout repairs:		
		Optional<ConsistencyRule> crOpt = sd1.getConditionStatus(Conditions.POSTCONDITION);
		RepairNode repairTree = RuleService.repairTree(crOpt.get());
		RepairTests.printRepairActions(repairTree);		
		
		TestArtifacts.setStateToJiraInstance(jiraB, JiraStates.Released);
		TestArtifacts.setStateToJiraInstance(jiraC, JiraStates.ReadyForReview);
		ws.concludeTransaction();
		InstanceTests.printFullProcessToLog(proc);
		// now we are still enabled
		crOpt = sd1.getConditionStatus(Conditions.POSTCONDITION);
		repairTree = RuleService.repairTree(crOpt.get());
		RepairTests.printRepairActions(repairTree);
		assert(sd1.getActualLifecycleState() == State.ENABLED);
				
		
		// now lets complete step1:		
		TestArtifacts.setStateToJiraInstance(jiraC, JiraStates.Released);
		ws.concludeTransaction();
		InstanceTests.printFullProcessToLog(proc);
		assert(sd1.getActualLifecycleState() == State.COMPLETED);
	}
	
	@Test
	void testRepairTemporalQA() throws Exception {
		Instance jiraB =  TestArtifacts.getJiraInstance(ws, "jiraB");
		Instance jiraC = TestArtifacts.getJiraInstance(ws, "jiraC");		
		Instance jiraA = TestArtifacts.getJiraInstance(ws, "jiraA");
		TestArtifacts.addJiraToJira(jiraA, jiraB);
		TestArtifacts.addJiraToJira(jiraA, jiraC);	
		
		ProcessDefinition procDef = TestProcesses.getSimpleTemporalProcessDefinitionWithQA(ws);
		ProcessInstance proc = ProcessInstance.getInstance(ws, procDef);
		proc.addInput("jiraIn", jiraA);
		TestArtifacts.setStateToJiraInstance(jiraB, JiraStates.ReadyForReview);
		
		ws.concludeTransaction();
		ProcessStep sd1 = proc.getProcessSteps().stream()
				.filter(step -> step.getDefinition().getName().equals("step1") ).findAny().get(); 
		InstanceTests.printFullProcessToLog(proc);
		// now we are enabled
		assert(sd1.getActualLifecycleState() == State.ENABLED);		
		// lets checkout repairs:		
		Optional<ConsistencyRule> crOpt = sd1.getQAstatus().stream()
					.map(cw -> cw.getRuleResult())
					.findAny(); //we only have one here
		RepairNode repairTree = RuleService.repairTree(crOpt.get());
		RepairTests.printRepairActions(repairTree);		
		
		// now simulate some changes
		TestArtifacts.setStateToJiraInstance(jiraB, JiraStates.Released);
		TestArtifacts.setStateToJiraInstance(jiraC, JiraStates.ReadyForReview);
		ws.concludeTransaction();
		
		// and now another round of revision
		TestArtifacts.setStateToJiraInstance(jiraB, JiraStates.Open);
		TestArtifacts.setStateToJiraInstance(jiraC, JiraStates.Open);	
		ws.concludeTransaction();
		
		InstanceTests.printFullProcessToLog(proc);
		// now we are still enabled
		crOpt = sd1.getQAstatus().stream()
				.map(cw -> cw.getRuleResult())
				.findAny(); //we only have one here
		repairTree = RuleService.repairTree(crOpt.get());
		RepairTests.printRepairActions(repairTree);

				

	}
	
	
}
