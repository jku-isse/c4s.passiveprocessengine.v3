package at.jku.isse.passiveprocessengine.instance.types;

import java.util.Optional;

import at.jku.isse.passiveprocessengine.core.DomainTypesRegistry;
import at.jku.isse.passiveprocessengine.core.PPEInstanceType;
import at.jku.isse.passiveprocessengine.core.SchemaRegistry;
import at.jku.isse.passiveprocessengine.core.TypeProviderBase;
import at.jku.isse.passiveprocessengine.instance.activeobjects.ProcessInstanceScopedElement;
import at.jku.isse.passiveprocessengine.instance.activeobjects.ProcessStep;

public class ProcessInstanceScopeType extends TypeProviderBase {

	public static enum CoreProperties {process};
	public static final String typeId = ProcessInstanceScopedElement.class.getSimpleName();

	public ProcessInstanceScopeType(SchemaRegistry schemaRegistry) {
		super(schemaRegistry);
		Optional<PPEInstanceType> thisType = schemaRegistry.findNonDeletedInstanceTypeByFQN(typeId);
		if (thisType.isPresent()) {
			schemaRegistry.registerType(ProcessInstanceScopedElement.class, thisType.get());
			this.type = thisType.get();
		} else {
			type = schemaRegistry.createNewInstanceType(typeId);
			schemaRegistry.registerType(ProcessInstanceScopedElement.class, type);
		}
	}
	
	
	@Override
	public void produceTypeProperties() {
		//type.createPropertyType(CoreProperties.process.toString(), Cardinality.SINGLE, typeStep); needs to be add in individual subclasses in order to be able to refine it					
	}
	
	public static void addGenericProcessProperty(PPEInstanceType instType, DomainTypesRegistry schemaRegistry) {
		if (instType.getPropertyType(ProcessInstanceScopeType.CoreProperties.process.toString()) == null) {
			instType.createSinglePropertyType(ProcessInstanceScopeType.CoreProperties.process.toString(), schemaRegistry.getType(ProcessStep.class));
		}
	}
}
