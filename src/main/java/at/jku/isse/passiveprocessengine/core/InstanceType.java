package at.jku.isse.passiveprocessengine.core;

import java.util.Set;

import lombok.Data;

public interface InstanceType extends Instance {

	// Defining a type
	void createListPropertyType(String name, InstanceType complexType);
	void createSetPropertyType(String name, InstanceType complexType);
	void createMapPropertyType(String name, InstanceType keyType, InstanceType valueType);
	void createSinglePropertyType(String name, InstanceType type);
	PropertyType getPropertyType(String propertyName);
	
	/**
	 * 
	 * @param instanceToCompareTo
	 * @return true if  the instance type this is called on, is of equals type or a subtype of 'instanceToCompareTo'
	 * accessing the type hierarchy
	 */
	boolean isOfTypeOrAnySubtype(InstanceType instanceToCompareTo);
	Set<InstanceType> getAllSubtypesRecursively();
	
	
	
	public static enum CARDINALITIES { SINGLE, LIST, SET, MAP };
	
	
	public static interface PropertyType {
		String getName();
		CARDINALITIES getCardinality();
		InstanceType getInstanceType();
		boolean isAssignable(Object object);
	};
}
