package at.jku.isse.passiveprocessengine.definition.types;

import java.util.Optional;

import at.jku.isse.passiveprocessengine.core.BuildInType;
import at.jku.isse.passiveprocessengine.core.InstanceType;
import at.jku.isse.passiveprocessengine.core.ProcessDomainTypesRegistry;
import at.jku.isse.passiveprocessengine.core.SchemaRegistry;
import at.jku.isse.passiveprocessengine.core.ProcessDomainTypesRegistry.TypeProvider;
import at.jku.isse.passiveprocessengine.definition.activeobjects.ProcessDefinitionScopedElement;

public class ProcessDefinitionScopeType  implements TypeProvider {

	public static enum CoreProperties {process, orderIndex};
	private SchemaRegistry schemaRegistry;
	public static final String typeId = ProcessDefinitionScopeType.class.getSimpleName();
	
	public ProcessDefinitionScopeType(SchemaRegistry schemaRegistry) {
		this.schemaRegistry = schemaRegistry;
	}
	
	@Override
	public void registerTypeInFactory(ProcessDomainTypesRegistry factory) {
		Optional<InstanceType> thisType = schemaRegistry.findNonDeletedInstanceTypeById(typeId);
		if (thisType.isPresent())
			factory.registerType(ProcessDefinitionScopedElement.class, thisType.get());
		else {
			InstanceType type = schemaRegistry.createNewInstanceType(typeId);
			factory.registerType(ProcessDefinitionScopedElement.class, type);
			type.createSinglePropertyType(ProcessDefinitionScopeType.CoreProperties.process.toString(), type);
			type.createSinglePropertyType((ProcessDefinitionScopeType.CoreProperties.orderIndex.toString()), BuildInType.INTEGER);
			
		}
	}
}
