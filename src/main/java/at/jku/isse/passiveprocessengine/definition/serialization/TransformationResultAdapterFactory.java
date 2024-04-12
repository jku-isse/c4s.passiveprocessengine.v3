package at.jku.isse.passiveprocessengine.definition.serialization;

import java.io.IOException;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import at.jku.isse.designspace.core.model.Instance;
import at.jku.isse.passiveprocessengine.ProcessDefinitionScopedElement;
import at.jku.isse.passiveprocessengine.definition.ProcessDefinition;

public class TransformationResultAdapterFactory extends MultiTypeAdapterFactory {

	@Override
	public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
		if (type.getRawType() == ProcessDefinition.class) {
            return (TypeAdapter<T>) wrapDefinition(gson);
        } else if (type.getRawType() == Instance.class) {
            return (TypeAdapter<T>) wrapInstance(gson);
        } else if (type.getRawType() == ProcessDefinitionScopedElement.class) {
            return (TypeAdapter<T>) wrapProcessDefinitionScopedElement(gson);
        } else
        	return super.create(gson, type);
	}
	

	
	
	private TypeAdapter<Instance> wrapInstance(Gson gson) {
		final TypeAdapter<String> instanceDelegate = gson.getDelegateAdapter(this, new TypeToken<String>() {});
		
		return new TypeAdapter<Instance>() {
			@Override
			public void write(JsonWriter out, Instance value) throws IOException {
				instanceDelegate.write(out, value.name());
			}

			@Override
			public Instance read(JsonReader in) throws IOException {
				// TODO Auto-generated method stub
				return null;
			}
		};
	}

	private TypeAdapter<ProcessDefinitionScopedElement> wrapProcessDefinitionScopedElement(Gson gson) {
		final TypeAdapter<String> instanceDelegate = gson.getDelegateAdapter(this, new TypeToken<String>() {});
		
		return new TypeAdapter<ProcessDefinitionScopedElement>() {
			@Override
			public void write(JsonWriter out, ProcessDefinitionScopedElement value) throws IOException {
				instanceDelegate.write(out, value.getName());
			}

			@Override
			public ProcessDefinitionScopedElement read(JsonReader in) throws IOException {
				// TODO Auto-generated method stub
				return null;
			}
		};
	}
	
	private TypeAdapter<ProcessDefinition> wrapDefinition(Gson gson) {
		 final TypeAdapter<DTOs.Process> procDelegate = gson.getDelegateAdapter(this, new TypeToken<DTOs.Process>() {});
	      
		 return new TypeAdapter<ProcessDefinition>() {

			@Override
			public void write(JsonWriter out, ProcessDefinition value) throws IOException {
				DTOs.Process proc = DefinitionTransformer.toDTO(value);
				procDelegate.write(out, proc);
			}

			@Override
			public ProcessDefinition read(JsonReader in) throws IOException {
				// TODO Auto-generated method stub
				return null;
			}
			 
		 };
	}
	

	
	
}
