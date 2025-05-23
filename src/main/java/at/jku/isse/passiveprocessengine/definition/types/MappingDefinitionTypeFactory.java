package at.jku.isse.passiveprocessengine.definition.types;

import java.util.Optional;

import at.jku.isse.passiveprocessengine.core.AbstractTypeProvider;
import at.jku.isse.passiveprocessengine.definition.activeobjects.MappingDefinition;
import at.jku.isse.passiveprocessengine.rdfwrapper.NodeToDomainResolver;
import at.jku.isse.passiveprocessengine.rdfwrapper.RDFInstanceType;
import at.jku.isse.passiveprocessengine.rdfwrapper.rule.RuleEnabledResolver;

public class MappingDefinitionTypeFactory  extends AbstractTypeProvider {

	private static final String NS = ProcessDefinitionScopeTypeFactory.NS+"/mappingdefinition#";
	
	public enum CoreProperties {fromStepType, fromParameter, toStepType, toParameter, flowDir
		;
		
		@Override
		public String toString() {
			return NS+name();
		}
		
		public String getURI() {
			return NS+name();
		}
	}
	
	public static final String typeId = ProcessDefinitionScopeTypeFactory.NS+"#"+MappingDefinitionTypeFactory.class.getSimpleName();
	
	public MappingDefinitionTypeFactory(RuleEnabledResolver schemaRegistry) {
		super(schemaRegistry);
		Optional<RDFInstanceType> thisType = schemaRegistry.findNonDeletedInstanceTypeByFQN(typeId);
		if (thisType.isPresent()) {			
			this.type = thisType.get();
		} else {
			this.type = schemaRegistry.createNewInstanceType(typeId, schemaRegistry.findNonDeletedInstanceTypeByFQN(ProcessDefinitionScopeTypeFactory.typeId).orElse(null));			
		}
		metaElements.registerInstanceSpecificClass(typeId, MappingDefinition.class);
	}
	
	public void produceTypeProperties() {
		type.cacheSuperProperties();
		type.createSinglePropertyType(CoreProperties.fromStepType.toString(), primitives.getStringType());
		type.createSinglePropertyType(CoreProperties.fromParameter.toString(), primitives.getStringType());
		type.createSinglePropertyType(CoreProperties.toStepType.toString(), primitives.getStringType());
		type.createSinglePropertyType(CoreProperties.toParameter.toString(), primitives.getStringType());
		type.createSinglePropertyType(CoreProperties.flowDir.toString(), primitives.getStringType());	
	}
	
	public MappingDefinition getInstance(String fromStepTypeLocalName, String fromParameter, String toStepTypeLocalName, String toParameter, String processURI) {
		String id = processURI+ fromStepTypeLocalName+fromParameter+toStepTypeLocalName+toParameter;
		MappingDefinition md = (MappingDefinition) schemaRegistry.createInstance(id, type);		
		md.setFromStepType(fromStepTypeLocalName);
		md.setFromParameter(fromParameter);
		md.setToStepType(toStepTypeLocalName);
		md.setToParameter(toParameter);
		return md;
	}
}
