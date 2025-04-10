package at.jku.isse.passiveprocessengine.instance.types;

import java.util.Optional;

import at.jku.isse.passiveprocessengine.core.BuildInType;
import at.jku.isse.passiveprocessengine.rdfwrapper.RDFInstanceType;
import at.jku.isse.passiveprocessengine.core.SchemaRegistry;
import at.jku.isse.passiveprocessengine.core.TypeProviderBase;
import at.jku.isse.passiveprocessengine.rdfwrapper.RDFInstanceType;


public class ProcessConfigBaseElementType extends TypeProviderBase {

	public static enum CoreProperties {description}; 
	public static final String typeId = "process_config_base";
	
	public ProcessConfigBaseElementType(SchemaRegistry schemaRegistry) {
		super(schemaRegistry);
		Optional<RDFInstanceType> thisType = schemaRegistry.findNonDeletedInstanceTypeByFQN(typeId);
		if (thisType.isPresent()) {
			//schemaRegistry.registerTypeByName(thisType.get());
			this.type = thisType.get();
		} else {
			type = schemaRegistry.createNewInstanceType(typeId, schemaRegistry.getTypeByName(ProcessInstanceScopeType.typeId));
		//	schemaRegistry.registerTypeByName(type);			
		}
	}

	@Override
	public void produceTypeProperties() {
		((RDFInstanceType) type).cacheSuperProperties();	
		type.createSinglePropertyType(CoreProperties.description.toString(), BuildInType.STRING);		
			ProcessInstanceScopeType.addGenericProcessProperty(type, schemaRegistry);
			
	}
}
