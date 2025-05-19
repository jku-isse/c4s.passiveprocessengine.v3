package at.jku.isse.passiveprocessengine.instance.serialization;

import java.io.IOException;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import at.jku.isse.passiveprocessengine.core.PPEInstance;
import at.jku.isse.passiveprocessengine.core.PPEInstanceType;

public class DefaultShallowTypeAdapter extends TypeAdapter<PPEInstance>{

	protected final ShallowPropertyGsonWriter propertyWriter = new ShallowPropertyGsonWriter();

	@Override
	public void write(JsonWriter out, PPEInstance value) throws IOException {
		out.beginObject();
		writeThisInstance(value, out);
		PPEInstanceType type = value.getInstanceType();
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
	public PPEInstance read(JsonReader in) throws IOException {
		throw new RuntimeException("PPEInstance can only be serialized but not deserialized");
	}
	
	protected void writeThisInstance(PPEInstance instance, JsonWriter writer) throws IOException {
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
