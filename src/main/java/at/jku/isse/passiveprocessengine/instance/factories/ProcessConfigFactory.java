package at.jku.isse.passiveprocessengine.instance.factories;

import at.jku.isse.passiveprocessengine.Context;
import at.jku.isse.passiveprocessengine.core.FactoryIndex.DomainFactory;
import at.jku.isse.passiveprocessengine.core.Instance;
import at.jku.isse.passiveprocessengine.core.InstanceType;
import at.jku.isse.passiveprocessengine.instance.ProcessException;

public class ProcessConfigFactory extends DomainFactory {
		
	public ProcessConfigFactory(Context context) {
		super(context);		
	}

	public Instance createConfigInstance(String name, InstanceType configSubType) {
		// any other logic such as default values etc, not implemented at the moment
		return getContext().getInstanceRepository().createInstance(name, configSubType);
	}

	public Instance createConfigInstance(String name, String subtypeName) throws ProcessException{
		InstanceType subType =  getContext().getSchemaRegistry().getTypeByName(subtypeName);		
		if (subType == null) {
			throw new ProcessException("Configuration Subtyp "+subtypeName+" does not exist");
		} else {
			return createConfigInstance(name, subType);
		}
	}
}
