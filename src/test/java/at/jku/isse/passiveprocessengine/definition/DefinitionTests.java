package at.jku.isse.passiveprocessengine.definition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import at.jku.isse.passiveprocessengine.core.PPEInstanceType;
import at.jku.isse.passiveprocessengine.definition.activeobjects.DecisionNodeDefinition;
import at.jku.isse.passiveprocessengine.definition.activeobjects.ProcessDefinition;
import at.jku.isse.passiveprocessengine.definition.activeobjects.StepDefinition;
import at.jku.isse.passiveprocessengine.definition.serialization.DTOs;
import at.jku.isse.passiveprocessengine.definition.serialization.DefinitionTransformer;
import at.jku.isse.passiveprocessengine.definition.serialization.JsonDefinitionSerializer;
import at.jku.isse.passiveprocessengine.demo.TestArtifacts;
import at.jku.isse.passiveprocessengine.demo.TestDTOProcesses;
import at.jku.isse.passiveprocessengine.wrappers.InstanceWrapperTests;

@ExtendWith(SpringExtension.class)
@SpringBootTest
class DefinitionTests extends InstanceWrapperTests {

	TestDTOProcesses procFactory;
	DTOs.Process procDTO;
	TestArtifacts artifactFactory;
	ProcessDefinition procDef;
	DefinitionTransformer transformer;
	static JsonDefinitionSerializer json = new JsonDefinitionSerializer();
	
	@Override
	@BeforeEach
	public
	void setup() {
		super.setup();
		artifactFactory = new TestArtifacts(instanceRepository, schemaReg);
		procFactory = new TestDTOProcesses(artifactFactory);
		procDTO = procFactory.getSimpleDTOSubprocess();
		procDTO.calculateDecisionNodeDepthIndex(1);
		transformer = new DefinitionTransformer(procDTO, configBuilder.getContext().getFactoryIndex(), schemaReg);
		procDef = transformer.fromDTO(false);
	}

	@Test
	void testObtainSimpleProcess() {		
		assertNotNull(procDef);
		transformer.getErrors().stream().forEach(err -> System.out.println(err.toString()));		
		assert(transformer.getErrors().isEmpty());
	}
	
	
	@Test
	void testSimpleProcContent() {
		PPEInstanceType typeJira = artifactFactory.getJiraInstanceType();
						
		assert(typeJira != null);
		assert(procDef.getExpectedInput().get("jiraIn").equals(typeJira));
		
		StepDefinition sd1 = procDef.getStepDefinitionByName("subtask1");
		StepDefinition sd2 = procDef.getStepDefinitionByName("subtask2");
		assert(sd1 != null);
		assert(sd2 != null);
		assertTrue(sd1.getExpectedInput().get("jiraIn").equals(typeJira));
		assertTrue(sd1.getExpectedOutput().get("jiraOut").equals(typeJira));
		assertTrue(sd2.getExpectedInput().get("jiraIn").equals(typeJira));
				
		assertTrue(procDef.getDecisionNodeDefinitions().size() == 2);
		assertTrue(procDef.getStepDefinitions().size() == 2);
		assertTrue(sd1.getPreconditions().size() > 0);
		
		DecisionNodeDefinition dnd1 = procDef.getDecisionNodeDefinitionByName("dndSubStart");
		
		DecisionNodeDefinition dnd2 = procDef.getDecisionNodeDefinitionByName("dndSubEnd");		
		assertNotNull(dnd1);
		assertNotNull(dnd2);
		assertTrue(procDef.getDecisionNodeDefinitions().contains(dnd2));
		assertTrue(procDef.getDecisionNodeDefinitions().contains(dnd1));
		
		DecisionNodeDefinition sd1In = sd1.getInDND();
		assertTrue(sd1.getInDND().equals(dnd1));
		assertTrue(sd1.getOutDND().equals(dnd2));
		assertTrue(sd2.getInDND().equals(dnd1));
		assertTrue(sd2.getOutDND().equals(dnd2));
		assertTrue(sd1.getInputToOutputMappingRules().containsKey("jiraOut"));
		
		assertTrue(dnd1.getMappings().size() == 2);
		assertTrue(dnd2.getMappings().size() == 1);
	}

//	@Test 
//	void testFromDTOandDefToJson() {
//		DTOs.Process dtoFromDef = DefinitionTransformer.toDTO(procDef);
//		String jsonFromDirectDTO = json.toJson(procDTO);
//		String jsonFromDef = json.toJson(dtoFromDef);
//		System.out.println(jsonFromDirectDTO);
//		System.out.println("----------------------------------------");
//		System.out.println(jsonFromDef);
//		assertTrue(jsonFromDirectDTO.equalsIgnoreCase(jsonFromDef));
//	}
	
	@Test
	void testSimpleProcContentFromJson() {
		PPEInstanceType typeJira = artifactFactory.getJiraInstanceType();
		procDTO.setCode("ProcToJson");		
		DTOs.DecisionNode dn1 = procDTO.getDecisionNodeByCode("dndSubStart");
		dn1.getMapping().clear();		
		DTOs.DecisionNode dn2 = procDTO.getDecisionNodeByCode("dndSubEnd");
		dn2.getMapping().clear();
		dn1.getMapping().add(new DTOs.Mapping(procDTO.getCode(), "jiraIn", "subtask1", "jiraIn")); //into both steps
		dn1.getMapping().add(new DTOs.Mapping(procDTO.getCode(), "jiraIn", "subtask2", "jiraIn")); //into both steps
		dn2.getMapping().add(new DTOs.Mapping("subtask1", "jiraOut", procDTO.getCode(), "jiraOut")); //out of the first
		
		String jsonFromDirectDTO = json.toJson(procDTO);
		DTOs.Process procDTOfromJson = json.fromJson(jsonFromDirectDTO);
		procDef.deleteCascading();
		transformer = new DefinitionTransformer(procDTOfromJson, configBuilder.getContext().getFactoryIndex(), schemaReg);
		procDef = transformer.fromDTO(false);
		
		assert(typeJira != null);
		assert(procDef.getExpectedInput().get("jiraIn").equals(typeJira));
		
		StepDefinition sd1 = procDef.getStepDefinitionByName("subtask1");
		StepDefinition sd2 = procDef.getStepDefinitionByName("subtask2");
		assert(sd1 != null);
		assert(sd2 != null);
		assertTrue(sd1.getExpectedInput().get("jiraIn").equals(typeJira));
		assertTrue(sd1.getExpectedOutput().get("jiraOut").equals(typeJira));
		assertTrue(sd2.getExpectedInput().get("jiraIn").equals(typeJira));
				
		assertTrue(procDef.getDecisionNodeDefinitions().size() == 2);
		assertTrue(procDef.getStepDefinitions().size() == 2);
		assertTrue(sd1.getPreconditions().size() > 0);
		
		DecisionNodeDefinition dnd1 = procDef.getDecisionNodeDefinitionByName("dndSubStart");
		DecisionNodeDefinition dnd2 = procDef.getDecisionNodeDefinitionByName("dndSubEnd");
		assertNotNull(dnd1);
		assertNotNull(dnd2);
		assertTrue(sd1.getInDND().equals(dnd1));
		assertTrue(sd1.getOutDND().equals(dnd2));
		assertTrue(sd2.getInDND().equals(dnd1));
		assertTrue(sd2.getOutDND().equals(dnd2));
		assertTrue(sd1.getInputToOutputMappingRules().containsKey("jiraOut"));
		
		assertEquals(2, dnd1.getMappings().size());
		assertEquals(1, dnd2.getMappings().size());
	}
	
	@Test
	void testHierarchyIndex() throws Exception {
		
		procDef.getDecisionNodeDefinitions().stream().forEach(dnd -> System.out.println(dnd.getName()+" "+dnd.getDepthIndex()));
		procDef.getStepDefinitions().stream().forEach(step -> System.out.println(step.getName()+" "+step.getDepthIndex()));
		assert(procDef.getDecisionNodeDefinitions().stream().allMatch(dnd -> dnd.getDepthIndex() == 1));
		assert(procDef.getStepDefinitions().stream().allMatch(step -> step.getDepthIndex() == 2));
	}

}
