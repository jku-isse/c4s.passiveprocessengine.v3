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
import at.jku.isse.passiveprocessengine.monitoring.serialization.MonitoringMultiTypeAdapterFactory;

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
			List<ProcessChangedEvent> list = logs.computeIfAbsent(event.getProcScope().getInstance().getName(), k->new LinkedList<>() );
			list.add(event);
		});
	}

	public Map<String, List<ProcessChangedEvent>> logs = new HashMap<>();

	private static Gson gson = new GsonBuilder()
			 .registerTypeAdapterFactory(new MonitoringMultiTypeAdapterFactory())
			 .setPrettyPrinting()
			 .create();

	public String getEventLogAsJson(String processName) {
		if (logs.containsKey(processName)) {
			return gson.toJson(logs.get(processName));
		} else {
			return null;
		}
	}
}
