package at.jku.isse.designspace.tests.passiveprocessengine.serverside;

import static org.junit.Assert.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import at.jku.isse.designspace.core.model.InstanceType;
import at.jku.isse.designspace.core.model.Workspace;
import at.jku.isse.designspace.core.service.WorkspaceService;
import at.jku.isse.designspace.rule.checker.ArlRuleEvaluator;
import at.jku.isse.designspace.rule.service.RuleService;
import at.jku.isse.passiveprocessengine.definition.DecisionNodeDefinition;
import at.jku.isse.passiveprocessengine.definition.MappingDefinition;
import at.jku.isse.passiveprocessengine.definition.ProcessDefinition;
import at.jku.isse.passiveprocessengine.definition.StepDefinition;
import at.jku.isse.passiveprocessengine.definition.DecisionNodeDefinition.InFlowType;
import at.jku.isse.passiveprocessengine.instance.ProcessInstance;
import at.jku.isse.passiveprocessengine.instance.ProcessInstanceChangeProcessor;
import at.jku.isse.passiveprocessengine.instance.ProcessStep;
import at.jku.isse.passiveprocessengine.instance.StepLifecycle.Conditions;

@ExtendWith(SpringExtension.class)
@SpringBootTest
class InstanceTests {

	Workspace ws;
	static InstanceType typeJira;
	ProcessInstanceChangeProcessor picp;
	
	@BeforeEach
	void setup() throws Exception {
		RuleService.setEvaluator(new ArlRuleEvaluator());
		ws = WorkspaceService.createWorkspace("test", WorkspaceService.PUBLIC_WORKSPACE, WorkspaceService.ANY_USER, null, false, false);
		ws.setAutoUpdate(true);
		picp = new ProcessInstanceChangeProcessor(ws);
		typeJira = TestArtifacts.getJiraInstanceType(ws);
	}

	@Test
	void testCreateInstance() {
		ProcessDefinition procDef = getTestDefinition(ws);
		ProcessInstance proc = ProcessInstance.getInstance(ws, procDef);
		proc.addInput("jiraIn", ws.createInstance(typeJira, "jira1"));
		ws.concludeTransaction();
		
		System.out.println(proc);
		proc.getProcessSteps().stream().forEach(step -> System.out.println(step));
		
		assertTrue(proc.getInput("jiraIn").size() == 1);
		assertTrue(proc.getDecisionNodeInstances().size() == 2);
		assertTrue(proc.getProcessSteps().size() == 1);
		
		ProcessStep step = proc.getProcessSteps().stream().findAny().get();
		assertTrue(step.getInput("jiraIn").size() == 1);
		
	
	}
	
	public static ProcessDefinition getTestDefinition(Workspace ws) {
		InstanceType typeJira = TestArtifacts.getJiraInstanceType(ws);
		ProcessDefinition procDef = ProcessDefinition.getInstance("proc1", ws);
		procDef.addExpectedInput("jiraIn", typeJira);	
//		ws.debugInstanceTypes().stream().forEach(it -> System.out.println(it));	
		DecisionNodeDefinition dnd1 = procDef.createDecisionNodeDefinition("dnd1", ws);
		DecisionNodeDefinition dnd2 =  procDef.createDecisionNodeDefinition("dnd2", ws);
		dnd2.setInflowType(InFlowType.AND); // set by default
		DecisionNodeDefinition dnd3 =  procDef.createDecisionNodeDefinition("dnd3", ws);
		procDef.addDecisionNodeDefinition(dnd1);
		procDef.addDecisionNodeDefinition(dnd2);
		procDef.addDecisionNodeDefinition(dnd3);
		
		StepDefinition sd1 = procDef.createStepDefinition("sd1", ws);
		sd1.addExpectedInput("jiraIn", typeJira);
		sd1.addExpectedOutput("jiraOut", typeJira);
		//sd1.addInputToOutputMappingRule("jiraIn2jiraOut", "self.in_jiraIn->forAll(elem | result->includes(elem) = self.out_jiraOut->excludes(elem))-> size() = 0"); // i.e., the symetricDifference is empty, i.e., the same elements need to be in both lists
		sd1.addInputToOutputMappingRule("jiraIn2jiraOutTest", "self.in_jiraIn->size() = self.out_jiraOut-> size()"); // i.e., the symetricDifference is empty, i.e., the same elements need to be in both lists
		sd1.setCondition(Conditions.PRECONDITION, "self.in_jiraIn->size() > 0");
		sd1.setCondition(Conditions.POSTCONDITION, "self.out_jiraOut->size() > 0");
		sd1.setInDND(dnd1);
		sd1.setOutDND(dnd2);
		
		StepDefinition sd2 = procDef.createStepDefinition("sd2", ws);
		sd2.setInDND(dnd2);
		sd2.setOutDND(dnd3);
		
		dnd1.addDataMappingDefinition(MappingDefinition.getInstance(procDef.getName(), "jiraIn", sd1.getName(), "jiraIn", ws));
		dnd2.addDataMappingDefinition(MappingDefinition.getInstance(sd1.getName(), "jiraOut", sd2.getName(), "jiraIn",ws));
		return procDef;
	}

}
