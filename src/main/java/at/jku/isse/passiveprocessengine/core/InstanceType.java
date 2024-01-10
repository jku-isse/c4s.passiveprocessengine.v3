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
	
	// accessing type hierarchy
	boolean isOfTypeOrAnySubtype(InstanceType instanceToCompareTo);
	Set<InstanceType> getAllSubtypesRecursively();
	
	
	
	public static enum CARDINALITIES { SINGLE, LIST, SET, MAP };
	
	@Data
	public static class PropertyType {
		final String name;
		final CARDINALITIES cardinality;
		final InstanceType instanceType;
		public boolean isAssignable(Object object) {
			// TODO Auto-generated method stub
			return false;
		}
	};
}
