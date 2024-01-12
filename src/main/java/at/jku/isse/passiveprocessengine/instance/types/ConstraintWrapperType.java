package at.jku.isse.passiveprocessengine.instance.types;

import java.util.Optional;

import at.jku.isse.passiveprocessengine.core.BuildInType;
import at.jku.isse.passiveprocessengine.core.InstanceType;
import at.jku.isse.passiveprocessengine.core.SchemaRegistry;
import at.jku.isse.passiveprocessengine.core.TypeProvider;
import at.jku.isse.passiveprocessengine.definition.activeobjects.ConstraintSpec;
import at.jku.isse.passiveprocessengine.instance.activeobjects.ConstraintResultWrapper;
import at.jku.isse.passiveprocessengine.instance.activeobjects.ProcessInstanceScopedElement;
import at.jku.isse.passiveprocessengine.instance.activeobjects.ProcessStep;

public class ConstraintWrapperType implements TypeProvider {
	
	public static enum CoreProperties {qaSpec, lastChanged, crule, parentStep, isOverriden, overrideValue, overrideReason}
	public static final String typeId = ConstraintResultWrapper.class.getSimpleName();

	private SchemaRegistry schemaRegistry;
	
	public ConstraintWrapperType(SchemaRegistry schemaRegistry) {
		this.schemaRegistry = schemaRegistry;
	}

	@Override
	public void produceTypeProperties() {
		Optional<InstanceType> thisType = schemaRegistry.findNonDeletedInstanceTypeById(typeId);
		if (thisType.isPresent())
			factory.registerType(ConstraintResultWrapper.class, thisType.get());
		else {
			InstanceType type = schemaRegistry.createNewInstanceType(typeId, factory.getType(ProcessInstanceScopedElement.class));
			factory.registerType(ConstraintResultWrapper.class, type);
			
			// so ugly:
			ProcessInstanceScopeType.addGenericProcessProperty(type, factory);
			type.createSinglePropertyType(ConstraintWrapperType.CoreProperties.qaSpec.toString(), factory.getType(ConstraintSpec.class));
			type.createSinglePropertyType(ConstraintWrapperType.CoreProperties.parentStep.toString(), factory.getType(ProcessStep.class));
			type.createSinglePropertyType(ConstraintWrapperType.CoreProperties.lastChanged.toString(), BuildInType.STRING);
			type.createSinglePropertyType(ConstraintWrapperType.CoreProperties.crule.toString(), BuildInType.RULE);
			type.createSinglePropertyType(ConstraintWrapperType.CoreProperties.isOverriden.toString(),  BuildInType.BOOLEAN);
			type.createSinglePropertyType(ConstraintWrapperType.CoreProperties.overrideValue.toString(),  BuildInType.BOOLEAN);
			type.createSinglePropertyType(ConstraintWrapperType.CoreProperties.overrideReason.toString(), BuildInType.STRING);
			
			
			}
	}


}
