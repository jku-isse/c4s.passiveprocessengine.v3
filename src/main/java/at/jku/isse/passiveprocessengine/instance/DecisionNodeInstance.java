//package at.jku.isse.passiveprocessengine.instance;
//
//import artifactapi.ArtifactIdentifier;
//import at.jku.isse.passiveprocessengine.instance.StepLifecycle.State;
//
//import com.github.oxo42.stateless4j.StateMachine;
//import lombok.extern.slf4j.Slf4j;
//import passiveprocessengine.definition.DecisionNodeDefinition;
//import passiveprocessengine.definition.DecisionNodeDefinition.Events;
//import passiveprocessengine.definition.DecisionNodeDefinition.InFlowType;
//import passiveprocessengine.definition.DecisionNodeDefinition.States;
//import passiveprocessengine.definition.IWorkflowTask;
//import passiveprocessengine.definition.MappingDefinition;
//import passiveprocessengine.instance.ExecutedMapping.DIR;
//import passiveprocessengine.instance.events.DNIUpdateEvent;
//import passiveprocessengine.instance.events.TaskArtifactEvent;
//import passiveprocessengine.instance.events.ProcessChangeEvent;
//import passiveprocessengine.instance.events.ProcessChangeEvent.ChangeType;
//
//import static passiveprocessengine.instance.events.ProcessChangeEvent.ChangeType.*;
//
//import java.util.*;
//import java.util.stream.Collectors;
//
//@Slf4j
//public class DecisionNodeInstance  {
//
//	
//	private DecisionNodeDefinition ofType;
//	
////	boolean taskCompletionConditionsFullfilled = false;
//	
////	boolean taskActivationConditionsFullfilled = false;
//	
//	boolean inflowFulfilled = false;
//		
//	boolean activationPropagationCompleted = false;
//
//	private HashMap<String, Progress> inTaskStatus = new HashMap<>();
//
//	private enum Progress {
//		WAITING, ENABLED, DISABLED, USED
//	}
//	
//	private List<ExecutedMapping> mappings = new LinkedList<>();
//
//	public List<ExecutedMapping> getExecutedMappings() {
//		return mappings;
//	}
//	
//	
//	@Deprecated
//	public DecisionNodeInstance() {
//		super();
//	}
//	
//	public DecisionNodeInstance(DecisionNodeDefinition ofType, WorkflowInstance workflow) {
//		//super(ofType.getId()+"#"+UUID.randomUUID().toString(), workflow);
//		super(ofType.getId()+"#"+workflow.getId().toString(), workflow);
//		this.ofType = ofType;
//	
//		if (workflow.getType().getTasksFlowingInto(ofType).isEmpty() ) {
//			inflowFulfilled = true;
//			
//		}
//	}
//	
//	public DecisionNodeDefinition getDefinition() {
//		return ofType;
//	}
//	
//	private Progress mapInitialStatus(WorkflowTask task) {
//		if (task.getExpectedLifecycleState().equals(State.COMPLETED))
//			return Progress.ENABLED;
//		else if (task.getExpectedLifecycleState().equals(State.NO_WORK_EXPECTED) || task.getExpectedLifecycleState().equals(State.CANCELED))
//			return Progress.DISABLED;
//		else 
//			return Progress.WAITING;
//	}
//	
//	private Progress mapExistingStatus(ProcessStep task, Progress currentState) {
//		switch(currentState) {
//			case WAITING:
//				if(task.getExpectedLifecycleState().equals(State.COMPLETED))
//					return Progress.ENABLED;
//				else if (task.getExpectedLifecycleState().equals(State.NO_WORK_EXPECTED) || task.getExpectedLifecycleState().equals(State.CANCELED))
//					return Progress.DISABLED;
//				else
//					return Progress.WAITING;
//			case DISABLED:
//				//canceling a step, will disable it here, but we have no signal when it becomes available again, thus it will permanently remain in DISABLED for the moment
//				if(task.getExpectedLifecycleState().equals(State.COMPLETED)) { 
//					// we dont override here, how to handle switching between exclusive tasks! --> one needs to be deactivated/canceled or reverted
//					// distinguish between having progressed and not having progressed -->
//					// IF we have no other ready yet, then transition into ENABLED, 
//					boolean isAnotherEnabled = inTaskStatus.entrySet().stream()
//						.filter(entry -> !entry.getKey().equals(task.getId()))
//						.anyMatch(entry -> entry.getValue().equals(Progress.ENABLED));
//					if (!isAnotherEnabled)
//						return Progress.ENABLED;										
//				}				
//				return Progress.DISABLED;
//			case ENABLED:
//				if(task.getExpectedLifecycleState().equals(State.COMPLETED))
//					return Progress.ENABLED;
//				else if (task.getExpectedLifecycleState().equals(State.NO_WORK_EXPECTED) || task.getExpectedLifecycleState().equals(State.CANCELED))
//					return Progress.DISABLED;
//				else // if no longer complete, returned to some other state // TODO: what if there is a deviation?
//					return Progress.WAITING;
//			case USED:
//				if(task.getExpectedLifecycleState().equals(State.NO_WORK_EXPECTED) || task.getExpectedLifecycleState().equals(State.CANCELED)) {
//					// TODO if used but now disabled, then lets check if some data remapping needs to be done
//				}
//				return Progress.USED;
//		}
//		return currentState;
//	}
//	
//	public List<ProcessChangeEvent> signalPrevTaskDataChanged(IWorkflowTask prevTask, ProcessChangeEvent cause) {
//		// --> check mappings what needs to be added or deleted from subsequent tasks!!!
//		// if so, then check datamappings if they have mapped something that needs to be removed, or have not yet mapped something they now need to map
//
//		List<ProcessChangeEvent> changes = new ArrayList<>();
//
//		// add
//		List<MappingDefinition> definitionsToExecute = ofType.getMappings().stream()
//				.filter(def -> def.getFrom().stream()
//						.anyMatch(pair -> pair.getFirst().equals(prevTask.getType().getId()))) // search all defined mappings with prevTask
//				.filter(def -> mappings.stream()
//						.noneMatch(mapping -> def.getFrom().stream().anyMatch(pair -> pair.getFirst().equals(mapping.getFromTask().getType().getId())))) // use only defined mappings that are not already executed
//				.collect(Collectors.toList());
//		checkAndExecuteDataMappings(definitionsToExecute, cause);
//
//		// remove
//		List<ExecutedMapping> mappingsToRemove = new ArrayList<>();
//		mappings.stream()
//				.filter(m -> m.getFromTask().equals(prevTask)) // check if prevTask has been used, if not, nothing to do
//				.filter(m -> prevTask.getOutput().stream()
//						.noneMatch(ai -> m.getFromRole().equals(ai.getRole()))) // search for executed mappings that were removed
//				.forEach(m -> {
//					switch (m.getDirection()) {
//						case inToIn: case outToIn:
//							changes.addAll(m.getToTask().removeInput(new ArtifactInput(m.getArtifact(), m.getToRole()), cause));
//							break;
//						case inToOut: case outToOut:
//							changes.addAll(m.getToTask().removeOutput(new ArtifactOutput(m.getArtifact(), m.getToRole()), cause));
//							break;
//					}
//					mappingsToRemove.add(m);
//				});
//		mappings.removeAll(mappingsToRemove); // remove the undone mappings
//
//		return changes;
//	}
//	
//	public List<ProcessChangeEvent> tryInConditionsFullfilled(ProcessChangeEvent cause) {
//		// make sure we have latest status in our mapping
//		//List<WorkflowChangeEvent> changes = new ArrayList<>();
//		//WorkflowChangeEvent wce = null;
//		this.workflow.getTasksFlowingInto(this).stream()
//			.peek(task -> inTaskStatus.computeIfPresent(task.getId(), (key, value) -> mapExistingStatus(task, value)))
//			.forEach(task -> inTaskStatus.computeIfAbsent(task.getId(), k -> mapInitialStatus(task))  );
//		
//		
//		switch(ofType.getInBranchingType()) {
//		case AND: 
//			if (inTaskStatus.values().stream()
//					.filter(p -> !(p.equals(Progress.DISABLED))) // we ignore those
//					.allMatch(p -> p.equals(Progress.ENABLED)) ) { // if non are left, we still progress trusting that any canceled or no work expected tasks are not required anymore
//				inTaskStatus.entrySet().stream()
//					.filter(entry -> !entry.getValue().equals(Progress.DISABLED) ) // we ignore those
//					.peek(entry -> entry.setValue(Progress.USED))
//					.collect(Collectors.toSet())
//					.forEach(entry2 -> inTaskStatus.put(entry2.getKey(), entry2.getValue()));
//				return setTaskCompletionConditionsFullfilled(cause);
//			}
//			break;
//		case OR:
//			if (inTaskStatus.values().stream()
//					.filter(p -> !(p.equals(Progress.DISABLED))) // we ignore those
//					.anyMatch(p -> p.equals(Progress.ENABLED)) ) {
//				inTaskStatus.entrySet().stream()
//					.filter(entry -> entry.getValue().equals(Progress.ENABLED) ) // we use all ready ones
//					.map(entry -> { entry.setValue(Progress.USED); 
//									return entry; })
//					.collect(Collectors.toSet())
//					.forEach(entry2 -> inTaskStatus.put(entry2.getKey(),  entry2.getValue()));
////				// this will be called several times, if branches become gradually enabled, for each new branch enabled, we need to check if data transfer has to happen
//				return setTaskCompletionConditionsFullfilled(cause);
//			}
//			break;
//		case XOR:
//			Optional<String> optId = inTaskStatus.entrySet().stream()
//					.filter(entry -> entry.getValue().equals(Progress.ENABLED))
//					.findAny()
//					.map(entry -> entry.getKey());
//			if (optId.isPresent()) {
//				String id = optId.get();
//				inTaskStatus.put(id, Progress.USED);
//				inTaskStatus.entrySet().stream()
//				.filter(entry -> entry.getValue().equals(Progress.ENABLED) || entry.getValue().equals(Progress.WAITING) ) 
//				.map(entry -> { workflow.getWorkflowTask(entry.getKey()).setWorkExpected(false, cause); //this only works for XORs that have no sub branches, 
//								entry.setValue(Progress.DISABLED); 
//								return entry; })
//				.collect(Collectors.toSet())
//				.forEach(entry2 -> inTaskStatus.put(entry2.getKey(),  entry2.getValue()));
//				// TOOD: find all tasks that have not reached until here, (which is when one xor branch is substructured and has not progressed as far yet to pop up as branch here yet)	
//				List<ProcessChangeEvent> awos = new ArrayList<>();
//				if (this.ofType.countIncomingTasks() > 1) {// there are other inbranches that we now need to deactivate
//					List<DecisionNodeInstance> path = findCorrespondingOutFlowPath(this, id); //the path to the corresponding outflow node via the just activating task
//					if (path != null) {
//						path.add(0, this);
//						// deactivate those not on path
//						DecisionNodeInstance correspondingDNI = path.get(path.size()-1);
//						DecisionNodeInstance exclNextDNI = path.size() >= 2 ? path.get(path.size()-2) : null;						
//						deactivateTasksOn(correspondingDNI, exclNextDNI, this, awos, cause); // TODO 'awos' is always empty
//					}
//				}				
//				awos.addAll(this.setTaskCompletionConditionsFullfilled(cause));
//				return awos;
//			}		
////			if (inBranches.stream()
////					//.filter(b -> b.getState()!=BranchState.Disabled) // we ignore those
////					.anyMatch(b -> b.getState()==BranchState.TransitionEnabled)) {
////					Optional<IBranchInstance> selectedB = inBranches.stream()
////						.filter(b -> b.getState()==BranchState.TransitionEnabled)
////						.findAny() 
////						.map(b -> {
////							b.setBranchUsedForProgress();
////							return b;
////						});
////					if (selectedB.isPresent()) {
////						// we need to mark other branch tasks as: disabled (if they are available or active), or canceled otherwise 
////						inBranches.stream()
////						    .filter(b -> b.getState()!=BranchState.Disabled) // we ignore those	
////						    .filter(b -> b.getState()!=BranchState.TransitionPassed) // filter out the branch we just tagged as used
////							.map(b -> b.getTask()) // we dont change other branch states to be able to detect when another step is ready but should not have been executed
////							.forEach(t -> { 
////								t.signalEvent(TaskLifecycle.Triggers.IGNORE_FOR_PROGRESS);
//////								switch(t.getLifecycleState()) {
//////								case AVAILABLE:
//////								case ENABLED:
//////									t.setLifecycleState(TaskLifecycle.State.DISABLED);
//////									break;
//////								case DISABLED:
//////								case CANCELED:
//////									break;
//////								default:
//////									t.setLifecycleState(TaskLifecycle.State.CANCELED);
//////									break;
//////								}
////							}); 
////						return this.setTaskCompletionConditionsFullfilled();
////					}
//			break;
//		default:
//			break;
//		}
////		return setTaskCompletionConditionsNoLongerHold(); // if the inconditions dont hold, then check if we need to transition
//		// check if we have passed outbranch conditions
//		return checkDelayedOutput(cause);
//		//return Collections.emptySet();
//	}
//	
//	private static void deactivateTasksOn(DecisionNodeInstance thisDNI, DecisionNodeInstance nextExcludeDNI, DecisionNodeInstance finalDNI, List<ProcessChangeEvent> collectedAffectedWFOs, ProcessChangeEvent cause) {
//		thisDNI.getWorkflow().getTasksFlowingOutOf(thisDNI).stream()
//			.filter(task -> (nextExcludeDNI == null) || task.getOutDNI() != nextExcludeDNI) // we filter out those on the path
//			.map(task -> {
//				if (!task.getExpectedLifecycleState().equals(State.NO_WORK_EXPECTED)) {					
//					collectedAffectedWFOs.addAll(task.setWorkExpected(false, cause));
//				} // else we dont set this again, but we need to travers onwards, thus we cant filter out them 
//				return task.getOutDNI();
//			})
//			.filter(dni -> dni != finalDNI) // we stop once we reache finalDNI
//			.forEach(dni -> deactivateTasksOn(dni, null, finalDNI, collectedAffectedWFOs, cause));
//							
//	}
//	
//
//	
//	
//	private List<ProcessChangeEvent> checkDelayedOutput(ProcessChangeEvent cause) {
//		if (sm.getState().equals(States.PASSED_OUTBRANCH_CONDITIONS)) {
//			// means we have fulfilled the conditions some other time and have conducted data mapping to the next steps
//			return tryActivationPropagation(cause);
//		}
//		else 
//			return Collections.emptyList();
//	}
//	
//	public List<ProcessChangeEvent> setTaskCompletionConditionsFullfilled(ProcessChangeEvent cause) {
//	// set externally from Rule Engine, or internally once in branches signal sufficient
//		/// if we have progressed, and we call this here again, two options: either wrongfully called from external, or
//		// we are in an OR inflow --> then a new branch is ready
//		// we are in an XOR inflow --> then an alternative is ready, that shoulds not be --> all this should be checked earlier
//		// advance state machine
//		if (this.getState().equals(DecisionNodeDefinition.States.PASSED_INBRANCH_CONDITIONS)) { // no need to signal again
//			return tryContextConditionsFullfilled(cause);
//		} else {
//			List<ProcessChangeEvent> changes = new LinkedList<>();
//			ProcessChangeEvent wce = new DNIUpdateEvent(ChangeType.INFLOW_OK, this, this.getWorkflow(), cause);
//			changes.add(wce);
//			fireIfPossible(Events.INBRANCHES_FULFILLED);
//			wce.getCascadingEffect().addAll(tryContextConditionsFullfilled(wce));
//			return changes;
//		}
//	}
//	
////	public Set<AbstractWorkflowInstanceObject> setTaskCompletionConditionsNoLongerHold() {
////		// set externally from Rule Engine, or internally once in branches signal sufficient
////			//this.taskCompletionConditionsFullfilled = false;
////			// advance state machine
////			fireIfPossible(Events.INCONDITIONS_NO_LONGER_HOLD);
////			// TODO: check what needs to be done
////			//return tryContextConditionsFullfilled();
////			return Collections.emptySet();
////		}
//
//	public List<ProcessChangeEvent> tryContextConditionsFullfilled(ProcessChangeEvent cause) {
//		if (isContextConditionsFullfilled() || !ofType.hasExternalContextRules) {// if context conditions already evaluated to true, OR DND claims there are no external rules
//			return setContextConditionsFullfilledInternal(cause); 
//		}
//		return Collections.emptyList();
//	}
//	
////	public boolean isTaskCompletionConditionsFullfilled() {
////		return taskCompletionConditionsFullfilled;
////	}
//
//	
//	
////	public void setOutConditionsFullfilled() {
////		outBranches.values().stream()
////			.forEach(b -> b.setConditionsFulfilled());
////	}
//	
//	public boolean isActivationPropagationCompleted() {
//		return activationPropagationCompleted;
//	}
//
//	public boolean isContextConditionsFullfilled() {
//		return contextConditionsFullfilled;
//	}
//
//	public List<ProcessChangeEvent> setContextConditionsFullfilled(ProcessChangeEvent cause) {
//		this.contextConditionsFullfilled = true; // in any case set context to true again
//		if (sm.canFire(Events.CONTEXT_FULFILLED)) { // only when in right state, do we progress, e.g., if just to signal context ok, but in task completion not, then dont progress
//			sm.fire(Events.CONTEXT_FULFILLED);
//			return tryOutConditionsFullfilled(cause); 
//		}
//		return Collections.emptyList();
//	}
//	
//	private List<ProcessChangeEvent> setContextConditionsFullfilledInternal(ProcessChangeEvent cause) {
//		this.contextConditionsFullfilled = true; // in any case set context to true again
//		fireIfPossible(Events.CONTEXT_FULFILLED); // this allows to trigger more data transfer even when we have passed context conditions previously 
//		return tryOutConditionsFullfilled(cause);
//	}
//
//	public Set<AbstractWorkflowInstanceObject> setContextConditionsNoLongerHold() {
//		this.contextConditionsFullfilled = false;
//		// only when in right state, i.e., when transition matters
//		fireIfPossible(Events.CONTEXT_NO_LONGER_HOLD);
//		// TODO: check what needs to be done
//		//return tryOutConditionsFullfilled(); 
//		return Collections.emptySet();
//	}
//	
//	public List<ProcessChangeEvent> tryOutConditionsFullfilled(ProcessChangeEvent cause) {
////		switch(ofType.getOutBranchingType()) {
////		case SYNC: 
////			if (outBranches.stream()
////					.filter(b -> b.getState()!=BranchState.Disabled) // we ignore those
////					.allMatch(b -> b.getState()==BranchState.TransitionEnabled || // either we are enabled, or we are waiting for a never active activation conditions
////									(b.getState()==BranchState.Waiting && !b.getBranchDefinition().hasActivationCondition()) ) ) {
////				// this will only be called once, as either all steps are used for progress or none
////				//this.taskActivationConditionsFullfilled = true;
////				fireIfPossible(Events.OUTBRANCHES_FULFILLED);
////				return tryActivationPropagation();
////			}
////			break;
////		case ASYNC:
////			if (outBranches.stream()
////					.filter(b -> b.getState()!=BranchState.Disabled )
////					.anyMatch(b -> b.getState()==BranchState.TransitionEnabled || // either we are enabled, or we are waiting for a never active activation conditions
////							(b.getState()==BranchState.Waiting && !b.getBranchDefinition().hasActivationCondition()) ) ) {
////				this.taskActivationConditionsFullfilled = true;
////				fireIfPossible(Events.OUTBRANCHES_FULFILLED);
////				return tryActivationPropagation();
////				// this enables all Steps that are ready at this state, later available steps need to be supported as well
////			}
////			break;
////		}
//		//return Collections.emptySet();
//		
////		CURRENTLY THERE IS NO WAY TO CHECK IF THE PRECONDITION OF A STEP ARE FULFILLED, so allow for SYNCED start, thus we async everything
//		// we would need to different DNI that allow this, e.g. for a if/else, 
//		return tryActivationPropagation(cause);
//		
//	}
//	
////	public boolean isTaskActivationConditionsFullfilled() {
////		return taskActivationConditionsFullfilled;
////	}
//
////	public Set<AbstractWorkflowInstanceObject> activateOutBranch(String branchId) {
////		//outBranches.getOrDefault(branchId, dummy).setConditionsFulfilled();
////		outBranches.stream()
////		.filter(b -> b.getBranchDefinition().getName().equals(branchId))
////		.findAny()
////		.ifPresent(b -> b.setConditionsFulfilled());
////		return tryOutConditionsFullfilled();
////	}
////	
////	public Set<AbstractWorkflowInstanceObject> activateOutBranches(String... branchIds) {
////		for (String id : branchIds) {
////			outBranches.stream()
////			.filter(b -> b.getBranchDefinition().getName().equals(id))
////			.findAny()
////			.ifPresent(b -> b.setConditionsFulfilled());
////		}
////		return tryOutConditionsFullfilled();
////	}
//	
////	public Set<AbstractWorkflowInstanceObject> activateInBranch(String branchId) {
////		//inBranches.getOrDefault(branchId, dummy).setConditionsFulfilled();
////		inBranches.stream()
////		.filter(b -> b.getBranchDefinition().getName().equals(branchId))
////		.findAny()
////		.ifPresent(b -> b.setConditionsFulfilled());
////		switch(sm.getState()) {
////		case AVAILABLE:
////			return tryInConditionsFullfilled();
////		case PASSED_INBRANCH_CONDITIONS: // we have an OR, have now additional branches active, but context not yet ok
////			return tryContextConditionsFullfilled();	
////		case PASSED_CONTEXT_CONDITIONS: // we have context ok, but not outbranch conditions fulfilled, typically a SYNC start
////			return tryOutConditionsFullfilled();
////		case PASSED_OUTBRANCH_CONDITIONS: // we have activated a few out steps already, lets see if we can map some more output data, with automated data mapping, we dont remain here, but progress to progressed out branches
////			return tryActivationPropagation();
////		case PROGRESSED_OUTBRANCHES: // we have also mapped some data, 
////			return tryActivationPropagation();
////		default:
////			log.warn(String.format("Decision node %s in unsupported state %s to process inflow activation event for branch %s", this.getId(), sm.getState(), branchId));
////		
////		}
////		
////		return tryInConditionsFullfilled();
////	}
//	
//	// used for initial instntiation of steps
////	public Map<IBranchInstance, WorkflowTask> calculatePossibleActivationPropagationUponWorkflowInstantiation() {
////		// checks whether output tasks can be activated
////		// can only activate tasks but not provide dataflow
////		if (inBranches.stream()
////			.allMatch(b -> b.getState() == BranchState.TransitionEnabled)
////			&& this.contextConditionsFullfilled) { // only if input and context conditions fulfilled
////			return outBranches.stream() // return for each enabled outbranch the respective workflow task passiveprocessengine.instance (not part of process yet)
////				.filter(b -> b.getState() == BranchState.TransitionEnabled && !b.hasTask() ) // && !b.getBranchDefinition().hasDataFlow()
////				.collect(Collectors.toMap(b -> b, b -> workflow.prepareTask(b.getBranchDefinition().getTask())));
////		}
////		return Collections.emptyMap();	
////	}
//	
////	public Set<AbstractWorkflowInstanceObject> tryActivationPropagation() {
////		// checks whether output tasks can be activated
////		if ((sm.isInState(States.PASSED_OUTBRANCH_CONDITIONS) || sm.isInState(States.PROGRESSED_OUTBRANCHES) ))// &&  // only if input, context, and activation conditions fulfilled
////			// THIS ACTIVATES ALL TASKS ON ENABLED BRANCHES, EVEN FOR XOR: the user can then choose what to execute, thus deactivation needs to happen upon activation/work output by user
////			// WHILE NO USER WORKS/ACTIVATES, remaining branches might still be activated
////			//outBranches.stream()
////			//		.anyMatch(b -> b.getState() == BranchState.TransitionEnabled && !b.hasTask())
////					//.allMatch(b -> !b.getBranchDefinition().hasDataFlow())
////		    //) 
////		{		
////			Set<AbstractWorkflowInstanceObject> awfos =
////				outBranches.stream() // return for each enabled outbranch the respective workflow task passiveprocessengine.instance (not part of process yet)
////				.filter(b -> ( b.getState() == BranchState.TransitionEnabled || // either we are enabled, or we are waiting for a never active activation conditions
////						(b.getState()==BranchState.Waiting && !b.getBranchDefinition().hasActivationCondition()) )
////							&& !b.hasTask() ) // WE NO LONGER CHECK IF HAS DATAFLOW &&  !b.getBranchDefinition().hasDataFlow())
////				.flatMap(b -> { List<AbstractWorkflowInstanceObject> awos = new ArrayList<>();
////								WorkflowTask wft = workflow.instantiateTask(b.getBranchDefinition().getTask());
////								if (wft == null) return Stream.empty();
////								awos.add(wft);
////								awos.addAll(workflow.activateDecisionNodesFromTask(wft));
////								consumeTaskForUnconnectedInBranch(wft);
////								b.setBranchUsedForProgress(); // because we activate now, regardless whether datamapping successful
////								return awos.stream();
////					})
////				.collect(Collectors.toSet());
////			if (!awfos.isEmpty()) {
////				executeSimpleDataflow(false);
////				if (!areAnyBranchesExceptDisabledOrTransitionCompletedLeft()) {
////					this.activationPropagationCompleted = true; //alternatively we have triggered progress, but some outbranches are still waiting and could fire later
////				}
////				fireIfPossible(Events.PROGRESS_TRIGGERED);
////				return awfos;
////			} // special case if this is the last/ending DNI
////			else if (this.getWorkflow().getType().getTasksFlowingOutOf(this.getDefinition()).size() == 0 ) {
////				// then we are done with this workflow and execute any final mappings into the workflows output
////				executeEndOfProcessDataflow();
////				fireIfPossible(Events.PROGRESS_TRIGGERED);
////				return Collections.emptySet();
////			}
////		}
////		return Collections.emptySet();	
////	}
//	
//	public List<ProcessChangeEvent> tryActivationPropagation(ProcessChangeEvent cause) {
//		// if not yet progressed,
//		List<ProcessChangeEvent> newAWOs = new ArrayList<>();
//		ProcessChangeEvent wce = null;
//		if (sm.isInState(States.PASSED_CONTEXT_CONDITIONS)) {
//			// get all out task defs, check if they exist or create them
//			// first time activation
//			ProcessChangeEvent wceInner = new DNIUpdateEvent(ChangeType.OUTFLOW_TRIGGERED, this, this.getWorkflow(), cause);
//			newAWOs.add(wceInner);	
//			wceInner.getCascadingEffect().addAll(this.getWorkflow().getType().getWorkflowTaskDefinitions().stream()
//				.filter(td -> td.getInDND().equals(this.getDefinition())) //retain only that have this DND as their predecessor
//				.filter(td -> getWorkflow().getWorkflowTasksReadonly().stream().map(WorkflowTask::getType).noneMatch(td1 -> td1.equals(td))) // retain those that are not yet instantiated
//				.flatMap(td -> getWorkflow().createAndWireTask(td, wceInner).stream()) //now these are changes already, thus we can drop the following commented statement
//				//.map(td -> new WorkflowChangeEvent(CREATED, td))
//				.collect(Collectors.toList()));
//			wce = wceInner;
//			fireIfPossible(Events.OUTBRANCHES_FULFILLED);
//		} //else {
////			newAWOs = 
////		}
//		if (sm.isInState(States.PASSED_OUTBRANCH_CONDITIONS)) {
//			if (wce != null) {
//				wce.getCascadingEffect().addAll(checkAndExecuteDataMappings(cause));
//				wce.getCascadingEffect().addAll(checkIfProcessIsCompleteNow(cause));
//			} else {
//				newAWOs.addAll(checkAndExecuteDataMappings(cause));
//				newAWOs.addAll(checkIfProcessIsCompleteNow(cause));
//			}
//		}
//		return newAWOs;
//	}
//
//	private List<ProcessChangeEvent> checkIfProcessIsCompleteNow(ProcessChangeEvent cause) {
//		// check if this is the last DNI, and thus notify process that it is done
//		if (this.getWorkflow().getType().getTasksFlowingOutOf(this.getDefinition()).size() == 0) {
//			return this.getWorkflow().postConditionsFulfilled(cause);
//		} else
//			return Collections.emptyList();
//	}
//	
//	private List<ProcessChangeEvent> checkAndExecuteDataMappings(ProcessChangeEvent cause) {
//		// also check if data mapping to execute
//		Set<ExecutedMapping> preparedMappings;
//		if (this.getWorkflow().getType().getTasksFlowingOutOf(this.getDefinition()).size() == 0 ) {
//			// then we are done with this workflow and execute any final mappings into the workflows output
//			preparedMappings = prepareEndOfProcessDataflow();
//		} else {
//			preparedMappings = prepareSimpleDataflow(false);
//		}
//		return preparedMappings.stream()
//				.flatMap(mapping -> execute(mapping, cause).stream())
//				.filter(Objects::nonNull)
//				.collect(Collectors.toList());
//	}
//
//	private List<ProcessChangeEvent> checkAndExecuteDataMappings(List<MappingDefinition> definitions, ProcessChangeEvent cause) { // TODO fix code duplication with checkAndExecuteDataMappings()
//		// also check if data mapping to execute
//		Set<ExecutedMapping> preparedMappings;
//		if (this.getWorkflow().getType().getTasksFlowingOutOf(this.getDefinition()).size() == 0 ) {
//			// then we are done with this workflow and execute any final mappings into the workflows output
//			preparedMappings = prepareEndOfProcessDataflow(definitions);
//		} else {
//			preparedMappings = prepareSimpleDataflow(false, definitions);
//		}
//		return preparedMappings.stream()
//				.flatMap(mapping -> execute(mapping, cause).stream())
//				.filter(Objects::nonNull)
//				.collect(Collectors.toList());
//	}
//	
////	private boolean areAnyBranchesExceptDisabledOrTransitionCompletedLeft() {
////		return outBranches.stream() 
////		.filter(b -> b.getState() != BranchState.Disabled)
////		.filter(b -> b.getState() != BranchState.TransitionPassed)
////		.count() > 0;
////	}
//	
////	public void completedDataflowInvolvingActivationPropagation() {
////		outBranches.stream()
////			.forEach(IBranchInstance::setBranchUsedForProgress); // TODO do we need to filter for deactivated ones?
////		// not necessary for outbranches as we set them via task assignment --> no we dont
////		activationPropagationCompleted = true;
////		fireIfPossible(Events.PROGRESS_TRIGGERED);
//
////		inBranches.values().stream()
////			.filter(b -> b.getState() != BranchState.Disabled)
////			.forEach(b -> b.setBranchUsedForProgress());
//		// not necessary as inbranches progress set when checking inbranch conditions
////	}
//	
////	public boolean acceptsTaskForUnconnectedInBranch(WorkflowTask wti) {
////		// checks if any inBranch yet has not an associated Task,
////		// this check is specific to the DecisionNodeDefinition or the DecisionNodeInstance,
////		// for now we only assume one tasktype per branch, and fixed branch number
////		Optional<AbstractBranchInstance> branch = inBranches.values().stream()
////			.filter(b -> b.hasTask())
////			.filter(b -> b.bd.getTask().equals(wti.getTaskType()))
////			.findFirst();
////		return branch.isPresent();
////	}
//	
////	public DecisionNodeInstance consumeTaskForUnconnectedInBranch(WorkflowTask wti) {
////		// checks if any inBranch yet has not an associated Task,
////		// this check is specific to the DecisionNodeDefinition or the DecisionNodeInstance,
////		// for now we only assume one tasktype per branch, and fixed branch number
////		Optional<IBranchInstance> branch = inBranches.stream()
////			.filter(b -> !b.hasTask())
////			.filter(b -> b.getBranchDefinition().getTask().equals(wti.getType()))
////			.findFirst();
////		branch.ifPresent(b -> { b.setTask(wti); 
////								this.getWorkflow().registerTaskAsInToDNI(this, wti); 
////							  });
////		// REQUIRES CHANGE LISTENER TO LET THE RULE ENGINE KNOW, WE UPDATEd THE BRANCH
////		if (branch.isPresent())
////			return this;
////		else
////			return null;
////	}
////	
////	public DecisionNodeInstance consumeTaskForUnconnectedOutBranch(WorkflowTask wti) {
////		Optional<IBranchInstance> branch = outBranches.stream()
////				.filter(b -> b.getState() != BranchState.Disabled)
////				.filter(b -> !b.hasTask())
////				.filter(b -> b.getBranchDefinition().getTask().getId().equals(wti.getType().getId()))
////				.findFirst();
////			branch.ifPresent(b -> { 
////					b.setTask(wti);
////					//b.setBranchUsedForProgress(); 
////					this.getWorkflow().registerTaskAsOutOfDNI(this, wti);
////				});
////			// REQUIRES CHANGE LISTENER TO LET THE RULE ENGINE KNOW, WE UPDATEd THE BRANCH
////			if (branch.isPresent())
////				return this;
////			else
////				return null;
////	}
//	
////	public void defineInBranch(String branchName, WorkflowTask wft) {
////		inBranches.put(branchName, new Branch(branchName, wft));
////	}
////	
////	public void defineOutBranch(String branchName, WorkflowTask wft) {
////		outBranches.put(branchName, new Branch(branchName, wft));
////	}
//	
////	public List<TaskDefinition> getTaskDefinitionsForNonDisabledOutBranchesWithUnresolvedTasks() {
////		return outBranches.stream()
////				.filter(b -> b.getState()!=BranchState.Disabled)				
////				.filter(b -> b.getTask() == null)
////				.map(b -> b.getBranchDefinition().getTask())
////				.filter(td -> td != null)
////				.collect(Collectors.toList());
////	}
////
////	public List<TaskDefinition> getTaskDefinitionsForFulfilledOutBranchesWithUnresolvedTasks() {
////		List<TaskDefinition> tds = outBranches.stream()
////				.filter(b -> b.getState()==BranchState.TransitionPassed)
////				.filter(b -> b.getTask() == null)
////				.map(b -> b.getBranchDefinition().getTask())
////				.filter(td -> td != null)
////				.collect(Collectors.toList());
////		return tds;
////	}
////	
////	public List<WorkflowTask> getNonDisabledTasksByInBranchName(String branchName) {
////		return inBranches.stream()
////				.filter(b -> b.getState()!=BranchState.Disabled)
////				.filter(b -> b.getBranchDefinition().getName().equals(branchName))
////				.filter(b -> b.hasTask())
////				.map(b -> b.getTask())
////				.collect(Collectors.toList());
////	}
////	
////	public List<WorkflowTask> getNonDisabledTasksByOutBranchName(String branchName) {
////		return outBranches.stream()
////				.filter(b -> b.getState()!=BranchState.Disabled)
////				.filter(b -> b.getBranchDefinition().getName().equals(branchName))
////				.filter(b -> b.hasTask())
////				.map(b -> b.getTask())
////				.collect(Collectors.toList());
////	}
////	
////	public String getInBranchIdForWorkflowTask(IWorkflowTask task) {
////		Optional<IBranchInstance> branch = inBranches.stream()
////				.filter(b -> b.getTask() != null)
////				.filter(b -> b.getTask().equals(task))
////				.findFirst();
////		return branch.isPresent() ? branch.get().getBranchDefinition().getName() : null;
////	}
////	
////	public String getOutBranchIdForWorkflowTask(IWorkflowTask task) {
////		Optional<IBranchInstance> branch = outBranches.stream()
////			.filter(b -> b.getTask().equals(task))
////			.findFirst();
////		return branch.isPresent() ? branch.get().getBranchDefinition().getName() : null;
////	}
////
////	public IBranchInstance getInBranchForWorkflowTask(IWorkflowTask task) {
////		Optional<IBranchInstance> branch = inBranches.stream()
////				.filter(b -> b.getTask().equals(task))
////				.findFirst();
////		return branch.orElse(null);
////	}
////
////	public IBranchInstance getOutBranchForWorkflowTask(IWorkflowTask task) {
////		Optional<IBranchInstance> branch = outBranches.stream()
////				.filter(b -> b.getTask().equals(task))
////				.findFirst();
////		return branch.orElse(null);
////	}
//	
//	protected boolean isWorkExpected(IWorkflowTask task) {
//		if (this.ofType.getInBranchingType().equals(InFlowType.XOR)) {
//			// if there is any other task that is used or enabled, then we expect no work, else we would expect some work
//			boolean otherTaskUsed = this.inTaskStatus.entrySet().stream()
//				.filter(entry -> !entry.getKey().equals(task.getId())) // disregard this one
//				.anyMatch(entry -> entry.getValue().equals(Progress.USED) || entry.getValue().equals(Progress.ENABLED)); // if we find any of these, 
//				//then no work expected
//			return !otherTaskUsed;
//		} else { // only for XOR inflows would we expect some task not to be done
//			return true;
//		}
//	}
//
//	private Set<ExecutedMapping> prepareEndOfProcessDataflow() {
//		return prepareSimpleDataflow(true);
//	}
//
//	private Set<ExecutedMapping> prepareEndOfProcessDataflow(List<MappingDefinition> definitions) {
//		return prepareSimpleDataflow(true, definitions);
//	}
//	
//	private Set<ExecutedMapping> prepareSimpleDataflow(boolean isEndOfProcess) {
//		return prepareSimpleDataflow(isEndOfProcess, getDefinition().getMappings());
//	}
//
//	private Set<ExecutedMapping> prepareSimpleDataflow(boolean isEndOfProcess, List<MappingDefinition> definitions) {
//		// this will take from each mapping only the first from and to taskids
//		return definitions.stream()
//				.flatMap(m ->
//						mapSingle(m.getFrom().get(0).getFirst(), m.getFrom().get(0).getSecond(),
//								m.getTo().get(0).getFirst(), m.getTo().get(0).getSecond(), isEndOfProcess).stream()
//				)
//				.filter(Objects::nonNull)
//				.collect(Collectors.toSet());
//	}
//	
//	private Set<ExecutedMapping> mapSingle(String taskFrom, String roleFrom, String taskTo, String roleTo, boolean isEndOfProcess) {
//		ExecutedMapping templateEM = new ExecutedMapping();
//		templateEM.setFromRole(roleFrom);
//		templateEM.setToRole(roleTo);
//		if (resolveSourceCompletedTaskOrWorkflow(taskFrom, templateEM, isEndOfProcess) && 
//				resolveDestinationTaskOrWorkflow(templateEM, taskTo)) { // now we have direction and fromTask set and if so then also toTask
//			Set<ExecutedMapping> withFromArt = getArtifactsFromCompletedTaskOrWorkflow(templateEM);
//			return withFromArt.stream().filter(em -> shouldBeMapped(em)).collect(Collectors.toSet());
//		}
//		return Collections.emptySet();
//	}
//	
////	private AbstractWorkflowInstanceObject mapAsOutput(String asRole, Set<IArtifact> fromArt, IWorkflowTask toTask) {
////		// check if not yet exists:
////		if (toTask.getAnyOneOutputByRole(asRole) == null) {
////			toTask.addOutput(new ArtifactOutput(fromArt, asRole));
////			return (AbstractWorkflowInstanceObject) toTask;
////		}  else {
////			Set<ArtifactIdentifier> existingArt = toTask.getAllOutputsByRole(asRole).stream().map(art -> art.getArtifactIdentifier()).collect(Collectors.toSet());
////			Set<IArtifact> newArt = fromArt.stream().filter(art -> !existingArt.contains(art.getArtifactIdentifier()) ).collect(Collectors.toSet());
////			if (newArt.isEmpty())
////				return null;
////			else {
////				toTask.getOutput().stream()
////			                .filter(o -> o.getRole().equals(asRole))
////			                .findAny().ifPresent(output ->  
////			                	newArt.stream().forEach(art -> output.addOrReplaceArtifact(art) ) );
////			    return (AbstractWorkflowInstanceObject) toTask;
////			}	
////		}
////	}
//	
////	private AbstractWorkflowInstanceObject mapAsInput(String asRole, Set<IArtifact> fromArt, IWorkflowTask toTask) {
////		// check if not yet exists:
////		if (toTask.getAnyOneInputByRole(asRole)==null) {
////			// then map as new Input
////			toTask.addInput(new ArtifactInput(fromArt, asRole)); 
////			return (AbstractWorkflowInstanceObject) toTask;
////		} else {
////			Set<ArtifactIdentifier> existingArt = toTask.getAllInputsByRole(asRole).stream().map(art -> art.getArtifactIdentifier()).collect(Collectors.toSet());
////			Set<IArtifact> newArt = fromArt.stream().filter(art -> !existingArt.contains(art.getArtifactIdentifier()) ).collect(Collectors.toSet());
////			if (newArt.isEmpty())
////				return null;
////			else {
////				toTask.getInput().stream()
////			                .filter(o -> o.getRole().equals(asRole))
////			                .findAny().ifPresent(output ->  
////			                	newArt.stream().forEach(art -> output.addOrReplaceArtifact(art) ) );
////				return (AbstractWorkflowInstanceObject) toTask;
////			}
////		}
////	}
//	
//	private boolean shouldBeMapped(ExecutedMapping em) {
//		// check if not yet exists:
//		switch(em.getDirection()) {
//		case inToIn: //fallthrough
//		case outToIn: // check the input of the task 
//			if (em.getToTask().getAnyOneInputByRole(em.getToRole())==null) {
//				// then mark this a mappable as new Input
//				return true;
//			} else {
//				Set<ArtifactIdentifier> existingArt = em.getToTask().getAllInputsByRole(em.getToRole()).stream().map(art -> art.getArtifactIdentifier()).collect(Collectors.toSet());
//				if (existingArt.contains(em.getArtifact().getArtifactIdentifier()))
//					return false;
//				else {
//					return true;
//				}
//			}
//		case inToOut: // fallthrough
//		case outToOut: //to output of task or process
//			if (em.getToTask().getAnyOneOutputByRole(em.getToRole())==null) {
//				// then mark this a mappable as new Input
//				return true;
//			} else {
//				Set<ArtifactIdentifier> existingArt = em.getToTask().getAllOutputsByRole(em.getToRole()).stream().map(art -> art.getArtifactIdentifier()).collect(Collectors.toSet());
//				if (existingArt.contains(em.getArtifact().getArtifactIdentifier()))
//					return false;
//				else {
//					return true;
//				}
//			}
//		default:
//			return false;
//		}
//	}
//	
//
//	
//	private List<ProcessChangeEvent> execute(ExecutedMapping mapping, ProcessChangeEvent cause) {
//		mappings.add(mapping);
//		List<ProcessChangeEvent> events = new LinkedList<>();
//		switch (mapping.getDirection()) {		
//			case inToIn : //fallthrough
//			case outToIn :
//				mapping.getToTask().getInput().stream()
//                .filter(o -> o.getRole().equals(mapping.getToRole()))
//                .findAny()
//                .ifPresentOrElse(input -> events.addAll(input.addOrReplaceArtifact(mapping.getArtifact(), cause)),
//                		() -> events.addAll(mapping.getToTask().addInput(new ArtifactInput(mapping.getArtifact(), mapping.getToRole()), cause)));
//				return events; //new TaskArtifactEvent(NEW_INPUT, mapping.getToTask(), Set.of(mapping.getArtifact().getArtifactIdentifier()));
//			case inToOut :// fallthrough
//			case outToOut :
//				mapping.getToTask().getOutput().stream()
//                .filter(o -> o.getRole().equals(mapping.toRole))
//                .findAny()
//                .ifPresentOrElse(output -> events.addAll(output.addOrReplaceArtifact(mapping.getArtifact(), cause)),
//                		() -> events.addAll(mapping.getToTask().addOutput(new ArtifactOutput(mapping.getArtifact(), mapping.getToRole()), cause)));
//				return events; //new TaskArtifactEvent(NEW_OUTPUT, this.getWorkflow(),Set.of(mapping.getArtifact().getArtifactIdentifier()));
//		}
//		return events;
//	}
//
//	
//	private boolean resolveSourceCompletedTaskOrWorkflow(String taskId, ExecutedMapping templateEM, boolean isEndOfProcess) {
//		return getWorkflow().getWorkflowTasksReadonly().stream()	
//		.filter(wft -> wft.getType().getId().equals(taskId) )
//		.filter(wft -> wft.getExpectedLifecycleState().equals(State.COMPLETED)) // we only map data for tasks that are indeed completed
//		.filter(Objects::nonNull)
//		.map(IWorkflowTask.class::cast)
//		.findFirst().map( wft -> { if (isEndOfProcess) 
//										templateEM.setDirection(DIR.outToOut);
//									else
//										templateEM.setDirection(DIR.outToIn);
//								templateEM.setFromTask(wft);
//								return true;
//							} ).orElseGet( () -> {
//			if (getWorkflow().getType().getId().equals(taskId)) {
//				if (isEndOfProcess) 
//					templateEM.setDirection(DIR.inToOut);
//				else
//					templateEM.setDirection(DIR.inToIn);
//				templateEM.setFromTask(getWorkflow());
//				return true;
//			}
//			else { 
//				return false;
//			}
//		});
//	}
//	
//	private Set<ExecutedMapping> getArtifactsFromCompletedTaskOrWorkflow(ExecutedMapping templateEM) {
//		switch(templateEM.getDirection()) {
//		case inToIn: //fallthrough
//		case inToOut: // for input from process
//			return templateEM.getFromTask().getAllInputsByRole(templateEM.getFromRole())
//					.stream()
//					.map(art -> ExecutedMapping.copyFrom(templateEM).fluentSetArtifact(art))
//					.collect(Collectors.toSet());
//		case outToIn: //fallthrough
//		case outToOut: //for output from task
//			// we checked for completed task earlier when adding it to the template
//			return templateEM.getFromTask().getAllOutputsByRole(templateEM.getFromRole())
//														.stream()
//														.map(art -> ExecutedMapping.copyFrom(templateEM).fluentSetArtifact(art))
//														.collect(Collectors.toSet());
//		default:
//			return Collections.emptySet();
//		}
//	}
//	
//	private boolean resolveDestinationTaskOrWorkflow(ExecutedMapping templateEM, String taskId) {
//		switch(templateEM.getDirection()) {
//		case outToIn: //fallthrough
//		case inToIn: // for input from process to input of task
//			return getWorkflow().getWorkflowTasksReadonly().stream()	
//			.filter(wft -> wft.getType().getId().equals(taskId) )
//			.map(wft -> (IWorkflowTask)wft)
//			.findAny()
//			.map( wft -> { templateEM.setToTask(wft);
//							return true;})
//			.orElse(false);
//		case inToOut: //fallthrough
//		case outToOut: //for output from task
//			if (getWorkflow().getType().getId().equals(taskId)) {
//				templateEM.setToTask(getWorkflow());
//				return true;
//			} else 
//		  		return false;
//		default:
//			return false;
//		}
//	}
//	
//	private boolean fireIfPossible(Events e) {
//		if (sm.canFire(e)) {
//			sm.fire(e);
//			return true;
//		} else {
//			log.debug(String.format("Unable to fire event %s in state %s", e, sm.getState()));
//			return false;
//		}
//	}
//
//	@Override
//	public String toString() {
//		return "DNI ("+getState()+") + [" + ofType + ","+  workflow + ", "
//				 + ", contextConditionsFullfilled=" + contextConditionsFullfilled 
//				 + ", activationPropagationCompleted=" + activationPropagationCompleted				
//				+ "]";
//	}
//
//	@Override
//	public boolean equals(Object o) {
//		if (this == o) return true;
//		if (!(o instanceof DecisionNodeInstance)) return false;
//		if (!super.equals(o)) return false;
//		DecisionNodeInstance that = (DecisionNodeInstance) o;
//		return 
//				contextConditionsFullfilled == that.contextConditionsFullfilled &&
//				activationPropagationCompleted == that.activationPropagationCompleted;
//	}
//	
//
//	public static List<DecisionNodeInstance> findCorrespondingOutFlowPath(DecisionNodeInstance dni, String priorityIfPresent) { // finds for this decision node inFlow the corresponding outFlow of a prior DNI, if exists 		
//		if (dni == null)
//			return null;		
//		else {
//			List<DecisionNodeInstance> path = new LinkedList<>();
//			// get any one (existing) predecessor DNIs, 
//			DecisionNodeInstance predDNI = null;
//			do {
//				Optional<DecisionNodeInstance> optPred = dni.getWorkflow().getTasksFlowingInto(dni).stream()
//						.filter(task -> {
//							return (priorityIfPresent != null) ? task.getId().equals(priorityIfPresent) : true;
//						})
//						.map(task -> task.getInDNI()).findAny();
//				if (optPred.isEmpty()) return null; // there is no predecessor, can happen only for start node
//				// now check if this is the outflow
//				predDNI = optPred.get();
//				if (predDNI.ofType.countOutgoingTasks() > 1) { 
//					path.add(predDNI);
//					return path;
//				}else if (predDNI.ofType.countIncomingTasks() > 1) {// skip substructure
//					List<DecisionNodeInstance> subPath = findCorrespondingOutFlowPath(predDNI, null);
//					if (subPath == null) // should not happen, but just in case 
//						return null;
//					else {
//						path.addAll(subPath);
//						dni = subPath.get(subPath.size()-1);
//					}
//				} else {// this is a sequential connecting DNI with a single in and out branch/flow which therefore has no corresponding outFlow DNI, thus needs to be skipped	
//					path.add(predDNI);
//					dni = predDNI; // we skip this
//				}
//			} while (true);																		 			
//		}		
//	}
//	
//	
//}
