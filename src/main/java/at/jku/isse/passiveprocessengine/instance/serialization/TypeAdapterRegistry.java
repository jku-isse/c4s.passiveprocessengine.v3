package at.jku.isse.passiveprocessengine.instance.serialization;

import java.util.HashMap;
import java.util.Map;

import com.google.gson.TypeAdapter;

import at.jku.isse.passiveprocessengine.core.PPEInstance;
import at.jku.isse.passiveprocessengine.core.PPEInstanceType;

public class TypeAdapterRegistry {

	
	private final Map<PPEInstanceType, TypeAdapter<PPEInstance>> registry = new HashMap<>();
	private final TypeAdapter<PPEInstance> defaultAdapter = new DefaultShallowTypeAdapter();
	
	public void registerTypeAdapter(TypeAdapter<PPEInstance> adapter, PPEInstanceType type) {
		registry.put(type, adapter);
	}
	
	public TypeAdapter<PPEInstance> getTypeAdapterOrDefault(PPEInstanceType type) {
		TypeAdapter<PPEInstance> adapter = registry.get(type);
		while (adapter == null && type.getParentType() != null) {
			adapter = registry.get(type.getParentType());
			type = type.getParentType(); // prep for next iteration
		}
		if (adapter != null) {
			return adapter;
		} else {
			return defaultAdapter;
		}
	}
}
