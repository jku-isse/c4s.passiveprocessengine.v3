package at.jku.isse.passiveprocessengine.designspace;

import java.util.HashMap;
import java.util.Set;
import java.util.stream.Collectors;

import at.jku.isse.designspace.core.model.Cardinality;
import at.jku.isse.passiveprocessengine.core.InstanceType;
import lombok.NonNull;

public class DesignspaceInstanceTypeWrapper implements InstanceType {

	final at.jku.isse.designspace.core.model.InstanceType delegate;
	final DesignSpaceSchemaRegistry dsSchemaRegistry;
	final HashMap<String, PropertyTypeWrapper> propertyWrappers = new HashMap<>();
	
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
		//TODO make this fqn for instance types
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
	public void setSingleProperty(@NonNull String property, Object value) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public <T> T getTypedProperty(@NonNull String property, @NonNull Class<T> clazz) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T> T getTypedProperty(@NonNull String property, @NonNull Class<T> clazz, T defaultValue) {
		// TODO Auto-generated method stub
		return null;
	}
	
	

	@Override
	public void createListPropertyType(@NonNull String name, @NonNull InstanceType complexType) {
		delegate.createPropertyType(name, Cardinality.LIST, dsSchemaRegistry.mapProcessDomainInstanceTypeToDesignspaceInstanceType(complexType));
	}

	@Override
	public void createSetPropertyType(@NonNull String name, @NonNull InstanceType complexType) {
		delegate.createPropertyType(name, Cardinality.SET, dsSchemaRegistry.mapProcessDomainInstanceTypeToDesignspaceInstanceType(complexType));		
	}

	@Override
	public void createMapPropertyType(@NonNull String name, @NonNull InstanceType keyType, @NonNull InstanceType valueType) {
		delegate.createPropertyType(name, Cardinality.MAP, dsSchemaRegistry.mapProcessDomainInstanceTypeToDesignspaceInstanceType(valueType));
	}

	@Override
	public void createSinglePropertyType(@NonNull String name, @NonNull InstanceType type) {		
		delegate.createPropertyType(name, Cardinality.SINGLE, dsSchemaRegistry.mapProcessDomainInstanceTypeToDesignspaceInstanceType(type));		
	}

	@Override
	public PropertyType getPropertyType(String propertyName) {
		at.jku.isse.designspace.core.model.PropertyType propType = delegate.getPropertyType(propertyName);
		if (propType != null) {
			PropertyTypeWrapper propertyType = propertyWrappers.computeIfAbsent(propertyName, k -> new PropertyTypeWrapper(propType, dsSchemaRegistry));
			return propertyType;
		} else		
			return null;
	}

	@Override
	public boolean isOfTypeOrAnySubtype(InstanceType instanceToCompareTo) {
		return delegate.isKindOf(dsSchemaRegistry.mapProcessDomainInstanceTypeToDesignspaceInstanceType(instanceToCompareTo));		
	}

	@Override
	public Set<InstanceType> getAllSubtypesRecursively() {
		return delegate.getAllSubTypes().stream().map(subtype -> dsSchemaRegistry.getWrappedType(subtype)).collect(Collectors.toSet());
	}

}
