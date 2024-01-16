package at.jku.isse.passiveprocessengine.designspace;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import at.jku.isse.designspace.core.model.Cardinality;
import at.jku.isse.designspace.core.model.SingleProperty;
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
		if (value instanceof DesignspaceInstanceWrapper) {
			value = ((DesignspaceInstanceWrapper) value).getDelegate();
		}
		delegate.getPropertyAsSingle(property).set(value);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getTypedProperty(@NonNull String property, @NonNull Class<T> clazz) {
		Object prop = delegate.getProperty(property);
		if (prop == null) // no such property
			return null;
		if (prop instanceof SingleProperty) {			
			Object dsValue = ((SingleProperty) prop).get();
			if (dsValue instanceof at.jku.isse.designspace.core.model.Instance) {
				return (T) dsSchemaRegistry.getWrappedInstance((at.jku.isse.designspace.core.model.Instance) dsValue);
			} else { 
				return (T) dsValue;
			}
		} else {
			PropertyType propType = getPropertyType(property);
			if (propType.getInstanceType() == null || DesignspaceInstanceWrapper.isAtomicType(propType.getInstanceType())) {
				return (T)prop; // no matter if single string, or list of strings, no mapping needed
			} else
				// if list/set/map create wrapper around to map ds instances to ppe instances
				if (Set.class.isAssignableFrom(clazz)) {
					return (T) new InstanceSetMapper((Set<at.jku.isse.designspace.core.model.Instance>) prop, dsSchemaRegistry);
				} else
				if (List.class.isAssignableFrom(clazz)) {
					return (T) new InstanceListMapper((List<at.jku.isse.designspace.core.model.Instance>) prop, dsSchemaRegistry);
				} else
				if (Map.class.isAssignableFrom(clazz)) {
						return (T) new InstanceMapMapper((Map<String,at.jku.isse.designspace.core.model.Element>) prop, dsSchemaRegistry);
				}
				else {
					throw new RuntimeException(String.format("Unknown property type or target classe for property %s and target class %s ", property, clazz.getName() ));
				}
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getTypedProperty(@NonNull String property, @NonNull Class<T> clazz, T defaultValue) {
		T value = getTypedProperty(property, clazz);
		if (value == null)
			return defaultValue;
		else
			return value; 
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
		@SuppressWarnings("rawtypes")
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
