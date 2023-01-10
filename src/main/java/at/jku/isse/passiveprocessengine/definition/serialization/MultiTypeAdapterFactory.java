package at.jku.isse.passiveprocessengine.definition.serialization;

import java.io.IOException;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;


public class MultiTypeAdapterFactory  implements TypeAdapterFactory {

    @SuppressWarnings("unchecked")
	@Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        if (type.getRawType() == DTOs.Step.class) {
            return (TypeAdapter<T>) wrapStep(gson);
        } else 
        if (type.getRawType() == DTOs.Process.class) {
            return (TypeAdapter<T>) wrapStep(gson);
        }
        return null;
    }
    
    
    

    private TypeAdapter<DTOs.Step> wrapStep(Gson gson) {
        final TypeAdapter<DTOs.Step> stepDelegate = gson.getDelegateAdapter(this, new TypeToken<DTOs.Step>() {});
        final TypeAdapter<DTOs.Process> procDelegate = gson.getDelegateAdapter(this, new TypeToken<DTOs.Process>() {});
        final TypeAdapter<JsonElement> jsonElementAdapter = gson.getAdapter(JsonElement.class);
        
        return new TypeAdapter<DTOs.Step>() {

            @Override
            public void write(JsonWriter out, DTOs.Step value) throws IOException {
                if (value instanceof DTOs.Process) {          
                    procDelegate.write(out, (DTOs.Process)value);
                } else {
                    stepDelegate.write(out, value);
                }
            }

            @Override
            public DTOs.Step read(JsonReader in) throws IOException {
            	JsonElement jsonElement = jsonElementAdapter.read(in);
            	JsonElement labelJsonElement = jsonElement.getAsJsonObject().get("_type");
            	String label = labelJsonElement.getAsString();
                
                if (DTOs.Step.class.getSimpleName().equals(label)) {
                    return stepDelegate.fromJsonTree(jsonElement);
                } else if (DTOs.Process.class.getSimpleName().equals(label)) {
                	return procDelegate.fromJsonTree(jsonElement);
                }
                return null;
            }

        };
    }

   
}
