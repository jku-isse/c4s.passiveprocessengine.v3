package at.jku.isse.designspace.passiveprocessengine.instance;

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
import at.jku.isse.designspace.rule.model.ConsistencyRuleType;
import at.jku.isse.designspace.rule.service.RuleService;
import at.jku.isse.passiveprocessengine.demo.TestArtifacts;
import at.jku.isse.passiveprocessengine.demo.TestArtifacts.JiraStates;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ExtendWith(SpringExtension.class)
@SpringBootTest
class PureTemporalConstraintTests {

	static Workspace ws;
	static InstanceType typeJira;
	
	@BeforeEach
	void setup() throws Exception {
		RuleService.setEvaluator(new ArlRuleEvaluator());
		ws = WorkspaceService.createWorkspace("test", WorkspaceService.PUBLIC_WORKSPACE, WorkspaceService.ANY_USER, null, true, false);
		RuleService.currentWorkspace = ws;
		typeJira = TestArtifacts.getJiraInstanceType(ws);
	}
	
	@Test
	void testPureConstraint() {
		//Instance jiraA = TestArtifacts.getJiraInstance(ws, "jiraA");		
		Instance jiraC = TestArtifacts.getJiraInstance(ws, "jiraC");		
		//TestArtifacts.addJiraToJira(jiraA, jiraC);	
		
		ConsistencyRuleType crt = ConsistencyRuleType.create(ws, typeJira, "TempTest", "eventually(self.state = 'ReadyForReview') and eventually( everytime( self.state = 'ReadyForReview' ,  eventually(self.state = 'Released')) )");
		ws.concludeTransaction();
		TestArtifacts.setStateToJiraInstance(jiraC, JiraStates.ReadyForReview);
		ws.concludeTransaction();
						
		TestArtifacts.setStateToJiraInstance(jiraC, JiraStates.Closed);
		ws.concludeTransaction();
				
		TestArtifacts.setStateToJiraInstance(jiraC, JiraStates.Open);
		ws.concludeTransaction();
		
		TestArtifacts.setStateToJiraInstance(jiraC, JiraStates.ReadyForReview);
		ws.concludeTransaction();
		
		// now lets complete step1:		
		TestArtifacts.setStateToJiraInstance(jiraC, JiraStates.Released);
		ws.concludeTransaction();
		
	}
	
	@Test
	public void testDeviationFromSequenceAbsence() {		
		Instance jiraC = TestArtifacts.getJiraInstance(ws, "jiraC", "jiraA");	
		TestArtifacts.setStateToJiraInstance(jiraC, JiraStates.Released);
		ConsistencyRuleType crt = ConsistencyRuleType.create(ws, typeJira, "TempTest2", "self.requirementIDs.size() >= 0 and asSoonAs(self.state = 'Released', always(self.state = 'Released') or not ( asSoonAs(self.state <> 'Released' , self.state = 'Released'))) ");
		ws.concludeTransaction();
		
		assert(crt.consistencyRuleEvaluation(jiraC).isConsistent()==true);
		
		TestArtifacts.setStateToJiraInstance(jiraC, JiraStates.ReadyForReview);
		ws.concludeTransaction();
		assert(crt.consistencyRuleEvaluation(jiraC).isConsistent()==true);
		
		TestArtifacts.setStateToJiraInstance(jiraC, JiraStates.Released);
		ws.concludeTransaction();
		
		TestArtifacts.addReqIdsToJira(jiraC, "jiraB");
		ws.concludeTransaction();
		
		TestArtifacts.setStateToJiraInstance(jiraC, JiraStates.Released);
		ws.concludeTransaction();
		assert(crt.consistencyRuleEvaluation(jiraC).isConsistent()==false);
	}
	
	@Test
	public void testDeviationFromSequenceAbsence1() {		
		Instance jiraC = TestArtifacts.getJiraInstance(ws, "jiraC");			
		TestArtifacts.setStateToJiraInstance(jiraC, JiraStates.Released);
		ConsistencyRuleType crt = ConsistencyRuleType.create(ws, typeJira, "TempTest2", " always(self.state = 'Released') or not ( asSoonAs(self.state <> 'Released' , self.state = 'Released')) ");
		ws.concludeTransaction();
		
		assert(crt.consistencyRuleEvaluation(jiraC).isConsistent()==true);
		
		TestArtifacts.setStateToJiraInstance(jiraC, JiraStates.ReadyForReview);
		ws.concludeTransaction();
		assert(crt.consistencyRuleEvaluation(jiraC).isConsistent()==true);
		
		TestArtifacts.setStateToJiraInstance(jiraC, JiraStates.Released);
		ws.concludeTransaction();
		assert(crt.consistencyRuleEvaluation(jiraC).isConsistent()==false);
	}
	
	@Test
	public void testDeviationFromAlwaysSequence() {		
		ConsistencyRuleType crt = ConsistencyRuleType.create(ws, typeJira, "TempTest2", " always(self.state = 'Released')");
		ws.concludeTransaction();
		
		Instance jiraC = TestArtifacts.getJiraInstance(ws, "jiraC");
		TestArtifacts.setStateToJiraInstance(jiraC, JiraStates.Released);
		ws.concludeTransaction();
		assert(crt.consistencyRuleEvaluation(jiraC).isConsistent()==true);
		
		TestArtifacts.setStateToJiraInstance(jiraC, JiraStates.ReadyForReview);
		ws.concludeTransaction();
		assert(crt.consistencyRuleEvaluation(jiraC).isConsistent()==false);
		
		TestArtifacts.setStateToJiraInstance(jiraC, JiraStates.Released);
		ws.concludeTransaction();
		assert(crt.consistencyRuleEvaluation(jiraC).isConsistent()==false);
	}
	
	@Test
	public void testDeviationFromEventuallyAlwaysSequence() {		
		ConsistencyRuleType crt = ConsistencyRuleType.create(ws, typeJira, "TempTest2", " eventually(always(self.state = 'Released'))");
		ws.concludeTransaction();
		
		Instance jiraC = TestArtifacts.getJiraInstance(ws, "jiraC");
		TestArtifacts.setStateToJiraInstance(jiraC, JiraStates.Released);
		ws.concludeTransaction();
		assert(crt.consistencyRuleEvaluation(jiraC).isConsistent()==true);
		
		TestArtifacts.setStateToJiraInstance(jiraC, JiraStates.ReadyForReview);
		ws.concludeTransaction();
		assert(crt.consistencyRuleEvaluation(jiraC).isConsistent()==false);
		
		TestArtifacts.setStateToJiraInstance(jiraC, JiraStates.Released);
		ws.concludeTransaction();
		assert(crt.consistencyRuleEvaluation(jiraC).isConsistent()==true);
	}
	
	@Test
	public void testDeviationFromSequenceAbsence2() {		
		Instance jiraC = TestArtifacts.getJiraInstance(ws, "jiraC");			
		
		ConsistencyRuleType crt = ConsistencyRuleType.create(ws, typeJira, "TempTest3", "eventually(self.state = 'Released') and asSoonAs(self.state = 'Released' , always(self.state = 'Released') or not (next( asSoonAs(self.state <> 'Released' , self.state = 'Released') ) )) ");
		ws.concludeTransaction();
		TestArtifacts.setStateToJiraInstance(jiraC, JiraStates.ReadyForReview);
		ws.concludeTransaction();
		assert(crt.consistencyRuleEvaluation(jiraC).isConsistent()==false);
						
		TestArtifacts.setStateToJiraInstance(jiraC, JiraStates.Open);
		ws.concludeTransaction();
		assert(crt.consistencyRuleEvaluation(jiraC).isConsistent()==false);
		
		TestArtifacts.setStateToJiraInstance(jiraC, JiraStates.Released);
		ws.concludeTransaction();
		assert(crt.consistencyRuleEvaluation(jiraC).isConsistent()==true);
		
		TestArtifacts.setStateToJiraInstance(jiraC, JiraStates.ReadyForReview);
		ws.concludeTransaction();
		assert(crt.consistencyRuleEvaluation(jiraC).isConsistent()==true);
		
		TestArtifacts.setStateToJiraInstance(jiraC, JiraStates.Released);
		ws.concludeTransaction();
		assert(crt.consistencyRuleEvaluation(jiraC).isConsistent()==false);
	}

}
