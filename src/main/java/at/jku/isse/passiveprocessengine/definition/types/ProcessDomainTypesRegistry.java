package at.jku.isse.passiveprocessengine.definition.types;

import java.util.HashMap;
import java.util.Map;

import at.jku.isse.passiveprocessengine.InstanceWrapper;
import at.jku.isse.passiveprocessengine.core.InstanceType;
import lombok.NonNull;

public class ProcessDomainTypesRegistry {

	private Map<String, InstanceType> types = new HashMap<>();
	
	public InstanceType getType(Class<? extends InstanceWrapper> clazz) {
		return types.get(clazz.getName());
	}
	
	public void registerType(Class<? extends InstanceWrapper> clazz, InstanceType type) {
		types.put(clazz.getName(), type);
	}
	
	public void registerTypeByName(@NonNull InstanceType type) {
		types.put(type.getName(), type);
	}
	
	public InstanceType getTypeByName(String typeName) {
		return types.get(typeName);
	}
	
	public static interface TypeProvider {
		void registerTypeInFactory(ProcessDomainTypesRegistry factory);
	}
}
