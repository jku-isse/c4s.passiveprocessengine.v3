package at.jku.isse.passiveprocessengine.instance;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import at.jku.isse.designspace.core.model.Cardinality;
import at.jku.isse.designspace.core.model.Instance;
import at.jku.isse.designspace.core.model.InstanceType;
import at.jku.isse.designspace.core.model.Workspace;
import at.jku.isse.passiveprocessengine.ProcessInstanceScopedElement;
import at.jku.isse.passiveprocessengine.WrapperCache;
import at.jku.isse.passiveprocessengine.definition.DecisionNodeDefinition;
import at.jku.isse.passiveprocessengine.definition.DecisionNodeDefinition.InFlowType;
import at.jku.isse.passiveprocessengine.instance.StepLifecycle.State;
import at.jku.isse.passiveprocessengine.instance.messages.Events;
import at.jku.isse.passiveprocessengine.instance.messages.Events.ProcessChangedEvent;
import lombok.extern.slf4j.Slf4j;


@Slf4j
public class DecisionNodeInstance extends ProcessInstanceScopedElement {

	public static enum CoreProperties {isInflowFulfilled, hasPropagated, dnd, inSteps, outSteps, closingDN};
	public static final String designspaceTypeId = DecisionNodeInstance.class.getSimpleName();
	
	private boolean isInternalPropagationDone = false;
	private InterStepDataMapper mapper = null;
	
	public DecisionNodeInstance(Instance instance) {
		super(instance);
	}
	
	public DecisionNodeDefinition getDefinition() {
		return  WrapperCache.getWrappedInstance(DecisionNodeDefinition.class, instance.getPropertyAsInstance(CoreProperties.dnd.toString()));
	}
	
	private void setInflowFulfilled(boolean isFulfilled) {
		//if (isInflowFulfilled() != isFulfilled) // a change -> not necessary, is done internally anyway
		instance.getPropertyAsSingle(CoreProperties.isInflowFulfilled.toString()).set(isFulfilled);
	}
	
	public boolean isInflowFulfilled() {
		return (boolean) instance.getPropertyAsValueOrElse(CoreProperties.isInflowFulfilled.toString(), () -> false);
	}
	
	private void setHasPropagated() {
		instance.getPropertyAsSingle(CoreProperties.hasPropagated.toString()).set(true);
	}
	
	public boolean hasPropagated() {
		return (boolean) instance.getPropertyAsValueOrElse(CoreProperties.hasPropagated.toString(), () -> false);
	}
	
	@SuppressWarnings("unchecked")
	protected void addOutStep(ProcessStep step) {
		instance.getPropertyAsSet(CoreProperties.outSteps.toString()).add(step.getInstance());
	}
	
	@SuppressWarnings("unchecked")
	public Set<ProcessStep> getOutSteps() {
		return (Set<ProcessStep>) instance.getPropertyAsSet(CoreProperties.outSteps.toString()).stream()
			.map(inst -> WrapperCache.getWrappedInstance(ProcessInstance.getMostSpecializedClass((Instance) inst), (Instance)inst))
			.collect(Collectors.toSet());
	}
	
	@SuppressWarnings("unchecked")
	public void addInStep(ProcessStep step) {
		instance.getPropertyAsSet(CoreProperties.inSteps.toString()).add(step.getInstance());
	}
	
	@SuppressWarnings("unchecked")
	public Set<ProcessStep> getInSteps() {
		return (Set<ProcessStep>) instance.getPropertyAsSet(CoreProperties.inSteps.toString()).stream()
			.map(inst -> WrapperCache.getWrappedInstance(ProcessInstance.getMostSpecializedClass((Instance) inst), (Instance)inst))
			.collect(Collectors.toSet());
	}
	
	protected List<ProcessChangedEvent> signalStateChanged(ProcessStep step) {
		List<Events.ProcessChangedEvent> events = new LinkedList<>();
		if( updateInConditionsFullfilledAndCheckIfHasChanged()) {
			if (isInflowFulfilled()) { // now we transitioned to fulfilled:
				if (this.getDefinition().getInFlowType().equals(InFlowType.XOR) && !hasPropagated())
					events.addAll(selectChosenXORStep());				
				events.addAll(doActivationPropagation()); // lets see if we need to propagate anything
			} else { // not we transitioned to unfulfilled, 
				if (this.getDefinition().getInFlowType().equals(InFlowType.XOR)) { // only relevant if we are XOR DNI
					// now if not fulfilled and we have already propagated: need to switch branches
					if (hasPropagated()) {
						//XOR inflow now unfulfill means the step triggering this was previously complete and is no more (now its e.g., cancelled or postcond no longer fulfilled)
						//hence no active branch , 	// then needs to reactivate all other branches that have no work expected set.
						events.addAll(releaseChosenXORStep(step)); 
					} 
				} else // releaseChoesenXORStep will result in reevaluation and reentry in this method			
					if (this.hasPropagated() && this.getDefinition().getOutSteps().size() == 0) {
					// just make sure that the process is not complete anymore (may be called several times)
						events.addAll(this.getProcess().signalDNIChanged(this));
					}
			}			
		} else { //check if immediate downstream tasks should be instantiated
			if (isImmediateInstantiateAllStepsEnabled()) {
				State exp = step.getExpectedLifecycleState();
				if (exp.equals(State.ACTIVE) || exp.equals(State.ENABLED)) {
					events.addAll(initiateDownstreamSteps(isImmediateDataPropagationEnabled()));
				}					
			}
			// if a now completed step (that is not essential in an OR dni) has data propagation delayed until completion, then we wont trigger datapropagation so far and need to do it here
			if (!isImmediateDataPropagationEnabled() && step.getActualLifecycleState().equals(State.COMPLETED)) {
				checkAndExecuteDataMappings(this.getDefinition().getOutSteps().size() == 0, false);
			}
		}			
		return events;
	}
	
	public List<Events.ProcessChangedEvent> signalPrevTaskDataChanged(ProcessStep prevTask) {
		if (isImmediateDataPropagationEnabled() || prevTask.getActualLifecycleState().equals(State.COMPLETED)) {
			//should we propagate now?		
			boolean isEndOfProcess = false;
			if (this.getDefinition().getOutSteps().size() == 0 ) {
				// then we are done with this workflow and execute any final mappings into the workflows output
				isEndOfProcess = true;
			}		
			return checkAndExecuteDataMappings(isEndOfProcess, false);// we just check everything, not too expensive as mappings are typically few.
		}
		return Collections.emptyList();
	}
	
	private boolean updateInConditionsFullfilledAndCheckIfHasChanged() {	
		boolean prioCond = this.isInflowFulfilled();
		switch(this.getDefinition().getInFlowType()) {		
		case SEQ: //fallthrough as treated just like and
		case AND: 
			// we ignore Expected Canceled and no work expected
			// we expect other to be E: COMPLETED, thus for a deviation we still propagate all inputs once we progated in the past, 
			//thus once a prior expected step get cancelled, we remove its mapping			
			boolean inFlowANDok = getInSteps().stream()
				.filter(step -> !step.getExpectedLifecycleState().equals(State.NO_WORK_EXPECTED) )
				.filter(step -> !step.getExpectedLifecycleState().equals(State.CANCELED) )
				.allMatch(step -> (step.getExpectedLifecycleState().equals(State.COMPLETED) && step.getActualLifecycleState().equals(State.COMPLETED)) );
			setInflowFulfilled(inFlowANDok);	
			return inFlowANDok!=prioCond;		
		case OR:
			// as soon as one is E: COMPLETED we set fulfillment and activate further, even when deviating
			boolean inFlowORok = getInSteps().stream()
				.filter(step -> !step.getExpectedLifecycleState().equals(State.NO_WORK_EXPECTED) )
				.filter(step -> !step.getExpectedLifecycleState().equals(State.CANCELED) )
				.anyMatch(step -> step.getExpectedLifecycleState().equals(State.COMPLETED) && step.getActualLifecycleState().equals(State.COMPLETED));
			setInflowFulfilled(inFlowORok);
			return inFlowORok!=prioCond;
		case XOR:
			// as soon as the first is E: complete we trigger, all others will be set to no work expected
			// thus there may be only one E:complete step as the others will be E: no work expected or E: cancelled
			// once an E:complete is cancelled however, we need to reactivate all E: no work expected and select there the first one for further mapping
			// we don't set any E: no work expected until the first branch signals completion
			List<ProcessStep> steps = getInSteps().stream()
			.filter(step -> !step.getExpectedLifecycleState().equals(State.NO_WORK_EXPECTED) )
			.filter(step -> !step.getExpectedLifecycleState().equals(State.CANCELED) )
			.filter(step -> step.getExpectedLifecycleState().equals(State.COMPLETED) && step.getActualLifecycleState().equals(State.COMPLETED) )
			.collect(Collectors.toList());
			boolean inFlowXORok = steps.size() == 1; 
			setInflowFulfilled(inFlowXORok);
			return inFlowXORok!=prioCond; 
		}
		return false;
		// check if have some delayed output that needs to be propagted
		//if (this.hasPropagated())
		//	tryActivationPropagation();
	}
	
	private List<Events.ProcessChangedEvent> selectChosenXORStep() {
		List<Events.ProcessChangedEvent> events = new LinkedList<>();
		List<ProcessStep> steps = getInSteps().stream()
				.filter(step -> !step.getExpectedLifecycleState().equals(State.NO_WORK_EXPECTED) )
				.filter(step -> !step.getExpectedLifecycleState().equals(State.CANCELED) )
				.filter(step -> step.getExpectedLifecycleState().equals(State.COMPLETED) && step.getActualLifecycleState().equals(State.COMPLETED) )
				.collect(Collectors.toList());
		// now if fulfilled, and not yet propagated: need to 'disable' other branches 
		ProcessStep chosenStep = steps.get(0);
		getInSteps().stream()
			.filter(step -> !step.getExpectedLifecycleState().equals(State.NO_WORK_EXPECTED) )
			//.filter(step -> !step.getExpectedLifecycleState().equals(State.CANCELED) ) // we also mark cancelled steps as no work expected, as if their cancel condition is lifted, the still should stay in no work expected
			.filter(step -> step != chosenStep)
			.forEach(step -> events.addAll(step.setWorkExpected(false)));

		if (this.getDefinition().getInSteps().size() > 1) {// there are other inbranches that we now need to deactivate
			List<DecisionNodeInstance> path = findCorrespondingOutFlowPath(this, chosenStep); //the path to the corresponding outflow node via the just activating task
			if (path != null) {
				path.add(0, this);
				// deactivate those not on path
				DecisionNodeInstance correspondingDNI = path.get(path.size()-1);
				DecisionNodeInstance exclNextDNI = path.size() >= 2 ? path.get(path.size()-2) : null;						
				events.addAll(deactivateTasksOn(correspondingDNI, exclNextDNI, this)); 
			}
		}			
		return events;
	}
	
	private List<Events.ProcessChangedEvent> releaseChosenXORStep(ProcessStep previouslyChosenStep) {
		List<Events.ProcessChangedEvent> events = new LinkedList<>();
		getInSteps().stream()
		.filter(step -> step.getExpectedLifecycleState().equals(State.NO_WORK_EXPECTED) )
		//.filter(step -> !step.getExpectedLifecycleState().equals(State.CANCELED) ) // we also mark cancelled steps as no work expected, as if their cancel condition is lifted, the still should stay in no work expected
		.filter(step -> step != previouslyChosenStep)
		.forEach(step -> events.addAll(step.setWorkExpected(true)));

	if (this.getDefinition().getInSteps().size() > 1) {// there are other inbranches that we now need to deactivate
		List<DecisionNodeInstance> path = findCorrespondingOutFlowPath(this, previouslyChosenStep); //the path to the corresponding outflow node via the just activating task
		if (path != null) {
			path.add(0, this);
			// deactivate those not on path
			DecisionNodeInstance correspondingDNI = path.get(path.size()-1);
			DecisionNodeInstance exclNextDNI = path.size() >= 2 ? path.get(path.size()-2) : null;						
			events.addAll(activateTasksOn(correspondingDNI, exclNextDNI, this)); 
		}
	}			
	return events;
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
	
	private static List<Events.ProcessChangedEvent> deactivateTasksOn(DecisionNodeInstance thisDNI, DecisionNodeInstance nextExcludeDNI, DecisionNodeInstance finalDNI) {
		List<Events.ProcessChangedEvent> events = new LinkedList<>();
		thisDNI.getOutSteps().stream()
			.filter(task -> (nextExcludeDNI == null) || task.getOutDNI() != nextExcludeDNI) // we filter out those on the path
			.map(task -> {
				if (!task.getExpectedLifecycleState().equals(State.NO_WORK_EXPECTED)) {					
					events.addAll(task.setWorkExpected(false));
				} // else we dont set this again, but we need to travers onwards, thus we cant filter out them 
				return task.getOutDNI();
			})
			.filter(dni -> dni != finalDNI) // we stop once we reache finalDNI
			.forEach(dni -> events.addAll(deactivateTasksOn(dni, null, finalDNI)));
		return events;
	}
	
	private static List<Events.ProcessChangedEvent> activateTasksOn(DecisionNodeInstance thisDNI, DecisionNodeInstance nextExcludeDNI, DecisionNodeInstance finalDNI) {
		List<Events.ProcessChangedEvent> events = new LinkedList<>();
		thisDNI.getOutSteps().stream()
			.filter(task -> (nextExcludeDNI == null) || task.getOutDNI() != nextExcludeDNI) // we filter out those on the path
			.map(task -> {
				if (task.getExpectedLifecycleState().equals(State.NO_WORK_EXPECTED)) {					
					events.addAll(task.setWorkExpected(true));
				} // else we dont set this again, but we need to travers onwards, thus we cant filter out them 
				return task.getOutDNI();
			})
			.filter(dni -> dni != finalDNI) // we stop once we reache finalDNI
			.forEach(dni -> events.addAll(activateTasksOn(dni, null, finalDNI)));
		return events;
	}
	
	private List<Events.ProcessChangedEvent> doActivationPropagation() {
		// if not yet progressed,
		List<Events.ProcessChangedEvent> events = new LinkedList<>();
		if (!this.hasPropagated()) {
			events.addAll(initiateDownstreamSteps(false)); //check and execute datamappings will be called below anyway
			this.setHasPropagated();
		} 
		
		boolean isEndOfProcess = false;
		if (this.getDefinition().getOutSteps().size() == 0 ) {
			// is this the final DNI?
			 isEndOfProcess = true;
		}
		events.addAll(checkAndExecuteDataMappings(isEndOfProcess, false));
		//if (isEndOfProcess)
		events.addAll(this.getProcess().signalDNIChanged(this));

		return events;
	}
	
	// only to be called internally and by process for moving data into child process
	protected List<ProcessChangedEvent> checkAndExecuteDataMappings(boolean isEndOfProcess, boolean isPremature) {
		if (mapper == null) {
			mapper = new InterStepDataMapper(this.getDefinition(), this.getProcess());			
		}
		return mapper.checkAndExecuteDataMappings(isEndOfProcess, isPremature);
	}
	
	private List<ProcessChangedEvent> initiateDownstreamSteps(boolean includeDataPropagation) {
		if (isInternalPropagationDone) return Collections.emptyList(); // to distinguish between official propagation or premature/immediate propagation
		// get all out task defs, check if they exist or create them
		// first time activation		
		List<ProcessChangedEvent> events = new LinkedList<>();
		this.getProcess().getDefinition().getStepDefinitions().stream()
		.filter(def -> def.getInDND().equals(this.getDefinition())) //retain only that have this DND as their predecessor
		.filter(td -> getProcess().getProcessSteps().stream()
				.map(ProcessStep::getDefinition)
				.noneMatch(td1 -> td1.equals(td))) // retain those that are not yet instantiated
		.forEach(td -> { ProcessStep step = getProcess().createAndWireTask(td);
					if (includeDataPropagation) {
						events.addAll(checkAndExecuteDataMappings(false, true));
					}
				});		
		isInternalPropagationDone = true;
		return events;
	}
	
	// when a downstream task is prematurely created, we want to be able to propagate existing output to it
	public List<Events.ProcessChangedEvent> doDataPropagationToPrematurelyTriggeredTask() {
		return checkAndExecuteDataMappings(false, true);
	}
	
	
	
	public void deleteCascading() {
		// remove any lower-level instances this step is managing, which is none
		// hence
		// finally delete self
		if (mapper != null) {
			mapper.delete();
			mapper = null;
		}
		super.deleteCascading();
	}

	private boolean isImmediateInstantiateAllStepsEnabled() {
		if (getProcess() == null)
			return false;
		else
			return getProcess().isImmediateInstantiateAllStepsEnabled();
	}
	
	private boolean isImmediateDataPropagationEnabled() {
		if (getProcess() == null)
			return false;
		else
			return getProcess().isImmediateDataPropagationEnabled();
	}
	
	public DecisionNodeInstance getScopeClosingDecisionNodeOrNull() {
		if (this.getOutSteps().isEmpty())
			return null;
		else {			
			Instance dnd = instance.getPropertyAsInstance(CoreProperties.closingDN.toString());
			if (dnd != null)
				return WrapperCache.getWrappedInstance(DecisionNodeInstance.class, dnd);
			else { 
				DecisionNodeInstance closingDnd = determineScopeClosingDN();
				instance.getPropertyAsSingle(CoreProperties.closingDN.toString()).set(closingDnd.getInstance());		
				return closingDnd;
			}
		}
	}
	
	private DecisionNodeInstance determineScopeClosingDN() {
//		List<Step> nextSteps = getOutStepsOf(dn);
//		if (nextSteps.isEmpty()) return null; // end of the process, closing DN reached
		Set<DecisionNodeInstance> nextStepOutDNs = this.getOutSteps().stream().map(step -> step.getOutDNI()).collect(Collectors.toSet());
		// size must be 1 or greater as we dont allow steps without subsequent DN
		if (nextStepOutDNs.size() == 1) { // implies the scope closing DN as otherwise there need to be multiple opening subscope ones
			return nextStepOutDNs.iterator().next();
		} else {
			Set<DecisionNodeInstance> sameDepthNodes = new HashSet<>();
			while (sameDepthNodes.size() != 1) {
				sameDepthNodes = nextStepOutDNs.stream().filter(nextDN -> nextDN.getDefinition().getDepthIndex() == this.getDefinition().getDepthIndex()).collect(Collectors.toSet());
				assert(sameDepthNodes.size() <= 1); //closing next nodes can only be on same level or deeper (i.e., larger values)
				if (sameDepthNodes.size() != 1) {
					Set<DecisionNodeInstance> nextNextStepOutDNs = nextStepOutDNs.stream().map(nextDN -> nextDN.getScopeClosingDecisionNodeOrNull()).collect(Collectors.toSet());
					nextStepOutDNs = nextNextStepOutDNs;
				}
				assert(nextStepOutDNs.size() > 0);
			} 
			return sameDepthNodes.iterator().next();				
		}
	}
	
	public static InstanceType getOrCreateDesignSpaceCoreSchema(Workspace ws) {
		Optional<InstanceType> thisTypeOpt = Optional.ofNullable(ws.TYPES_FOLDER.instanceTypeWithName(designspaceTypeId)); 
//		Optional<InstanceType> thisType = ws.debugInstanceTypes().stream()
//			.filter(it -> !it.isDeleted)
//			.filter(it -> it.name().equals(designspaceTypeId))
//			.findAny();
		if (thisTypeOpt.isPresent())
			return thisTypeOpt.get();
		else {
			InstanceType thisType = ws.createInstanceType(designspaceTypeId, ws.TYPES_FOLDER, ProcessInstanceScopedElement.getOrCreateDesignSpaceCoreSchema(ws));
			ProcessInstanceScopedElement.addGenericProcessProperty(thisType);
			thisType.createPropertyType(CoreProperties.isInflowFulfilled.toString(), Cardinality.SINGLE, Workspace.BOOLEAN);
			thisType.createPropertyType(CoreProperties.hasPropagated.toString(), Cardinality.SINGLE, Workspace.BOOLEAN);
			thisType.createPropertyType(CoreProperties.dnd.toString(), Cardinality.SINGLE, DecisionNodeDefinition.getOrCreateDesignSpaceCoreSchema(ws));
			thisType.createPropertyType(CoreProperties.inSteps.toString(), Cardinality.SET, ProcessStep.getOrCreateDesignSpaceCoreSchema(ws));
			thisType.createPropertyType(CoreProperties.outSteps.toString(), Cardinality.SET, ProcessStep.getOrCreateDesignSpaceCoreSchema(ws));
			thisType.createPropertyType(CoreProperties.closingDN.toString(), Cardinality.SINGLE, thisType);
			return thisType;
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
		instance.getPropertyAsSingle(CoreProperties.isInflowFulfilled.toString()).set(/*dnd.getInSteps().size() == 0 ? true :*/ false);
	
	}

	@Override
	public String toString() {
		return "DecisionNodeInstance [" + getDefinition().getName() + ", isInflowFulfilled()="
				+ isInflowFulfilled() + ", hasPropagated()=" + hasPropagated() + "]";
	}
	
	public static final DecisionNodeComparatorByDefinitionOrder comparator = new DecisionNodeComparatorByDefinitionOrder();
	
	private static class DecisionNodeComparatorByDefinitionOrder implements Comparator<DecisionNodeInstance> {
		@Override
		public int compare(DecisionNodeInstance o1, DecisionNodeInstance o2) {
			return o1.getDefinition().getDepthIndex().compareTo(o2.getDefinition().getDepthIndex());
		}
		
	}
}
