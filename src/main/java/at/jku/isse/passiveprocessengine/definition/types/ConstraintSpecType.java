package at.jku.isse.passiveprocessengine.definition.types;

import java.util.Optional;

import at.jku.isse.passiveprocessengine.core.BuildInType;
import at.jku.isse.passiveprocessengine.core.PPEInstanceType;
import at.jku.isse.passiveprocessengine.core.SchemaRegistry;
import at.jku.isse.passiveprocessengine.core.TypeProvider;
import at.jku.isse.passiveprocessengine.definition.activeobjects.ConstraintSpec;
import at.jku.isse.passiveprocessengine.definition.activeobjects.ProcessDefinitionScopedElement;

public class ConstraintSpecType implements TypeProvider {

	public static enum CoreProperties {constraintSpec, augmentedSpec, humanReadableDescription, constraintSpecOrderIndex, isOverridable, ruleType, conditionsType}

	private SchemaRegistry schemaRegistry;
	public static final String typeId = ConstraintSpecType.class.getSimpleName();
	private final PPEInstanceType type;
	
	public ConstraintSpecType(SchemaRegistry schemaRegistry) {
		this.schemaRegistry = schemaRegistry;
		Optional<PPEInstanceType> thisType = schemaRegistry.findNonDeletedInstanceTypeByFQN(typeId);
		if (thisType.isPresent()) {
			//schemaRegistry.registerType(ConstraintSpec.class, thisType.get());
			this.type = thisType.get();
		} else {
			//PPEInstanceType type = schemaRegistry.createNewInstanceType(typeId, schemaRegistry.getType(ProcessDefinitionScopedElement.class));
			PPEInstanceType type = schemaRegistry.createNewInstanceType(typeId, schemaRegistry.getTypeByName(ProcessDefinitionScopeType.typeId));			
			//schemaRegistry.registerType(ConstraintSpec.class, type);
			this.type = type;
		}
	}
	
	@Override
	public void produceTypeProperties() {
		// constraintId maps to Instance name property
		type.createSinglePropertyType(CoreProperties.constraintSpec.toString(), BuildInType.STRING);
		type.createSinglePropertyType(CoreProperties.augmentedSpec.toString(),  BuildInType.STRING);
		type.createSinglePropertyType(CoreProperties.humanReadableDescription.toString(),  BuildInType.STRING);
		type.createSinglePropertyType(CoreProperties.constraintSpecOrderIndex.toString(),  BuildInType.INTEGER);
		type.createSinglePropertyType(CoreProperties.isOverridable.toString(),  BuildInType.BOOLEAN);
		type.createSinglePropertyType(CoreProperties.ruleType.toString(),  BuildInType.RULE);
		type.createSinglePropertyType(CoreProperties.conditionsType.toString(),  BuildInType.STRING);
	}

}
