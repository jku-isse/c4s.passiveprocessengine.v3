package at.jku.isse.passiveprocessengine.instance.serialization;

import java.io.IOException;
import java.util.Set;

import com.google.gson.stream.JsonWriter;

import at.jku.isse.passiveprocessengine.core.PPEInstance;
import at.jku.isse.passiveprocessengine.core.PPEInstanceType;

public class ConfigurablePropertyTypeAdapter extends DefaultShallowTypeAdapter{

	protected final Set<String> propertiesToSerializeShallow;
	protected final Set<String> propertiesToSerializeDeep;
	protected final DeepPropertyGsonWriter deepPropertyWriter;
	
	public ConfigurablePropertyTypeAdapter(TypeAdapterRegistry registry, Set<String> propertiesToSerializeShallow,
			Set<String> propertiesToSerializeDeep) {
		super();
		this.propertiesToSerializeShallow = propertiesToSerializeShallow;
		this.propertiesToSerializeDeep = propertiesToSerializeDeep;
		this.deepPropertyWriter = new DeepPropertyGsonWriter(registry);
	}

	@Override
	public void write(JsonWriter out, PPEInstance value) throws IOException {
		out.beginObject();
		writeThisInstance(value, out);
		PPEInstanceType type = value.getInstanceType();
		type.getPropertyNamesIncludingSuperClasses().stream()
		.filter(propName -> propertiesToSerializeShallow.contains(propName) 
				|| propertiesToSerializeDeep.contains(propName))
		.map(propName -> type.getPropertyType(propName))
		.forEach(propType -> {
			try {
				if (propertiesToSerializeShallow.contains(propType.getName())) {
					propertyWriter.writeProperty(propType, value, out);
				} else {
					deepPropertyWriter.writeProperty(propType, value, out);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
		out.endObject();
	}
}
