package at.jku.isse.passiveprocessengine.instance.factories;

import at.jku.isse.passiveprocessengine.core.FactoryIndex.DomainFactory;
import at.jku.isse.passiveprocessengine.rdfwrapper.RDFInstance;
import at.jku.isse.passiveprocessengine.rdfwrapper.RDFInstanceType;
import at.jku.isse.passiveprocessengine.rdfwrapper.rule.RuleEnabledResolver;
import at.jku.isse.passiveprocessengine.instance.ProcessException;

public class ProcessConfigFactory extends DomainFactory {
		
	public ProcessConfigFactory(RuleEnabledResolver context) {
		super(context);		
	}

	public RDFInstance createConfigInstance(String name, RDFInstanceType configSubType) {
		// any other logic such as default values etc, not implemented at the moment
		return getContext().createInstance(name, configSubType);
	}

	public RDFInstance createConfigInstance(String name, String subtypeName) throws ProcessException{
		var subType =  getContext().findNonDeletedInstanceTypeByFQN(subtypeName);		
		if (subType.isEmpty()) {
			throw new ProcessException("Configuration Subtyp "+subtypeName+" does not exist");
		} else {
			return createConfigInstance(name, subType.get());
		}
	}
}
