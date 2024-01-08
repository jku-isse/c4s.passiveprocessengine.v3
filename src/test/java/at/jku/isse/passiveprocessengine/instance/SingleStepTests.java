package at.jku.isse.passiveprocessengine.instance;

import static org.junit.jupiter.api.Assertions.*;

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
import at.jku.isse.passiveprocessengine.definition.ProcessDefinition;
import at.jku.isse.passiveprocessengine.definition.activeobjects.DecisionNodeDefinition;
import at.jku.isse.passiveprocessengine.definition.activeobjects.MappingDefinition;
import at.jku.isse.passiveprocessengine.definition.activeobjects.StepDefinition;
import at.jku.isse.passiveprocessengine.definition.serialization.JsonDefinitionSerializer;
import at.jku.isse.passiveprocessengine.demo.TestArtifacts;
import at.jku.isse.passiveprocessengine.instance.ProcessException;
import at.jku.isse.passiveprocessengine.instance.ProcessInstance;
import at.jku.isse.passiveprocessengine.instance.ProcessInstanceChangeProcessor;
import at.jku.isse.passiveprocessengine.instance.ProcessStep;
import at.jku.isse.passiveprocessengine.instance.StepLifecycle.Conditions;
import at.jku.isse.passiveprocessengine.instance.StepLifecycle.State;
import at.jku.isse.passiveprocessengine.instance.messages.EventDistributor;
import at.jku.isse.passiveprocessengine.instance.messages.WorkspaceListenerSequencer;

@ExtendWith(SpringExtension.class)
@SpringBootTest
class SingleStepTests {

	static Workspace ws;
	static InstanceType typeJira;
	ProcessInstanceChangeProcessor picp;
	static JsonDefinitionSerializer json = new JsonDefinitionSerializer();	
	
	@BeforeEach
	void setup() throws Exception {
		RuleService.setEvaluator(new ArlRuleEvaluator());
		ws = WorkspaceService.createWorkspace("test", WorkspaceService.PUBLIC_WORKSPACE, WorkspaceService.ANY_USER, null, true, false);		
		RuleService.currentWorkspace = ws;		
		EventDistributor eventDistrib = new EventDistributor();
		picp = new ProcessInstanceChangeProcessor(ws, eventDistrib);
		WorkspaceListenerSequencer wsls = new WorkspaceListenerSequencer(ws);
		wsls.registerListener(picp);
		typeJira = TestArtifacts.getJiraInstanceType(ws);
	}
	
	@Test
	void testStepCompletedAvailableCompleted() throws Exception{
		Instance jiraA =  TestArtifacts.getJiraInstance(ws, "jiraA");
		Instance jiraParentA =  TestArtifacts.getJiraInstance(ws, "jiraParentA");
		TestArtifacts.addParentToJira(jiraA, jiraParentA);
		TestArtifacts.setStateToJiraInstance(jiraA, TestArtifacts.JiraStates.Open);
		ws.concludeTransaction();
		ProcessDefinition procDef = getSingleStepProcessDefinitionWithOutput(ws);
		ProcessInstance proc = ProcessInstance.getInstance(ws, procDef, "process");
		proc.addInput("jiraIn", jiraA);
		ws.concludeTransaction();
		InstanceTests.printFullProcessToLog(proc);
		ProcessStep sd1 = proc.getProcessSteps().stream()
				.filter(step -> step.getDefinition().getName().equals("sd1") ).findAny().get(); 
		assert(sd1.getActualLifecycleState().equals(State.ACTIVE));
		assert(sd1.getExpectedLifecycleState().equals(State.ACTIVE));
		
		
		TestArtifacts.setStateToJiraInstance(jiraParentA, TestArtifacts.JiraStates.Closed);
		ws.concludeTransaction();
		InstanceTests.printFullProcessToLog(proc);		
		assert(sd1.getActualLifecycleState().equals(State.COMPLETED));
		assert(sd1.getExpectedLifecycleState().equals(State.COMPLETED));
		
		TestArtifacts.setStateToJiraInstance(jiraA, TestArtifacts.JiraStates.Closed);
		ws.concludeTransaction();
		InstanceTests.printFullProcessToLog(proc);		
		assert(sd1.getActualLifecycleState().equals(State.AVAILABLE));
		assert(sd1.getExpectedLifecycleState().equals(State.COMPLETED));
		
		
		TestArtifacts.setStateToJiraInstance(jiraA, TestArtifacts.JiraStates.Open);
		ws.concludeTransaction();
		InstanceTests.printFullProcessToLog(proc);		
		assert(sd1.getActualLifecycleState().equals(State.COMPLETED));
		assert(sd1.getExpectedLifecycleState().equals(State.COMPLETED));
		
	}

	
	public static ProcessDefinition getSingleStepProcessDefinitionWithOutput(Workspace ws) throws ProcessException {
		InstanceType typeJira = TestArtifacts.getJiraInstanceType(ws);
		ProcessDefinition procDef = ProcessDefinition.getInstance("proc1", ws);
		procDef.addExpectedInput("jiraIn", typeJira);				
		
		DecisionNodeDefinition dnd1 = procDef.createDecisionNodeDefinition("dnd1", ws);
		DecisionNodeDefinition dnd2 =  procDef.createDecisionNodeDefinition("dnd2", ws);
		
		StepDefinition sd1 = procDef.createStepDefinition("sd1", ws);
		sd1.addExpectedInput("jiraIn", typeJira);		
		sd1.addExpectedOutput("jiraOut", typeJira);

		sd1.setCondition(Conditions.PRECONDITION, "self.in_jiraIn->forAll(issue | issue.state='Open')"); 
		
		sd1.addInputToOutputMappingRule("jiraOut", 
				"self.in_jiraIn->collect(issue | issue.parent)"							
								); 
		
		sd1.setCondition(Conditions.POSTCONDITION, "self.out_jiraOut->size() > 0 and self.out_jiraOut->forAll( \r\n"
				+ "issue | issue.state='Closed')"); 

		sd1.setInDND(dnd1);
		sd1.setOutDND(dnd2);
		sd1.setSpecOrderIndex(1);
		dnd1.addDataMappingDefinition(MappingDefinition.getInstance(procDef.getName(), "jiraIn", sd1.getName(), "jiraIn",  ws));
		procDef.initializeInstanceTypes(false);
		procDef.setImmediateInstantiateAllStepsEnabled(true); 
		return procDef;
	}
}
