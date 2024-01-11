package at.jku.isse.passiveprocessengine.designspace;

import java.util.Set;

import at.jku.isse.passiveprocessengine.core.InstanceType;

public class DesignspaceInstanceTypeWrapper implements InstanceType {

	final at.jku.isse.designspace.core.model.InstanceType delegate;
	final DesignSpaceSchemaRegistry dsSchemaRegistry;
	
	public DesignspaceInstanceTypeWrapper(at.jku.isse.designspace.core.model.InstanceType delegate, DesignSpaceSchemaRegistry dsSchemaRegistry) {
		this.delegate = delegate;
		this.dsSchemaRegistry = dsSchemaRegistry;
	}
	
	public at.jku.isse.designspace.core.model.InstanceType getDelegate() {
		return delegate;
	}
	
	@Override
	public String getId() {
		return delegate.id().toString();
	}

	@Override
	public String getName() {
		return delegate.name();
	}

	@Override
	public InstanceType getInstanceType() {
		return dsSchemaRegistry.getWrappedType(delegate.getInstanceType());
	}

	@Override
	public void markAsDeleted() {
		delegate.delete();
	}

	@Override
	public boolean isMarkedAsDeleted() {
		return delegate.isDeleted();
	}

	@Override
	public void setSingleProperty(String property, Object value) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public <T> T getTypedProperty(String property, Class<T> clazz) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T> T getTypedProperty(String property, Class<T> clazz, T defaultValue) {
		// TODO Auto-generated method stub
		return null;
	}
	
	

	@Override
	public void createListPropertyType(String name, InstanceType complexType) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void createSetPropertyType(String name, InstanceType complexType) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void createMapPropertyType(String name, InstanceType keyType, InstanceType valueType) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void createSinglePropertyType(String name, InstanceType type) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public PropertyType getPropertyType(String propertyName) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isOfTypeOrAnySubtype(InstanceType instanceToCompareTo) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Set<InstanceType> getAllSubtypesRecursively() {
		// TODO Auto-generated method stub
		return null;
	}

}
