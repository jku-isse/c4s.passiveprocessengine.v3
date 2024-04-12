package at.jku.isse.passiveprocessengine.instance;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.github.oxo42.stateless4j.StateMachine;

import at.jku.isse.designspace.core.events.PropertyUpdateAdd;
import at.jku.isse.designspace.core.events.PropertyUpdateRemove;
import at.jku.isse.designspace.core.events.PropertyUpdateSet;
import at.jku.isse.designspace.core.model.Cardinality;
import at.jku.isse.designspace.core.model.Element;
import at.jku.isse.designspace.core.model.Id;
import at.jku.isse.designspace.core.model.Instance;
import at.jku.isse.designspace.core.model.InstanceType;
import at.jku.isse.designspace.core.model.Property;
import at.jku.isse.designspace.core.model.SetProperty;
import at.jku.isse.designspace.core.model.SingleProperty;
import at.jku.isse.designspace.core.model.Workspace;
import at.jku.isse.designspace.rule.model.ConsistencyRule;
import at.jku.isse.designspace.rule.model.ConsistencyRuleType;
import at.jku.isse.passiveprocessengine.ProcessInstanceScopedElement;
import at.jku.isse.passiveprocessengine.WrapperCache;
import at.jku.isse.passiveprocessengine.analysis.RuleAugmentation;
import at.jku.isse.passiveprocessengine.definition.ConstraintSpec;
import at.jku.isse.passiveprocessengine.definition.ProcessDefinition;
import at.jku.isse.passiveprocessengine.definition.StepDefinition;
import at.jku.isse.passiveprocessengine.instance.StepLifecycle.Conditions;
import at.jku.isse.passiveprocessengine.instance.StepLifecycle.State;
import at.jku.isse.passiveprocessengine.instance.StepLifecycle.Trigger;
import at.jku.isse.passiveprocessengine.instance.messages.Commands.ConditionChangedCmd;
import at.jku.isse.passiveprocessengine.instance.messages.Commands.IOMappingConsistencyCmd;
import at.jku.isse.passiveprocessengine.instance.messages.Commands.OutputChangedCmd;
import at.jku.isse.passiveprocessengine.instance.messages.Commands.ProcessScopedCmd;
import at.jku.isse.passiveprocessengine.instance.messages.Commands.QAConstraintChangedCmd;
import at.jku.isse.passiveprocessengine.instance.messages.Events;
import at.jku.isse.passiveprocessengine.instance.messages.Events.ProcessChangedEvent;
import at.jku.isse.passiveprocessengine.instance.messages.Responses;
import at.jku.isse.passiveprocessengine.instance.messages.Responses.IOResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ProcessStep extends ProcessInstanceScopedElement{

	public static final String PREFIX_OUT = "out_";

	public static final String PREFIX_IN = "in_";

	private static final String CRD_QASPEC_PREFIX = "crd_qaspec_";

	public static final String CRD_DATAMAPPING_PREFIX = "crd_datamapping_";


	public static enum CoreProperties {actualLifecycleState, expectedLifecycleState, stepDefinition, inDNI, outDNI, qaState, 
		preconditions, postconditions, cancelconditions, activationconditions,
		processedPreCondFulfilled, processedPostCondFulfilled, processedCancelCondFulfilled, processedActivationCondFulfilled, isWorkExpected};
	
	public static final String designspaceTypeId = ProcessStep.class.getSimpleName();
	
	protected transient StateMachine<StepLifecycle.State, StepLifecycle.Trigger> actualSM;
	protected transient StateMachine<StepLifecycle.State, StepLifecycle.Trigger> expectedSM;
	
	public ProcessStep(Instance instance) {
		super(instance);
		initState();
	}
	
	protected transient boolean priorQAfulfilled = false;
	
	private void initState() {
		if (this.getName().startsWith(StepDefinition.NOOPSTEP_PREFIX)) { // assumes/expects no pre/post cond and no qa			
			actualSM = StepLifecycle.buildActualStatemachineInState(State.COMPLETED);
			instance.getPropertyAsSingle(CoreProperties.actualLifecycleState.toString()).set(actualSM.getState().toString());
			expectedSM = StepLifecycle.buildExpectedStatemachineInState(State.COMPLETED);
			instance.getPropertyAsSingle(CoreProperties.expectedLifecycleState.toString()).set(expectedSM.getState().toString());
			priorQAfulfilled = true;
		} else {
		
			String actState = (String) instance.getPropertyAsValueOrNull(CoreProperties.actualLifecycleState.toString());
			if (actState == null) {
				actualSM = StepLifecycle.buildActualStatemachineInState(State.AVAILABLE);
				instance.getPropertyAsSingle(CoreProperties.actualLifecycleState.toString()).set(actualSM.getState().toString());
			} else { // state already set, now just init FSM
				actualSM = StepLifecycle.buildActualStatemachineInState(State.valueOf(actState));
			}

			String expState = (String) instance.getPropertyAsValueOrNull(CoreProperties.expectedLifecycleState.toString());
			if (expState == null) {
				expectedSM = StepLifecycle.buildExpectedStatemachineInState(State.AVAILABLE);
				instance.getPropertyAsSingle(CoreProperties.expectedLifecycleState.toString()).set(expectedSM.getState().toString());
			} else { // state already set, now just init FSM
				expectedSM = StepLifecycle.buildExpectedStatemachineInState(State.valueOf(expState));
			}
			priorQAfulfilled = areConstraintsFulfilled(CoreProperties.qaState.toString());
		}
	}
	
	public ProcessScopedCmd prepareRuleEvaluationChange(ConsistencyRule cr, PropertyUpdateSet op) {
		// now here we have to distinguish what this evaluation change implies
		ConsistencyRuleType crt = (ConsistencyRuleType)cr.getInstanceType();
		Conditions cond = determineCondition(crt);
		if (cond != null ) {
			String value = op.value() != null ? op.value().toString() : "NULL";
			log.debug(String.format("Step %s has %s evaluate to %s in transaction %s ", this.getName(), cond, value, op.getConclusionId()));
			//FIXME: check if multiconstraints still need this
			//SingleProperty prop = instance.getPropertyAsSingle(cond.toString());
			//if (prop.get() == null) 
			//	prop.set(cr);
			return new ConditionChangedCmd(this, cr, cond, Boolean.valueOf(op.value().toString()));
		} else {
		// if premature conditions, then delegate to process instance, resp often will need to be on process level anyway
		
			// input to putput mappings
			if (crt.name().startsWith(CRD_DATAMAPPING_PREFIX) ) { 
				if (Boolean.valueOf(op.value().toString()) == false) { // an unfulfilled datamapping rules
				// now we need to "repair" this, i.e., set the output accordingly
					log.debug(String.format("Datamapping %s queued for repair", crt.name()));
					return new IOMappingConsistencyCmd(this, cr, true);
				} else {
					log.debug(String.format("Datamapping %s now consistent", crt.name()));
					return new IOMappingConsistencyCmd(this, cr, false);
				}
			} else if (crt.name().startsWith(CRD_QASPEC_PREFIX) ) { // a qa constraint
				log.debug(String.format("QA Constraint %s now %s ", crt.name(), op.value() != null ? op.value().toString() : "NULL"));
				//processQAEvent(cr, op); Boolean.parseBoolean(op.value().toString())
				return op.value() != null ? new QAConstraintChangedCmd(this, cr, Boolean.parseBoolean(op.value().toString())) : 
					new QAConstraintChangedCmd(this, cr, true);			
			}	else
				log.debug(String.format("Step %s has rule %s evaluate to %s", this.getName(), crt.name(), op.value().toString()));
		}
		return null;
	}
	
	private Conditions determineCondition(ConsistencyRuleType crt) {
		 //FIXME better matching needed
		if (crt.name().startsWith("crd_"+Conditions.PRECONDITION.toString())) 
			return Conditions.PRECONDITION;
		else if (crt.name().startsWith("crd_"+Conditions.POSTCONDITION.toString())) 
			return Conditions.POSTCONDITION;
		else if (crt.name().startsWith("crd_"+Conditions.ACTIVATION.toString())) 
			return Conditions.ACTIVATION;
		else if (crt.name().startsWith("crd_"+Conditions.CANCELATION.toString())) 
			return Conditions.CANCELATION;
		else {
			if (!crt.name().startsWith(CRD_DATAMAPPING_PREFIX) && !crt.name().startsWith(CRD_QASPEC_PREFIX))
					log.error("Unknown consistency rule: "+crt.name());
			return null;
		}
	}
	
	
	public ProcessScopedCmd prepareIOAddEvent(PropertyUpdateAdd op) { //List<Events.ProcessChangedEvent>
		// if in added, establish if this resembles unexpected late input 
		if (op.name().startsWith(PREFIX_IN) 
				&& ( this.getActualLifecycleState().equals(State.ACTIVE) 
					|| this.getActualLifecycleState().equals(State.COMPLETED) )) {
			//(if so, then do something about this)
			Id addedId = (Id) op.value();
			Element added = ws.findElement(addedId);
			log.info(String.format("Step %s received unexpected late input %s %s", this.getName(), op.name(), added.name()  ));
			// Note that the adding has already happened, thus there is nothing to report back, this is only for checking whether we need to do something else as well.
		}
		else if (op.name().startsWith(PREFIX_OUT)) { // if out added, establish if this is late output, then propagate further
				//&& ( this.getActualLifecycleState().equals(State.COMPLETED) || isImmediateDataPropagationEnabled() ) ){
			if (this.getActualLifecycleState().equals(State.COMPLETED)) {
				Id addedId = (Id) op.value();
				Element added = ws.findElement(addedId);
				log.info(String.format("Step %s received unexpected late output %s %s, queuing for propagation to successors", this.getName(), op.name(), added.name()  ));
			}
				// we should not just propagate, as the newly added output could be violating completion or qa constraints and we should not propagate the artifact just yet. -->
			// return a potential propagation cause Command, that is later checked again, whether it is still valid.
			if (getOutDNI() != null) { // to avoid NPE in case this is a ProcessInstance 
				return new OutputChangedCmd(this, op);
				//return getOutDNI().signalPrevTaskDataChanged(this);
			}
		}
		return null; //Collections.emptyList();
	}
	
	public ProcessScopedCmd prepareIORemoveEvent(PropertyUpdateRemove op) { //List<Events.ProcessChangedEvent>
		// if in removed, establish if this resembles unexpected late removeal 
		if (op.name().startsWith(PREFIX_IN) 
				&& ( this.getActualLifecycleState().equals(State.ACTIVE) 
					|| this.getActualLifecycleState().equals(State.COMPLETED) )) {
			//(if so, then do something about this)
			log.info(String.format("Step %s had some input removed from %s after step start", this.getName(), op.name()));
		}
		else if (op.name().startsWith(PREFIX_OUT)) { // if out removed, establish if this is late output removal, then propagate further
				//&& ( this.getActualLifecycleState().equals(State.COMPLETED) || isImmediateDataPropagationEnabled() ) ){
			
			if (this.getActualLifecycleState().equals(State.COMPLETED)) {
				log.debug(String.format("Step %s had some output removed from %s after step completion, queuing for propagation to successors", this.getName(), op.name()));
			}
			// we should not just propagate, as the newly added output could be violating completion or qa constraints and we should not propagate the artifact just yet. -->
			// return a potential propagation cause Command, that is later checked again, whether it is still valid.
			if (getOutDNI() != null) { // to avoid NPE in case this is a ProcessInstance
				return new OutputChangedCmd(this, op);
				//return getOutDNI().signalPrevTaskDataChanged(this);
			}
		}
		//else 
		//	log.debug(String.format("Step %s had some output removed from %s, not propagating to successors yet", this.getName(), op.name()));
		return null; //Collections.emptyList();
	}
	
	
	public List<Events.ProcessChangedEvent> processQAEvent(ConsistencyRule cr, boolean fulfilled) {
		String id = cr.name();		
		//ConstraintWrapper cw = qaState.get(id);
		//in one occasion found null instance in map, which should not happen!!!
		ConstraintWrapper cw = WrapperCache.getWrappedInstance(ConstraintWrapper.class, (Instance) instance.getPropertyAsMap(CoreProperties.qaState.toString()).get(id));
		cw.setCrIfEmpty(cr);
		//cw.setEvalResult(fulfilled);
		cw.setLastChanged(getParentProcessOrThisIfProcessElseNull().getCurrentTimestamp());
		List<Events.ProcessChangedEvent> qaChanges = new LinkedList<>();
		qaChanges.add(new Events.QAConstraintFulfillmentChanged(getParentProcessOrThisIfProcessElseNull(), this, cw));
		boolean newQaState = areConstraintsFulfilled(CoreProperties.qaState.toString()); // are all QA checks now fulfilled?
		if (priorQAfulfilled != newQaState) { // a change in qa fulfillment that we might want to react to
			priorQAfulfilled = newQaState;
			qaChanges.add(new Events.QAFulfillmentChanged(getParentProcessOrThisIfProcessElseNull(), this, newQaState));
			if (arePostCondFulfilled() && newQaState)  {
				qaChanges.addAll(this.trigger(StepLifecycle.Trigger.MARK_COMPLETE)) ;
			} else if (!newQaState && actualSM.isInState(State.COMPLETED)) {
				qaChanges.addAll(this.trigger(StepLifecycle.Trigger.ACTIVATE));
			}
		}
		return qaChanges;
	}
	
	public List<Events.ProcessChangedEvent> processOutputChangedCmd(String outputName) {
		List<Events.ProcessChangedEvent> events = new LinkedList<>();
		// whenever there is output added or removed, it means someone was active regardless of otherstate (except for COMPLETED)
		if (!getActualLifecycleState().equals(State.ACTIVE) && !getActualLifecycleState().equals(State.COMPLETED))
			events.addAll(setActivationConditionsFulfilled(true));
		// now lets take care of datapropagation
		if (getProcess() != null) {
			// there might be instances used in steps further down (not just the outDNI, thus we also need to trigger their inDNIs as otherwise there wont be any instances added/removed)
			// we find all DNIs that have this as input //TODO: are there any DNIs that should not be triggered? perhaps if two exclusive steps deliver the same output, both are active and have output mapped, then triggering might result in unpredictable mappings 
			Set<DecisionNodeInstance> downstreamDNIs = getProcess().getInstantiatedDNIsHavingStepsOutputAsInput(this, outputName);
			downstreamDNIs.stream()
				.forEach(dni -> events.addAll(dni.signalPrevTaskDataChanged(this)));						
		} 
		return events;
	}
	
	public Set<ConstraintWrapper> getQAstatus() {
		return (Set<ConstraintWrapper>) instance.getPropertyAsMap(CoreProperties.qaState.toString()).values().stream()
		.map(inst -> WrapperCache.getWrappedInstance(ConstraintWrapper.class, (Instance) inst))
		.collect(Collectors.toSet());
		//return qaState.values().parallelStream().collect(Collectors.toSet());
	}
	

	
	public boolean arePostCondFulfilled() {
		return (boolean) instance.getPropertyAsValue(CoreProperties.processedPostCondFulfilled.toString());
	}
	
	public boolean arePreCondFulfilled() {
		return (boolean) instance.getPropertyAsValue(CoreProperties.processedPreCondFulfilled.toString());
	}
	
	public boolean areCancelCondFulfilled() {
		return (boolean) instance.getPropertyAsValue(CoreProperties.processedCancelCondFulfilled.toString());
	}
	
	public boolean areActivationCondFulfilled() {
		return (boolean) instance.getPropertyAsValue(CoreProperties.processedActivationCondFulfilled.toString());
	}
	
	public boolean isWorkExpected() {
		return (boolean) instance.getPropertyAsValue(CoreProperties.isWorkExpected.toString());
	}
	

	
	public List<ProcessStep> isInUnsafeOperationModeDueTo() {
		List<ProcessStep> unsafeSteps = new LinkedList<>();
		// returns the list of closest/nearest process steps that are not QA complete prior to it that makes this unsafe to work (i.e., might require rework later on)
		if (this.getInDNI() != null) {
			if (this.getInDNI().hasPropagated()) {
				// when propagated: which of the insteps used for propagation are not QA complete, and if so, perhaps still unsafe from upstream 
				// note that these steps might also no longer be in state complete hence differentiate between inflow fulfilled or not
				if (this.getInDNI().isInflowFulfilled()) { // necessary number of steps are complete, i.e., AND: all, OR: at least one, XOR, exactly one
					//filter for those that are actually and expectedly complete
					this.getInDNI().getInSteps().stream()
					.filter(step -> step.getExpectedLifecycleState().equals(State.COMPLETED) && step.getActualLifecycleState().equals(State.COMPLETED))
					.forEach(step -> {
						if (step.areConstraintsFulfilled(CoreProperties.qaState.toString())) // the have to be as otherwise we would have inflow fulfilled/resp exp and actual State Completed?!
							unsafeSteps.addAll(step.isInUnsafeOperationModeDueTo());
						else 
							unsafeSteps.add(step);
					});
					
				} else { // not sufficient steps available: AND: not all, OR, none, XOR, none, hence here check all steps as all of them can/must be fulfilled eventually
					this.getInDNI().getInSteps().stream().forEach(step -> {
						if (step.areConstraintsFulfilled(CoreProperties.qaState.toString())) 
							unsafeSteps.addAll(step.isInUnsafeOperationModeDueTo());
						else 
							unsafeSteps.add(step);
					});
				}
			} else {
				// when not propagated --> hence why might this have happened, get all insteps and check their QA constraints, if fine, ask them for isInUsafeOperation
				this.getInDNI().getInSteps().stream().forEach(step -> {
					if (step.areConstraintsFulfilled(CoreProperties.qaState.toString())) 
						unsafeSteps.addAll(step.isInUnsafeOperationModeDueTo());
					else 
						unsafeSteps.add(step);
				});
			}
		}
		return unsafeSteps;
	}
	
	public List<ProcessStep> isInPrematureOperationModeDueTo() {
		// returns the list of closest/nearest process steps that are not complete prior to it that makes this premature to work (i.e., might require rework later on)
		List<ProcessStep> premSteps = new LinkedList<>();
		if (this.getInDNI() != null) {
			//if (!this.getInDNI().hasPropagated() || !this.getInDNI().isInflowFulfilled()) {
				// when propagated but not fulfilled: which of the insteps used for propagation are not complete, or if not, are perhaps still premature from upstream 
				// when not propagated --> hence why might this have happened, get all insteps and check their postcon constraints, if fine, ask them for isInPrematureOperation
				this.getInDNI().getInSteps().stream().forEach(step -> {
					if (step.arePostCondFulfilled()) 
						premSteps.addAll(step.isInPrematureOperationModeDueTo());
					else 
						premSteps.add(step);
				});
			//}
		}
		return premSteps;
	}
	
	public void deleteCascading() {
		// remove any lower-level instances this step is managing
		// DNIs are deleted at process level, not managed here
		//qaState.values().forEach(cw -> cw.deleteCascading());
		instance.getPropertyAsMap(CoreProperties.qaState.toString()).values().stream()
		.map(inst -> WrapperCache.getWrappedInstance(ConstraintWrapper.class, (Instance) inst))
		.forEach(cw -> ((ConstraintWrapper)cw).deleteCascading());
		//FIXME: remove multiconstraints
		
		// we are not deleting input and output artifacts as we are just referencing them!
		// finally delete self
		super.deleteCascading();
	}
	
	public StepDefinition getDefinition() {
		return  WrapperCache.getWrappedInstance(StepDefinition.class, instance.getPropertyAsInstance(CoreProperties.stepDefinition.toString()));
		
	}
	
	protected Responses.IOResponse removeInput(String inParam, Instance artifact) {
		if (getDefinition().getExpectedInput().containsKey(inParam)) {
			Property<?> prop = instance.getProperty(PREFIX_IN+inParam);
			if (prop.propertyType.isAssignable(artifact)) {
				instance.getPropertyAsSet(PREFIX_IN+inParam).remove(artifact);
				return IOResponse.okResponse();
			} else {
				String msg = String.format("Cannot remove input %s to %s with nonmatching artifact type %s of id % %s", inParam, this.getName(), artifact.getInstanceType().toString(), artifact.id(), artifact.name());
				log.warn(msg);
				return IOResponse.errorResponse(msg);
			}
		} else {
			// additionally Somehow notify about wrong param access
			String msg = String.format("Ignoring attempt to remove unexpected input %s to %s", inParam, this.getName());
			log.warn(msg);
			return IOResponse.errorResponse(msg);
		}
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Set<Instance> getInput(String param) {
		SetProperty setP = instance.getPropertyAsSet(PREFIX_IN+param);
		if (setP == null) {
			//if (!instance.hasProperty("in_"+param))
			log.error(String.format("Attempt to access non-existing input %s in Step %s.", param, this.getName()));
			return Collections.emptySet();
		}
		return (Set<Instance>) setP.get();
	}
	
	@SuppressWarnings("unchecked")
	public Responses.IOResponse addInput(String inParam, Instance artifact) {
		if (getDefinition().getExpectedInput().containsKey(inParam)) {
			Property<?> prop = instance.getProperty(PREFIX_IN+inParam);
			if (prop.propertyType.isAssignable(artifact)) {
				instance.getPropertyAsSet(PREFIX_IN+inParam).add(artifact);
				return IOResponse.okResponse();
			} else {
				String msg = String.format("Cannot add input %s to %s with nonmatching artifact type %s of id %s %s", inParam, this.getName(), artifact.getInstanceType().toString(), artifact.id(), artifact.name());
				log.warn(msg);
				return IOResponse.errorResponse(msg);
			}
		} else {
			String msg = String.format("Ignoring attempt to add unexpected input %s to %s", inParam, this.getName());
			log.warn(msg);
			return IOResponse.errorResponse(msg);
		}
	}
	
	@SuppressWarnings("unchecked")
	public Set<Instance> getOutput(String param) {
		SetProperty setP = instance.getPropertyAsSet(PREFIX_OUT+param);
		if (setP == null) {
			//if (!instance.hasProperty("in_"+param))
			log.error(String.format("Attempt to access non-existing output %s in Step %s.", param, this.getName()));
			return Collections.emptySet();
		}
		return (Set<Instance>) setP.get();
	}
	
	@SuppressWarnings("unchecked")
	public Responses.IOResponse addOutput(String param, Instance artifact) {
		if (getDefinition().getExpectedOutput().containsKey(param)) {
			Property<?> prop = instance.getProperty(PREFIX_OUT+param);
			if (prop.propertyType.isAssignable(artifact)) {
				instance.getPropertyAsSet(PREFIX_OUT+param).add(artifact);
				return IOResponse.okResponse();
			} else {
				String msg = String.format("Cannot add outnput %s to %s with nonmatching artifact type %s of id % %s", param, this.getName(), artifact.getInstanceType().toString(), artifact.id(), artifact.name());
				log.warn(msg);
				return IOResponse.errorResponse(msg);
			}
		} else {
			String msg = String.format("Ignoring attempt to add unexpected output %s to %s", param, this.getName());
			log.warn(msg);
			return IOResponse.errorResponse(msg);
		}
	}
	
	protected void removeOutput(String param, Instance art) {
		instance.getPropertyAsSet(PREFIX_OUT+param).remove(art);
	}
	
	public DecisionNodeInstance getInDNI() {
		return WrapperCache.getWrappedInstance(DecisionNodeInstance.class, instance.getPropertyAsInstance(CoreProperties.inDNI.toString()));
	}
	
	public DecisionNodeInstance getOutDNI() {
		return WrapperCache.getWrappedInstance(DecisionNodeInstance.class, instance.getPropertyAsInstance(CoreProperties.outDNI.toString()));
	}
	
	public State getExpectedLifecycleState() {
		return expectedSM.getState();
	}
	
	public State getActualLifecycleState() {
		return actualSM.getState();
	}
	
	public boolean areConstraintsFulfilled(String constraintProperty) {
		// are there all constraint wrappers actually added already
//		if (this.getDefinition() == null)
//			return false;		
//		int expQA = this.getDefinition().getQAConstraints().size(); //no longer needed as we create skeletons upon init
//		int actualQA = instance.getPropertyAsMap(constraintProperty).values().size();
//		if (expQA != actualQA) 
//			return false; // as long as the expected QA is not the actual number of QA checks, the eval cant be true;		
		return instance.getPropertyAsMap(constraintProperty).values().stream()
			.map(inst -> WrapperCache.getWrappedInstance(ConstraintWrapper.class, (Instance) inst))
			.allMatch(cw -> ((ConstraintWrapper)cw).getEvalResult()==true);		
	}
	
	@SuppressWarnings("unchecked")
	public Set<ConstraintWrapper> getConstraints(String constraintProperty) {
		return (Set<ConstraintWrapper>) instance.getPropertyAsMap(constraintProperty).values().stream()
				.map(inst -> WrapperCache.getWrappedInstance(ConstraintWrapper.class, (Instance) inst))
				.filter(Objects::nonNull)
				.map(ConstraintWrapper.class::cast)
				.collect(Collectors.toSet());
	}
	
	public List<Events.ProcessChangedEvent> setWorkExpected(boolean isExpected) {
		if (isWorkExpected() != isExpected ) {
			instance.getPropertyAsSingle(CoreProperties.isWorkExpected.toString()).set(isExpected);	
				if (!isExpected) {
					return trigger(Trigger.HALT);
				}
				else {
					return trigger(Trigger.UNHALT);														
				}
			}
		else return Collections.emptyList();
	}
	
	protected ProcessInstance getParentProcessOrThisIfProcessElseNull() {
		return this.getProcess() != null ? this.getProcess() : (ProcessInstance)this; //ugly hack if this is a process without parent
	}
	
	public List<Events.ProcessChangedEvent> processConditionsChanged(ConstraintWrapper cw) {
		List<ProcessChangedEvent> events;
		boolean newResult = cw.getEvalResult();
		switch(cw.getSpec().getConditionType()) {
		case ACTIVATION:
			events = processActivationConditionsChange(cw.getCr(), newResult);
			break;
		case CANCELATION:
			events = processCancelConditionsChange(cw.getCr(), newResult);
			break;						
		case POSTCONDITION:
			events = processPostConditionsChange(cw.getCr(), newResult);
			break;
		case PRECONDITION:
			events = processPreConditionsChange(cw.getCr(), newResult);
			break;
		case QA:
			events = processQAEvent(cw.getCr(), newResult);
			break;
		case DATAMAPPING: //fallthrough							
		default:
			// not supported for setting directly
			events = Collections.emptyList();
			break;						        					
		}
		return events;
	}
	
	public List<Events.ProcessChangedEvent> processPostConditionsChange(ConsistencyRule cr, boolean isfulfilled) {
		String id = cr.name();		
		ConstraintWrapper cw = WrapperCache.getWrappedInstance(ConstraintWrapper.class, (Instance) instance.getPropertyAsMap(CoreProperties.postconditions.toString()).get(id));
		cw.setCrIfEmpty(cr);		
		cw.setLastChanged(getParentProcessOrThisIfProcessElseNull().getCurrentTimestamp());			
		boolean newState = areConstraintsFulfilled(CoreProperties.postconditions.toString()); 
		List<Events.ProcessChangedEvent> events =  setPostConditionsFulfilled(newState);
		if (events.isEmpty())
			return List.of(new Events.PartialConditionFulfillmentChanged(getParentProcessOrThisIfProcessElseNull(), this, Conditions.POSTCONDITION, isfulfilled, cr.name()));
		else
			return events;	
	}
	
	protected List<Events.ProcessChangedEvent> setPostConditionsFulfilled(boolean isfulfilled) {						
		if (arePostCondFulfilled() != isfulfilled) { // a change
			List<Events.ProcessChangedEvent> events = new LinkedList<>();
			ProcessInstance pi = getParentProcessOrThisIfProcessElseNull(); 
			events.add(new Events.ConditionFulfillmentChanged(pi, this, Conditions.POSTCONDITION, isfulfilled));
			instance.getPropertyAsSingle(CoreProperties.processedPostCondFulfilled.toString()).set(isfulfilled);
			if (isfulfilled && areConstraintsFulfilled(CoreProperties.qaState.toString()) && arePreCondFulfilled())  
				events.addAll(this.trigger(StepLifecycle.Trigger.MARK_COMPLETE)) ;
			if (!isfulfilled && actualSM.isInState(State.COMPLETED)) // in canceled and nowork expected we dont care if no longer post conditions fulfilled, this is not a deviation, 
																	//the deviation might have occurred earlier if we no longer expected work for something aleady completed, but no further deviation happening now
				if (areActivationCondFulfilled())
					events.addAll(this.trigger(StepLifecycle.Trigger.ACTIVATE));
				else if (arePreCondFulfilled())
					events.addAll(this.trigger(StepLifecycle.Trigger.ENABLE));
				else 
					events.addAll(this.trigger(StepLifecycle.Trigger.RESET));
			return events;
		}
		return Collections.emptyList();
	}

	
	public List<Events.ProcessChangedEvent> processPreConditionsChange(ConsistencyRule cr, boolean isfulfilled) {		
		String id = cr.name();		
		ConstraintWrapper cw = WrapperCache.getWrappedInstance(ConstraintWrapper.class, (Instance) instance.getPropertyAsMap(CoreProperties.preconditions.toString()).get(id));
		cw.setCrIfEmpty(cr);		
		cw.setLastChanged(getParentProcessOrThisIfProcessElseNull().getCurrentTimestamp());			
		boolean newState = areConstraintsFulfilled(CoreProperties.preconditions.toString()); 
		List<Events.ProcessChangedEvent> events =  setPreConditionsFulfilled(newState);
		if (events.isEmpty())
			return List.of(new Events.PartialConditionFulfillmentChanged(getParentProcessOrThisIfProcessElseNull(), this, Conditions.PRECONDITION, isfulfilled, cr.name()));
		else
			return events;
	}
	
	protected List<Events.ProcessChangedEvent> setPreConditionsFulfilled(boolean isfulfilled) {
		if (arePreCondFulfilled() != isfulfilled) {  // a change
			List<Events.ProcessChangedEvent> events = new LinkedList<>();
			ProcessInstance pi = getParentProcessOrThisIfProcessElseNull();
			events.add(new Events.ConditionFulfillmentChanged(pi, this, Conditions.PRECONDITION, isfulfilled));
			instance.getPropertyAsSingle(CoreProperties.processedPreCondFulfilled.toString()).set(isfulfilled);
			if (isfulfilled)  {
				events.addAll(this.trigger(StepLifecycle.Trigger.ENABLE)) ;
				if (arePostCondFulfilled() && areConstraintsFulfilled(CoreProperties.qaState.toString()))
					events.addAll(this.trigger(StepLifecycle.Trigger.MARK_COMPLETE)) ;
				else if (areActivationCondFulfilled())
					events.addAll(this.trigger(StepLifecycle.Trigger.ACTIVATE)) ;
			}
			else {
				//if (!actualSM.isInState(State.CANCELED)) // no need to check any longer as CANCELED state only reacts to uncancel triggers
				events.addAll(this.trigger(StepLifecycle.Trigger.RESET));
				// we stay in cancelled even if there are preconditions no longer fulfilled,
				// if we are no longer cancelled, and precond do not hold, then reset
			}
			return events;
		}
		return Collections.emptyList();
	}
	
	public List<Events.ProcessChangedEvent> processCancelConditionsChange(ConsistencyRule cr, boolean isfulfilled) {		
		String id = cr.name();		
		ConstraintWrapper cw = WrapperCache.getWrappedInstance(ConstraintWrapper.class, (Instance) instance.getPropertyAsMap(CoreProperties.cancelconditions.toString()).get(id));
		cw.setCrIfEmpty(cr);		
		cw.setLastChanged(getParentProcessOrThisIfProcessElseNull().getCurrentTimestamp());			
		boolean newState = areConstraintsFulfilled(CoreProperties.cancelconditions.toString()); 
		List<Events.ProcessChangedEvent> events =  setCancelConditionsFulfilled(newState);
		if (events.isEmpty())
			return List.of(new Events.PartialConditionFulfillmentChanged(getParentProcessOrThisIfProcessElseNull(), this, Conditions.CANCELATION, isfulfilled, cr.name()));
		else
			return events;
	}
	
	
	protected List<Events.ProcessChangedEvent> setCancelConditionsFulfilled(boolean isfulfilled) {
		if (areCancelCondFulfilled() != isfulfilled) {
			List<Events.ProcessChangedEvent> events = new LinkedList<>();
			ProcessInstance pi = getParentProcessOrThisIfProcessElseNull();
			events.add(new Events.ConditionFulfillmentChanged(pi, this, Conditions.CANCELATION, isfulfilled));
			instance.getPropertyAsSingle(CoreProperties.processedCancelCondFulfilled.toString()).set(isfulfilled);
			if (isfulfilled) 
					events.addAll(trigger(Trigger.CANCEL));
			else { 
				events.addAll(trigger(Trigger.UNCANCEL));
			}
			return events;
		}
		return Collections.emptyList();
	}
	
	public List<Events.ProcessChangedEvent> processActivationConditionsChange(ConsistencyRule cr, boolean isFulfilled) {
		String id = cr.name();		
		ConstraintWrapper cw = WrapperCache.getWrappedInstance(ConstraintWrapper.class, (Instance) instance.getPropertyAsMap(CoreProperties.activationconditions.toString()).get(id));
		cw.setCrIfEmpty(cr);		
		cw.setLastChanged(getParentProcessOrThisIfProcessElseNull().getCurrentTimestamp());			
		boolean newState = areConstraintsFulfilled(CoreProperties.activationconditions.toString()); 
		List<Events.ProcessChangedEvent> events =  setActivationConditionsFulfilled(newState);
		if (events.isEmpty())
			return List.of(new Events.PartialConditionFulfillmentChanged(getParentProcessOrThisIfProcessElseNull(), this, Conditions.ACTIVATION, isFulfilled, cr.name()));
		else
			return events;
	}
	
	public List<Events.ProcessChangedEvent> setActivationConditionsFulfilled( boolean isFulfilled) {
		if (areActivationCondFulfilled() != isFulfilled) {			
			List<Events.ProcessChangedEvent> events = new LinkedList<>();
			ProcessInstance pi = getParentProcessOrThisIfProcessElseNull();
			events.add(new Events.ConditionFulfillmentChanged(pi, this, Conditions.ACTIVATION, isFulfilled));		
			if (isFulfilled) {				
				events.addAll(trigger(Trigger.ACTIVATE));
			} else {		
				// TODO decide what exactly to do here.
//				if (arePostCondFulfilled() && are)
//				events.addAll(trigger(Trigger.ACTIVATE));
			}
			return events;
		}
		return Collections.emptyList();
	}
	
	protected List<Events.ProcessChangedEvent> trigger(Trigger event) {
		List<Events.ProcessChangedEvent> events = new LinkedList<>();
		events.addAll(triggerExpectedSM(event));
		events.addAll(triggerActualSM(event));
		// NEW: we just notifice the DNI and let it decide when and what to check and progress
		if (this.getOutDNI() != null && events.size() > 0) {
				events.addAll(getOutDNI().signalStateChanged(this));
		}
		return events;
	}
	
	private List<Events.ProcessChangedEvent> triggerActualSM(Trigger event) { 
		List<Events.ProcessChangedEvent> events = new LinkedList<>();
		if (actualSM.canFire(event)) {
			State prevActualLifecycleState = actualSM.getState();
			if (event.equals(Trigger.UNCANCEL)) {
				actualSM.fire(StepLifecycle.uncancel, this);
			} else if (event.equals(Trigger.UNHALT)) {
				actualSM.fire(StepLifecycle.unhalt, this);
			} else 		
				actualSM.fire(event);
			State actualLifecycleState = actualSM.getState();
			if (actualLifecycleState != prevActualLifecycleState) { // state transition
				instance.getPropertyAsSingle(CoreProperties.actualLifecycleState.toString()).set(actualSM.getState().toString());				
				ProcessInstance pi = getParentProcessOrThisIfProcessElseNull();
				events.add(new Events.StepStateTransitionEvent(pi, this, prevActualLifecycleState, actualLifecycleState, true));
				events.addAll(triggerProcessTransitions());
			}
		} 
		return events;
	}
	
	private List<Events.ProcessChangedEvent> triggerExpectedSM(Trigger event) {
		List<Events.ProcessChangedEvent> events = new LinkedList<>();
		State prevExpState = expectedSM.getState();
		
		if (event.equals(Trigger.UNCANCEL) && this.expectedSM.canFire(event)) {
			expectedSM.fire(StepLifecycle.uncancel, this);
		} else if (event.equals(Trigger.UNHALT) && this.expectedSM.canFire(event)) {
			expectedSM.fire(StepLifecycle.unhalt, this);
		}
		else {
			if (this.expectedSM.canFire(event)) 	
				expectedSM.fire(event);			
		}
		if (expectedSM.getState() != prevExpState) { // state transition
				instance.getPropertyAsSingle(CoreProperties.expectedLifecycleState.toString()).set(expectedSM.getState().toString());
				ProcessInstance pi = getParentProcessOrThisIfProcessElseNull();
				events.add(new Events.StepStateTransitionEvent(pi, this, prevExpState, expectedSM.getState(), false));
				switch(expectedSM.getState()) {
				case ENABLED:					
					// handle deviation mitigation --> if actualSM==ACTIVE and expected transitions from Available to Enabled, then should continue to Active
					if (actualSM.getState().equals(State.ACTIVE)) {
						expectedSM.fire(Trigger.ACTIVATE);
						instance.getPropertyAsSingle(CoreProperties.expectedLifecycleState.toString()).set(expectedSM.getState().toString());
						events.add(new Events.StepStateTransitionEvent(pi, this, State.ENABLED, expectedSM.getState(), false));
						// same fore COMPLETED
					} else if (actualSM.getState().equals(State.COMPLETED)) {
						expectedSM.fire(Trigger.MARK_COMPLETE);
						instance.getPropertyAsSingle(CoreProperties.expectedLifecycleState.toString()).set(expectedSM.getState().toString());
						events.add(new Events.StepStateTransitionEvent(pi, this, State.ENABLED, expectedSM.getState(), false));
					//	tryProgress = true;
					} 
					//				// what if we have been in No Work Expected and go back to Enabled or Available, shoulnt we also go to Active --> rules should do this automatically
					//				// or we do upon artifact output --> no: as we can go to completed anyway and thus only activate if there is new activity going on.
					//				}
					break;				
				}
		}			
		return events;
	}
	
	private List<Events.ProcessChangedEvent> triggerProcessTransitions() {
		if (this.getProcess() == null)
			return Collections.emptyList();
		else
			return this.getProcess().signalChildStepStateChanged(this);
	}
	
	@Deprecated
	public List<ProcessScopedCmd> ensureRuleToStateConsistency() {
		// ensure that the rule state is correctly reflected in the process step properties.
		// by checking every condition
		List<ProcessScopedCmd> inconsistencies = new LinkedList<>();
		if (this.getDefinition().getCondition(Conditions.PRECONDITION).isPresent()) {
			Optional<ConsistencyRule> crOpt = this.getConditionStatus(Conditions.PRECONDITION);					
			if (crOpt.isPresent()) {
				ConsistencyRule cr = crOpt.get();
				if (cr.isConsistent() != this.arePreCondFulfilled())
					inconsistencies.add(new ConditionChangedCmd(this, cr, Conditions.PRECONDITION, cr.isConsistent()));				
			}						
		}
		if (this.getDefinition().getCondition(Conditions.POSTCONDITION).isPresent()) {
			Optional<ConsistencyRule> crOpt = this.getConditionStatus(Conditions.POSTCONDITION);					
			if (crOpt.isPresent()) {
				ConsistencyRule cr = crOpt.get();
				if (cr.isConsistent() != this.arePostCondFulfilled())
					inconsistencies.add(new ConditionChangedCmd(this, cr, Conditions.POSTCONDITION, cr.isConsistent()));				
			}						
		}
		return inconsistencies;
	}
	
	@Deprecated
	public Optional<ConsistencyRule> getConditionStatus(Conditions cond) {
		//FIXME: support for multi constraints
		SingleProperty prop = instance.getPropertyAsSingle(cond.toString());
		if (prop == null) 
			return Optional.empty();
		else
			return Optional.ofNullable((ConsistencyRule)prop.get());
	}
	
//	public static List<ProcessDefinitionError> getConstraintValidityStatus(Workspace ws, StepDefinition td) {
//		List<ProcessDefinitionError> errors = new LinkedList<>();
//	//	Map<String, String> status = new HashMap<>();
//		InstanceType instType = getOrCreateDesignSpaceInstanceType(ws, td);
//		for (Conditions condition : Conditions.values()) {
//			if (td.getCondition(condition).isPresent()) {
//				String name = "crd_"+condition+"_"+instType.name();
//				ConsistencyRuleType crt = ConsistencyRuleType.consistencyRuleTypeExists(ws,  name, instType, td.getCondition(condition).get());
//				if (crt == null) {
//					log.error("Expected Rule for existing process not found: "+name);
//					errors.add(new ProcessDefinitionError(td, "Expected Constraint Not Found - Internal Data Corruption", name));
//					//status.put(name, "Corrupt data - Expected Rule not found");
//				} else {
//					if (crt.hasRuleError())
//						errors.add(new ProcessDefinitionError(td, String.format("Condition % has an error", condition), crt.ruleError()));
//				}
//			}	
//		}
//		td.getInputToOutputMappingRules().entrySet().stream()
//			.forEach(entry -> {
//				String name = getDataMappingId(entry, td);
//				String propName = CRD_DATAMAPPING_PREFIX+entry.getKey();
//				InstanceType stepType = getOrCreateDesignSpaceInstanceType(ws, td);
//				PropertyType ioPropType = stepType.getPropertyType(propName);
//				InstanceType ruleType = ioPropType.referencedInstanceType();
//				if (ruleType == null) 	{							
//					log.error("Expected Datamapping Rule for existing process not found: "+name);
//					//status.put(name, "Corrupt data - Expected Datamapping Rule not found");
//					errors.add(new ProcessDefinitionError(td, "Expected DataMapping Not Found - Internal Data Corruption", name));
//				} else {
//					ConsistencyRuleType crt = (ConsistencyRuleType)ruleType;
//					if (crt.hasRuleError())
//						errors.add(new ProcessDefinitionError(td, String.format("DataMapping % has an error", name), crt.ruleError()));
//				}
//			});
//		//qa constraints:
//		ProcessDefinition pd = td.getProcess() !=null ? td.getProcess() : (ProcessDefinition)td;
//		td.getQAConstraints().stream()
//			.forEach(spec -> {
//				String specId = getQASpecId(spec, pd);
//				ConsistencyRuleType crt = ConsistencyRuleType.consistencyRuleTypeExists(ws,  specId, instType, spec.getQaConstraintSpec());
//				if (crt == null) {
//					log.error("Expected Rule for existing process not found: "+specId);
//					errors.add(new ProcessDefinitionError(td, "Expected QA Constraint Not Found - Internal Data Corruption", specId));
//				} else
//					if (crt.hasRuleError())
//						errors.add(new ProcessDefinitionError(td, String.format("QA Constraint % has an error", specId), crt.ruleError()));
//			});
//		return errors;
//	}
	
	public static InstanceType getOrCreateDesignSpaceCoreSchema(Workspace ws) {
		Optional<InstanceType> thisType = Optional.ofNullable(ws.TYPES_FOLDER.instanceTypeWithName(designspaceTypeId)); 
		if (thisType.isPresent())
			return thisType.get();
		else {
			InstanceType typeStep = ws.createInstanceType(ProcessStep.designspaceTypeId, ws.TYPES_FOLDER, ProcessInstanceScopedElement.getOrCreateDesignSpaceCoreSchema(ws));
			typeStep.createPropertyType(CoreProperties.stepDefinition.toString(), Cardinality.SINGLE, StepDefinition.getOrCreateDesignSpaceCoreSchema(ws));
			typeStep.createPropertyType(CoreProperties.inDNI.toString(), Cardinality.SINGLE, DecisionNodeInstance.getOrCreateDesignSpaceCoreSchema(ws));
			typeStep.createPropertyType(CoreProperties.outDNI.toString(), Cardinality.SINGLE, DecisionNodeInstance.getOrCreateDesignSpaceCoreSchema(ws));
			// FIXME: better realized via bidirectional properties
			typeStep.createPropertyType(CoreProperties.actualLifecycleState.toString(), Cardinality.SINGLE, Workspace.STRING);
			typeStep.createPropertyType(CoreProperties.expectedLifecycleState.toString(), Cardinality.SINGLE, Workspace.STRING);
			
			typeStep.createPropertyType(CoreProperties.processedPreCondFulfilled.toString(), Cardinality.SINGLE, Workspace.BOOLEAN);
			typeStep.createPropertyType(CoreProperties.processedPostCondFulfilled.toString(), Cardinality.SINGLE, Workspace.BOOLEAN);
			typeStep.createPropertyType(CoreProperties.processedCancelCondFulfilled.toString(), Cardinality.SINGLE, Workspace.BOOLEAN);
			typeStep.createPropertyType(CoreProperties.processedActivationCondFulfilled.toString(), Cardinality.SINGLE, Workspace.BOOLEAN);			
			typeStep.createPropertyType(CoreProperties.isWorkExpected.toString(), Cardinality.SINGLE, Workspace.BOOLEAN);
			// opposable no longer possible as, we cant then set is for pre/post, etc
			typeStep.createPropertyType(CoreProperties.qaState.toString(), Cardinality.MAP,  ConstraintWrapper.getOrCreateDesignSpaceInstanceType(ws));
			//check if we need to set step parent on opposite end --> we do now set it upon instantiation
			typeStep.createPropertyType(CoreProperties.preconditions.toString(), Cardinality.MAP,  ConstraintWrapper.getOrCreateDesignSpaceInstanceType(ws));
			typeStep.createPropertyType(CoreProperties.postconditions.toString(), Cardinality.MAP,  ConstraintWrapper.getOrCreateDesignSpaceInstanceType(ws));
			typeStep.createPropertyType(CoreProperties.cancelconditions.toString(), Cardinality.MAP,  ConstraintWrapper.getOrCreateDesignSpaceInstanceType(ws));
			typeStep.createPropertyType(CoreProperties.activationconditions.toString(), Cardinality.MAP,  ConstraintWrapper.getOrCreateDesignSpaceInstanceType(ws));
			
			return typeStep;
		}
	}
	
	public static InstanceType getOrCreateDesignSpaceInstanceType(Workspace ws, StepDefinition td, InstanceType processType) {
		String stepName = getProcessStepName(td);
		Optional<InstanceType> thisType = Optional.ofNullable(ws.TYPES_FOLDER.instanceTypeWithName(stepName));
		if (thisType.isPresent())
			return thisType.get();
		else {
			InstanceType superType = getOrCreateDesignSpaceCoreSchema(ws);
			InstanceType typeStep = ws.createInstanceType(stepName, ws.TYPES_FOLDER, superType);
			td.getExpectedInput().entrySet().stream()
				.forEach(entry -> {
						typeStep.createPropertyType(PREFIX_IN+entry.getKey(), Cardinality.SET, entry.getValue());
				});
			td.getExpectedOutput().entrySet().stream()
			.forEach(entry -> {
					typeStep.createPropertyType(PREFIX_OUT+entry.getKey(), Cardinality.SET, entry.getValue());
			});
			td.getInputToOutputMappingRules().entrySet().stream()
				.forEach(entry -> {
					if (entry.getValue() != null) {
						ConsistencyRuleType crt = ConsistencyRuleType.create(ws, typeStep, getDataMappingId(entry, td), completeDatamappingRule(entry.getKey(), entry.getValue())); 
						typeStep.createPropertyType(CRD_DATAMAPPING_PREFIX+entry.getKey(), Cardinality.SINGLE, crt);					
					}//assert ConsistencyUtils.crdValid(crt); as no workspace.concludeTransaction is called here, no need to assert this here, as will never be false here	
				});
			// override process property type to actual process so we can access its config when needed
			if (processType != null) {
				if (typeStep.getPropertyType(ProcessInstanceScopedElement.CoreProperties.process.toString()) == null)
					typeStep.createPropertyType(ProcessInstanceScopedElement.CoreProperties.process.toString(), Cardinality.SINGLE, processType);
				//else
					//typeStep.getPropertyType(ProcessInstanceScopedElement.CoreProperties.process.toString()).setInstanceType(processType);
			} else {
				ProcessInstanceScopedElement.addGenericProcessProperty(typeStep);
			}
			return typeStep;
		}
	}
	
	private static String completeDatamappingRule(String param, String rule) {
		return rule
				+"\r\n"
				+"->asSet()\r\n"  
				+"->symmetricDifference(self.out_"+param+")\r\n"  
				+"->size() = 0";
	}
	
	public static String getDataMappingId(Map.Entry<String,String> ioMapping, StepDefinition sd) {
		String procId = sd.getProcess() != null ? sd.getProcess().getName() : "";
		return CRD_DATAMAPPING_PREFIX+ioMapping.getKey()+"_"+sd.getName()+"_"+procId;
	}
	
	public static String getQASpecId(ConstraintSpec spec, ProcessDefinition context) {
		return CRD_QASPEC_PREFIX+spec.getConstraintId()+"_"+context.getName();
	}

	public static String getProcessStepName(StepDefinition sd) {
		String procName = sd.getProcess() != null ? sd.getProcess().getName() : "ROOTPROCESS";
		return designspaceTypeId+"_"+sd.getName()+"_"+procName;
	}
	

	
	protected static ProcessStep getInstance(Workspace ws, StepDefinition sd, DecisionNodeInstance inDNI, DecisionNodeInstance outDNI, ProcessInstance scope) {
		assert(sd != null);
		assert(inDNI != null);
		assert(outDNI != null);
		assert(scope != null);
		if (sd instanceof ProcessDefinition) { // we have a subprocess
			// we delegate to ProcessInstance
			return ProcessInstance.getSubprocessInstance(ws, (ProcessDefinition) sd, inDNI, outDNI, scope);
		} else {
			Instance instance = ws.createInstance(getOrCreateDesignSpaceInstanceType(ws, sd, scope.getInstance().getInstanceType()), sd.getName()+"_"+UUID.randomUUID());
			ProcessStep step = WrapperCache.getWrappedInstance(ProcessStep.class, instance);
			step.setProcess(scope);
			step.init(ws, sd, inDNI, outDNI);
//			// if this is a noop step, complete it immediately
//			if (step.getName().startsWith(StepDefinition.NOOPSTEP_PREFIX)) {
//				step.setPreConditionsFulfilled(true);
//				step.setPostConditionsFulfilled(true);			
//			}
			return step;
		}
	}

	protected void init(Workspace ws, StepDefinition sd, DecisionNodeInstance inDNI, DecisionNodeInstance outDNI) {
		
		if (this.getName().startsWith(StepDefinition.NOOPSTEP_PREFIX)) { // assumes/expects no pre/post cond and no qa	
			instance.getPropertyAsSingle(CoreProperties.processedPreCondFulfilled.toString()).set(true);
			instance.getPropertyAsSingle(CoreProperties.processedPostCondFulfilled.toString()).set(true);				
			instance.getPropertyAsSingle(CoreProperties.isWorkExpected.toString()).set(false);
		} else {
			instance.getPropertyAsSingle(CoreProperties.processedPreCondFulfilled.toString()).set(false);
			instance.getPropertyAsSingle(CoreProperties.processedPostCondFulfilled.toString()).set(false);					
			instance.getPropertyAsSingle(CoreProperties.isWorkExpected.toString()).set(true);
		}
		instance.getPropertyAsSingle(CoreProperties.processedCancelCondFulfilled.toString()).set(false);
		instance.getPropertyAsSingle(CoreProperties.processedActivationCondFulfilled.toString()).set(false);
		
		instance.getPropertyAsSingle(CoreProperties.stepDefinition.toString()).set(sd.getInstance());
		if (inDNI != null) {
			instance.getPropertyAsSingle(CoreProperties.inDNI.toString()).set(inDNI.getInstance());
			inDNI.addOutStep(this);
			// FIXME: better realized via bidirectional properties
		}
		if (outDNI != null) {
			instance.getPropertyAsSingle(CoreProperties.outDNI.toString()).set(outDNI.getInstance());
			outDNI.addInStep(this);		
			// FIXME: better realized via bidirectional properties
		}
		// only if no input and no preconditions --> automatically go into enabled, (if there is input, then there needs to be a precondition checking for presence of input)
		// but this implies that only manual output can be set as there is no input to derive output from (as there cannot be any io mapping)
		// --> UPDATE: if there is no precondition then we assume the input is optional, 
		if (/*DEL-UPDATE: sd.getExpectedInput().isEmpty() &&*/ sd.getPreconditions().isEmpty()) {
			this.setPreConditionsFulfilled(true);
		}
		ProcessDefinition pd = sd.getProcess() !=null ? sd.getProcess() : (ProcessDefinition)sd;
		sd.getQAConstraints().stream()
		.forEach(spec -> { 
			String qid = getQASpecId(spec, pd);			
			ConstraintWrapper cw = ConstraintWrapper.getInstance(ws, spec, ZonedDateTime.now(), this, getParentProcessOrThisIfProcessElseNull());
			instance.getPropertyAsMap(CoreProperties.qaState.toString()).put(qid, cw.getInstance());
		});
		// init of multi constraint wrappers:
		sd.getPostconditions().stream()
		.sorted(RuleAugmentation.CONSTRAINTCOMPARATOR)
		.forEach(spec -> {
			String specId = RuleAugmentation.getConstraintName(Conditions.POSTCONDITION, spec.getOrderIndex(), this.getInstance().getInstanceType());		
			ConstraintWrapper cw = ConstraintWrapper.getInstance(ws, spec, ZonedDateTime.now(), this, getParentProcessOrThisIfProcessElseNull());
			instance.getPropertyAsMap(CoreProperties.postconditions.toString()).put(specId, cw.getInstance());
		});
		sd.getPreconditions().stream()
		.sorted(RuleAugmentation.CONSTRAINTCOMPARATOR)
		.forEach(spec -> {
			String specId = RuleAugmentation.getConstraintName(Conditions.PRECONDITION, spec.getOrderIndex(), this.getInstance().getInstanceType());		
			ConstraintWrapper cw = ConstraintWrapper.getInstance(ws, spec, ZonedDateTime.now(), this, getParentProcessOrThisIfProcessElseNull());
			instance.getPropertyAsMap(CoreProperties.preconditions.toString()).put(specId, cw.getInstance());
		});
		sd.getCancelconditions().stream()
		.sorted(RuleAugmentation.CONSTRAINTCOMPARATOR)
		.forEach(spec -> {
			String specId = RuleAugmentation.getConstraintName(Conditions.CANCELATION, spec.getOrderIndex(), this.getInstance().getInstanceType());		
			ConstraintWrapper cw = ConstraintWrapper.getInstance(ws, spec, ZonedDateTime.now(), this, getParentProcessOrThisIfProcessElseNull());
			instance.getPropertyAsMap(CoreProperties.cancelconditions.toString()).put(specId, cw.getInstance());
		});
		sd.getActivationconditions().stream()
		.sorted(RuleAugmentation.CONSTRAINTCOMPARATOR)
		.forEach(spec -> {
			String specId = RuleAugmentation.getConstraintName(Conditions.ACTIVATION, spec.getOrderIndex(), this.getInstance().getInstanceType());		
			ConstraintWrapper cw = ConstraintWrapper.getInstance(ws, spec, ZonedDateTime.now(), this, getParentProcessOrThisIfProcessElseNull());
			instance.getPropertyAsMap(CoreProperties.activationconditions.toString()).put(specId, cw.getInstance());
		});
	}
	
//	private ProcessInstance getParentOrThisProcess() {
//		ProcessInstance process = getProcess(); //from parent
//		if (process == null && this instanceof ProcessInstance) {
//			return (ProcessInstance)this; 
//		} else {
//			return process;
//		}
//	}
	
	@Override
	public String toString() {
		String input = getDefinition().getExpectedInput().keySet().stream()
				.map(eIn -> eIn+"="+getInput(eIn).stream().map(art -> art.name()).collect(Collectors.joining(",","[","]")) )
				.collect(Collectors.joining("; "));
		String output = getDefinition().getExpectedOutput().keySet().stream()
				.map(eIn -> eIn+"="+getOutput(eIn).stream().map(art -> art.name()).collect(Collectors.joining(",","[","]")) )
				.collect(Collectors.joining("; "));
		String states = "E:"+expectedSM.getState()+"|A:"+actualSM.getState();
		String process = getProcess() != null ? getProcess().getName() : "NONE";
		String inDNI = getInDNI() != null ? getInDNI().getDefinition().getName() : "NONE" ; 
		String outDNI = getOutDNI() != null ? getOutDNI().getDefinition().getName() : "NONE";
		
		
		String cond = String.format("[Pre: %s |Post: %s |Canc: %s |QAok: %s |Unsafe: %s |Premature: %s]", arePreCondFulfilled(), arePostCondFulfilled(), areCancelCondFulfilled(), areConstraintsFulfilled(CoreProperties.qaState.toString()), isInUnsafeOperationModeDueTo().size(), isInPrematureOperationModeDueTo().size());
		
		return "Step "+ getName() + " "+states+" "+input+" "+output+" "+cond+" [DNIs: "+inDNI+":"+outDNI+"] in Proc: " + process +" DS: " +getInstance().toString();
	}


	public static class CompareBySpecOrder  implements Comparator<ProcessStep> {

		@Override
		public int compare(ProcessStep o1, ProcessStep o2) {
			if (o1 != null && o1.getDefinition() != null && o2 != null && o2.getDefinition() != null)
				return o1.getDefinition().getSpecOrderIndex().compareTo(o2.getDefinition().getSpecOrderIndex());
			else return 0;
		}
		
	}

}
