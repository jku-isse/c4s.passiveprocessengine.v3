package at.jku.isse.passiveprocessengine.definition.registry;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import at.jku.isse.passiveprocessengine.monitoring.serialization.MonitoringMultiTypeAdapterFactory;



public class JsonDefinitionSerializer {

	 Gson gson;

	 public JsonDefinitionSerializer() {
//		 RuntimeTypeAdapterFactory<DTOs.Typed> runtimeTypeAdapterFactory = RuntimeTypeAdapterFactory
//				    .of(DTOs.Typed.class, "_type")
//				    .registerSubtype(DTOs.DecisionNode.class, DTOs.DecisionNode.class.getSimpleName())
//				    .registerSubtype(DTOs.Process.class, DTOs.Process.class.getSimpleName())
//				    .registerSubtype(DTOs.Step.class, DTOs.Step.class.getSimpleName())
//				    ;

		 gson = new GsonBuilder()
				 .registerTypeAdapterFactory(new MonitoringMultiTypeAdapterFactory())
				 //.registerTypeAdapterFactory(runtimeTypeAdapterFactory)
				 .setPrettyPrinting()
				 .create();
	 }

	 public String toJson(DTOs.Process procDef) {
		 return gson.toJson(procDef);
	 }

	 public DTOs.Process fromJson(String procDefJson) throws JsonSyntaxException {
		 return gson.fromJson(procDefJson,  DTOs.Process.class);
	 }
}
