package at.jku.isse.passiveprocessengine.wrappers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import at.jku.isse.passiveprocessengine.instance.activeobjects.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
public class InstanceWrapperTests extends DefinitionWrapperTests {
	 
	@BeforeEach
	protected
	void setup() throws Exception {
		super.setup();	
	}	
	
	@Test
	void testAllBaseInstanceTypeRegistration() {				
		assert(schemaReg.getType(ProcessInstanceScopedElement.class) != null);
		assert(schemaReg.getType(ConstraintResultWrapper.class) != null);
		assert(schemaReg.getType(DecisionNodeInstance.class) != null);
		assert(schemaReg.getType(ProcessStep.class) != null);	
	}
	

	
}
