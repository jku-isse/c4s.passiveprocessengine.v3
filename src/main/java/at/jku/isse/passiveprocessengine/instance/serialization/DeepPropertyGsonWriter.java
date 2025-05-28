package at.jku.isse.passiveprocessengine.instance.serialization;

import java.io.IOException;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonWriter;

import at.jku.isse.passiveprocessengine.rdfwrapper.RDFInstance;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DeepPropertyGsonWriter extends ShallowPropertyGsonWriter {

	private final TypeAdapterRegistry registry;

	@Override
	protected void writeInstanceValue(RDFInstance instance, JsonWriter writer) throws IOException {
		
		TypeAdapter<RDFInstance> adapter = registry.getTypeAdapterOrDefault(instance.getInstanceType());
		adapter.write(writer, instance);
	}
	
	
}
