package at.jku.isse.designspace.passiveprocessengine.instance;

import static org.junit.Assert.assertEquals;
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
import at.jku.isse.designspace.core.model.PropertyType;
import at.jku.isse.designspace.core.model.Workspace;
import at.jku.isse.designspace.core.service.WorkspaceService;
import at.jku.isse.designspace.rule.arl.repair.AbstractRepairAction;
import at.jku.isse.designspace.rule.arl.repair.Operator;
import at.jku.isse.designspace.rule.arl.repair.RepairNode;
import at.jku.isse.designspace.rule.arl.repair.RestrictionNode;
import at.jku.isse.designspace.rule.arl.repair.RestrictionNode.SubtreeCombinatorNode;
import at.jku.isse.designspace.rule.checker.ArlRuleEvaluator;
import at.jku.isse.designspace.rule.checker.ConsistencyUtils;
import at.jku.isse.designspace.rule.model.ConsistencyRuleType;
import at.jku.isse.designspace.rule.model.DerivedPropertyRuleType;
import at.jku.isse.designspace.rule.model.Rule;
import at.jku.isse.designspace.rule.service.RuleService;
import at.jku.isse.passiveprocessengine.definition.StepDefinition;
import at.jku.isse.passiveprocessengine.demo.TestArtifacts;
import at.jku.isse.passiveprocessengine.instance.ProcessStep;

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
	void testRepairUnequalTraversal() {
		Instance jiraB =  TestArtifacts.getJiraInstance(ws, "jiraB");
		//Instance jiraC = TestArtifacts.getJiraInstance(ws, "jiraC");
		//Instance jiraD = TestArtifacts.getJiraInstance(ws, "jiraD");
		Instance jiraA = TestArtifacts.getJiraInstance(ws, "jiraA");
		TestArtifacts.addParentToJira(jiraA, jiraB); //link from A to B (as parent
		//TestArtifacts.addParentToJira(jiraB, jiraC);
		//TestArtifacts.addParentToJira(jiraC, jiraA); // we generate a cycle for sake of otherwise running into null pointer exceptions
		
		ConsistencyRuleType crt = ConsistencyRuleType.create(ws, typeJira, "TranverseTest", "self.parent.name = self.name");
		ws.concludeTransaction();
		crt.consistencyRuleEvaluations().getValue().forEach(cr -> { RepairNode repairTree = RuleService.repairTree(cr); });
		RepairNode rnodeA = crt.consistencyRuleEvaluations().getValue().stream()
			.filter(cr -> cr.contextInstance().equals(jiraA))
			.map(cr -> RuleService.repairTree(cr))
			.findAny().get();		
		printRepairActions(rnodeA);		
		
		RestrictionNode comp = new RestrictionNode.PropertyNode("parent", null)
				.setNextNodeFluent(new RestrictionNode.PropertyNode("name", null)
						.setNextNodeFluent(new RestrictionNode.OnlyComparatorNode(Operator.MOD_EQ)
								.setNextNodeFluent(new RestrictionNode.ValueNode("jiraA"))));
		assert(matchesRestriction(rnodeA, jiraA, comp));
	}
	
	@Test
	void testRepairRefenceTraversal() {
		Instance jiraB =  TestArtifacts.getJiraInstance(ws, "jiraB");
		Instance jiraC = TestArtifacts.getJiraInstance(ws, "jiraC");
		Instance jiraA = TestArtifacts.getJiraInstance(ws, "jiraA");
		TestArtifacts.addParentToJira(jiraA, jiraB); //link from A to B (as parent
		TestArtifacts.addJiraToJira(jiraA, jiraC);
		
		ConsistencyRuleType crt = ConsistencyRuleType.create(ws, typeJira, "TranverseAndRefTest", 
				"self.requirements"
				+ "->exists(req : <"+typeJira.getQualifiedName()+">  | self.parent.name = req.name)");
		ws.concludeTransaction();
		crt.consistencyRuleEvaluations().getValue().forEach(cr -> { RepairNode repairTree = RuleService.repairTree(cr); });
		RepairNode rnodeA = crt.consistencyRuleEvaluations().getValue().stream()
			.filter(cr -> cr.contextInstance().equals(jiraA))
			.map(cr -> RuleService.repairTree(cr))
			.findAny().get();		
		printRepairActions(rnodeA);
		//TODO: somehow express that the parent name needs to match, in either direction 		
	}
	
	@Test
	void testRepairSelectOrTraversal() {
		Instance jiraB =  TestArtifacts.getJiraInstance(ws, "jiraB");
		Instance jiraC = TestArtifacts.getJiraInstance(ws, "jiraC");
		//Instance jiraD = TestArtifacts.getJiraInstance(ws, "jiraD");
		Instance jiraA = TestArtifacts.getJiraInstance(ws, "jiraA");
		TestArtifacts.addJiraToJira(jiraA, jiraB);
		TestArtifacts.addJiraToJira(jiraA, jiraC);
		
		ConsistencyRuleType crt = ConsistencyRuleType.create(ws, typeJira, "ForAllEqualTest",
				"self.requirements"
				+ "->select(req : <"+typeJira.getQualifiedName()+"> | req.name = self.name or req.name = 'JiraX')"
						+ "->size() > 1");
		ws.concludeTransaction();
		
		crt.consistencyRuleEvaluations().getValue().forEach(cr -> { 
			RepairNode repairTree = RuleService.repairTree(cr);
			printRepairActions(repairTree);
		});		
		//TODO: propagate req.name into repair description (otherwise only the term 'name' is confusing)
	}
	
	@Test
	void testRepairVarComparison() {
		Instance jiraB =  TestArtifacts.getJiraInstance(ws, "jiraB");
		Instance jiraC = TestArtifacts.getJiraInstance(ws, "jiraC");
		//Instance jiraD = TestArtifacts.getJiraInstance(ws, "jiraD");
		Instance jiraA = TestArtifacts.getJiraInstance(ws, "jiraA");
		TestArtifacts.addJiraToJira(jiraA, jiraB);
		TestArtifacts.addJiraToJira(jiraA, jiraC);
		
		ConsistencyRuleType crt = ConsistencyRuleType.create(ws, typeJira, "ForAllEqualTest", 
				"self.requirements"
				+ "->select(req : <"+typeJira.getQualifiedName()+"> | req.state <> self.state)"
						+ "->size() > 0");
		ws.concludeTransaction();
		
		RepairNode rnodeA = crt.consistencyRuleEvaluations().getValue().stream()
				.filter(cr -> cr.contextInstance().equals(jiraA))
				.map(cr -> RuleService.repairTree(cr))
				.findAny().get();		
			printRepairActions(rnodeA);		
		RestrictionNode comp = new RestrictionNode.PropertyNode("requirements", null)
				.setNextNodeFluent(new RestrictionNode.NotNode(new RestrictionNode.BipartComparatorNode(Operator.MOD_EQ, new RestrictionNode.PropertyNode("state", null), new RestrictionNode.ValueNode("Open"))))	;
		assert(matchesRestriction(rnodeA, jiraA, comp));
		
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
		crt.consistencyRuleEvaluations().getValue().forEach(cr -> { RepairNode repairTree = RuleService.repairTree(cr); });
		RepairNode rnodeA = crt.consistencyRuleEvaluations().getValue().stream()
			.filter(cr -> cr.contextInstance().equals(jiraA))
			.map(cr -> RuleService.repairTree(cr))
			.findAny().get();		
		printRepairActions(rnodeA);		
		
		RestrictionNode comp = new RestrictionNode.PropertyNode("parent", null)
				.setNextNodeFluent(new RestrictionNode.PropertyNode("name", null)
						.setNextNodeFluent(new RestrictionNode.OnlyComparatorNode(Operator.MOD_EQ)
								.setNextNodeFluent(new RestrictionNode.ValueNode("'jiraX'"))));
		assert(matchesRestriction(rnodeA, jiraA, comp));
		
		// this works, the next test with one now as well
	}
	
	@Test
	void testLongTraversal() {
		Instance jiraB =  TestArtifacts.getJiraInstance(ws, "jiraB");
		Instance jiraC = TestArtifacts.getJiraInstance(ws, "jiraC");
		Instance jiraD = TestArtifacts.getJiraInstance(ws, "jiraD");
		Instance jiraA = TestArtifacts.getJiraInstance(ws, "jiraA");
		TestArtifacts.addParentToJira(jiraA, jiraB); //link from A to B (as parent
		TestArtifacts.addParentToJira(jiraB, jiraC);
		TestArtifacts.addParentToJira(jiraC, jiraD); 
		
		ConsistencyRuleType crt = ConsistencyRuleType.create(ws, typeJira, "TraverseTest", "self.parent.parent.name = 'jiraD'");
		ws.concludeTransaction();
		RepairNode rnodeA = crt.consistencyRuleEvaluations().getValue().stream()
				.filter(cr -> cr.contextInstance().equals(jiraA))
				.map(cr -> RuleService.repairTree(cr))
				.findAny().get();	
		printRepairActions(rnodeA);	
		
		RestrictionNode comp1 = new RestrictionNode.PropertyNode("parent", null)				
						.setNextNodeFluent(new RestrictionNode.PropertyNode("parent", null)
								.setNextNodeFluent(new RestrictionNode.PropertyNode("name", null)						
										.setNextNodeFluent(new RestrictionNode.OnlyComparatorNode(Operator.MOD_EQ)
												.setNextNodeFluent(new RestrictionNode.ValueNode("'jiraD'")))));
		assert(matchesRestriction(rnodeA, jiraA, comp1));
		
		RestrictionNode comp = new RestrictionNode.PropertyNode("parent", null)
				.setNextNodeFluent(new RestrictionNode.PropertyNode("name", null)
						.setNextNodeFluent(new RestrictionNode.OnlyComparatorNode(Operator.MOD_EQ)
								.setNextNodeFluent(new RestrictionNode.ValueNode("'jiraD'"))));
		assert(matchesRestriction(rnodeA, jiraB, comp));
		
		/* Expected repairs:
		 * 
		 * OK jiraC change name = jiraD
		 * OK jiraB change parent to name with jiraD
		 * OK jiraA change parent to Issue with parent with name = jiraD 
		 * 
		 * */
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
		crt.consistencyRuleEvaluations().getValue().forEach(cr -> { RepairNode repairTree = RuleService.repairTree(cr);
		});
		RepairNode rnodeA = crt.consistencyRuleEvaluations().getValue().stream()
				.filter(cr -> cr.contextInstance().equals(jiraA))
				.map(cr -> RuleService.repairTree(cr))
				.findAny().get();		
		printRepairActions(rnodeA);	
			
		RestrictionNode comp = new RestrictionNode.PropertyNode("parent", null)
				.setNextNodeFluent(new RestrictionNode.PropertyNode("parent", null)
				.setNextNodeFluent(new RestrictionNode.PropertyNode("name", null)
						.setNextNodeFluent(new RestrictionNode.OperationNode("equalsignorecase")						
								.setNextNodeFluent(new RestrictionNode.ValueNode("'jiraD'")))));
		assert(matchesRestriction(rnodeA, jiraA, comp));
		
		RestrictionNode comp1 = new RestrictionNode.PropertyNode("parent", null)				
				.setNextNodeFluent(new RestrictionNode.PropertyNode("name", null)
						.setNextNodeFluent(new RestrictionNode.OperationNode("equalsignorecase")						
								.setNextNodeFluent(new RestrictionNode.ValueNode("'jiraD'"))));
		assert(matchesRestriction(rnodeA, jiraB, comp1));
		
		/* Expected repairs:
		 * 
		 * OK jiraC change name = jiraD
		 * OK change jiraB.parent to name with jiraD
		 * OK jiraA change parent to Issue with parent with name = jiraD 
		 * 
		 * */
	}
	
	@Test
	void testTraversalStartsWithChange() {
		Instance jiraB =  TestArtifacts.getJiraInstance(ws, "jiraB");
		Instance jiraC = TestArtifacts.getJiraInstance(ws, "jiraC");
		Instance jiraD = TestArtifacts.getJiraInstance(ws, "jiraD");
		Instance jiraA = TestArtifacts.getJiraInstance(ws, "jiraA");
		TestArtifacts.addParentToJira(jiraA, jiraB); //link from A to B (as parent
		TestArtifacts.addParentToJira(jiraB, jiraC);
		TestArtifacts.addParentToJira(jiraC, jiraD); 
		
		ConsistencyRuleType crt = ConsistencyRuleType.create(ws, typeJira, "TranverseTest", "self.parent.parent.name.startsWith('jiraD')");
		ws.concludeTransaction();
		crt.consistencyRuleEvaluations().getValue().forEach(cr -> { RepairNode repairTree = RuleService.repairTree(cr);
		});
		RepairNode rnodeA = crt.consistencyRuleEvaluations().getValue().stream()
				.filter(cr -> cr.contextInstance().equals(jiraA))
				.map(cr -> RuleService.repairTree(cr))
				.findAny().get();		
		printRepairActions(rnodeA);	
			
		RestrictionNode comp = new RestrictionNode.PropertyNode("parent", null)
				.setNextNodeFluent(new RestrictionNode.PropertyNode("parent", null)
				.setNextNodeFluent(new RestrictionNode.PropertyNode("name", null)
						.setNextNodeFluent(new RestrictionNode.OperationNode("startswith")						
								.setNextNodeFluent(new RestrictionNode.ValueNode("'jiraD'")))));
		assert(matchesRestriction(rnodeA, jiraA, comp));
		
		RestrictionNode comp1 = new RestrictionNode.PropertyNode("parent", null)				
				.setNextNodeFluent(new RestrictionNode.PropertyNode("name", null)
						.setNextNodeFluent(new RestrictionNode.OperationNode("startswith")						
								.setNextNodeFluent(new RestrictionNode.ValueNode("'jiraD'"))));
		assert(matchesRestriction(rnodeA, jiraB, comp1));
		
		/* Expected repairs:
		 * 
		 * OK jiraC change name = jiraD
		 * OK change jiraB.parent to name with jiraD
		 * OK jiraA change parent to Issue with parent with name = jiraD 
		 * 
		 * */
	}
	
	@Test
	void testAnyRepair() {
		Instance jiraB =  TestArtifacts.getJiraInstance(ws, "jiraB");
		Instance jiraC = TestArtifacts.getJiraInstance(ws, "jiraC");
		Instance jiraD = TestArtifacts.getJiraInstance(ws, "jiraD");
		Instance jiraA = TestArtifacts.getJiraInstance(ws, "jiraA");
		TestArtifacts.addJiraToJira(jiraA, jiraB);
		//TestArtifacts.addJiraToJira(jiraA, jiraC);
		TestArtifacts.addParentToJira(jiraB, jiraD); // only B has a parent, which is D, and D is NOT closed
	
		ConsistencyRuleType crt = ConsistencyRuleType.create(ws, typeJira, "AnyTest",
		"self.requirements->any() \r\n"
		+ "->asType(<"+typeJira.getQualifiedName()+">)"
				+ " .parent.state='Closed'"
		);
		ws.concludeTransaction();
		assertTrue(ConsistencyUtils.crdValid(crt));
		String eval = (String) crt.ruleEvaluations().get().stream()
				.map(rule -> ((Rule)rule).contextInstance().toString()+":"+((Rule)rule).result()+"\r\n" )
				.collect(Collectors.joining(",","[","]"));
		System.out.println("Checking "+crt.name() +" Result: "+ eval);
		crt.consistencyRuleEvaluations().getValue().stream()
			.filter(cr -> cr.contextInstance().equals(jiraA))
			.forEach(cr -> { 
			RepairNode repairTree = RuleService.repairTree(cr);
			printRepairActions(repairTree);
			
			RestrictionNode comp2 = new RestrictionNode.PropertyNode("parent", null)
					.setNextNodeFluent(new RestrictionNode.PropertyNode("state", null)
							.setNextNodeFluent(new RestrictionNode.OnlyComparatorNode(Operator.MOD_EQ)
									.setNextNodeFluent(new RestrictionNode.ValueNode("'Closed'"))));
			assert(matchesRestriction(repairTree, jiraB, comp2));
			RestrictionNode comp =  new RestrictionNode.PropertyNode("requirements", null)
					.setNextNodeFluent(new RestrictionNode.OperationNode("any")
					.setNextNodeFluent(new RestrictionNode.PropertyNode("parent", null)
							.setNextNodeFluent(new RestrictionNode.PropertyNode("state", null)
							.setNextNodeFluent(new RestrictionNode.OnlyComparatorNode(Operator.MOD_EQ)
									.setNextNodeFluent(new RestrictionNode.ValueNode("'Closed'"))))));
			assert(matchesRestriction(repairTree, jiraA, comp));
		});
		/*
		 * expected repairs:
		 * OK  setting of jiraD.state to closed
		 * OK set parent of B to issue with state = closed
		 * TODO DISCUSS: adding something to requirements does not make sense as any
		 */
	}
	
    @Test
    void testAnyRepair2() {
        Instance jiraB =  TestArtifacts.getJiraInstance(ws, "jiraB");
        Instance jiraC = TestArtifacts.getJiraInstance(ws, "jiraC");
        Instance jiraA = TestArtifacts.getJiraInstance(ws, "jiraA");
        TestArtifacts.addJiraToJira(jiraA, jiraC);
        TestArtifacts.addJiraToJira(jiraA, jiraB);
       

        ConsistencyRuleType crt = ConsistencyRuleType.create(ws, typeJira, "AnyTest",
                "self.requirements->any()->asType(<"+typeJira.getQualifiedName()+">).requirements.size() = 2"
        );
        ws.concludeTransaction();
        assertTrue(ConsistencyUtils.crdValid(crt));
        String eval = (String) crt.ruleEvaluations().get().stream()
                .map(rule -> ((Rule)rule).contextInstance().toString()+":"+((Rule)rule).result()+"\r\n" )
                .collect(Collectors.joining(",","[","]"));
        System.out.println("Checking "+crt.name() +" Result: "+ eval);

        RepairNode repairTree = ConsistencyUtils.getRepairTree(crt,jiraA);
        printRepairActions(repairTree);
        assert(repairTree != null);

        // works but size handling not accurate yet as we need to detail that any req added to jiraA needs to have two requirements linked, 
        // although adding to jiraA in combination with any() is not that sensical, see better version in next test below 
    }
	
    @Test
    void testExistsSizeRepair() {
        Instance jiraB =  TestArtifacts.getJiraInstance(ws, "jiraB");
        Instance jiraC = TestArtifacts.getJiraInstance(ws, "jiraC");
        Instance jiraA = TestArtifacts.getJiraInstance(ws, "jiraA");
        TestArtifacts.addJiraToJira(jiraA, jiraC);
        TestArtifacts.addJiraToJira(jiraA, jiraB);
       

        ConsistencyRuleType crt = ConsistencyRuleType.create(ws, typeJira, "AnyTest",
                "self.requirements->exists(req | req.requirements.size() = 2)"
        );
        ws.concludeTransaction();
        assertTrue(ConsistencyUtils.crdValid(crt));
        String eval = (String) crt.ruleEvaluations().get().stream()
                .map(rule -> ((Rule)rule).contextInstance().toString()+":"+((Rule)rule).result()+"\r\n" )
                .collect(Collectors.joining(",","[","]"));
        System.out.println("Checking "+crt.name() +" Result: "+ eval);

        RepairNode repairTree = ConsistencyUtils.getRepairTree(crt,jiraA);
        printRepairActions(repairTree);
        assert(repairTree != null);

        //works
    }
    
//    @Test
//	void testCollectWithAnyRepair() {
//		Instance jiraB =  TestArtifacts.getJiraInstance(ws, "jiraB");
//		Instance jiraC = TestArtifacts.getJiraInstance(ws, "jiraC");
//		Instance jiraD = TestArtifacts.getJiraInstance(ws, "jiraD");
//		Instance jiraA = TestArtifacts.getJiraInstance(ws, "jiraA");
//		Instance jiraE = TestArtifacts.getJiraInstance(ws, "jiraE");
//		TestArtifacts.addJiraToJira(jiraA, jiraB);
//		TestArtifacts.addJiraToJira(jiraA, jiraD);
//		TestArtifacts.addJiraToJira(jiraB, jiraC);
//	
//		//TestArtifacts.addJiraToJira(jiraD, jiraC);
//		//TestArtifacts.addJiraToJira(jiraC, jiraE);
//		TestArtifacts.setStateToJiraInstance(jiraB, JiraStates.Closed);
//		//TestArtifacts.setStateToJiraInstance(jiraC, JiraStates.Closed);
//	
////		ConsistencyRuleType crt = ConsistencyRuleType.create(ws, typeJira, "CollectWithAnyTest",
////		"self.requirements"
////				+ "->select(req3 | req3.state='Closed') \r\n" //from which that are closed		
////				+ "->collect(req : <"+typeJira.getQualifiedName()+"> | req.requirements \r\n" // take from linked requirements				
////		+ "		->any()"
////		+ ") \r\n"		
////		+ "->asSet() \r\n"				
////		+ "->select(ra : <"+typeJira.getQualifiedName()+"> | ra.isDefined())" // only if not null
////		+ "->size() > 1"
////		);
//		
//		// Essentially, select from every linked,closed requirements one random linked requirement and ensure there is at least one
//		ConsistencyRuleType crt = ConsistencyRuleType.create(ws, typeJira, "AnyTraverseTest",
//		"self.requirements->collect(req | req.requirements \r\n" // take all linked requirements
//		+ "   ->select(req3 | req3.state='Closed') \r\n" //then those which that are closed		JiraC (for B) and non for JiraD
//		+ "   ->collect( req2 : <"+typeJira.getQualifiedName()+"> | req2.requirements) \r\n" // again all their requirements, but choose only one, null
//		+ "   ->any()) \r\n"		
//		+ "->asSet() \r\n"				
//		+ "->select(ra : <"+typeJira.getQualifiedName()+"> | ra.isDefined())" // only if not null
//		+ "->size() > 0"
//		);
//		
//		ws.concludeTransaction();
//		assertTrue(ConsistencyUtils.crdValid(crt));
//
//		 RepairNode repairTree = ConsistencyUtils.getRepairTree(crt,jiraA);
//		 printRepairActions(repairTree);
//	        assert(repairTree != null);
//	        
//	        //fIXME: too complicated what this actually should do
//	}
    
	@Test
	void testAnyTraversRepair() {
		Instance jiraB =  TestArtifacts.getJiraInstance(ws, "jiraB");
		Instance jiraC = TestArtifacts.getJiraInstance(ws, "jiraC");
		Instance jiraD = TestArtifacts.getJiraInstance(ws, "jiraD");
		Instance jiraA = TestArtifacts.getJiraInstance(ws, "jiraA");
		TestArtifacts.addJiraToJira(jiraA, jiraB);
		TestArtifacts.addJiraToJira(jiraB, jiraC);
		
	// Take any of the linked requirement, and check if at least one of its requirements is closed.
		ConsistencyRuleType crt = ConsistencyRuleType.create(ws, typeJira, "AnyTraverseTest",
		"self.requirements->any() \r\n"
		+ "->asType(<"+typeJira.getQualifiedName()+">)"
			+ ".requirements"
				+ "->select(req | req.state='Closed')"
				//+ "->collect(req2 : <"+typeJira.getQualifiedName()+"> | req2.parent)"
				//+ "->asSet()"
				+ "->size() > 0"
		);
		ws.concludeTransaction();
		assertTrue(ConsistencyUtils.crdValid(crt));
		String eval = (String) crt.ruleEvaluations().get().stream()
				.map(rule -> ((Rule)rule).contextInstance().toString()+":"+((Rule)rule).result()+"\r\n" )
				.collect(Collectors.joining(",","[","]"));
		System.out.println("Checking "+crt.name() +" Result: "+ eval);
		crt.consistencyRuleEvaluations().getValue().stream()
			.filter(cr -> !cr.isConsistent() )
			.filter(cr -> cr.contextInstance().equals(jiraA))
			.forEach(cr -> { 
			RepairNode repairTree = RuleService.repairTree(cr);
			RestrictionNode comp = new RestrictionNode.PropertyNode("requirements", null)
							.setNextNodeFluent(new RestrictionNode.BipartComparatorNode(Operator.MOD_EQ, 
									new RestrictionNode.PropertyNode("state", null), 
									new RestrictionNode.ValueNode("'Closed'"))
									);
			printRepairActions(repairTree);
			assert(matchesRestriction(repairTree, jiraB, comp));
			assert(repairTree != null);
		});		
		// 
		// WORKS, TODO decide upon: we dont propose to add to A as its not guarnateed that any() would select it
	}
	
	@Test
	void testExistsIsEmptyRepair() {
		Instance jiraB =  TestArtifacts.getJiraInstance(ws, "jiraB");
		Instance jiraC = TestArtifacts.getJiraInstance(ws, "jiraC");
		Instance jiraD = TestArtifacts.getJiraInstance(ws, "jiraD");
		Instance jiraA = TestArtifacts.getJiraInstance(ws, "jiraA");
		TestArtifacts.addJiraToJira(jiraA, jiraB);
		TestArtifacts.addJiraToJira(jiraB, jiraC);
		
	// Assure at least one of the linked requirements, has at least one of its requirements is closed.
		ConsistencyRuleType crt = ConsistencyRuleType.create(ws, typeJira, "AnyTraverseTest",
		"self.requirements"
		+ "->exists( req : <"+typeJira.getQualifiedName()+"> | req.requirements"
				+ "->select(req2 | req2.state='Open')"
				+ "->isEmpty()"
				+ ")"
		);
		ws.concludeTransaction();
		assertTrue(ConsistencyUtils.crdValid(crt));
		String eval = (String) crt.ruleEvaluations().get().stream()
				.map(rule -> ((Rule)rule).contextInstance().toString()+":"+((Rule)rule).result()+"\r\n" )
				.collect(Collectors.joining(",","[","]"));
		System.out.println("Checking "+crt.name() +" Result: "+ eval);
		crt.consistencyRuleEvaluations().getValue().stream()
			.filter(cr -> !cr.isConsistent() )
			.filter(cr -> cr.contextInstance().equals(jiraA))
			.forEach(cr -> { 
			RepairNode repairTree = RuleService.repairTree(cr);
			printRepairActions(repairTree);
			assert(repairTree != null);
			
			/* Expect repairs:
			 * OK remove C from B 
			 * OK add to a.requirments Issue with its requirements containing at least one issue state that is not in state open
			 * OK change c.state to <> open
			 * */			
			RestrictionNode comp = new RestrictionNode.PropertyNode("requirements", null)											
											.setNextNodeFluent( 
													new RestrictionNode.OperationNode("isempty")
															.setNextNodeFluent(new SubtreeCombinatorNode(
																	new RestrictionNode.PropertyNode("requirements", null),
																	new RestrictionNode.BipartComparatorNode(Operator.MOD_EQ, 
																							new RestrictionNode.PropertyNode("state", null), 
																							new RestrictionNode.ValueNode("'Open'"))
													)));
			assert(matchesRestriction(repairTree, jiraA, comp));						
		});
	}
	
	@Test
	void testExistsTraversRepair() {
		Instance jiraB =  TestArtifacts.getJiraInstance(ws, "jiraB");
		Instance jiraC = TestArtifacts.getJiraInstance(ws, "jiraC");
		Instance jiraD = TestArtifacts.getJiraInstance(ws, "jiraD");
		Instance jiraA = TestArtifacts.getJiraInstance(ws, "jiraA");
		TestArtifacts.addJiraToJira(jiraA, jiraB);
		TestArtifacts.addJiraToJira(jiraB, jiraC);
		
	// Assure at least one of the linked requirements, has at least one of its requirements is closed.
		ConsistencyRuleType crt = ConsistencyRuleType.create(ws, typeJira, "AnyTraverseTest",
		"self.requirements"
		+ "->exists( req : <"+typeJira.getQualifiedName()+"> | req.requirements"
				+ "->select(req2 | req2.state='Closed')"
				+ "->size() > 0"
				+ ")"
		);
		ws.concludeTransaction();
		assertTrue(ConsistencyUtils.crdValid(crt));
		String eval = (String) crt.ruleEvaluations().get().stream()
				.map(rule -> ((Rule)rule).contextInstance().toString()+":"+((Rule)rule).result()+"\r\n" )
				.collect(Collectors.joining(",","[","]"));
		System.out.println("Checking "+crt.name() +" Result: "+ eval);
		crt.consistencyRuleEvaluations().getValue().stream()
			.filter(cr -> !cr.isConsistent() )
			.filter(cr -> cr.contextInstance().equals(jiraA))
			.forEach(cr -> { 
			RepairNode repairTree = RuleService.repairTree(cr);
			printRepairActions(repairTree);
			assert(repairTree != null);
			
			/* Expect repairs:
			 * add to a.requirments Issue with its requirements containing at least one issue state = closed
			 * add to b.requirements one issue with state is closed
			 * change c.state to closed
			 * */			
			RestrictionNode comp = new RestrictionNode.PropertyNode("requirements", null)											
											.setNextNodeFluent(new RestrictionNode.BipartComparatorNode(Operator.MOD_GT, 
													new RestrictionNode.OperationNode("size")
															.setNextNodeFluent(new SubtreeCombinatorNode(
																	new RestrictionNode.PropertyNode("requirements", null),
																	new RestrictionNode.BipartComparatorNode(Operator.MOD_EQ, 
																							new RestrictionNode.PropertyNode("state", null), 
																							new RestrictionNode.ValueNode("'Closed'"))
													)) , 
													new RestrictionNode.ValueNode("0"))
												);
			assert(matchesRestriction(repairTree, jiraA, comp));
			
			RestrictionNode comp2 = new RestrictionNode.PropertyNode("requirements", null)											
					.setNextNodeFluent(	new RestrictionNode.BipartComparatorNode(Operator.MOD_EQ, 
																	new RestrictionNode.PropertyNode("state", null), 
																	new RestrictionNode.ValueNode("'Closed'")
											));
			assert(matchesRestriction(repairTree, jiraB, comp2));
		});
	}
	
	@Test
	void testAnyExistsTraversRepair() {
		Instance jiraB =  TestArtifacts.getJiraInstance(ws, "jiraB");
		Instance jiraC = TestArtifacts.getJiraInstance(ws, "jiraC");
		Instance jiraD = TestArtifacts.getJiraInstance(ws, "jiraD");
		Instance jiraA = TestArtifacts.getJiraInstance(ws, "jiraA");
		TestArtifacts.addJiraToJira(jiraA, jiraB);
		TestArtifacts.addJiraToJira(jiraB, jiraC);
		
	// Take any of the linked requirement, and check if at least one of its requirements is closed.
		ConsistencyRuleType crt = ConsistencyRuleType.create(ws, typeJira, "AnyTraverseTest",
		"self.requirements->any() \r\n"
		+ "->asType(<"+typeJira.getQualifiedName()+">)"
			+ ".requirements"
				+ "->exists(req | req.state='Closed')"				
		);
		ws.concludeTransaction();
		assertTrue(ConsistencyUtils.crdValid(crt));
		String eval = (String) crt.ruleEvaluations().get().stream()
				.map(rule -> ((Rule)rule).contextInstance().toString()+":"+((Rule)rule).result()+"\r\n" )
				.collect(Collectors.joining(",","[","]"));
		System.out.println("Checking "+crt.name() +" Result: "+ eval);
		crt.consistencyRuleEvaluations().getValue().stream()
			.filter(cr -> !cr.isConsistent() )
			.forEach(cr -> { 
			RepairNode repairTree = RuleService.repairTree(cr);
			printRepairActions(repairTree);
			assert(repairTree != null);
		});
		 RepairNode repairTree = ConsistencyUtils.getRepairTree(crt,jiraA);		 
		 RestrictionNode comp2 = new RestrictionNode.PropertyNode("requirements", null)											
					.setNextNodeFluent(	new RestrictionNode.BipartComparatorNode(Operator.MOD_EQ, 
																	new RestrictionNode.PropertyNode("state", null), 
																	new RestrictionNode.ValueNode("'Closed'")
											));
		assert(matchesRestriction(repairTree, jiraB, comp2));
	 
		 RestrictionNode comp1 = new RestrictionNode.PropertyNode("requirements", null)	
				 .setNextNodeFluent(new RestrictionNode.OperationNode("any")
				 	.setNextNodeFluent( new RestrictionNode.PropertyNode("requirements", null)
					.setNextNodeFluent(	new RestrictionNode.BipartComparatorNode(Operator.MOD_EQ, 
																	new RestrictionNode.PropertyNode("state", null), 
																	new RestrictionNode.ValueNode("'Closed'")
											))));
		assert(matchesRestriction(repairTree, jiraA, comp1));
		// works
	}
	
	@Test
	void testRejectRepair() {
		Instance jiraB =  TestArtifacts.getJiraInstance(ws, "jiraB");
		Instance jiraC = TestArtifacts.getJiraInstance(ws, "jiraC");
		Instance jiraD = TestArtifacts.getJiraInstance(ws, "jiraD");
		Instance jiraA = TestArtifacts.getJiraInstance(ws, "jiraA");
		TestArtifacts.addJiraToJira(jiraA, jiraB);
		TestArtifacts.addJiraToJira(jiraA, jiraC);
		
	// Assure at least one of the linked requirements is closed.
		ConsistencyRuleType crt = ConsistencyRuleType.create(ws, typeJira, "RejectTest",
		"self.requirements"
				+ "->reject(req2 | req2.state='Open')"
				+ "->size() > 0"
		);
		ws.concludeTransaction();
		assertTrue(ConsistencyUtils.crdValid(crt));
		String eval = (String) crt.ruleEvaluations().get().stream()
				.map(rule -> ((Rule)rule).contextInstance().toString()+":"+((Rule)rule).result()+"\r\n" )
				.collect(Collectors.joining(",","[","]"));
		System.out.println("Checking "+crt.name() +" Result: "+ eval);
		crt.consistencyRuleEvaluations().getValue().stream()
			.filter(cr -> !cr.isConsistent() )
			.filter(cr -> cr.contextInstance().equals(jiraA))
			.forEach(cr -> { 
			RepairNode repairTree = RuleService.repairTree(cr);
			printRepairActions(repairTree);
			RestrictionNode comp = new RestrictionNode.PropertyNode("requirements", null)											
					.setNextNodeFluent(new RestrictionNode.NotNode(
											new RestrictionNode.BipartComparatorNode(Operator.MOD_EQ, 
																	new RestrictionNode.PropertyNode("state", null), 
																	new RestrictionNode.ValueNode("'Open'"))
							));
			assert(matchesRestriction(repairTree, jiraA, comp));
			/* Expect repairs:			 
			 * OK add to a.requirements one issue with state is closed
			 * OK change b.state to closed
			 * */			
		});
	}
	
	@Test
	void testRejectNotSizeRepair() {
		Instance jiraB =  TestArtifacts.getJiraInstance(ws, "jiraB");
		Instance jiraC = TestArtifacts.getJiraInstance(ws, "jiraC");
		Instance jiraD = TestArtifacts.getJiraInstance(ws, "jiraD");
		Instance jiraA = TestArtifacts.getJiraInstance(ws, "jiraA");
		TestArtifacts.addJiraToJira(jiraA, jiraB);		
		
	// Assure that not just one requirements is not closed.
		ConsistencyRuleType crt = ConsistencyRuleType.create(ws, typeJira, "RejectTest",
		"self.requirements"
				+ "->reject(req2 | req2.state='Closed')"
				+ "->size() <> 1"
		);
		ws.concludeTransaction();
		assertTrue(ConsistencyUtils.crdValid(crt));
		String eval = (String) crt.ruleEvaluations().get().stream()
				.map(rule -> ((Rule)rule).contextInstance().toString()+":"+((Rule)rule).result()+"\r\n" )
				.collect(Collectors.joining(",","[","]"));
		System.out.println("Checking "+crt.name() +" Result: "+ eval);
		crt.consistencyRuleEvaluations().getValue().stream()
			.filter(cr -> !cr.isConsistent() )
			.filter(cr -> cr.contextInstance().equals(jiraA))
			.forEach(cr -> { 
			RepairNode repairTree = RuleService.repairTree(cr);
			printRepairActions(repairTree);
			RestrictionNode comp = new RestrictionNode.PropertyNode("requirements", null)											
					.setNextNodeFluent(new RestrictionNode.NotNode(
											new RestrictionNode.BipartComparatorNode(Operator.MOD_EQ, 
																	new RestrictionNode.PropertyNode("state", null), 
																	new RestrictionNode.ValueNode("'Closed'"))
							));
			assert(matchesRestriction(repairTree, jiraA, comp));
			/* Expect repairs:			 
			 * OK add to a.requirements one issue with state not closed
			 * OK change b.state to closed
			 * OK remove b from a.reqs
			 * */			
		});
	}
	
	@Test
	void testSelectRepair() {
		Instance jiraB =  TestArtifacts.getJiraInstance(ws, "jiraB");
		Instance jiraC = TestArtifacts.getJiraInstance(ws, "jiraC");
		Instance jiraD = TestArtifacts.getJiraInstance(ws, "jiraD");
		Instance jiraA = TestArtifacts.getJiraInstance(ws, "jiraA");
		TestArtifacts.addJiraToJira(jiraA, jiraB);
		TestArtifacts.addJiraToJira(jiraA, jiraC);
		
	// Assure at least one of the linked requirements is closed.
		ConsistencyRuleType crt = ConsistencyRuleType.create(ws, typeJira, "RejectTest",
		"self.requirements"
				+ "->select(req2 | req2.state<>'Open')"
				+ "->size() > 0"
		);
		ws.concludeTransaction();
		assertTrue(ConsistencyUtils.crdValid(crt));
		String eval = (String) crt.ruleEvaluations().get().stream()
				.map(rule -> ((Rule)rule).contextInstance().toString()+":"+((Rule)rule).result()+"\r\n" )
				.collect(Collectors.joining(",","[","]"));
		System.out.println("Checking "+crt.name() +" Result: "+ eval);
		crt.consistencyRuleEvaluations().getValue().stream()
			.filter(cr -> !cr.isConsistent() )
			.filter(cr -> cr.contextInstance().equals(jiraA))
			.forEach(cr -> { 
			RepairNode repairTree = RuleService.repairTree(cr);
			printRepairActions(repairTree);
			RestrictionNode comp = new RestrictionNode.PropertyNode("requirements", null)											
					.setNextNodeFluent(new RestrictionNode.NotNode(
											new RestrictionNode.BipartComparatorNode(Operator.MOD_EQ, 
																	new RestrictionNode.PropertyNode("state", null), 
																	new RestrictionNode.ValueNode("'Open'"))
							));
			assert(matchesRestriction(repairTree, jiraA, comp));
			/* Expect repairs:			 
			 * OK add to a.requirements one issue with state is not open
			 * OK change c/b.state to not open
			 * */			
		});
	}
	
	@Test
	void testSelectNotSizeRepair() {
		Instance jiraB =  TestArtifacts.getJiraInstance(ws, "jiraB");
		Instance jiraC = TestArtifacts.getJiraInstance(ws, "jiraC");
		Instance jiraD = TestArtifacts.getJiraInstance(ws, "jiraD");
		Instance jiraA = TestArtifacts.getJiraInstance(ws, "jiraA");
		TestArtifacts.addJiraToJira(jiraA, jiraB);
		
		
	// Assure at there is not just a single requirement that is in state open
		ConsistencyRuleType crt = ConsistencyRuleType.create(ws, typeJira, "RejectTest",
		"self.requirements"
				+ "->select(req2 | req2.state='Open')"
				+ "->size() <> 1"
		);
		ws.concludeTransaction();
		assertTrue(ConsistencyUtils.crdValid(crt));
		String eval = (String) crt.ruleEvaluations().get().stream()
				.map(rule -> ((Rule)rule).contextInstance().toString()+":"+((Rule)rule).result()+"\r\n" )
				.collect(Collectors.joining(",","[","]"));
		System.out.println("Checking "+crt.name() +" Result: "+ eval);
		crt.consistencyRuleEvaluations().getValue().stream()
			.filter(cr -> !cr.isConsistent() )
			.filter(cr -> cr.contextInstance().equals(jiraA))
			.forEach(cr -> { 
			RepairNode repairTree = RuleService.repairTree(cr);
			printRepairActions(repairTree);
			RestrictionNode comp = new RestrictionNode.PropertyNode("requirements", null)											
					.setNextNodeFluent(						new RestrictionNode.BipartComparatorNode(Operator.MOD_EQ, 
																	new RestrictionNode.PropertyNode("state", null), 
																	new RestrictionNode.ValueNode("'Open'"))
							);
			assert(matchesRestriction(repairTree, jiraA, comp));
			/* Expect repairs:			 
			 * OK add to a.requirements one issue with state is not open
			 * OK remove b from a.reqs
			 * OK change b.state to not open
			 * */			
		});
	}
	
	@Test
	void testCollectRepair() {
		Instance jiraB =  TestArtifacts.getJiraInstance(ws, "jiraB");
		Instance jiraC = TestArtifacts.getJiraInstance(ws, "jiraC");
		Instance jiraD = TestArtifacts.getJiraInstance(ws, "jiraD");
		Instance jiraA = TestArtifacts.getJiraInstance(ws, "jiraA");
		TestArtifacts.addJiraToJira(jiraA, jiraB);
		TestArtifacts.addJiraToJira(jiraA, jiraC);
		
	// Assure at least one of the linked requirements is closed.
		ConsistencyRuleType crt = ConsistencyRuleType.create(ws, typeJira, "CollectTest",
		"self.requirements"
				+ "->select(req2 | req2.state<>'Open')"
				+"->collect(req3 : <"+typeJira.getQualifiedName()+"> | req3.state)" // this collect is not really sensical, but restrictions work
				+ "->size() > 0"
		);
		ws.concludeTransaction();
		assertTrue(ConsistencyUtils.crdValid(crt));
		String eval = (String) crt.ruleEvaluations().get().stream()
				.map(rule -> ((Rule)rule).contextInstance().toString()+":"+((Rule)rule).result()+"\r\n" )
				.collect(Collectors.joining(",","[","]"));
		System.out.println("Checking "+crt.name() +" Result: "+ eval);
		crt.consistencyRuleEvaluations().getValue().stream()
			.filter(cr -> !cr.isConsistent() )
			.filter(cr -> cr.contextInstance().equals(jiraA))
			.forEach(cr -> { 
			RepairNode repairTree = RuleService.repairTree(cr);
			printRepairActions(repairTree);
			RestrictionNode comp = new RestrictionNode.PropertyNode("requirements", null)											
					.setNextNodeFluent(new RestrictionNode.NotNode(
											new RestrictionNode.BipartComparatorNode(Operator.MOD_EQ, 
																	new RestrictionNode.PropertyNode("state", null), 
																	new RestrictionNode.ValueNode("'Open'"))
							));
			assert(matchesRestriction(repairTree, jiraA, comp));
			/* Expect repairs:			 
			 * OK add to a.requirements one issue with state is not open
			 * OK change c/b.state to not open
			 * */			
		});
	}
	
	@Test
	void testForAllRepair() {
		Instance jiraB =  TestArtifacts.getJiraInstance(ws, "jiraB");
		Instance jiraC = TestArtifacts.getJiraInstance(ws, "jiraC");
		Instance jiraD = TestArtifacts.getJiraInstance(ws, "jiraD");
		Instance jiraA = TestArtifacts.getJiraInstance(ws, "jiraA");
		TestArtifacts.addJiraToJira(jiraA, jiraB);
		TestArtifacts.addJiraToJira(jiraA, jiraC);
		TestArtifacts.addParentToJira(jiraB, jiraD); // only B has a parent, which is D, and D is NOT closed
	
		ConsistencyRuleType crt = ConsistencyRuleType.create(ws, typeJira, "ForAllTest",
		"self.requirements \r\n"
				+"->forAll(issue : <"+typeJira.getQualifiedName()+"> | issue.requirements.size() = 2)"
		);
		ws.concludeTransaction();
		assertTrue(ConsistencyUtils.crdValid(crt));
		String eval = (String) crt.ruleEvaluations().get().stream()
				.map(rule -> ((Rule)rule).contextInstance().toString()+":"+((Rule)rule).result()+"\r\n" )
				.collect(Collectors.joining(",","[","]"));
		System.out.println("Checking "+crt.name() +" Result: "+ eval);
		crt.consistencyRuleEvaluations().getValue().stream()
			.filter(cr -> !cr.isConsistent() || cr.contextInstance().equals(jiraA))
			.forEach(cr -> { 
			RepairNode repairTree = RuleService.repairTree(cr);
			printRepairActions(repairTree);
			assert(repairTree != null);
		});
		
		//works, no restrictions to create
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
		// from the issues in the requirements collect (select all parents that are defined) collect the parents and check that one exists that is in state Closed
		// jiraA --> req: JiraB . parent = jiraD . state = Open
		//		 --> req: JiraC . parent = null
		
		ws.concludeTransaction();
		assertTrue(ConsistencyUtils.crdValid(crt));
		String eval = (String) crt.ruleEvaluations().get().stream()
				.map(rule -> ((Rule)rule).result()+"" )
				.collect(Collectors.joining(",","[","]"));
		System.out.println("Checking "+crt.name() +" Result: "+ eval);
		crt.consistencyRuleEvaluations().getValue().stream()
			.filter(cr -> !cr.isConsistent())
			.filter(cr -> cr.contextInstance().equals(jiraA))
			.forEach(cr -> { 
			RepairNode repairTree = RuleService.repairTree(cr);
			printRepairActions(repairTree);
			RestrictionNode comp1 = new RestrictionNode.PropertyNode("requirements", null)	
					.setNextNodeFluent(new RestrictionNode.AndNode(
							new RestrictionNode.PropertyNode("parent", null)
								.setNextNodeFluent(new RestrictionNode.OperationNode("isdefined"))	, 
							new RestrictionNode.SubtreeCombinatorNode( 
									new RestrictionNode.PropertyNode("parent", null), 
									new RestrictionNode.BipartComparatorNode(Operator.MOD_EQ, 
											new RestrictionNode.PropertyNode("state", null), 
											new RestrictionNode.ValueNode("'Closed'"))
							)		
					));
			assert(matchesRestriction(repairTree, jiraA, comp1));
			RestrictionNode comp2 = new RestrictionNode.PropertyNode("parent", null)	
					.setNextNodeFluent(new RestrictionNode.AndNode(
										new RestrictionNode.OperationNode("isdefined"),
										new RestrictionNode.SubtreeCombinatorNode(
												new RestrictionNode.PropertyNode("parent", null), 
												new RestrictionNode.BipartComparatorNode(Operator.MOD_EQ, 
														new RestrictionNode.PropertyNode("state", null), 
														new RestrictionNode.ValueNode("'Closed'")																		
											))));
			assert(matchesRestriction(repairTree, jiraB, comp2));
			assert(matchesRestriction(repairTree, jiraC, comp2));
			

			
		});
		/* expected repairs: 
		 * OK jiraA.req add an issue that has a parent that is in state closed
		 * OK set B parent that is in state closed
		 * OK set C.parent that is in state closed
		 * OK: jiraD set state closed
		 * */		
	}
	
	@Test
	void testSelectExistsRepair() {
		Instance jiraB =  TestArtifacts.getJiraInstance(ws, "jiraB");
		Instance jiraC = TestArtifacts.getJiraInstance(ws, "jiraC");
		Instance jiraD = TestArtifacts.getJiraInstance(ws, "jiraD");
		Instance jiraA = TestArtifacts.getJiraInstance(ws, "jiraA");
		TestArtifacts.addJiraToJira(jiraA, jiraB);
		TestArtifacts.addJiraToJira(jiraA, jiraC);
		TestArtifacts.addParentToJira(jiraB, jiraD);
		// semantically identical to above constraint
		ConsistencyRuleType crt = ConsistencyRuleType.create(ws, typeJira, "DeepExistsTest",
		"self.requirements \r\n"
		+"->select(req | req.parent.isDefined() ) \r\n"
		+"->exists(req2 : <"+typeJira.getQualifiedName()+"> | req2.parent.state='Closed')"
		);
		// from the issues in the requirements collect (select all parents that are defined) collect the parents and check that one exists that is in state Closed
		// jiraA --> req: JiraB . parent = jiraD . state = Open
		//		 --> req: JiraC . parent = null
		
		ws.concludeTransaction();
		assertTrue(ConsistencyUtils.crdValid(crt));
		String eval = (String) crt.ruleEvaluations().get().stream()
				.map(rule -> ((Rule)rule).result()+"" )
				.collect(Collectors.joining(",","[","]"));
		System.out.println("Checking "+crt.name() +" Result: "+ eval);
		crt.consistencyRuleEvaluations().getValue().stream()
			.filter(cr -> !cr.isConsistent())
			.filter(cr -> cr.contextInstance().equals(jiraA))
			.forEach(cr -> { 
			RepairNode repairTree = RuleService.repairTree(cr);
			printRepairActions(repairTree);

			RestrictionNode comp1 = new RestrictionNode.PropertyNode("requirements", null)	
					.setNextNodeFluent(new RestrictionNode.AndNode(
							new RestrictionNode.PropertyNode("parent", null)
								.setNextNodeFluent(new RestrictionNode.OperationNode("isdefined"))	, 
							new RestrictionNode.BipartComparatorNode(Operator.MOD_EQ, 
									new RestrictionNode.PropertyNode("state", null), 
									new RestrictionNode.ValueNode("'Closed'")
							))		
					);
			assert(matchesRestriction(repairTree, jiraA, comp1));
			
			RestrictionNode comp2 = new RestrictionNode.PropertyNode("parent", null)											
					.setNextNodeFluent(new RestrictionNode.PropertyNode("state", null)
							.setNextNodeFluent(new RestrictionNode.OnlyComparatorNode(Operator.MOD_EQ)
									.setNextNodeFluent(new RestrictionNode.ValueNode("'Closed'")
											)));
			assert(matchesRestriction(repairTree, jiraB, comp2));
			
			RestrictionNode comp3 =  new RestrictionNode.PropertyNode("parent", null)	
					.setNextNodeFluent(new RestrictionNode.AndNode(
							new RestrictionNode.OperationNode("isdefined"),
							new RestrictionNode.BipartComparatorNode(Operator.MOD_EQ, 
									new RestrictionNode.PropertyNode("state", null), 
									new RestrictionNode.ValueNode("'Closed'")
											)));			
			assert(matchesRestriction(repairTree, jiraC, comp3));
			assert(matchesRestriction(repairTree, jiraB, comp3));

		});
		/* expected repairs: 
		 * OK jiraA.req add an issue that has a parent that is in state closed
		 * OK set B parent that is in state closed
		 * OK set C.parent that is in state closed
		 * OK: jiraD set state closed
		 * */		
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
	
		// jiraA --> req: JiraB . parent = jiraD . state = Open
		//		 --> req: JiraC . parent = null
		
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
		crt.consistencyRuleEvaluations().getValue().stream()
			.filter(cr -> cr.contextInstance().equals(jiraA))
			.forEach(cr -> { 
			RepairNode repairTree = RuleService.repairTree(cr);
			printRepairActions(repairTree);
			RestrictionNode comp1 = new RestrictionNode.PropertyNode("requirements", null)	
					.setNextNodeFluent(new RestrictionNode.AndNode(
							new RestrictionNode.PropertyNode("parent", null)
								.setNextNodeFluent(new RestrictionNode.OperationNode("isdefined"))	, 
							new RestrictionNode.SubtreeCombinatorNode( 
										new RestrictionNode.PropertyNode("parent", null),
										new RestrictionNode.PropertyNode("state", null)
											.setNextNodeFluent(new RestrictionNode.OperationNode("equalsignorecase")
													.setNextNodeFluent(new RestrictionNode.ValueNode("'Closed'")) )  
							)));
			assert(matchesRestriction(repairTree, jiraA, comp1));
			
			RestrictionNode comp2 = new RestrictionNode.PropertyNode("parent", null)
					.setNextNodeFluent(new RestrictionNode.AndNode(
							new RestrictionNode.OperationNode("isdefined")	, 
							new RestrictionNode.SubtreeCombinatorNode( 
										new RestrictionNode.PropertyNode("parent", null),
										new RestrictionNode.PropertyNode("state", null)
											.setNextNodeFluent(new RestrictionNode.OperationNode("equalsignorecase")
													.setNextNodeFluent(new RestrictionNode.ValueNode("'Closed'")) )  
							)));					
			assert(matchesRestriction(repairTree, jiraB, comp2));
			assert(matchesRestriction(repairTree, jiraC, comp2));			
		});
		
		/* expected repairs:
		 * OK jiraA.req add an issue that has a parent that is in state closed
		 * OK jiraC set a parent that is in state closed
		 * OK jiraB set a parent (that is in state closed)
		 * OK: jiraD set state closed
		 * */
	}
	
	@Test
	void testUnionRepair() {
		Instance jiraB =  TestArtifacts.getJiraInstance(ws, "jiraB");
		Instance jiraC = TestArtifacts.getJiraInstance(ws, "jiraC");
		Instance jiraD = TestArtifacts.getJiraInstance(ws, "jiraD");
		Instance jiraA = TestArtifacts.getJiraInstance(ws, "jiraA");
		TestArtifacts.addJiraToJira(jiraA, jiraB); // this has B in reqs and C as parent
		TestArtifacts.addParentToJira(jiraA, jiraC);
		TestArtifacts.addJiraToJira(jiraC, jiraD); // parent=C has D in req
		// calc union as A.reqs = B, and A.parent.reqs = D, neither are in state closed
		
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
		crt.consistencyRuleEvaluations().getValue().stream()
			.filter(cr -> !cr.isConsistent())
			.filter(cr -> cr.contextInstance().equals(jiraA))
			.forEach(cr -> { 
			RepairNode repairTree = RuleService.repairTree(cr);
			printRepairActions(repairTree);
			
			RestrictionNode comp1 = new RestrictionNode.PropertyNode("parent", null)
					.setNextNodeFluent(new RestrictionNode.PropertyNode("requirements", null)
					.setNextNodeFluent(new RestrictionNode.BipartComparatorNode(Operator.MOD_EQ, 
							new RestrictionNode.PropertyNode("state", null), 
							new RestrictionNode.ValueNode("'Closed'")))) ;
			assert(matchesRestriction(repairTree, jiraA, comp1));
			
			RestrictionNode comp3 = new RestrictionNode.PropertyNode("requirements", null)
					.setNextNodeFluent(new RestrictionNode.BipartComparatorNode(Operator.MOD_EQ, 
							new RestrictionNode.PropertyNode("state", null), 
							new RestrictionNode.ValueNode("'Closed'"))) ;
			assert(matchesRestriction(repairTree, jiraC, comp3));
		});
		/* Expected repairs to include: 
		 * OK close b
		 * OK add to a.req demoissue with state = closed
		 * OK add to C.req demoissue with state = closed 
		 * OK close d
		 * OK add to a.parent requirements with state = closed
		 * */
	}
	
	@Test
	void testUnionSizeRepair() {
		Instance jiraB =  TestArtifacts.getJiraInstance(ws, "jiraB");
		Instance jiraC = TestArtifacts.getJiraInstance(ws, "jiraC");
		Instance jiraD = TestArtifacts.getJiraInstance(ws, "jiraD");
		Instance jiraA = TestArtifacts.getJiraInstance(ws, "jiraA");
		TestArtifacts.addJiraToJira(jiraA, jiraB); // this has B in reqs and C as parent
		TestArtifacts.addParentToJira(jiraA, jiraC);
		TestArtifacts.addJiraToJira(jiraC, jiraD); // parent=C has D in req
		// calc union as A.reqs = B, and A.parent.reqs = D, neither are in state closed
		
		ConsistencyRuleType crt = ConsistencyRuleType.create(ws, typeJira, "UnionTest",
		"self.requirements"
		+ "		->select(req : <"+typeJira.getQualifiedName()+"> | req.state='Closed') \r\n"
		+ "->union(self.parent.requirements"
		+ "		->select(req2 : <"+typeJira.getQualifiedName()+"> | req2.state='Closed')) \r\n"
		+"->size() > 0"
		);
		ws.concludeTransaction();
		assertTrue(ConsistencyUtils.crdValid(crt));
		String eval = (String) crt.ruleEvaluations().get().stream()
				.map(rule -> ((Rule)rule).result()+"" )
				.collect(Collectors.joining(",","[","]"));
		System.out.println("Checking "+crt.name() +" Result: "+ eval);
		crt.consistencyRuleEvaluations().getValue().stream()
			.filter(cr -> !cr.isConsistent())
			.filter(cr -> cr.contextInstance().equals(jiraA))
			.forEach(cr -> { 
			RepairNode repairTree = RuleService.repairTree(cr);
			printRepairActions(repairTree);
			
			RestrictionNode comp1 = new RestrictionNode.PropertyNode("parent", null)
					.setNextNodeFluent(new RestrictionNode.PropertyNode("requirements", null)
					.setNextNodeFluent(new RestrictionNode.BipartComparatorNode(Operator.MOD_EQ, 
							new RestrictionNode.PropertyNode("state", null), 
							new RestrictionNode.ValueNode("'Closed'")))) ;
			assert(matchesRestriction(repairTree, jiraA, comp1));
			
			RestrictionNode comp3 = new RestrictionNode.PropertyNode("requirements", null)
					.setNextNodeFluent(new RestrictionNode.BipartComparatorNode(Operator.MOD_EQ, 
							new RestrictionNode.PropertyNode("state", null), 
							new RestrictionNode.ValueNode("'Closed'"))) ;
			assert(matchesRestriction(repairTree, jiraC, comp3));
		});
		/* Expected repairs to include: 
		 * OK close b
		 * OK add to a.req demoissue with state = closed
		 * OK add to C.req demoissue with state = close 
		 * OK close d
		 * OK add to a.parent requirements with state = closed
		 * */
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
		"self.requirements"
		+ "	->select(req : <"+typeJira.getQualifiedName()+"> | req.state='Closed')"
		 + "->size() > 0 \r\n"
		+ "or if self.parent.isDefined() then "
		+ 		"self.parent.requirements"
		+ 		"->select(req2 : <"+typeJira.getQualifiedName()+"> | req2.state='Closed')"
		 	+ "->size() > 0 "
		 + "else true endif"
		);
		ws.concludeTransaction();
		assertTrue(ConsistencyUtils.crdValid(crt));
		String eval = (String) crt.ruleEvaluations().get().stream()
				.map(rule -> ((Rule)rule).result()+"" )
				.collect(Collectors.joining(",","[","]"));
		System.out.println("Checking "+crt.name() +" Result: "+ eval);
		crt.consistencyRuleEvaluations().getValue().stream()
			.filter(cr -> !cr.isConsistent())
			.forEach(cr -> { 
			RepairNode repairTree = RuleService.repairTree(cr);
			printRepairActions(repairTree);
			
			RestrictionNode comp1 = new RestrictionNode.PropertyNode("parent", null)
					.setNextNodeFluent(new RestrictionNode.PropertyNode("requirements", null)
					.setNextNodeFluent(new RestrictionNode.BipartComparatorNode(Operator.MOD_EQ, 
							new RestrictionNode.PropertyNode("state", null), 
							new RestrictionNode.ValueNode("'Closed'")))) ;
			assert(matchesRestriction(repairTree, jiraA, comp1));
			
			RestrictionNode comp3 = new RestrictionNode.PropertyNode("requirements", null)
					.setNextNodeFluent(new RestrictionNode.BipartComparatorNode(Operator.MOD_EQ, 
							new RestrictionNode.PropertyNode("state", null), 
							new RestrictionNode.ValueNode("'Closed'"))) ;
			assert(matchesRestriction(repairTree, jiraC, comp3));
		});
		
		/* Expected repairs to include: 
		 * OK close b
		 * OK add to a.req demoissue with state = closed
		 * OK add to C a req with state = close -
		 * OK close d
		 * OK change JiraA.parent to state is closed
		 * */
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
	
	@Test
	void testSelectWithAsSetRepair() {
		StepDefinition s1 = StepDefinition.getInstance("S1", ws);
		//s1.setCondition(Conditions.PRECONDITION, "self.in_story->size() > 0");
		InstanceType typeStep = ProcessStep.getOrCreateDesignSpaceInstanceType(ws, s1);
		InstanceType gitType = TestArtifacts.getDemoGitIssueType(ws);
		if (typeStep.getPropertyType("in_story") == null) {
			typeStep.createPropertyType("in_story", Cardinality.SET, gitType);
		}
		Instance git1 = ws.createInstance(gitType, "Git1");
		git1.getPropertyAsSingle("title").set("Git1Title");
		Instance git2 = ws.createInstance(gitType, "Git2");
		git2.getPropertyAsSingle("title").set("WriteOrReviseMMF for Git1");
		git1.getPropertyAsSet("linkedIssues").add(git2);
		Instance step1 = ws.createInstance(typeStep, "Step1");
		step1.getPropertyAsSet("in_story").add(git1);
		ws.concludeTransaction();
		
		String arl = "((self.in_story.any().asType( <root/types/git_issue> ).linkedIssues\r\n" + 
				"->select( ref_3 : <root/types/git_issue> | ref_3.title.substring(1, 16).equalsIgnoreCase('WriteOrReviseMMF')).asSet().size() > 0 \r\n" + 
				"and self.in_story.any().asType( <root/types/git_issue> ).linkedIssues\r\n" + 
				"->select( ref_2 : <root/types/git_issue> | ref_2.title.substring(1, 11).equalsIgnoreCase('RefineToSUC')).asSet().size() > 0) \r\n" + 
				"and self.in_story.any().asType( <root/types/git_issue> ).linkedIssues\r\n" + 
				"->select( ref_1 : <root/types/git_issue> | ref_1.title.substring(1, 17).equalsIgnoreCase('CreateOrRefineCSC')).asSet().size() > 0)";
		ConsistencyRuleType crd1 = ConsistencyRuleType.create(ws, typeStep, "crd1", arl);
		ws.concludeTransaction();
		
		crd1.consistencyRuleEvaluations().getValue().stream()
		.filter(cr -> !cr.isConsistent())
		.forEach(cr -> { 
		RepairNode repairTree = RuleService.repairTree(cr);
		printRepairActions(repairTree);
		assert(repairTree != null);
	});
		// WORKS
	}
	
	@Test
	void testStartsWithRepair() {
		StepDefinition s1 = StepDefinition.getInstance("S1", ws);
		InstanceType typeStep = ProcessStep.getOrCreateDesignSpaceInstanceType(ws, s1);
		InstanceType gitType = TestArtifacts.getDemoGitIssueType(ws);
		if (typeStep.getPropertyType("in_story") == null) {
			typeStep.createPropertyType("in_story", Cardinality.SET, gitType);
		}
		Instance git1 = ws.createInstance(gitType, "Git1");
		git1.getPropertyAsSingle("title").set("Git1Title");
		Instance git2 = ws.createInstance(gitType, "Git2");
		git2.getPropertyAsSingle("title").set("WriteOrReviseMMF for Git1");
		git1.getPropertyAsSet("linkedIssues").add(git2);
		Instance step1 = ws.createInstance(typeStep, "Step1");
		step1.getPropertyAsSet("in_story").add(git1);
		ws.concludeTransaction();
		
		String arl = "self.in_story.any().asType( <root/types/git_issue> )"
				+ ".linkedIssues\r\n" + 
				"->select( ref_3 : <root/types/git_issue> | ref_3.title.startsWith('CreateOrRefineCSC'))"
				+ ".asSet().size() > 0 ";
		
		ConsistencyRuleType crd1 = ConsistencyRuleType.create(ws, typeStep, "crd1", arl);
		ws.concludeTransaction();
		
		crd1.consistencyRuleEvaluations().getValue().stream()
		.filter(cr -> !cr.isConsistent())
		.forEach(cr -> { 
		RepairNode repairTree = RuleService.repairTree(cr);
		printRepairActions(repairTree);
		
		RestrictionNode comp1 =  new RestrictionNode.PropertyNode("in_story", null)		
				.setNextNodeFluent(new RestrictionNode.OperationNode("any")
				.setNextNodeFluent(new RestrictionNode.PropertyNode("linkedIssues", null)		
				.setNextNodeFluent(new RestrictionNode.PropertyNode("title", null)
					.setNextNodeFluent(	new RestrictionNode.OperationNode("startswith")
						.setNextNodeFluent(new RestrictionNode.ValueNode("'CreateOrRefineCSC'")
										) ))));
		assert(matchesRestriction(repairTree, step1, comp1));
		
		RestrictionNode comp2 = new RestrictionNode.PropertyNode("linkedIssues", null)		
				.setNextNodeFluent(new RestrictionNode.PropertyNode("title", null)
					.setNextNodeFluent(	new RestrictionNode.OperationNode("startswith")
						.setNextNodeFluent(new RestrictionNode.ValueNode("'CreateOrRefineCSC'")
										) ));
		assert(matchesRestriction(repairTree, git1, comp2));
		
		assert(repairTree != null);
	});
		/* expected repairs
		 * OK add to Step1 gitissue wiht linkedIssues containing gitissue title startng with 'createor refin...
		 * OK add to git1 linked issues a gitissue with title startng with 'createor refin...
		 * OK set title of Git2 to CreateOrRefine ...
		 * */
		
	}
	
	@Test
	void testSelectWithoutAsSetRepair() {
		StepDefinition s1 = StepDefinition.getInstance("S1", ws);
		//s1.setCondition(Conditions.PRECONDITION, "self.in_story->size() > 0");
		InstanceType typeStep = ProcessStep.getOrCreateDesignSpaceInstanceType(ws, s1);
		InstanceType gitType = TestArtifacts.getDemoGitIssueType(ws);
		if (typeStep.getPropertyType("in_story") == null) {
			typeStep.createPropertyType("in_story", Cardinality.SET, gitType);
		}
		Instance git1 = ws.createInstance(gitType, "Git1");
		git1.getPropertyAsSingle("title").set("Git1Title");
		Instance git2 = ws.createInstance(gitType, "Git2");
		git2.getPropertyAsSingle("title").set("WriteOrReviseMMF");
		git1.getPropertyAsSet("linkedIssues").add(git2);
		Instance step1 = ws.createInstance(typeStep, "Step1");
		step1.getPropertyAsSet("in_story").add(git1);
		ws.concludeTransaction();
		
		String arl = "((self.in_story.any().asType( <root/types/git_issue> ).linkedIssues\r\n" + 
				"->select( ref_3 : <root/types/git_issue> | ref_3.title='WriteOrReviseMMF').size() > 0 \r\n" + 
				"and self.in_story.any().asType( <root/types/git_issue> ).linkedIssues\r\n" + 
				"->select( ref_2 : <root/types/git_issue> | ref_2.title.substring(1, 11).equalsIgnoreCase('RefineToSUC')).size() > 0) \r\n" + 
				"and self.in_story.any().asType( <root/types/git_issue> ).linkedIssues\r\n" + 
				"->select( ref_1 : <root/types/git_issue> | ref_1.title='CreateOrRefineCSC').size() > 0)";
		ConsistencyRuleType crd1 = ConsistencyRuleType.create(ws, typeStep, "crd1", arl);
		ws.concludeTransaction();
		
		crd1.consistencyRuleEvaluations().getValue().stream()
		.filter(cr -> !cr.isConsistent())
		.forEach(cr -> { 
		RepairNode repairTree = RuleService.repairTree(cr);
		printRepairActions(repairTree);
		assert(repairTree != null);
	});
		// WORKS
	}
	
	@Test
	void testAsSetRepair() {
		Instance jiraB =  TestArtifacts.getJiraInstance(ws, "jiraB");
		Instance jiraC = TestArtifacts.getJiraInstance(ws, "jiraC");
		//Instance jiraD = TestArtifacts.getJiraInstance(ws, "jiraD");
		Instance jiraA = TestArtifacts.getJiraInstance(ws, "jiraA");
		TestArtifacts.addJiraToJira(jiraA, jiraB); 
		TestArtifacts.addJiraToJira(jiraA, jiraC);
		TestArtifacts.addJiraToJira(jiraB, jiraC); 
		
		ConsistencyRuleType crt = ConsistencyRuleType.create(ws, typeJira, "AsSetTest", "self.requirements.asSet().size() > 2");
		ws.concludeTransaction();
		crt.consistencyRuleEvaluations().getValue().forEach(cr -> { RepairNode repairTree = RuleService.repairTree(cr); 
		printRepairActions(repairTree);});
		;
		//works
	}
	
	
	@Test
	void testSizeRepair() {
		Instance jiraB =  TestArtifacts.getJiraInstance(ws, "jiraB");
		Instance jiraC = TestArtifacts.getJiraInstance(ws, "jiraC");
		//Instance jiraD = TestArtifacts.getJiraInstance(ws, "jiraD");
		Instance jiraA = TestArtifacts.getJiraInstance(ws, "jiraA");
		TestArtifacts.addJiraToJira(jiraA, jiraB); 
		TestArtifacts.addJiraToJira(jiraA, jiraC);
		TestArtifacts.addJiraToJira(jiraB, jiraC); 
		
		ConsistencyRuleType crt = ConsistencyRuleType.create(ws, typeJira, "AsSetTest", "self.requirements.size() > 2");
		ws.concludeTransaction();
		crt.consistencyRuleEvaluations().getValue().forEach(cr -> { RepairNode repairTree = RuleService.repairTree(cr);
		printRepairActions(repairTree);});
		;
		//works
	}
	
	@Test
	void testAndRepair() {
		//Instance jiraB =  TestArtifacts.getJiraInstance(ws, "jiraB");
		//Instance jiraC = TestArtifacts.getJiraInstance(ws, "jiraC");
		Instance jiraD = TestArtifacts.getJiraInstance(ws, "jiraD");
		Instance jiraA = TestArtifacts.getJiraInstance(ws, "jiraA");
		TestArtifacts.addJiraToJira(jiraA, jiraD); 
		//TestArtifacts.addJiraToJira(jiraA, jiraC);
		//TestArtifacts.addJiraToJira(jiraB, jiraC); 
		
		ConsistencyRuleType crt = ConsistencyRuleType.create(ws, typeJira, "AndTest", 
				"self.requirements"
				+          "->forAll(req | req.parent.name='X')"
			  + "and "
			  + "self.requirements->size() > 1");
		ws.concludeTransaction();
		assertTrue(ConsistencyUtils.crdValid(crt));
		String eval = (String) crt.ruleEvaluations().get().stream()
				.map(rule -> ((Rule)rule).result()+"" )
				.collect(Collectors.joining(",","[","]"));
		System.out.println("Checking "+crt.name() +" Result: "+ eval);
		crt.consistencyRuleEvaluations().getValue().stream()
			.filter(cr -> !cr.isConsistent())
			.forEach(cr -> { 
			RepairNode repairTree = RuleService.repairTree(cr);
			printRepairActions(repairTree);
			assert(repairTree != null);
		});
		;
		
		// TODO: adding to requirements does not consider the constraint of the requirements property in the other AND branch
		// TODO: size not properly handled, how to understand that the size should not lead to any restriction "output"
	}
	
	@Test
	void testSelectForAllRepair() {
		Instance jiraB =  TestArtifacts.getJiraInstance(ws, "jiraB");
		Instance jiraC = TestArtifacts.getJiraInstance(ws, "jiraC");
		Instance jiraD = TestArtifacts.getJiraInstance(ws, "jiraD");
		Instance jiraA = TestArtifacts.getJiraInstance(ws, "jiraA");
		TestArtifacts.addJiraToJira(jiraA, jiraB); 
		TestArtifacts.addJiraToJira(jiraA, jiraC);
		TestArtifacts.addJiraToJira(jiraB, jiraD); 
		// for all issues in requirements select those that have all their requirements name != to jiraD, ensure that this does occur more than once

		ConsistencyRuleType crt = ConsistencyRuleType.create(ws, typeJira, "AndTest", 
				"self.requirements"
				+ "->select(req2 | "
				+          "req2.requirements->forAll(req | req.name <> 'jiraD'))"
			  + "->size() > 1");
		ws.concludeTransaction();
		assertTrue(ConsistencyUtils.crdValid(crt));
		String eval = (String) crt.ruleEvaluations().get().stream()
				.map(rule -> ((Rule)rule).result()+"" )
				.collect(Collectors.joining(",","[","]"));
		System.out.println("Checking "+crt.name() +" Result: "+ eval);
		crt.consistencyRuleEvaluations().getValue().stream()
			.filter(cr -> !cr.isConsistent())
			.filter(cr -> cr.contextInstance().equals(jiraA))
			.forEach(cr -> { 
			RepairNode repairTree = RuleService.repairTree(cr);
			printRepairActions(repairTree);
			assert(repairTree != null);
		});
		;
		// OK repair should suggest to change jiraD.name to something else than jiraD
		// OK repair should suggest to remove JiraD from jira B.reqs
		// OK repair should suggest to add to A where requirements all have name not jiraD (TODO: alternative to have reqs empty is missing)
	}
	
	@Test
	void testForAllSizeRepair() {
		Instance jiraB =  TestArtifacts.getJiraInstance(ws, "jiraB");
		Instance jiraC = TestArtifacts.getJiraInstance(ws, "jiraC");
		Instance jiraD = TestArtifacts.getJiraInstance(ws, "jiraD");
		Instance jiraA = TestArtifacts.getJiraInstance(ws, "jiraA");
		TestArtifacts.addJiraToJira(jiraA, jiraB); 
		TestArtifacts.addJiraToJira(jiraA, jiraC);
		TestArtifacts.addJiraToJira(jiraB, jiraD); 

		ConsistencyRuleType crt = ConsistencyRuleType.create(ws, typeJira, "AndTest", 
				"self.requirements"
				+"->forAll(req | req.requirements->size() = 1)");
		ws.concludeTransaction();
		assertTrue(ConsistencyUtils.crdValid(crt));
		String eval = (String) crt.ruleEvaluations().get().stream()
				.map(rule -> ((Rule)rule).result()+"" )
				.collect(Collectors.joining(",","[","]"));
		System.out.println("Checking "+crt.name() +" Result: "+ eval);
		crt.consistencyRuleEvaluations().getValue().stream()
			.filter(cr -> !cr.isConsistent())
			.forEach(cr -> { 
			RepairNode repairTree = RuleService.repairTree(cr);
			printRepairActions(repairTree);
			assert(repairTree != null);
		});
		;
		//works

	}
	
	@Test
	void testIncludesRepair() {
		Instance jiraB =  TestArtifacts.getJiraInstance(ws, "jiraB");
		Instance jiraC = TestArtifacts.getJiraInstance(ws, "jiraC");
		Instance jiraD = TestArtifacts.getJiraInstance(ws, "jiraD");
		Instance jiraA = TestArtifacts.getJiraInstance(ws, "jiraA");
		TestArtifacts.addParentToJira(jiraA, jiraB); 
		TestArtifacts.addJiraToJira(jiraA, jiraC);
		TestArtifacts.addJiraToJira(jiraA, jiraB);
		TestArtifacts.addJiraToJira(jiraB, jiraD); 
		// at least one element in parent requirements must be also found in self.requirements
		ConsistencyRuleType crt = ConsistencyRuleType.create(ws, typeJira, "IncludesTest",
				"self.parent.requirements" 
		 + "->exists(refitem |  self.requirements->includes(refitem) ) "
		);
		ws.concludeTransaction();
		assertTrue(ConsistencyUtils.crdValid(crt));
		String eval = (String) crt.ruleEvaluations().get().stream()
				.map(rule -> ((Rule)rule).result()+"" )
				.collect(Collectors.joining(",","[","]"));
		System.out.println("Checking "+crt.name() +" Result: "+ eval);
		crt.consistencyRuleEvaluations().getValue().stream()
			.filter(cr -> !cr.isConsistent())
			.filter(cr -> cr.contextInstance().equals(jiraA))
			.forEach(cr -> { 
			RepairNode repairTree = RuleService.repairTree(cr);
			printRepairActions(repairTree);
			assert(repairTree != null);
		});
		
		/* expected repairs:
		 * TODO: set as a.parent a demoissue with requirements containing a demo issue that is one of [jiraC, jiraB]
		 * TODO: add to jiraB.requirements a Demoissue where SELF/jiraA.requirements contains/includes that demoissue  
		 * */
	}
	
	@Test
	void testIncludesRepair2() {
		Instance jiraB =  TestArtifacts.getJiraInstance(ws, "jiraB");
		Instance jiraC = TestArtifacts.getJiraInstance(ws, "jiraC");
		Instance jiraD = TestArtifacts.getJiraInstance(ws, "jiraD");
		Instance jiraA = TestArtifacts.getJiraInstance(ws, "jiraA");
		TestArtifacts.addParentToJira(jiraA, jiraB); 		
		TestArtifacts.addJiraToJira(jiraB, jiraD); 
		// at least one element in parent requirements must be also found in self.requirements
		ConsistencyRuleType crt = ConsistencyRuleType.create(ws, typeJira, "IncludesTest",
				"self.parent.requirements" 
		 + "->exists(refitem |  self.requirements->includes(refitem) ) "
		);
		ws.concludeTransaction();
		assertTrue(ConsistencyUtils.crdValid(crt));
		String eval = (String) crt.ruleEvaluations().get().stream()
				.map(rule -> ((Rule)rule).result()+"" )
				.collect(Collectors.joining(",","[","]"));
		System.out.println("Checking "+crt.name() +" Result: "+ eval);
		crt.consistencyRuleEvaluations().getValue().stream()
			.filter(cr -> !cr.isConsistent())
			.filter(cr -> cr.contextInstance().equals(jiraA))
			.forEach(cr -> { 
			RepairNode repairTree = RuleService.repairTree(cr);
			printRepairActions(repairTree);
			assert(repairTree != null);
		});
		
		/* expected repairs:
		 * TODO: set as a.parent a demoissue with requirements containing a demo issue that is one of []
		 * TODO: add to jiraB.requirments a Demoissue with SELF.requirements containing/including that demoissue  
		 * */
	}
	
	public static boolean matchesRestriction(RepairNode rn, Instance subject, RestrictionNode treeToMatch) {
		return rn.getRepairActions().stream()
		.filter(ra -> ra instanceof AbstractRepairAction)
		.map(AbstractRepairAction.class::cast)
		.filter(ra -> ra.getRepairValueOption().getRestriction() != null )
		.filter(ra -> subject.equals(ra.getElement()))
		.filter(ra -> ra.getRepairValueOption().getRestriction().getRootNode() != null)
		.filter(ra -> (ra.getRepairValueOption().getRestriction().getRootNode().matches(treeToMatch)))
		.count() == 1l;		
	}
	
	public static long countRestrictedActions(RepairNode rnode) {
		return rnode.getRepairActions().stream()
		.filter(ra -> ra instanceof AbstractRepairAction)
		.map(AbstractRepairAction.class::cast)
		.filter(ra -> ra.getRepairValueOption().getRestriction() != null )
		.count();
	}
	
	public static void printRepairActions(RepairNode rnode) {
		if (rnode == null || rnode.getRepairs() == null) return;
		rnode.getRepairActions().stream()
		.filter(ra -> ra instanceof AbstractRepairAction)
		.map(AbstractRepairAction.class::cast)
		.forEach(ra -> {
			RestrictionNode rootNode =  ra.getRepairValueOption().getRestriction() != null ? ra.getRepairValueOption().getRestriction().getRootNode() : null;
			String value = ra.getValue() != null ? " "+ra.getOperator().toString()+" "+ra.getValue().toString() : " NO VALUE";
			String restriction = rootNode != null ? rootNode.printNodeTree(false,40) : value;					
			String inst = ra.getElement() != null ? ra.getElement().toString() : "null";
			StringBuffer sb = new StringBuffer();
			switch(ra.getOperator()) {
			case ADD:						 
				sb.append(String.format("Add to %s of ", ra.getProperty()));
				sb.append(inst);							
				sb.append(restriction);			
				break;
			case MOD_EQ:
			case MOD_GT:
			case MOD_LT:
			case MOD_NEQ:
				sb.append(String.format("Set the %s of ", ra.getProperty()));
				sb.append(inst);			
				sb.append(" to");
				sb.append(restriction);
				break;
			case REMOVE:
				sb.append(String.format("Remove from %s of ", ra.getProperty()));
				sb.append(inst);
				sb.append(restriction);
				break;
			default:
				break;		
			}
			System.out.println(sb.toString());
		});
	}
	
//	public static void printRepairActions(RepairNode rnode) {
//		if (rnode == null || rnode.getRepairs() == null) return;
//		rnode.getRepairActions().stream()
//		.filter(ra -> ra instanceof AbstractRepairAction)
//		.map(AbstractRepairAction.class::cast)
//		.filter(ra -> ra.getRepairValueOption().getRestriction() != null )
//		.forEach(ra -> { 
//			String inst = ra.getElement() != null ? ra.getElement().toString() : "null";			
//			String changeType = "Change ";
//			String opType = "";
//			String instType = "";
//			if (ra.getRepairValueOption().operator.equals(Operator.ADD)) {
//				changeType = "Add to ";
//				instType = "a "+getTypeOfProperty((Instance)ra.getElement(), ra.getProperty())+" ";
//			} else if (ra.getRepairValueOption().operator.equals(Operator.REMOVE)) {
//				changeType = "Remove from ";
//				instType = "a "+getTypeOfProperty((Instance)ra.getElement(), ra.getProperty())+" ";
//			} else {
//				switch(ra.getRepairValueOption().operator) {
//				case MOD_EQ:
//					opType = "";
//					break;
//				case MOD_GT:
//					opType = Operator.MOD_GT.toString();
//					break;
//				case MOD_LT:
//					opType = Operator.MOD_LT.toString();
//					break;
//				case MOD_NEQ:
//					opType = "not";
//					break;				
//				}				
//			}
//			RestrictionNode rootNode =  ra.getRepairValueOption().getRestriction() != null ? ra.getRepairValueOption().getRestriction().getRootNode() : null;
//			String restriction = rootNode != null ? rootNode.printNodeTree(false) : "";			
//			String out = changeType+inst+restriction;
//			System.out.println(out);
//			});
//	}
	
	public static String getTypeOfProperty(Instance subject, String property) {		
		if (subject != null && subject.hasProperty(property)) {
			PropertyType propT = subject.getProperty(property).propertyType();
			String propType = propT.referencedInstanceType().name();
			return propType;
		} else return "";
	}
}
