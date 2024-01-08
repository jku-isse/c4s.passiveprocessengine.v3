package at.jku.isse.passiveprocessengine.definition;

import java.util.Optional;

import at.jku.isse.designspace.core.model.Cardinality;
import at.jku.isse.designspace.core.model.Instance;
import at.jku.isse.designspace.core.model.InstanceType;
import at.jku.isse.designspace.core.model.Workspace;
import at.jku.isse.passiveprocessengine.InstanceWrapper;
import at.jku.isse.passiveprocessengine.WrapperCache;
import at.jku.isse.passiveprocessengine.configurability.ProcessConfigBaseElementFactory;

public class MappingDefinition extends InstanceWrapper{

//	public enum FlowDir { outToIn, //from Step output to Step input
//		inToIn, // from process input to Step input
//		outToOut, // from Step output to process output
//		inToOut //from process input to process output
//		};
	
	public static enum CoreProperties {fromStepType, fromParameter, toStepType, toParameter, flowDir}
	
	public static final String designspaceTypeId = MappingDefinition.class.getSimpleName();
	
	public MappingDefinition(Instance instance) {
		super(instance);
	}
	
	public String getFromStepType() {
		return (String) instance.getPropertyAsValueOrNull(CoreProperties.fromStepType.toString());
	}

	public void setFromStepType(String fromStepType) {
		instance.getPropertyAsSingle(CoreProperties.fromStepType.toString()).set(fromStepType);
	}

	public String getFromParameter() {
		return (String) instance.getPropertyAsValueOrNull(CoreProperties.fromParameter.toString());
	}

	public void setFromParameter(String fromParameter) {
		instance.getPropertyAsSingle(CoreProperties.fromParameter.toString()).set(fromParameter);
	}

	public String getToStepType() {
		return (String) instance.getPropertyAsValueOrNull(CoreProperties.toStepType.toString());
	}

	public void setToStepType(String toStepType) {
		instance.getPropertyAsSingle(CoreProperties.toStepType.toString()).set(toStepType);
	}

	public String getToParameter() {
		return (String) instance.getPropertyAsValueOrNull(CoreProperties.toParameter.toString());
	}

	public void setToParameter(String toParameter) {
		instance.getPropertyAsSingle(CoreProperties.toParameter.toString()).set(toParameter);
	}
	
	@Override
	public void deleteCascading(ProcessConfigBaseElementFactory configFactory) {
		super.deleteCascading(configFactory);
	}
	
//	public FlowDir getFlowDir() {
//		return FlowDir.valueOf((String) instance.getPropertyAsValueOrNull(CoreProperties.flowDir.toString()));
//	}
//
//	public void setFlowDir(FlowDir flowDir) {
//		instance.getPropertyAsSingle(CoreProperties.flowDir.toString()).set(flowDir.toString());
//	}
	
	public static InstanceType getOrCreateDesignSpaceCoreSchema(Workspace ws) {
		Optional<InstanceType> thisType = Optional.ofNullable(ws.TYPES_FOLDER.instanceTypeWithName(designspaceTypeId)); 
//		Optional<InstanceType> thisType = ws.debugInstanceTypes().stream()
//				.filter(it -> it.name().contentEquals(designspaceTypeId))
//				.findAny();
			if (thisType.isPresent())
				return thisType.get();
			else {
				InstanceType typeStep = ws.createInstanceType(designspaceTypeId, ws.TYPES_FOLDER);
				typeStep.createPropertyType(CoreProperties.fromStepType.toString(), Cardinality.SINGLE, Workspace.STRING);
				typeStep.createPropertyType(CoreProperties.fromParameter.toString(), Cardinality.SINGLE, Workspace.STRING);
				typeStep.createPropertyType(CoreProperties.toStepType.toString(), Cardinality.SINGLE, Workspace.STRING);
				typeStep.createPropertyType(CoreProperties.toParameter.toString(), Cardinality.SINGLE, Workspace.STRING);
		//		typeStep.createPropertyType(CoreProperties.flowDir.toString(), Cardinality.SINGLE, Workspace.STRING);
				return typeStep;
			}
	}

	public static MappingDefinition getInstance(String fromStepType, String fromParameter, String toStepType, String toParameter,Workspace ws) {
		Instance instance = ws.createInstance(getOrCreateDesignSpaceCoreSchema(ws), fromStepType+fromParameter+toStepType+toParameter);
		MappingDefinition md = WrapperCache.getWrappedInstance(MappingDefinition.class, instance);
		md.setFromStepType(fromStepType);
		md.setFromParameter(fromParameter);
		md.setToStepType(toStepType);
		md.setToParameter(toParameter);
	//	md.setFlowDir(flowDir);
		return md;
	}

	@Override
	public String toString() {
		return "MapDef [" + getFromStepType() + ":"+getFromParameter()+" -> "+ getToStepType() + ":"+getToParameter()+"]";
	}


	
	
}
