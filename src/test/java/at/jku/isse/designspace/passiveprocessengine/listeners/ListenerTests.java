package at.jku.isse.designspace.passiveprocessengine.listeners;

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
import at.jku.isse.passiveprocessengine.definition.DecisionNodeDefinition.InFlowType;
import at.jku.isse.passiveprocessengine.demo.TestArtifacts;
import at.jku.isse.passiveprocessengine.definition.MappingDefinition;
import at.jku.isse.passiveprocessengine.definition.ProcessDefinition;
import at.jku.isse.passiveprocessengine.definition.StepDefinition;
import at.jku.isse.passiveprocessengine.instance.StepLifecycle.Conditions;

@ExtendWith(SpringExtension.class)
@SpringBootTest
class ListenerTests {

	Workspace ws;	
	JiraItemAugmentor jia;
	
	@BeforeEach
	void setup() throws Exception {
		ws = WorkspaceService.createWorkspace("test", WorkspaceService.PUBLIC_WORKSPACE, WorkspaceService.ANY_USER, null, false, false);
		ws.setAutoUpdate(true);
		jia = new JiraItemAugmentor(ws);
		ws.workspaceListeners.add(jia);
	}

	@Test
	void testListenCreateSpec() {
		
		InstanceType procDef = ProcessDefinition.getOrCreateDesignSpaceCoreSchema(ws);
		ws.concludeTransaction();
		InstanceType someType = TestArtifacts.getJiraInstanceType(ws);
		ProcessDefinition pDef = ProcessDefinition.getInstance("proc1", ws);
		pDef.addExpectedInput("jiraIn", someType);
		Instance someInst = TestArtifacts.getJiraInstance(ws, "Test");
		ws.concludeTransaction();
		assert(pDef.getInstance().hasProperty(JiraItemAugmentor.JAMA2JIRALINKPROPERTYNAME));
		assert(someInst.hasProperty(JiraItemAugmentor.JAMA2JIRALINKPROPERTYNAME));
	}

	

}
