package at.jku.isse.passiveprocessengine.definition.types;

import java.util.Optional;

import at.jku.isse.passiveprocessengine.core.BuildInType;
import at.jku.isse.passiveprocessengine.core.InstanceType;
import at.jku.isse.passiveprocessengine.core.SchemaRegistry;
import at.jku.isse.passiveprocessengine.core.TypeProvider;
import at.jku.isse.passiveprocessengine.definition.activeobjects.ProcessDefinition;
import at.jku.isse.passiveprocessengine.definition.activeobjects.ProcessDefinitionScopedElement;

public class ProcessDefinitionScopeType  implements TypeProvider {

	public static enum CoreProperties {process, orderIndex};
	private SchemaRegistry schemaRegistry;
	public static final String typeId = ProcessDefinitionScopeType.class.getSimpleName();
	private final InstanceType type;
	
	public ProcessDefinitionScopeType(SchemaRegistry schemaRegistry) {
		this.schemaRegistry = schemaRegistry;
		Optional<InstanceType> thisType = schemaRegistry.findNonDeletedInstanceTypeById(typeId);
		if (thisType.isPresent()) {
			schemaRegistry.registerType(ProcessDefinitionScopedElement.class, thisType.get());
			this.type = thisType.get();
		} else {
			InstanceType type = schemaRegistry.createNewInstanceType(typeId);
			schemaRegistry.registerType(ProcessDefinitionScopedElement.class, type);
			this.type = type;
		}
	}
	
	@Override
	public void produceTypeProperties() {
		
			schemaRegistry.registerType(ProcessDefinitionScopedElement.class, type);
			type.createSinglePropertyType(ProcessDefinitionScopeType.CoreProperties.process.toString(), schemaRegistry.getType(ProcessDefinition.class));
			type.createSinglePropertyType((ProcessDefinitionScopeType.CoreProperties.orderIndex.toString()), BuildInType.INTEGER);
			
		
	}
}
