package at.jku.isse.passiveprocessengine.definition.types;

import java.util.Optional;

import at.jku.isse.passiveprocessengine.rdfwrapper.NodeToDomainResolver;
import at.jku.isse.passiveprocessengine.rdfwrapper.RDFInstanceType;

public class ProcessDefinitionScopeType {

	public enum CoreProperties {processDefinition, orderIndex}
	
	private NodeToDomainResolver schemaRegistry;
	public static final String typeId = ProcessDefinitionScopeType.class.getSimpleName();
	private final RDFInstanceType type;
	
	public ProcessDefinitionScopeType(NodeToDomainResolver schemaRegistry) {
		this.schemaRegistry = schemaRegistry;
		Optional<RDFInstanceType> thisType = schemaRegistry.findNonDeletedInstanceTypeByFQN(typeId);
		if (thisType.isPresent()) {
			this.type = thisType.get();
		} else {
			this.type = schemaRegistry.createNewInstanceType(typeId);
		}
	}
	
	public void produceTypeProperties() {
		type.cacheSuperProperties();
		type.createSinglePropertyType(ProcessDefinitionScopeType.CoreProperties.processDefinition.toString(), schemaRegistry.getTypeByName(ProcessDefinitionType.typeId));
		type.createSinglePropertyType((ProcessDefinitionScopeType.CoreProperties.orderIndex.toString()), BuildInType.INTEGER);
	}
}
