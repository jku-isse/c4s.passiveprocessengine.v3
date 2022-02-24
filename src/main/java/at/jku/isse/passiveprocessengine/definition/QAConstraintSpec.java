package at.jku.isse.passiveprocessengine.definition;

import java.util.Optional;

import at.jku.isse.designspace.core.model.Cardinality;
import at.jku.isse.designspace.core.model.Instance;
import at.jku.isse.designspace.core.model.InstanceType;
import at.jku.isse.designspace.core.model.Workspace;
import at.jku.isse.passiveprocessengine.InstanceWrapper;

public class QAConstraintSpec extends InstanceWrapper{

	
	public static enum CoreProperties {qaConstraintSpec, humanReadableDescription};
	public static final String designspaceTypeId = QAConstraintSpec.class.getSimpleName();

	public QAConstraintSpec(String qaConstraintId, String qaConstraintSpec, String humanReadableDescription, Workspace ws) {
		super(ws.createInstance(getOrCreateDesignSpaceCoreSchema(ws), qaConstraintId));
		instance.getPropertyAsSingle(CoreProperties.qaConstraintSpec.toString()).set(qaConstraintSpec);
		instance.getPropertyAsSingle(CoreProperties.humanReadableDescription.toString()).set(humanReadableDescription);
	}
	
	public QAConstraintSpec(Instance instance) {
		super(instance);
	}

	public String getQaConstraintId() {
		return instance.name();
	}

	public String getQaConstraintSpec() {
		return (String) instance.getPropertyAsValue(CoreProperties.qaConstraintSpec.toString());
	}

	public String getHumanReadableDescription() {
		return (String) instance.getPropertyAsValue(CoreProperties.humanReadableDescription.toString());
	}
	
	public static InstanceType getOrCreateDesignSpaceCoreSchema(Workspace ws) {
		Optional<InstanceType> thisType = ws.debugInstanceTypes().stream()
				.filter(it -> it.name().contentEquals(designspaceTypeId))
				.findAny();
			if (thisType.isPresent())
				return thisType.get();
			else {
				InstanceType typeStep = ws.createInstanceType(designspaceTypeId, ws.ROOT_FOLDER);
				// constraintId maps to Instance name property
				typeStep.createPropertyType(CoreProperties.qaConstraintSpec.toString(), Cardinality.SINGLE, Workspace.STRING);
				typeStep.createPropertyType(CoreProperties.humanReadableDescription.toString(), Cardinality.SINGLE, Workspace.STRING);
				return typeStep;
			}
	}

}
