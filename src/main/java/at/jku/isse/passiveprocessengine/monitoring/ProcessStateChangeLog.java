package at.jku.isse.passiveprocessengine.monitoring;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import at.jku.isse.passiveprocessengine.instance.messages.Events.ConditionFulfillmentChanged;
import at.jku.isse.passiveprocessengine.instance.messages.Events.ProcessChangedEvent;
import at.jku.isse.passiveprocessengine.instance.messages.Events.QAFulfillmentChanged;
import at.jku.isse.passiveprocessengine.instance.messages.Events.StepStateTransitionEvent;
import at.jku.isse.passiveprocessengine.instance.messages.IProcessEventHandler;

public class ProcessStateChangeLog implements IProcessEventHandler{

	@Autowired
	ITimeStampProvider timeStampProvider;
	
	@Override
	public void handleEvents(Collection<ProcessChangedEvent> events) {
		events.stream()
		.filter(event -> event instanceof QAFulfillmentChanged || event instanceof ConditionFulfillmentChanged || event instanceof StepStateTransitionEvent)
		.filter(event -> event.getProcScope() != null) // wont handle process instance events
		.forEach(event -> {
			event.setTimestamp(timeStampProvider.getLastChangeTimeStamp());
			List<ProcessChangedEvent> list = logs.computeIfAbsent(event.getProcScope().getInstance().id().value(), k->new LinkedList<>() );
			list.add(event);
		});
	}
	
	public Map<Long, List<ProcessChangedEvent>> logs = new HashMap<>();
	
	private static Gson gson = new GsonBuilder()
			 .registerTypeAdapterFactory(new at.jku.isse.passiveprocessengine.monitoring.serialization.MultiTypeAdapterFactory())
			 .setPrettyPrinting()
			 .create();
	
	public String getEventLogAsJson(Long processId) {
		if (logs.containsKey(processId)) {
			return gson.toJson(logs.get(processId));
		} else {
			return null;
		}
	}
}
