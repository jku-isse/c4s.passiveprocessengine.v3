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
import at.jku.isse.passiveprocessengine.rdfwrapper.RDFInstanceType;
import at.jku.isse.passiveprocessengine.rdfwrapper.RDFInstanceType.Cardinalities;
import at.jku.isse.passiveprocessengine.rdfwrapper.RDFInstanceType.PPEPropertyType;
import at.jku.isse.passiveprocessengine.core.RepairTreeProvider;
import at.jku.isse.passiveprocessengine.core.RuleAnalysisService;
import at.jku.isse.passiveprocessengine.core.NodeToDomainResolver;
import at.jku.isse.passiveprocessengine.definition.types.ConstraintSpecTypeFactory;
import at.jku.isse.passiveprocessengine.definition.types.DecisionNodeDefinitionType;
import at.jku.isse.passiveprocessengine.definition.types.MappingDefinitionTypeFactory;
import at.jku.isse.passiveprocessengine.definition.types.ProcessDefinitionScopeType;
import at.jku.isse.passiveprocessengine.definition.types.ProcessDefinitionType;
import at.jku.isse.passiveprocessengine.definition.types.StepDefinitionTypeFactory;
import at.jku.isse.passiveprocessengine.designspace.RewriterFactory;
import at.jku.isse.passiveprocessengine.rdfwrapper.AbstractionMapper;
import at.jku.isse.passiveprocessengine.rdfwrapper.config.RDFWrapperTestSetup;
import at.jku.isse.passiveprocessengine.rdfwrapper.rule.RuleEvaluationService;

//@ExtendWith(SpringExtension.class)
//@SpringBootTest
public class DefinitionWrapperTests {
	
	//@Autowired
	protected DesignspaceTestSetup dsSetup;
	
	protected InstanceRepository instanceRepository;
	protected NodeToDomainResolver schemaReg;
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
		assertTrue(schemaReg.findNonDeletedInstanceTypeByFQN(ConstraintSpecTypeFactory.typeId) != null);
	}
	
	@Test
	void testSuperType() {
		RDFInstanceType scopeType = schemaReg.findNonDeletedInstanceTypeByFQN(ProcessDefinitionScopeType.typeId);
		RDFInstanceType specType = schemaReg.findNonDeletedInstanceTypeByFQN(ConstraintSpecTypeFactory.typeId);
		Set<RDFInstanceType> subtypes = scopeType.getAllSubtypesRecursively();
		assertTrue(subtypes != null);
		assertTrue(subtypes.contains(specType));
		assertTrue(specType.isOfTypeOrAnySubtype(scopeType));
	}
	
	@Test
	void testAllDefinitionsTypeRegistration() {				
		assertTrue(schemaReg.findNonDeletedInstanceTypeByFQN(ConstraintSpecTypeFactory.typeId) != null);
		System.out.println(schemaReg.findNonDeletedInstanceTypeByFQN(ConstraintSpecTypeFactory.typeId).getName());
		assertTrue(schemaReg.findNonDeletedInstanceTypeByFQN(MappingDefinitionTypeFactory.typeId) != null);
		System.out.println(schemaReg.findNonDeletedInstanceTypeByFQN(MappingDefinitionTypeFactory.typeId).getName());
		assertTrue(schemaReg.findNonDeletedInstanceTypeByFQN(DecisionNodeDefinitionType.typeId) != null);
		System.out.println(schemaReg.findNonDeletedInstanceTypeByFQN(DecisionNodeDefinitionType.typeId).getName());
		assertTrue(schemaReg.findNonDeletedInstanceTypeByFQN(StepDefinitionTypeFactory.typeId) != null);
		System.out.println(schemaReg.findNonDeletedInstanceTypeByFQN(StepDefinitionTypeFactory.typeId).getName());
		assertTrue(schemaReg.findNonDeletedInstanceTypeByFQN(ProcessDefinitionType.typeId) != null);
		System.out.println(schemaReg.findNonDeletedInstanceTypeByFQN(ProcessDefinitionType.typeId).getName());		
	}
	
	
	@Test
	void testSingleTypePropertyGeneration() {				
		RDFInstanceType type = schemaReg.findNonDeletedInstanceTypeByFQN(ConstraintSpecTypeFactory.typeId);
		PPEPropertyType propType = type.getPropertyType(ConstraintSpecTypeFactory.CoreProperties.isOverridable.toString());
		assertTrue(propType != null);
		assertTrue(propType.getCardinality().equals(Cardinalities.SINGLE));
		assertTrue(propType.getInstanceType().equals(BuildInType.BOOLEAN));
		
		PPEPropertyType propType2 = type.getPropertyType(ConstraintSpecTypeFactory.CoreProperties.humanReadableDescription.toString());
		assertTrue(propType2 != null);
		assertTrue(propType2.getCardinality().equals(Cardinalities.SINGLE));
		assertTrue(propType2.getInstanceType().equals(BuildInType.STRING));
		
		PPEPropertyType propType3 = type.getPropertyType(ConstraintSpecTypeFactory.CoreProperties.ruleType.toString());
		assertTrue(propType3 != null);
		assertTrue(propType3.getCardinality().equals(Cardinalities.SINGLE));
		assertTrue(propType3.getInstanceType().equals(BuildInType.RULE));	
	}
	
	@Test
	void testDataMappingTypePropertyGeneration() {
		RDFInstanceType type = schemaReg.findNonDeletedInstanceTypeByFQN(MappingDefinitionTypeFactory.typeId);
		List.of(MappingDefinitionTypeFactory.CoreProperties.values()).stream().forEach(prop -> {
			PPEPropertyType propType = type.getPropertyType(prop.toString());
			assertTrue(propType != null);
			assertTrue(propType.getCardinality().equals(Cardinalities.SINGLE));
			assertTrue(propType.getInstanceType().equals(BuildInType.STRING));
		});
	}		
	
	@Test
	void testNonRegisteredType() {
		RDFInstanceType nonExistingType = schemaReg.findNonDeletedInstanceTypeByFQN("nonono");
		Assertions.assertNull(nonExistingType);
	}
	
	@Test
	void testMapListSetPropertyGeneration() {
			
		RDFInstanceType type = schemaReg.findNonDeletedInstanceTypeByFQN(DecisionNodeDefinitionType.typeId);
		RDFInstanceType mappingType = schemaReg.findNonDeletedInstanceTypeByFQN(MappingDefinitionTypeFactory.typeId);
		RDFInstanceType dndType = schemaReg.findNonDeletedInstanceTypeByFQN(DecisionNodeDefinitionType.typeId);
		RDFInstanceType stepType = schemaReg.findNonDeletedInstanceTypeByFQN(StepDefinitionTypeFactory.typeId);
		RDFInstanceType processType = schemaReg.findNonDeletedInstanceTypeByFQN(ProcessDefinitionType.typeId);
		
		PPEPropertyType propType = type.getPropertyType(DecisionNodeDefinitionType.CoreProperties.dataMappingDefinitions.toString());
		assertTrue(propType != null);
		assertTrue(propType.getCardinality().equals(Cardinalities.SET));
		assertTrue(propType.getInstanceType().equals(mappingType));
				
		PPEPropertyType inType = type.getPropertyType(DecisionNodeDefinitionType.CoreProperties.inSteps.toString());
		assertTrue(inType != null);
		assertTrue(inType.getCardinality().equals(Cardinalities.SET));
		assertTrue(inType.getInstanceType().equals(stepType));
		
		PPEPropertyType procType = type.getPropertyType(ProcessDefinitionScopeType.CoreProperties.processDefinition.toString());
		assertTrue(procType != null);
		assertTrue(procType.getCardinality().equals(Cardinalities.SINGLE));
		RDFInstanceType procInstanceType = procType.getInstanceType(); 
		assertTrue(procInstanceType.equals(processType));
		
		
		PPEPropertyType dndPropType = processType.getPropertyType(ProcessDefinitionType.CoreProperties.stepDefinitions.toString());
		assertTrue(dndPropType != null);
		assertTrue(dndPropType.getCardinality().equals(Cardinalities.LIST));
		assertTrue(dndPropType.getInstanceType().equals(stepType));
		
		PPEPropertyType premPropType = processType.getPropertyType(ProcessDefinitionType.CoreProperties.prematureTriggers.toString());
		assertTrue(premPropType != null);
		assertTrue(premPropType.getCardinality().equals(Cardinalities.MAP));
		assertTrue(premPropType.getInstanceType().equals(BuildInType.STRING));
		
		
	}
	

	
	
}
