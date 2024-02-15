package at.jku.isse.passiveprocessengine.designspace;

import at.jku.isse.passiveprocessengine.core.PPEInstance;

public interface DesignspaceAbstractionMapper {

	
	
	public at.jku.isse.designspace.core.model.Element mapProcessDomainInstanceToDesignspaceInstance(PPEInstance processDomainInstance);
	
	public at.jku.isse.designspace.core.model.Element mapProcessDomainInstanceTypeToDesignspaceInstanceType(PPEInstance processDomainInstanceType);
	
}
