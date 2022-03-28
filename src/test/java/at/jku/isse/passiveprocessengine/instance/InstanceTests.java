package at.jku.isse.passiveprocessengine.instance;

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
import at.jku.isse.passiveprocessengine.definition.ProcessDefinition;
import at.jku.isse.passiveprocessengine.definition.DecisionNodeDefinition.InFlowType;
import at.jku.isse.passiveprocessengine.definition.serialization.DTOs;
import at.jku.isse.passiveprocessengine.definition.serialization.DefinitionTransformer;
import at.jku.isse.passiveprocessengine.definition.serialization.JsonDefinitionSerializer;
import at.jku.isse.passiveprocessengine.instance.ProcessInstance;
import at.jku.isse.passiveprocessengine.instance.ProcessInstanceChangeProcessor;
import at.jku.isse.passiveprocessengine.instance.ProcessStep;
import at.jku.isse.passiveprocessengine.instance.StepLifecycle.Conditions;
import at.jku.isse.passiveprocessengine.instance.StepLifecycle.State;
import at.jku.isse.passiveprocessengine.instance.TestArtifacts.JiraStates;

@ExtendWith(SpringExtension.class)
@SpringBootTest
public
class InstanceTests {

	static Workspace ws;
	static InstanceType typeJira;
	ProcessInstanceChangeProcessor picp;
	static JsonDefinitionSerializer json = new JsonDefinitionSerializer();
	
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
	void testComplexDataMappingUpdateToProperty() {
		Instance jiraB =  TestArtifacts.getJiraInstance(ws, "jiraB");
		Instance jiraC = TestArtifacts.getJiraInstance(ws, "jiraC");
		Instance jiraD = TestArtifacts.getJiraInstance(ws, "jiraD");
		Instance jiraA = TestArtifacts.getJiraInstance(ws, "jiraA", "jiraB", "jiraC");
		
		ProcessDefinition procDef = TestProcesses.getSimple2StepProcessDefinition(ws);
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
		
		ProcessDefinition procDef = TestProcesses.getSimple2StepProcessDefinition(ws);
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
		
		ProcessDefinition procDef = TestProcesses.getSimple2StepProcessDefinition(ws);
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
	
	@Test
	void testSimpleSubprocess() {
		Instance jiraE =  TestArtifacts.getJiraInstance(ws, "jiraE");
		ProcessDefinition procDef = TestProcesses.getSimpleSubprocessDefinition(ws);
		ProcessInstance proc = ProcessInstance.getInstance(ws, procDef);
		proc.addInput("jiraIn", jiraE);
		ws.concludeTransaction();
		TestArtifacts.setStateToJiraInstance(jiraE, JiraStates.Closed);
		ws.concludeTransaction();
		
		System.out.println(proc);
		proc.getProcessSteps().stream().forEach(step -> System.out.println(step));
		assert(proc.getExpectedLifecycleState().equals(State.COMPLETED));
		assert(proc.getActualLifecycleState().equals(State.COMPLETED));
		assert(proc.getOutput("jiraOut").size() == 1);
	}
	
	@Test
	void testSimpleParentprocess() {
		Instance jiraF =  TestArtifacts.getJiraInstance(ws, "jiraF");
		ProcessDefinition procDef = TestProcesses.getSimpleSuperProcessDefinition(ws);
		ProcessInstance proc = ProcessInstance.getInstance(ws, procDef);
		proc.addInput("jiraIn", jiraF);
		ws.concludeTransaction();
		TestArtifacts.setStateToJiraInstance(jiraF, JiraStates.Closed);
		ws.concludeTransaction();
		
		printFullProcessToLog(proc); 
		assert(proc.getExpectedLifecycleState().equals(State.COMPLETED));
		assert(proc.getActualLifecycleState().equals(State.COMPLETED));
		assert(proc.getOutput("jiraOut").size() == 1);
	}
	
	@Test
	void testSimpleParentProcessFromSerializationForm() {
		Instance jiraF =  TestArtifacts.getJiraInstance(ws, "jiraF");
		DTOs.Process procD = TestProcesses.getSimpleSuperDTOProcessDefinition(ws);
		String jsonProc = json.toJson(procD);
		DTOs.Process deSer = json.fromJson(jsonProc);
		ProcessDefinition procDef = DefinitionTransformer.fromDTO(deSer, ws);
		ProcessInstance proc = ProcessInstance.getInstance(ws, procDef);
		proc.addInput("jiraIn", jiraF);
		ws.concludeTransaction();
		TestArtifacts.setStateToJiraInstance(jiraF, JiraStates.Closed);
		ws.concludeTransaction();
		
		InstanceTests.printFullProcessToLog(proc); 
		assert(proc.getExpectedLifecycleState().equals(State.COMPLETED));
		assert(proc.getActualLifecycleState().equals(State.COMPLETED));
		assert(proc.getOutput("jiraOut").size() == 1);
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
		proc.getDecisionNodeInstances().stream().forEach(dni -> System.out.println(nextIndent+dni.toString()));
	}
	
}
