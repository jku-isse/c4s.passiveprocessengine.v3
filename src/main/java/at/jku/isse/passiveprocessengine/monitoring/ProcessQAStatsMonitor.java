package at.jku.isse.passiveprocessengine.monitoring;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import at.jku.isse.passiveprocessengine.instance.ProcessInstance;
import at.jku.isse.passiveprocessengine.instance.ProcessStep;
import at.jku.isse.passiveprocessengine.instance.StepLifecycle.Conditions;
import at.jku.isse.passiveprocessengine.instance.StepLifecycle.State;
import at.jku.isse.passiveprocessengine.instance.messages.Events.ConditionFulfillmentChanged;
import at.jku.isse.passiveprocessengine.instance.messages.Events.ProcessChangedEvent;
import at.jku.isse.passiveprocessengine.instance.messages.Events.QAConstraintFulfillmentChanged;
import at.jku.isse.passiveprocessengine.instance.messages.Events.QAFulfillmentChanged;
import at.jku.isse.passiveprocessengine.instance.messages.Events.StepStateTransitionEvent;
import at.jku.isse.passiveprocessengine.instance.messages.IProcessEventHandler;
import lombok.extern.slf4j.Slf4j;


@Slf4j
public class ProcessQAStatsMonitor implements IProcessEventHandler {

	@Autowired
	ITimeStampProvider timeStampProvider;
	
	public Map<ProcessInstance, ProcessStats> stats = new HashMap<>();
	
	public ProcessQAStatsMonitor() {}
	
	public ProcessQAStatsMonitor(ITimeStampProvider tsProvider) {
		this.timeStampProvider = tsProvider;
	}
	
	public void reset() {
		stats.clear();
	}
	
	@Override
	public void handleEvents(Collection<ProcessChangedEvent> events) {
		events.stream()
		.filter(StepStateTransitionEvent.class::isInstance)
		.map(StepStateTransitionEvent.class::cast)
		.forEach(event -> handleEvent(event));
		
		// now trigger intermediary QA calculation
		events.stream()
		.filter(QAConstraintFulfillmentChanged.class::isInstance)
		.map(QAConstraintFulfillmentChanged.class::cast)
		.map(event -> event.getStep())
		.distinct()
		.filter(step -> step.getProcess() != null) // wont handle process instance 
		.forEach(step -> {
			ProcessStats perProcStats = stats.computeIfAbsent(step.getProcess(), k->new ProcessStats(step.getProcess().getName()) );
			ProcessStepStats entry = perProcStats.getPerStepStats().computeIfAbsent(step, k-> new ProcessStepStats(step));
			entry.calcMidQaFulfillment();
		});
		
		determineUnsafeAndPrematureChanges(events);
	}
	
	
	private void determineUnsafeAndPrematureChanges(Collection<ProcessChangedEvent> events) {
		// go through QA changed events to check when there might be downstream Unsafe Modes relevant, same for premature start
		// requires ability to travers downstream
		events.stream()
		.filter(QAFulfillmentChanged.class::isInstance)
		.map(QAFulfillmentChanged.class::cast)
		.forEach(qac -> {
			// if we are not in canceled or no work expected then signal downstream
			if (qac.getStep().getExpectedLifecycleState().equals(State.CANCELED) || qac.getStep().getExpectedLifecycleState().equals(State.NO_WORK_EXPECTED))
				return;
			// if now fulfilled, check any unsafe mode steps below, there might be other steps that cause unsafe mode, hence we cannot automatically flip them.
			if (qac.isFulfilled()) {
				getAllRelevantDownstreamSteps(qac.getStep()).stream().forEach(dss -> {
					if (dss.isInUnsafeOperationModeDueTo().isEmpty()) {
						ProcessStats perProcStats = stats.computeIfAbsent(dss.getProcess(), k->new ProcessStats(dss.getProcess().getName()) );
						ProcessStepStats entry = perProcStats.getPerStepStats().computeIfAbsent(dss, k-> new ProcessStepStats(dss));
						entry.setUnsafeStartRepaired(timeStampProvider.getLastChangeTimeStamp());
					}
				});
			} else {
			// if no longer fulfilled, set all steps below to unsafe
				getAllRelevantDownstreamSteps(qac.getStep()).stream().forEach(dss -> {
					ProcessStats perProcStats = stats.computeIfAbsent(dss.getProcess(), k->new ProcessStats(dss.getProcess().getName()) );
					ProcessStepStats entry = perProcStats.getPerStepStats().computeIfAbsent(dss, k-> new ProcessStepStats(dss));
					entry.setUnsafeStarted(timeStampProvider.getLastChangeTimeStamp());
				});
			}
		});
		
		events.stream()
		.filter(ConditionFulfillmentChanged.class::isInstance)
		.map(ConditionFulfillmentChanged.class::cast)
		.filter(e -> e.getCondition().equals(Conditions.POSTCONDITION))
		.forEach(pfc -> { 
			// if we are not in canceled or no work expected then signal downstream
			if (pfc.getStep().getExpectedLifecycleState().equals(State.CANCELED) || pfc.getStep().getExpectedLifecycleState().equals(State.NO_WORK_EXPECTED))
				return;
			// if now fulfilled, check any prematue mode steps below, there might be other steps that cause premature mode, hence we cannot automatically flip them.
			if (pfc.isFulfilled()) {
				getAllRelevantDownstreamSteps(pfc.getStep()).stream().forEach(dss -> {
					if (dss.isInPrematureOperationModeDueTo().isEmpty()) {
						ProcessStats perProcStats = stats.computeIfAbsent(dss.getProcess(), k->new ProcessStats(dss.getProcess().getName()) );
						ProcessStepStats entry = perProcStats.getPerStepStats().computeIfAbsent(dss, k-> new ProcessStepStats(dss));
						entry.setPrematureStartRepaired(timeStampProvider.getLastChangeTimeStamp());
					}
				});
			} else {
				// if no longer fulfilled, set all steps below to premature
				getAllRelevantDownstreamSteps(pfc.getStep()).stream().forEach(dss -> {
					ProcessStats perProcStats = stats.computeIfAbsent(dss.getProcess(), k->new ProcessStats(dss.getProcess().getName()) );
					ProcessStepStats entry = perProcStats.getPerStepStats().computeIfAbsent(dss, k-> new ProcessStepStats(dss));
					entry.setPrematurelyStarted(timeStampProvider.getLastChangeTimeStamp());
				});
			}			
		});
	}
	
	private Set<ProcessStep> getAllRelevantDownstreamSteps(ProcessStep root) {
		if (root.getOutDNI() == null || root.getOutDNI().getOutSteps() == null) // in case we hit a process instance or the end of the process
			return Collections.emptySet();
		// also we dont continue if this step should not be relevant, i.e., canceled or no work expected
		if (root.getExpectedLifecycleState().equals(State.CANCELED) || root.getExpectedLifecycleState().equals(State.NO_WORK_EXPECTED))
			return Collections.emptySet();
					
		Set<ProcessStep> downstream = new HashSet<>();
		root.getOutDNI().getOutSteps().forEach(step -> { 
			downstream.add(step); 
			downstream.addAll(getAllRelevantDownstreamSteps(step));
			});
		return downstream;
	}
	
	private void handleEvent(StepStateTransitionEvent event) {
		log.debug(String.format("%-30s [%s]: %16s --> %-16s", event.getStep().getName(), event.isActualState() ? "A" : "E", event.getOldState(), event.getNewState()));
		// if this is a root process, then just record last state
		if (event.getStep().getProcess() == null) { 
			ProcessStats perProcStats = stats.computeIfAbsent((ProcessInstance) event.getStep(), k->new ProcessStats(event.getStep().getName()) );
			if (event.isActualState())
				perProcStats.setLastActualState(event.getNewState());
			else 
				perProcStats.setLastExpectedState(event.getNewState());
			return;
		}
		ProcessStats perProcStats = stats.computeIfAbsent(event.getStep().getProcess(), k->new ProcessStats(event.getStep().getProcess().getName()) );
		ProcessStepStats entry = perProcStats.getPerStepStats().computeIfAbsent(event.getStep(), k-> new ProcessStepStats(event.getStep()));
		if (event.isActualState()) {
			// count how often a task was canceled (paused)
			if (event.getNewState().equals(State.CANCELED)) {					
				entry.countCancelled++;
			}
//			if (event.getOldState().equals(State.AVAILABLE) //No longer used as: THE IMMEDIATE DATA PROPAGATION AND STEP activation causes this to fail premature start detection!
//					&& !event.getNewState().equals(State.ENABLED) 
//					&& !event.getNewState().equals(State.CANCELED) 
//					&& !event.getNewState().equals(State.NO_WORK_EXPECTED) ) { // a premature step
//				// we should also check if this was not due to a race condition
//				event.getStep().getProcess().getDecisionNodeInstances().stream() 
//				.filter(dni -> dni.getDefinition().equals(event.getStep().getDefinition().getInDND()) )							
//				.findAny().ifPresent(dni -> { // there must be exactly one
//					if (!dni.hasPropagated()) {
//						entry.setPrematurelyStarted(timeStampProvider.getLastChangeTimeStamp()); // only if the previous DNI has triggered progress, and hence all prev Tasks QA constraints are fulfilled, can we assume prematurity fixed
//					}
//				});					
//			}
			if (event.getOldState().equals(State.AVAILABLE) && event.getStep().isInPrematureOperationModeDueTo().size() > 0) { // a premature start
				entry.setPrematurelyStarted(timeStampProvider.getLastChangeTimeStamp());
			}
			if (event.getOldState().equals(State.AVAILABLE) && event.getStep().isInUnsafeOperationModeDueTo().size() > 0) { // a unsafe start
				entry.setUnsafeStarted(timeStampProvider.getLastChangeTimeStamp());
				//we check here for unsafe transition, i.e., when after the start the prior step QA conditions are no longer fulfilled
				// we check when unsafe mode is repaired, i.e., a prior step has now all QA conditions fulfilled by monitoring QA status in separate method
			}
			if (event.getNewState().equals(State.ACTIVE) && 
					!(event.getOldState().equals(State.AVAILABLE) 
							|| event.getOldState().equals(State.ENABLED))) { // a reactivated/repeated step
				entry.countReactivated++;
			}
			if (event.getNewState().equals(State.AVAILABLE)) { // a revoked step as any new step automatically goes into AVAILABLE without an event, thus this event here means the task was already progressed
				entry.countRevoked++;
			}
			
		} else { // expected state change
			// if premature start was fixed: that is when expected changes from available to enabled, 
			// but actual state is already there.
			// BEWARE: actualState change is published before expectedState change					
			// NOT SURE THIS IS RELIABLE, WE DO THIS DIfferENTLY as we rather track if prior steps are at any stage not in completed anymore
//			if (event.getOldState().equals(State.AVAILABLE) 
//					&& event.getNewState().equals(State.ENABLED) 
//					&& entry.getPrematurelyStarted() != null) {
//				// AND WE NEED TO CHECK if preceeding tasks are fulfilled, as the precondition of this step is ignorant of the other QA conditions and just might trigger this transition anyway
//				event.getStep().getProcess().getDecisionNodeInstances().stream() 
//					.filter(dni -> dni.getDefinition().equals(event.getStep().getDefinition().getInDND()) )							
//					.findAny().ifPresent(dni -> { // there must be exactly one
//						if (dni.hasPropagated()) {
//							entry.setPrematureStartRepaired(timeStampProvider.getLastChangeTimeStamp()); // only if the previous DNI has triggered progress, and hence all prev Tasks QA constraints are fulfilled, can we assume prematurity fixed
//						}
//					});					
//			}
		}
	}		
	
	public void calcFinalStats() {
		stats.entrySet().stream()
		.forEach(entry -> {		
			entry.getValue().getPerStepStats().values().stream()
				.forEach(stat -> stat.calcEOPqaFulfillment());
			boolean fulfilled = isProcessFulfilled(entry.getKey(), entry.getValue().getPerStepStats().values());
			//log.debug("Workflow Complete? "+fulfilled);
			entry.getValue().setProcessCompleted(fulfilled);		
		});
	}
	
	private static boolean isProcessFulfilled(ProcessInstance wfi, Collection<ProcessStepStats> stats) {		
		// for every task defined in WFD, check if the corresponding task is either expected to be canceled, noworkexpected, or completed
		return wfi.getDefinition().getStepDefinitions().stream()
			.map(sd -> stats.stream()
						.filter( stat -> stat.getStep().getDefinition().getName().equals(sd.getId()) )
						.findAny() 
						)
			.allMatch(optStat -> optStat.isPresent() && 
								( optStat.get().getStep().getExpectedLifecycleState().equals(State.COMPLETED)
										|| optStat.get().getStep().getExpectedLifecycleState().equals(State.NO_WORK_EXPECTED)
										|| optStat.get().getStep().getExpectedLifecycleState().equals(State.CANCELED)
								)
					);
	}
	
	private static Gson gson = new GsonBuilder()
			 .registerTypeAdapterFactory(new at.jku.isse.passiveprocessengine.monitoring.serialization.MultiTypeAdapterFactory())
			 .setPrettyPrinting()
			 .create();
	
	public String stats2Json(Collection<ProcessStats> stats) {
		return gson.toJson(stats);
	}
}
