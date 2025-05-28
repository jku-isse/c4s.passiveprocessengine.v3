package at.jku.isse.passiveprocessengine.instance.serialization;

import java.io.IOException;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import at.jku.isse.passiveprocessengine.rdfwrapper.RDFElement;
import at.jku.isse.passiveprocessengine.rdfwrapper.RDFInstance;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class TypeAdapterRouter extends TypeAdapter<RDFInstance> {
	
	private final TypeAdapterRegistry registry;
	
	@Override
	public void write(JsonWriter out, RDFInstance value) throws IOException {
		// now lookup instance type specific
		TypeAdapter<RDFInstance> adapter = registry.getTypeAdapterOrDefault(value.getInstanceType());
		adapter.write(out, value);
	}

	@Override
	public RDFInstance read(JsonReader in) throws IOException {
		throw new RuntimeException("ProcessInstance can only be serialized but not deserialized");
	}
}
