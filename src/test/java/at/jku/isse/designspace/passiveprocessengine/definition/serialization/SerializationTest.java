package at.jku.isse.designspace.passiveprocessengine.definition.serialization;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import at.jku.isse.designspace.core.model.Workspace;
import at.jku.isse.designspace.core.service.WorkspaceService;
import at.jku.isse.passiveprocessengine.definition.ProcessDefinition;
import at.jku.isse.passiveprocessengine.definition.serialization.DTOs;
import at.jku.isse.passiveprocessengine.definition.serialization.DefinitionTransformer;
import at.jku.isse.passiveprocessengine.definition.serialization.JsonDefinitionSerializer;
import at.jku.isse.passiveprocessengine.demo.TestProcesses;
import at.jku.isse.passiveprocessengine.instance.StepLifecycle.Conditions;

@ExtendWith(SpringExtension.class)
@SpringBootTest
public class SerializationTest {

	
	@Autowired
	WorkspaceService workspaceService;
	
	//static Workspace ws;
	static JsonDefinitionSerializer json = new JsonDefinitionSerializer();
	
	@BeforeEach
	void setup() throws Exception {
	//	Workspace ws = WorkspaceService.createWorkspace("test", WorkspaceService.PUBLIC_WORKSPACE, WorkspaceService.ANY_USER, null, false, false);
	}
	

	@Test
	void testSerializeAndBackSimpleProcessDefinition() {
		Workspace ws = WorkspaceService.createWorkspace("test", WorkspaceService.PUBLIC_WORKSPACE, WorkspaceService.ANY_USER, null, false, false);
		
		DTOs.Process procD = TestProcesses.getSimpleDTOSubprocess(ws);
		String jsonProc = json.toJson(procD);
		System.out.println(jsonProc);
		DTOs.Process deSer = json.fromJson(jsonProc);
		ProcessDefinition procDef = DefinitionTransformer.fromDTO(deSer, ws);
		assert(procDef.getName().equals(procD.getCode()));
		assert(procDef.getStepDefinitions().size() == procD.getSteps().size());
		assert(procDef.getCondition(Conditions.PRECONDITION).get().equals(procD.getConditions().get(Conditions.PRECONDITION)));
	}
	
	@Test
	void testSerializeAndBackParentChildProcessDefinition() {
		Workspace ws = WorkspaceService.createWorkspace("test", WorkspaceService.PUBLIC_WORKSPACE, WorkspaceService.ANY_USER, null, false, false);
		
		DTOs.Process procChild = TestProcesses.getSimpleDTOSubprocess(ws);
		DTOs.Process procD = TestProcesses.getSimpleSuperDTOProcessDefinition(ws);
		String jsonProc = json.toJson(procD);
		System.out.println(jsonProc);
		DTOs.Process deSer = json.fromJson(jsonProc);
		ProcessDefinition procDef = DefinitionTransformer.fromDTO(deSer, ws);
		assert(procDef.getName().equals(procD.getCode()));
		assert(procDef.getStepDefinitions().size() == procD.getSteps().size());
		assert(procDef.getCondition(Conditions.PRECONDITION).get().equals(procD.getConditions().get(Conditions.PRECONDITION)));
	
		assert(procDef.getStepDefinitions().stream()
			.filter(sd -> sd.getName().equals(procChild.getCode()))
			.map(sd -> {
				assert(((ProcessDefinition) sd).getStepDefinitions().size() == procChild.getSteps().size());
				assert(sd.getCondition(Conditions.PRECONDITION).get().equals(procChild.getConditions().get(Conditions.PRECONDITION)));
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
	void testOutputFromProcessDef() {
		Workspace ws = WorkspaceService.createWorkspace("test", WorkspaceService.PUBLIC_WORKSPACE, WorkspaceService.ANY_USER, null, false, false);
		ProcessDefinition inPD = TestProcesses.getSimple2StepProcessDefinition(ws);
		DTOs.Process procD = DefinitionTransformer.toDTO(inPD);
		String jsonProc = json.toJson(procD);
		System.out.println(jsonProc);
		DTOs.Process deSer = json.fromJson(jsonProc);
		ProcessDefinition procDef = DefinitionTransformer.fromDTO(deSer, ws);
		assert(procDef.getName().equals(inPD.getName()));
	}

}
