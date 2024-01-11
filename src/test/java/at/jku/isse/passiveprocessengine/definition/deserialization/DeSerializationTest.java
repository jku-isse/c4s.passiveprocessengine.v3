package at.jku.isse.passiveprocessengine.definition.deserialization;

import java.util.LinkedList;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import at.jku.isse.designspace.core.model.Workspace;
import at.jku.isse.designspace.core.service.WorkspaceService;
import at.jku.isse.passiveprocessengine.definition.activeobjects.ProcessDefinition;
import at.jku.isse.passiveprocessengine.definition.serialization.DTOs;
import at.jku.isse.passiveprocessengine.definition.serialization.DefinitionTransformer;
import at.jku.isse.passiveprocessengine.definition.serialization.JsonDefinitionSerializer;
import at.jku.isse.passiveprocessengine.demo.TestProcesses;
import at.jku.isse.passiveprocessengine.instance.types.ProcessConfigBaseElementType;

@ExtendWith(SpringExtension.class)
@SpringBootTest
public class DeSerializationTest {

	@Autowired
	WorkspaceService workspaceService;
	static JsonDefinitionSerializer json = new JsonDefinitionSerializer();
	
	@Test
	void testDeSerializeHtmlUrlAndDescription()
	{
		Workspace ws = WorkspaceService.createWorkspace("test", WorkspaceService.PUBLIC_WORKSPACE, WorkspaceService.ANY_USER, null, false, false);
		DTOs.Process procD = TestProcesses.getSimpleDTOSubprocess(ws);
		String jsonProc = json.toJson(procD);
		DTOs.Process deSer = json.fromJson(jsonProc);
		ProcessDefinition procDef = DefinitionTransformer.fromDTO(deSer, ws, false, new LinkedList<>(), new ProcessConfigBaseElementType(ws));
		procDef.setHtml_url("https://www.google.com/");
		procDef.setDescription("<ul><li>Inform participants about scope, review criteria, etc</li><li>Send work products to be reviewed to all participants</li><li>Schedule joint review</li><li>Set up mechanism to handle review outcomes</li></ul>");
		procD=DefinitionTransformer.toDTO(procDef);
		assert(procD.getHtml_url().equals(procDef.getHtml_url()));
		assert(procD.getDescription().equals(procDef.getDescription()));
	}

}
