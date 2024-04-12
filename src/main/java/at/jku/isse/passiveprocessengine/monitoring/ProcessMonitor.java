package at.jku.isse.passiveprocessengine.monitoring;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.jku.isse.passiveprocessengine.instance.messages.Events;
import at.jku.isse.passiveprocessengine.instance.messages.Events.ConstraintOverrideEvent;
import at.jku.isse.passiveprocessengine.instance.messages.Events.ProcessChangedEvent;
import at.jku.isse.passiveprocessengine.instance.messages.Events.QAConstraintFulfillmentChanged;
import at.jku.isse.passiveprocessengine.instance.messages.Events.QAFulfillmentChanged;
import at.jku.isse.passiveprocessengine.instance.messages.Events.StepStateTransitionEvent;
import at.jku.isse.passiveprocessengine.instance.messages.IProcessEventHandler;
import net.logstash.logback.argument.StructuredArgument;

public class ProcessMonitor implements IProcessEventHandler{

	private ITimeStampProvider timeProvider;
	private final Logger monitor = LoggerFactory.getLogger("monitor.process");
	public static enum LogProperties {condition, fulfillment, fromState, toState, stateType, reason, isUndo};
	
	public ProcessMonitor(ITimeStampProvider timeProvider) {
		this.timeProvider = timeProvider;
	}
	
	private StructuredArgument getTime() {
		return kv(UsageMonitor.LogProperties.originTime.toString(), timeProvider.getLastChangeTimeStamp().toString());
	}
	
	private List<StructuredArgument> getDefaultArguments(ProcessChangedEvent pce) {
		List<StructuredArgument> args = new LinkedList<>();
		args.add(kv(UsageMonitor.LogProperties.rootProcessInstanceId.toString(), UsageMonitor.getRootProcessInstanceId(pce.getProcScope()))); 
		args.add(kv(UsageMonitor.LogProperties.processInstanceId.toString(), pce.getProcScope().getName()));
		args.add(kv(UsageMonitor.LogProperties.processDefinitionId.toString(), pce.getProcScope().getDefinition().getName()));
		if (pce.getStep() != null) {
			args.add(kv(UsageMonitor.LogProperties.stepDefinitionId.toString(), pce.getStep().getDefinition().getName()));
		} else {
			args.add(kv(UsageMonitor.LogProperties.stepDefinitionId.toString(), pce.getProcScope().getDefinition().getName())); //for processes use process definition instead of step name
		}
		args.add(kv(UsageMonitor.LogProperties.eventType.toString(), pce.getClass().getSimpleName()));
		args.add(getTime());
		return args;
	}
	
	public void logProcessChangedEvent(ProcessChangedEvent pce) {
		List<StructuredArgument> args = getDefaultArguments(pce);
		if (pce instanceof Events.ConditionFulfillmentChanged) {
			Events.ConditionFulfillmentChanged e = (Events.ConditionFulfillmentChanged) pce;
			args.add(3, kv(LogProperties.condition.toString(), e.getCondition()));
			args.add(4, kv(LogProperties.fulfillment.toString(), e.isFulfilled()));
		} else if (pce instanceof StepStateTransitionEvent) {
			StepStateTransitionEvent e = (StepStateTransitionEvent) pce;
			args.add(3, kv(LogProperties.fromState.toString(), e.getOldState()));
			args.add(4, kv(LogProperties.toState.toString(), e.getNewState()));
			args.add(5, kv(LogProperties.stateType.toString(), e.isActualState() ? "ACTUAL" : "EXPECTED"));
		} else if (pce instanceof QAFulfillmentChanged) {
			QAFulfillmentChanged e = (QAFulfillmentChanged)pce;
			args.add(3, kv(LogProperties.fulfillment.toString(), e.isFulfilled()));
		} else if (pce instanceof ConstraintOverrideEvent) { 
			ConstraintOverrideEvent e = (ConstraintOverrideEvent)pce;
			args.add(3, kv(UsageMonitor.LogProperties.constraintId.toString(), e.getQacWrapper().getSpec().getConstraintId()));
			args.add(4, kv(LogProperties.fulfillment.toString(), e.getQacWrapper().getEvalResult()));
			args.add(5, kv(LogProperties.reason.toString(), e.getReason()));
			args.add(6, kv(LogProperties.isUndo.toString(), e.isUndo()));
		} else if (pce instanceof  QAConstraintFulfillmentChanged) {
			QAConstraintFulfillmentChanged e = (QAConstraintFulfillmentChanged)pce;
			args.add(3, kv(UsageMonitor.LogProperties.constraintId.toString(), e.getQacWrapper().getSpec().getConstraintId()));
			args.add(4, kv(LogProperties.fulfillment.toString(), e.getQacWrapper().getEvalResult()));			
		} // we ignore DataMappingChangeEvents for now
		monitor.info("ProcessChangedEvent", args.toArray() );
	}

	@Override
	public void handleEvents(Collection<ProcessChangedEvent> events) {
		events.stream()
		.forEach(event -> {
			event.setTimestamp(timeProvider.getLastChangeTimeStamp()); 
			logProcessChangedEvent(event);
		});
	}
}
