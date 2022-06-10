package at.jku.isse.passiveprocessengine.instance.messages;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import at.jku.isse.passiveprocessengine.instance.messages.Events.ProcessChangedEvent;

public class EventDistributor {

	Set<IProcessEventHandler> handlers = new LinkedHashSet<IProcessEventHandler>();

	
	public EventDistributor() {
		
	}
	
	public void registerHandler(IProcessEventHandler handler) {
		handlers.add(handler);
	}
	
	public void handleEvents(Collection<ProcessChangedEvent> events) {
		handlers.forEach(handler -> handler.handleEvents(events));
	}
}
