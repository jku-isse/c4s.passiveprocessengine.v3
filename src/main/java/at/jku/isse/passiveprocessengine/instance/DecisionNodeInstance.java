package at.jku.isse.passiveprocessengine.instance;

import at.jku.isse.designspace.core.model.Cardinality;
import at.jku.isse.designspace.core.model.Id;
import at.jku.isse.designspace.core.model.Instance;
import at.jku.isse.designspace.core.model.InstanceType;
import at.jku.isse.designspace.core.model.Workspace;
import at.jku.isse.passiveprocessengine.ProcessInstanceScopedElement;
import at.jku.isse.passiveprocessengine.WrapperCache;
import at.jku.isse.passiveprocessengine.definition.DecisionNodeDefinition;
import at.jku.isse.passiveprocessengine.definition.MappingDefinition;
import at.jku.isse.passiveprocessengine.instance.ExecutedMapping.DIR;
import at.jku.isse.passiveprocessengine.instance.StepLifecycle.State;

import java.util.*;
import java.util.stream.Collectors;

public class DecisionNodeInstance extends ProcessInstanceScopedElement {

	public static enum CoreProperties {isInflowFulfilled, hasPropagated, dnd, inSteps, outSteps};
	public static final String designspaceTypeId = DecisionNodeInstance.class.getSimpleName();
	
	public DecisionNodeInstance(Instance instance) {
		super(instance);
	}

	// FIXME: check that transient fields are properly initialized upon loading
	private transient HashMap<ProcessStep, Progress> inTaskStatus = new HashMap<>();
	private enum Progress {
		WAITING, ENABLED, DISABLED, USED
	}

	private transient List<ExecutedMapping> mappings = new LinkedList<>(); //TODO: remove this and directly check
	// Also ensure that with every input data add/remove, all valid/eligible instep.outputs are mapped/propagated,

	public List<ExecutedMapping> getExecutedMappings() {
		return mappings;
	}
	
	protected DecisionNodeDefinition getDefinition() {
		return  WrapperCache.getWrappedInstance(DecisionNodeDefinition.class, instance.getPropertyAsInstance(CoreProperties.dnd.toString()));
	}
	
	private void setInflowFulfilled(boolean isFulfilled) {
		instance.getPropertyAsSingle(CoreProperties.isInflowFulfilled.toString()).set(isFulfilled);
	}
	
	private boolean isInflowFulfilled() {
		return (boolean) instance.getPropertyAsValueOrElse(CoreProperties.isInflowFulfilled.toString(), () -> false);
	}
	
	private void setHasPropagated() {
		instance.getPropertyAsSingle(CoreProperties.hasPropagated.toString()).set(true);
	}
	
	private boolean hasPropagated() {
		return (boolean) instance.getPropertyAsValueOrElse(CoreProperties.hasPropagated.toString(), () -> false);
	}
	
	@SuppressWarnings("unchecked")
	protected void addOutStep(ProcessStep step) {
		instance.getPropertyAsSet(CoreProperties.outSteps.toString()).add(step.getInstance());
	}
	
	@SuppressWarnings("unchecked")
	protected Set<ProcessStep> getOutSteps() {
		return (Set<ProcessStep>) instance.getPropertyAsSet(CoreProperties.outSteps.toString()).stream()
			.map(inst -> WrapperCache.getWrappedInstance(ProcessStep.class, (Instance)inst))
			.collect(Collectors.toSet());
	}
	
	@SuppressWarnings("unchecked")
	protected void addInStep(ProcessStep step) {
		instance.getPropertyAsSet(CoreProperties.inSteps.toString()).add(step.getInstance());
	}
	
	@SuppressWarnings("unchecked")
	protected Set<ProcessStep> getInSteps() {
		return (Set<ProcessStep>) instance.getPropertyAsSet(CoreProperties.inSteps.toString()).stream()
			.map(inst -> WrapperCache.getWrappedInstance(ProcessStep.class, (Instance)inst))
			.collect(Collectors.toSet());
	}
	
	private Progress mapInitialStatus(ProcessStep task) {
		if (task.getExpectedLifecycleState().equals(State.COMPLETED))
			return Progress.ENABLED;
		else if (task.getExpectedLifecycleState().equals(State.NO_WORK_EXPECTED) || task.getExpectedLifecycleState().equals(State.CANCELED))
			return Progress.DISABLED;
		else 
			return Progress.WAITING;
	}
	
	private Progress mapExistingStatus(ProcessStep task, Progress currentState) {
		switch(currentState) {
			case WAITING:
				if(task.getExpectedLifecycleState().equals(State.COMPLETED))
					return Progress.ENABLED;
				else if (task.getExpectedLifecycleState().equals(State.NO_WORK_EXPECTED) || task.getExpectedLifecycleState().equals(State.CANCELED))
					return Progress.DISABLED;
				else
					return Progress.WAITING;
			case DISABLED:
				//canceling a step, will disable it here, but we have no signal when it becomes available again, thus it will permanently remain in DISABLED for the moment
				if(task.getExpectedLifecycleState().equals(State.COMPLETED)) { 
					// we dont override here, how to handle switching between exclusive tasks! --> one needs to be deactivated/canceled or reverted
					// distinguish between having progressed and not having progressed -->
					// IF we have no other ready yet, then transition into ENABLED, 
					boolean isAnotherEnabled = inTaskStatus.entrySet().stream()
						.filter(entry -> !entry.getKey().equals(task.getId()))
						.anyMatch(entry -> entry.getValue().equals(Progress.ENABLED));
					if (!isAnotherEnabled)
						return Progress.ENABLED;										
				}				
				return Progress.DISABLED;
			case ENABLED:
				if(task.getExpectedLifecycleState().equals(State.COMPLETED))
					return Progress.ENABLED;
				else if (task.getExpectedLifecycleState().equals(State.NO_WORK_EXPECTED) || task.getExpectedLifecycleState().equals(State.CANCELED))
					return Progress.DISABLED;
				else // if no longer complete, returned to some other state // TODO: what if there is a deviation?
					return Progress.WAITING;
			case USED:
				if(task.getExpectedLifecycleState().equals(State.NO_WORK_EXPECTED) || task.getExpectedLifecycleState().equals(State.CANCELED)) {
					// TODO if used but now disabled, then lets check if some data remapping needs to be done
				}
				return Progress.USED;
		}
		return currentState;
	}
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
	
	public void tryInConditionsFullfilled() {
		// make sure we have latest status in our mapping
		//List<WorkflowChangeEvent> changes = new ArrayList<>();
		//WorkflowChangeEvent wce = null;
		this.getInSteps().stream()
			.peek(task -> inTaskStatus.computeIfPresent(task, (key, value) -> mapExistingStatus(task, value)))
			.forEach(task -> inTaskStatus.computeIfAbsent(task, k -> mapInitialStatus(task))  );
		
		
		switch(this.getDefinition().getInFlowType()) {
		case AND: 
			if (inTaskStatus.values().stream()
					.filter(p -> !(p.equals(Progress.DISABLED))) // we ignore those
					.allMatch(p -> p.equals(Progress.ENABLED)) ) { // if non are left, we still progress trusting that any canceled or no work expected tasks are not required anymore
				inTaskStatus.entrySet().stream()
					.filter(entry -> !entry.getValue().equals(Progress.DISABLED) ) // we ignore those
					.peek(entry -> entry.setValue(Progress.USED))
					.collect(Collectors.toSet())
					.forEach(entry2 -> inTaskStatus.put(entry2.getKey(), entry2.getValue()));
				setInflowFulfilled(true);
				tryActivationPropagation();
			} else
				setInflowFulfilled(false);
			break;
		case OR:
			if (inTaskStatus.values().stream()
					.filter(p -> !(p.equals(Progress.DISABLED))) // we ignore those
					.anyMatch(p -> p.equals(Progress.ENABLED)) ) {
				inTaskStatus.entrySet().stream()
					.filter(entry -> entry.getValue().equals(Progress.ENABLED) ) // we use all ready ones
					.map(entry -> { entry.setValue(Progress.USED); 
									return entry; })
					.collect(Collectors.toSet())
					.forEach(entry2 -> inTaskStatus.put(entry2.getKey(),  entry2.getValue()));
//				// this will be called several times, if branches become gradually enabled, for each new branch enabled, we need to check if data transfer has to happen
				setInflowFulfilled(true);
				tryActivationPropagation();
			} else
				setInflowFulfilled(false);
			break;
		case XOR:
			Optional<ProcessStep> optId = inTaskStatus.entrySet().stream()
					.filter(entry -> entry.getValue().equals(Progress.ENABLED))
					.findAny()
					.map(entry -> entry.getKey());
			if (optId.isPresent()) {
				ProcessStep step = optId.get();
				inTaskStatus.put(step, Progress.USED);
				inTaskStatus.entrySet().stream()
				.filter(entry -> entry.getValue().equals(Progress.ENABLED) || entry.getValue().equals(Progress.WAITING) ) 
				.map(entry -> { entry.getKey().setWorkExpected(false); //this only works for XORs that have no sub branches, 
								entry.setValue(Progress.DISABLED); 
								return entry; })
				.collect(Collectors.toSet())
				.forEach(entry2 -> inTaskStatus.put(entry2.getKey(),  entry2.getValue()));
				// TOOD: find all tasks that have not reached until here, (which is when one xor branch is substructured and has not progressed as far yet to pop up as branch here yet)	
				
				if (this.getDefinition().getInSteps().size() > 1) {// there are other inbranches that we now need to deactivate
					List<DecisionNodeInstance> path = findCorrespondingOutFlowPath(this, step); //the path to the corresponding outflow node via the just activating task
					if (path != null) {
						path.add(0, this);
						// deactivate those not on path
						DecisionNodeInstance correspondingDNI = path.get(path.size()-1);
						DecisionNodeInstance exclNextDNI = path.size() >= 2 ? path.get(path.size()-2) : null;						
						deactivateTasksOn(correspondingDNI, exclNextDNI, this); 
					}
				}				
				setInflowFulfilled(true);
				tryActivationPropagation();
			} else
				setInflowFulfilled(false);
			break;
			//FIXME: check if no active branch --> happens after one step on active branch is canceled
			// then needs to reactivate all other branches that have no work expected set.
		default:
			break;
		}
		// check if have some delayed output that needs to be propagted
		if (this.hasPropagated())
			tryActivationPropagation();
	}
	
	public static List<DecisionNodeInstance> findCorrespondingOutFlowPath(DecisionNodeInstance dni, ProcessStep priorityIfPresent) { // finds for this decision node inFlow the corresponding outFlow of a prior DNI, if exists 		
	if (dni == null)
		return null;		
	else {
		List<DecisionNodeInstance> path = new LinkedList<>();
		// get any one (existing) predecessor DNIs, 
		DecisionNodeInstance predDNI = null;
		do {
			Optional<DecisionNodeInstance> optPred = dni.getInSteps().stream()
					.filter(task -> {
						return (priorityIfPresent != null) ? task.getName().equals(priorityIfPresent.getName()) : true;
					})
					.map(task -> task.getInDNI()).findAny();
			if (optPred.isEmpty()) return null; // there is no predecessor, can happen only for start node
			// now check if this is the outflow
			predDNI = optPred.get();
			if (predDNI.getDefinition().getOutSteps().size() > 1) { 
				path.add(predDNI);
				return path;
			}else if (predDNI.getDefinition().getInSteps().size() > 1) {// skip substructure
				List<DecisionNodeInstance> subPath = findCorrespondingOutFlowPath(predDNI, null);
				if (subPath == null) // should not happen, but just in case 
					return null;
				else {
					path.addAll(subPath);
					dni = subPath.get(subPath.size()-1);
				}
			} else {// this is a sequential connecting DNI with a single in and out branch/flow which therefore has no corresponding outFlow DNI, thus needs to be skipped	
				path.add(predDNI);
				dni = predDNI; // we skip this
			}
		} while (true);																		 			
	}		
}
	
	private static void deactivateTasksOn(DecisionNodeInstance thisDNI, DecisionNodeInstance nextExcludeDNI, DecisionNodeInstance finalDNI) {
		thisDNI.getOutSteps().stream()
			.filter(task -> (nextExcludeDNI == null) || task.getOutDNI() != nextExcludeDNI) // we filter out those on the path
			.map(task -> {
				if (!task.getExpectedLifecycleState().equals(State.NO_WORK_EXPECTED)) {					
					task.setWorkExpected(false);
				} // else we dont set this again, but we need to travers onwards, thus we cant filter out them 
				return task.getOutDNI();
			})
			.filter(dni -> dni != finalDNI) // we stop once we reache finalDNI
			.forEach(dni -> deactivateTasksOn(dni, null, finalDNI));
	}
	
	public void tryActivationPropagation() {
		// if not yet progressed,
		if (this.isInflowFulfilled() && !this.hasPropagated()) {
			// get all out task defs, check if they exist or create them
			// first time activation		
			this.getProcess().getDefinition().getStepDefinitions().stream()
				.filter(def -> def.getInDND().equals(this.getDefinition())) //retain only that have this DND as their predecessor
				.filter(td -> getProcess().getProcessSteps().stream()
								.map(ProcessStep::getDefinition)
								.noneMatch(td1 -> td1.equals(td))) // retain those that are not yet instantiated
				.forEach(td -> getProcess().createAndWireTask(td));		
			this.setHasPropagated();
		} 
		
		if (this.isInflowFulfilled() && this.hasPropagated()) {
			checkAndExecuteDataMappings();
			checkIfProcessIsCompleteNow();
		}
	}
	
	private void checkIfProcessIsCompleteNow() {
		// check if this is the last DNI, and thus notify process that it is done
		if (this.getDefinition().getOutSteps().size() == 0) {
			this.getProcess().setPostConditionsFulfilled(true);
		} 
	}
	
	private void checkAndExecuteDataMappings() {
		// also check if data mapping to execute
		boolean isEndOfProcess = false;
		if (this.getDefinition().getOutSteps().size() == 0 ) {
			// then we are done with this workflow and execute any final mappings into the workflows output
			 isEndOfProcess = true;
		}
		Set<ExecutedMapping> preparedMappings = prepareDataflow(isEndOfProcess, getDefinition().getMappings());
		preparedMappings.stream()
				.forEach(mapping -> execute(mapping));
	}
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


	private Set<ExecutedMapping> prepareDataflow(boolean isEndOfProcess, Set<MappingDefinition> definitions) {
		return definitions.stream()
				.flatMap(md -> {
						ExecutedMapping templateEM = new ExecutedMapping();
						templateEM.setFromParam(md.getFromParameter());
						templateEM.setToParam(md.getToParameter());
						if (resolveSourceCompletedTaskOrWorkflow(md.getFromStepType(), templateEM, isEndOfProcess) 
								&& resolveDestinationTaskOrWorkflow(templateEM, md.getToStepType())) { // now we have direction and fromTask set and if so then also toTask
							Set<ExecutedMapping> cand = getArtifactsFromCompletedTaskOrWorkflow(templateEM);
							return cand.stream()
									.filter(em -> shouldBeMapped(em));
						}
						else return null;//Collections.emptySet().stream();
			})
			.filter(Objects::nonNull)
			.collect(Collectors.toSet());
	}

	private boolean shouldBeMapped(ExecutedMapping em) {
		// check if not yet exists:
		switch(em.getDirection()) {
		case inToIn: //fallthrough
		case outToIn: // check the input of the task 
			if (em.getToStep().getInput(em.getToParam())==null) {
				// then mark this a mappable as new Input
				return true;
			} else {
				Set<Id> existingArt = em.getToStep().getInput(em.getToParam())
															.stream()
															.map(art -> art.id())
															.collect(Collectors.toSet());
				if (existingArt.contains(em.getArtifact().id()))
					return false;
				else {
					return true;
				}
			}
		case inToOut: // fallthrough
		case outToOut: //to output of task or process
			if (em.getToStep().getOutput(em.getToParam()).size()==0) {
				// then mark this a mappable as new Input
				return true;
			} else {
				Set<Id> existingArt = em.getToStep().getOutput(em.getToParam()).stream().map(art -> art.id()).collect(Collectors.toSet());
				if (existingArt.contains(em.getArtifact().id()))
					return false;
				else {
					return true;
				}
			}
		default:
			return false;
		}
	}
	
	private void execute(ExecutedMapping mapping) {
		mappings.add(mapping);
		switch (mapping.getDirection()) {		
			case inToIn : //fallthrough
			case outToIn :
				mapping.getToStep().addInput(mapping.getToParam(), mapping.getArtifact());
				this.mappings.add(mapping);
				break;
			case inToOut :// fallthrough
			case outToOut :
				mapping.getToStep().addOutput(mapping.getToParam(), mapping.getArtifact());
				this.mappings.add(mapping);
				break;
		}
	}
	
	private boolean resolveSourceCompletedTaskOrWorkflow(String taskId, ExecutedMapping templateEM, boolean isEndOfProcess) {
		return getProcess().getProcessSteps().stream()	
		.filter(wft -> wft.getDefinition().getName().equals(taskId) )
		.filter(wft -> wft.getExpectedLifecycleState().equals(State.COMPLETED)) // we only map data for tasks that are indeed completed
		.filter(Objects::nonNull)
		.findFirst().map( wft -> { if (isEndOfProcess) 
										templateEM.setDirection(DIR.outToOut);
									else
										templateEM.setDirection(DIR.outToIn);
								templateEM.setFromStep(wft);
								return true;
							} ).orElseGet( () -> {
			if (getProcess().getDefinition().getName().equals(taskId)) {
				if (isEndOfProcess) 
					templateEM.setDirection(DIR.inToOut);
				else
					templateEM.setDirection(DIR.inToIn);
				templateEM.setFromStep(getProcess());
				return true;
			}
			else { 
				return false;
			}
		});
	}
	
	private Set<ExecutedMapping> getArtifactsFromCompletedTaskOrWorkflow(ExecutedMapping templateEM) {
		switch(templateEM.getDirection()) {
		case inToIn: //fallthrough
		case inToOut: // for input from process
			return templateEM.getFromStep().getInput(templateEM.getFromParam())
					.stream()
					.map(art -> ExecutedMapping.copyFrom(templateEM).fluentSetArtifact(art))
					.collect(Collectors.toSet());
		case outToIn: //fallthrough
		case outToOut: //for output from task
			// we checked for completed task earlier when adding it to the template
			return templateEM.getFromStep().getOutput(templateEM.getFromParam())
														.stream()
														.map(art -> ExecutedMapping.copyFrom(templateEM).fluentSetArtifact(art))
														.collect(Collectors.toSet());
		default:
			return Collections.emptySet();
		}
	}
	
	private boolean resolveDestinationTaskOrWorkflow(ExecutedMapping templateEM, String taskId) {
		switch(templateEM.getDirection()) {
		case outToIn: //fallthrough
		case inToIn: // for input from process to input of task
			return getProcess().getProcessSteps().stream()	
			.filter(wft -> wft.getDefinition().getName().equals(taskId) )
			.map(wft -> (ProcessStep)wft)
			.findAny()
			.map( wft -> { templateEM.setToStep(wft);
							return true;})
			.orElse(false);
		case inToOut: //fallthrough
		case outToOut: //for output from task
			if (getProcess().getDefinition().getName().equals(taskId)) {
				templateEM.setToStep(getProcess());
				return true;
			} else 
		  		return false;
		default:
			return false;
		}
	}

	
	public static InstanceType getOrCreateDesignSpaceCoreSchema(Workspace ws) {
		Optional<InstanceType> thisType = ws.debugInstanceTypes().stream()
			.filter(it -> it.name().equals(designspaceTypeId))
			.findAny();
		if (thisType.isPresent())
			return thisType.get();
		else {
			InstanceType typeStep = ws.createInstanceType(designspaceTypeId, ws.TYPES_FOLDER, ProcessInstanceScopedElement.getOrCreateDesignSpaceCoreSchema(ws));
			typeStep.createPropertyType(CoreProperties.isInflowFulfilled.toString(), Cardinality.SINGLE, Workspace.BOOLEAN);
			typeStep.createPropertyType(CoreProperties.hasPropagated.toString(), Cardinality.SINGLE, Workspace.BOOLEAN);
			typeStep.createPropertyType(CoreProperties.dnd.toString(), Cardinality.SINGLE, DecisionNodeDefinition.getOrCreateDesignSpaceCoreSchema(ws));
			typeStep.createPropertyType(CoreProperties.inSteps.toString(), Cardinality.SET, ProcessStep.getOrCreateDesignSpaceCoreSchema(ws));
			typeStep.createPropertyType(CoreProperties.outSteps.toString(), Cardinality.SET, ProcessStep.getOrCreateDesignSpaceCoreSchema(ws));
			return typeStep;
		}
	}
	
	protected static DecisionNodeInstance getInstance(Workspace ws, DecisionNodeDefinition dnd) {
		Instance instance = ws.createInstance(getOrCreateDesignSpaceCoreSchema(ws), dnd.getName()+"_"+UUID.randomUUID());
		DecisionNodeInstance dni = WrapperCache.getWrappedInstance(DecisionNodeInstance.class, instance);
		dni.init(dnd);
		return dni;
	}
	
	protected void init(DecisionNodeDefinition dnd) {
		instance.getPropertyAsSingle(CoreProperties.dnd.toString()).set(dnd.getInstance());
		instance.getPropertyAsSingle(CoreProperties.hasPropagated.toString()).set(false);
		// if kickoff DN, then set inflow fulfillment to true
		this.setInflowFulfilled(dnd.getInSteps().size() == 0 ? true : false);
		
	}
}
