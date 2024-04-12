package at.jku.isse.designspace.passiveprocessengine.definition;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import at.jku.isse.passiveprocessengine.configurability.ProcessConfigBaseElementFactory;
import at.jku.isse.passiveprocessengine.definition.ProcessDefinition;
import at.jku.isse.passiveprocessengine.definition.serialization.DTOs;
import at.jku.isse.passiveprocessengine.definition.serialization.JsonDefinitionSerializer;
import at.jku.isse.passiveprocessengine.definition.serialization.ProcessRegistry;
import at.jku.isse.passiveprocessengine.definition.serialization.ProcessRegistry.ProcessDeployResult;
import at.jku.isse.passiveprocessengine.demo.TestArtifacts;
import at.jku.isse.passiveprocessengine.demo.TestProcesses;
import at.jku.isse.passiveprocessengine.instance.ProcessException;
import at.jku.isse.passiveprocessengine.instance.ProcessInstance;
import at.jku.isse.passiveprocessengine.instance.ProcessInstanceChangeProcessor;
import at.jku.isse.passiveprocessengine.instance.StepLifecycle.Conditions;
import at.jku.isse.passiveprocessengine.instance.StepLifecycle.State;
import at.jku.isse.passiveprocessengine.instance.messages.EventDistributor;
import at.jku.isse.passiveprocessengine.instance.messages.WorkspaceListenerSequencer;
import at.jku.isse.passiveprocessengine.monitoring.CurrentSystemTimeProvider;
import at.jku.isse.passiveprocessengine.monitoring.ProcessQAStatsMonitor;

@ExtendWith(SpringExtension.class)
@SpringBootTest
class ReplacementTests {

	static Workspace ws;
	static InstanceType typeJira;
	ProcessInstanceChangeProcessor picp;
	static JsonDefinitionSerializer json = new JsonDefinitionSerializer();
	static ProcessQAStatsMonitor monitor;
	static ProcessRegistry procReg = new ProcessRegistry();
	static Instance jiraB, jiraD, jiraC;
	Map<String, Set<Instance>> input = new HashMap<>();
	
	@BeforeEach
	void setup() throws Exception {
		RuleService.setEvaluator(new ArlRuleEvaluator());
		ws = WorkspaceService.PUBLIC_WORKSPACE;
		procReg.inject(ws,new ProcessConfigBaseElementFactory(ws));
		RuleService.currentWorkspace = ws;
		EventDistributor eventDistrib = new EventDistributor();
		monitor = new ProcessQAStatsMonitor(new CurrentSystemTimeProvider());
		eventDistrib.registerHandler(monitor);
		picp = new ProcessInstanceChangeProcessor(ws, eventDistrib);
		WorkspaceListenerSequencer wsls = new WorkspaceListenerSequencer(ws);
	//	wsls.registerListener(repAnalyzer);
		wsls.registerListener(picp);
		typeJira = TestArtifacts.getJiraInstanceType(ws);
		jiraB =  TestArtifacts.getJiraInstance(ws, "jiraB");
		jiraC = TestArtifacts.getJiraInstance(ws, "jiraC");
		jiraD = TestArtifacts.getJiraInstance(ws, "jiraD");
		input.clear();
		
	}

	@Test
	void testBaselineProcessWorks() throws ProcessException {
		DTOs.Process proc1 = TestProcesses.getSimpleDTOSubprocess(ws);
		ProcessDefinition pd = procReg.createOrReplaceProcessDefinition(proc1, false).getProcDef();
		input.put("jiraIn", Set.of(jiraB));
		ProcessInstance p1 = procReg.instantiateProcess(pd, input).getKey();
		p1.printProcessToConsole("");
		assert(p1.getExpectedLifecycleState().equals(State.ACTIVE));
	}
	
	@Test
	void testReplaceWithIdentical() throws ProcessException {
		DTOs.Process proc1 = TestProcesses.getSimpleDTOSubprocess(ws);
		procReg.createOrReplaceProcessDefinition(proc1, false);
		DTOs.Process proc2 = TestProcesses.getSimpleDTOSubprocess(ws);
		ProcessDefinition pd = procReg.createOrReplaceProcessDefinition(proc2, false).getProcDef();
		input.put("jiraIn", Set.of(jiraB));
		ProcessInstance p1 = procReg.instantiateProcess(pd, input).getKey();
		p1.printProcessToConsole("");
		assert(p1.getExpectedLifecycleState().equals(State.ACTIVE));
	}
	
	@Test
	void testReplaceSpecWithoutPreexistingProcInstances() throws ProcessException {
		DTOs.Process proc1 = TestProcesses.getSimpleDTOSubprocess(ws);
		procReg.createOrReplaceProcessDefinition(proc1, false);
		//changing the processes, and re-registering it.
		DTOs.Process proc2 = TestProcesses.getSimpleDTOSubprocess(ws);
		DTOs.Step step2 = proc2.getStepByCode("subtask2");
		step2.getInput().remove("jiraIn");
		step2.getInput().put("issueIn", typeJira.name());
		step2.getConditions().put(Conditions.PRECONDITION, List.of(new DTOs.Constraint("self.in_issueIn->size() = 1")));
		step2.getConditions().put(Conditions.POSTCONDITION, List.of(new DTOs.Constraint("self.in_issueIn->size() = 1 and self.in_issueIn->forAll( issue | issue.state = 'Closed')"))); 
		DTOs.DecisionNode dn1 = proc2.getEntryNode();
		dn1.getMapping().clear();
		dn1.getMapping().add(new DTOs.Mapping(proc2.getCode(), "jiraIn", "subtask1", "jiraIn")); //into both steps
		dn1.getMapping().add(new DTOs.Mapping(proc2.getCode(), "jiraIn", step2.getCode(), "issueIn")); //into both steps
		ProcessDefinition pd = procReg.createOrReplaceProcessDefinition(proc2, false).getProcDef();
		
		input.put("jiraIn", Set.of(jiraB));
		ProcessInstance p1 = procReg.instantiateProcess(pd, input).getKey();
		p1.printProcessToConsole("");
		assert(p1.getExpectedLifecycleState().equals(State.ACTIVE));
				
	}

	@Test
	void testReplaceSpecWithPreexistingProcInstances() throws ProcessException {
		DTOs.Process proc1 = TestProcesses.getSimpleDTOSubprocess(ws);
		ProcessDefinition pd1 = procReg.createOrReplaceProcessDefinition(proc1, false).getProcDef();
		input.put("jiraIn", Set.of(jiraB));
		ProcessInstance p1 = procReg.instantiateProcess(pd1, input).getKey();
		p1.printProcessToConsole("");
		assert(p1.getExpectedLifecycleState().equals(State.ACTIVE));
		
		//changing the processes, and re-registering it.
		DTOs.Process proc2 = TestProcesses.getSimpleDTOSubprocess(ws);
		DTOs.Step step2 = proc2.getStepByCode("subtask2");
		step2.getInput().remove("jiraIn");
		step2.getInput().put("issueIn", typeJira.name());
		step2.getConditions().put(Conditions.PRECONDITION, List.of(new DTOs.Constraint( "self.in_issueIn->size() = 1")));
		step2.getConditions().put(Conditions.POSTCONDITION, List.of(new DTOs.Constraint("self.in_issueIn->size() = 1 and self.in_issueIn->forAll( issue | issue.state = 'Closed')"))); 
		DTOs.DecisionNode dn1 = proc2.getEntryNode();
		dn1.getMapping().clear();
		dn1.getMapping().add(new DTOs.Mapping(proc2.getCode(), "jiraIn", "subtask1", "jiraIn")); //into both steps
		dn1.getMapping().add(new DTOs.Mapping(proc2.getCode(), "jiraIn", step2.getCode(), "issueIn")); //into both steps
		ProcessDefinition pd = procReg.createOrReplaceProcessDefinition(proc2, true).getProcDef();
		
		ProcessInstance p2 = procReg.getProcess("TestSerializeProc1_[jiraB]");
		p2.printProcessToConsole("");
		assert(p2.getExpectedLifecycleState().equals(State.ACTIVE));
				
		
		
	}
	
	@Test
	void testReplaceSpecWithPreexistingProcInstancesNotReinstantiated() throws ProcessException {
		DTOs.Process proc1 = TestProcesses.getSimpleDTOSubprocess(ws);
		ProcessDefinition pd1 = procReg.createOrReplaceProcessDefinition(proc1, false).getProcDef();
		input.put("jiraIn", Set.of(jiraB));
		ProcessInstance p1 = procReg.instantiateProcess(pd1, input).getKey();
		p1.printProcessToConsole("");
		assert(p1.getExpectedLifecycleState().equals(State.ACTIVE));
		
		//changing the processes, and re-registering it.
		DTOs.Process proc2 = TestProcesses.getSimpleDTOSubprocess(ws);
		proc2.getInput().remove("jiraIn");
		proc2.getInput().put("issueIn", typeJira.name());
		proc2.getConditions().put(Conditions.PRECONDITION, List.of(new DTOs.Constraint("self.in_issueIn->size() = 1")));
		DTOs.Step step2 = proc2.getStepByCode("subtask2");
		step2.getInput().remove("jiraIn");
		step2.getInput().put("issueIn", typeJira.name());
		step2.getConditions().put(Conditions.PRECONDITION, List.of(new DTOs.Constraint("self.in_issueIn->size() = 1")));
		step2.getConditions().put(Conditions.POSTCONDITION, List.of(new DTOs.Constraint("self.in_issueIn->size() = 1 and self.in_issueIn->forAll( issue | issue.state = 'Closed')"))); 
		DTOs.DecisionNode dn1 = proc2.getEntryNode();
		dn1.getMapping().clear();
		dn1.getMapping().add(new DTOs.Mapping(proc2.getCode(), "issueIn", "subtask1", "jiraIn")); //into both steps
		dn1.getMapping().add(new DTOs.Mapping(proc2.getCode(), "issueIn", step2.getCode(), "issueIn")); //into both steps
		


		ProcessDeployResult result = procReg.createOrReplaceProcessDefinition(proc2, true);
		ProcessDefinition pd = result.getProcDef();
		ProcessInstance p2 = procReg.getProcess("TestSerializeProc1_[jiraB]");
		assert(p2 == null);
		assert(result.getInstanceErrors().get(0).getErrorType().equals("Input Invalid"));



		
	}
	
}
