package at.jku.isse.passiveprocessengine.wrappers;

import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import at.jku.isse.passiveprocessengine.core.BuildInType;
import at.jku.isse.passiveprocessengine.core.ProcessEngineConfigurationBuilder;
import at.jku.isse.passiveprocessengine.core.DesignspaceTestSetup;
import at.jku.isse.passiveprocessengine.core.InstanceRepository;
import at.jku.isse.passiveprocessengine.core.PPEInstanceType;
import at.jku.isse.passiveprocessengine.core.PPEInstanceType.CARDINALITIES;
import at.jku.isse.passiveprocessengine.core.PPEInstanceType.PPEPropertyType;
import at.jku.isse.passiveprocessengine.core.RepairTreeProvider;
import at.jku.isse.passiveprocessengine.core.RuleAnalysisService;
import at.jku.isse.passiveprocessengine.core.RuleEvaluationService;
import at.jku.isse.passiveprocessengine.core.SchemaRegistry;
import at.jku.isse.passiveprocessengine.definition.types.ConstraintSpecType;
import at.jku.isse.passiveprocessengine.definition.types.DecisionNodeDefinitionType;
import at.jku.isse.passiveprocessengine.definition.types.MappingDefinitionType;
import at.jku.isse.passiveprocessengine.definition.types.ProcessDefinitionScopeType;
import at.jku.isse.passiveprocessengine.definition.types.ProcessDefinitionType;
import at.jku.isse.passiveprocessengine.definition.types.ProcessStepDefinitionType;
import at.jku.isse.passiveprocessengine.designspace.RewriterFactory;
import at.jku.isse.passiveprocessengine.rdfwrapper.AbstractionMapper;
import at.jku.isse.passiveprocessengine.rdfwrapper.RDFWrapperTestSetup;

//@ExtendWith(SpringExtension.class)
//@SpringBootTest
public class DefinitionWrapperTests {
	
	//@Autowired
	protected DesignspaceTestSetup dsSetup;
	
	protected InstanceRepository instanceRepository;
	protected SchemaRegistry schemaReg;
	protected RepairTreeProvider ruleServiceWrapper;
	protected ProcessEngineConfigurationBuilder configBuilder;
	
	
	@BeforeEach
	public void setup() {
		dsSetup = new RDFWrapperTestSetup();
		dsSetup.setup();
		this.schemaReg = dsSetup.getSchemaRegistry();
		this.instanceRepository = dsSetup.getInstanceRepository();
		this.ruleServiceWrapper = dsSetup.getRepairTreeProvider();			
		AbstractionMapper designspaceAbstractionMapper = (AbstractionMapper) schemaReg; // ugly as we know this is a DesignSpace in the background
		RuleEvaluationService ruleEvaluationFactory = dsSetup.getRuleEvaluationService(); 
		configBuilder = new ProcessEngineConfigurationBuilder(schemaReg
				, instanceRepository
				, ruleServiceWrapper
				, new RewriterFactory(designspaceAbstractionMapper, false, ((RDFWrapperTestSetup) dsSetup).getRuleSchemaProvider())
				, ruleEvaluationFactory
				, dsSetup.getCoreTypeFactory()
				, (RuleAnalysisService) ruleServiceWrapper);
	}
	
	@AfterEach
	public void tearDown() {
		dsSetup.tearDown();
	}
	
	@Test
	void testBasicDefinitionTypeRegistration() {
		assertTrue(schemaReg.getTypeByName(ConstraintSpecType.typeId) != null);
	}
	
	@Test
	void testSuperType() {
		PPEInstanceType scopeType = schemaReg.getTypeByName(ProcessDefinitionScopeType.typeId);
		PPEInstanceType specType = schemaReg.getTypeByName(ConstraintSpecType.typeId);
		Set<PPEInstanceType> subtypes = scopeType.getAllSubtypesRecursively();
		assertTrue(subtypes != null);
		assertTrue(subtypes.contains(specType));
		assertTrue(specType.isOfTypeOrAnySubtype(scopeType));
	}
	
	@Test
	void testAllDefinitionsTypeRegistration() {				
		assertTrue(schemaReg.getTypeByName(ConstraintSpecType.typeId) != null);
		System.out.println(schemaReg.getTypeByName(ConstraintSpecType.typeId).getName());
		assertTrue(schemaReg.getTypeByName(MappingDefinitionType.typeId) != null);
		System.out.println(schemaReg.getTypeByName(MappingDefinitionType.typeId).getName());
		assertTrue(schemaReg.getTypeByName(DecisionNodeDefinitionType.typeId) != null);
		System.out.println(schemaReg.getTypeByName(DecisionNodeDefinitionType.typeId).getName());
		assertTrue(schemaReg.getTypeByName(ProcessStepDefinitionType.typeId) != null);
		System.out.println(schemaReg.getTypeByName(ProcessStepDefinitionType.typeId).getName());
		assertTrue(schemaReg.getTypeByName(ProcessDefinitionType.typeId) != null);
		System.out.println(schemaReg.getTypeByName(ProcessDefinitionType.typeId).getName());		
	}
	
	
	@Test
	void testSingleTypePropertyGeneration() {				
		PPEInstanceType type = schemaReg.getTypeByName(ConstraintSpecType.typeId);
		PPEPropertyType propType = type.getPropertyType(ConstraintSpecType.CoreProperties.isOverridable.toString());
		assertTrue(propType != null);
		assertTrue(propType.getCardinality().equals(CARDINALITIES.SINGLE));
		assertTrue(propType.getInstanceType().equals(BuildInType.BOOLEAN));
		
		PPEPropertyType propType2 = type.getPropertyType(ConstraintSpecType.CoreProperties.humanReadableDescription.toString());
		assertTrue(propType2 != null);
		assertTrue(propType2.getCardinality().equals(CARDINALITIES.SINGLE));
		assertTrue(propType2.getInstanceType().equals(BuildInType.STRING));
		
		PPEPropertyType propType3 = type.getPropertyType(ConstraintSpecType.CoreProperties.ruleType.toString());
		assertTrue(propType3 != null);
		assertTrue(propType3.getCardinality().equals(CARDINALITIES.SINGLE));
		assertTrue(propType3.getInstanceType().equals(BuildInType.RULE));	
	}
	
	@Test
	void testDataMappingTypePropertyGeneration() {
		PPEInstanceType type = schemaReg.getTypeByName(MappingDefinitionType.typeId);
		List.of(MappingDefinitionType.CoreProperties.values()).stream().forEach(prop -> {
			PPEPropertyType propType = type.getPropertyType(prop.toString());
			assertTrue(propType != null);
			assertTrue(propType.getCardinality().equals(CARDINALITIES.SINGLE));
			assertTrue(propType.getInstanceType().equals(BuildInType.STRING));
		});
	}		
	
	@Test
	void testNonRegisteredType() {
		PPEInstanceType nonExistingType = schemaReg.getTypeByName("nonono");
		Assertions.assertNull(nonExistingType);
	}
	
	@Test
	void testMapListSetPropertyGeneration() {
			
		PPEInstanceType type = schemaReg.getTypeByName(DecisionNodeDefinitionType.typeId);
		PPEInstanceType mappingType = schemaReg.getTypeByName(MappingDefinitionType.typeId);
		PPEInstanceType dndType = schemaReg.getTypeByName(DecisionNodeDefinitionType.typeId);
		PPEInstanceType stepType = schemaReg.getTypeByName(ProcessStepDefinitionType.typeId);
		PPEInstanceType processType = schemaReg.getTypeByName(ProcessDefinitionType.typeId);
		
		PPEPropertyType propType = type.getPropertyType(DecisionNodeDefinitionType.CoreProperties.dataMappingDefinitions.toString());
		assertTrue(propType != null);
		assertTrue(propType.getCardinality().equals(CARDINALITIES.SET));
		assertTrue(propType.getInstanceType().equals(mappingType));
				
		PPEPropertyType inType = type.getPropertyType(DecisionNodeDefinitionType.CoreProperties.inSteps.toString());
		assertTrue(inType != null);
		assertTrue(inType.getCardinality().equals(CARDINALITIES.SET));
		assertTrue(inType.getInstanceType().equals(stepType));
		
		PPEPropertyType procType = type.getPropertyType(ProcessDefinitionScopeType.CoreProperties.processDefinition.toString());
		assertTrue(procType != null);
		assertTrue(procType.getCardinality().equals(CARDINALITIES.SINGLE));
		PPEInstanceType procInstanceType = procType.getInstanceType(); 
		assertTrue(procInstanceType.equals(processType));
		
		
		PPEPropertyType dndPropType = processType.getPropertyType(ProcessDefinitionType.CoreProperties.stepDefinitions.toString());
		assertTrue(dndPropType != null);
		assertTrue(dndPropType.getCardinality().equals(CARDINALITIES.LIST));
		assertTrue(dndPropType.getInstanceType().equals(stepType));
		
		PPEPropertyType premPropType = processType.getPropertyType(ProcessDefinitionType.CoreProperties.prematureTriggers.toString());
		assertTrue(premPropType != null);
		assertTrue(premPropType.getCardinality().equals(CARDINALITIES.MAP));
		assertTrue(premPropType.getInstanceType().equals(BuildInType.STRING));
		
		
	}
	

	
	
}
