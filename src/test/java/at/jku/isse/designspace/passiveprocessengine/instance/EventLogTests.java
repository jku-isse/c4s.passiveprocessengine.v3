package at.jku.isse.designspace.passiveprocessengine.instance;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Map;
import java.util.Optional;
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
import at.jku.isse.designspace.passiveprocessengine.TestUtils;
import at.jku.isse.designspace.rule.arl.repair.RepairNode;
import at.jku.isse.designspace.rule.checker.ArlRuleEvaluator;
import at.jku.isse.designspace.rule.checker.ConsistencyUtils;
import at.jku.isse.designspace.rule.model.ConsistencyRule;
import at.jku.isse.designspace.rule.model.ConsistencyRuleType;
import at.jku.isse.designspace.rule.model.Rule;
import at.jku.isse.designspace.rule.service.RuleService;
import at.jku.isse.passiveprocessengine.WrapperCache;
import at.jku.isse.passiveprocessengine.definition.DecisionNodeDefinition;
import at.jku.isse.passiveprocessengine.definition.MappingDefinition;
import at.jku.isse.passiveprocessengine.definition.ProcessDefinition;
import at.jku.isse.passiveprocessengine.definition.QAConstraintSpec;
import at.jku.isse.passiveprocessengine.definition.StepDefinition;
import at.jku.isse.passiveprocessengine.definition.DecisionNodeDefinition.InFlowType;
import at.jku.isse.passiveprocessengine.definition.serialization.DTOs;
import at.jku.isse.passiveprocessengine.definition.serialization.DefinitionTransformer;
import at.jku.isse.passiveprocessengine.definition.serialization.JsonDefinitionSerializer;
import at.jku.isse.passiveprocessengine.demo.TestArtifacts;
import at.jku.isse.passiveprocessengine.demo.TestProcesses;
import at.jku.isse.passiveprocessengine.demo.TestArtifacts.JiraStates;
import at.jku.isse.passiveprocessengine.instance.ConstraintWrapper;
import at.jku.isse.passiveprocessengine.instance.ProcessException;
import at.jku.isse.passiveprocessengine.instance.ProcessInstance;
import at.jku.isse.passiveprocessengine.instance.ProcessInstanceChangeProcessor;
import at.jku.isse.passiveprocessengine.instance.ProcessStep;
import at.jku.isse.passiveprocessengine.instance.ProcessStep.CoreProperties;
import at.jku.isse.passiveprocessengine.instance.StepLifecycle.Conditions;
import at.jku.isse.passiveprocessengine.instance.StepLifecycle.State;
import at.jku.isse.passiveprocessengine.instance.messages.EventDistributor;
import at.jku.isse.passiveprocessengine.instance.messages.WorkspaceListenerSequencer;
import at.jku.isse.passiveprocessengine.monitoring.CurrentSystemTimeProvider;
import at.jku.isse.passiveprocessengine.monitoring.ProcessQAStatsMonitor;
import at.jku.isse.passiveprocessengine.monitoring.ProcessStateChangeLog;
import at.jku.isse.passiveprocessengine.monitoring.ProcessStats;
import at.jku.isse.passiveprocessengine.monitoring.ProcessStepStats;
import at.jku.isse.passiveprocessengine.monitoring.RepairAnalyzer;

@ExtendWith(SpringExtension.class)
@SpringBootTest
public
class EventLogTests {

	static Workspace ws;
	static InstanceType typeJira;
	ProcessInstanceChangeProcessor picp;
	static JsonDefinitionSerializer json = new JsonDefinitionSerializer();
	static ProcessQAStatsMonitor monitor;
	static RepairAnalyzer repAnalyzer;
	static ProcessStateChangeLog logs = new ProcessStateChangeLog();
	
	@BeforeEach
	void setup() throws Exception {
		RuleService.setEvaluator(new ArlRuleEvaluator());
		ws = WorkspaceService.createWorkspace("test", WorkspaceService.PUBLIC_WORKSPACE, WorkspaceService.ANY_USER, null, true, false);
		//ws = WorkspaceService.PUBLIC_WORKSPACE;
		RuleService.currentWorkspace = ws;
		EventDistributor eventDistrib = new EventDistributor();
		monitor = new ProcessQAStatsMonitor(new CurrentSystemTimeProvider());
		eventDistrib.registerHandler(monitor);
		eventDistrib.registerHandler(logs);
		picp = new ProcessInstanceChangeProcessor(ws, eventDistrib);
		repAnalyzer = new RepairAnalyzer(ws);
		WorkspaceListenerSequencer wsls = new WorkspaceListenerSequencer(ws);
		wsls.registerListener(repAnalyzer);
		wsls.registerListener(picp);
		typeJira = TestArtifacts.getJiraInstanceType(ws);
	}

	
	@Test
	void testEventsUponImmediateDatapropagation() throws ProcessException {
		Instance jiraA = TestArtifacts.getJiraInstance(ws, "jiraA");
		Instance reqB =  TestArtifacts.getJiraInstance(ws, "reqB");
		Instance reqC = TestArtifacts.getJiraInstance(ws, "reqC");
		Instance reqD = TestArtifacts.getJiraInstance(ws, "reqD");
		
		ProcessDefinition procDef = getPrematureDetectableProcessDefinition(ws, true);
		procDef.getPrematureTriggers().entrySet().stream().forEach(entry -> System.out.println(entry.getKey() + ": "+entry.getValue()));
		ProcessInstance proc = ProcessInstance.getInstance(ws, procDef);
		proc.addInput("jiraIn", jiraA);
		ws.concludeTransaction();
		
		assert(proc.getProcessSteps().stream()
				.filter(step -> step.getDefinition().getName().equals("sd1") )
				.allMatch(step -> (step.getOutput("jiraOut").size() == 0) && step.getActualLifecycleState().equals(State.ENABLED) ));
		TestUtils.printFullProcessToLog(proc);
		assert(proc.getProcessSteps().size() == 2);
		assert(proc.getProcessSteps().stream()
				.filter(step -> step.getDefinition().getName().equals("sd2") )
				.allMatch(step -> (step.isInPrematureOperationModeDueTo().size() == 1)
						&& (step.isInUnsafeOperationModeDueTo().size() == 1)
						&& (step.getInput("reqIn").size() == 0) 
						&& step.getActualLifecycleState().equals(State.AVAILABLE) ));
		
		TestArtifacts.addJiraToJira(jiraA, reqB); // add a requirement 
		ws.concludeTransaction();
		TestUtils.printFullProcessToLog(proc);
		assert(proc.getProcessSteps().stream()
				.filter(step -> step.getDefinition().getName().equals("sd2") )
				.allMatch(step -> (step.isInPrematureOperationModeDueTo().size() == 1)
						&& (step.isInUnsafeOperationModeDueTo().size() == 1)
						&& (step.getInput("reqIn").size() == 1) 
						&& step.getActualLifecycleState().equals(State.ENABLED) ));
		
		TestArtifacts.setStateToJiraInstance(reqB, JiraStates.Released); //and  set it to Released
		ws.concludeTransaction();
		TestUtils.printFullProcessToLog(proc);
		TestUtils.assertAllConstraintsAreValid(proc);
		assert(proc.getProcessSteps().stream()
				.filter(step -> step.getDefinition().getName().equals("sd2") )
				.allMatch(step -> (step.isInPrematureOperationModeDueTo().size() == 1)
						&& (step.isInUnsafeOperationModeDueTo().size() == 0)
						&& (step.getInput("reqIn").size() == 1) 
						&& step.getActualLifecycleState().equals(State.COMPLETED) ));
		assert(proc.getProcessSteps().size() == 2);
		final Map<ProcessStep, ProcessStepStats> stepStats = monitor.stats.get(proc).getPerStepStats();
		ProcessStepStats sd2Stats = proc.getProcessSteps().stream()
				.filter(step -> step.getDefinition().getName().equals("sd2"))
				.map(step -> stepStats.get(step))
				.findAny().get();
		assert(sd2Stats.getPrematurelyStarted() != null);
		assert(sd2Stats.getUnsafeStarted() == null);
		assert(sd2Stats.getPrematureIntervals().size() == 0);
		assert(sd2Stats.getUnsafeIntervals().size() == 1);
		
		// repair now
		TestArtifacts.addJiraToJira(jiraA, reqC); // add a requirement 
		TestArtifacts.setStateToJiraInstance(reqC, JiraStates.Released);
		TestArtifacts.setStateToJiraInstance(jiraA, JiraStates.Closed);// and now also complete step
		ws.concludeTransaction();
		TestUtils.printFullProcessToLog(proc);
		TestUtils.assertAllConstraintsAreValid(proc);
		ProcessStep sd2 = proc.getProcessSteps().stream()
				.filter(step -> step.getDefinition().getName().equals("sd2") ).findAny().get();
		assert(	sd2.isInPrematureOperationModeDueTo().size() == 0);
		assert(sd2.isInUnsafeOperationModeDueTo().size() == 0);
		assert(sd2.getInput("reqIn").size() == 2) ;
		assert(sd2.getActualLifecycleState().equals(State.COMPLETED) );
		assert(proc.getProcessSteps().size() == 2);
		assert(sd2Stats.getPrematurelyStarted() == null);
		assert(sd2Stats.getPrematureIntervals().size() == 1);
		
		System.out.println(logs.getEventLogAsJson(proc.getInstance().id().value()));
		
		//assert(logs.getEventLogAsJson(proc.getName()).size() == 10);
	}
	
	
	
	public static ProcessDefinition getPrematureDetectableProcessDefinition(Workspace ws) throws ProcessException {
		return getPrematureDetectableProcessDefinition(ws, false);
	}
	
	public static ProcessDefinition getPrematureDetectableProcessDefinition(Workspace ws, boolean useImmediatePropagationInsteadOfRule) throws ProcessException {
		InstanceType typeJira = TestArtifacts.getJiraInstanceType(ws);
		ProcessDefinition procDef = ProcessDefinition.getInstance("proc1", ws);
		procDef.addExpectedInput("jiraIn", typeJira);	
		DecisionNodeDefinition dnd1 = procDef.createDecisionNodeDefinition("dnd1", ws);
		DecisionNodeDefinition dnd2 =  procDef.createDecisionNodeDefinition("dnd2", ws);
		DecisionNodeDefinition dnd3 =  procDef.createDecisionNodeDefinition("dnd3", ws);
		
		StepDefinition sd1 = procDef.createStepDefinition("sd1", ws);
		sd1.setSpecOrderIndex(0);
		sd1.addExpectedInput("jiraIn", typeJira);
		sd1.addExpectedOutput("jiraOut", typeJira);
		sd1.addInputToOutputMappingRule("jiraOut", 
				"self.in_jiraIn"
					+ "->any()"
					+ "->asType(<"+typeJira.getQualifiedName()+">)"
							+ ".requirements"
								+ "->asSet() "  
								+"->symmetricDifference(self.out_jiraOut) " +  
								"->size() = 0"
								); 		
		
		sd1.setCondition(Conditions.PRECONDITION, "self.in_jiraIn->size() = 1 "
				+ "and self.in_jiraIn->forAll( issue | issue.state = 'Open') ");
		sd1.setCondition(Conditions.POSTCONDITION, "self.out_jiraOut->size() = 2 "
				+ "and self.in_jiraIn->forAll( issue2 | issue2.state = 'Closed') ");
		QAConstraintSpec qa1 = QAConstraintSpec.createInstance("sd1-qa1-state", "self.out_jiraOut->size() > 0 and self.out_jiraOut->forAll( issue | issue.state = 'ReadyForReview' or issue.state = 'Released')", "All linked requirements should be ready for review", 2,ws);
		sd1.addQAConstraint(qa1);
		sd1.setInDND(dnd1);
		sd1.setOutDND(dnd2);
		
		StepDefinition sd2 = procDef.createStepDefinition("sd2", ws);
		sd2.setSpecOrderIndex(1);
		sd2.addExpectedInput("reqIn", typeJira);
		sd2.setCondition(Conditions.PRECONDITION, "self.in_reqIn->size() >= 1");
		sd2.setCondition(Conditions.ACTIVATION, "self.in_reqIn->size() >= 1 and self.in_reqIn->select( issue | issue.state = 'Released')->size() > 0");
		sd2.setCondition(Conditions.POSTCONDITION, "self.in_reqIn->size() >= 1 and self.in_reqIn->forAll( issue | issue.state = 'Released')"); // TODO: we somehow need to reason whether this min input size needs to be included
		sd2.setInDND(dnd2);
		sd2.setOutDND(dnd3);
		
		
		dnd1.addDataMappingDefinition(MappingDefinition.getInstance(procDef.getName(), "jiraIn", sd1.getName(), "jiraIn",  ws));
		dnd2.addDataMappingDefinition(MappingDefinition.getInstance(sd1.getName(), "jiraOut", sd2.getName(), "reqIn",  ws));
		procDef.setImmediateDataPropagationEnabled(useImmediatePropagationInsteadOfRule);
		procDef.initializeInstanceTypes(!useImmediatePropagationInsteadOfRule);
		return procDef;
	}
}
