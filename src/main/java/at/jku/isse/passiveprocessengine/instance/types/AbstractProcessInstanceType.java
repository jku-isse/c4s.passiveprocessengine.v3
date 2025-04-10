package at.jku.isse.passiveprocessengine.instance.types;

import java.util.Optional;

import at.jku.isse.passiveprocessengine.rdfwrapper.RDFInstanceType;
import at.jku.isse.passiveprocessengine.core.SchemaRegistry;
import at.jku.isse.passiveprocessengine.core.TypeProviderBase;
import at.jku.isse.passiveprocessengine.instance.activeobjects.ProcessInstance;
import at.jku.isse.passiveprocessengine.rdfwrapper.RDFInstanceType;

public class AbstractProcessInstanceType extends TypeProviderBase {

	public static final String typeId = ProcessInstance.class.getSimpleName();

	public AbstractProcessInstanceType(SchemaRegistry schemaRegistry) {
		super(schemaRegistry);
		Optional<RDFInstanceType> thisType = schemaRegistry.findNonDeletedInstanceTypeByFQN(typeId);
		if (thisType.isPresent()) {
			//schemaRegistry.registerType(ProcessStep.class, thisType.get());
			this.type = thisType.get();
		} else {
			type = schemaRegistry.createNewInstanceType(typeId, schemaRegistry.getTypeByName(AbstractProcessStepType.typeId));
			//schemaRegistry.registerType(ProcessInstance.class, type);	
		}
	}

	@Override
	public void produceTypeProperties() {
		// none to create, we just need to have a base process instance type
		((RDFInstanceType) type).cacheSuperProperties();
	}
}
