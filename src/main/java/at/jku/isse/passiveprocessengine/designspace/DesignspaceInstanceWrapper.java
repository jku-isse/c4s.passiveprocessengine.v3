package at.jku.isse.passiveprocessengine.designspace;

import java.util.Set;

import at.jku.isse.passiveprocessengine.core.Instance;
import at.jku.isse.passiveprocessengine.core.InstanceType;

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

	

}
