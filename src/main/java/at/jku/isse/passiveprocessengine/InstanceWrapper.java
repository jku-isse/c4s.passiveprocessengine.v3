package at.jku.isse.passiveprocessengine;

import at.jku.isse.designspace.core.model.Instance;
import at.jku.isse.designspace.core.model.Workspace;

public abstract class InstanceWrapper implements IdentifiableElement{
	
	protected transient Instance instance;
	protected transient Workspace ws;
	
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
	
	public void deleteCascading() {
		WrapperCache.removeWrapper(getInstance().id());
		instance.delete();
		//this.instance = null;
		
	}
}
