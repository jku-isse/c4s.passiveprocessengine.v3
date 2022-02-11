package at.jku.isse.passiveprocessengine.definition;

import java.util.Optional;

import at.jku.isse.designspace.sdk.core.model.Cardinality;
import at.jku.isse.designspace.sdk.core.model.Instance;
import at.jku.isse.designspace.sdk.core.model.InstanceType;
import at.jku.isse.designspace.sdk.core.model.Workspace;
import at.jku.isse.passiveprocessengine.InstanceWrapper;
import at.jku.isse.passiveprocessengine.instance.ProcessStep;

public class QAConstraintSpec implements InstanceWrapper{

	
	public static enum CoreProperties {qaConstraintSpec, humanReadableDescription};
	public static final String designspaceTypeId = QAConstraintSpec.class.getSimpleName();
	
	private transient Instance instance;

	public QAConstraintSpec(String qaConstraintId, String qaConstraintSpec, String humanReadableDescription, Workspace ws) {
		super();
		this.instance = ws.createInstance(qaConstraintId, getOrCreateDesignSpaceCoreSchema(ws));
		instance.propertyAsSingle(CoreProperties.qaConstraintSpec.toString()).set(qaConstraintSpec);
		instance.propertyAsSingle(CoreProperties.humanReadableDescription.toString()).set(humanReadableDescription);
	}
	
	public QAConstraintSpec(Instance instance) {
		this.instance = instance;
	}

	public String getQaConstraintId() {
		return instance.name();
	}

	public String getQaConstraintSpec() {
		return (String) instance.propertyAsValue(CoreProperties.qaConstraintSpec.toString());
	}

	public String getHumanReadableDescription() {
		return (String) instance.propertyAsValue(CoreProperties.humanReadableDescription.toString());
	}
	
	public static InstanceType getOrCreateDesignSpaceCoreSchema(Workspace ws) {
		Optional<InstanceType> thisType = ws.debugInstanceTypes().stream()
				.filter(it -> it.name().contentEquals(designspaceTypeId))
				.findAny();
			if (thisType.isPresent())
				return thisType.get();
			else {
				InstanceType typeStep = ws.createInstanceType(designspaceTypeId);
				// constraintId maps to Instance name property
				typeStep.createPropertyType(CoreProperties.qaConstraintSpec.toString(), Cardinality.SINGLE, ws.STRING);
				typeStep.createPropertyType(CoreProperties.humanReadableDescription.toString(), Cardinality.SINGLE, ws.STRING);
				return typeStep;
			}
	}

	@Override
	public Instance getInstance() {
		return instance;
	}
}
