package at.jku.isse.passiveprocessengine.designspace;

import at.jku.isse.passiveprocessengine.core.Instance;

public interface DesignspaceAbstractionMapper {

	
	
	public at.jku.isse.designspace.core.model.Element mapProcessDomainInstanceToDesignspaceInstance(Instance processDomainInstance);
	
	public at.jku.isse.designspace.core.model.Element mapProcessDomainInstanceTypeToDesignspaceInstanceType(Instance processDomainInstanceType);
	
}
