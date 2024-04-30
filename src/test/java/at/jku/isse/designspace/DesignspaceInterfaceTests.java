package at.jku.isse.designspace;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import at.jku.isse.passiveprocessengine.core.BuildInType;
import at.jku.isse.passiveprocessengine.core.PPEInstance;
import at.jku.isse.passiveprocessengine.core.PPEInstanceType;
import at.jku.isse.passiveprocessengine.core.PPEInstanceType.CARDINALITIES;

@ExtendWith(SpringExtension.class)
@SpringBootTest
class DesignspaceInterfaceTests extends Designspace5Setup {


	private static final String ENTRY1 = "Entry1";
	private static final String TEST_BASE_TYPE = "TestBaseType";
	private static final String MAP_PROP = "MapProp";
	private static final String SET_PROP = "SetProp";
	private static final String LIST_PROP = "ListProp";
	private static final String SINGLE_PROP = "SingleProp";
	private static final String TEST_CHILD_TYPE = "TestChildType";
	private static final String PARENT_PROP = "parentProp";

	@Test
	void testBasicSchema() {
		createBaseType();
		PPEInstanceType baseType = schemaRegistry.getTypeByName(TEST_BASE_TYPE);
		assert(baseType != null);
		assert(baseType.getPropertyType(LIST_PROP).getCardinality().equals(CARDINALITIES.LIST));
		assert(baseType.getPropertyType(LIST_PROP).getInstanceType().getName().equals(TEST_BASE_TYPE));
		
		assert(baseType.getPropertyType(SET_PROP).getCardinality().equals(CARDINALITIES.SET));
		assert(baseType.getPropertyType(SET_PROP).getInstanceType().equals(BuildInType.BOOLEAN));
		
		assert(baseType.getPropertyType(MAP_PROP).getCardinality().equals(CARDINALITIES.MAP));
		assert(baseType.getPropertyType(MAP_PROP).getInstanceType().equals(BuildInType.INTEGER));
		
		assert(baseType.getPropertyType(SINGLE_PROP).getCardinality().equals(CARDINALITIES.SINGLE));
		assert(baseType.getPropertyType(SINGLE_PROP).getInstanceType().equals(BuildInType.STRING));
	}
	
	@Test
	void testSubclassSchema() {
		createBaseType();
		createChildType();
		PPEInstanceType childType = schemaRegistry.getTypeByName(TEST_CHILD_TYPE);
		assert(childType != null);
		PPEInstanceType baseType = schemaRegistry.getTypeByName(TEST_BASE_TYPE);
		assert(baseType != null);
		assert(childType.getPropertyType(PARENT_PROP).getInstanceType().equals(baseType));
		assert(childType.getPropertyType(PARENT_PROP).getCardinality().equals(CARDINALITIES.SINGLE));
		
	}
	
	
	@Test
	void testInstanceCreation() {
		createBaseType();
		PPEInstanceType baseType = schemaRegistry.getTypeByName(TEST_BASE_TYPE);
		PPEInstance inst1 = instanceRepository.createInstance("Inst1", baseType);
		PPEInstance inst2 = instanceRepository.createInstance("Inst2", baseType);
		PPEInstance inst3 = instanceRepository.createInstance("Inst3", baseType);
		inst1.setSingleProperty(SINGLE_PROP, ENTRY1);
		inst1.getTypedProperty(LIST_PROP, List.class).add(inst2);
		inst1.getTypedProperty(SET_PROP, Set.class).add(Boolean.TRUE);
		inst1.getTypedProperty(MAP_PROP, Map.class).put(ENTRY1, 3);
		
		assert(inst1.getTypedProperty(SINGLE_PROP, String.class).equals(ENTRY1));
		assert(inst1.getTypedProperty(LIST_PROP, List.class).get(0).equals(inst2));
		assert(inst1.getTypedProperty(SET_PROP, Set.class).stream().anyMatch(entry -> entry==Boolean.TRUE));
		assert(inst1.getTypedProperty(MAP_PROP, Map.class).get(ENTRY1).equals(3));
	}
	
	@Test
	void testInstanceSubclassing() {
		createBaseType();
		createChildType();
		PPEInstanceType baseType = schemaRegistry.getTypeByName(TEST_BASE_TYPE);
		PPEInstance inst1 = instanceRepository.createInstance("Inst1", baseType);
		PPEInstance inst2 = instanceRepository.createInstance("Inst2", baseType);
		PPEInstance inst3 = instanceRepository.createInstance("Inst3", baseType);
		inst1.setSingleProperty(SINGLE_PROP, ENTRY1);
		inst1.getTypedProperty(LIST_PROP, List.class).add(inst2);
		inst1.getTypedProperty(SET_PROP, Set.class).add(Boolean.TRUE);
		inst1.getTypedProperty(MAP_PROP, Map.class).put(ENTRY1, 3);
		
		PPEInstanceType childType = schemaRegistry.getTypeByName(TEST_CHILD_TYPE);
		assert(childType != null);
		inst2.setInstanceType(childType);
		inst3.setInstanceType(childType);
		assert(inst2.getInstanceType().equals(childType));
		
		inst2.setSingleProperty(PARENT_PROP, inst3);
		
		PPEInstance parent = (PPEInstance) inst1.getTypedProperty(LIST_PROP, List.class).get(0);
		assert(parent.getTypedProperty(PARENT_PROP, PPEInstance.class).equals(inst3));
	}
	
	protected void createBaseType() {
		PPEInstanceType testType = schemaRegistry.createNewInstanceType(TEST_BASE_TYPE);
		testType.createSinglePropertyType(SINGLE_PROP, BuildInType.STRING);
		testType.createListPropertyType(LIST_PROP, testType);
		testType.createSetPropertyType(SET_PROP, BuildInType.BOOLEAN);
		testType.createMapPropertyType(MAP_PROP, BuildInType.STRING, BuildInType.INTEGER);
		schemaRegistry.registerTypeByName(testType);
	}
	
	protected void createChildType() {
		
		PPEInstanceType childType = schemaRegistry.createNewInstanceType(TEST_CHILD_TYPE, schemaRegistry.getTypeByName(TEST_BASE_TYPE));
		childType.createSinglePropertyType(PARENT_PROP, schemaRegistry.getTypeByName(TEST_BASE_TYPE));
		schemaRegistry.registerTypeByName(childType);
	}
	
	
}
