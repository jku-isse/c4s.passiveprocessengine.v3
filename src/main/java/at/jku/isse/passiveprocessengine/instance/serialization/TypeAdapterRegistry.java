package at.jku.isse.passiveprocessengine.instance.serialization;

import java.util.HashMap;
import java.util.Map;

import com.google.gson.TypeAdapter;

import at.jku.isse.passiveprocessengine.rdfwrapper.RDFInstance;
import at.jku.isse.passiveprocessengine.rdfwrapper.RDFInstanceType;


public class TypeAdapterRegistry {

	
	private final Map<RDFInstanceType, TypeAdapter<RDFInstance>> registry = new HashMap<>();
	private final TypeAdapter<RDFInstance> defaultAdapter = new DefaultShallowTypeAdapter();
	
	public void registerTypeAdapter(TypeAdapter<RDFInstance> adapter, RDFInstanceType type) {
		registry.put(type, adapter);
	}
	
	public TypeAdapter<RDFInstance> getTypeAdapterOrDefault(RDFInstanceType type) {
		TypeAdapter<RDFInstance> adapter = registry.get(type);
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
