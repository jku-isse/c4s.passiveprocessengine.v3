package at.jku.isse.passiveprocessengine.wrappers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.URI;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import at.jku.isse.passiveprocessengine.core.ProcessEngineConfigurationBuilder;
import at.jku.isse.artifacteventstreaming.schemasupport.Cardinalities;
import at.jku.isse.passiveprocessengine.rdfwrapper.PrimitiveTypesFactory;
import at.jku.isse.passiveprocessengine.rdfwrapper.RDFInstanceType;
import at.jku.isse.passiveprocessengine.rdfwrapper.RDFPropertyType;
import at.jku.isse.passiveprocessengine.definition.types.ConstraintSpecTypeFactory;
import at.jku.isse.passiveprocessengine.definition.types.DecisionNodeDefinitionTypeFactory;
import at.jku.isse.passiveprocessengine.definition.types.MappingDefinitionTypeFactory;
import at.jku.isse.passiveprocessengine.definition.types.ProcessDefinitionScopeTypeFactory;
import at.jku.isse.passiveprocessengine.definition.types.ProcessDefinitionTypeFactory;
import at.jku.isse.passiveprocessengine.definition.types.StepDefinitionTypeFactory;
import at.jku.isse.passiveprocessengine.instance.types.AbstractProcessStepType;
import at.jku.isse.passiveprocessengine.instance.types.ConstraintResultWrapperTypeFactory;
import at.jku.isse.passiveprocessengine.instance.types.DecisionNodeInstanceTypeFactory;
import at.jku.isse.passiveprocessengine.instance.types.ProcessInstanceScopeTypeFactory;
import at.jku.isse.passiveprocessengine.rdfwrapper.config.InMemoryEventStreamingSetupFactory;
import at.jku.isse.passiveprocessengine.rdfwrapper.rule.RuleEnabledResolver;

@TestInstance(Lifecycle.PER_CLASS)
public class DefinitionWrapperTests {
	
	public static final String REPOURI = "http://ppetests";
	
	protected ProcessEngineConfigurationBuilder configBuilder;
	protected InMemoryEventStreamingSetupFactory wrapperFactory;
	protected RuleEnabledResolver schemaReg;
	protected PrimitiveTypesFactory primitives;
	
	@BeforeAll
	public void setup() throws Exception {
		wrapperFactory = new InMemoryEventStreamingSetupFactory.FactoryBuilder()
				.withBranchName("main")
				.withRepoURI(new URI(REPOURI)).build();
		schemaReg = wrapperFactory.getResolver();
		primitives = schemaReg.getMetaschemata().getPrimitiveTypesFactory();
		configBuilder = new ProcessEngineConfigurationBuilder(
				schemaReg
				, wrapperFactory.getCoreTypeFactory());
	}
	
	
	@Test
	void testBasicDefinitionTypeRegistration() {
		assertNotNull(schemaReg.findNonDeletedInstanceTypeByFQN(ConstraintSpecTypeFactory.typeId));
	}
	
	@Test
	void testSuperType() {
		RDFInstanceType scopeType = schemaReg.findNonDeletedInstanceTypeByFQN(ProcessDefinitionScopeTypeFactory.typeId).get();
		RDFInstanceType specType = schemaReg.findNonDeletedInstanceTypeByFQN(ConstraintSpecTypeFactory.typeId).get();
		Set<RDFInstanceType> subtypes = scopeType.getAllSubtypesRecursively();
		assertNotNull(subtypes);
		assertTrue(subtypes.contains(specType));
		assertTrue(specType.isOfTypeOrAnySubtype(scopeType));
	}
	
	@Test
	void testAllDefinitionsTypeRegistration() {				
		assertNotNull(schemaReg.findNonDeletedInstanceTypeByFQN(ConstraintSpecTypeFactory.typeId) );
		System.out.println(schemaReg.findNonDeletedInstanceTypeByFQN(ConstraintSpecTypeFactory.typeId).get().getName());
		assertNotNull(schemaReg.findNonDeletedInstanceTypeByFQN(MappingDefinitionTypeFactory.typeId));
		System.out.println(schemaReg.findNonDeletedInstanceTypeByFQN(MappingDefinitionTypeFactory.typeId).get().getName());
		assertNotNull(schemaReg.findNonDeletedInstanceTypeByFQN(DecisionNodeDefinitionTypeFactory.typeId));
		System.out.println(schemaReg.findNonDeletedInstanceTypeByFQN(DecisionNodeDefinitionTypeFactory.typeId).get().getName());
		assertNotNull(schemaReg.findNonDeletedInstanceTypeByFQN(StepDefinitionTypeFactory.typeId) );
		System.out.println(schemaReg.findNonDeletedInstanceTypeByFQN(StepDefinitionTypeFactory.typeId).get().getName());
		assertNotNull(schemaReg.findNonDeletedInstanceTypeByFQN(ProcessDefinitionTypeFactory.typeId) );
		System.out.println(schemaReg.findNonDeletedInstanceTypeByFQN(ProcessDefinitionTypeFactory.typeId).get().getName());		
	}
	
	
	@Test
	void testSingleTypePropertyGeneration() {				
		RDFInstanceType type = schemaReg.findNonDeletedInstanceTypeByFQN(ConstraintSpecTypeFactory.typeId).get();
		RDFPropertyType propType = type.getPropertyType(ConstraintSpecTypeFactory.CoreProperties.isOverridable.toString());
		assertNotNull(propType);
		assertEquals(propType.getCardinality(),(Cardinalities.SINGLE));
		assertEquals(propType.getValueType(), primitives.getBooleanType());
		
		RDFPropertyType propType2 = type.getPropertyType(ConstraintSpecTypeFactory.CoreProperties.humanReadableDescription.toString());
		assertNotNull(propType2);
		assertEquals(propType2.getCardinality(),(Cardinalities.SINGLE));
		assertEquals(propType2.getValueType(), primitives.getStringType());
		
		RDFPropertyType propType3 = type.getPropertyType(ConstraintSpecTypeFactory.CoreProperties.ruleType.toString());
		assertNotNull(propType3 );
		assertEquals(propType3.getCardinality(), (Cardinalities.SINGLE));
		assertEquals(propType3.getValueType().getClassType(), schemaReg.getRuleSchema().getDefinitionType());	
	}
	
	@Test
	void testDataMappingTypePropertyGeneration() {
		RDFInstanceType type = schemaReg.findNonDeletedInstanceTypeByFQN(MappingDefinitionTypeFactory.typeId).get();
		List.of(MappingDefinitionTypeFactory.CoreProperties.values()).stream().forEach(prop -> {
			RDFPropertyType propType = type.getPropertyType(prop.toString());
			assertNotNull(propType);
			assertEquals(propType.getCardinality(), (Cardinalities.SINGLE));
			assertEquals(propType.getValueType(), primitives.getStringType());
		});
	}		
	
	@Test
	void testNonRegisteredType() {
		var nonExistingType = schemaReg.findNonDeletedInstanceTypeByFQN("nonono");
		assertFalse(nonExistingType.isPresent());
	}
	
	@Test
	void testMapListSetPropertyGeneration() {
			
		RDFInstanceType type = schemaReg.findNonDeletedInstanceTypeByFQN(DecisionNodeDefinitionTypeFactory.typeId).get();
		RDFInstanceType mappingType = schemaReg.findNonDeletedInstanceTypeByFQN(MappingDefinitionTypeFactory.typeId).get();
		RDFInstanceType dndType = schemaReg.findNonDeletedInstanceTypeByFQN(DecisionNodeDefinitionTypeFactory.typeId).get();
		RDFInstanceType stepType = schemaReg.findNonDeletedInstanceTypeByFQN(StepDefinitionTypeFactory.typeId).get();
		RDFInstanceType processType = schemaReg.findNonDeletedInstanceTypeByFQN(ProcessDefinitionTypeFactory.typeId).get();
		
		RDFPropertyType propType = type.getPropertyType(DecisionNodeDefinitionTypeFactory.CoreProperties.dataMappingDefinitions.toString());
		assertNotNull(propType);
		assertEquals(propType.getCardinality(),(Cardinalities.SET));
		assertEquals(propType.getValueType(), mappingType.getAsPropertyType());
				
		RDFPropertyType inType = type.getPropertyType(DecisionNodeDefinitionTypeFactory.CoreProperties.inSteps.toString());
		assertNotNull(inType);
		assertEquals(inType.getCardinality(), (Cardinalities.SET));
		assertEquals(inType.getValueType(), (stepType.getAsPropertyType()));
		
		RDFPropertyType procType = type.getPropertyType(ProcessDefinitionScopeTypeFactory.CoreProperties.processDefinition.toString());
		assertNotNull(procType);
		assertEquals(procType.getCardinality(), (Cardinalities.SINGLE));
		var procInstanceType = procType.getValueType(); 
		assertEquals(procInstanceType, (processType.getAsPropertyType()));
		
		
		RDFPropertyType dndPropType = processType.getPropertyType(ProcessDefinitionTypeFactory.CoreProperties.stepDefinitions.toString());
		assertNotNull(dndPropType);
		assertEquals(dndPropType.getCardinality(), (Cardinalities.LIST));
		assertEquals(dndPropType.getValueType(), (stepType.getAsPropertyType()));
	}
	
	@Test
	void testAllBaseInstanceTypeRegistration() {				
		assertTrue(schemaReg.findNonDeletedInstanceTypeByFQN(ProcessInstanceScopeTypeFactory.typeId).isPresent() );
		assertTrue(schemaReg.findNonDeletedInstanceTypeByFQN(ConstraintResultWrapperTypeFactory.typeId) .isPresent());
		assertTrue(schemaReg.findNonDeletedInstanceTypeByFQN(DecisionNodeInstanceTypeFactory.typeId).isPresent());
		assertTrue(schemaReg.findNonDeletedInstanceTypeByFQN(AbstractProcessStepType.typeId).isPresent() );	
	}
	
	
}
