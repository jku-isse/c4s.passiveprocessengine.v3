package at.jku.isse.passiveprocessengine.instance.types;

import java.util.Optional;

import at.jku.isse.passiveprocessengine.core.BuildInType;
import at.jku.isse.passiveprocessengine.core.InstanceType;
import at.jku.isse.passiveprocessengine.core.SchemaRegistry;
import at.jku.isse.passiveprocessengine.core.TypeProviderBase;
import at.jku.isse.passiveprocessengine.definition.activeobjects.ConstraintSpec;
import at.jku.isse.passiveprocessengine.instance.activeobjects.ConstraintResultWrapper;
import at.jku.isse.passiveprocessengine.instance.activeobjects.ProcessInstanceScopedElement;
import at.jku.isse.passiveprocessengine.instance.activeobjects.ProcessStep;

public class ConstraintWrapperType extends TypeProviderBase {
	
	public static enum CoreProperties {qaSpec, lastChanged, crule, parentStep, isOverriden, overrideValue, overrideReason}
	public static final String typeId = ConstraintResultWrapper.class.getSimpleName();

	
	public ConstraintWrapperType(SchemaRegistry schemaRegistry) {
		super(schemaRegistry);
		Optional<InstanceType> thisType = schemaRegistry.findNonDeletedInstanceTypeById(typeId);
		if (thisType.isPresent()) {
			schemaRegistry.registerType(ConstraintResultWrapper.class, thisType.get());
			this.type = thisType.get();
		} else {
			type = schemaRegistry.createNewInstanceType(typeId, schemaRegistry.getType(ProcessInstanceScopedElement.class));
			schemaRegistry.registerType(ConstraintResultWrapper.class, type);
		}
	}

	@Override
	public void produceTypeProperties() {
		
			// so ugly:
			ProcessInstanceScopeType.addGenericProcessProperty(type, schemaRegistry);
			type.createSinglePropertyType(ConstraintWrapperType.CoreProperties.qaSpec.toString(), schemaRegistry.getType(ConstraintSpec.class));
			type.createSinglePropertyType(ConstraintWrapperType.CoreProperties.parentStep.toString(), schemaRegistry.getType(ProcessStep.class));
			type.createSinglePropertyType(ConstraintWrapperType.CoreProperties.lastChanged.toString(), BuildInType.STRING);
			type.createSinglePropertyType(ConstraintWrapperType.CoreProperties.crule.toString(), BuildInType.RULE);
			type.createSinglePropertyType(ConstraintWrapperType.CoreProperties.isOverriden.toString(),  BuildInType.BOOLEAN);
			type.createSinglePropertyType(ConstraintWrapperType.CoreProperties.overrideValue.toString(),  BuildInType.BOOLEAN);
			type.createSinglePropertyType(ConstraintWrapperType.CoreProperties.overrideReason.toString(), BuildInType.STRING);
	}


}
