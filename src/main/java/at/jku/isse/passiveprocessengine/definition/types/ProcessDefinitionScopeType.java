package at.jku.isse.passiveprocessengine.definition.types;

import java.util.Optional;

import at.jku.isse.passiveprocessengine.core.BuildInType;
import at.jku.isse.passiveprocessengine.core.PPEInstanceType;
import at.jku.isse.passiveprocessengine.core.SchemaRegistry;
import at.jku.isse.passiveprocessengine.core.TypeProvider;
import at.jku.isse.passiveprocessengine.definition.activeobjects.ProcessDefinition;
import at.jku.isse.passiveprocessengine.definition.activeobjects.ProcessDefinitionScopedElement;
import at.jku.isse.passiveprocessengine.rdfwrapper.RDFInstanceType;

public class ProcessDefinitionScopeType  implements TypeProvider {

	public static enum CoreProperties {processDefinition, orderIndex};
	private SchemaRegistry schemaRegistry;
	public static final String typeId = ProcessDefinitionScopeType.class.getSimpleName();
	private final PPEInstanceType type;
	
	public ProcessDefinitionScopeType(SchemaRegistry schemaRegistry) {
		this.schemaRegistry = schemaRegistry;
		Optional<PPEInstanceType> thisType = schemaRegistry.findNonDeletedInstanceTypeByFQN(typeId);
		if (thisType.isPresent()) {
			//schemaRegistry.registerType(ProcessDefinitionScopedElement.class, thisType.get());
			this.type = thisType.get();
		} else {
			PPEInstanceType type = schemaRegistry.createNewInstanceType(typeId);
			//schemaRegistry.registerType(ProcessDefinitionScopedElement.class, type);
			this.type = type;
		}
	}
	
	@Override
	public void produceTypeProperties() {
		((RDFInstanceType) type).cacheSuperProperties();
			//schemaRegistry.registerType(ProcessDefinitionScopedElement.class, type);
			type.createSinglePropertyType(ProcessDefinitionScopeType.CoreProperties.processDefinition.toString(), schemaRegistry.getTypeByName(ProcessDefinitionType.typeId));
			type.createSinglePropertyType((ProcessDefinitionScopeType.CoreProperties.orderIndex.toString()), BuildInType.INTEGER);
			
		
	}
}
