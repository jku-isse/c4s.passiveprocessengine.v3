package at.jku.isse.designspace.passiveprocessengine.modeling;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import at.jku.isse.passiveprocessengine.core.Instance;
import at.jku.isse.passiveprocessengine.core.InstanceType;
import at.jku.isse.passiveprocessengine.demo.TestArtifacts;
import at.jku.isse.passiveprocessengine.utils.ElementInspectionUtils;

@ExtendWith(SpringExtension.class)
@SpringBootTest
class TestInspectElements {

	static InstanceType typeJira;
	
	@BeforeEach
	void setup() throws Exception {
		ws = WorkspaceService.createWorkspace("test", WorkspaceService.PUBLIC_WORKSPACE, WorkspaceService.ANY_USER, null, true, false);
		RuleService.currentWorkspace = ws;		
		typeJira = TestArtifacts.getJiraInstanceType(ws);
	}

	
	@Test
	void testInstanceType() {
		StringBuffer instBuf = new StringBuffer();	
		ElementInspectionUtils.printInstanceType(typeJira, instBuf);
		System.out.println(instBuf.toString());
	}
	
	@Test
	void testInstance() {
		Instance jiraB =  TestArtifacts.getJiraInstance(ws, "jiraB");				
		Instance jiraD = TestArtifacts.getJiraInstance(ws, "jiraD");
		Instance jiraA = TestArtifacts.getJiraInstance(ws, "jiraA");//, "jiraB", "jiraC");
		TestArtifacts.addJiraToJira(jiraA, jiraB);
		TestArtifacts.addJiraToJira(jiraA, jiraD);
		StringBuffer instBuf = new StringBuffer();	
		ElementInspectionUtils.printInstance(jiraA, instBuf);
		System.out.println(instBuf.toString());
	}

}
