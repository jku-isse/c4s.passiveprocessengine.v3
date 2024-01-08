package at.jku.isse.passiveprocessengine.core;

import lombok.Data;

public interface InstanceType extends Instance {

	void createListPropertyType(String name, InstanceType complexType);
	void createSetPropertyType(String name, InstanceType complexType);
	void createMapPropertyType(String name, InstanceType keyType, InstanceType valueType);
	void createSinglePropertyType(String name, InstanceType type);
	
	PropertyType getPropertyType(String propertyName);
	
	public static enum CARDINALITIES { SINGLE, LIST, SET, MAP };
	
	@Data
	public static class PropertyType {
		final String name;
		final CARDINALITIES cardinality;
		final InstanceType instanceType;
	};
}
