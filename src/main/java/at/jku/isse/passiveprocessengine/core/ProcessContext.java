package at.jku.isse.passiveprocessengine.core;

import java.util.HashMap;
import java.util.Map;

import at.jku.isse.passiveprocessengine.instance.InputToOutputMapper;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public class ProcessContext extends Context{

	final InputToOutputMapper ioMapper;
	protected FactoryIndex factoryIndex;
				
	private final Map<String, InstanceWrapper> cache = new HashMap<>();
	
	public ProcessContext(InstanceRepository instanceRepository, SchemaRegistry schemaRegistry, 
			InputToOutputMapper ioMapper) {
		super(instanceRepository, schemaRegistry);
		
		this.ioMapper = ioMapper;
	}
	
	protected void inject(FactoryIndex factoryIndex) {
		this.factoryIndex = factoryIndex;
	}

}
