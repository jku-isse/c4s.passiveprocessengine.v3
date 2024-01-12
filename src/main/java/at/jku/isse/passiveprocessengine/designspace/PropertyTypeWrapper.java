package at.jku.isse.passiveprocessengine.designspace;

import at.jku.isse.passiveprocessengine.core.InstanceType;
import at.jku.isse.passiveprocessengine.core.InstanceType.CARDINALITIES;
import at.jku.isse.passiveprocessengine.core.InstanceType.PropertyType;

public class PropertyTypeWrapper implements PropertyType {

	final at.jku.isse.designspace.core.model.PropertyType delegate;
	final DesignSpaceSchemaRegistry dsWrapper;
	
	public PropertyTypeWrapper(at.jku.isse.designspace.core.model.PropertyType delegate, DesignSpaceSchemaRegistry dsWrapper) {
		this.delegate = delegate;
		this.dsWrapper = dsWrapper;
	}
	
	@Override
	public String getName() {
		return delegate.name();
	}

	@Override
	public CARDINALITIES getCardinality() {
		return CARDINALITIES.valueOf(delegate.cardinality().toString());
	}

	@Override
	public InstanceType getInstanceType() {
		return dsWrapper.getWrappedType(delegate.referencedInstanceType());		
	}

	@Override
	public boolean isAssignable(Object object) {
		return delegate.isAssignable(object);
	}

}
