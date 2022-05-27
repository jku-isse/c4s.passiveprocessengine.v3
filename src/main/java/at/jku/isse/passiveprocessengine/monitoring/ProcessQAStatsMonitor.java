package at.jku.isse.passiveprocessengine.monitoring;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Autowired;

import at.jku.isse.passiveprocessengine.instance.ProcessInstance;
import at.jku.isse.passiveprocessengine.instance.StepLifecycle.State;
import at.jku.isse.passiveprocessengine.instance.messages.Events.ProcessChangedEvent;
import at.jku.isse.passiveprocessengine.instance.messages.Events.QAConstraintFulfillmentChanged;
import at.jku.isse.passiveprocessengine.instance.messages.Events.StepStateTransitionEvent;
import lombok.extern.slf4j.Slf4j;


@Slf4j
public class ProcessQAStatsMonitor {

	@Autowired
	ITimeStampProvider timeStampProvider;
	
	public Map<ProcessInstance, ProcessStats> stats = new HashMap<>();
	
	public ProcessQAStatsMonitor() {}
	
	@Inject
	public ProcessQAStatsMonitor(ITimeStampProvider tsProvider) {
		this.timeStampProvider = tsProvider;
	}
	
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
			if (event.getOldState().equals(State.AVAILABLE) 
					&& !event.getNewState().equals(State.ENABLED) 
					&& !event.getNewState().equals(State.CANCELED) 
					&& !event.getNewState().equals(State.NO_WORK_EXPECTED) ) { // a premature step
				// we should also check if this was not due to a race condition
				event.getStep().getProcess().getDecisionNodeInstances().stream() 
				.filter(dni -> dni.getDefinition().equals(event.getStep().getDefinition().getInDND()) )							
				.findAny().ifPresent(dni -> { // there must be exactly one
					if (!dni.hasPropagated()) {
						entry.setPrematurelyStarted(timeStampProvider.getLastChangeTimeStamp()); // only if the previous DNI has triggered progress, and hence all prev Tasks QA constraints are fulfilled, can we assume prematurity fixed
					}
				});					
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
			if (event.getOldState().equals(State.AVAILABLE) 
					&& event.getNewState().equals(State.ENABLED) 
					&& entry.getPrematurelyStarted() != null) {
				// AND WE NEED TO CHECK if preceeding tasks are fulfilled, as the precondition of this step is ignorant of the other QA conditions and just might trigger this transition anyway
				event.getStep().getProcess().getDecisionNodeInstances().stream() 
					.filter(dni -> dni.getDefinition().equals(event.getStep().getDefinition().getInDND()) )							
					.findAny().ifPresent(dni -> { // there must be exactly one
						if (dni.hasPropagated()) {
							entry.setPrematureStartRepaired(timeStampProvider.getLastChangeTimeStamp()); // only if the previous DNI has triggered progress, and hence all prev Tasks QA constraints are fulfilled, can we assume prematurity fixed
						}
					});
				
				// we simply check if prematureEntry in stats is filled, if so, there was one and we now register the repair						
			}
		}
	}		
	
	public void calcFinalStats() {
		stats.entrySet().stream()
		.forEach(entry -> {		
			entry.getValue().getPerStepStats().values().stream()
				.peek(stat -> stat.calcEOPqaFulfillment())
				.forEach(stat -> { 
					System.out.println(stat.toString());
					
				});
			boolean fulfilled = isProcessFulfilled(entry.getKey(), entry.getValue().getPerStepStats().values());
			System.out.println("Workflow Complete? "+fulfilled);
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
}
