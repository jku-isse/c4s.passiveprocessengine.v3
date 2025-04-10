package at.jku.isse.passiveprocessengine.instance.types;

import java.util.Optional;

import at.jku.isse.passiveprocessengine.core.BuildInType;
import at.jku.isse.passiveprocessengine.rdfwrapper.RDFInstanceType;
import at.jku.isse.passiveprocessengine.core.SchemaRegistry;
import at.jku.isse.passiveprocessengine.core.TypeProviderBase;
import at.jku.isse.passiveprocessengine.definition.types.DecisionNodeDefinitionType;
import at.jku.isse.passiveprocessengine.instance.activeobjects.DecisionNodeInstance;
import at.jku.isse.passiveprocessengine.rdfwrapper.RDFInstanceType;

public class DecisionNodeInstanceType extends TypeProviderBase {

	
	public static enum CoreProperties {isInflowFulfilled, hasPropagated, dnd, inSteps, outSteps, closingDN}

	public static final String typeId = DecisionNodeInstance.class.getSimpleName();
	
	public DecisionNodeInstanceType(SchemaRegistry schemaRegistry) {
		super(schemaRegistry);
		Optional<RDFInstanceType> thisType = schemaRegistry.findNonDeletedInstanceTypeByFQN(typeId);
		if (thisType.isPresent()) {
			//schemaRegistry.registerType(DecisionNodeInstance.class, thisType.get());
			this.type = thisType.get();
		} else {
			type = schemaRegistry.createNewInstanceType(typeId, schemaRegistry.getTypeByName(ProcessInstanceScopeType.typeId));
			//schemaRegistry.registerType(DecisionNodeInstance.class, type);	
		}
	}

	@Override
	public void produceTypeProperties() {
		((RDFInstanceType) type).cacheSuperProperties();
		ProcessInstanceScopeType.addGenericProcessProperty(type, schemaRegistry);
			type.createSinglePropertyType(CoreProperties.isInflowFulfilled.toString(),  BuildInType.BOOLEAN);
			type.createSinglePropertyType(CoreProperties.hasPropagated.toString(), BuildInType.BOOLEAN);
			type.createSinglePropertyType(CoreProperties.dnd.toString(), schemaRegistry.getTypeByName(DecisionNodeDefinitionType.typeId));
			type.createSetPropertyType(CoreProperties.inSteps.toString(), schemaRegistry.getTypeByName(AbstractProcessStepType.typeId));
			type.createSetPropertyType(CoreProperties.outSteps.toString(), schemaRegistry.getTypeByName(AbstractProcessStepType.typeId));
			type.createSinglePropertyType(CoreProperties.closingDN.toString(), type);
			
			
	}	
}
