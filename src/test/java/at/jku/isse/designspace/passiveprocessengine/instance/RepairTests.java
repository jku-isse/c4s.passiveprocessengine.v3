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
	void testDeepExistsRepair() {
		Instance jiraB =  TestArtifacts.getJiraInstance(ws, "jiraB");
		Instance jiraC = TestArtifacts.getJiraInstance(ws, "jiraC");
		Instance jiraD = TestArtifacts.getJiraInstance(ws, "jiraD");
		Instance jiraA = TestArtifacts.getJiraInstance(ws, "jiraA");
		TestArtifacts.addJiraToJira(jiraA, jiraB);
		TestArtifacts.addJiraToJira(jiraA, jiraC);
		TestArtifacts.addParentToJira(jiraB, jiraD);
		
		ConsistencyRuleType crt = ConsistencyRuleType.create(ws, typeJira, "DeepExistsTest",
		"self.requirements \r\n"
		+"->select(req | req.parent.isDefined() ) \r\n"
		+"->collect(req2 | req2.parent) \r\n"
		+"->exists(parent : <"+typeJira.getQualifiedName()+"> | parent.state='Closed')"
		);
		ws.concludeTransaction();
		assertTrue(ConsistencyUtils.crdValid(crt));
		String eval = (String) crt.ruleEvaluations().get().stream()
				.map(rule -> ((Rule)rule).result()+"" )
				.collect(Collectors.joining(",","[","]"));
		System.out.println("Checking "+crt.name() +" Result: "+ eval);
		crt.consistencyRuleEvaluations().value.stream()
			.filter(cr -> !cr.isConsistent())
			.forEach(cr -> { 
			RepairNode repairTree = RuleService.repairTree(cr);
			assert(repairTree != null);
		});
		
		//FIXME: I would expect the repairtree generation not to throw an exception
	}
	
	@Test
	void testMimicExistsWithSizeRepair() {
		Instance jiraB =  TestArtifacts.getJiraInstance(ws, "jiraB");
		Instance jiraC = TestArtifacts.getJiraInstance(ws, "jiraC");
		Instance jiraD = TestArtifacts.getJiraInstance(ws, "jiraD");
		Instance jiraA = TestArtifacts.getJiraInstance(ws, "jiraA");
		TestArtifacts.addJiraToJira(jiraA, jiraB);
		TestArtifacts.addJiraToJira(jiraA, jiraC);
		TestArtifacts.addParentToJira(jiraB, jiraD); // only B has a parent, which is D, and D is NOT closed
	
		
		ConsistencyRuleType crt = ConsistencyRuleType.create(ws, typeJira, "MimicExistsTest",
		"self.requirements \r\n"
		+"->select(req | req.parent.isDefined() ) \r\n"
		+ "->collect(req2 | req2.parent) \r\n"
		+ "->select(parent : <"+typeJira.getQualifiedName()+"> | parent.state.equalsIgnoreCase('Closed'))->size() > 0"
		);
		ws.concludeTransaction();
		assertTrue(ConsistencyUtils.crdValid(crt));
		String eval = (String) crt.ruleEvaluations().get().stream()
				.map(rule -> ((Rule)rule).contextInstance().toString()+":"+((Rule)rule).result()+"\r\n" )
				.collect(Collectors.joining(",","[","]"));
		System.out.println("Checking "+crt.name() +" Result: "+ eval);
		crt.consistencyRuleEvaluations().value.stream()
			.filter(cr -> !cr.isConsistent() || cr.contextInstance().equals(jiraA))
			.forEach(cr -> { 
			RepairNode repairTree = RuleService.repairTree(cr);
			assert(repairTree != null);
		});
		
		//FIXME: I would expect repair to suggest adding to jiraA.parent.requirements as well as setting to state of any jiraA.requirements.parent to ? (and not just removing the instance)
	}
	
	@Test
	void testUnionRepair() {
		Instance jiraB =  TestArtifacts.getJiraInstance(ws, "jiraB");
		Instance jiraC = TestArtifacts.getJiraInstance(ws, "jiraC");
		Instance jiraD = TestArtifacts.getJiraInstance(ws, "jiraD");
		Instance jiraA = TestArtifacts.getJiraInstance(ws, "jiraA");
		TestArtifacts.addJiraToJira(jiraA, jiraB); // this has B in reqs and C as parent
		TestArtifacts.addParentToJira(jiraA, jiraC);
		TestArtifacts.addJiraToJira(jiraC, jiraD); // parent has D in req
		// calc union as A.reqs = B, and A.parent.reqs = D
		
		ConsistencyRuleType crt = ConsistencyRuleType.create(ws, typeJira, "UnionTest",
		"self.requirements->union(self.parent.requirements) \r\n"
		+"->select(req : <"+typeJira.getQualifiedName()+"> | req.state='Closed')->size() > 0"
		);
		ws.concludeTransaction();
		assertTrue(ConsistencyUtils.crdValid(crt));
		String eval = (String) crt.ruleEvaluations().get().stream()
				.map(rule -> ((Rule)rule).result()+"" )
				.collect(Collectors.joining(",","[","]"));
		System.out.println("Checking "+crt.name() +" Result: "+ eval);
		crt.consistencyRuleEvaluations().value.stream()
			.filter(cr -> !cr.isConsistent())
			.forEach(cr -> { 
			RepairNode repairTree = RuleService.repairTree(cr);
			assert(repairTree != null);
		});
		
		//FIXME: I would expect repair to suggest adding to jiraA.parent.requirements as well as adding to jiraA.requirements (and not just removing the instance)
	}
	
	@Test
	void testMimicUnionWithOrRepair() {
		Instance jiraB =  TestArtifacts.getJiraInstance(ws, "jiraB");
		Instance jiraC = TestArtifacts.getJiraInstance(ws, "jiraC");
		Instance jiraD = TestArtifacts.getJiraInstance(ws, "jiraD");
		Instance jiraA = TestArtifacts.getJiraInstance(ws, "jiraA");
		TestArtifacts.addJiraToJira(jiraA, jiraB); // this has B in reqs and C as parent
		TestArtifacts.addParentToJira(jiraA, jiraC);
		TestArtifacts.addJiraToJira(jiraC, jiraD); // parent has D in req
		// calc union as A.reqs = B, and A.parent.reqs = D
		
		ConsistencyRuleType crt = ConsistencyRuleType.create(ws, typeJira, "UnionTest",
		"self.requirements->select(req : <"+typeJira.getQualifiedName()+"> | req.state='Closed')->size() > 0 \r\n"
		+ "or if self.parent.isDefined() then self.parent.requirements->select(req2 : <"+typeJira.getQualifiedName()+"> | req2.state='Closed')->size() > 0 else true endif"
		);
		ws.concludeTransaction();
		assertTrue(ConsistencyUtils.crdValid(crt));
		String eval = (String) crt.ruleEvaluations().get().stream()
				.map(rule -> ((Rule)rule).result()+"" )
				.collect(Collectors.joining(",","[","]"));
		System.out.println("Checking "+crt.name() +" Result: "+ eval);
		crt.consistencyRuleEvaluations().value.stream()
			.filter(cr -> !cr.isConsistent())
			.forEach(cr -> { 
			RepairNode repairTree = RuleService.repairTree(cr);
			assert(repairTree != null);
		});
		
		//FIXME: I would expect repair to suggest adding also the jiraA.parent.requirements (instead of having adding to jiraA.requirements twice)
	}
	
	@Test
	void testDerivedPropertyFromDerivedProperty() {
		
		InstanceType instanceType = WorkspaceService.createInstanceType(ws, "TaskType", ws.TYPES_FOLDER);
        WorkspaceService.createPropertyType(ws, instanceType, "prev", Cardinality.SINGLE, instanceType);
        WorkspaceService.createPropertyType(ws, instanceType, "inData", Cardinality.SINGLE, typeJira);
        ws.concludeTransaction();
        WorkspaceService.createPropertyType(ws, instanceType, "out", Cardinality.SINGLE, typeJira);
        DerivedPropertyRuleType dPropIn2Out = DerivedPropertyRuleType.create(ws, instanceType, "out", Cardinality.SINGLE, "if self.inData.isDefined() and self.inData.parent.isDefined() then self.inData.parent else self.out endif");
        //DerivedPropertyRuleType dPropOut2In = DerivedPropertyRuleType.create(ws, instanceType, "inData", Cardinality.SINGLE, "if self.prev.isDefined() and self.prev.out.isDefined() then self.prev.out else self.inData endif");
        ws.concludeTransaction();
                
        Instance jiraB =  TestArtifacts.getJiraInstance(ws, "jiraB");
		Instance jiraC = TestArtifacts.getJiraInstance(ws, "jiraC");
		TestArtifacts.addParentToJira(jiraB, jiraC);
        Instance step1 = WorkspaceService.createInstance(ws, "step1", instanceType);
        Instance step2 = WorkspaceService.createInstance(ws, "step2", instanceType);
        step2.getProperty("prev").set(step1);

        ws.concludeTransaction();
        assertEquals(null, step1.getPropertyAsValue("out"));
        step1.getProperty("inData").set(jiraB);
       
        ws.concludeTransaction();
        assertEquals(jiraC, step1.getPropertyAsValue("out"));
      //  assertEquals(jiraC, step2.getPropertyAsValue("inData"));
        
		
	}
	
}
