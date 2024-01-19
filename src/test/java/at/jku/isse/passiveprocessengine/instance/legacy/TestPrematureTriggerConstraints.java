//package at.jku.isse.passiveprocessengine.definition;
//
//import static org.junit.jupiter.api.Assertions.*;
//
//import java.io.IOException;
//import java.nio.charset.Charset;
//import java.nio.file.Files;
//import java.nio.file.Paths;
//
//import javax.xml.bind.JAXBException;
//
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.test.context.junit.jupiter.SpringExtension;
//
//import at.jku.isse.designspace.core.model.InstanceType;
//import at.jku.isse.designspace.core.model.Workspace;
//import at.jku.isse.designspace.core.service.WorkspaceService;
//import at.jku.isse.passiveprocessengine.analysis.PrematureTriggerGenerator;
//import at.jku.isse.passiveprocessengine.definition.activeobjects.ProcessDefinition;
//import at.jku.isse.passiveprocessengine.definition.serialization.DTOs;
//import at.jku.isse.passiveprocessengine.definition.serialization.DefinitionTransformer;
//import at.jku.isse.passiveprocessengine.definition.serialization.JsonDefinitionSerializer;
//import at.jku.isse.passiveprocessengine.definition.serialization.ProcessRegistry;
//import at.jku.isse.passiveprocessengine.demo.TestArtifacts;
//import at.jku.isse.passiveprocessengine.instance.ProcessException;
//import at.jku.isse.passiveprocessengine.instance.types.ProcessConfigBaseElementType;
//
//@ExtendWith(SpringExtension.class)
//@SpringBootTest
//class TestPrematureTriggerConstraints {
//
//	
//	@Autowired
//	WorkspaceService workspaceService;
//	
//	static JsonDefinitionSerializer json = new JsonDefinitionSerializer();
//	
//
//	@Test
//	void testPrematureRuleGeneration() throws IOException, ProcessException  {
//		Workspace ws = WorkspaceService.createWorkspace("test", WorkspaceService.PUBLIC_WORKSPACE, WorkspaceService.ANY_USER, null, false, false);
//		InstanceType typeGitDemo = TestArtifacts.getDemoGitIssueType(ws);
//		ws.concludeTransaction();
//		String path = ".";
//		String file = path+"/src/test/resources/prematuretest.json"; 
//		String content = Files.readString(Paths.get(file));	
//		DTOs.Process procD = json.fromJson(content);
//		
//		ProcessRegistry preg = new ProcessRegistry();
//		preg.inject(ws, new ProcessConfigBaseElementType(ws));
//		ProcessDefinition pd = preg.storeProcessDefinition(procD, false).getKey();
//		// = DefinitionTransformer.fromDTO(procD, ws);
//		//new PrematureTriggerGenerator(ws, pd).generatePrematureConstraints();
//		pd.getPrematureTriggers().entrySet().forEach(entry -> System.out.println(entry.getKey()+":\r\n"+entry.getValue()));
//		assert(pd.getPrematureTriggers().containsKey("WriteOrReviseMMF") == true);
//		assert(pd.getPrematureTriggers().containsKey("RefineToSuc") == true);
//		assert(pd.getPrematureTriggers().containsKey("CreateOrRefineCSC") == true);
//		
//		
//	}
//	
//	@Test
//	void testPrematureRuleGenerationWithSingleHopAcrossOutParam() throws IOException, ProcessException  {
//		Workspace ws = WorkspaceService.createWorkspace("test", WorkspaceService.PUBLIC_WORKSPACE, WorkspaceService.ANY_USER, null, false, false);
//		InstanceType typeGitDemo = TestArtifacts.getDemoGitIssueType(ws);
//		ws.concludeTransaction();
//		String path = ".";
//		String file = path+"/src/test/resources/prematuretestV2.json"; 
//		String content = Files.readString(Paths.get(file));	
//		DTOs.Process procD = json.fromJson(content);
//		ProcessRegistry preg = new ProcessRegistry();
//		preg.inject(ws, new ProcessConfigBaseElementType(ws));
//		ProcessDefinition pd = preg.storeProcessDefinition(procD, false).getKey();
//		//ProcessDefinition pd = DefinitionTransformer.fromDTO(procD, ws);
//		//new PrematureTriggerGenerator().generatePrematureConstraints(pd);
//		pd.getPrematureTriggers().entrySet().forEach(entry -> System.out.println(entry.getKey()+":\r\n"+entry.getValue()));
//		assert(pd.getPrematureTriggers().containsKey("WriteOrReviseMMF") == true);
//		assert(pd.getPrematureTriggers().containsKey("RefineToSuc") == true);
//		assert(pd.getPrematureTriggers().containsKey("CreateOrRefineCSC") == true);
//		
//		
//	}
//	
//	@Test
//	void testPrematureRuleGenerationWithMultiSource() throws IOException, ProcessException  {
//		Workspace ws = WorkspaceService.createWorkspace("test", WorkspaceService.PUBLIC_WORKSPACE, WorkspaceService.ANY_USER, null, false, false);
//		InstanceType typeDemo = TestArtifacts.getTestAzureIssueType(ws);
//		ws.concludeTransaction();
//		String path = ".";
//		String file = path+"/src/test/resources/prematuretestV3.json"; 
//		String content = Files.readString(Paths.get(file));	
//		DTOs.Process procD = json.fromJson(content);
//		ProcessRegistry preg = new ProcessRegistry();
//		preg.inject(ws, new ProcessConfigBaseElementType(ws));
//		ProcessDefinition pd = preg.storeProcessDefinition(procD, false).getKey();
////		ProcessDefinition pd = DefinitionTransformer.fromDTO(procD, ws);
////		new PrematureTriggerGenerator().generatePrematureConstraints(pd);
//		pd.getPrematureTriggers().entrySet().forEach(entry -> System.out.println(entry.getKey()+":\r\n"+entry.getValue()));
//		assert(pd.getPrematureTriggers().containsKey("ReviewFunctionSpecification") == true);
//		assert(pd.getPrematureTriggers().containsKey("CreateOrUpdateSRS") == true);
//		
//		
//	}
//	
//	@Test
//	void testPrematureRuleGenerationWithTypedIterator() throws IOException, ProcessException  {
//		Workspace ws = WorkspaceService.createWorkspace("test", WorkspaceService.PUBLIC_WORKSPACE, WorkspaceService.ANY_USER, null, false, false);
//		InstanceType typeDemo = TestArtifacts.getTestAzureIssueType(ws);
//		ws.concludeTransaction();
//		String path = ".";
//		String file = path+"/src/test/resources/prematuretestV4.json"; 
//		String content = Files.readString(Paths.get(file));	
//		DTOs.Process procD = json.fromJson(content);
//		ProcessRegistry preg = new ProcessRegistry();
//		preg.inject(ws, new ProcessConfigBaseElementType(ws));
//		ProcessDefinition pd = preg.storeProcessDefinition(procD, false).getKey();
////		ProcessDefinition pd = DefinitionTransformer.fromDTO(procD, ws);
////		new PrematureTriggerGenerator().generatePrematureConstraints(pd);
//		pd.getPrematureTriggers().entrySet().forEach(entry -> System.out.println(entry.getKey()+":\r\n"+entry.getValue()));
//		assert(pd.getPrematureTriggers().containsKey("ReviewFunctionSpecification") == true);
//	}
//	
//	@Test
//	void testPrematureRuleGenerationWithBranchesWithIdenticalRootSource() throws IOException, ProcessException  {
//		Workspace ws = WorkspaceService.createWorkspace("test", WorkspaceService.PUBLIC_WORKSPACE, WorkspaceService.ANY_USER, null, false, false);
//		InstanceType typeDemo = TestArtifacts.getTestAzureIssueType(ws);
//		ws.concludeTransaction();
//		String path = ".";
//		String file = path+"/src/test/resources/prematurebranchingtest.json"; 
//		String content = Files.readString(Paths.get(file));	
//		DTOs.Process procD = json.fromJson(content);
//		ProcessRegistry preg = new ProcessRegistry();
//		preg.inject(ws, new ProcessConfigBaseElementType(ws));
//		ProcessDefinition pd = preg.storeProcessDefinition(procD, false).getKey();
////		ProcessDefinition pd = DefinitionTransformer.fromDTO(procD, ws);
////		new PrematureTriggerGenerator().generatePrematureConstraints(pd);
//		pd.getPrematureTriggers().entrySet().forEach(entry -> System.out.println(entry.getKey()+":\r\n"+entry.getValue()));
//		assert(pd.getPrematureTriggers().containsKey("ReviewFunctionSpecification") == true);
//		assert(pd.getPrematureTriggers().get("ReviewFunctionSpecification").indexOf("union") > -1);
//	}
//	
//	@Test
//	void testPrematureRuleGenerationWithBranches() throws IOException, ProcessException  {
//		Workspace ws = WorkspaceService.createWorkspace("test", WorkspaceService.PUBLIC_WORKSPACE, WorkspaceService.ANY_USER, null, false, false);
//		InstanceType typeDemo = TestArtifacts.getTestAzureIssueType(ws);
//		ws.concludeTransaction();
//		String path = ".";
//		String file = path+"/src/test/resources/prematurebranchingtestV2.json"; 
//		String content = Files.readString(Paths.get(file));	
//		DTOs.Process procD = json.fromJson(content);
//		ProcessRegistry preg = new ProcessRegistry();
//		preg.inject(ws, new ProcessConfigBaseElementType(ws));
//		ProcessDefinition pd = preg.storeProcessDefinition(procD, false).getKey();
////		ProcessDefinition pd = DefinitionTransformer.fromDTO(procD, ws);
////		new PrematureTriggerGenerator().generatePrematureConstraints(pd);
//		pd.getPrematureTriggers().entrySet().forEach(entry -> System.out.println(entry.getKey()+":\r\n"+entry.getValue()));
//		assert(pd.getPrematureTriggers().containsKey("ReviewFunctionSpecification") == true);
//		assert(pd.getPrematureTriggers().get("ReviewFunctionSpecification").indexOf("union") > -1);
//	}
//}
package at;


