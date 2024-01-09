package at.jku.isse.passiveprocessengine;

import at.jku.isse.passiveprocessengine.configurability.ProcessConfigBaseElementFactory;
import at.jku.isse.passiveprocessengine.core.Instance;
import at.jku.isse.passiveprocessengine.core.NameIdentifiableElement;

public abstract class InstanceWrapper implements NameIdentifiableElement{

	protected transient Instance instance;
	protected transient Context context;
	
	public InstanceWrapper(Instance instance, Context context) {
		this.instance = instance;
		this.context = context;
	}

	public Instance getInstance() {
		return instance;
	}

	@Override
	public String getName() {
		return instance.getId();
	}

	public void deleteCascading() {
		context.removeWrapper(getInstance().getId());
		instance.markAsDeleted();
	}

	public void deleteCascading(ProcessConfigBaseElementFactory configFactory) {
		context.removeWrapper(getInstance().getId());
		instance.markAsDeleted();
	}


}
