package at.jku.isse.passiveprocessengine.instance.types;

import java.util.Optional;

import at.jku.isse.passiveprocessengine.core.BuildInType;
import at.jku.isse.passiveprocessengine.core.PPEInstanceType;
import at.jku.isse.passiveprocessengine.core.SchemaRegistry;
import at.jku.isse.passiveprocessengine.core.TypeProviderBase;
import at.jku.isse.passiveprocessengine.definition.activeobjects.DecisionNodeDefinition;
import at.jku.isse.passiveprocessengine.instance.activeobjects.DecisionNodeInstance;
import at.jku.isse.passiveprocessengine.instance.activeobjects.ProcessInstanceScopedElement;
import at.jku.isse.passiveprocessengine.instance.activeobjects.ProcessStep;

public class DecisionNodeInstanceType extends TypeProviderBase {

	
	public static enum CoreProperties {isInflowFulfilled, hasPropagated, dnd, inSteps, outSteps, closingDN}

	public static final String typeId = DecisionNodeInstance.class.getSimpleName();
	
	public DecisionNodeInstanceType(SchemaRegistry schemaRegistry) {
		super(schemaRegistry);
		Optional<PPEInstanceType> thisType = schemaRegistry.findNonDeletedInstanceTypeByFQN(typeId);
		if (thisType.isPresent()) {
			schemaRegistry.registerType(DecisionNodeInstance.class, thisType.get());
			this.type = thisType.get();
		} else {
			type = schemaRegistry.createNewInstanceType(typeId, schemaRegistry.getType(ProcessInstanceScopedElement.class));
			schemaRegistry.registerType(DecisionNodeInstance.class, type);	
		}
	}

	@Override
	public void produceTypeProperties() {
		ProcessInstanceScopeType.addGenericProcessProperty(type, schemaRegistry);
			type.createSinglePropertyType(CoreProperties.isInflowFulfilled.toString(),  BuildInType.BOOLEAN);
			type.createSinglePropertyType(CoreProperties.hasPropagated.toString(), BuildInType.BOOLEAN);
			type.createSinglePropertyType(CoreProperties.dnd.toString(), schemaRegistry.getType(DecisionNodeDefinition.class));
			type.createSetPropertyType(CoreProperties.inSteps.toString(), schemaRegistry.getType(ProcessStep.class));
			type.createSetPropertyType(CoreProperties.outSteps.toString(), schemaRegistry.getType(ProcessStep.class));
			type.createSinglePropertyType(CoreProperties.closingDN.toString(), type);
	}	
}
