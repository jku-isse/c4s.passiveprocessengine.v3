package at.jku.isse.passiveprocessengine.designspace;

import java.util.Collection;
import java.util.Set;

import at.jku.isse.passiveprocessengine.core.Instance;
import at.jku.isse.passiveprocessengine.core.InstanceType;
import lombok.NonNull;

public class DesignspaceInstanceWrapper implements Instance {

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
	public void setSingleProperty(@NonNull String property, Object value) {
		if (value instanceof DesignspaceInstanceWrapper) {
			value = ((DesignspaceInstanceWrapper) value).getDelegate();
		}
		delegate.getPropertyAsSingle(property).set(value);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getTypedProperty(@NonNull String property, @NonNull Class<T> clazz) {
		Object value = delegate.getProperty(property);
		// if list/set/map create wrapper around to map ds instances to ppe instances
		if (Set.class.isAssignableFrom(clazz)) {
			
		}
		if (value == null) 
			return null;
		else 
			return (T)value;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getTypedProperty(@NonNull String property, @NonNull Class<T> clazz, T defaultValue) {
		Object value = delegate.getProperty(property);
		if (value == null) 
			return defaultValue;
		else 
			return (T)value;
	}

	

}
