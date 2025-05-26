package at.jku.isse.passiveprocessengine.instance.types;

import java.util.Optional;

import at.jku.isse.passiveprocessengine.core.AbstractTypeProvider;
import at.jku.isse.passiveprocessengine.instance.ProcessException;
import at.jku.isse.passiveprocessengine.rdfwrapper.NodeToDomainResolver;
import at.jku.isse.passiveprocessengine.rdfwrapper.RDFInstance;
import at.jku.isse.passiveprocessengine.rdfwrapper.RDFInstanceType;
import at.jku.isse.passiveprocessengine.rdfwrapper.rule.RuleEnabledResolver;


public class ProcessConfigBaseElementTypeFactory extends AbstractTypeProvider {

	private static final String NS = ProcessInstanceScopeTypeFactory.NS+"/config";
	
	public enum CoreProperties {description;
		
		@Override
		public String toString() {
			return NS+name();
		}
		
		public String getURI() {
			return NS+name();
		}	
	} 
	public static final String typeId = NS+"#"+"Base";
	
	public ProcessConfigBaseElementTypeFactory(RuleEnabledResolver schemaRegistry) {
		super(schemaRegistry);
		Optional<RDFInstanceType> thisType = schemaRegistry.findNonDeletedInstanceTypeByFQN(typeId);
		if (thisType.isPresent()) {
			this.type = thisType.get();
		} else {
			this.type = schemaRegistry.createNewInstanceType(typeId, schemaRegistry.findNonDeletedInstanceTypeByFQN(ProcessInstanceScopeTypeFactory.typeId).orElse(null));		
		}
	}

	public void produceTypeProperties(ProcessInstanceScopeTypeFactory processInstanceScopeType) {
		type.cacheSuperProperties();	
		type.createSinglePropertyType(CoreProperties.description.toString(), primitives.getStringType());		
		
		processInstanceScopeType.addGenericProcessProperty(type);
	}
	
	public RDFInstance createConfigInstance(String name, RDFInstanceType configSubType) {
		// any other logic such as default values etc, not implemented at the moment
		return schemaRegistry.createInstance(name, configSubType);
	}

	public RDFInstance createConfigInstance(String name, String subtypeName) throws ProcessException{
		var subType =  schemaRegistry.findNonDeletedInstanceTypeByFQN(subtypeName);		
		if (subType.isEmpty()) {
			throw new ProcessException("Configuration Subtyp "+subtypeName+" does not exist");
		} else {
			return createConfigInstance(name, subType.get());
		}
	}
}
