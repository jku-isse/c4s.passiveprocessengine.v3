package at.jku.isse.passiveprocessengine;

import at.jku.isse.passiveprocessengine.configurability.ProcessConfigBaseElementFactory;
import at.jku.isse.passiveprocessengine.core.Instance;
import at.jku.isse.passiveprocessengine.core.NameIdentifiableElement;

public abstract class InstanceWrapper implements NameIdentifiableElement{

	protected transient Instance instance;
	protected transient WrapperCache wrapperCache;
	
	public InstanceWrapper(Instance instance, WrapperCache wrapperCache) {
		this.instance = instance;
		this.wrapperCache = wrapperCache;
	}

	public Instance getInstance() {
		return instance;
	}

	@Override
	public String getName() {
		return instance.getId();
	}

	public void deleteCascading() {
		wrapperCache.removeWrapper(getInstance().getId());
		instance.delete();
	}

	public void deleteCascading(ProcessConfigBaseElementFactory configFactory) {
		wrapperCache.removeWrapper(getInstance().getId());
		instance.delete();
	}


}
