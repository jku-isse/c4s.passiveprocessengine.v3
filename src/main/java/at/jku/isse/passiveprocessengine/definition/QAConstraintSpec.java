package at.jku.isse.passiveprocessengine.definition;

import java.util.Optional;

import at.jku.isse.designspace.core.model.Cardinality;
import at.jku.isse.designspace.core.model.Instance;
import at.jku.isse.designspace.core.model.InstanceType;
import at.jku.isse.designspace.core.model.Workspace;
import at.jku.isse.passiveprocessengine.ProcessDefinitionScopedElement;
import at.jku.isse.passiveprocessengine.WrapperCache;

public class QAConstraintSpec extends /*InstanceWrapper*/ ProcessDefinitionScopedElement{

	
	public static enum CoreProperties {qaConstraintSpec, humanReadableDescription, specOrderIndex};
	public static final String designspaceTypeId = QAConstraintSpec.class.getSimpleName();
	
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
	
	public Integer getOrderIndex() {
		return (Integer) instance.getPropertyAsValue(CoreProperties.specOrderIndex.toString());
	}
	
	@Override
	public void deleteCascading() {
		instance.delete();
	}
	
	public static InstanceType getOrCreateDesignSpaceCoreSchema(Workspace ws) {
		Optional<InstanceType> thisType = Optional.ofNullable(ws.TYPES_FOLDER.instanceTypeWithName(designspaceTypeId)); 
//		Optional<InstanceType> thisType = ws.debugInstanceTypes().stream()
//				.filter(it -> it.name().contentEquals(designspaceTypeId))
//				.findAny();
			if (thisType.isPresent())
				return thisType.get();
			else {
				InstanceType typeStep = ws.createInstanceType(designspaceTypeId, ws.TYPES_FOLDER, ProcessDefinitionScopedElement.getOrCreateDesignSpaceCoreSchema(ws));
				// constraintId maps to Instance name property
				typeStep.createPropertyType(CoreProperties.qaConstraintSpec.toString(), Cardinality.SINGLE, Workspace.STRING);
				typeStep.createPropertyType(CoreProperties.humanReadableDescription.toString(), Cardinality.SINGLE, Workspace.STRING);
				typeStep.createPropertyType((CoreProperties.specOrderIndex.toString()), Cardinality.SINGLE, Workspace.INTEGER);
				return typeStep;
			}
	}
	
	public static QAConstraintSpec createInstance(String qaConstraintId, String qaConstraintSpec, String humanReadableDescription, int specOrderIndex, Workspace ws) {
		Instance instance = ws.createInstance(getOrCreateDesignSpaceCoreSchema(ws), qaConstraintId);
		instance.getPropertyAsSingle(CoreProperties.qaConstraintSpec.toString()).set(qaConstraintSpec);
		instance.getPropertyAsSingle(CoreProperties.humanReadableDescription.toString()).set(humanReadableDescription);
		instance.getPropertyAsSingle(CoreProperties.specOrderIndex.toString()).set(specOrderIndex);
		return WrapperCache.getWrappedInstance(QAConstraintSpec.class, instance);
	}



}
