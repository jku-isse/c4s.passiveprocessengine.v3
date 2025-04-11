package at.jku.isse.passiveprocessengine.definition.activeobjects;

import at.jku.isse.passiveprocessengine.rdfwrapper.NodeToDomainResolver;
import at.jku.isse.passiveprocessengine.rdfwrapper.RDFInstanceType;
import lombok.NonNull;

import org.apache.jena.ontapi.model.OntIndividual;

import at.jku.isse.passiveprocessengine.definition.types.MappingDefinitionTypeFactory;

public class MappingDefinition extends ProcessDefinitionScopedElement {

	public MappingDefinition(@NonNull OntIndividual element, RDFInstanceType type, @NonNull NodeToDomainResolver resolver) {
		super(element, type, resolver);
	}

	public String getFromStepType() {
		return getTypedProperty(MappingDefinitionTypeFactory.CoreProperties.fromStepType.toString(), String.class);
	}

	public void setFromStepType(String fromStepType) {
		setSingleProperty(MappingDefinitionTypeFactory.CoreProperties.fromStepType.toString(), fromStepType);
	}

	public String getFromParameter() {
		return getTypedProperty(MappingDefinitionTypeFactory.CoreProperties.fromParameter.toString(), String.class);
	}

	public void setFromParameter(String fromParameter) {
		setSingleProperty(MappingDefinitionTypeFactory.CoreProperties.fromParameter.toString(), fromParameter);
	}

	public String getToStepType() {
		return getTypedProperty(MappingDefinitionTypeFactory.CoreProperties.toStepType.toString(), String.class);
	}

	public void setToStepType(String toStepType) {
		setSingleProperty(MappingDefinitionTypeFactory.CoreProperties.toStepType.toString(), toStepType);
	}

	public String getToParameter() {
		return getTypedProperty(MappingDefinitionTypeFactory.CoreProperties.toParameter.toString(), String.class);
	}

	public void setToParameter(String toParameter) {
		setSingleProperty(MappingDefinitionTypeFactory.CoreProperties.toParameter.toString(), toParameter);
	}

	@Override
	public String toString() {
		return "MapDef [" + getFromStepType() + ":"+getFromParameter()+" -> "+ getToStepType() + ":"+getToParameter()+"]";
	}




}
