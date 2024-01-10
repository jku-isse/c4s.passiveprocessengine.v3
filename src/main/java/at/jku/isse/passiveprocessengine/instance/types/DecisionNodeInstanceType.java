package at.jku.isse.passiveprocessengine.instance.types;

import java.util.Optional;

import at.jku.isse.passiveprocessengine.core.BuildInType;
import at.jku.isse.passiveprocessengine.core.InstanceType;
import at.jku.isse.passiveprocessengine.core.SchemaRegistry;
import at.jku.isse.passiveprocessengine.definition.activeobjects.DecisionNodeDefinition;
import at.jku.isse.passiveprocessengine.definition.types.ProcessDomainTypesRegistry;
import at.jku.isse.passiveprocessengine.definition.types.ProcessDomainTypesRegistry.TypeProvider;
import at.jku.isse.passiveprocessengine.instance.activeobjects.DecisionNodeInstance;
import at.jku.isse.passiveprocessengine.instance.activeobjects.ProcessInstanceScopedElement;
import at.jku.isse.passiveprocessengine.instance.activeobjects.ProcessStep;

public class DecisionNodeInstanceType implements TypeProvider {

	
	public static enum CoreProperties {isInflowFulfilled, hasPropagated, dnd, inSteps, outSteps, closingDN}

	private SchemaRegistry schemaRegistry;
	public static final String typeId = DecisionNodeInstance.class.getSimpleName();
	
	public DecisionNodeInstanceType(SchemaRegistry schemaRegistry) {
		this.schemaRegistry = schemaRegistry;
	}

	@Override
	public void registerTypeInFactory(ProcessDomainTypesRegistry factory) {
		Optional<InstanceType> thisType = schemaRegistry.findNonDeletedInstanceTypeById(typeId);
		if (thisType.isPresent())
			factory.registerType(DecisionNodeInstance.class, thisType.get());
		else {
			InstanceType type = schemaRegistry.createNewInstanceType(typeId, factory.getType(ProcessInstanceScopedElement.class));
			factory.registerType(DecisionNodeInstance.class, type);
			
			ProcessInstanceScopeType.addGenericProcessProperty(type, factory);
			type.createSinglePropertyType(CoreProperties.isInflowFulfilled.toString(),  BuildInType.BOOLEAN);
			type.createSinglePropertyType(CoreProperties.hasPropagated.toString(), BuildInType.BOOLEAN);
			type.createSinglePropertyType(CoreProperties.dnd.toString(), factory.getType(DecisionNodeDefinition.class));
			type.createSetPropertyType(CoreProperties.inSteps.toString(), factory.getType(ProcessStep.class));
			type.createSetPropertyType(CoreProperties.outSteps.toString(), factory.getType(ProcessStep.class));
			type.createSinglePropertyType(CoreProperties.closingDN.toString(), type);
		}
	}	
}
