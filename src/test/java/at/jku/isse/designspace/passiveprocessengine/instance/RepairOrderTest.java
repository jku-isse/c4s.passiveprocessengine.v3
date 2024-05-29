package at.jku.isse.designspace.passiveprocessengine.instance;

import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import java.util.Iterator;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import at.jku.isse.designspace.core.model.Instance;
import at.jku.isse.designspace.core.model.InstanceType;
import at.jku.isse.designspace.core.model.SetProperty;
import at.jku.isse.designspace.core.model.Workspace;
import at.jku.isse.designspace.core.service.WorkspaceService;
import at.jku.isse.designspace.rule.arl.repair.RepairNode;
import at.jku.isse.designspace.rule.arl.repair.analyzer.RepairAnalyzer1;
import at.jku.isse.designspace.rule.arl.repair.analyzer.RepairFeatureToggle;
import at.jku.isse.designspace.rule.arl.repair.ranking.NoSort;
import at.jku.isse.designspace.rule.arl.repair.ranking.RepairNodeScoringMechanism;
import at.jku.isse.designspace.rule.arl.repair.ranking.RepairTemplateLog;
import at.jku.isse.designspace.rule.arl.repair.ranking.RepairTreeSorter;
import at.jku.isse.designspace.rule.arl.repair.ranking.SortOnRepairPercentage;
import at.jku.isse.designspace.rule.arl.repair.ranking.SortOnRestriction;
import at.jku.isse.designspace.rule.checker.ArlRuleEvaluator;
import at.jku.isse.designspace.rule.checker.ConsistencyUtils;
import at.jku.isse.designspace.rule.model.ConsistencyRule;
import at.jku.isse.designspace.rule.model.ConsistencyRuleType;
import at.jku.isse.designspace.rule.model.ReservedNames;
import at.jku.isse.designspace.rule.model.Rule;
import at.jku.isse.designspace.rule.service.RuleService;
import at.jku.isse.passiveprocessengine.definition.ConstraintSpec;
import at.jku.isse.passiveprocessengine.definition.DecisionNodeDefinition;
import at.jku.isse.passiveprocessengine.definition.MappingDefinition;
import at.jku.isse.passiveprocessengine.definition.ProcessDefinition;
import at.jku.isse.passiveprocessengine.definition.QAConstraintSpec;
import at.jku.isse.passiveprocessengine.definition.StepDefinition;
import at.jku.isse.passiveprocessengine.definition.serialization.JsonDefinitionSerializer;
import at.jku.isse.passiveprocessengine.demo.TestArtifacts;
import at.jku.isse.passiveprocessengine.demo.TestProcesses;
import at.jku.isse.passiveprocessengine.demo.TestArtifacts.JiraStates;
import at.jku.isse.passiveprocessengine.instance.ProcessException;
import at.jku.isse.passiveprocessengine.instance.ProcessInstance;
import at.jku.isse.passiveprocessengine.instance.ProcessInstanceChangeProcessor;
import at.jku.isse.passiveprocessengine.instance.StepLifecycle.Conditions;
import at.jku.isse.passiveprocessengine.instance.messages.EventDistributor;
import at.jku.isse.passiveprocessengine.instance.messages.WorkspaceListenerSequencer;
import at.jku.isse.passiveprocessengine.monitoring.CurrentSystemTimeProvider;
import at.jku.isse.passiveprocessengine.monitoring.ProcessQAStatsMonitor;
import at.jku.isse.passiveprocessengine.monitoring.ReplayTimeProvider;
import at.jku.isse.passiveprocessengine.monitoring.UsageMonitor;

@ExtendWith(SpringExtension.class)
@SpringBootTest
public class RepairOrderTest {
	static Workspace ws;
	static InstanceType typeJira;

	static JsonDefinitionSerializer json = new JsonDefinitionSerializer();
	static ProcessQAStatsMonitor monitor;
	static RepairAnalyzer1 repAnalyzer;
	static RepairTemplateLog rs = new RepairTemplateLog();
	static RepairNodeScoringMechanism scorer=new SortOnRestriction();
	static ReplayTimeProvider timeProvider=new ReplayTimeProvider();
	@BeforeEach
	void setup() throws Exception {
		RuleService.setEvaluator(new ArlRuleEvaluator());
		ws = WorkspaceService.PUBLIC_WORKSPACE; //.createWorkspace("test", WorkspaceService.PUBLIC_WORKSPACE, WorkspaceService.ANY_USER,
				//null, true, false);
		// ws = WorkspaceService.PUBLIC_WORKSPACE;
		RuleService.currentWorkspace = ws;
		EventDistributor eventDistrib = new EventDistributor();
		monitor = new ProcessQAStatsMonitor(new CurrentSystemTimeProvider());
		eventDistrib.registerHandler(monitor);
		ProcessInstanceChangeProcessor picp = new ProcessInstanceChangeProcessor(ws, eventDistrib);
		repAnalyzer = new RepairAnalyzer1(ws, rs,scorer,timeProvider, new UsageMonitor(timeProvider),new RepairFeatureToggle());
		WorkspaceListenerSequencer wsls = new WorkspaceListenerSequencer(ws);
		wsls.registerListener(repAnalyzer);
		wsls.registerListener(picp);
		typeJira = TestArtifacts.getJiraInstanceType(ws);
	}

	// The test checks if the property have been collected correctly
	@Test
	void testPropertyRanking() throws ProcessException {
		Instance jiraA = TestArtifacts.getJiraInstance(ws, "jiraA");
		Instance jiraB = TestArtifacts.getJiraInstance(ws, "jiraB");
		ProcessDefinition procDef = TestProcesses.getComplexSingleStepProcessDefinition(ws);
		ProcessInstance proc = ProcessInstance.getInstance(ws, procDef);
		proc.addInput("jiraIn", jiraA);
		ws.concludeTransaction();
		repAnalyzer.printImpact();
		repAnalyzer.getImpact().clear();
		TestArtifacts.setStateToJiraInstance(jiraA, JiraStates.Closed);
		ws.concludeTransaction();
		repAnalyzer.printImpact();
		repAnalyzer.getImpact().clear();
		this.helperFunction();
	}
	// The test checks if at the end the program it calculates the correct
	//select and unselect count of the nodes. Test Passed
	@Test
	void testRepairTreeSelectUnSelecrList() throws ProcessException {
		Instance jiraA = TestArtifacts.getJiraInstance(ws, "jiraA");
		Instance jiraB = TestArtifacts.getJiraInstance(ws, "jiraB");
		ProcessDefinition procDef = TestProcesses.getComplexSingleStepProcessDefinition(ws);
		ProcessInstance proc = ProcessInstance.getInstance(ws, procDef);
		proc.addInput("jiraIn", jiraA);
		ws.concludeTransaction();
		repAnalyzer.printImpact();
		repAnalyzer.getImpact().clear();
		//rs.display_SelectedRep_DS();
		//rs.display_UnSelectedRep_DS();
		TestArtifacts.addJiraToJira(jiraA, jiraB);
		TestArtifacts.setStateToJiraInstance(jiraA, JiraStates.Closed);
		ws.concludeTransaction();
		repAnalyzer.printImpact();
		repAnalyzer.getImpact().clear();
		//rs.display_SelectedRep_DS();
		//rs.display_UnSelectedRep_DS();
		TestArtifacts.setStateToJiraInstance(jiraA, JiraStates.Open);
		TestArtifacts.setStateToJiraInstance(jiraB, JiraStates.Closed);
		ws.concludeTransaction();
		repAnalyzer.printImpact();
		repAnalyzer.getImpact().clear();
		//rs.display_SelectedRep_DS();
		//rs.display_UnSelectedRep_DS();
		
	}

	// The test checks if the repair tree nodes percentage is being calculated and
	// communicated correctly or not. Passed
	@Test
	void testRepairTreePercentage() throws ProcessException {
		Instance jiraA = TestArtifacts.getJiraInstance(ws, "jiraA");
		Instance jiraB = TestArtifacts.getJiraInstance(ws, "jiraB");
		ProcessDefinition procDef = TestProcesses.getComplexSingleStepProcessDefinition(ws);
		ProcessInstance proc = ProcessInstance.getInstance(ws, procDef);
		proc.addInput("jiraIn", jiraA);
		ws.concludeTransaction();
		repAnalyzer.printImpact();
		repAnalyzer.getImpact().clear();
		TestArtifacts.setStateToJiraInstance(jiraA, JiraStates.Closed);
		TestArtifacts.addJiraToJira(jiraA, jiraB);
		ws.concludeTransaction();
		repAnalyzer.printImpact();
		repAnalyzer.getImpact().clear();
		TestArtifacts.setStateToJiraInstance(jiraA, JiraStates.Open);
		TestArtifacts.setStateToJiraInstance(jiraB, JiraStates.Closed);
		ws.concludeTransaction();
		repAnalyzer.printImpact();
		repAnalyzer.getImpact().clear();
		//rs.display_SelectedRep_DS();
		//rs.display_UnSelectedRep_DS();
		
		// Here the repair tree should show the add repair on top with high percentage.
		Instance jiraC = TestArtifacts.getJiraInstance(ws, "jiraC");
		Instance jiraD = TestArtifacts.getJiraInstance(ws, "jiraD");
		proc.removeInput("jiraIn", jiraA);
		proc.addInput("jiraIn", jiraC);
		TestArtifacts.setStateToJiraInstance(jiraC, JiraStates.Closed);
		ws.concludeTransaction();
		repAnalyzer.printImpact();
		repAnalyzer.getImpact().clear();
		this.helperFunction();
		//rs.display_SelectedRep_DS();
		//rs.display_UnSelectedRep_DS();
	}

	//The test checks for the inverse operations in case of add and remove.
	@Test
	void testClientOperationInverseAddRemove() throws ProcessException {
		Instance jiraA = TestArtifacts.getJiraInstance(ws, "jiraA");
		Instance jiraB = TestArtifacts.getJiraInstance(ws, "jiraB");
		ProcessDefinition procDef = TestProcesses.getComplexSingleStepProcessDefinition(ws);
		ProcessInstance proc = ProcessInstance.getInstance(ws, procDef);
		proc.addInput("jiraIn", jiraA);
		ws.concludeTransaction();
		repAnalyzer.printImpact();
		repAnalyzer.getImpact().clear();
		TestArtifacts.setStateToJiraInstance(jiraA, JiraStates.Closed);
		TestArtifacts.addJiraToJira(jiraA, jiraB);
		ws.concludeTransaction();
		repAnalyzer.printImpact();
		repAnalyzer.getImpact().clear();
		TestArtifacts.setStateToJiraInstance(jiraA, JiraStates.Open);
		TestArtifacts.removeJiraFromJira(jiraA, jiraB);
		ws.concludeTransaction();
		repAnalyzer.printImpact();
		repAnalyzer.getImpact().clear();
		//rs.display_SelectedRep_DS();
		//rs.display_UnSelectedRep_DS();
		TestArtifacts.addJiraToJira(jiraA, jiraB);
		TestArtifacts.setStateToJiraInstance(jiraB, JiraStates.Closed);
		ws.concludeTransaction();
		repAnalyzer.printImpact();
		repAnalyzer.getImpact().clear();
		this.helperFunction();
		//rs.display_SelectedRep_DS();
		//rs.display_UnSelectedRep_DS();
	}
// The test to check if remove template can become a part of the select nodes. Test Pass
	@Test
	void testClientOperationRemoveToFullfillCRE() throws ProcessException {
		Instance jiraA = TestArtifacts.getJiraInstance(ws, "jiraA");
		Instance jiraB = TestArtifacts.getJiraInstance(ws, "jiraB");
		Instance jiraC = TestArtifacts.getJiraInstance(ws, "jiraC");
		ProcessDefinition procDef = TestProcesses.getComplexSingleStepProcessDefinition(ws);
		ProcessInstance proc = ProcessInstance.getInstance(ws, procDef);
		proc.addInput("jiraIn", jiraA);
		ws.concludeTransaction();
		repAnalyzer.printImpact();
		repAnalyzer.getImpact().clear();
		TestArtifacts.setStateToJiraInstance(jiraA, JiraStates.Closed);
		TestArtifacts.addJiraToJira(jiraA, jiraB);
		TestArtifacts.addJiraToJira(jiraA, jiraC);
		ws.concludeTransaction();
		repAnalyzer.printImpact();
		repAnalyzer.getImpact().clear();
		TestArtifacts.setStateToJiraInstance(jiraA, JiraStates.Open);
		TestArtifacts.setStateToJiraInstance(jiraC, JiraStates.Closed);
		TestArtifacts.removeJiraFromJira(jiraA, jiraB);
		ws.concludeTransaction();
		repAnalyzer.printImpact();
		repAnalyzer.getImpact().clear();
		//rs.display_SelectedRep_DS();
		//rs.display_UnSelectedRep_DS();
	}
	
	// The test check for the inverse operation in case of update.
	@Test
	void testOperationInverseUpdate() throws ProcessException {
		Instance jiraA = TestArtifacts.getJiraInstance(ws, "jiraA");
		Instance jiraB = TestArtifacts.getJiraInstance(ws, "jiraB");
		Instance jiraC = TestArtifacts.getJiraInstance(ws, "jiraC");
		ProcessDefinition procDef = TestProcesses.getComplexSingleStepProcessDefinition(ws);
		ProcessInstance proc = ProcessInstance.getInstance(ws, procDef);
		proc.addInput("jiraIn", jiraA);
		TestArtifacts.setStateToJiraInstance(jiraB, JiraStates.Open);
		TestArtifacts.setStateToJiraInstance(jiraC, JiraStates.Closed);
		ws.concludeTransaction();
		repAnalyzer.printImpact();
		repAnalyzer.getImpact().clear();
		TestArtifacts.setStateToJiraInstance(jiraA, JiraStates.Closed);
		TestArtifacts.addJiraToJira(jiraA, jiraB);
		ws.concludeTransaction();
		repAnalyzer.printImpact();
		repAnalyzer.getImpact().clear();
		TestArtifacts.setStateToJiraInstance(jiraA, JiraStates.Open);
		TestArtifacts.addJiraToJira(jiraA, jiraC);
		ws.concludeTransaction();
		repAnalyzer.printImpact();
		repAnalyzer.getImpact().clear();
		TestArtifacts.setStateToJiraInstance(jiraA, JiraStates.Closed);
		TestArtifacts.setStateToJiraInstance(jiraB, JiraStates.Closed);
		ws.concludeTransaction();
		repAnalyzer.printImpact();
		repAnalyzer.getImpact().clear();
		this.helperFunction();
	}

	public void helperFunction()
	{
		System.out.println("The Updated Repair Trees are as follows");
		InstanceType type = ws.debugInstanceTypeFindByName(ReservedNames.CONSISTENCY_RULE_TYPE_NAME);
		Set<InstanceType> all = type.subTypes();
		Iterator<InstanceType> it = all.iterator();
		while (it.hasNext()) {
			SetProperty<Rule> rule = ((ConsistencyRuleType) it.next()).ruleEvaluations();
			Iterator<Rule> it_rule = rule.iterator();
			while (it_rule.hasNext()) {
				ConsistencyRule cre = (ConsistencyRule) it_rule.next();
				System.out.println("Rule"+cre.ruleDefinition());
				if(!cre.isConsistent())
				{
				RepairNode rn = RuleService.repairTree(cre);
				if (rn != null) {
					ConsistencyUtils.printRepairTree(rn);
				//	scorer=new alphaBeticalSort();
					RepairTreeSorter rts = new RepairTreeSorter(this.rs,scorer);
				//	rts.printSortedRepairTree(rn, 1);
					
					rts.updateTreeOnScores(rn, cre.getProperty("name").getValue().toString());
				//	rts.printSortedRepairTree(rn, 1);
				}
				}
			}

		}
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
