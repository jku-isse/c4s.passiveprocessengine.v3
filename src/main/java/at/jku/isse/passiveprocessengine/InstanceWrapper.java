package at.jku.isse.passiveprocessengine;

import at.jku.isse.passiveprocessengine.core.PPEInstance;
import at.jku.isse.passiveprocessengine.core.NameIdentifiableElement;
import at.jku.isse.passiveprocessengine.instance.types.ProcessConfigBaseElementType;

public abstract class InstanceWrapper implements NameIdentifiableElement{

	protected transient PPEInstance instance;
	protected transient Context context;
	
	public InstanceWrapper(PPEInstance instance, Context context) {
		this.instance = instance;
		this.context = context;
	}

	public PPEInstance getInstance() {
		return instance;
	}

	@Override
	public String getName() {
		return instance.getName();
	}

	public void deleteCascading() {
		context.removeWrapper(getInstance().getId());
		instance.markAsDeleted();
	}

	public void deleteCascading(ProcessConfigBaseElementType configFactory) {
		context.removeWrapper(getInstance().getId());
		instance.markAsDeleted();
	}


}
