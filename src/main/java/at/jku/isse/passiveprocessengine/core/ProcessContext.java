package at.jku.isse.passiveprocessengine.core;

import at.jku.isse.passiveprocessengine.instance.InputToOutputMapper;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public class ProcessContext extends Context{

	final InputToOutputMapper ioMapper;
	protected FactoryIndex factoryIndex;
			
	
	public ProcessContext(InstanceRepository instanceRepository, SchemaRegistry schemaRegistry, 
			InputToOutputMapper ioMapper) {
		super(instanceRepository, schemaRegistry);
		
		this.ioMapper = ioMapper;
	}
	
	@Override
	protected <T extends InstanceWrapper> T instantiateWithSpecificContext(Class<? extends InstanceWrapper> clazz, PPEInstance instance) {
		try {
			// otherwise we take the constructor of that class that takes an Instance object as parameter
			// and create it, passing it the instance object
			// assumption: every managed class implements such an constructor, (otherwise will fail fast here anyway)
			T t = (T) clazz.getConstructor(PPEInstance.class, ProcessContext.class).newInstance(instance, this);
			//t.ws = instance.workspace;
			cache.put(instance.getId(), t);
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
