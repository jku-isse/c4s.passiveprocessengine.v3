package at.jku.isse.passiveprocessengine.instance.types;

import java.util.Optional;

import at.jku.isse.passiveprocessengine.core.AbstractTypeProvider;
import at.jku.isse.passiveprocessengine.rdfwrapper.NodeToDomainResolver;
import at.jku.isse.passiveprocessengine.rdfwrapper.RDFInstanceType;
import at.jku.isse.passiveprocessengine.definition.types.DecisionNodeDefinitionTypeFactory;
import at.jku.isse.passiveprocessengine.instance.activeobjects.DecisionNodeInstance;

public class DecisionNodeInstanceType extends AbstractTypeProvider {

	private static final String NS = ProcessInstanceScopeType.NS+"/dni";
	
	public enum CoreProperties {isInflowFulfilled, hasPropagated, dnd, inSteps, outSteps, closingDN
		;
		
		@Override
		public String toString() {
			return NS+name();
		}
		
		public String getURI() {
			return NS+name();
		}
	}

	public static final String typeId = NS+"#"+DecisionNodeInstance.class.getSimpleName();
	
	public DecisionNodeInstanceType(NodeToDomainResolver schemaRegistry) {
		super(schemaRegistry);
		Optional<RDFInstanceType> thisType = schemaRegistry.findNonDeletedInstanceTypeByFQN(typeId);
		if (thisType.isPresent()) {			
			this.type = thisType.get();
		} else {
			this.type = schemaRegistry.createNewInstanceType(typeId, schemaRegistry.findNonDeletedInstanceTypeByFQN(ProcessInstanceScopeType.typeId).orElse(null));
		}
		metaElements.registerInstanceSpecificClass(typeId, DecisionNodeInstance.class);
	}


	public void produceTypeProperties(ProcessInstanceScopeType processInstanceScopeType) {
		type.cacheSuperProperties();		
		type.createSinglePropertyType(CoreProperties.isInflowFulfilled.toString(), primitives.getBooleanType());
		type.createSinglePropertyType(CoreProperties.hasPropagated.toString(), primitives.getBooleanType());
		type.createSinglePropertyType(CoreProperties.dnd.toString(), 
				schemaRegistry.findNonDeletedInstanceTypeByFQN(DecisionNodeDefinitionTypeFactory.typeId)
				.map(vtype->vtype.getAsPropertyType())
				.orElse(null));
		type.createSetPropertyType(CoreProperties.inSteps.toString(), 
				schemaRegistry.findNonDeletedInstanceTypeByFQN(AbstractProcessStepType.typeId)
				.map(vtype->vtype.getAsPropertyType())
				.orElse(null));
		type.createSetPropertyType(CoreProperties.outSteps.toString(), 
				schemaRegistry.findNonDeletedInstanceTypeByFQN(AbstractProcessStepType.typeId)
				.map(vtype->vtype.getAsPropertyType())
				.orElse(null));
		type.createSinglePropertyType(CoreProperties.closingDN.toString(), type.getAsPropertyType());

		processInstanceScopeType.addGenericProcessProperty(type);
	}	
}
