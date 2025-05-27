package at.jku.isse.passiveprocessengine.instance.activeobjects;

import at.jku.isse.passiveprocessengine.rdfwrapper.NodeToDomainResolver;
import at.jku.isse.passiveprocessengine.rdfwrapper.RDFInstance;
import at.jku.isse.passiveprocessengine.rdfwrapper.RDFInstanceType;
import lombok.NonNull;

import org.apache.jena.ontapi.model.OntIndividual;

import at.jku.isse.passiveprocessengine.definition.activeobjects.ProcessDefinitionScopedElement;
import at.jku.isse.passiveprocessengine.instance.types.ProcessInstanceScopeTypeFactory;

public abstract class ProcessInstanceScopedElement extends RDFInstance {

	protected ProcessInstanceScopedElement(@NonNull OntIndividual element, RDFInstanceType type, @NonNull NodeToDomainResolver resolver) {
		super(element, type, resolver);
	}

	public void setProcess(ProcessInstance pi) {
		setSingleProperty(ProcessInstanceScopeTypeFactory.CoreProperties.process.toString(), pi);
	}

	public ProcessInstance getProcess() {
		return (ProcessInstance) getTypedProperty(ProcessInstanceScopeTypeFactory.CoreProperties.process.toString(), RDFInstance.class);		
	}
	
	public void deleteCascading() {
		super.delete();
	}
	
	public abstract ProcessDefinitionScopedElement getDefinition();
}
