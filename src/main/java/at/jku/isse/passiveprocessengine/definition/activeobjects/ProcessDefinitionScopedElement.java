package at.jku.isse.passiveprocessengine.definition.activeobjects;


import org.apache.jena.ontapi.model.OntIndividual;

import at.jku.isse.passiveprocessengine.definition.types.ProcessDefinitionScopeType;
import at.jku.isse.passiveprocessengine.rdfwrapper.NodeToDomainResolver;
import at.jku.isse.passiveprocessengine.rdfwrapper.RDFInstance;
import at.jku.isse.passiveprocessengine.rdfwrapper.RDFInstanceType;
import lombok.NonNull;

public abstract class ProcessDefinitionScopedElement extends RDFInstance {
	
	protected ProcessDefinitionScopedElement(@NonNull OntIndividual element, RDFInstanceType type, @NonNull NodeToDomainResolver resolver) {
		super(element, type, resolver);
	}

	public void setProcess(ProcessDefinition pi) {
		setSingleProperty(ProcessDefinitionScopeType.CoreProperties.processDefinition.toString(), pi.getInstance());
	}

	public void setProcOrderIndex(int index) {
		setSingleProperty(ProcessDefinitionScopeType.CoreProperties.orderIndex.toString(), index);
	}

	public Integer getProcOrderIndex() {
		return getTypedProperty(ProcessDefinitionScopeType.CoreProperties.orderIndex.toString(), Integer.class, -1);
	}

	public ProcessDefinition getProcess() {
		return getTypedProperty(ProcessDefinitionScopeType.CoreProperties.processDefinition.toString(), ProcessDefinition.class);		
	}

	@Override
	public String toString() {
		return getName();
	}
	
	public void deleteCascading() {
		super.delete();
	}
}
