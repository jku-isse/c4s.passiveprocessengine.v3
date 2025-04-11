package at.jku.isse.passiveprocessengine.instance.types;

import java.util.Optional;

import at.jku.isse.passiveprocessengine.core.AbstractTypeProvider;
import at.jku.isse.passiveprocessengine.core.BaseNamespace;
import at.jku.isse.passiveprocessengine.rdfwrapper.NodeToDomainResolver;
import at.jku.isse.passiveprocessengine.rdfwrapper.RDFInstanceType;
import at.jku.isse.passiveprocessengine.instance.activeobjects.ProcessInstanceScopedElement;

public class ProcessInstanceScopeType extends AbstractTypeProvider {

	public static final String NS = BaseNamespace.NS+"/instances";
	
	public enum CoreProperties {process
		;
		@Override
		public String toString() {
			return NS+name();
		}
		
		public String getURI() {
			return NS+name();
		}
	}
	
	public static final String typeId = NS+"#"+ProcessInstanceScopedElement.class.getSimpleName();

	public ProcessInstanceScopeType(NodeToDomainResolver schemaRegistry) {
		super(schemaRegistry);
		Optional<RDFInstanceType> thisType = schemaRegistry.findNonDeletedInstanceTypeByFQN(typeId);
		if (thisType.isPresent()) {			
			this.type = thisType.get();
		} else {
			this.type = schemaRegistry.createNewInstanceType(typeId);			
		}
	}	
	
	public void produceTypeProperties() {
		//type.createPropertyType(CoreProperties.process.toString(), Cardinality.SINGLE, typeStep); needs to be add in individual subclasses in order to be able to refine it					
	}
	
	public void addGenericProcessProperty(RDFInstanceType instType) {
		instType.cacheSuperProperties();
		if (instType.getPropertyType(ProcessInstanceScopeType.CoreProperties.process.toString()) == null) {
			instType.createSinglePropertyType(ProcessInstanceScopeType.CoreProperties.process.toString(), type.getAsPropertyType());			
		}
	}
}
