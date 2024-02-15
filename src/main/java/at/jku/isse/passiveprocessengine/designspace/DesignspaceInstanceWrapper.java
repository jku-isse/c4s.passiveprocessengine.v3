package at.jku.isse.passiveprocessengine.designspace;

import java.util.List;
import java.util.Map;
import java.util.Set;

import at.jku.isse.designspace.core.model.InstanceType;
import at.jku.isse.designspace.core.model.PropertyType;
import at.jku.isse.designspace.core.model.SingleProperty;
import at.jku.isse.passiveprocessengine.core.BuildInType;
import at.jku.isse.passiveprocessengine.core.PPEInstance;
import at.jku.isse.passiveprocessengine.core.PPEInstanceType;
import lombok.NonNull;

public class DesignspaceInstanceWrapper implements PPEInstance {

	final at.jku.isse.designspace.core.model.Instance delegate;
	final DesignSpaceSchemaRegistry dsSchemaRegistry;
	
	public DesignspaceInstanceWrapper(at.jku.isse.designspace.core.model.Instance delegate, DesignSpaceSchemaRegistry dsSchemaRegistry) {
		this.delegate = delegate;
		this.dsSchemaRegistry = dsSchemaRegistry;
	}
	
	public at.jku.isse.designspace.core.model.Instance getDelegate() {
		return delegate;
	}
	
	@Override
	public String getId() {
		return ""+delegate.getId();
	}

	@Override
	public String getName() {
		return delegate.getName();
	}

	@Override
	public PPEInstanceType getInstanceType() {
		return dsSchemaRegistry.getWrappedType(delegate.getInstanceType());
	}
	
	@Override
	public void setInstanceType(PPEInstanceType childType) {
		this.delegate.set(InstanceType.INSTANCE_OF, dsSchemaRegistry.mapProcessDomainInstanceTypeToDesignspaceInstanceType(childType));
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
		delegate.set(resolveProperty(property), value);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getTypedProperty(@NonNull String property, @NonNull Class<T> clazz) {
		Object prop = delegate.getProperty(resolveProperty(property));
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
			if ( isAtomicType(getInstanceType().getPropertyType(property).getInstanceType())) {
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

	private PropertyType resolveProperty(String propertyName) {
		return this.delegate.getInstanceType().getPropertyType(propertyName);
	}
	
	public static boolean isAtomicType(PPEInstanceType type) {
		return type.equals(BuildInType.BOOLEAN)
				|| type.equals(BuildInType.STRING)
				|| type.equals(BuildInType.FLOAT)
				|| type.equals(BuildInType.DATE)
				|| type.equals(BuildInType.INTEGER);
	}
	
	@Override
	public String toString() {
		return getDelegate().toString();
	}


}
