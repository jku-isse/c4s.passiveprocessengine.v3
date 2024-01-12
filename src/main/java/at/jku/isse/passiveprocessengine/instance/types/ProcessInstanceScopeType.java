package at.jku.isse.passiveprocessengine.instance.types;

import java.util.Optional;

import at.jku.isse.passiveprocessengine.core.DomainTypesRegistry;
import at.jku.isse.passiveprocessengine.core.InstanceType;
import at.jku.isse.passiveprocessengine.core.SchemaRegistry;
import at.jku.isse.passiveprocessengine.core.TypeProvider;
import at.jku.isse.passiveprocessengine.instance.activeobjects.ProcessInstanceScopedElement;
import at.jku.isse.passiveprocessengine.instance.activeobjects.ProcessStep;

public class ProcessInstanceScopeType implements TypeProvider {

	public static enum CoreProperties {process};
	public static final String typeId = ProcessInstanceScopedElement.class.getSimpleName();

	private SchemaRegistry schemaRegistry;

	public ProcessInstanceScopeType(SchemaRegistry schemaRegistry) {
		this.schemaRegistry = schemaRegistry;
	}
	
	
	@Override
	public void produceTypeProperties() {
		Optional<InstanceType> thisType = schemaRegistry.findNonDeletedInstanceTypeById(typeId);
		if (thisType.isPresent())
			factory.registerType(ProcessInstanceScopedElement.class, thisType.get());
		else {
			InstanceType type = schemaRegistry.createNewInstanceType(typeId);
			factory.registerType(ProcessInstanceScopedElement.class, type);
			//type.createPropertyType(CoreProperties.process.toString(), Cardinality.SINGLE, typeStep); needs to be add in individual subclasses in order to be able to refine it					
		}
	}
	
	public static void addGenericProcessProperty(InstanceType instType, DomainTypesRegistry factory) {
		if (instType.getPropertyType(ProcessInstanceScopeType.CoreProperties.process.toString()) == null) {
			instType.createSinglePropertyType(ProcessInstanceScopeType.CoreProperties.process.toString(), factory.getType(ProcessStep.class));
		}
	}
}
