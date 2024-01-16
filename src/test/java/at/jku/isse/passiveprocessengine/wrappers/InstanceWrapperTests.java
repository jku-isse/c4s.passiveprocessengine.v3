package at.jku.isse.passiveprocessengine.wrappers;

import static org.junit.Assert.assertNotNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import at.jku.isse.passiveprocessengine.definition.activeobjects.ProcessDefinition;
import at.jku.isse.passiveprocessengine.definition.serialization.DTOs;
import at.jku.isse.passiveprocessengine.definition.serialization.DefinitionTransformer;
import at.jku.isse.passiveprocessengine.demo.TestArtifacts;
import at.jku.isse.passiveprocessengine.demo.TestDTOProcesses;
import at.jku.isse.passiveprocessengine.instance.activeobjects.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
public class InstanceWrapperTests extends DefinitionWrapperTests {
	 
	TestDTOProcesses procFactory;
	
	@BeforeEach
	void setup() throws Exception {
		super.setup();
		TestArtifacts artifactFactory = new TestArtifacts(designspace, schemaReg);
		procFactory = new TestDTOProcesses(artifactFactory);
	}
	

	
	@Test
	void testAllBaseInstanceTypeRegistration() {				
		assert(schemaReg.getType(ProcessInstanceScopedElement.class) != null);
		assert(schemaReg.getType(ConstraintResultWrapper.class) != null);
		assert(schemaReg.getType(DecisionNodeInstance.class) != null);
		assert(schemaReg.getType(ProcessStep.class) != null);	
	}
	
	@Test
	void testObtainSimpleProcess() {
		DTOs.Process procDTO = procFactory.getSimpleDTOSubprocess();
		DefinitionTransformer transformer = new DefinitionTransformer(procDTO, configBuilder.getContext().getFactoryIndex(), schemaReg);
		ProcessDefinition procDef = transformer.fromDTO(false);
		assertNotNull(procDef);
		transformer.getErrors().stream().forEach(err -> System.out.println(err.toString()));		
		assert(transformer.getErrors().isEmpty());
	}
	
	

	
	
}
