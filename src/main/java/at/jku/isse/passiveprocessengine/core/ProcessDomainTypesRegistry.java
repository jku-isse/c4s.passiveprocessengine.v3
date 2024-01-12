package at.jku.isse.passiveprocessengine.core;

import java.util.HashMap;
import java.util.Map;

import at.jku.isse.passiveprocessengine.InstanceWrapper;
import lombok.NonNull;

public class ProcessDomainTypesRegistry implements DomainTypesRegistry {

	private Map<String, InstanceType> types = new HashMap<>();
	
	@Override
	public InstanceType getType(Class<? extends InstanceWrapper> clazz) {
		return types.get(clazz.getName());
	}
	
	@Override
	public void registerType(Class<? extends InstanceWrapper> clazz, InstanceType type) {
		types.put(clazz.getName(), type);
	}
	
	@Override
	public void registerTypeByName(@NonNull InstanceType type) {
		types.put(type.getName(), type);
	}
	
	@Override
	public InstanceType getTypeByName(String typeName) {
		return types.get(typeName);
	}
}
