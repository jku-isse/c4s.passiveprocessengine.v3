package at.jku.isse.passiveprocessengine.instance.serialization;

import java.io.IOException;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import at.jku.isse.passiveprocessengine.rdfwrapper.RDFElement;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class TypeAdapterRouter extends TypeAdapter<RDFElement> {
	
	private final TypeAdapterRegistry registry;
	
	@Override
	public void write(JsonWriter out, RDFElement value) throws IOException {
		// now lookup instance type specific
		TypeAdapter<PPEInstance> adapter = registry.getTypeAdapterOrDefault(value.getInstanceType());
		adapter.write(out, value);
	}

	@Override
	public PPEInstance read(JsonReader in) throws IOException {
		throw new RuntimeException("ProcessInstance can only be serialized but not deserialized");
	}
}
