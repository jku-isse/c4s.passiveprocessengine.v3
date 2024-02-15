package at.jku.isse.passiveprocessengine.designspace;

import at.jku.isse.designspace.core.model.PropertyType;
import at.jku.isse.passiveprocessengine.core.PPEInstanceType;
import at.jku.isse.passiveprocessengine.core.PPEInstanceType.CARDINALITIES;
import at.jku.isse.passiveprocessengine.core.PPEInstanceType.PPEPropertyType;

public class PropertyTypeWrapper implements PPEPropertyType {

	final PropertyType delegate;
	final DesignSpaceSchemaRegistry dsWrapper;
	
	public PropertyTypeWrapper(PropertyType delegate, DesignSpaceSchemaRegistry dsWrapper) {
		this.delegate = delegate;
		this.dsWrapper = dsWrapper;
	}
	
	@Override
	public String getName() {
		return delegate.getName();
	}

	@Override
	public CARDINALITIES getCardinality() {
		return CARDINALITIES.valueOf(delegate.getCardinality().toString());
	}

	@Override
	public PPEInstanceType getInstanceType() {
		return dsWrapper.getWrappedType(delegate.getReferencedInstanceType());		
	}

	@Override
	public boolean isAssignable(Object object) {
		// map from process domain object to designspace domain object
		if (object instanceof DesignspaceInstanceWrapper) {
			return delegate.isAssignable(((DesignspaceInstanceWrapper) object).getDelegate());
		} else
			return delegate.isAssignable(object);
	}

}
