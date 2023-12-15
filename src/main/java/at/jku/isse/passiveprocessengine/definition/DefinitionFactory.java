package at.jku.isse.passiveprocessengine.definition;

import java.util.HashMap;
import java.util.Map;

import at.jku.isse.passiveprocessengine.InstanceWrapper;
import at.jku.isse.passiveprocessengine.core.InstanceType;

public class DefinitionFactory {

	private Map<Class<? extends InstanceWrapper>, TypeProvider> providers = new HashMap<>();
	
	public void registerBaseType(Class<? extends InstanceWrapper> clazz, TypeProvider provider) {
		providers.put(clazz, provider);
	}
	
	public TypeProvider getProvider(Class<? extends InstanceWrapper> clazz) {
		return providers.get(clazz);
	}
	
	
	public static interface TypeProvider {
		InstanceType getType();
	}
}
