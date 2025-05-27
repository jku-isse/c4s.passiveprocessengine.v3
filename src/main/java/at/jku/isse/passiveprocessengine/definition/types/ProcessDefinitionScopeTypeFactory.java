package at.jku.isse.passiveprocessengine.definition.types;

import java.util.Optional;

import at.jku.isse.passiveprocessengine.core.AbstractTypeProvider;
import at.jku.isse.passiveprocessengine.core.BaseNamespace;
import at.jku.isse.passiveprocessengine.rdfwrapper.NodeToDomainResolver;
import at.jku.isse.passiveprocessengine.rdfwrapper.RDFInstanceType;
import at.jku.isse.passiveprocessengine.rdfwrapper.rule.RuleEnabledResolver;

public class ProcessDefinitionScopeTypeFactory extends AbstractTypeProvider {

	public static final String NS = BaseNamespace.NS+"/types";
	
	public enum CoreProperties {processDefinition, orderIndex
		;
		@Override
		public String toString() {
			return NS+"#"+name();
		}
		
		public String getURI() {
			return NS+"#"+name();
		}	
	}
	
	public static final String typeId = NS+"#ProcessDefinitionScope";
		
	public ProcessDefinitionScopeTypeFactory(RuleEnabledResolver schemaRegistry) {
		super(schemaRegistry);
		Optional<RDFInstanceType> thisType = schemaRegistry.findNonDeletedInstanceTypeByFQN(typeId);
		if (thisType.isPresent()) {
			this.type = thisType.get();
		} else {
			this.type = schemaRegistry.createNewInstanceType(typeId);
		}
	}
	
	public void produceTypeProperties() {
		type.cacheSuperProperties();
		type.createSinglePropertyType(CoreProperties.processDefinition.toString(), schemaRegistry.findNonDeletedInstanceTypeByFQN(ProcessDefinitionTypeFactory.typeId)
				.map(vtype->vtype.getAsPropertyType())
				.orElseThrow());
		type.createSinglePropertyType(CoreProperties.orderIndex.toString(), schemaRegistry.getMetaschemata().getPrimitiveTypesFactory().getIntType());
	}
}
