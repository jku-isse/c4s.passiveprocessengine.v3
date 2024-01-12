package at.jku.isse.passiveprocessengine.wrappers;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import at.jku.isse.designspace.core.model.Tool;
import at.jku.isse.designspace.core.model.User;
import at.jku.isse.designspace.core.model.Workspace;
import at.jku.isse.designspace.core.service.WorkspaceService;
import at.jku.isse.passiveprocessengine.core.BuildInType;
import at.jku.isse.passiveprocessengine.core.DomainTypesRegistry;
import at.jku.isse.passiveprocessengine.core.InstanceType;
import at.jku.isse.passiveprocessengine.core.InstanceType.CARDINALITIES;
import at.jku.isse.passiveprocessengine.core.InstanceType.PropertyType;
import at.jku.isse.passiveprocessengine.core.SchemaRegistry;
import at.jku.isse.passiveprocessengine.designspace.DesignSpaceSchemaRegistry;
import at.jku.isse.passiveprocessengine.definition.types.*;
import at.jku.isse.passiveprocessengine.definition.activeobjects.*;
import at.jku.isse.passiveprocessengine.definition.factories.*;
import at.jku.isse.passiveprocessengine.instance.activeobjects.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
public class DefinitionWrapperTests {
	
	protected SchemaRegistry schemaReg;	
	
	@BeforeEach
	void setup() throws Exception {
		Workspace testWS = WorkspaceService.createWorkspace("test", WorkspaceService.PUBLIC_WORKSPACE, new User("Test"), new Tool("test", "v1.0"), false, false);					
		schemaReg = new DesignSpaceSchemaRegistry(testWS);			
		assert(schemaReg != null);		
		registerAllDefinitionTypes();
	}
	
	protected void registerAllDefinitionTypes() {
		ProcessDefinitionScopeType scopeTypeProvider = new ProcessDefinitionScopeType(schemaReg);		
		ConstraintSpecType specTypeProvider = new ConstraintSpecType(schemaReg);		
		MappingDefinitionType mapTypeProvider = new MappingDefinitionType(schemaReg);		
		DecisionNodeDefinitionType dndTypeProvider = new DecisionNodeDefinitionType(schemaReg);		
		ProcessStepDefinitionType stepTypeProvider = new ProcessStepDefinitionType(schemaReg);		
		ProcessDefinitionType processTypeProvider = new ProcessDefinitionType(schemaReg);
		scopeTypeProvider.produceTypeProperties();
		specTypeProvider.produceTypeProperties();
		mapTypeProvider.produceTypeProperties();
		dndTypeProvider.produceTypeProperties();
		processTypeProvider.produceTypeProperties();
		stepTypeProvider.produceTypeProperties();		
	}
	
	
	@Test
	void testBasicDefinitionTypeRegistration() {
		assert(schemaReg.getType(ConstraintSpec.class) != null);
	}
	
	@Test
	void testSuperType() {
		InstanceType scopeType = schemaReg.getType(ProcessDefinitionScopedElement.class);
		InstanceType specType = schemaReg.getType(ConstraintSpec.class);
		Set<InstanceType> subtypes = scopeType.getAllSubtypesRecursively();
		assert(subtypes != null);
		assert(subtypes.contains(specType));
		assert(specType.isOfTypeOrAnySubtype(scopeType));
	}
	
	@Test
	void testAllDefinitionsTypeRegistration() {				
		assert(schemaReg.getType(ConstraintSpec.class) != null);
		System.out.println(schemaReg.getType(ConstraintSpec.class).getName());
		assert(schemaReg.getType(MappingDefinition.class) != null);
		System.out.println(schemaReg.getType(MappingDefinition.class).getName());
		assert(schemaReg.getType(DecisionNodeDefinition.class) != null);
		System.out.println(schemaReg.getType(DecisionNodeDefinition.class).getName());
		assert(schemaReg.getType(StepDefinition.class) != null);
		System.out.println(schemaReg.getType(StepDefinition.class).getName());
		assert(schemaReg.getType(ProcessDefinition.class) != null);
		System.out.println(schemaReg.getType(ProcessDefinition.class).getName());		
	}
	
	
	@Test
	void testSingleTypePropertyGeneration() {				
		InstanceType type = schemaReg.getType(ConstraintSpec.class);
		PropertyType propType = type.getPropertyType(ConstraintSpecType.CoreProperties.isOverridable.toString());
		assert(propType != null);
		assert(propType.getCardinality().equals(CARDINALITIES.SINGLE));
		assert(propType.getInstanceType().equals(BuildInType.BOOLEAN));
		
		PropertyType propType2 = type.getPropertyType(ConstraintSpecType.CoreProperties.humanReadableDescription.toString());
		assert(propType2 != null);
		assert(propType2.getCardinality().equals(CARDINALITIES.SINGLE));
		assert(propType2.getInstanceType().equals(BuildInType.STRING));
		
		PropertyType propType3 = type.getPropertyType(ConstraintSpecType.CoreProperties.ruleType.toString());
		assert(propType3 != null);
		assert(propType3.getCardinality().equals(CARDINALITIES.SINGLE));
		assert(propType3.getInstanceType().equals(BuildInType.RULE));	
	}
	
	@Test
	void testDataMappingTypePropertyGeneration() {
		InstanceType type = schemaReg.getType(MappingDefinition.class);
		List.of(MappingDefinitionType.CoreProperties.values()).stream().forEach(prop -> {
			PropertyType propType = type.getPropertyType(prop.toString());
			assert(propType != null);
			assert(propType.getCardinality().equals(CARDINALITIES.SINGLE));
			assert(propType.getInstanceType().equals(BuildInType.STRING));
		});
	}		
	
	@Test
	void testNonRegisteredType() {
		InstanceType type = schemaReg.getType(ConstraintResultWrapper.class);
		assertNull(type); // as we haven't registered it yet.
		
		InstanceType nonExistingType = schemaReg.getTypeByName("nonono");
		assertNull(nonExistingType);
	}
	
	@Test
	void testMapListSetPropertyGeneration() {
			
		InstanceType type = schemaReg.getType(DecisionNodeDefinition.class);
		InstanceType mappingType = schemaReg.getType(MappingDefinition.class);
		InstanceType dndType = schemaReg.getType(DecisionNodeDefinition.class);
		InstanceType stepType = schemaReg.getType(StepDefinition.class);
		InstanceType processType = schemaReg.getType(ProcessDefinition.class);
		
		PropertyType propType = type.getPropertyType(DecisionNodeDefinitionType.CoreProperties.dataMappingDefinitions.toString());
		assert(propType != null);
		assert(propType.getCardinality().equals(CARDINALITIES.SET));
		assert(propType.getInstanceType().equals(mappingType));
				
		PropertyType inType = type.getPropertyType(DecisionNodeDefinitionType.CoreProperties.inSteps.toString());
		assert(inType != null);
		assert(inType.getCardinality().equals(CARDINALITIES.SET));
		assert(inType.getInstanceType().equals(stepType));
		
		PropertyType procType = type.getPropertyType(ProcessDefinitionScopeType.CoreProperties.process.toString());
		assert(procType != null);
		assert(procType.getCardinality().equals(CARDINALITIES.SINGLE));
		InstanceType procInstanceType = procType.getInstanceType(); 
		assert(procInstanceType.equals(processType));
		
		
		PropertyType dndPropType = processType.getPropertyType(ProcessDefinitionType.CoreProperties.stepDefinitions.toString());
		assert(dndPropType != null);
		assert(dndPropType.getCardinality().equals(CARDINALITIES.LIST));
		assert(dndPropType.getInstanceType().equals(stepType));
		
		PropertyType premPropType = processType.getPropertyType(ProcessDefinitionType.CoreProperties.prematureTriggers.toString());
		assert(premPropType != null);
		assert(premPropType.getCardinality().equals(CARDINALITIES.MAP));
		assert(premPropType.getInstanceType().equals(BuildInType.STRING));
		
		
	}
	

	
	
}
