package at.jku.isse.passiveprocessengine.instance.messages;

import java.util.Collection;

import at.jku.isse.passiveprocessengine.instance.messages.Events.ProcessChangedEvent;
import at.jku.isse.passiveprocessengine.instance.messages.Events.StepStateTransitionEvent;
import at.jku.isse.passiveprocessengine.monitoring.ProcessQAStatsMonitor;

public class EventDistributor {

	ProcessQAStatsMonitor monitor;
	
	public EventDistributor(ProcessQAStatsMonitor monitor) {
		this.monitor = monitor;
	}
	
	public void handleEvents(Collection<ProcessChangedEvent> events) {
		monitor.handleEvents(events);
	}
}
