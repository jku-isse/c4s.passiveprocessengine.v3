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

	
	public RuleEnabledResolver(InstanceRepository instanceRepository, NodeToDomainResolver schemaRegistry, 
			InputToOutputMapper ioMapper, RepairTreeProvider repairTreeProvider, RuleAnalysisService ruleAnalysisService) {
		super(instanceRepository, schemaRegistry, repairTreeProvider, ruleAnalysisService);
		this.ioMapper = ioMapper;
	}
	
	@Override
	protected <T extends InstanceWrapper> T instantiateWithSpecificContext(Class<? extends InstanceWrapper> clazz, RDFInstance instance) {
		try {
			// otherwise we take the constructor of that class that takes an Instance object as parameter
			// and create it, passing it the instance object
			// assumption: every managed class implements such an constructor, (otherwise will fail fast here anyway)
			T t = (T) clazz.getConstructor(RDFInstance.class, RuleEnabledResolver.class).newInstance(instance, this);
			//t.ws = instance.workspace;
			cache.put(instance.getId(), t);
			if (ProcessInstance.class.isAssignableFrom(clazz)) {
				((ProcessInstance)t).inject(this.getFactoryIndex().getProcessStepFactory(), this.getFactoryIndex().getDecisionNodeInstanceFactory());
			}
			return t;
		} catch (Exception e) {
			e.printStackTrace();
			log.error(e.getMessage());
		}
		return null;
	}
	
	protected void inject(FactoryIndex factoryIndex) {
		this.factoryIndex = factoryIndex;
	}


}
