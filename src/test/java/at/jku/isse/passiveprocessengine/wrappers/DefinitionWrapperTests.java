package at.jku.isse.passiveprocessengine.wrappers;

import static org.junit.Assert.assertNull;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import at.jku.isse.passiveprocessengine.core.BuildInType;
import at.jku.isse.passiveprocessengine.core.ConfigurationBuilder;
import at.jku.isse.passiveprocessengine.core.DesignspaceTestSetup;
import at.jku.isse.passiveprocessengine.core.InstanceRepository;
import at.jku.isse.passiveprocessengine.core.PPEInstanceType;
import at.jku.isse.passiveprocessengine.core.PPEInstanceType.CARDINALITIES;
import at.jku.isse.passiveprocessengine.core.PPEInstanceType.PPEPropertyType;
import at.jku.isse.passiveprocessengine.core.RepairTreeProvider;
import at.jku.isse.passiveprocessengine.core.RuleEvaluationService;
import at.jku.isse.passiveprocessengine.core.SchemaRegistry;
import at.jku.isse.passiveprocessengine.definition.activeobjects.ConstraintSpec;
import at.jku.isse.passiveprocessengine.definition.activeobjects.DecisionNodeDefinition;
import at.jku.isse.passiveprocessengine.definition.activeobjects.MappingDefinition;
import at.jku.isse.passiveprocessengine.definition.activeobjects.ProcessDefinition;
import at.jku.isse.passiveprocessengine.definition.activeobjects.ProcessDefinitionScopedElement;
import at.jku.isse.passiveprocessengine.definition.activeobjects.StepDefinition;
import at.jku.isse.passiveprocessengine.definition.types.ConstraintSpecType;
import at.jku.isse.passiveprocessengine.definition.types.DecisionNodeDefinitionType;
import at.jku.isse.passiveprocessengine.definition.types.MappingDefinitionType;
import at.jku.isse.passiveprocessengine.definition.types.ProcessDefinitionScopeType;
import at.jku.isse.passiveprocessengine.definition.types.ProcessDefinitionType;
import at.jku.isse.passiveprocessengine.designspace.DesignspaceAbstractionMapper;
import at.jku.isse.passiveprocessengine.designspace.RewriterFactory;

@ExtendWith(SpringExtension.class)
@SpringBootTest
public class DefinitionWrapperTests {
	
	@Autowired
	protected DesignspaceTestSetup dsSetup;
	
	protected InstanceRepository instanceRepository;
	protected SchemaRegistry schemaReg;
	protected RepairTreeProvider ruleServiceWrapper;
	protected ConfigurationBuilder configBuilder;
	
	
	@BeforeEach
	public void setup() throws Exception {
		dsSetup.setup();
		this.schemaReg = dsSetup.getSchemaRegistry();
		this.instanceRepository = dsSetup.getInstanceRepository();
		this.ruleServiceWrapper = dsSetup.getRepairTreeProvider();			
		DesignspaceAbstractionMapper designspaceAbstractionMapper = (DesignspaceAbstractionMapper) schemaReg; // ugly as we know this is a DesignSpace in the background
		RuleEvaluationService ruleEvaluationFactory = dsSetup.getRuleEvaluationService(); 
		configBuilder = new ConfigurationBuilder(schemaReg, instanceRepository, ruleServiceWrapper, new RewriterFactory(designspaceAbstractionMapper), ruleEvaluationFactory, dsSetup.getCoreTypeFactory());
	}
	
	@AfterEach
	public void tearDown() {
		dsSetup.tearDown();
	}
	
	@Test
	void testBasicDefinitionTypeRegistration() {
		assert(schemaReg.getType(ConstraintSpec.class) != null);
	}
	
	@Test
	void testSuperType() {
		PPEInstanceType scopeType = schemaReg.getType(ProcessDefinitionScopedElement.class);
		PPEInstanceType specType = schemaReg.getType(ConstraintSpec.class);
		Set<PPEInstanceType> subtypes = scopeType.getAllSubtypesRecursively();
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
		PPEInstanceType type = schemaReg.getType(ConstraintSpec.class);
		PPEPropertyType propType = type.getPropertyType(ConstraintSpecType.CoreProperties.isOverridable.toString());
		assert(propType != null);
		assert(propType.getCardinality().equals(CARDINALITIES.SINGLE));
		assert(propType.getInstanceType().equals(BuildInType.BOOLEAN));
		
		PPEPropertyType propType2 = type.getPropertyType(ConstraintSpecType.CoreProperties.humanReadableDescription.toString());
		assert(propType2 != null);
		assert(propType2.getCardinality().equals(CARDINALITIES.SINGLE));
		assert(propType2.getInstanceType().equals(BuildInType.STRING));
		
		PPEPropertyType propType3 = type.getPropertyType(ConstraintSpecType.CoreProperties.ruleType.toString());
		assert(propType3 != null);
		assert(propType3.getCardinality().equals(CARDINALITIES.SINGLE));
		assert(propType3.getInstanceType().equals(BuildInType.RULE));	
	}
	
	@Test
	void testDataMappingTypePropertyGeneration() {
		PPEInstanceType type = schemaReg.getType(MappingDefinition.class);
		List.of(MappingDefinitionType.CoreProperties.values()).stream().forEach(prop -> {
			PPEPropertyType propType = type.getPropertyType(prop.toString());
			assert(propType != null);
			assert(propType.getCardinality().equals(CARDINALITIES.SINGLE));
			assert(propType.getInstanceType().equals(BuildInType.STRING));
		});
	}		
	
	@Test
	void testNonRegisteredType() {
		PPEInstanceType nonExistingType = schemaReg.getTypeByName("nonono");
		assertNull(nonExistingType);
	}
	
	@Test
	void testMapListSetPropertyGeneration() {
			
		PPEInstanceType type = schemaReg.getType(DecisionNodeDefinition.class);
		PPEInstanceType mappingType = schemaReg.getType(MappingDefinition.class);
		PPEInstanceType dndType = schemaReg.getType(DecisionNodeDefinition.class);
		PPEInstanceType stepType = schemaReg.getType(StepDefinition.class);
		PPEInstanceType processType = schemaReg.getType(ProcessDefinition.class);
		
		PPEPropertyType propType = type.getPropertyType(DecisionNodeDefinitionType.CoreProperties.dataMappingDefinitions.toString());
		assert(propType != null);
		assert(propType.getCardinality().equals(CARDINALITIES.SET));
		assert(propType.getInstanceType().equals(mappingType));
				
		PPEPropertyType inType = type.getPropertyType(DecisionNodeDefinitionType.CoreProperties.inSteps.toString());
		assert(inType != null);
		assert(inType.getCardinality().equals(CARDINALITIES.SET));
		assert(inType.getInstanceType().equals(stepType));
		
		PPEPropertyType procType = type.getPropertyType(ProcessDefinitionScopeType.CoreProperties.process.toString());
		assert(procType != null);
		assert(procType.getCardinality().equals(CARDINALITIES.SINGLE));
		PPEInstanceType procInstanceType = procType.getInstanceType(); 
		assert(procInstanceType.equals(processType));
		
		
		PPEPropertyType dndPropType = processType.getPropertyType(ProcessDefinitionType.CoreProperties.stepDefinitions.toString());
		assert(dndPropType != null);
		assert(dndPropType.getCardinality().equals(CARDINALITIES.LIST));
		assert(dndPropType.getInstanceType().equals(stepType));
		
		PPEPropertyType premPropType = processType.getPropertyType(ProcessDefinitionType.CoreProperties.prematureTriggers.toString());
		assert(premPropType != null);
		assert(premPropType.getCardinality().equals(CARDINALITIES.MAP));
		assert(premPropType.getInstanceType().equals(BuildInType.STRING));
		
		
	}
	

	
	
}
