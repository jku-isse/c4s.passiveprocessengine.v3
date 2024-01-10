package at.jku.isse.passiveprocessengine.instance;

import at.jku.isse.designspace.core.controlflow.ControlEventEngine;
import at.jku.isse.designspace.core.model.Instance;
import at.jku.isse.designspace.core.model.InstanceType;
import at.jku.isse.designspace.core.model.Workspace;
import at.jku.isse.designspace.core.service.WorkspaceService;
import at.jku.isse.designspace.rule.arl.repair.RepairNode;
import at.jku.isse.designspace.rule.checker.ArlRuleEvaluator;
import at.jku.isse.designspace.rule.checker.ConsistencyUtils;
import at.jku.isse.designspace.rule.model.ConsistencyRule;
import at.jku.isse.designspace.rule.model.ConsistencyRuleType;
import at.jku.isse.designspace.rule.model.Rule;
import at.jku.isse.designspace.rule.service.RuleService;
import at.jku.isse.passiveprocessengine.Context;
import at.jku.isse.passiveprocessengine.definition.activeobjects.ProcessDefinition;
import at.jku.isse.passiveprocessengine.definition.serialization.JsonDefinitionSerializer;
import at.jku.isse.passiveprocessengine.demo.TestArtifacts;
import at.jku.isse.passiveprocessengine.demo.TestArtifacts.JiraStates;
import at.jku.isse.passiveprocessengine.demo.TestProcesses;
import at.jku.isse.passiveprocessengine.instance.*;
import at.jku.isse.passiveprocessengine.instance.StepLifecycle.Conditions;
import at.jku.isse.passiveprocessengine.instance.StepLifecycle.State;
import at.jku.isse.passiveprocessengine.instance.activeobjects.ConstraintWrapper;
import at.jku.isse.passiveprocessengine.instance.activeobjects.ProcessInstance;
import at.jku.isse.passiveprocessengine.instance.activeobjects.ProcessStep;
import at.jku.isse.passiveprocessengine.instance.messages.EventDistributor;
import at.jku.isse.passiveprocessengine.instance.messages.WorkspaceListenerSequencer;
import at.jku.isse.passiveprocessengine.instance.types.AbstractProcessStepType;
import at.jku.isse.passiveprocessengine.instance.types.AbstractProcessStepType.CoreProperties;
import at.jku.isse.passiveprocessengine.monitoring.CurrentSystemTimeProvider;
import at.jku.isse.passiveprocessengine.monitoring.ProcessQAStatsMonitor;
import at.jku.isse.passiveprocessengine.monitoring.ProcessStats;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.Assert.assertTrue;

@ExtendWith(SpringExtension.class)
@SpringBootTest
public
class InstanceTestsAfterPersistence {

	static Workspace ws;
	static InstanceType typeJira;
	ProcessInstanceChangeProcessor picp;
	static JsonDefinitionSerializer json = new JsonDefinitionSerializer();
	static ProcessQAStatsMonitor monitor;

	@Autowired
	ControlEventEngine controlEventEngine;

	@BeforeEach
	void setup() {
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

	private ProcessInstance findProcessInstanceByName(String processDefinitionName, String namePostFix) {
		for (Instance instance : ws.debugInstances()) {
			if (instance.name().equals(processDefinitionName + "_" + namePostFix)) {
				return Context.getWrappedInstance(ProcessInstance.class, instance);
			}
		}
		return null;
	}
	
	@Test
	void testComplexDataMapping() {
		ProcessInstance proc = findProcessInstanceByName("proc1", "ComplexDataMapping");
		assert(proc != null);
		assert(proc.getProcessSteps().stream()
			.filter(step -> step.getDefinition().getName().equals("sd1") )
			.allMatch(step -> step.getOutput("jiraOut").size() == 2));
	}
	
	@Test
	void testComplexDataMappingUpdateToProperty() throws ProcessException {
		ProcessInstance proc = findProcessInstanceByName("proc1", "ComplexDataMappingUpdateToProperty");

		assert(proc != null);
		assertAllConstraintsAreValid(proc);
		assert(proc.getProcessSteps().stream()
			.filter(step -> step.getDefinition().getName().equals("sd1") )
			.allMatch(step -> (step.getOutput("jiraOut").iterator().next().name().equals("jiraB")) && step.getExpectedLifecycleState().equals(State.COMPLETED) ) );
		
		ProcessStep step2 = proc.getProcessSteps().stream()
				.filter(step -> step.getDefinition().getName().equals("sd2") ).findAny().get();
		assert(step2.getInput("jiraIn").iterator().next().name().equals("jiraB")) ;
		assert(step2.getInput("jiraIn").size()==1) ;
		assert(step2.getOutput("jiraOut").size()==1) ;
		assert(step2.getOutput("jiraOut").iterator().next().name().equals("jiraB")) ;
		assert(step2.getActualLifecycleState().equals(State.ENABLED) );
	}
	
	@Test
	void testComplexDataMappingRemoveInput() {
		WorkspaceService.PUBLIC_WORKSPACE.concludeTransaction();
		ws.concludeTransaction();

		ProcessInstance proc = findProcessInstanceByName("proc1", "ComplexDataMappingRemoveInput");
		assert(proc != null);

		assertAllConstraintsAreValid(proc);
		assert(proc.getProcessSteps().stream()
				.filter(step -> step.getDefinition().getName().equals("sd1") )
				.allMatch(step -> (step.getOutput("jiraOut").iterator().next().name().equals("jiraC")) && step.getExpectedLifecycleState().equals(State.COMPLETED) ));
		assert(proc.getProcessSteps().stream()
				.filter(step -> step.getDefinition().getName().equals("sd2") )
				.allMatch(step -> (step.getOutput("jiraOut").iterator().next().name().equals("jiraC")) && step.getActualLifecycleState().equals(State.ACTIVE) ));
	}
	
	@Test
	void testRules() throws ProcessException {
		ProcessInstance proc = findProcessInstanceByName("proc1", "Rules");

		assertAllConstraintsAreValid(proc);
		assert(proc.getProcessSteps().stream()
				.filter(step -> step.getDefinition().getName().equals("sd2") )
				.allMatch(step -> step.getActualLifecycleState().equals(State.COMPLETED) ));
	}
	
	@Test
	void testSimpleSubprocess() throws ProcessException {
		System.out.println(controlEventEngine.isInitialized());

		ProcessInstance proc = findProcessInstanceByName("subproc1", "SimpleSubprocess");

		assert(proc.getExpectedLifecycleState().equals(State.COMPLETED));
		assert(proc.getActualLifecycleState().equals(State.COMPLETED));
		assert(proc.getOutput("jiraOut").size() == 1);

		System.out.println(controlEventEngine.isInitialized());

		WorkspaceService.allWorkspaces().forEach(workspace -> printWorkspaceSpace(workspace));
	}
	
	@Test
	void testSimpleParentprocess() throws ProcessException {
		ProcessInstance proc = findProcessInstanceByName("parentproc1", "SimpleParentprocess");

		assert(proc.getExpectedLifecycleState().equals(State.COMPLETED));
		assert(proc.getActualLifecycleState().equals(State.COMPLETED));
		assert(proc.getOutput("jiraOut").size() == 1);
	}

	private void printWorkspaceSpace(Workspace workspace) {
		System.out.println("------------------ " + workspace.name() + " INSTANCES + ----------------------");
		workspace.debugInstances().stream().sorted((x,y) -> (int) (x.id().value() - y.id().value())).forEach(instance -> {
			System.out.println("------------------- {instanceName: \"" + instance.name() + "\", \"elementId\"" + instance.id() + "}");
		});
	}

//	@Test
//	void testSimpleParentProcessFromSerializationForm() {
//		Instance jiraF =  TestArtifacts.getJiraInstance(ws, "jiraF");
//		DTOs.Process procD = TestProcesses.getSimpleSuperDTOProcessDefinition(ws);
//		String jsonProc = json.toJson(procD);
//		DTOs.Process deSer = json.fromJson(jsonProc);
//		ProcessDefinition procDef = DefinitionTransformer.fromDTO(deSer, ws);
//		ProcessInstance proc = ProcessInstance.getInstance(ws, procDef);
//		proc.addInput("jiraIn", jiraF);
//		ws.concludeTransaction();
//		TestArtifacts.setStateToJiraInstance(jiraF, JiraStates.Closed);
//		ws.concludeTransaction();
//		
//		InstanceTests.printFullProcessToLog(proc); 
//		assert(proc.getExpectedLifecycleState().equals(State.COMPLETED));
//		assert(proc.getActualLifecycleState().equals(State.COMPLETED));
//		assert(proc.getOutput("jiraOut").size() == 1);
//	}
	
	@Test
	void testSymmetricDifferenceDatamapping() throws ProcessException {
		ProcessInstance proc = findProcessInstanceByName("proc1", "SymmetricDifferenceDatamapping");

		assert(proc.getProcessSteps().stream()
				.filter(step -> step.getDefinition().getName().equals("sd1") )
				.allMatch(step -> step.getOutput("jiraOut").size() == 2 ));

	}
	
	@Test
	void testUnionSymmetricDifferenceDatamapping() throws ProcessException {
		ProcessInstance proc = findProcessInstanceByName("proc1", "UnionSymmetricDifferenceDatamapping");

		assert(proc.getProcessSteps().stream()
				.filter(step -> step.getDefinition().getName().equals("sd1") )
				.allMatch(step -> step.getOutput("jiraOut").size() == 1 ));
		// while this works for this usecase, the repair suggestion for union picks the first collection found, and not the right collection,
		// i.e., JiraD is suggested to be removed from jiraA.requirements and not (as would be correct) from in_jiraIn2

	}
	
	@Test
	void testExistsCompletion() throws ProcessException {
		ProcessInstance proc = findProcessInstanceByName("proc1", "ExistsCompletion");

		assertAllConstraintsAreValid(proc);
		printFullProcessToLog(proc);
		assert(proc.getProcessSteps().stream()
				.filter(step -> step.getDefinition().getName().equals("sd1") )
				.allMatch(step -> step.getActualLifecycleState().equals(State.COMPLETED) ));

	}
	
	
	
	
	public static void assertAllConstraintsAreValid(ProcessInstance proc) {
		proc.getProcessSteps().stream()
		.peek(td -> System.out.println("Visiting Step: "+td.getName()))
		.forEach(td -> {
			td.getDefinition().getInputToOutputMappingRules().entrySet().stream().forEach(entry -> {
				InstanceType type = td.getInstance().getProperty("crd_datamapping_"+entry.getKey()).propertyType().referencedInstanceType();
				ConsistencyRuleType crt = (ConsistencyRuleType)type;
				assertTrue(ConsistencyUtils.crdValid(crt));
				String eval = (String) crt.ruleEvaluations().get().stream()
						.map(rule -> ((Rule)rule).result()+"" )
						.collect(Collectors.joining(",","[","]"));
				System.out.println("Checking "+crt.name() +" Result: "+ eval);
			});
			ProcessDefinition pd = td.getProcess() !=null ? td.getProcess().getDefinition() : (ProcessDefinition)td.getDefinition();
			td.getDefinition().getQAConstraints().stream().forEach(entry -> {
				//InstanceType type = td.getInstance().getProperty(ProcessStep.getQASpecId(entry, ProcessStep.getOrCreateDesignSpaceInstanceType(ws, td.getDefinition()))).propertyType().referencedInstanceType();
				String id = ProcessStep.getQASpecId(entry, pd);
				ConstraintWrapper cw = Context.getWrappedInstance(ConstraintWrapper.class, (Instance) td.getInstance().getPropertyAsMap(AbstractProcessStepType.CoreProperties.qaState.toString()).get(id));
				ConsistencyRuleType crt = (ConsistencyRuleType)cw.getRuleResult().getInstanceType();
				assertTrue(ConsistencyUtils.crdValid(crt));
				String eval = (String) crt.ruleEvaluations().get().stream()
								.map(rule -> ((Rule)rule).result()+"" )
								.collect(Collectors.joining(",","[","]"));
				System.out.println("Checking "+crt.name() +" Result: "+ eval);
				
			});
			for (Conditions condition : Conditions.values()) {
				if (td.getDefinition().getCondition(condition).isPresent()) {
					InstanceType type = td.getInstance().getProperty(condition.toString()).propertyType().referencedInstanceType();
					ConsistencyRuleType crt = (ConsistencyRuleType)type;
					assertTrue(ConsistencyUtils.crdValid(crt));
					String eval = (String) crt.ruleEvaluations().get().stream()
							.map(rule -> ((Rule)rule).result()+"" )
							.collect(Collectors.joining(",","[","]"));
					System.out.println("Checking "+crt.name() +" Result: "+ eval);
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
