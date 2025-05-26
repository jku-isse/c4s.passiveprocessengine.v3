package at.jku.isse.passiveprocessengine.instance.types;

import java.util.Optional;
import java.util.UUID;

import at.jku.isse.passiveprocessengine.core.AbstractTypeProvider;
import at.jku.isse.passiveprocessengine.rdfwrapper.NodeToDomainResolver;
import at.jku.isse.passiveprocessengine.rdfwrapper.RDFInstance;
import at.jku.isse.passiveprocessengine.rdfwrapper.RDFInstanceType;
import at.jku.isse.passiveprocessengine.rdfwrapper.rule.RuleEnabledResolver;
import at.jku.isse.passiveprocessengine.definition.activeobjects.DecisionNodeDefinition;
import at.jku.isse.passiveprocessengine.definition.types.DecisionNodeDefinitionTypeFactory;
import at.jku.isse.passiveprocessengine.instance.activeobjects.DecisionNodeInstance;
import at.jku.isse.passiveprocessengine.instance.activeobjects.ProcessInstance;

public class DecisionNodeInstanceTypeFactory extends AbstractTypeProvider {

	private static final String NS = ProcessInstanceScopeTypeFactory.NS+"/dni";
	
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

	public static final String typeId = NS+"#DecisionNodeInstance";
	
	public DecisionNodeInstanceTypeFactory(RuleEnabledResolver schemaRegistry) {
		super(schemaRegistry);
		Optional<RDFInstanceType> thisType = schemaRegistry.findNonDeletedInstanceTypeByFQN(typeId);
		if (thisType.isPresent()) {			
			this.type = thisType.get();
		} else {
			this.type = schemaRegistry.createNewInstanceType(typeId, schemaRegistry.findNonDeletedInstanceTypeByFQN(ProcessInstanceScopeTypeFactory.typeId).orElse(null));
		}
		metaElements.registerInstanceSpecificClass(typeId, DecisionNodeInstance.class);
	}


	public void produceTypeProperties(ProcessInstanceScopeTypeFactory processInstanceScopeType) {
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
	
	public DecisionNodeInstance getInstance(DecisionNodeDefinition dnd, ProcessInstance scope) {			
		var base = scope.getInstance().getNameSpace();
		var procName = scope.getInstance().getLocalName();
		var id = base + "/" + procName + "/dni#" + dnd.getName();
		
		RDFInstance instance = schemaRegistry.createInstance(id
			, schemaRegistry.findNonDeletedInstanceTypeByFQN(DecisionNodeInstanceTypeFactory.typeId).orElseThrow());
		DecisionNodeInstance dni = (DecisionNodeInstance) instance;

		instance.setSingleProperty(DecisionNodeInstanceTypeFactory.CoreProperties.dnd.toString(),dnd.getInstance());
		instance.setSingleProperty(DecisionNodeInstanceTypeFactory.CoreProperties.hasPropagated.toString(),false);
		instance.setSingleProperty(DecisionNodeInstanceTypeFactory.CoreProperties.isInflowFulfilled.toString(), false);
		return dni;
	}
}
