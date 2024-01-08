package at.jku.isse.passiveprocessengine.core;

public interface Instance {

	String getId();	
	InstanceType getInstanceType();
	void markAsDeleted();
	boolean isMarkedAsDeleted();
	
	Object getPropertyAsSingle(String property);
	void setSingleProperty(String property, Object value);
	<T> T getTypedProperty(String property, Class<T> clazz);
	<T> T getTypedProperty(String property, Class<T> clazz, T defaultValue);

}
