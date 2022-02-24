package at.jku.isse.passiveprocessengine.instance;

import at.jku.isse.designspace.core.model.Cardinality;
import at.jku.isse.designspace.core.model.Instance;
import at.jku.isse.designspace.core.model.InstanceType;
import at.jku.isse.designspace.core.model.Workspace;
import at.jku.isse.passiveprocessengine.ProcessScopedElement;
import at.jku.isse.passiveprocessengine.definition.DecisionNodeDefinition;
import java.util.*;

public class DecisionNodeInstance extends ProcessScopedElement {

	public static enum CoreProperties {isInflowFulfilled, hasPropagated};
	public static final String designspaceTypeId = ProcessInstance.class.getSimpleName();
	
	public DecisionNodeInstance(Instance instance) {
		super(instance);
	}
	
	
	private DecisionNodeDefinition ofType;

	private HashMap<String, Progress> inTaskStatus = new HashMap<>();
	private enum Progress {
		WAITING, ENABLED, DISABLED, USED
	}

	private List<ExecutedMapping> mappings = new LinkedList<>();

	public List<ExecutedMapping> getExecutedMappings() {
		return mappings;
	}
	
	
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
//
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
//
//	public List<ProcessChangeEvent> tryContextConditionsFullfilled(ProcessChangeEvent cause) {
//		if (isContextConditionsFullfilled() || !ofType.hasExternalContextRules) {// if context conditions already evaluated to true, OR DND claims there are no external rules
//			return setContextConditionsFullfilledInternal(cause); 
//		}
//		return Collections.emptyList();
//	}
//	
//
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
//
//		return tryActivationPropagation(cause);
//		
//	}
//	
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
//
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
	
	
	public static InstanceType getOrCreateDesignSpaceCoreSchema(Workspace ws) {
		Optional<InstanceType> thisType = ws.debugInstanceTypes().stream()
			.filter(it -> it.name().equals(DecisionNodeInstance.designspaceTypeId))
			.findAny();
		if (thisType.isPresent())
			return thisType.get();
		else {
			InstanceType typeStep = ws.createInstanceType(ProcessStep.designspaceTypeId, ws.TYPES_FOLDER, ProcessScopedElement.getOrCreateDesignSpaceCoreSchema(ws));
			typeStep.createPropertyType(CoreProperties.isInflowFulfilled.toString(), Cardinality.SINGLE, Workspace.BOOLEAN);
			typeStep.createPropertyType(CoreProperties.hasPropagated.toString(), Cardinality.SINGLE, Workspace.BOOLEAN);
			return typeStep;
		}
	}
	
}
