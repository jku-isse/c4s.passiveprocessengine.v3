package at.jku.isse.passiveprocessengine.instance.types;

import java.util.Optional;
import at.jku.isse.passiveprocessengine.core.BuildInType;
import at.jku.isse.passiveprocessengine.core.InstanceType;
import at.jku.isse.passiveprocessengine.core.ProcessDomainTypesRegistry;
import at.jku.isse.passiveprocessengine.core.SchemaRegistry;
import at.jku.isse.passiveprocessengine.core.ProcessDomainTypesRegistry.TypeProvider;
import at.jku.isse.passiveprocessengine.instance.activeobjects.ProcessInstanceScopedElement;


public class ProcessConfigBaseElementType implements TypeProvider {

	public static enum CoreProperties {description}; 
	public static final String typeId = "process_config_base";
	private SchemaRegistry schemaRegistry;
	
	public ProcessConfigBaseElementType(SchemaRegistry schemaRegistry) {

		this.schemaRegistry = schemaRegistry;
		
	}

	@Override
	public void registerTypeInFactory(ProcessDomainTypesRegistry factory) {
		Optional<InstanceType> thisType = schemaRegistry.findNonDeletedInstanceTypeById(typeId);
		if (thisType.isPresent())
			factory.registerTypeByName(thisType.get());
		else {
			InstanceType type = schemaRegistry.createNewInstanceType(typeId, factory.getType(ProcessInstanceScopedElement.class));
			factory.registerTypeByName(type);			
			
			type.createSinglePropertyType(CoreProperties.description.toString(), BuildInType.STRING);		
			ProcessInstanceScopeType.addGenericProcessProperty(type, factory);
		}		
	}
	
	




}
