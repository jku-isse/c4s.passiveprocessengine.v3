package at.jku.isse.passiveprocessengine.wrappers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import at.jku.isse.passiveprocessengine.instance.activeobjects.*;
import at.jku.isse.passiveprocessengine.instance.types.AbstractProcessStepType;
import at.jku.isse.passiveprocessengine.instance.types.ConstraintWrapperType;
import at.jku.isse.passiveprocessengine.instance.types.DecisionNodeInstanceType;
import at.jku.isse.passiveprocessengine.instance.types.ProcessInstanceScopeType;

@ExtendWith(SpringExtension.class)
@SpringBootTest
public class InstanceWrapperTests extends DefinitionWrapperTests {
	
	
	@BeforeEach
	void setup() throws Exception {
		super.setup();
		registerAllInstanceBaseTypes();
	}
	
	private void registerAllInstanceBaseTypes() {
			ProcessInstanceScopeType scopeTypeProvider = new ProcessInstanceScopeType(schemaReg);
			ConstraintWrapperType constraintWrapperType = new ConstraintWrapperType(schemaReg);
			DecisionNodeInstanceType dniType = new DecisionNodeInstanceType(schemaReg);
			AbstractProcessStepType stepType = new AbstractProcessStepType(schemaReg);
			scopeTypeProvider.produceTypeProperties();
			constraintWrapperType.produceTypeProperties();
			dniType.produceTypeProperties();
			stepType.produceTypeProperties();
	}
	
	@Test
	void testAllBaseInstanceTypeRegistration() {				
		assert(schemaReg.getType(ProcessInstanceScopedElement.class) != null);
		assert(schemaReg.getType(ConstraintResultWrapper.class) != null);
		assert(schemaReg.getType(DecisionNodeInstance.class) != null);
		assert(schemaReg.getType(ProcessStep.class) != null);	
	}
	
	
	
	

	
	
}
