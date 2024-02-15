package at.jku.isse.passiveprocessengine.core;

public interface PPEInstance {

	String getId();	
	String getName();
	PPEInstanceType getInstanceType();
	void markAsDeleted();
	boolean isMarkedAsDeleted();
	
	void setSingleProperty(String property, Object value);
	<T> T getTypedProperty(String property, Class<T> clazz);
	<T> T getTypedProperty(String property, Class<T> clazz, T defaultValue);
	void setInstanceType(PPEInstanceType childType);

}
