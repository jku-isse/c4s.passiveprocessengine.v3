package at.jku.isse.passiveprocessengine.definition.types;

import java.util.Optional;

import at.jku.isse.passiveprocessengine.rdfwrapper.NodeToDomainResolver;
import at.jku.isse.passiveprocessengine.rdfwrapper.RDFInstance;
import at.jku.isse.passiveprocessengine.rdfwrapper.RDFInstanceType;
import at.jku.isse.passiveprocessengine.core.AbstractTypeProvider;
import at.jku.isse.passiveprocessengine.definition.activeobjects.DecisionNodeDefinition;
import at.jku.isse.passiveprocessengine.definition.activeobjects.DecisionNodeDefinition.InFlowType;

public class DecisionNodeDefinitionType extends AbstractTypeProvider {

	private static final String NS = ProcessDefinitionScopeType.NS+"/decisionnodedefinition#";
	
	public enum CoreProperties {inFlowType, dataMappingDefinitions, inSteps, outSteps, hierarchyDepth, closingDN
		;
		
		@Override
		public String toString() {
			return NS+name();
		}
		
		public String getURI() {
			return NS+name();
		}	
	}
	
	public static final String typeId = ProcessDefinitionScopeType.NS+"#"+DecisionNodeDefinition.class.getSimpleName();

	public DecisionNodeDefinitionType(NodeToDomainResolver schemaRegistry) {
		super(schemaRegistry);
		Optional<RDFInstanceType> thisType = schemaRegistry.findNonDeletedInstanceTypeByFQN(typeId);
		if (thisType.isPresent()) {
			this.type = thisType.get();
		} else {
			this.type = schemaRegistry.createNewInstanceType(typeId,  schemaRegistry.findNonDeletedInstanceTypeByFQN(ProcessDefinitionScopeType.typeId).orElse(null));
		}
		metaElements.registerInstanceSpecificClass(typeId, DecisionNodeDefinition.class);
	}
	
	public void produceTypeProperties() {
		type.cacheSuperProperties();
		
		type.createSinglePropertyType(CoreProperties.inFlowType.toString(), primitives.getStringType());
		type.createSetPropertyType(CoreProperties.inSteps.toString(), 
				schemaRegistry.findNonDeletedInstanceTypeByFQN(StepDefinitionTypeFactory.typeId)
				.map(vtype->vtype.getAsPropertyType())
				.orElse(null));
		type.createSetPropertyType(CoreProperties.outSteps.toString(),  
				schemaRegistry.findNonDeletedInstanceTypeByFQN(StepDefinitionTypeFactory.typeId)
				.map(vtype->vtype.getAsPropertyType())
				.orElse(null));
		type.createSetPropertyType(CoreProperties.dataMappingDefinitions.toString(),  
				schemaRegistry.findNonDeletedInstanceTypeByFQN(MappingDefinitionTypeFactory.typeId)
				.map(vtype->vtype.getAsPropertyType())
				.orElse(null));
		type.createSinglePropertyType((CoreProperties.hierarchyDepth.toString()),  primitives.getIntType());
		type.createSinglePropertyType(CoreProperties.closingDN.toString(), type.getAsPropertyType());
		
	}	

	public DecisionNodeDefinition createInstance(String dndId) {
		DecisionNodeDefinition instance = (DecisionNodeDefinition) schemaRegistry.createInstance(dndId, type);
		// default SEQ
		instance.setSingleProperty(CoreProperties.inFlowType.toString(), InFlowType.SEQ.toString());
		instance.setSingleProperty(CoreProperties.hierarchyDepth.toString(), -1);
		return instance;
	}
}
