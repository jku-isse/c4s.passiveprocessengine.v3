package at.jku.isse.passiveprocessengine.instance;

import static org.junit.jupiter.api.Assertions.fail;

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
import at.jku.isse.designspace.rule.model.ConsistencyRuleType;
import at.jku.isse.designspace.rule.service.RuleService;
import at.jku.isse.passiveprocessengine.definition.DecisionNodeDefinition;
import at.jku.isse.passiveprocessengine.definition.MappingDefinition;
import at.jku.isse.passiveprocessengine.definition.ProcessDefinition;
import at.jku.isse.passiveprocessengine.definition.StepDefinition;
import at.jku.isse.passiveprocessengine.definition.serialization.JsonDefinitionSerializer;
import at.jku.isse.passiveprocessengine.demo.TestArtifacts;
import at.jku.isse.passiveprocessengine.demo.TestProcesses;
import at.jku.isse.passiveprocessengine.instance.ProcessException;
import at.jku.isse.passiveprocessengine.instance.ProcessInstance;
import at.jku.isse.passiveprocessengine.instance.ProcessInstanceChangeProcessor;
import at.jku.isse.passiveprocessengine.instance.StepLifecycle.Conditions;
import at.jku.isse.passiveprocessengine.instance.messages.EventDistributor;
import at.jku.isse.passiveprocessengine.instance.messages.WorkspaceListenerSequencer;

@ExtendWith(SpringExtension.class)
@SpringBootTest
class SubSuperProcessTests {

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
	void testParentStepPrematureCompletion() throws Exception{
		Instance jiraA =  TestArtifacts.getJiraInstance(ws, "jiraA");
		Instance jiraParentA =  TestArtifacts.getJiraInstance(ws, "jiraParentA");
		TestArtifacts.addParentToJira(jiraA, jiraParentA);
		TestArtifacts.setStateToJiraInstance(jiraA, TestArtifacts.JiraStates.Open);
		ws.concludeTransaction();
		ProcessDefinition procDef = getSimpleSuperProcessDefinition(ws);
		ProcessInstance proc = ProcessInstance.getInstance(ws, procDef, "Parentprocess");
		proc.addInput("procJiraIn", jiraA);
		ws.concludeTransaction();
		InstanceTests.printFullProcessToLog(proc);
		
//		ws.debugInstanceTypes().stream().filter(type -> type instanceof ConsistencyRuleType).forEach(crt -> System.out.println(crt.name()+"::"+crt.toString()));
//		
		TestArtifacts.setStateToJiraInstance(jiraParentA, TestArtifacts.JiraStates.InProgress);
		ws.concludeTransaction();
		InstanceTests.printFullProcessToLog(proc);
		
		TestArtifacts.setStateToJiraInstance(jiraParentA, TestArtifacts.JiraStates.Closed);
		ws.concludeTransaction();
		InstanceTests.printFullProcessToLog(proc);
		
		
	}

	public static ProcessDefinition getSimpleSuperProcessDefinition(Workspace ws) throws ProcessException {
		InstanceType typeJira = TestArtifacts.getJiraInstanceType(ws);
		ProcessDefinition procDef = ProcessDefinition.getInstance("parentproc1", ws);
		procDef.addExpectedInput("procJiraIn", typeJira);					
		//procDef.setCondition(Conditions.POSTCONDITION, "self.out_jiraOut->size() > 0");
		//no definition how many outputs, there is a possibility to provide output, but completion is upon subtask completion
		DecisionNodeDefinition dnd1 = procDef.createDecisionNodeDefinition("dndParentStart", ws);
		DecisionNodeDefinition dnd2 = procDef.createDecisionNodeDefinition("dndParentEnd", ws);	
		StepDefinition sd2 = getSingleStepProcessDefinitionWithOutput(ws);
		// we need to wire up the step definiton:
		sd2.setProcess(procDef);
		procDef.addStepDefinition(sd2);
		//inputs and output set in subprocess/step definition, pre and post cond set here
		sd2.setCondition(Conditions.PRECONDITION, "self.in_jiraIn->size() > 0 ");
		sd2.setCondition(Conditions.POSTCONDITION, "self.in_jiraIn->size() >= 1 and self->isDefined()");		
		sd2.setInDND(dnd1);
		sd2.setOutDND(dnd2);
		sd2.setSpecOrderIndex(1);
		
		dnd1.addDataMappingDefinition(MappingDefinition.getInstance(procDef.getName(), "procJiraIn", sd2.getName(), "jiraIn",  ws)); 
		
		procDef.setDepthIndexRecursive(1);
		procDef.initializeInstanceTypes(false);
		procDef.setImmediateInstantiateAllStepsEnabled(true); 
		return procDef;
	}
	
	public static ProcessDefinition getSingleStepProcessDefinitionWithOutput(Workspace ws) throws ProcessException {
		InstanceType typeJira = TestArtifacts.getJiraInstanceType(ws);
		ProcessDefinition procDef = ProcessDefinition.getInstance("proc1", ws);
		procDef.addExpectedInput("jiraIn", typeJira);				
		//procDef.setCondition(Conditions.PRECONDITION, "self.in_jiraIn->size() > 0 ");
		//procDef.setCondition(Conditions.POSTCONDITION, "self.in_jiraIn->size() >= 1 and self->isDefined()");		
		
		
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
