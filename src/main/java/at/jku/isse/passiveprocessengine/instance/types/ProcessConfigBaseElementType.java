package at.jku.isse.passiveprocessengine.instance.types;

import java.util.Optional;

import at.jku.isse.passiveprocessengine.core.AbstractTypeProvider;
import at.jku.isse.passiveprocessengine.rdfwrapper.NodeToDomainResolver;
import at.jku.isse.passiveprocessengine.rdfwrapper.RDFInstanceType;
import at.jku.isse.passiveprocessengine.rdfwrapper.rule.RuleEnabledResolver;


public class ProcessConfigBaseElementType extends AbstractTypeProvider {

	private static final String NS = ProcessInstanceScopeType.NS+"/config";
	
	public  enum CoreProperties {description;
		
		@Override
		public String toString() {
			return NS+name();
		}
		
		public String getURI() {
			return NS+name();
		}	
	} 
	public static final String typeId = NS+"#"+"base";
	
	public ProcessConfigBaseElementType(RuleEnabledResolver schemaRegistry) {
		super(schemaRegistry);
		Optional<RDFInstanceType> thisType = schemaRegistry.findNonDeletedInstanceTypeByFQN(typeId);
		if (thisType.isPresent()) {
			this.type = thisType.get();
		} else {
			this.type = schemaRegistry.createNewInstanceType(typeId, schemaRegistry.findNonDeletedInstanceTypeByFQN(ProcessInstanceScopeType.typeId).orElse(null));		
		}
	}

	public void produceTypeProperties(ProcessInstanceScopeType processInstanceScopeType) {
		type.cacheSuperProperties();	
		type.createSinglePropertyType(CoreProperties.description.toString(), primitives.getStringType());		
		
		processInstanceScopeType.addGenericProcessProperty(type);
	}
}
