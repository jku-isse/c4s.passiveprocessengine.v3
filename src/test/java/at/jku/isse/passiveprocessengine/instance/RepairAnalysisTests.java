package at.jku.isse.passiveprocessengine.instance;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import at.jku.isse.designspace.core.model.Cardinality;
import at.jku.isse.designspace.core.model.Instance;
import at.jku.isse.designspace.core.model.InstanceType;
import at.jku.isse.designspace.core.model.MapProperty;
import at.jku.isse.designspace.core.model.PropertyType;
import at.jku.isse.designspace.core.model.SetProperty;
import at.jku.isse.designspace.core.model.Workspace;
import at.jku.isse.designspace.core.service.WorkspaceService;
import at.jku.isse.designspace.rule.arl.evaluator.EvaluationNode;
import at.jku.isse.designspace.rule.arl.repair.RepairAction;
import at.jku.isse.designspace.rule.arl.repair.RepairNode;
import at.jku.isse.designspace.rule.arl.repair.order.RepairNodeScorer;
import at.jku.isse.designspace.rule.arl.repair.order.RepairStats;
import at.jku.isse.designspace.rule.arl.repair.order.RepairTreeSorter;
import at.jku.isse.designspace.rule.arl.repair.order.SortOnRepairPercentage;
import at.jku.isse.designspace.rule.arl.repair.order.SortOnRestriction;
import at.jku.isse.designspace.rule.checker.ArlRuleEvaluator;
import at.jku.isse.designspace.rule.checker.ConsistencyUtils;
import at.jku.isse.designspace.rule.model.ConsistencyRule;
import at.jku.isse.designspace.rule.model.ConsistencyRuleType;
import at.jku.isse.designspace.rule.model.ReservedNames;
import at.jku.isse.designspace.rule.model.Rule;
import at.jku.isse.designspace.rule.service.RuleService;
import at.jku.isse.passiveprocessengine.TestUtils;
import at.jku.isse.passiveprocessengine.WrapperCache;
import at.jku.isse.passiveprocessengine.definition.DecisionNodeDefinition;
import at.jku.isse.passiveprocessengine.definition.MappingDefinition;
import at.jku.isse.passiveprocessengine.definition.ProcessDefinition;
import at.jku.isse.passiveprocessengine.definition.ConstraintSpec;
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
import at.jku.isse.passiveprocessengine.monitoring.ITimeStampProvider;
import at.jku.isse.passiveprocessengine.monitoring.ProcessQAStatsMonitor;
import at.jku.isse.passiveprocessengine.monitoring.ProcessStats;
import at.jku.isse.passiveprocessengine.monitoring.RepairAnalyzer;
import at.jku.isse.passiveprocessengine.monitoring.RepairFeatureToggle;
import at.jku.isse.passiveprocessengine.monitoring.ReplayTimeProvider;
import at.jku.isse.passiveprocessengine.monitoring.UsageMonitor;

@ExtendWith(SpringExtension.class)
@SpringBootTest
public class RepairAnalysisTests {

	static Workspace ws;
	static InstanceType typeJira;

	static JsonDefinitionSerializer json = new JsonDefinitionSerializer();
	static ProcessQAStatsMonitor monitor;
	static RepairAnalyzer repAnalyzer;
	static RepairStats rs = new RepairStats();
	static RepairNodeScorer scorer=new SortOnRestriction();
	static ITimeStampProvider timeProvider=new CurrentSystemTimeProvider();
	static RepairFeatureToggle rtf=new RepairFeatureToggle(true,false,false);

	@BeforeEach
	void setup() throws Exception {
		RuleService.setEvaluator(new ArlRuleEvaluator());
		//ws = WorkspaceService.createWorkspace("test", WorkspaceService.PUBLIC_WORKSPACE, WorkspaceService.ANY_USER,
//				null, true, false);
		ws = WorkspaceService.PUBLIC_WORKSPACE;
		// ws = WorkspaceService.PUBLIC_WORKSPACE;
		RuleService.currentWorkspace = ws;
		EventDistributor eventDistrib = new EventDistributor();
		monitor = new ProcessQAStatsMonitor(new CurrentSystemTimeProvider());
		eventDistrib.registerHandler(monitor);
		ProcessInstanceChangeProcessor picp = new ProcessInstanceChangeProcessor(ws, eventDistrib);
		repAnalyzer = new RepairAnalyzer(ws, rs,scorer,timeProvider, new UsageMonitor(timeProvider),rtf);
		WorkspaceListenerSequencer wsls = new WorkspaceListenerSequencer(ws);
		wsls.registerListener(repAnalyzer);
		wsls.registerListener(picp);
		typeJira = TestArtifacts.getJiraInstanceType(ws);
	}

	
	@Test
	void testCollectionAndPropertyChange() throws ProcessException {
		Instance jiraB = TestArtifacts.getJiraInstance(ws, "jiraB");
		Instance jiraC = TestArtifacts.getJiraInstance(ws, "jiraC");
		Instance jiraD = TestArtifacts.getJiraInstance(ws, "jiraD");
		Instance jiraA = TestArtifacts.getJiraInstance(ws, "jiraA");

		ProcessDefinition procDef = TestProcesses.getComplexSingleStepProcessDefinition(ws);
		ProcessInstance proc = ProcessInstance.getInstance(ws, procDef);
		ws.concludeTransaction();
		proc.addInput("jiraIn", jiraA);
		ws.concludeTransaction();

		assert (proc.getProcessSteps().stream().filter(step -> step.getDefinition().getName().equals("sd1"))
				.allMatch(step -> (step.getOutput("jiraOut").size() == 0)
						&& step.getActualLifecycleState().equals(State.ENABLED)));

		// now lets add reqids to input jira
		TestArtifacts.addJiraToJira(jiraA, jiraB);
		// this should invalidate the output as we now have a requirement that doesn't
		// fulfill output condition
		// hence we should have a negative impact of adding jiraB, (even though we need
		// (requirement but not sufficient) it to fulfill the postcondition eventually)
		// on postcondition
		// yet as we don't have any other constraints evaluated here, we wont see the
		// effect of adding here just yet
		// here we only see the impact of setting jiraA to Open (from the initialization
		// of the jira item).
		ws.concludeTransaction();
		repAnalyzer.printImpact();
		repAnalyzer.getImpact().clear();

		TestArtifacts.setStateToJiraInstance(jiraA, JiraStates.Closed); // to trigger a reeval of the precondition
		// here we should see the adding of jiraB as positive as the repair of postcond
		// should include adding something to 'requirements' of jiraA,
		// (as this repair suggestion is not there, the action is neutral/none)
		// closing jiraA should be positive towards postCond, and negative towards
		// precondition
		ws.concludeTransaction();
		repAnalyzer.printImpact();
		repAnalyzer.getImpact().clear();

		// now lets set the state of JiraB so that is fulfills the requirement, but also
		// add another unfulfilling at the same time
		// overall the output constraint should remain false
		TestArtifacts.setStateToJiraInstance(jiraB, JiraStates.Closed);
		TestArtifacts.addJiraToJira(jiraA, jiraC);
		TestArtifacts.setStateToJiraInstance(jiraA, JiraStates.Open); // to trigger a reevaluation of the precondition
		ws.concludeTransaction();
		repAnalyzer.printImpact();
		repAnalyzer.getImpact().clear();

		proc.getProcessSteps().stream().forEach(step -> System.out.println(step));
		assert (proc.getProcessSteps().stream().filter(step -> step.getDefinition().getName().equals("sd1"))
				.allMatch(step -> (step.getOutput("jiraOut").size() == 2)
						&& step.getActualLifecycleState().equals(State.ACTIVE)));

		// finally we fulfill the completion constraint
		TestArtifacts.setStateToJiraInstance(jiraC, JiraStates.Closed);
		//TestArtifacts.setStateToJiraInstance(jiraA, JiraStates.Closed);
		// both changes are necessary

		ws.concludeTransaction();
		repAnalyzer.printImpact();
		repAnalyzer.getImpact().clear();

		System.out.println(proc);
		proc.getProcessSteps().stream().forEach(step -> System.out.println(step));
		proc.getDecisionNodeInstances().stream().forEach(dni -> System.out.println(dni));
		TestUtils.assertAllConstraintsAreValid(proc);
		assert (proc.getProcessSteps().stream().filter(step -> step.getDefinition().getName().equals("sd1"))
				.allMatch(step -> (step.getActualLifecycleState().equals(State.COMPLETED))));

		monitor.calcFinalStats();
		ProcessStats stats = monitor.stats.get(proc);
		assert (stats.isProcessCompleted() == false);
	}
	

	@Test
	void testCollectionAndPropertyChangeOnQAandPrePost() throws ProcessException {
		Instance jiraA = TestArtifacts.getJiraInstance(ws, "jiraA");
		Instance jiraB = TestArtifacts.getJiraInstance(ws, "jiraB");
		Instance jiraC = TestArtifacts.getJiraInstance(ws, "jiraC");
		Instance jiraD = TestArtifacts.getJiraInstance(ws, "jiraD");

		ProcessDefinition procDef = getSingleStepProcessDefinition(ws);
		ProcessInstance proc = ProcessInstance.getInstance(ws, procDef);
		proc.addInput("jiraIn", jiraA);
		ws.concludeTransaction();
		repAnalyzer.printImpact();
		repAnalyzer.getImpact().clear();

		assert (proc.getProcessSteps().stream().filter(step -> step.getDefinition().getName().equals("sd1"))
				.allMatch(step -> (step.getOutput("jiraOut").size() == 0)
						&& step.getActualLifecycleState().equals(State.ENABLED)));

		// now lets add reqids to input jira and set it to closed to trigger QA
		TestArtifacts.addJiraToJira(jiraA, jiraB);
		TestArtifacts.setStateToJiraInstance(jiraB, JiraStates.Closed);
		TestArtifacts.setStateToJiraInstance(jiraA, JiraStates.Closed);
		// this should invalidate the output as we now have a requirement that fulfills
		// part of post condition but postcond is not complete yet
		// hence we should have a positive impact of adding jiraB, (even though we need
		// (req but not sufficient) it to fulfill the postcond eventually) on postcond
		ws.concludeTransaction();
		repAnalyzer.printImpact();
		repAnalyzer.getImpact().clear();

		// now lets add more reqids to input jira
		TestArtifacts.addJiraToJira(jiraA, jiraC);
		TestArtifacts.setStateToJiraInstance(jiraA, JiraStates.Open);
		// here we should see the adding of jiraC as negative on qaspec1 and
		// neutral/none on outcond
		ws.concludeTransaction();
		repAnalyzer.printImpact();
		repAnalyzer.getImpact().clear();

		// now lets inverse the things at the same time again, by adding another ref
		TestArtifacts.addJiraToJira(jiraA, jiraD);
		TestArtifacts.setStateToJiraInstance(jiraD, JiraStates.Closed);
		TestArtifacts.setStateToJiraInstance(jiraA, JiraStates.Closed);
		// expecting that adding jiraD even if closed, is negative on Postcond, neutral
		// on QA as qa is unfulfilled still
		// FIXME: NOT THE CASE DUE TO BUG IN REPAIR GENERATION!!!
		ws.concludeTransaction();
		repAnalyzer.printImpact();
		repAnalyzer.getImpact().clear();

		// now lets wrap things up by removing open ref issue
		TestArtifacts.removeJiraFromJira(jiraA, jiraC);
		// expecting this to be positive on Post and QA
		ws.concludeTransaction();
		repAnalyzer.printImpact();
		repAnalyzer.getImpact().clear();

		System.out.println(proc);
		proc.getProcessSteps().stream().forEach(step -> System.out.println(step));
		proc.getDecisionNodeInstances().stream().forEach(dni -> System.out.println(dni));
		TestUtils.assertAllConstraintsAreValid(proc);
		assert (proc.getProcessSteps().stream().filter(step -> step.getDefinition().getName().equals("sd1"))
				.allMatch(step -> (step.getOutput("jiraOut").size() == 2)
						&& step.getActualLifecycleState().equals(State.COMPLETED)));

		monitor.calcFinalStats();
		ProcessStats stats = monitor.stats.get(proc);

		System.out.println(repAnalyzer.stats2Json(repAnalyzer.getSerializableStats()));
		assert (stats.isProcessCompleted() == false);

	}

	@Test
	void testCollectionAndPropertyChangeOnQAandPrePost2() throws ProcessException {
		Instance jiraA = TestArtifacts.getJiraInstance(ws, "jiraA");
		Instance jiraB = TestArtifacts.getJiraInstance(ws, "jiraB");
		Instance jiraC = TestArtifacts.getJiraInstance(ws, "jiraC");
		Instance jiraD = TestArtifacts.getJiraInstance(ws, "jiraD");

		ProcessDefinition procDef = getSingleStepProcessDefinition(ws);
		ProcessInstance proc = ProcessInstance.getInstance(ws, procDef);
		proc.addInput("jiraIn", jiraA);
		ws.concludeTransaction();
		repAnalyzer.printImpact();
		repAnalyzer.getImpact().clear();

		assert (proc.getProcessSteps().stream().filter(step -> step.getDefinition().getName().equals("sd1"))
				.allMatch(step -> (step.getOutput("jiraOut").size() == 0)
						&& step.getActualLifecycleState().equals(State.ENABLED)));

		// now lets add reqids to input jira and set it to closed to trigger QA
		TestArtifacts.addJiraToJira(jiraA, jiraB);
		TestArtifacts.setStateToJiraInstance(jiraB, JiraStates.Closed);
		TestArtifacts.setStateToJiraInstance(jiraA, JiraStates.Closed);
		TestArtifacts.setStateToJiraInstance(jiraD, JiraStates.Closed); // lets do this early before adding jira D
																		// further below.
		// this should invalidate the output as we now have a requirement that fulfills
		// part of post condition but postcond is not complete yet
		// hence we should have a positive impact of adding jiraB, (even though we need
		// (req but not sufficient) it to fulfill the postcond eventually) on postcond
		ws.concludeTransaction();
		repAnalyzer.printImpact();
		repAnalyzer.getImpact().clear();

		// now lets add more reqids to input jira
		TestArtifacts.addJiraToJira(jiraA, jiraC);
		TestArtifacts.setStateToJiraInstance(jiraA, JiraStates.Open);
		// here we should see the adding of jiraC as negative on qaspec1 and
		// neutral/none on outcond
		ws.concludeTransaction();
		repAnalyzer.printImpact();
		repAnalyzer.getImpact().clear();

		// now lets inverse the things at the same time again, by adding another ref
		TestArtifacts.addJiraToJira(jiraA, jiraD);
		TestArtifacts.setStateToJiraInstance(jiraA, JiraStates.Closed);
		ws.concludeTransaction();
		repAnalyzer.printImpact();
		repAnalyzer.getImpact().clear();

		// now lets wrap things up by removing open ref issue
		TestArtifacts.removeJiraFromJira(jiraA, jiraC);
		// expecting this to be positive on Post and QA
		ws.concludeTransaction();
		repAnalyzer.printImpact();
		repAnalyzer.getImpact().clear();

		System.out.println(proc);
		proc.getProcessSteps().stream().forEach(step -> System.out.println(step));
		proc.getDecisionNodeInstances().stream().forEach(dni -> System.out.println(dni));
		TestUtils.assertAllConstraintsAreValid(proc);
		assert (proc.getProcessSteps().stream().filter(step -> step.getDefinition().getName().equals("sd1"))
				.allMatch(step -> (step.getOutput("jiraOut").size() == 2)
						&& step.getActualLifecycleState().equals(State.COMPLETED)));

		monitor.calcFinalStats();
		ProcessStats stats = monitor.stats.get(proc);

		assert (stats.isProcessCompleted() == false);
		System.out.println(repAnalyzer.stats2Json(repAnalyzer.getSerializableStats()));

	}

	public static ProcessDefinition getSingleStepProcessDefinition(Workspace ws) throws ProcessException {
		InstanceType typeJira = TestArtifacts.getJiraInstanceType(ws);
		ProcessDefinition procDef = ProcessDefinition.getInstance("proc1", ws);
		procDef.addExpectedInput("jiraIn", typeJira);
		procDef.addExpectedOutput("jiraOut", typeJira);
		DecisionNodeDefinition dnd1 = procDef.createDecisionNodeDefinition("dnd1", ws);
		DecisionNodeDefinition dnd2 = procDef.createDecisionNodeDefinition("dnd2", ws);

		StepDefinition sd1 = procDef.createStepDefinition("sd1", ws);
		sd1.addExpectedInput("jiraIn", typeJira);
		sd1.addExpectedOutput("jiraOut", typeJira);
		sd1.addInputToOutputMappingRule("jiraOut",
				"self.in_jiraIn" + "->any()" + "->asType(<" + typeJira.getQualifiedName() + ">)" + ".requirements"
						+ "->asSet() " + "->symmetricDifference(self.out_jiraOut) " + "->size() = 0");
		sd1.setCondition(Conditions.PRECONDITION,
				"self.in_jiraIn->size() = 1 " + "and self.in_jiraIn->forAll( issue | issue.state = 'Open') ");
		sd1.setCondition(Conditions.POSTCONDITION,
				"self.out_jiraOut->size() = 2 " + "and self.in_jiraIn->forAll( issue2 | issue2.state = 'Closed') ");
		ConstraintSpec qa2 = ConstraintSpec.createInstance(Conditions.QA,
				"sd1-qa1-state",
				"self.out_jiraOut->size() > 0 and self.out_jiraOut->forAll( issue | issue.state = 'Closed')", "All linked issues should be closed", 2, ws);
		sd1.addQAConstraint(qa2);
		sd1.setInDND(dnd1);
		sd1.setOutDND(dnd2);

		dnd1.addDataMappingDefinition(
				MappingDefinition.getInstance(procDef.getName(), "jiraIn", sd1.getName(), "jiraIn", ws));
		procDef.initializeInstanceTypes(false);
		return procDef;
	}
}
