package at.jku.isse.passiveprocessengine.instance;

import at.jku.isse.designspace.core.model.Cardinality;
import at.jku.isse.designspace.core.model.Instance;
import at.jku.isse.designspace.core.model.InstanceType;
import at.jku.isse.designspace.core.model.Workspace;
import at.jku.isse.passiveprocessengine.ProcessInstanceScopedElement;
import at.jku.isse.passiveprocessengine.WrapperCache;
import at.jku.isse.passiveprocessengine.definition.DecisionNodeDefinition;
import at.jku.isse.passiveprocessengine.definition.MappingDefinition;
import at.jku.isse.passiveprocessengine.instance.RuntimeMapping.FlowDir;
import at.jku.isse.passiveprocessengine.instance.StepLifecycle.State;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@Slf4j
public class DecisionNodeInstance extends ProcessInstanceScopedElement {

	public static enum CoreProperties {isInflowFulfilled, hasPropagated, dnd, inSteps, outSteps};
	public static final String designspaceTypeId = DecisionNodeInstance.class.getSimpleName();
	
	public DecisionNodeInstance(Instance instance) {
		super(instance);
	}
	
	protected DecisionNodeDefinition getDefinition() {
		return  WrapperCache.getWrappedInstance(DecisionNodeDefinition.class, instance.getPropertyAsInstance(CoreProperties.dnd.toString()));
	}
	
	private void setInflowFulfilled(boolean isFulfilled) {
		if (isInflowFulfilled() != isFulfilled) // a change
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
		
	public void signalPrevTaskDataChanged(ProcessStep prevTask) {
		boolean isEndOfProcess = false;
		if (this.getDefinition().getOutSteps().size() == 0 ) {
			// then we are done with this workflow and execute any final mappings into the workflows output
			 isEndOfProcess = true;
		}
		checkAndExecuteDataMappings(isEndOfProcess);// we just check everything, not too expensive as mappings are typically few.
	}
	
	public void tryInConditionsFullfilled() {	
		switch(this.getDefinition().getInFlowType()) {
		case AND: 
			// we ignore Expected Canceled and no work expected
			// we expect other to be E: COMPLETED, thus for a deviation we still propagate all inputs once we progated in the past, 
			//thus once a prior expected step get cancelled, we remove its mapping
			boolean inFlowANDok = getInSteps().stream()
				.filter(step -> !step.getExpectedLifecycleState().equals(State.NO_WORK_EXPECTED) )
				.filter(step -> !step.getExpectedLifecycleState().equals(State.CANCELED) )
				.allMatch(step -> step.getExpectedLifecycleState().equals(State.COMPLETED) );
			setInflowFulfilled(inFlowANDok);
			tryActivationPropagation(); // we can always trigger, check for inflowfulfilled is done there
			
//			if (inTaskStatus.values().stream()
//					.filter(p -> !(p.equals(Progress.DISABLED))) // we ignore task.getExpectedLifecycleState().equals(State.NO_WORK_EXPECTED) || task.getExpectedLifecycleState().equals(State.CANCELED) and expect the rest to be Expected)COMPLETE
//					.allMatch(p -> p.equals(Progress.ENABLED)) ) { // if non are left, we still progress trusting that any canceled or no work expected tasks are not required anymore
//				inTaskStatus.entrySet().stream()
//					.filter(entry -> !entry.getValue().equals(Progress.DISABLED) ) // we ignore those
//					.peek(entry -> entry.setValue(Progress.USED))
//					.collect(Collectors.toSet())
//					.forEach(entry2 -> inTaskStatus.put(entry2.getKey(), entry2.getValue()));
//				setInflowFulfilled(true);
//				tryActivationPropagation();
//			} else
//				if (inTaskStatus.values().stream()
//						.filter(p -> !(p.equals(Progress.DISABLED))) // we ignore those
//						.allMatch(p -> p.equals(Progress.USED)) ) // all have been used, still are, thus we remain in fulfilled inflow
//					tryActivationPropagation();// we were triggred, so we propagate
//				else
//					setInflowFulfilled(false);
			break;
		case OR:
			// as soon as one is E: COMPLETED we set fulfillment and activate further, even when deviating
			boolean inFlowORok = getInSteps().stream()
				.filter(step -> !step.getExpectedLifecycleState().equals(State.NO_WORK_EXPECTED) )
				.filter(step -> !step.getExpectedLifecycleState().equals(State.CANCELED) )
				.anyMatch(step -> step.getExpectedLifecycleState().equals(State.COMPLETED) );
			setInflowFulfilled(inFlowORok);
			tryActivationPropagation(); // we can always trigger, check for inflowfulfilled is done there
			
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
//				setInflowFulfilled(true);
//				tryActivationPropagation();
//			} else
//				setInflowFulfilled(false);
			break;
		case XOR:
			// as soon as the first is E: complete we trigger, all others will be set to no work expected
			// thus there may be only one E:complete step as the others will be E: no work expected or E: cancelled
			// one an E:complete is cancelled however, we need to reactivate all E: no work expected and select there the first one for further mapping
			// we don't set any E: no work expected until the first branch signals completion
			List<ProcessStep> steps = getInSteps().stream()
			.filter(step -> !step.getExpectedLifecycleState().equals(State.NO_WORK_EXPECTED) )
			.filter(step -> !step.getExpectedLifecycleState().equals(State.CANCELED) )
			.filter(step -> step.getExpectedLifecycleState().equals(State.COMPLETED) )
			.collect(Collectors.toList());
			setInflowFulfilled(steps.size() == 1);
			
			// now if not fulfilled and we have already propagated: need to switch branches
			if (!isInflowFulfilled() && hasPropagated()) {
				//FIXME: check if no active branch --> happens after one step on active branch is canceled
				// then needs to reactivate all other branches that have no work expected set.
				;
			} else // now if fulfilled, and not yet propagated: need to 'disable' other branches 
			if (isInflowFulfilled() && !hasPropagated()) {
				ProcessStep chosenStep = steps.get(0);
				getInSteps().stream()
				.filter(step -> !step.getExpectedLifecycleState().equals(State.NO_WORK_EXPECTED) )
				//.filter(step -> !step.getExpectedLifecycleState().equals(State.CANCELED) ) // we also mark cancelled steps as no work expected, as if their cancel condition is lifted, the still should stay in no work expected
				.filter(step -> step != chosenStep)
				.forEach(step -> step.setWorkExpected(false));
				
				
				if (this.getDefinition().getInSteps().size() > 1) {// there are other inbranches that we now need to deactivate
					List<DecisionNodeInstance> path = findCorrespondingOutFlowPath(this, chosenStep); //the path to the corresponding outflow node via the just activating task
					if (path != null) {
						path.add(0, this);
						// deactivate those not on path
						DecisionNodeInstance correspondingDNI = path.get(path.size()-1);
						DecisionNodeInstance exclNextDNI = path.size() >= 2 ? path.get(path.size()-2) : null;						
						deactivateTasksOn(correspondingDNI, exclNextDNI, this); 
					}
				}	
			}
			
			tryActivationPropagation(); // we can always trigger, check for inflow fulfilled is done there
			
//			Optional<ProcessStep> optId = inTaskStatus.entrySet().stream()
//					.filter(entry -> entry.getValue().equals(Progress.ENABLED))
//					.findAny()
//					.map(entry -> entry.getKey());
//			if (optId.isPresent()) {
//				ProcessStep step = optId.get();
//				inTaskStatus.put(step, Progress.USED);
//				inTaskStatus.entrySet().stream()
//				.filter(entry -> entry.getValue().equals(Progress.ENABLED) || entry.getValue().equals(Progress.WAITING) ) 
//				.map(entry -> { entry.getKey().setWorkExpected(false); //this only works for XORs that have no sub branches, 
//								entry.setValue(Progress.DISABLED); 
//								return entry; })
//				.collect(Collectors.toSet())
//				.forEach(entry2 -> inTaskStatus.put(entry2.getKey(),  entry2.getValue()));
//				// TOOD: find all tasks that have not reached until here, (which is when one xor branch is substructured and has not progressed as far yet to pop up as branch here yet)	
//				
//				if (this.getDefinition().getInSteps().size() > 1) {// there are other inbranches that we now need to deactivate
//					List<DecisionNodeInstance> path = findCorrespondingOutFlowPath(this, step); //the path to the corresponding outflow node via the just activating task
//					if (path != null) {
//						path.add(0, this);
//						// deactivate those not on path
//						DecisionNodeInstance correspondingDNI = path.get(path.size()-1);
//						DecisionNodeInstance exclNextDNI = path.size() >= 2 ? path.get(path.size()-2) : null;						
//						deactivateTasksOn(correspondingDNI, exclNextDNI, this); 
//					}
//				}				
//				setInflowFulfilled(true);
//				tryActivationPropagation();
//			} else
//				setInflowFulfilled(false);
			break;
			//FIXME: check if no active branch --> happens after one step on active branch is canceled
			// then needs to reactivate all other branches that have no work expected set.
		default:
			break;
		}
		// check if have some delayed output that needs to be propagted
		//if (this.hasPropagated())
		//	tryActivationPropagation();
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
		
		boolean isEndOfProcess = false;
		if (this.getDefinition().getOutSteps().size() == 0 ) {
			// is this the final DNI?
			 isEndOfProcess = true;
		}
		if (this.isInflowFulfilled() && this.hasPropagated()) {
			checkAndExecuteDataMappings(isEndOfProcess);
			if (isEndOfProcess)
				this.getProcess().setPostConditionsFulfilled(true);
		}
		if (this.hasPropagated() && !this.isInflowFulfilled() && isEndOfProcess) {
			// just make sure that the process is not complete anymore (may be called several times)
			this.getProcess().setPostConditionsFulfilled(false);
		}
	}
	
	private void checkAndExecuteDataMappings(boolean isEndOfProcess) {
		// all mappings of this DNI resolved:
		Set<RuntimeMapping> templates = getDefinition().getMappings().stream()
				.map(mdef -> resolveMappingDefinitionToTemplates(mdef, isEndOfProcess))
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());

		// get desired and actual mappings
		Set<RuntimeMapping> desiredMappings = templates.stream().flatMap(em -> fillViaSource(em)).collect(Collectors.toSet());
		Set<RuntimeMapping> actualMappings = templates.stream().flatMap(em -> fillViaDestination(em)).collect(Collectors.toSet());

		// in desired and actual --> keep
		Set<RuntimeMapping> keepM = desiredMappings.stream().filter(entry -> actualMappings.contains(entry)).collect(Collectors.toSet());
		// in desired and not actual --> add
		Set<RuntimeMapping> newM = desiredMappings.stream().filter(entry -> !actualMappings.contains(entry)).collect(Collectors.toSet());
		// in actual but not desired --> remove
		Set<RuntimeMapping> delM = actualMappings.stream().filter(entry -> !desiredMappings.contains(entry)).collect(Collectors.toSet());
		// tricky bit: the same artifact could be affected by multiple mappings, a positive mapping overrides a negative mapping, no artifact is added twice
		// thus check, any remove entry that is found in desired entry is kept, --> BUT BEWARE: check based on destination step, param, and artifact, but not based on source.
		 // so for each step, param, art, in toRemove --> check if found in toAdd, we do this in an inefficient double loop, but as mappings are expected to be small in size, this should be fine
		Stream<RuntimeMapping> delFinallyM = delM.stream().filter(rmDel -> {
//					return !newM.stream().anyMatch(rmAdd -> (rmAdd.getArtifact() == rmDel.getArtifact() 
//													&& rmAdd.getToStep() == rmDel.getToStep()
//													&& rmAdd.getToParam() == rmDel.getToParam()) );
					return !newM.contains(rmDel); // we have hashCode and equals use only those three properties
				});
		
		//Set<RuntimeMapping> preparedMappings =  prepareDataflow(isEndOfProcess, getDefinition().getMappings());
		// now we know which to add and which to keep, next determine which to remove!
		//Set<RuntimeMapping> combined = Stream.concat(preparedMappings.stream(), 
		//											determineUndoMappingStatus(preparedMappings, getDefinition().getMappings()).stream())
		//       								.collect(Collectors.toSet()); 
		keepM.forEach(em -> {
			log.debug("Keep: "+em.toString());
		});
		newM.forEach(em -> {
			log.debug("Add: "+em.toString());
			execute(em);
		});
		delFinallyM.forEach(em -> {
			log.debug("Del: "+em.toString());
			undo(em);
		});
	}
	
	private Stream<RuntimeMapping> fillViaSource(RuntimeMapping templateEM) {
			switch(templateEM.getDirection()) {
			case inToIn: //fallthrough
			case inToOut: // for input from process
				return templateEM.getFromStep().getInput(templateEM.getFromParam())
						.stream()
						.map(art -> RuntimeMapping.copyFrom(templateEM).fluentSetArtifact(art));
						
			case outToIn: //fallthrough
			case outToOut: //for output from task
				// we checked for completed task earlier when adding it to the template
				return templateEM.getFromStep().getOutput(templateEM.getFromParam())
															.stream()
															.map(art -> RuntimeMapping.copyFrom(templateEM).fluentSetArtifact(art));							
			default:
				return Stream.empty();
			}
	}

	private Stream<RuntimeMapping> fillViaDestination(RuntimeMapping templateEM) {
		switch(templateEM.getDirection()) {
		case inToIn: //fallthrough
		case outToIn:	
			// we checked for completed task earlier when adding it to the template
			return templateEM.getToStep().getInput(templateEM.getToParam())
														.stream()
														.map(art -> RuntimeMapping.copyFrom(templateEM).fluentSetArtifact(art));
		case outToOut: // fallthrough	
		case inToOut: 
			return templateEM.getToStep().getOutput(templateEM.getToParam())
					.stream()
					.map(art -> RuntimeMapping.copyFrom(templateEM).fluentSetArtifact(art));
		default:
			return Stream.empty();
		}
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


//	private Set<RuntimeMapping> prepareDataflow(boolean isEndOfProcess, Set<MappingDefinition> definitions) {
//		return definitions.stream()
//				.flatMap(md -> {
//						RuntimeMapping templateEM = new RuntimeMapping();
//						templateEM.setFromParam(md.getFromParameter());
//						templateEM.setToParam(md.getToParameter());
//						if (resolveSourceCompletedTaskOrWorkflow(md.getFromStepType(), templateEM, isEndOfProcess) 
//								&& resolveDestinationTaskOrWorkflow(templateEM, md.getToStepType())) { // now we have direction and fromTask set and if so then also toTask
//							Set<RuntimeMapping> cand = getArtifactsFromCompletedTaskOrWorkflow(templateEM);
//							// now we have all RuntimeMappings (i.e., mapping instances) for this mapping and the available artifacts
//							// next we determine which of those are still ok, which ones are to be added
//							return cand.stream()
//							//		.filter(em -> shouldBeMapped(em)); // we no longer filter here, but eval every mapping, to be handeled separately later
//									.map(em -> determineMappingStatus(em));
//							// this might return mappings referencing the same artifact multiple times, which is not an issue, as step input and outputs are sets anyway
//						}
//						else return null;//Collections.emptySet().stream();
//			})
//			.filter(Objects::nonNull)
//			.collect(Collectors.toSet());
//	}

//	private RuntimeMapping determineMappingStatus(RuntimeMapping em) {
//		// check if not yet exists:
//		switch(em.getDirection()) {
//		case inToIn: //fallthrough
//		case outToIn: // check the input of the task 
//			Set<Instance> in = em.getToStep().getInput(em.getToParam());
//			if (in==null || in.isEmpty()) {
//				// then mark this a mappable as new Input
//				em.setStatus(Status.TO_BE_ADDED);
//			} else {
//				Set<Id> existingArt = in.stream()
//															.map(art -> art.id())
//															.collect(Collectors.toSet());
//				if (existingArt.contains(em.getArtifact().id()))
//					em.setStatus(Status.CONSISTENT);
//				else {
//					em.setStatus(Status.TO_BE_ADDED);
//				}
//			}
//			break;
//		case inToOut: // fallthrough
//		case outToOut: //to output of task or process
//			Set<Instance> out = em.getToStep().getOutput(em.getToParam());
//			if (out == null || out.isEmpty()) {
//				// then mark this a mappable as new Input
//				em.setStatus(Status.TO_BE_ADDED);
//			} else {
//				Set<Id> existingArt = out.stream().map(art -> art.id()).collect(Collectors.toSet());
//				if (existingArt.contains(em.getArtifact().id()))
//					em.setStatus(Status.CONSISTENT);
//				else {
//					em.setStatus(Status.TO_BE_ADDED);
//				}
//			}
//			break;
//		default:
//			em.setStatus(Status.CONSISTENT);
//			break;
//		}
//		return em;
//	}
	
	
//	private Set<RuntimeMapping> determineUndoMappingStatus(Set<RuntimeMapping> newAndExistingMappings, Set<MappingDefinition> mDef) {
//		// we need to first collect all target params (in and outs) from the DNI - assumption: and in is only mapped from process or prior steps, and out is only mapped from step to process
//		// then obtain all artifacts available
//		// for each artifact check if it is used in any of the mappings (in that param)
//		// if not, the mark it as removed, NOTE: we only have to set the target param and artifact, not the source as we dont care where it came from (and might not even be able to establish this).
//		
//		Set<RuntimeMapping> inMappingsToRemove = mDef.stream()
//			.filter(em -> em.getFlowDir().equals(FlowDir.inToIn) || em.getFlowDir().equals(FlowDir.outToIn)) // look at in and out params separately
//			.map(em -> new AbstractMap.SimpleEntry<ProcessStep, String>(em.getToStep(), em.getToParam()))
//			.distinct() // now we have all pairs of <step ,inparameter >
//			.flatMap(entry -> checkExistingInArtifacts(entry.getKey(), entry.getValue(),  artInNewOrExistingMapping(newAndExistingMappings, entry.getKey(), entry.getValue())))
//			.collect(Collectors.toSet());
//		
//		Set<RuntimeMapping> outMappingsToRemove = newAndExistingMappings.stream()
//				.filter(em -> em.dir.equals(FlowDir.inToOut) || em.dir.equals(FlowDir.outToOut)) // look at in and out params separately
//				.map(em -> new AbstractMap.SimpleEntry<ProcessStep, String>(em.getToStep(), em.getToParam()))
//				.distinct() // now we have all pairs of <step ,inparameter >
//				.flatMap(entry -> checkExistingOutArtifacts(entry.getKey(), entry.getValue(),  artInNewOrExistingMapping(newAndExistingMappings, entry.getKey(), entry.getValue())))
//				.collect(Collectors.toSet());
//		
//		// we also need to check if there are any mapping target that should be empty, those will not appear as new or existing mappings
//		// so for each destination in mDef we need to check if the target artifact appears in the newOrExisting Mapping, other wise remove
//		
//		return  Stream.concat(inMappingsToRemove.stream(),outMappingsToRemove.stream()).collect(Collectors.toSet());
//	}
	
//	private Stream<RuntimeMapping> checkExistingInArtifacts(ProcessStep step, String param, Set<Instance> mappedArt) {
//		return step.getInput(param).stream()
//			.filter(art -> !mappedArt.contains(art)) // any art that is not in existing mappings for that input
//			.map(art -> new RuntimeMapping(null, null, art, step, param, FlowDir.outToIn)) // source does not matter, as we are only removing this mapping later
//			.map(m-> { m.setStatus(Status.TO_BE_REMOVED); return m;});
//	}
//	
//	private Stream<RuntimeMapping> checkExistingOutArtifacts(ProcessStep step, String param, Set<Instance> mappedArt) {
//		return step.getOutput(param).stream()
//			.filter(art -> !mappedArt.contains(art)) // any art that is not in existing mappings for that output
//			.map(art -> new RuntimeMapping(null, null, art, step, param, FlowDir.outToOut)) // source does not matter, as we are only removing this mapping later
//			.map(m-> { m.setStatus(Status.TO_BE_REMOVED); return m;});
//	}
//	
//	private Set<Instance> artInNewOrExistingMapping(Set<RuntimeMapping> newAndExistingMappings, ProcessStep step, String param) {
//		return newAndExistingMappings.stream()
//			.filter(m -> m.getToStep().equals(step))
//			.filter(m -> m.getToParam().equals(param))
//			.map(m -> m.getArtifact())
//			.collect(Collectors.toSet());
//	}
	
	private void execute(RuntimeMapping mapping) {
		//mappings.add(mapping);
		switch (mapping.getDirection()) {		
			case inToIn : //fallthrough
			case outToIn :
				mapping.getToStep().addInput(mapping.getToParam(), mapping.getArtifact());
			//	this.mappings.add(mapping);
				break;
			case inToOut :// fallthrough
			case outToOut :
				mapping.getToStep().addOutput(mapping.getToParam(), mapping.getArtifact());
			//	this.mappings.add(mapping);
				break;
		}
	}
	
	private void undo(RuntimeMapping mapping) {
		switch (mapping.getDirection()) {		
		case inToIn : //fallthrough
		case outToIn :
			mapping.getToStep().removeInput(mapping.getToParam(), mapping.getArtifact());
			break;
		case inToOut :// fallthrough
		case outToOut :
			mapping.getToStep().removeOutput(mapping.getToParam(), mapping.getArtifact());
			break;
		}
	}
	
	private RuntimeMapping resolveMappingDefinitionToTemplates(MappingDefinition mdef, boolean isEndOfProcess) {
		RuntimeMapping templateEM = new RuntimeMapping();
		templateEM.setFromParam(mdef.getFromParameter());
		templateEM.setToParam(mdef.getToParameter());
		// first get the source
		getProcess().getProcessSteps().stream()	
		.filter(wft -> wft.getDefinition().getName().equals(mdef.getFromStepType()) )
		.filter(wft -> wft.getExpectedLifecycleState().equals(State.COMPLETED) &&  wft.getActualLifecycleState().equals(State.COMPLETED)) // we only map data for tasks that are indeed completed
		.filter(Objects::nonNull)
		.findFirst().ifPresentOrElse( wft -> { if (isEndOfProcess) 
												templateEM.setDirection(FlowDir.outToOut);
											else
												templateEM.setDirection(FlowDir.outToIn);
											templateEM.setFromStep(wft);
									} ,
			() -> {
			if (getProcess().getDefinition().getName().equals(mdef.getFromStepType())) { // the taskId identifies the process
				if (isEndOfProcess) 
					templateEM.setDirection(FlowDir.inToOut);
				else
					templateEM.setDirection(FlowDir.inToIn);
				templateEM.setFromStep(getProcess());
			}
		});
		if (templateEM.getFromStep() == null) // not found, e.g., a step not yet created, can only happen when premature step is not connected to remaining process
			return null;
		
		// now do the destination part
		switch(templateEM.getDirection()) {
		case outToIn: //fallthrough
		case inToIn: // for input from process to input of task
			getProcess().getProcessSteps().stream()	
			.filter(wft -> wft.getDefinition().getName().equals(mdef.getToStepType()) )
			.map(wft -> (ProcessStep)wft)
			.findAny()
			.ifPresent( wft -> { templateEM.setToStep(wft);
							});
			break;
		case inToOut: //fallthrough
		case outToOut: //for output from task
			if (getProcess().getDefinition().getName().equals(mdef.getToStepType())) {
				templateEM.setToStep(getProcess());
			} 
			break;
		default:
			;
		}
		
		if (templateEM.getToStep() == null) // not found, e.g., a step not yet created, should not happen
			return null;
	
		return templateEM;
	}
	
//	private boolean resolveSourceCompletedTaskOrWorkflow(String taskId, RuntimeMapping templateEM, boolean isEndOfProcess) {
//		return getProcess().getProcessSteps().stream()	
//		.filter(wft -> wft.getDefinition().getName().equals(taskId) )
//		.filter(wft -> wft.getExpectedLifecycleState().equals(State.COMPLETED)) // we only map data for tasks that are indeed completed
//		.filter(Objects::nonNull)
//		.findFirst().map( wft -> { if (isEndOfProcess) 
//										templateEM.setDirection(FlowDir.outToOut);
//									else
//										templateEM.setDirection(FlowDir.outToIn);
//								templateEM.setFromStep(wft);
//								return true;
//							} ).orElseGet( () -> {
//			if (getProcess().getDefinition().getName().equals(taskId)) { // the taskId identifies the process
//				if (isEndOfProcess) 
//					templateEM.setDirection(FlowDir.inToOut);
//				else
//					templateEM.setDirection(FlowDir.inToIn);
//				templateEM.setFromStep(getProcess());
//				return true;
//			}
//			else { 
//				return false;
//			}
//		});
//	}
	
//	private Set<RuntimeMapping> getArtifactsFromCompletedTaskOrWorkflow(RuntimeMapping templateEM) { // the filtering of completed tasks was done earlier, templateEM fulfills this constraint already
//		switch(templateEM.getDirection()) {
//		case inToIn: //fallthrough
//		case inToOut: // for input from process
//			return templateEM.getFromStep().getInput(templateEM.getFromParam())
//					.stream()
//					.map(art -> RuntimeMapping.copyFrom(templateEM).fluentSetArtifact(art))
//					.collect(Collectors.toSet());
//		case outToIn: //fallthrough
//		case outToOut: //for output from task
//			// we checked for completed task earlier when adding it to the template
//			return templateEM.getFromStep().getOutput(templateEM.getFromParam())
//														.stream()
//														.map(art -> RuntimeMapping.copyFrom(templateEM).fluentSetArtifact(art))
//														.collect(Collectors.toSet());
//		default:
//			return Collections.emptySet();
//		}
//	}
	
//	private boolean resolveDestinationTaskOrWorkflow(RuntimeMapping templateEM, String taskId) {
//		switch(templateEM.getDirection()) {
//		case outToIn: //fallthrough
//		case inToIn: // for input from process to input of task
//			return getProcess().getProcessSteps().stream()	
//			.filter(wft -> wft.getDefinition().getName().equals(taskId) )
//			.map(wft -> (ProcessStep)wft)
//			.findAny()
//			.map( wft -> { templateEM.setToStep(wft);
//							return true;})
//			.orElse(false);
//		case inToOut: //fallthrough
//		case outToOut: //for output from task
//			if (getProcess().getDefinition().getName().equals(taskId)) {
//				templateEM.setToStep(getProcess());
//				return true;
//			} else 
//		  		return false;
//		default:
//			return false;
//		}
//	}

	
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
		instance.getPropertyAsSingle(CoreProperties.isInflowFulfilled.toString()).set(dnd.getInSteps().size() == 0 ? true : false);
	}

	@Override
	public String toString() {
		return "DecisionNodeInstance [" + getDefinition().getName() + ", isInflowFulfilled()="
				+ isInflowFulfilled() + ", hasPropagated()=" + hasPropagated() + "]";
	}
	
	
}
