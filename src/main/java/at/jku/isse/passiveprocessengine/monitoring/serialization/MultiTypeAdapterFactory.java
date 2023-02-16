package at.jku.isse.passiveprocessengine.monitoring.serialization;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import at.jku.isse.passiveprocessengine.instance.messages.Events.ConditionFulfillmentChanged;
import at.jku.isse.passiveprocessengine.instance.messages.Events.QAFulfillmentChanged;
import at.jku.isse.passiveprocessengine.instance.messages.Events.StepStateTransitionEvent;
import at.jku.isse.passiveprocessengine.monitoring.ProcessStats;
import at.jku.isse.passiveprocessengine.monitoring.ProcessStepStats;


public class MultiTypeAdapterFactory  implements TypeAdapterFactory {

    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        if (type.getRawType() == ProcessStats.class) {
            return (TypeAdapter<T>) wrapProcessStatsAdapter(gson, new TypeToken<ProcessStats>() {});
        }
        if (type.getRawType() == OffsetDateTime.class) {
            return (TypeAdapter<T>) wrapOffsetDateTimeAdapter(gson, new TypeToken<OffsetDateTime>() {});
        }
        if (type.getRawType() == StepStateTransitionEvent.class) {
        	return (TypeAdapter<T>) wrapStepStateTransitionEvent(gson, new TypeToken<StepStateTransitionEvent>() {});
        }
        if (type.getRawType() == ConditionFulfillmentChanged.class) {
        	return (TypeAdapter<T>) wrapPostconditionFulfillmentChanged(gson, new TypeToken<ConditionFulfillmentChanged>() {});
        }
        if (type.getRawType() == QAFulfillmentChanged.class) {
        	return (TypeAdapter<T>) wrapQAFulfillmentChanged(gson, new TypeToken<QAFulfillmentChanged>() {});
        }
        return null;
    }
    
    
    
    private TypeAdapter<ProcessStats> wrapProcessStatsAdapter(Gson gson, TypeToken<ProcessStats> type) {
        final TypeAdapter<ProcessStats> delegate = gson.getDelegateAdapter(this, type);

        return new TypeAdapter<ProcessStats>() {

            @Override
            public void write(JsonWriter out, ProcessStats value) throws IOException {
                value.setStepStats( value.getPerStepStats().values( ).stream().sorted(new CompareBySpecOrder()).collect(Collectors.toList())  );
            	delegate.write(out, value);
            }

            @Override public ProcessStats read(JsonReader in) throws IOException {
            	ProcessStats ps = delegate.read(in);
            	// we would need a lookup for the actual task
                return ps;
            }
        };
    }

    private TypeAdapter<OffsetDateTime> wrapOffsetDateTimeAdapter(Gson gson, TypeToken<OffsetDateTime> type) {
    	
    	final TypeAdapter<String> delegate = gson.getDelegateAdapter(this, TypeToken.get(String.class) );

        return new TypeAdapter<OffsetDateTime>() {

            @Override
            public void write(JsonWriter out, OffsetDateTime value) throws IOException {
                if (value != null) {
                	String strvalue = value.toString();
            		delegate.write(out, strvalue);
                } else
                	delegate.write(out, "");
            	
            }

            @Override public OffsetDateTime read(JsonReader in) throws IOException {
            	String str = delegate.read(in);
            	OffsetDateTime s = OffsetDateTime.parse(str);
                return s;
            }
        };
    }
    
	private static class CompareBySpecOrder  implements Comparator<ProcessStepStats> {

		@Override
		public int compare(ProcessStepStats o1, ProcessStepStats o2) {
			if (o1.getStep() != null && o1.getStep().getDefinition() != null && o2.getStep() != null && o2.getStep().getDefinition() != null)
				return o1.getStep().getDefinition().getSpecOrderIndex().compareTo(o2.getStep().getDefinition().getSpecOrderIndex());
			else return 0;
		}
		
	}
 
	
	private TypeAdapter<StepStateTransitionEvent> wrapStepStateTransitionEvent(Gson gson, TypeToken<StepStateTransitionEvent> type) {
		return new TypeAdapter<StepStateTransitionEvent>() {

			@Override
			public void write(JsonWriter out, StepStateTransitionEvent value) throws IOException {
				out.beginObject();
				out.name("event");
				out.value(value.getClass().getSimpleName());
				out.name("step");
				out.value(value.getStep().getDefinition().getName());
				out.name("oldState");
				out.value(value.getOldState().toString());
				out.name("newState");
				out.value(value.getNewState().toString());
				out.name("type");
				out.value(value.isActualState() ? "ACTUAL" : "EXPECTED");
				out.name("timestamp");
				out.value(value.getTimestamp().toString());
				out.endObject();
			}

			@Override
			public StepStateTransitionEvent read(JsonReader in) throws IOException {	
				throw new RuntimeException("Adapter not intended to be used for deserialization");
			}
		};
	}
	
	private TypeAdapter<ConditionFulfillmentChanged> wrapPostconditionFulfillmentChanged(Gson gson, TypeToken<ConditionFulfillmentChanged> type) {
		return new TypeAdapter<ConditionFulfillmentChanged>() {

			@Override
			public void write(JsonWriter out, ConditionFulfillmentChanged value) throws IOException {
				out.beginObject();
				out.name("event");
				out.value(value.getClass().getSimpleName());
				out.name("step");
				out.value(value.getStep().getDefinition().getName());
				out.name("condition");
				out.value(value.getCondition().toString());
				out.name("isFulfilled");
				out.value(value.isFulfilled());
				out.name("timestamp");
				out.value(value.getTimestamp().toString());
				out.endObject();
			}

			@Override
			public ConditionFulfillmentChanged read(JsonReader in) throws IOException {	
				throw new RuntimeException("Adapter not intended to be used for deserialization");
			}
		};
	}
	
	private TypeAdapter<QAFulfillmentChanged> wrapQAFulfillmentChanged(Gson gson, TypeToken<QAFulfillmentChanged> type) {
		return new TypeAdapter<QAFulfillmentChanged>() {

			@Override
			public void write(JsonWriter out, QAFulfillmentChanged value) throws IOException {
				out.beginObject();
				out.name("event");
				out.value(value.getClass().getSimpleName());
				out.name("step");
				out.value(value.getStep().getDefinition().getName());
				out.name("isFulfilled");
				out.value(value.isFulfilled());
				out.name("timestamp");
				out.value(value.getTimestamp().toString());
				out.endObject();
			}

			@Override
			public QAFulfillmentChanged read(JsonReader in) throws IOException {	
				throw new RuntimeException("Adapter not intended to be used for deserialization");
			}
		};
	}
	
    public static Gson gson;
    
	public static void writeToJSONFile(String pathInclFileName, Collection<ProcessStats> stats) {
		 if (gson == null) {
			 gson = new GsonBuilder()	        
				 	.registerTypeAdapterFactory(new MultiTypeAdapterFactory())
	                .setPrettyPrinting()
	                .create();
		 }
		 String json = gson.toJson(stats);
		 try {
			Files.write(Paths.get(pathInclFileName), json.getBytes());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
    
}

