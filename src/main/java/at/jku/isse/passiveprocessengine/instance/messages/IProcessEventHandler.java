package at.jku.isse.passiveprocessengine.instance.messages;

import java.util.Collection;

import at.jku.isse.passiveprocessengine.instance.messages.Events.ProcessChangedEvent;

public interface IProcessEventHandler {

	void handleEvents(Collection<ProcessChangedEvent> events);

}