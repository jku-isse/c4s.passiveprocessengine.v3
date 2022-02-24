package at.jku.isse.passiveprocessengine.definition;

import java.util.Optional;

import at.jku.isse.designspace.core.model.Cardinality;
import at.jku.isse.designspace.core.model.Instance;
import at.jku.isse.designspace.core.model.InstanceType;
import at.jku.isse.designspace.core.model.Workspace;
import at.jku.isse.passiveprocessengine.InstanceWrapper;
import at.jku.isse.passiveprocessengine.WrapperCache;

public class DecisionNodeDefinition extends InstanceWrapper {

public static enum CoreProperties {inFlowType, dataMappingDefinitions}
	
	public static final String designspaceTypeId = DecisionNodeDefinition.class.getSimpleName();
	
	public DecisionNodeDefinition(Instance instance) {
		super(instance);
	}

	
	public static InstanceType getOrCreateDesignSpaceCoreSchema(Workspace ws) {
		Optional<InstanceType> thisType = ws.debugInstanceTypes().stream()
				.filter(it -> it.name().contentEquals(designspaceTypeId))
				.findAny();
			if (thisType.isPresent())
				return thisType.get();
			else {
				InstanceType typeStep = ws.createInstanceType(designspaceTypeId, ws.TYPES_FOLDER);
				typeStep.createPropertyType(CoreProperties.inFlowType.toString(), Cardinality.SINGLE, Workspace.STRING);
				// todo DATAMAPPINGS
				return typeStep;
			}
	}

	public static DecisionNodeDefinition getInstance(String dndId, Workspace ws) {
		Instance instance = ws.createInstance(getOrCreateDesignSpaceCoreSchema(ws), dndId);
		return WrapperCache.getWrappedInstance(DecisionNodeDefinition.class, instance);
	}
	
}
