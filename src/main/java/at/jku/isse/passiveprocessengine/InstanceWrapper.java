package at.jku.isse.passiveprocessengine;

import at.jku.isse.designspace.core.model.Instance;

public abstract class InstanceWrapper implements IdentifiableElement{
	
	protected transient Instance instance;
	
	public InstanceWrapper(Instance instance) {
		assert instance != null;
		this.instance = instance;
	}
	
	public Instance getInstance() {
		return instance;
	}
	
	public String getId() {
		return instance.id().toString();
	}
	
	@Override
	public String getName() {
		return instance.name();
	}
}
