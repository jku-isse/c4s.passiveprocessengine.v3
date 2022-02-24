package at.jku.isse.designspace.tests.passiveprocessengine.serverside;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import at.jku.isse.designspace.core.model.Instance;
import at.jku.isse.designspace.core.model.InstanceType;
import at.jku.isse.designspace.core.model.Workspace;
import at.jku.isse.designspace.core.service.WorkspaceService;
import at.jku.isse.passiveprocessengine.definition.DecisionNodeDefinition;
import at.jku.isse.passiveprocessengine.definition.ProcessDefinition;
import at.jku.isse.passiveprocessengine.definition.StepDefinition;

@ExtendWith(SpringExtension.class)
@SpringBootTest
class DefinitionTests {

	Workspace ws;
	InstanceType typeJira;
	Instance jira1, jira2;
	
	@BeforeEach
	void setup() throws Exception {
		ws = WorkspaceService.createWorkspace("test", WorkspaceService.PUBLIC_WORKSPACE, WorkspaceService.ANY_USER, null, false, false);
		ws.setAutoUpdate(true);
		typeJira = TestArtifacts.getJiraInstanceType(ws);
	}

	@Test
	void testCreateSpec() {
		StepDefinition sd1 = StepDefinition.getInstance("sd1", ws);
		StepDefinition sd2 = StepDefinition.getInstance("sd2", ws);
		DecisionNodeDefinition dnd1 = DecisionNodeDefinition.getInstance("dnd1", ws);
		DecisionNodeDefinition dnd2 = DecisionNodeDefinition.getInstance("dnd2", ws);
		DecisionNodeDefinition dnd3 = DecisionNodeDefinition.getInstance("dnd3", ws);
		ProcessDefinition procDef = ProcessDefinition.getInstance("proc1", ws);
		procDef.addExpectedInput("jira", typeJira);
		
		procDef.addDecisionNodeDefinition(dnd1);
		procDef.addDecisionNodeDefinition(dnd2);
		procDef.addDecisionNodeDefinition(dnd3);
		procDef.addStepDefinition(sd1);
		procDef.addStepDefinition(sd2);
		
		assertTrue(procDef.getDecisionNodeDefinitions().size() == 3);
		assertTrue(procDef.getStepDefinitions().size() == 2);
		
		
		
	}

}
