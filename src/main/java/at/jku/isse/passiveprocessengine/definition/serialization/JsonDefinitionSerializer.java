package at.jku.isse.passiveprocessengine.definition.serialization;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

public class JsonDefinitionSerializer {

	 Gson gson;
	 
	 public JsonDefinitionSerializer() {
		 gson = new GsonBuilder().setPrettyPrinting().create();
	 }
	 
	 public String toJson(DTOs.Process procDef) {
		 return gson.toJson(procDef);
	 }
	 
	 public DTOs.Process fromJson(String procDefJson) throws JsonSyntaxException {
		 DTOs.Process procDef = gson.fromJson(procDefJson, DTOs.Process.class);
		 return procDef;
	 }
}
