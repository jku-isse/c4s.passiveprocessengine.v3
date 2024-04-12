package at.jku.isse.designspace.passiveprocessengine.definition.serialization;

import java.util.ArrayList;

import org.junit.Ignore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import at.jku.isse.designspace.core.model.Workspace;
import at.jku.isse.designspace.core.service.WorkspaceService;
import at.jku.isse.passiveprocessengine.configurability.ProcessConfigBaseElementFactory;
import at.jku.isse.passiveprocessengine.definition.ProcessDefinition;
import at.jku.isse.passiveprocessengine.definition.serialization.DTOs;
import at.jku.isse.passiveprocessengine.definition.serialization.DefinitionTransformer;
import at.jku.isse.passiveprocessengine.definition.serialization.JsonDefinitionSerializer;
import at.jku.isse.passiveprocessengine.demo.TestProcesses;
import at.jku.isse.passiveprocessengine.instance.ProcessException;

@ExtendWith(SpringExtension.class)
@SpringBootTest
public class SerializationAfterPersistenceTest {

	
	@Autowired
	WorkspaceService workspaceService;
	
	//static Workspace ws;
	static JsonDefinitionSerializer json = new JsonDefinitionSerializer();
	
	@BeforeEach
	void setup() throws Exception {
	//	Workspace ws = WorkspaceService.createWorkspace("test", WorkspaceService.PUBLIC_WORKSPACE, WorkspaceService.ANY_USER, null, false, false);
	}
	
	@Test
	void testSerializeHtmlUrlAndDescription()
	{
		Workspace ws = WorkspaceService.createWorkspace("test", WorkspaceService.PUBLIC_WORKSPACE, WorkspaceService.ANY_USER, null, false, false);
		DTOs.Process procD = TestProcesses.getSimpleDTOSubprocess(ws);
		procD.setHtml_url("https://www.google.com/");
		procD.setDescription("<ul><li>Inform participants about scope, review criteria, etc</li><li>Send work products to be reviewed to all participants</li><li>Schedule joint review</li><li>Set up mechanism to handle review outcomes</li></ul>");
		String jsonProc = json.toJson(procD);
		System.out.println(jsonProc);
		DTOs.Process deSer = json.fromJson(jsonProc);
		ProcessDefinition procDef = DefinitionTransformer.fromDTO(deSer, ws, false, new ArrayList<>(), new ProcessConfigBaseElementFactory(ws));
		assert(procDef.getName().equals(procD.getCode()));
		assert(procDef.getStepDefinitions().size() == procD.getSteps().size());
		//assert(procDef.getPreconditions().stream().map(cond -> cond.getConstraintSpec()).allMatch(cond -> procD.getConditions().);
		assert(procDef.getHtml_url().equals(procD.getHtml_url()));
		assert(procDef.getDescription().equals(procD.getDescription()));
	}
	@Test
	void testSerializeAndBackSimpleProcessDefinition() {
		Workspace ws = WorkspaceService.createWorkspace("test", WorkspaceService.PUBLIC_WORKSPACE, WorkspaceService.ANY_USER, null, false, false);

		DTOs.Process procD = TestProcesses.getSimpleDTOSubprocess(ws);
		String jsonProc = json.toJson(procD);
		System.out.println(jsonProc);
		DTOs.Process deSer = json.fromJson(jsonProc);
		ProcessDefinition procDef = DefinitionTransformer.fromDTO(deSer, ws, false, new ArrayList<>(), new ProcessConfigBaseElementFactory(ws));
		assert(procDef.getName().equals(procD.getCode()));
		assert(procDef.getStepDefinitions().size() == procD.getSteps().size());
	//	assert(procDef.getCondition(Conditions.PRECONDITION).get().equals(procD.getConditions().get(Conditions.PRECONDITION)));
	}
	
	@Ignore //needs adaptation
	@Test
	void testSerializeAndBackParentChildProcessDefinition() {
		Workspace ws = WorkspaceService.createWorkspace("test", WorkspaceService.PUBLIC_WORKSPACE, WorkspaceService.ANY_USER, null, false, false);
		
		DTOs.Process procChild = TestProcesses.getSimpleDTOSubprocess(ws);
		DTOs.Process procD = TestProcesses.getSimpleSuperDTOProcessDefinition(ws);
		String jsonProc = json.toJson(procD);
		System.out.println(jsonProc);
		DTOs.Process deSer = json.fromJson(jsonProc);
		ProcessDefinition procDef = DefinitionTransformer.fromDTO(deSer, ws, false, new ArrayList<>(), new ProcessConfigBaseElementFactory(ws));
		assert(procDef.getName().equals(procD.getCode()));
		assert(procDef.getStepDefinitions().size() == procD.getSteps().size());
	//	assert(procDef.getCondition(Conditions.PRECONDITION).get().equals(procD.getConditions().get(Conditions.PRECONDITION)));
	
		assert(procDef.getStepDefinitions().stream()
			.filter(sd -> sd.getName().equals(procChild.getCode()))
			.map(sd -> {
				assert(((ProcessDefinition) sd).getStepDefinitions().size() == procChild.getSteps().size());
		//		assert(sd.getCondition(Conditions.PRECONDITION).get().equals(procChild.getConditions().get(Conditions.PRECONDITION)));
				return true;
			})
			.count() == 1);
				
	}
	
	@Test
	void testOutputGitSimpleProcess() {
		DTOs.Process procD = TestProcesses.getMinimalGithubBasedProcess();
		String jsonProc = json.toJson(procD);
		System.out.println(jsonProc);
	}
	
	@Test
	void testOutputFromProcessDef() throws ProcessException {
		Workspace ws = WorkspaceService.PUBLIC_WORKSPACE; //WorkspaceService.createWorkspace("test", WorkspaceService.PUBLIC_WORKSPACE, WorkspaceService.ANY_USER, null, false, false);
		ProcessDefinition inPD = TestProcesses.getSimple2StepProcessDefinition(ws);
		DTOs.Process procD = DefinitionTransformer.toDTO(inPD);
		String jsonProc = json.toJson(procD);
		System.out.println(jsonProc);
		DTOs.Process deSer = json.fromJson(jsonProc);
		ProcessDefinition procDef = DefinitionTransformer.fromDTO(deSer, ws, false, new ArrayList<>(), new ProcessConfigBaseElementFactory(ws));
		assert(procDef.getName().equals(inPD.getName()));
	}

}
