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
	public void setup() {
		super.setup();	
	}	
	
	@Test
	void testAllBaseInstanceTypeRegistration() {				
		assert(schemaReg.findNonDeletedInstanceTypeByFQN(ProcessInstanceScopeType.typeId) != null);
		assert(schemaReg.findNonDeletedInstanceTypeByFQN(ConstraintWrapperType.typeId) != null);
		assert(schemaReg.findNonDeletedInstanceTypeByFQN(DecisionNodeInstanceType.typeId) != null);
		assert(schemaReg.findNonDeletedInstanceTypeByFQN(AbstractProcessStepType.typeId) != null);	
	}
	

	
}
