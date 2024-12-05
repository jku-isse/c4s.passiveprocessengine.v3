package at.jku.isse.passiveprocessengine.wrappers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import at.jku.isse.passiveprocessengine.instance.activeobjects.ConstraintResultWrapper;
import at.jku.isse.passiveprocessengine.instance.activeobjects.DecisionNodeInstance;
import at.jku.isse.passiveprocessengine.instance.activeobjects.ProcessInstanceScopedElement;
import at.jku.isse.passiveprocessengine.instance.activeobjects.ProcessStep;
import at.jku.isse.passiveprocessengine.instance.types.AbstractProcessStepType;
import at.jku.isse.passiveprocessengine.instance.types.ConstraintWrapperType;
import at.jku.isse.passiveprocessengine.instance.types.DecisionNodeInstanceType;
import at.jku.isse.passiveprocessengine.instance.types.ProcessInstanceScopeType;

@ExtendWith(SpringExtension.class)
@SpringBootTest
public class InstanceWrapperTests extends DefinitionWrapperTests {
	 
	@BeforeEach
	public void setup() throws Exception {
		super.setup();	
	}	
	
	@Test
	void testAllBaseInstanceTypeRegistration() {				
		assert(schemaReg.getTypeByName(ProcessInstanceScopeType.typeId) != null);
		assert(schemaReg.getTypeByName(ConstraintWrapperType.typeId) != null);
		assert(schemaReg.getTypeByName(DecisionNodeInstanceType.typeId) != null);
		assert(schemaReg.getTypeByName(AbstractProcessStepType.typeId) != null);	
	}
	

	
}
