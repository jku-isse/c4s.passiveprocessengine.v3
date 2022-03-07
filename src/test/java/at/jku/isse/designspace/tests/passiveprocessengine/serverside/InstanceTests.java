package at.jku.isse.designspace.tests.passiveprocessengine.serverside;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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
import at.jku.isse.designspace.rule.checker.ConsistencyUtils;
import at.jku.isse.designspace.rule.model.ConsistencyRuleType;
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
	
	@Test
	void testComplexDataMapping() {
		Instance jiraB = ws.createInstance(typeJira, "jiraB");
		Instance jiraC = ws.createInstance(typeJira, "jiraC");
		Instance jiraD = ws.createInstance(typeJira, "jiraD");
		Instance jiraA = ws.createInstance(typeJira, "jiraA");
		jiraA.getPropertyAsSet(TestArtifacts.CoreProperties.requirementIDs.toString()).add("jiraB");
		jiraA.getPropertyAsSet(TestArtifacts.CoreProperties.requirementIDs.toString()).add("jiraC");
		
		ProcessDefinition procDef = getTestDefinition(ws);
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
	void testRules() {
		Instance jiraB = ws.createInstance(typeJira, "jiraB");
		Instance jiraC = ws.createInstance(typeJira, "jiraC");
		Instance jiraD = ws.createInstance(typeJira, "jiraD");
		Instance jiraA = ws.createInstance(typeJira, "jiraA");
		jiraA.getPropertyAsSet(TestArtifacts.CoreProperties.requirementIDs.toString()).add("jiraB");
		jiraA.getPropertyAsSet(TestArtifacts.CoreProperties.requirementIDs.toString()).add("jiraC");
		
		
		ProcessDefinition procDef = getTestDefinition(ws);
		ProcessInstance proc = ProcessInstance.getInstance(ws, procDef);
		proc.addInput("jiraIn", jiraA);
		ws.concludeTransaction();
		assertTrue(jiraA.getPropertyAsSet(TestArtifacts.CoreProperties.requirementIDs.toString()).get().size()==2);
		proc.getProcessSteps().stream()
			.filter(td -> td.getDefinition().getInputToOutputMappingRules().size() > 0)
			.peek(td -> System.out.println("Visiting Step: "+td.getName()))
			.forEach(td -> {
				td.getDefinition().getInputToOutputMappingRules().entrySet().stream().forEach(entry -> {
					InstanceType type = td.getInstance().getProperty("crd_datamapping_"+entry.getKey()).propertyType().referencedInstanceType();
					ConsistencyRuleType crt = (ConsistencyRuleType)type;
					System.out.println("Checking "+crt.name());
					
					assertTrue(ConsistencyUtils.crdValid(crt));
				});
		});
		
		System.out.println(proc);
		proc.getProcessSteps().stream().forEach(step -> System.out.println(step));

	}
	
	public static ProcessDefinition getTestDefinition(Workspace ws) {
		InstanceType typeJira = TestArtifacts.getJiraInstanceType(ws);
		ProcessDefinition procDef = ProcessDefinition.getInstance("proc1", ws);
		procDef.addExpectedInput("jiraIn", typeJira);	
//		ws.debugInstanceTypes().stream().forEach(it -> System.out.println(it));	
		DecisionNodeDefinition dnd1 = procDef.createDecisionNodeDefinition("dnd1", ws);
		dnd1.setInflowType(InFlowType.AND); 
		DecisionNodeDefinition dnd2 =  procDef.createDecisionNodeDefinition("dnd2", ws);
		dnd2.setInflowType(InFlowType.AND); 
		DecisionNodeDefinition dnd3 =  procDef.createDecisionNodeDefinition("dnd3", ws);
		procDef.addDecisionNodeDefinition(dnd1);
		procDef.addDecisionNodeDefinition(dnd2);
		procDef.addDecisionNodeDefinition(dnd3);
		
		StepDefinition sd1 = procDef.createStepDefinition("sd1", ws);
		sd1.addExpectedInput("jiraIn", typeJira);
		sd1.addExpectedOutput("jiraOut", typeJira);
		//sd1.addInputToOutputMappingRule("jiraIn2jiraOut", "self.in_jiraIn->forAll(elem | result->includes(elem) = self.out_jiraOut->excludes(elem))-> size() = 0"); // i.e., the symetricDifference is empty, i.e., the same elements need to be in both lists
		//sd1.addInputToOutputMappingRule("jiraIn2jiraOutTest", "self.in_jiraIn->size() = self.out_jiraOut-> size()"); // i.e., the symetricDifference is empty, i.e., the same elements need to be in both lists
		//->asList()->first().asType('JiraArtifact')
		sd1.addInputToOutputMappingRule("jiraIn2jiraOutTest", "self.in_jiraIn->asList()->first()->asType(<"+typeJira.getQualifiedName()+">).requirementIDs->forAll(id | self.out_jiraOut->exists(art  | art.name = id))"); // for every id in requirements there is an instance with that name
		
		//sd1.addInputToOutputMappingRule("jiraIn2jiraOutTest", "self.in_jiraIn->asList()->first()->asType(<"+typeJira.getQualifiedName()+">).requirementIDs->forAll(id | self.out_jiraOut->exists(art  | art.name = id))"); // for every id in requirements there is an instance with that name
		//sd1.addInputToOutputMappingRule("jiraIn2jiraOutTest", "self.in_jiraIn->forAll(artIn | self.out_jiraOut->exists(artOut  | artOut = artIn)) and "
		//		+ " self.out_jiraOut->forAll(artOut2 | self.in_jiraIn->exists(artIn2  | artOut2 = artIn2))"); // for every id in requirements there is an instance with that name
		
		sd1.setCondition(Conditions.PRECONDITION, "self.in_jiraIn->size() = 1");
		//sd1.setCondition(Conditions.POSTCONDITION, "self.out_jiraOut->size() = self.in_jiraIn->asList()->first().requirementIDs->size()");
		sd1.setCondition(Conditions.POSTCONDITION, "self.out_jiraOut->size() >= 1");
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
