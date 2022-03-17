package at.jku.isse.designspace.tests.passiveprocessengine.serverside;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import at.jku.isse.designspace.core.model.Cardinality;
import at.jku.isse.designspace.core.model.Instance;
import at.jku.isse.designspace.core.model.InstanceType;
import at.jku.isse.designspace.core.model.Workspace;
import at.jku.isse.designspace.core.service.WorkspaceService;
import at.jku.isse.designspace.rule.checker.ArlRuleEvaluator;
import at.jku.isse.designspace.rule.checker.ConsistencyUtils;
import at.jku.isse.designspace.rule.model.ConsistencyRuleType;
import at.jku.isse.designspace.rule.model.Rule;
import at.jku.isse.designspace.rule.service.RuleService;
import at.jku.isse.designspace.tests.passiveprocessengine.serverside.TestArtifacts.JiraStates;
import at.jku.isse.passiveprocessengine.definition.DecisionNodeDefinition;
import at.jku.isse.passiveprocessengine.definition.MappingDefinition;
import at.jku.isse.passiveprocessengine.definition.ProcessDefinition;
import at.jku.isse.passiveprocessengine.definition.QAConstraintSpec;
import at.jku.isse.passiveprocessengine.definition.StepDefinition;
import at.jku.isse.passiveprocessengine.instance.ProcessInstance;
import at.jku.isse.passiveprocessengine.instance.ProcessInstanceChangeProcessor;
import at.jku.isse.passiveprocessengine.instance.ProcessStep;
import at.jku.isse.passiveprocessengine.instance.StepLifecycle.Conditions;
import at.jku.isse.passiveprocessengine.instance.StepLifecycle.State;

@ExtendWith(SpringExtension.class)
@SpringBootTest
class InstanceTests {

	static Workspace ws;
	static InstanceType typeJira;
	ProcessInstanceChangeProcessor picp;
	
	@BeforeEach
	void setup() throws Exception {
		RuleService.setEvaluator(new ArlRuleEvaluator());
		ws = WorkspaceService.createWorkspace("test", WorkspaceService.PUBLIC_WORKSPACE, WorkspaceService.ANY_USER, null, false, false);
		//ws.setAutoUpdate(true);
		picp = new ProcessInstanceChangeProcessor(ws);
		typeJira = TestArtifacts.getJiraInstanceType(ws);
	}

	
	@Test
	void testComplexDataMapping() {
		Instance jiraB =  TestArtifacts.getJiraInstance(ws, "jiraB");
		Instance jiraC = TestArtifacts.getJiraInstance(ws, "jiraC");
		Instance jiraD = TestArtifacts.getJiraInstance(ws, "jiraD");
		Instance jiraA = TestArtifacts.getJiraInstance(ws, "jiraA", "jiraB", "jiraC");
		
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
	void testComplexDataMappingUpdateToProperty() {
		Instance jiraB =  TestArtifacts.getJiraInstance(ws, "jiraB");
		Instance jiraC = TestArtifacts.getJiraInstance(ws, "jiraC");
		Instance jiraD = TestArtifacts.getJiraInstance(ws, "jiraD");
		Instance jiraA = TestArtifacts.getJiraInstance(ws, "jiraA", "jiraB", "jiraC");
		
		ProcessDefinition procDef = getTestDefinition(ws);
		ProcessInstance proc = ProcessInstance.getInstance(ws, procDef);
		proc.addInput("jiraIn", jiraA);
		ws.concludeTransaction();
		assert(proc.getProcessSteps().stream()
				.filter(step -> step.getDefinition().getName().equals("sd1") )
				.allMatch(step -> (step.getOutput("jiraOut").size() == 2) && step.getActualLifecycleState().equals(State.COMPLETED) ));
		assert(proc.getProcessSteps().stream()
				.filter(step -> step.getDefinition().getName().equals("sd2") )
				.allMatch(step -> (step.getInput("jiraIn").size() == 2) && step.getActualLifecycleState().equals(State.ENABLED) ) );
		
		jiraA.getPropertyAsSet(TestArtifacts.CoreProperties.requirementIDs.toString()).remove("jiraC");
		TestArtifacts.setStateToJiraInstance(jiraB, JiraStates.Closed);
		// we close, thus keep SD1 in active state, thus no output propagation yet, 
		ws.concludeTransaction();
		assert(jiraA.getPropertyAsSet(TestArtifacts.CoreProperties.requirementIDs.toString()).size() == 1);
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
			.allMatch(step -> (step.getOutput("jiraOut").size() == 1) && step.getExpectedLifecycleState().equals(State.COMPLETED) ) );
		
		assert(proc.getProcessSteps().stream()
				.filter(step -> step.getDefinition().getName().equals("sd2") )
				.allMatch(step -> (step.getInput("jiraIn").size() == 1) 
									&& (step.getOutput("jiraOut").size() == 1) 
									&& step.getActualLifecycleState().equals(State.ENABLED) ) );
	}
	
	@Test
	void testComplexDataMappingRemoveInput() {
		Instance jiraB =  TestArtifacts.getJiraInstance(ws, "jiraB");
		Instance jiraC = TestArtifacts.getJiraInstance(ws, "jiraC");
		Instance jiraD = TestArtifacts.getJiraInstance(ws, "jiraD");
		Instance jiraA = TestArtifacts.getJiraInstance(ws, "jiraA", "jiraB", "jiraC");
		
		ProcessDefinition procDef = getTestDefinition(ws);
		ProcessInstance proc = ProcessInstance.getInstance(ws, procDef);
		proc.addInput("jiraIn", jiraA);
		ws.concludeTransaction();
		
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
		
		jiraD.getPropertyAsSet(TestArtifacts.CoreProperties.requirementIDs.toString()).add("jiraB");
		//TestArtifacts.setStateToJiraInstance(jiraB, JiraStates.Closed);
		ws.concludeTransaction();
		assert(proc.getProcessSteps().stream()
				.filter(step -> step.getDefinition().getName().equals("sd2") )
				.allMatch(step -> (step.getOutput("jiraOut").size() == 1)) );
		
		jiraD.getPropertyAsSet(TestArtifacts.CoreProperties.requirementIDs.toString()).remove("jiraB");
//		ws.concludeTransaction();
//		assert(proc.getProcessSteps().stream()
//				.filter(step -> step.getDefinition().getName().equals("sd2") )
//				.allMatch(step -> (step.getOutput("jiraOut").size() == 0)) );
		
		jiraD.getPropertyAsSet(TestArtifacts.CoreProperties.requirementIDs.toString()).add("jiraC");
		ws.concludeTransaction();
		
		assertAllConstraintsAreValid(proc);
		System.out.println(proc);
		proc.getProcessSteps().stream().forEach(step -> System.out.println(step));
		assert(proc.getProcessSteps().stream()
				.filter(step -> step.getDefinition().getName().equals("sd1") )
				.allMatch(step -> (step.getOutput("jiraOut").size() == 1) && step.getExpectedLifecycleState().equals(State.COMPLETED) ));
		assert(proc.getProcessSteps().stream()
				.filter(step -> step.getDefinition().getName().equals("sd2") )
				.allMatch(step -> (step.getOutput("jiraOut").size() == 1) && step.getActualLifecycleState().equals(State.ACTIVE) ));
	}
	
	@Test
	void testRules() {
		Instance jiraB =  TestArtifacts.getJiraInstance(ws, "jiraB");
		Instance jiraC = TestArtifacts.getJiraInstance(ws, "jiraC");
		Instance jiraD = TestArtifacts.getJiraInstance(ws, "jiraD");
		Instance jiraA = TestArtifacts.getJiraInstance(ws, "jiraA", "jiraB", "jiraC");
		
		ProcessDefinition procDef = getTestDefinition(ws);
		ProcessInstance proc = ProcessInstance.getInstance(ws, procDef);
		proc.addInput("jiraIn", jiraA);
		ws.concludeTransaction();
		assertTrue(jiraA.getPropertyAsSet(TestArtifacts.CoreProperties.requirementIDs.toString()).get().size()==2);
		
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
	
	public static void assertAllConstraintsAreValid(ProcessInstance proc) {
		proc.getProcessSteps().stream()
		.peek(td -> System.out.println("Visiting Step: "+td.getName()))
		.forEach(td -> {
			td.getDefinition().getInputToOutputMappingRules().entrySet().stream().forEach(entry -> {
				InstanceType type = td.getInstance().getProperty("crd_datamapping_"+entry.getKey()).propertyType().referencedInstanceType();
				ConsistencyRuleType crt = (ConsistencyRuleType)type;
				String eval = (String) crt.ruleEvaluations().get().stream()
						.map(rule -> ((Rule)rule).result()+"" )
						.collect(Collectors.joining(",","[","]"));
				System.out.println("Checking "+crt.name() +" Result: "+ eval);
				assertTrue(ConsistencyUtils.crdValid(crt));
			});
			td.getDefinition().getQAConstraints().stream().forEach(entry -> {
				InstanceType type = td.getInstance().getProperty(ProcessStep.getQASpecId(entry, ProcessStep.getOrCreateDesignSpaceInstanceType(ws, td.getDefinition()))).propertyType().referencedInstanceType();
				ConsistencyRuleType crt = (ConsistencyRuleType)type;
				String eval = (String) crt.ruleEvaluations().get().stream()
								.map(rule -> ((Rule)rule).result()+"" )
								.collect(Collectors.joining(",","[","]"));
				System.out.println("Checking "+crt.name() +" Result: "+ eval);
				assertTrue(ConsistencyUtils.crdValid(crt));
			});
			for (Conditions condition : Conditions.values()) {
				if (td.getDefinition().getCondition(condition).isPresent()) {
					InstanceType type = td.getInstance().getProperty(condition.toString()).propertyType().referencedInstanceType();
					ConsistencyRuleType crt = (ConsistencyRuleType)type;
					String eval = (String) crt.ruleEvaluations().get().stream()
							.map(rule -> ((Rule)rule).result()+"" )
							.collect(Collectors.joining(",","[","]"));
					System.out.println("Checking "+crt.name() +" Result: "+ eval);
					assertTrue(ConsistencyUtils.crdValid(crt));
				}	
			}
	});
	}
	
	public static ProcessDefinition getTestDefinition(Workspace ws) {
		InstanceType typeJira = TestArtifacts.getJiraInstanceType(ws);
		ProcessDefinition procDef = ProcessDefinition.getInstance("proc1", ws);
		procDef.addExpectedInput("jiraIn", typeJira);	
//		ws.debugInstanceTypes().stream().forEach(it -> System.out.println(it));	
		DecisionNodeDefinition dnd1 = procDef.createDecisionNodeDefinition("dnd1", ws);
		//dnd1.setInflowType(InFlowType.AND); 
		DecisionNodeDefinition dnd2 =  procDef.createDecisionNodeDefinition("dnd2", ws);
		//dnd2.setInflowType(InFlowType.AND); 
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
		sd1.addInputToOutputMappingRule("jiraIn2jiraOutTest", 
			"self.in_jiraIn"
				+ "->asList()"
				+ "->first()"
				+ "->asType(<"+typeJira.getQualifiedName()+">)"
						+ ".requirementIDs"
							+ "->forAll(id | self.out_jiraOut->exists(art  | art.name = id))"
			+ " and "
				+ "self.out_jiraOut"
				+ "->forAll(out | self.in_jiraIn"
									+ "->asList()"
									+ "->first()"
									+ "->asType(<"+typeJira.getQualifiedName()+">)"
											+ ".requirementIDs"
											+ "->exists(artId | artId = out.name))"); // for every id in requirements there is an instance with that name, and vice versa
		
		//sd1.addInputToOutputMappingRule("jiraIn2jiraOutTest", "self.in_jiraIn->asList()->first()->asType(<"+typeJira.getQualifiedName()+">).requirementIDs->forAll(id | self.out_jiraOut->exists(art  | art.name = id))"); // for every id in requirements there is an instance with that name
		//sd1.addInputToOutputMappingRule("jiraIn2jiraOutTest", "self.in_jiraIn->forAll(artIn | self.out_jiraOut->exists(artOut  | artOut = artIn)) and "
		//		+ " self.out_jiraOut->forAll(artOut2 | self.in_jiraIn->exists(artIn2  | artOut2 = artIn2))"); // for every id in requirements there is an instance with that name
		
		sd1.setCondition(Conditions.PRECONDITION, "self.in_jiraIn->size() = 1");
		sd1.setCondition(Conditions.POSTCONDITION, "self.out_jiraOut->size() = self.in_jiraIn->asList()->first()->asType(<"+typeJira.getQualifiedName()+">).requirementIDs->size()");
		QAConstraintSpec qa1 = new QAConstraintSpec("sd1-qa1-state", "self.out_jiraOut->forAll( issue | issue.state = 'Open')", "All issue states must be 'Open'", ws);
		sd1.addQAConstraint(qa1);
		QAConstraintSpec qa2 = new QAConstraintSpec("sd1-qa2-state", "self.out_jiraOut->forAll( issue | issue.state <> 'InProgress')", "None of the issue states must be 'InProgress'", ws);
		sd1.addQAConstraint(qa2);
		sd1.setInDND(dnd1);
		sd1.setOutDND(dnd2);
		
		StepDefinition sd2 = procDef.createStepDefinition("sd2", ws);
		sd2.addExpectedInput("jiraIn", typeJira);
		sd2.addExpectedOutput("jiraOut", typeJira);
		sd2.setCondition(Conditions.PRECONDITION, "self.in_jiraIn->size() >= 1");
		sd2.setCondition(Conditions.POSTCONDITION, "self.out_jiraOut->size() >= 0");
		sd2.addInputToOutputMappingRule("jiraIn2jiraOut2", "self.in_jiraIn->forAll(artIn | self.out_jiraOut->exists(artOut  | artOut = artIn)) and "
						+ " self.out_jiraOut->forAll(artOut2 | self.in_jiraIn->exists(artIn2  | artOut2 = artIn2))"); // ensures both sets are identical in content
		QAConstraintSpec qa3 = new QAConstraintSpec("sd2-qa3-state", "self.in_jiraIn->forAll( issue | issue.state = 'Closed')", "All in issue states must be 'Closed'", ws);
		sd2.addQAConstraint(qa3);
		sd2.setInDND(dnd2);
		sd2.setOutDND(dnd3);
		
		
		dnd1.addDataMappingDefinition(MappingDefinition.getInstance(procDef.getName(), "jiraIn", sd1.getName(), "jiraIn",  ws));
		dnd2.addDataMappingDefinition(MappingDefinition.getInstance(sd1.getName(), "jiraOut", sd2.getName(), "jiraIn",  ws));
		return procDef;
	}

	
	
}
