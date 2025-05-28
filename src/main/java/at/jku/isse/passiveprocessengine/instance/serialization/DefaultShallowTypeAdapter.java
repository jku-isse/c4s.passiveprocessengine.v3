package at.jku.isse.passiveprocessengine.instance.serialization;

import java.io.IOException;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import at.jku.isse.passiveprocessengine.rdfwrapper.RDFInstance;
import at.jku.isse.passiveprocessengine.rdfwrapper.RDFInstanceType;



public class DefaultShallowTypeAdapter extends TypeAdapter<RDFInstance>{

	protected final ShallowPropertyGsonWriter propertyWriter = new ShallowPropertyGsonWriter();

	@Override
	public void write(JsonWriter out, RDFInstance value) throws IOException {
		out.beginObject();
		writeThisInstance(value, out);
		RDFInstanceType type = value.getInstanceType();
		type.getPropertyNamesIncludingSuperClasses().stream()
		.map(propName -> type.getPropertyType(propName))
		.forEach(propType -> {
			try {
				propertyWriter.writeProperty(propType, value, out);
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
		out.endObject();
	}

	@Override
	public RDFInstance read(JsonReader in) throws IOException {
		throw new RuntimeException("RDFInstance can only be serialized but not deserialized");
	}
	
	protected void writeThisInstance(RDFInstance instance, JsonWriter writer) throws IOException {
	      writer.name("internalId"); 
	      writer.value(instance.getId()); 
	      writer.name("name"); 
	      writer.value(instance.getName()); 
	      if (instance.getInstanceType() != null) {
	    	  writer.name("instanceType"); 
	    	  writer.value(instance.getInstanceType().getName()); 
	      }
	}
}
