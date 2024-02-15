package at.jku.isse.passiveprocessengine.core;

import java.util.Set;

public interface PPEInstanceType extends PPEInstance {

	// Defining a type
	void createListPropertyType(String name, PPEInstanceType complexType);
	void createSetPropertyType(String name, PPEInstanceType complexType);
	void createMapPropertyType(String name, PPEInstanceType keyType, PPEInstanceType valueType);
	void createSinglePropertyType(String name, PPEInstanceType type);
	PPEPropertyType getPropertyType(String propertyName);
	
	/**
	 * 
	 * @param instanceToCompareTo
	 * @return true if  the instance type this is called on, is of equals type or a subtype of 'instanceToCompareTo'
	 * accessing the type hierarchy
	 */
	boolean isOfTypeOrAnySubtype(PPEInstanceType instanceToCompareTo);
	Set<PPEInstanceType> getAllSubtypesRecursively();
	
	
	
	public static enum CARDINALITIES { SINGLE, LIST, SET, MAP };
	
	
	public static interface PPEPropertyType {
		String getName();
		CARDINALITIES getCardinality();
		PPEInstanceType getInstanceType();
		boolean isAssignable(Object object);
	};
}
