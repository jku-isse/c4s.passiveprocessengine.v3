package at.jku.isse.passiveprocessengine.core;

import at.jku.isse.passiveprocessengine.instance.InputToOutputMapper;
import at.jku.isse.passiveprocessengine.instance.activeobjects.ProcessInstance;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public class ProcessContext extends Context{

	final InputToOutputMapper ioMapper;
	protected FactoryIndex factoryIndex;

	
	public ProcessContext(InstanceRepository instanceRepository, NodeToDomainResolver schemaRegistry, 
			InputToOutputMapper ioMapper, RepairTreeProvider repairTreeProvider, RuleAnalysisService ruleAnalysisService) {
		super(instanceRepository, schemaRegistry, repairTreeProvider, ruleAnalysisService);
		this.ioMapper = ioMapper;
	}
	
	
	
	protected void inject(FactoryIndex factoryIndex) {
		this.factoryIndex = factoryIndex;
	}


}
