package at.jku.isse.passiveprocessengine.instance.serialization;

import java.io.IOException;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonWriter;

import at.jku.isse.passiveprocessengine.core.PPEInstance;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DeepPropertyGsonWriter extends ShallowPropertyGsonWriter {

	private final TypeAdapterRegistry registry;

	@Override
	protected void writeInstanceValue(PPEInstance instance, JsonWriter writer) throws IOException {
		
		TypeAdapter<PPEInstance> adapter = registry.getTypeAdapterOrDefault(instance.getInstanceType());
		adapter.write(writer, instance);
	}
	
	
}
