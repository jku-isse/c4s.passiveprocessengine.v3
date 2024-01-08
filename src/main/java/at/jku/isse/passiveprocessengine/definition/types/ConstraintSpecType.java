package at.jku.isse.passiveprocessengine.definition.types;

import java.util.Optional;

import at.jku.isse.passiveprocessengine.core.BuildInType;
import at.jku.isse.passiveprocessengine.core.InstanceType;
import at.jku.isse.passiveprocessengine.core.SchemaRegistry;
import at.jku.isse.passiveprocessengine.definition.activeobjects.ConstraintSpec;
import at.jku.isse.passiveprocessengine.definition.activeobjects.ProcessDefinitionScopedElement;
import at.jku.isse.passiveprocessengine.definition.types.ProcessDomainTypesFactory.TypeProvider;

public class ConstraintSpecType implements TypeProvider {

	public static enum CoreProperties {constraintSpec, humanReadableDescription, specOrderIndex, isOverridable, ruleType, conditionsType}

	private SchemaRegistry schemaRegistry;
	public static final String typeId = ConstraintSpecType.class.getSimpleName();
	
	public ConstraintSpecType(SchemaRegistry schemaRegistry) {
		this.schemaRegistry = schemaRegistry;
	}
	
	@Override
	public void registerTypeInFactory(ProcessDomainTypesFactory factory) {
		Optional<InstanceType> thisType = schemaRegistry.findNonDeletedInstanceTypeById(typeId);
		if (thisType.isEmpty())
			factory.registerType(ConstraintSpec.class, thisType.get());
		else {
			InstanceType specType = schemaRegistry.createNewInstanceType(typeId, factory.getType(ProcessDefinitionScopedElement.class));
			factory.registerType(ConstraintSpec.class, specType);
			// constraintId maps to Instance name property
			specType.createSinglePropertyType(CoreProperties.constraintSpec.toString(), BuildInType.STRING);
			specType.createSinglePropertyType(CoreProperties.humanReadableDescription.toString(),  BuildInType.STRING);
			specType.createSinglePropertyType(CoreProperties.specOrderIndex.toString(),  BuildInType.INTEGER);
			specType.createSinglePropertyType(CoreProperties.isOverridable.toString(),  BuildInType.BOOLEAN);
			specType.createSinglePropertyType(CoreProperties.ruleType.toString(),  BuildInType.RULE);
			specType.createSinglePropertyType(CoreProperties.conditionsType.toString(),  BuildInType.STRING);
		}
	}

}
