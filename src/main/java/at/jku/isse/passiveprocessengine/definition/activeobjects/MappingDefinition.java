package at.jku.isse.passiveprocessengine.definition.activeobjects;

import at.jku.isse.passiveprocessengine.Context;
import at.jku.isse.passiveprocessengine.InstanceWrapper;
import at.jku.isse.passiveprocessengine.core.PPEInstance;
import at.jku.isse.passiveprocessengine.definition.types.MappingDefinitionType;
import at.jku.isse.passiveprocessengine.instance.types.ProcessConfigBaseElementType;

public class MappingDefinition extends InstanceWrapper{

	public MappingDefinition(PPEInstance instance, Context wrapperCache) {
		super(instance, wrapperCache);
	}

	public String getFromStepType() {
		return instance.getTypedProperty(MappingDefinitionType.CoreProperties.fromStepType.toString(), String.class);
	}

	public void setFromStepType(String fromStepType) {
		instance.setSingleProperty(MappingDefinitionType.CoreProperties.fromStepType.toString(), fromStepType);
	}

	public String getFromParameter() {
		return instance.getTypedProperty(MappingDefinitionType.CoreProperties.fromParameter.toString(), String.class);
	}

	public void setFromParameter(String fromParameter) {
		instance.setSingleProperty(MappingDefinitionType.CoreProperties.fromParameter.toString(), fromParameter);
	}

	public String getToStepType() {
		return instance.getTypedProperty(MappingDefinitionType.CoreProperties.toStepType.toString(), String.class);
	}

	public void setToStepType(String toStepType) {
		instance.setSingleProperty(MappingDefinitionType.CoreProperties.toStepType.toString(), toStepType);
	}

	public String getToParameter() {
		return instance.getTypedProperty(MappingDefinitionType.CoreProperties.toParameter.toString(), String.class);
	}

	public void setToParameter(String toParameter) {
		instance.setSingleProperty(MappingDefinitionType.CoreProperties.toParameter.toString(), toParameter);
	}

	@Override
	public void deleteCascading(ProcessConfigBaseElementType configFactory) {
		super.deleteCascading(configFactory);
	}

	@Override
	public String toString() {
		return "MapDef [" + getFromStepType() + ":"+getFromParameter()+" -> "+ getToStepType() + ":"+getToParameter()+"]";
	}




}
