package at.jku.isse.passiveprocessengine.definition.types;

import java.util.HashMap;
import java.util.Map;

import at.jku.isse.passiveprocessengine.InstanceWrapper;
import at.jku.isse.passiveprocessengine.core.InstanceType;

public class ProcessDomainTypesFactory {

	private Map<Class<? extends InstanceWrapper>, InstanceType> types = new HashMap<>();
	
	public InstanceType getType(Class<? extends InstanceWrapper> clazz) {
		return types.get(clazz);
	}
	
	public void registerType(Class<? extends InstanceWrapper> clazz, InstanceType type) {
		types.put(clazz, type);
	}
	
	public static interface TypeProvider {
		void registerTypeInFactory(ProcessDomainTypesFactory factory);
	}
}
