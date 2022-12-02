package at.jku.isse.passiveprocessengine.instance;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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
import at.jku.isse.passiveprocessengine.definition.ProcessDefinition;
import at.jku.isse.passiveprocessengine.definition.QAConstraintSpec;
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
import at.jku.isse.passiveprocessengine.instance.messages.Responses;
import at.jku.isse.passiveprocessengine.instance.messages.Responses.IOResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ProcessStep extends ProcessInstanceScopedElement{

	private static final String CRD_QASPEC_PREFIX = "crd_qaspec_";

	private static final String CRD_DATAMAPPING_PREFIX = "crd_datamapping_";


	public static enum CoreProperties {actualLifecycleState, expectedLifecycleState, stepDefinition, inDNI, outDNI, qaState, 
		processedPreCondFulfilled, processedPostCondFulfilled, processedCancelCondFulfilled, isWorkExpected};
	
	public static final String designspaceTypeId = ProcessStep.class.getSimpleName();
	
	protected transient StateMachine<StepLifecycle.State, StepLifecycle.Trigger> actualSM;
	protected transient StateMachine<StepLifecycle.State, StepLifecycle.Trigger> expectedSM;
	
	public ProcessStep(Instance instance) {
		super(instance);
		initState();
	}
	
	//check if those transient properties are correctly reset upon loading
	//protected transient boolean arePreCondFulfilled = false;
	//protected transient boolean arePostCondFulfilled = false;
	//protected transient boolean areCancelCondFulfilled = false;
	//protected transient boolean isWorkExpected = true;
//	protected transient Map<String, ConstraintWrapper> qaState = new HashMap<>();
	
	protected transient boolean priorQAfulfilled = false;
	
	private void initState() {
		
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
		priorQAfulfilled = areQAconstraintsFulfilled();
	}
	
	public ProcessScopedCmd prepareRuleEvaluationChange(ConsistencyRule cr, PropertyUpdateSet op) {
		// now here we have to distinguish what this evaluation change implies
		ConsistencyRuleType crt = (ConsistencyRuleType)cr.getInstanceType();
		Conditions cond = determineCondition(crt);
		if (cond != null ) {
			log.debug(String.format("Step %s has %s evaluate to %s", this.getName(), cond, op.value().toString()));
			SingleProperty prop = instance.getPropertyAsSingle(cond.toString());
			if (prop.get() == null) 
				prop.set(cr);
			return new ConditionChangedCmd(this, cond, Boolean.valueOf(op.value().toString()));
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
		if (crt.name().startsWith("crd_"+Conditions.PRECONDITION.toString()+"_")) 
			return Conditions.PRECONDITION;
		else if (crt.name().startsWith("crd_"+Conditions.POSTCONDITION.toString()+"_")) 
			return Conditions.POSTCONDITION;
		else if (crt.name().startsWith("crd_"+Conditions.ACTIVATION.toString()+"_")) 
			return Conditions.ACTIVATION;
		else if (crt.name().startsWith("crd_"+Conditions.CANCELATION.toString()+"_")) 
			return Conditions.CANCELATION;
		else {
			if (!crt.name().startsWith(CRD_DATAMAPPING_PREFIX) && !crt.name().startsWith(CRD_QASPEC_PREFIX))
					log.error("Unknown consistency rule: "+crt.name());
			return null;
		}
	}
	
	
	public ProcessScopedCmd prepareIOAddEvent(PropertyUpdateAdd op) { //List<Events.ProcessChangedEvent>
		// if in added, establish if this resembles unexpected late input 
		if (op.name().startsWith("in_") 
				&& ( this.getActualLifecycleState().equals(State.ACTIVE) 
					|| this.getActualLifecycleState().equals(State.COMPLETED) )) {
			//(if so, then do something about this)
			Id addedId = (Id) op.value();
			Element added = ws.findElement(addedId);
			log.info(String.format("Step %s received unexpected late input %s %s", this.getName(), op.name(), added.name()  ));
			// Note that the adding has already happened, thus there is nothing to report back, this is only for checking whether we need to do something else as well.
		}
		else if (op.name().startsWith("out_") // if out added, establish if this is late output, then propagate further
				&& ( this.getActualLifecycleState().equals(State.COMPLETED) || isImmediateDataPropagationEnabled() ) ){
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
		if (op.name().startsWith("in_") 
				&& ( this.getActualLifecycleState().equals(State.ACTIVE) 
					|| this.getActualLifecycleState().equals(State.COMPLETED) )) {
			//(if so, then do something about this)
			log.info(String.format("Step %s had some input removed from %s after step start", this.getName(), op.name()));
		}
		else if (op.name().startsWith("out_") // if out removed, establish if this is late output removal, then propagate further
				&& ( this.getActualLifecycleState().equals(State.COMPLETED) || isImmediateDataPropagationEnabled() ) ){
			
			if (this.getActualLifecycleState().equals(State.COMPLETED)) {
				log.info(String.format("Step %s had some output removed from %s after step completion, queuing for propagation to successors", this.getName(), op.name()));
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
		//boolean preQaState = areQAconstraintsFulfilled(); // are all QA checks fulfilled?
		//ConstraintWrapper cw = qaState.get(id);
		ConstraintWrapper cw = WrapperCache.getWrappedInstance(ConstraintWrapper.class, (Instance) instance.getPropertyAsMap(CoreProperties.qaState.toString()).get(id));
		
		cw.setCrIfEmpty(cr);
		//cw.setEvalResult(fulfilled);
		cw.setLastChanged(getProcess().getCurrentTimestamp());
		List<Events.ProcessChangedEvent> qaChanges = new LinkedList<>();
		qaChanges.add(new Events.QAConstraintFulfillmentChanged(this.getProcess(), this, cw));
		boolean newQaState = areQAconstraintsFulfilled(); // are all QA checks now fulfilled?
		if (priorQAfulfilled != newQaState) { // a change in qa fulfillment that we might want to react to
			priorQAfulfilled = newQaState;
			qaChanges.add(new Events.QAFulfillmentChanged(this.getProcess(), this, newQaState));
			if (arePostCondFulfilled() && newQaState)  {
				qaChanges.addAll(this.trigger(StepLifecycle.Trigger.MARK_COMPLETE)) ;
			} else if (!newQaState && actualSM.isInState(State.COMPLETED)) {
				qaChanges.addAll(this.trigger(StepLifecycle.Trigger.ACTIVATE));
			}
		}
		return qaChanges;
	}
	
	public Set<ConstraintWrapper> getQAstatus() {
		return (Set<ConstraintWrapper>) instance.getPropertyAsMap(CoreProperties.qaState.toString()).values().stream()
		.map(inst -> WrapperCache.getWrappedInstance(ConstraintWrapper.class, (Instance) inst))
		.collect(Collectors.toSet());
		//return qaState.values().parallelStream().collect(Collectors.toSet());
	}
	
	public Optional<ConsistencyRule> getConditionStatus(Conditions cond) {
		SingleProperty prop = instance.getPropertyAsSingle(cond.toString());
		if (prop == null) 
			return Optional.empty();
		else
			return Optional.ofNullable((ConsistencyRule)prop.get());
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
	
	public boolean isWorkExpected() {
		return (boolean) instance.getPropertyAsValue(CoreProperties.isWorkExpected.toString());
	}
	
	public boolean isImmediateDataPropagationEnabled() {
		if (getProcess() == null)
			return false;
		else
			return getProcess().isImmediateDataPropagationEnabled();
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
						if (step.areQAconstraintsFulfilled()) // the have to be as otherwise we would have inflow fulfilled/resp exp and actual State Completed?!
							unsafeSteps.addAll(step.isInUnsafeOperationModeDueTo());
						else 
							unsafeSteps.add(step);
					});
					
				} else { // not sufficient steps available: AND: not all, OR, none, XOR, none, hence here check all steps as all of them can/must be fulfilled eventually
					this.getInDNI().getInSteps().stream().forEach(step -> {
						if (step.areQAconstraintsFulfilled()) 
							unsafeSteps.addAll(step.isInUnsafeOperationModeDueTo());
						else 
							unsafeSteps.add(step);
					});
				}
			} else {
				// when not propagated --> hence why might this have happened, get all insteps and check their QA constraints, if fine, ask them for isInUsafeOperation
				this.getInDNI().getInSteps().stream().forEach(step -> {
					if (step.areQAconstraintsFulfilled()) 
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
		// we are not deleting input and output artifacts as we are just referencing them!
		// finally delete self
		this.getInstance().delete();
	}
	
	public StepDefinition getDefinition() {
		return  WrapperCache.getWrappedInstance(StepDefinition.class, instance.getPropertyAsInstance(CoreProperties.stepDefinition.toString()));
		
	}
	
	protected Responses.IOResponse removeInput(String inParam, Instance artifact) {
		if (getDefinition().getExpectedInput().containsKey(inParam)) {
			Property<?> prop = instance.getProperty("in_"+inParam);
			if (prop.propertyType.isAssignable(artifact)) {
				instance.getPropertyAsSet("in_"+inParam).remove(artifact);
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
		SetProperty setP = instance.getPropertyAsSet("in_"+param);
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
			Property<?> prop = instance.getProperty("in_"+inParam);
			if (prop.propertyType.isAssignable(artifact)) {
				instance.getPropertyAsSet("in_"+inParam).add(artifact);
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
		SetProperty setP = instance.getPropertyAsSet("out_"+param);
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
			Property<?> prop = instance.getProperty("out_"+param);
			if (prop.propertyType.isAssignable(artifact)) {
				instance.getPropertyAsSet("out_"+param).add(artifact);
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
		instance.getPropertyAsSet("out_"+param).remove(art);
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
	
	public void setWorkExpected(boolean isExpected) {
		if (isWorkExpected() != isExpected ) {
			instance.getPropertyAsSingle(CoreProperties.isWorkExpected.toString()).set(isExpected);	
				if (!isExpected) {
					trigger(Trigger.HALT);
				}
				else {
				if (arePostCondFulfilled() && areQAconstraintsFulfilled())
					trigger(Trigger.MARK_COMPLETE_REPAIR);
				if (arePreCondFulfilled())
					trigger(Trigger.ENABLE);
				if (actualSM.isInState(State.ACTIVE))
					trigger(Trigger.ACTIVATE_REPAIR);
				else
					trigger(Trigger.RESET);
				}
			}
	}
	
	public boolean areQAconstraintsFulfilled() {
		// are there all constraint wrappers actually added already
		if (this.getDefinition() == null)
			return false;
		
		int expQA = this.getDefinition().getQAConstraints().size();
		int actualQA = instance.getPropertyAsMap(CoreProperties.qaState.toString()).values().size();
		if (expQA != actualQA) 
			return false; // as long as the expected QA is not the actual number of QA checks, the eval cant be true;
		
		return instance.getPropertyAsMap(CoreProperties.qaState.toString()).values().stream()
			.map(inst -> WrapperCache.getWrappedInstance(ConstraintWrapper.class, (Instance) inst))
			.allMatch(cw -> ((ConstraintWrapper)cw).getEvalResult()==true);
		//return  qaState.values().parallelStream().allMatch(cw -> cw.getEvalResult()==true);
	}
	
	public List<Events.ProcessChangedEvent> setPostConditionsFulfilled(boolean isfulfilled) {
		if (arePostCondFulfilled() != isfulfilled) { // a change
			List<Events.ProcessChangedEvent> events = new LinkedList<>();
			events.add(new Events.PostconditionFulfillmentChanged(this.getProcess(), this, isfulfilled));
			instance.getPropertyAsSingle(CoreProperties.processedPostCondFulfilled.toString()).set(isfulfilled);
			if (isfulfilled && areQAconstraintsFulfilled())  
				events.addAll(this.trigger(StepLifecycle.Trigger.MARK_COMPLETE)) ;
			if (!isfulfilled && actualSM.isInState(State.COMPLETED)) 
				events.addAll(this.trigger(StepLifecycle.Trigger.ACTIVATE));
			return events;
		}
		return Collections.emptyList();
	}

	
	public List<Events.ProcessChangedEvent> setPreConditionsFulfilled(boolean isfulfilled) {
		if (arePreCondFulfilled() != isfulfilled) {  // a change
			instance.getPropertyAsSingle(CoreProperties.processedPreCondFulfilled.toString()).set(isfulfilled);
			if (isfulfilled)  
				return this.trigger(StepLifecycle.Trigger.ENABLE) ;
			else 
				return this.trigger(StepLifecycle.Trigger.RESET);
		}
		return Collections.emptyList();
	}
	
	public List<Events.ProcessChangedEvent> setCancelConditionsFulfilled(boolean isfulfilled) {
		if (areCancelCondFulfilled() != isfulfilled) {
			instance.getPropertyAsSingle(CoreProperties.processedCancelCondFulfilled.toString()).set(isfulfilled);
			if (isfulfilled)
				return trigger(Trigger.CANCEL);
			else { // check which is the new state:
				// we cant use actual state as this might be deviating multiple ways (e.g., we should now be in available, but actual state is in active
				if (!isWorkExpected())
					return trigger(Trigger.HALT); //other steps are prefered/used at the moment
				if (arePostCondFulfilled() && areQAconstraintsFulfilled())
					return trigger(Trigger.MARK_COMPLETE_REPAIR);
				if (arePreCondFulfilled())
					return trigger(Trigger.ENABLE);
				if (actualSM.isInState(State.ACTIVE))
					return trigger(Trigger.ACTIVATE_REPAIR);
				else
					return trigger(Trigger.RESET);
			}
		}
		return Collections.emptyList();
	}
	
	public List<Events.ProcessChangedEvent> setActivationConditionsFulfilled() {
		return trigger(Trigger.ACTIVATE);
	}
	
	protected List<Events.ProcessChangedEvent> trigger(Trigger event) {
		State prevExpectedSM = expectedSM.getState(); // to check whether we were in AVAILABLE before and are not anywhere else
		// trigger expectedTransition:
		if (event.equals(Trigger.ACTIVATE)) {
			if (expectedSM.isInState(State.CANCELED) || expectedSM.isInState(State.NO_WORK_EXPECTED))
				event = Trigger.ACTIVATE_DEVIATING;
		}
		else if (event.equals(Trigger.MARK_COMPLETE))
			if (expectedSM.isInState(State.CANCELED) || expectedSM.isInState(State.NO_WORK_EXPECTED))
				event = Trigger.MARK_COMPLETE_DEVIATING;

		List<Events.ProcessChangedEvent> events = new LinkedList<>();
		boolean tryProgress = false;		
		if (actualSM.canFire(event)) {
			State prevActualLifecycleState = actualSM.getState();
			actualSM.fire(event);
			State actualLifecycleState = actualSM.getState();
			if (actualLifecycleState != prevActualLifecycleState) { // state transition
				instance.getPropertyAsSingle(CoreProperties.actualLifecycleState.toString()).set(actualSM.getState().toString());
				events.add(new Events.StepStateTransitionEvent(this.getProcess(), this, prevActualLifecycleState, actualLifecycleState, true));
				events.addAll(triggerProcessTransitions());
				switch (expectedSM.getState()) { // we only progress in deviating state when postcond fulfilled or cancled or no work expected
				case CANCELED: //fallthrough
				case COMPLETED://fallthrough
				case NO_WORK_EXPECTED: 
					tryProgress = true;
				}				
			}
		} else {
			log.info(String.format("Step %s received (and ignored) for 'expectedSM' unexpected Event %s for State %s ", this.getName(),  event,  actualSM.getState()));			
		}		

		if (this.expectedSM.canFire(event)) {
			State prevExpState = expectedSM.getState();
			expectedSM.fire(event);
			if (expectedSM.getState() != prevExpState) { // state transition
				instance.getPropertyAsSingle(CoreProperties.expectedLifecycleState.toString()).set(expectedSM.getState().toString());
				events.add(new Events.StepStateTransitionEvent(this.getProcess(), this, prevExpState, expectedSM.getState(), false));
				switch(expectedSM.getState()) {
				case ENABLED:					
					// handle deviation mitigation --> if actualSM==ACTIVE and expected transitions from Available to Enabled, then should continue to Active
					if (actualSM.getState().equals(State.ACTIVE)) {
						expectedSM.fire(Trigger.ACTIVATE);
						instance.getPropertyAsSingle(CoreProperties.expectedLifecycleState.toString()).set(expectedSM.getState().toString());
						events.add(new Events.StepStateTransitionEvent(this.getProcess(), this, State.ENABLED, expectedSM.getState(), false));
						// same fore COMPLETED
					} else if (actualSM.getState().equals(State.COMPLETED)) {
						expectedSM.fire(Trigger.MARK_COMPLETE);
						instance.getPropertyAsSingle(CoreProperties.expectedLifecycleState.toString()).set(expectedSM.getState().toString());
						events.add(new Events.StepStateTransitionEvent(this.getProcess(), this, State.ENABLED, expectedSM.getState(), false));
						tryProgress = true;
					} 
					//				// what if we have been in No Work Expected and go back to Enabled or Available, shoulnt we also go to Active --> rules should do this automatically
					//				// or we do upon artifact output --> no: as we can go to completed anyway and thus only activate if there is new activity going on.
					//				}
					break;
				case CANCELED:
				case NO_WORK_EXPECTED:
				case COMPLETED:
					tryProgress = true;
					break;
				}
			}
		}		
		if (tryProgress) {
			switch (expectedSM.getState()) { // we only progress automatically when we are indeed on track with the process
			case CANCELED: //fallthrough
			case COMPLETED://fallthrough
			case NO_WORK_EXPECTED: 
				if (this.getOutDNI() != null) //need to check, we might be a process without a parent
					events.addAll(this.getOutDNI().tryInConditionsFullfilled());
			} 
		} else {
			switch (expectedSM.getState()) { // we trigger downstream early steps when we have just left available and are now in active or enabled
			case ACTIVE:
			case ENABLED:
				if (prevExpectedSM.equals(State.AVAILABLE) && isImmediateDataPropagationEnabled() && this.getOutDNI() != null ) {
					this.getOutDNI().initiateDownstreamSteps(true); // to prepare the next steps further downstream even though they should not start yet.
				}
			} 
		}
		return events;
	}
	
	private List<Events.ProcessChangedEvent> triggerProcessTransitions() {
		if (this.getProcess() == null)
			return Collections.emptyList();
		if (actualSM.getState().equals(State.ENABLED) &&  this.getProcess().getDefinition().getCondition(Conditions.PRECONDITION).isEmpty()) {
			if (this.getProcess().getActualLifecycleState().equals(State.COMPLETED))
				return getProcess().setActivationConditionsFulfilled(); // we are back in an enabled state, let the process know that its not COMPLETED anymore
			return getProcess().setPreConditionsFulfilled(true); // ensure the process is also in an enabled state
		} else
		if ((actualSM.getState().equals(State.ACTIVE) || actualSM.getState().equals(State.COMPLETED)) 
				&& !getProcess().getActualLifecycleState().equals(State.ACTIVE)) {
			return getProcess().setActivationConditionsFulfilled();
		} 
		return Collections.emptyList();
	}
	
	public static Map<String, String> getConstraintValidityStatus(Workspace ws, StepDefinition td) {
		Map<String, String> status = new HashMap<>();
		InstanceType instType = getOrCreateDesignSpaceInstanceType(ws, td);
		for (Conditions condition : Conditions.values()) {
			if (td.getCondition(condition).isPresent()) {
				String name = "crd_"+condition+"_"+instType.name();
				ConsistencyRuleType crt = ConsistencyRuleType.consistencyRuleTypeExists(ws,  name, instType, td.getCondition(condition).get());
				if (crt == null) {
					log.error("Expected Rule for existing process not found: "+name);
					status.put(name, "Corrupt data - Expected Rule not found");
				} else
					status.put(name, crt.hasRuleError() ? crt.ruleError() : "valid");
			}	
		}
		td.getInputToOutputMappingRules().entrySet().stream()
			.forEach(entry -> {
				String name = CRD_DATAMAPPING_PREFIX+entry.getKey()+"_"+instType.name();
				ConsistencyRuleType crt = ConsistencyRuleType.consistencyRuleTypeExists(ws,  name, instType, entry.getValue());
				if (crt == null) {
					log.error("Expected Rule for existing process not found: "+name);
					status.put(name, "Corrupt data - Expected Rule not found");
				} else
					status.put(name, crt.hasRuleError() ? crt.ruleError() : "valid");
			});
		//qa constraints:
		ProcessDefinition pd = td.getProcess() !=null ? td.getProcess() : (ProcessDefinition)td;
		td.getQAConstraints().stream()
			.forEach(spec -> {
				String specId = getQASpecId(spec, pd);
				ConsistencyRuleType crt = ConsistencyRuleType.consistencyRuleTypeExists(ws,  specId, instType, spec.getQaConstraintSpec());
				if (crt == null) {
					log.error("Expected Rule for existing process not found: "+specId);
					status.put(specId, "Corrupt data - Expected Rule not found");
				} else
					status.put(specId, crt.hasRuleError() ? crt.ruleError() : "valid");
			});
		return status;
	}
	
	public static InstanceType getOrCreateDesignSpaceCoreSchema(Workspace ws) {
		Optional<InstanceType> thisType = ws.debugInstanceTypes().stream()
			.filter(it -> it.name().equals(ProcessStep.designspaceTypeId))
			.findAny();
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
			typeStep.createPropertyType(CoreProperties.isWorkExpected.toString(), Cardinality.SINGLE, Workspace.BOOLEAN);
			return typeStep;
		}
	}
	
	public static InstanceType getOrCreateDesignSpaceInstanceType(Workspace ws, StepDefinition td) {
		String stepName = getProcessStepName(td);
		Optional<InstanceType> thisType = ws.debugInstanceTypes().stream()
				.filter(it -> !it.isDeleted)
				.filter(it -> it.name().equals(stepName))
				.findAny();
		if (thisType.isPresent())
			return thisType.get();
		else {
			InstanceType superType = getOrCreateDesignSpaceCoreSchema(ws);
			InstanceType typeStep = ws.createInstanceType(stepName, ws.TYPES_FOLDER, superType);
			td.getExpectedInput().entrySet().stream()
				.forEach(entry -> {
						typeStep.createPropertyType("in_"+entry.getKey(), Cardinality.SET, entry.getValue());
				});
			td.getExpectedOutput().entrySet().stream()
			.forEach(entry -> {
					typeStep.createPropertyType("out_"+entry.getKey(), Cardinality.SET, entry.getValue());
			});
			td.getInputToOutputMappingRules().entrySet().stream()
				.forEach(entry -> {
					if (entry.getValue() != null) {
						ConsistencyRuleType crt = ConsistencyRuleType.create(ws, typeStep, getDataMappingId(entry, td), entry.getValue()); 
						typeStep.createPropertyType(CRD_DATAMAPPING_PREFIX+entry.getKey(), Cardinality.SINGLE, crt);					
					}//assert ConsistencyUtils.crdValid(crt); as no workspace.concludeTransaction is called here, no need to assert this here, as will never be false here	
				});
			
			typeStep.createPropertyType(CoreProperties.qaState.toString(), Cardinality.MAP, ConstraintWrapper.getOrCreateDesignSpaceCoreSchema(ws));
			return typeStep;
		}
	}
	
	public static String getDataMappingId(Map.Entry<String,String> ioMapping, StepDefinition sd) {
		String procId = sd.getProcess() != null ? sd.getProcess().getName() : "";
		return CRD_DATAMAPPING_PREFIX+ioMapping.getKey()+"_"+sd.getName()+"_"+procId;
	}
	
	public static String getQASpecId(QAConstraintSpec spec, ProcessDefinition context) {
		return CRD_QASPEC_PREFIX+spec.getQaConstraintId()+"_"+context.getName();
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
			Instance instance = ws.createInstance(getOrCreateDesignSpaceInstanceType(ws, sd), sd.getName()+"_"+UUID.randomUUID());
			ProcessStep step = WrapperCache.getWrappedInstance(ProcessStep.class, instance);
			step.setProcess(scope);
			step.init(ws, sd, inDNI, outDNI);
			return step;
		}
	}

	protected void init(Workspace ws, StepDefinition sd, DecisionNodeInstance inDNI, DecisionNodeInstance outDNI) {
		instance.getPropertyAsSingle(CoreProperties.processedPreCondFulfilled.toString()).set(false);
		instance.getPropertyAsSingle(CoreProperties.processedPostCondFulfilled.toString()).set(false);
		instance.getPropertyAsSingle(CoreProperties.processedCancelCondFulfilled.toString()).set(false);
		instance.getPropertyAsSingle(CoreProperties.isWorkExpected.toString()).set(true);
		
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
		if (/*DEL-UPDATE: sd.getExpectedInput().isEmpty() &&*/ sd.getCondition(Conditions.PRECONDITION).isEmpty()) {
			this.setPreConditionsFulfilled(true);
		}
		ProcessDefinition pd = sd.getProcess() !=null ? sd.getProcess() : (ProcessDefinition)sd;
		sd.getQAConstraints().stream()
		.forEach(spec -> { 
			String qid = getQASpecId(spec, pd);
			//qaState.put(qid, ConstraintWrapper.getInstance(ws, spec, getProcess().getCurrentTimestamp(), this.getProcess()));
			ConstraintWrapper cw = ConstraintWrapper.getInstance(ws, spec, getProcess().getCurrentTimestamp(), this.getProcess());
			instance.getPropertyAsMap(CoreProperties.qaState.toString()).put(qid, cw.getInstance());
		});
	}
	
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
		
		
		String cond = String.format("[Pre: %s |Post: %s |Canc: %s |QAok: %s |Unsafe: %s |Premature: %s]", arePreCondFulfilled(), arePostCondFulfilled(), areCancelCondFulfilled(), areQAconstraintsFulfilled(), isInUnsafeOperationModeDueTo().size(), isInPrematureOperationModeDueTo().size());
		
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
