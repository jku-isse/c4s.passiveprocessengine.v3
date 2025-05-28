package at.jku.isse.passiveprocessengine.instance.serialization;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

import com.google.gson.stream.JsonWriter;

import at.jku.isse.passiveprocessengine.rdfwrapper.RDFInstance;
import at.jku.isse.passiveprocessengine.rdfwrapper.RDFPropertyType;

public class ShallowPropertyGsonWriter {

	public void writeProperty(RDFPropertyType propType, RDFInstance instance, JsonWriter writer) throws IOException {
		writer.name(propType.getName());
		switch(propType.getCardinality()) {
		case LIST:
			writeCollectionProperty(propType, instance.getTypedProperty(propType.getName(), List.class), writer);
			break;
		case SET:
			writeCollectionProperty(propType, instance.getTypedProperty(propType.getName(), Set.class), writer);
			break;
		case MAP:
			writeMapProperty(propType, instance.getTypedProperty(propType.getName(), Map.class), writer);
			break;
		case SINGLE:
			writeSingleProperty(propType, instance, writer);
			break;
		default:
			break;
		}
	}
	
	private void writeMapProperty(RDFPropertyType propType, @SuppressWarnings("rawtypes") Map<String, ?> map, JsonWriter writer) throws IOException {
		writer.beginObject();
		boolean isAtomic = propType.getValueType().isPrimitiveType();
		for (Entry<String, ?> entry : map.entrySet()) {
			writer.name(entry.getKey());
			if (isAtomic) {
				writer.value(Objects.toString(entry.getValue()));
			} else {
				writeInstanceValue((RDFInstance)entry.getValue(), writer);
			}
		};
		writer.endObject();
	}
	
	private void writeCollectionProperty(RDFPropertyType propType, @SuppressWarnings("rawtypes") Collection collection, JsonWriter writer) throws IOException {
		writer.beginArray();
		if (propType.getValueType().isPrimitiveType() ) {
			for(Object value : collection) {
				writer.value(Objects.toString(value));
			}
		} else {
			for(Object value : collection) {
				writeInstanceValue((RDFInstance)value, writer);
			}
		}
		writer.endArray();
	}
	
	private void writeSingleProperty(RDFPropertyType propType, RDFInstance instance, JsonWriter writer) throws IOException {
		if (propType.getValueType().isPrimitiveType() ) {
			Object value = instance.getTypedProperty(propType.getName(), Object.class);
			writer.value(Objects.toString(value));
		} else {
			writeInstanceValue(instance.getTypedProperty(propType.getName(), RDFInstance.class), writer);
		}
	}
	
	protected void writeInstanceValue(RDFInstance instance, JsonWriter writer) throws IOException {
		if (instance == null) {
			writer.nullValue();
		} else {
			writer.beginObject(); 
			writer.name("internalId"); 
			writer.value(instance.getId()); 
			writer.name("name"); 
			writer.value(instance.getName()); 
			if (instance.getInstanceType() != null) {
				writer.name("instanceType"); 
				writer.value(instance.getInstanceType().getName()); 
			}
			writer.endObject(); 
		}
	}
	
}
