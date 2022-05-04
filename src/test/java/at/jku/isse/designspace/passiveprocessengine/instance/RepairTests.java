package at.jku.isse.designspace.passiveprocessengine.instance;

import static org.junit.Assert.assertEquals;
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
import at.jku.isse.designspace.rule.arl.repair.RepairNode;
import at.jku.isse.designspace.rule.checker.ArlRuleEvaluator;
import at.jku.isse.designspace.rule.checker.ConsistencyUtils;
import at.jku.isse.designspace.rule.model.ConsistencyRuleType;
import at.jku.isse.designspace.rule.model.DerivedPropertyRuleType;
import at.jku.isse.designspace.rule.model.Rule;
import at.jku.isse.designspace.rule.service.RuleService;
import at.jku.isse.passiveprocessengine.WrapperCache;
import at.jku.isse.passiveprocessengine.definition.ProcessDefinition;
import at.jku.isse.passiveprocessengine.definition.DecisionNodeDefinition.InFlowType;
import at.jku.isse.passiveprocessengine.definition.serialization.DTOs;
import at.jku.isse.passiveprocessengine.definition.serialization.DefinitionTransformer;
import at.jku.isse.passiveprocessengine.definition.serialization.JsonDefinitionSerializer;
import at.jku.isse.passiveprocessengine.demo.TestArtifacts;
import at.jku.isse.passiveprocessengine.demo.TestProcesses;
import at.jku.isse.passiveprocessengine.demo.TestArtifacts.JiraStates;
import at.jku.isse.passiveprocessengine.instance.ConstraintWrapper;
import at.jku.isse.passiveprocessengine.instance.ProcessInstance;
import at.jku.isse.passiveprocessengine.instance.ProcessInstanceChangeProcessor;
import at.jku.isse.passiveprocessengine.instance.ProcessStep;
import at.jku.isse.passiveprocessengine.instance.ProcessStep.CoreProperties;
import at.jku.isse.passiveprocessengine.instance.StepLifecycle.Conditions;
import at.jku.isse.passiveprocessengine.instance.StepLifecycle.State;

@ExtendWith(SpringExtension.class)
@SpringBootTest
public
class RepairTests {

	static Workspace ws;
	static InstanceType typeJira;
	
	@BeforeEach
	void setup() throws Exception {
		RuleService.setEvaluator(new ArlRuleEvaluator());
		ws = WorkspaceService.createWorkspace("test", WorkspaceService.PUBLIC_WORKSPACE, WorkspaceService.ANY_USER, null, true, false);
		//ws = WorkspaceService.PUBLIC_WORKSPACE;
		RuleService.currentWorkspace = ws;

		typeJira = TestArtifacts.getJiraInstanceType(ws);
	}

	
	@Test
	void testRepairTraversal() {
		Instance jiraB =  TestArtifacts.getJiraInstance(ws, "jiraB");
		Instance jiraC = TestArtifacts.getJiraInstance(ws, "jiraC");
		//Instance jiraD = TestArtifacts.getJiraInstance(ws, "jiraD");
		Instance jiraA = TestArtifacts.getJiraInstance(ws, "jiraA");
		TestArtifacts.addParentToJira(jiraA, jiraB); //link from A to B (as parent
		TestArtifacts.addParentToJira(jiraB, jiraC);
		TestArtifacts.addParentToJira(jiraC, jiraA); // we generate a cycle for sake of otherwise running into null pointer exceptions
		
		ConsistencyRuleType crt = ConsistencyRuleType.create(ws, typeJira, "TranverseTest", "self.parent.name = 'jiraX'");
		ws.concludeTransaction();
		crt.consistencyRuleEvaluations().value.forEach(cr -> { RepairNode repairTree = RuleService.repairTree(cr); });
		;
	}
	
	@Test
	void testReevaluationOnReferenceChange() {
		Instance jiraB =  TestArtifacts.getJiraInstance(ws, "jiraB");
		Instance jiraC = TestArtifacts.getJiraInstance(ws, "jiraC");
		Instance jiraD = TestArtifacts.getJiraInstance(ws, "jiraD");
		Instance jiraA = TestArtifacts.getJiraInstance(ws, "jiraA");
		TestArtifacts.addParentToJira(jiraA, jiraB); //link from A to B (as parent
		TestArtifacts.addParentToJira(jiraB, jiraC);
		TestArtifacts.addParentToJira(jiraC, jiraD); 
		
		ConsistencyRuleType crt = ConsistencyRuleType.create(ws, typeJira, "TranverseTest", "self.parent.parent.name.equalsIgnoreCase('jiraD')");
		ws.concludeTransaction();
		crt.consistencyRuleEvaluations().value.forEach(cr -> { RepairNode repairTree = RuleService.repairTree(cr); });
		;
		System.out.println("UPDATING PARENT");
		TestArtifacts.addParentToJira(jiraB, jiraD);
		ws.concludeTransaction();
		crt.consistencyRuleEvaluations().value.forEach(cr -> { RepairNode repairTree = RuleService.repairTree(cr); });
	}
	
	@Test
	void testDerivedPropertyFromDerivedProperty() {
		
		InstanceType instanceType = WorkspaceService.createInstanceType(ws, "TaskType", ws.TYPES_FOLDER);
        WorkspaceService.createPropertyType(ws, instanceType, "in", Cardinality.SINGLE, typeJira);
        WorkspaceService.createPropertyType(ws, instanceType, "out", Cardinality.SINGLE, typeJira);
        WorkspaceService.createPropertyType(ws, instanceType, "prev", Cardinality.SINGLE, instanceType);
        
        DerivedPropertyRuleType dPropIn2Out = DerivedPropertyRuleType.create(ws, instanceType, "out", Cardinality.SINGLE, "if self.in.isDefined() and self.in.parent.isDefined() then self.in.parent else null endif");
        DerivedPropertyRuleType dPropOut2In = DerivedPropertyRuleType.create(ws, instanceType, "in", Cardinality.SINGLE, "if self.prev.isDefined() and self.prev.out.isDefined() then self.prev.out else null endif");

                
        Instance jiraB =  TestArtifacts.getJiraInstance(ws, "jiraB");
		Instance jiraC = TestArtifacts.getJiraInstance(ws, "jiraC");
		TestArtifacts.addParentToJira(jiraB, jiraC);
        Instance step1 = WorkspaceService.createInstance(ws, "step1", instanceType);
        Instance step2 = WorkspaceService.createInstance(ws, "step1", instanceType);
        step2.getProperty("prev").set(step1);

        ws.concludeTransaction();
        assertEquals(null, step1.getPropertyAsValue("out"));
        step1.getProperty("in").set(jiraB);
       
        ws.concludeTransaction();
        assertEquals(jiraC, step1.getPropertyAsValue("out"));
        assertEquals(jiraC, step2.getPropertyAsValue("in"));
        
		
	}
	
}
