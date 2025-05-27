package at.jku.isse.passiveprocessengine.instance.activeobjects;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.jena.ontapi.model.OntIndividual;

import com.github.oxo42.stateless4j.StateMachine;

import at.jku.isse.artifacteventstreaming.rule.definition.RDFRuleDefinition;
import at.jku.isse.designspace.rule.arl.evaluator.RuleDefinition;
import at.jku.isse.passiveprocessengine.rdfwrapper.RDFElement;
import at.jku.isse.passiveprocessengine.rdfwrapper.RDFInstance;
import at.jku.isse.passiveprocessengine.rdfwrapper.RDFInstanceType;
import at.jku.isse.passiveprocessengine.definition.activeobjects.StepDefinition;
import at.jku.isse.passiveprocessengine.definition.factories.SpecificProcessInstanceTypesFactory;
import at.jku.isse.passiveprocessengine.instance.StepLifecycle;
import at.jku.isse.passiveprocessengine.instance.StepLifecycle.Conditions;
import at.jku.isse.passiveprocessengine.instance.StepLifecycle.State;
import at.jku.isse.passiveprocessengine.instance.StepLifecycle.Trigger;
import at.jku.isse.passiveprocessengine.instance.messages.Commands.ConditionChangedCmd;
import at.jku.isse.passiveprocessengine.instance.messages.Commands.OutputChangedCmd;
import at.jku.isse.passiveprocessengine.instance.messages.Commands.ProcessScopedCmd;
import at.jku.isse.passiveprocessengine.instance.messages.Events;
import at.jku.isse.passiveprocessengine.instance.messages.Events.ProcessChangedEvent;
import at.jku.isse.passiveprocessengine.instance.messages.Responses;
import at.jku.isse.passiveprocessengine.instance.messages.Responses.IOResponse;
import at.jku.isse.passiveprocessengine.instance.types.AbstractProcessStepType;
import at.jku.isse.passiveprocessengine.instance.types.SpecificProcessStepType;
import at.jku.isse.passiveprocessengine.rdfwrapper.events.PropertyChange;
import at.jku.isse.passiveprocessengine.rdfwrapper.rule.RDFRuleResultWrapper;
import at.jku.isse.passiveprocessengine.rdfwrapper.rule.RuleEnabledResolver;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ProcessStep extends ProcessInstanceScopedElement{

	private StateMachine<StepLifecycle.State, StepLifecycle.Trigger> actualSM;
	private StateMachine<StepLifecycle.State, StepLifecycle.Trigger> expectedSM;
	protected boolean priorQAfulfilled = false;
	
	public ProcessStep(@NonNull OntIndividual element, @NonNull RDFInstanceType type, @NonNull RuleEnabledResolver context) {
		super(element, type, context);
		initState();
	}



	protected void initState() {
		if (this.getName().startsWith(StepDefinition.NOOPSTEP_PREFIX)) { // assumes/expects no pre/post cond and no qa
			actualSM = StepLifecycle.buildActualStatemachineInState(State.COMPLETED);
			setSingleProperty(AbstractProcessStepType.CoreProperties.actualLifecycleState.toString(),actualSM.getState().toString());
			expectedSM = StepLifecycle.buildExpectedStatemachineInState(State.COMPLETED);
			setSingleProperty(AbstractProcessStepType.CoreProperties.expectedLifecycleState.toString(),expectedSM.getState().toString());
			priorQAfulfilled = true;
		} else {

			String actState = getTypedProperty(AbstractProcessStepType.CoreProperties.actualLifecycleState.toString(), String.class);
			if (actState == null) {
				actualSM = StepLifecycle.buildActualStatemachineInState(State.AVAILABLE);
				setSingleProperty(AbstractProcessStepType.CoreProperties.actualLifecycleState.toString(),actualSM.getState().toString());
			} else { // state already set, now just init FSM
				actualSM = StepLifecycle.buildActualStatemachineInState(State.valueOf(actState));
			}

			String expState = getTypedProperty(AbstractProcessStepType.CoreProperties.expectedLifecycleState.toString(), String.class);
			if (expState == null) {
				expectedSM = StepLifecycle.buildExpectedStatemachineInState(State.AVAILABLE);
				setSingleProperty(AbstractProcessStepType.CoreProperties.expectedLifecycleState.toString(),expectedSM.getState().toString());
			} else { // state already set, now just init FSM
				expectedSM = StepLifecycle.buildExpectedStatemachineInState(State.valueOf(expState));
			}
			priorQAfulfilled = areConstraintsFulfilled(AbstractProcessStepType.CoreProperties.qaState.toString());
		}
	}

	public ProcessScopedCmd prepareRuleEvaluationChange(RDFRuleResultWrapper cr, PropertyChange.Set op) {
		// now here we have to distinguish what this evaluation change implies
		var crt = cr.getEvalWrapper().getDefinition();
		Conditions cond = SpecificProcessInstanceTypesFactory.getConditionFromURI(crt.getRuleDefinition().getURI());
		if (cond != null ) {
			String value = op.getValue() != null ? op.getValue().toString() : "NULL";
			log.debug(String.format("Step %s has %s condition evaluate to %s", this.getName(), cond, value));
			return new ConditionChangedCmd(this, cr, cond, Boolean.valueOf(op.getValue().toString()));
		} else {
//			if (crt.getName().startsWith(SpecificProcessInstanceTypesFactory.CRD_QASPEC_PREFIX) ) { // a qa constraint
//				log.debug(String.format("QA Constraint %s now %s ", crt.getName(), op.getValue() != null ? op.getValue().toString() : "NULL"));
//				return op.getValue() != null ? new QAConstraintChangedCmd(this, cr, Boolean.parseBoolean(op.getValue().toString())) :
//					new QAConstraintChangedCmd(this, cr, true);
//			}	else
				log.debug(String.format("Step %s has rule %s evaluate to %s", this.getName(), crt.getName(), op.getValue().toString()));
		}
		return null;
	}


	public ProcessScopedCmd prepareIOAddEvent(PropertyChange.Add op) { //List<Events.ProcessChangedEvent>
		// if in added, establish if this resembles unexpected late input
		if (op.getPropertyURI().getFragment().startsWith(SpecificProcessStepType.PREFIX_IN)
				&& ( this.getActualLifecycleState().equals(State.ACTIVE)
					|| this.getActualLifecycleState().equals(State.COMPLETED) )) {
			//(if so, then do something about this)			
			RDFElement added = op.getElement();
			log.info(String.format("Step %s received late input %s %s", this.getName(), op.getPropertyURI(), added.getName()  ));
			// Note that the adding has already happened, thus there is nothing to report back, this is only for checking whether we need to do something else as well.
		}
		else if (op.getPropertyURI().getFragment().startsWith(SpecificProcessStepType.PREFIX_OUT)) { // if out added, establish if this is late output, then propagate further
				//&& ( this.getActualLifecycleState().equals(State.COMPLETED) || isImmediateDataPropagationEnabled() ) ){
			if (this.getActualLifecycleState().equals(State.COMPLETED)) {
				RDFElement added = op.getElement();
				log.info(String.format("Step %s received late output %s %s, queuing for propagation to successors", this.getName(), op.getPropertyURI(), added.getName()  ));
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

	public ProcessScopedCmd prepareIORemoveEvent(PropertyChange.Remove op) { //List<Events.ProcessChangedEvent>
		// if in removed, establish if this resembles unexpected late removeal
		if (op.getPropertyURI().getFragment().startsWith(SpecificProcessStepType.PREFIX_IN)
				&& ( this.getActualLifecycleState().equals(State.ACTIVE)
					|| this.getActualLifecycleState().equals(State.COMPLETED) )) {
			//(if so, then do something about this)
			log.info(String.format("Step %s had some input removed from %s after step start", this.getName(), op.getPropertyURI()));
		}
		else if (op.getPropertyURI().getFragment().startsWith(SpecificProcessStepType.PREFIX_OUT)) { // if out removed, establish if this is late output removal, then propagate further
				//&& ( this.getActualLifecycleState().equals(State.COMPLETED) || isImmediateDataPropagationEnabled() ) ){

			if (this.getActualLifecycleState().equals(State.COMPLETED)) {
				log.debug(String.format("Step %s had some output removed from %s after step completion, queuing for propagation to successors", this.getName(), op.getPropertyURI()));
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


	public List<Events.ProcessChangedEvent> processQAEvent(RDFRuleResultWrapper cr, boolean fulfilled) {
		String id = cr.getEvalWrapper().getDefinition().getRuleDefinition().getURI();
		//ConstraintWrapper cw = qaState.get(id);
		//in one occasion found null instance in map, which should not happen!!!
		var cw = (ConstraintResultWrapper) getTypedProperty(AbstractProcessStepType.CoreProperties.qaState.toString(), Map.class).get(id);		
		cw.setRuleResultIfEmpty(cr);
		//cw.setEvalResult(fulfilled);
		cw.setLastChanged(getParentProcessOrThisIfProcessElseNull().getCurrentTimestamp());
		List<Events.ProcessChangedEvent> qaChanges = new LinkedList<>();
		qaChanges.add(new Events.QAConstraintFulfillmentChanged(this.getParentProcessOrThisIfProcessElseNull(), this, cw));
		boolean newQaState = areConstraintsFulfilled(AbstractProcessStepType.CoreProperties.qaState.toString()); // are all QA checks now fulfilled?
		if (priorQAfulfilled != newQaState) { // a change in qa fulfillment that we might want to react to
			priorQAfulfilled = newQaState;
			qaChanges.add(new Events.QAFulfillmentChanged(this.getParentProcessOrThisIfProcessElseNull(), this, newQaState));
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

	@SuppressWarnings("unchecked")
	public Set<ConstraintResultWrapper> getQAstatus() {
		return (Set<ConstraintResultWrapper>) getTypedProperty(AbstractProcessStepType.CoreProperties.qaState.toString(), Map.class).values().stream()
		.map(ConstraintResultWrapper.class::cast)
		.collect(Collectors.toSet());		
	}

	public boolean arePostCondFulfilled() {
		return getTypedProperty(AbstractProcessStepType.CoreProperties.processedPostCondFulfilled.toString(), Boolean.class);
	}

	public boolean arePreCondFulfilled() {
		return getTypedProperty(AbstractProcessStepType.CoreProperties.processedPreCondFulfilled.toString(), Boolean.class);
	}

	public boolean areCancelCondFulfilled() {
		return getTypedProperty(AbstractProcessStepType.CoreProperties.processedCancelCondFulfilled.toString(), Boolean.class);
	}

	public boolean areActivationCondFulfilled() {
		return getTypedProperty(AbstractProcessStepType.CoreProperties.processedActivationCondFulfilled.toString(), Boolean.class);
	}

	public boolean isWorkExpected() {
		return getTypedProperty(AbstractProcessStepType.CoreProperties.isWorkExpected.toString(), Boolean.class);
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
						if (step.areConstraintsFulfilled(AbstractProcessStepType.CoreProperties.qaState.toString())) // the have to be as otherwise we would have inflow fulfilled/resp exp and actual State Completed?!
							unsafeSteps.addAll(step.isInUnsafeOperationModeDueTo());
						else
							unsafeSteps.add(step);
					});

				} else { // not sufficient steps available: AND: not all, OR, none, XOR, none, hence here check all steps as all of them can/must be fulfilled eventually
					this.getInDNI().getInSteps().stream().forEach(step -> {
						if (step.areConstraintsFulfilled(AbstractProcessStepType.CoreProperties.qaState.toString()))
							unsafeSteps.addAll(step.isInUnsafeOperationModeDueTo());
						else
							unsafeSteps.add(step);
					});
				}
			} else {
				// when not propagated --> hence why might this have happened, get all insteps and check their QA constraints, if fine, ask them for isInUsafeOperation
				this.getInDNI().getInSteps().stream().forEach(step -> {
					if (step.areConstraintsFulfilled(AbstractProcessStepType.CoreProperties.qaState.toString()))
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

	@SuppressWarnings("unchecked")
	@Override
	public void deleteCascading() {
		// remove any lower-level instances this step is managing
		// DNIs are deleted at process level, not managed here
		//qaState.values().forEach(cw -> cw.deleteCascading());
		getTypedProperty(AbstractProcessStepType.CoreProperties.qaState.toString(), Map.class).values().stream()
		.map(ConstraintResultWrapper.class::cast)
		.forEach(cw -> ((ConstraintResultWrapper)cw).deleteCascading());		
		getTypedProperty(AbstractProcessStepType.CoreProperties.preconditions.toString(), Map.class).values().stream()
		.map(ConstraintResultWrapper.class::cast)
		.forEach(cw -> ((ConstraintResultWrapper)cw).deleteCascading());
		getTypedProperty(AbstractProcessStepType.CoreProperties.postconditions.toString(), Map.class).values().stream()
		.map(ConstraintResultWrapper.class::cast)
		.forEach(cw -> ((ConstraintResultWrapper)cw).deleteCascading());
		getTypedProperty(AbstractProcessStepType.CoreProperties.activationconditions.toString(), Map.class).values().stream()
		.map(ConstraintResultWrapper.class::cast)
		.forEach(cw -> ((ConstraintResultWrapper)cw).deleteCascading());
		getTypedProperty(AbstractProcessStepType.CoreProperties.cancelconditions.toString(), Map.class).values().stream()
		.map(ConstraintResultWrapper.class::cast)
		.forEach(cw -> ((ConstraintResultWrapper)cw).deleteCascading());
		// we are not deleting input and output artifacts as we are just referencing them!
		// finally delete self
		super.deleteCascading();
	}

	@Override
	public StepDefinition getDefinition() {
		return getTypedProperty(AbstractProcessStepType.CoreProperties.stepDefinition.toString(), StepDefinition.class);
	}

	//only to be used by InterStepMapper or other internal mechanisms
	public Responses.IOResponse removeInput(String inParam, RDFInstance artifact) {
		if (getDefinition().getExpectedInput().containsKey(inParam)) {
			String param = SpecificProcessStepType.PREFIX_IN+inParam;
			//if (instance.getInstanceType().getPropertyType(param).isAssignable(artifact)) {			
			//Property<?> prop = instance.getProperty(param);
			//if (prop.propertyType.isAssignable(artifact)) {
				getTypedProperty(param, Set.class).remove(artifact);
				return IOResponse.okResponse();
			//} else {
			//	String msg = String.format("Cannot remove input %s to %s with nonmatching artifact type %s of id % %s", inParam, this.getName(), artifact.getInstanceType().toString(), artifact.id(), artifact.getName());
			//	log.warn(msg);
			//	return IOResponse.errorResponse(msg);
			//}
		} else {
			// additionally Somehow notify about wrong param access
			String msg = String.format("Ignoring attempt to remove %s from nondefined input %s of %s ", artifact.getId(), inParam, this.getName());
			log.warn(msg);
			return IOResponse.errorResponse(msg);
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Set<RDFInstance> getInput(String inParam) {
		String param = SpecificProcessStepType.PREFIX_IN+inParam;		
		//SetProperty setP = instance.getPropertyAsSet(PREFIX_IN+param);
		if (getInstanceType().getPropertyType(param) == null) {
			//if (!instance.hasProperty("in_"+param))
			log.error(String.format("Attempt to access non-existing input %s in Step %s.", param, this.getName()));
			return Collections.emptySet();
		} else {
			return getTypedProperty(param, Set.class);
		}
	}

	@SuppressWarnings("unchecked")
	public Responses.IOResponse addInput(String inParam, RDFInstance artifact) {		
		if (getDefinition().getExpectedInput().containsKey(inParam)) {
			String param = SpecificProcessStepType.PREFIX_IN+inParam;
			if (getInstanceType().getPropertyType(param).isAssignable(artifact)) {
			//Property<?> prop = instance.getProperty(PREFIX_IN+inParam);
			//if (prop.propertyType.isAssignable(artifact)) {
				getTypedProperty(param, Set.class).add(artifact);
				return IOResponse.okResponse();
			} else {
				String msg = String.format("Cannot add input %s to %s with nonmatching artifact type %s of id %s %s", inParam, this.getName(), artifact.getInstanceType().toString(), artifact.getId(), artifact.getName());
				log.warn(msg);
				return IOResponse.errorResponse(msg);
			}
		} else {
			String msg = String.format("Ignoring attempt to add %s to undefined input %s of %s", artifact.getId(), inParam, this.getName());
			log.warn(msg);
			return IOResponse.errorResponse(msg);
		}
	}

	@SuppressWarnings("unchecked")
	public Set<RDFInstance> getOutput(String outParam) {
		String param = SpecificProcessStepType.PREFIX_OUT+outParam;
		if (getInstanceType().getPropertyType(param) == null) {
			log.error(String.format("Attempt to access non-existing output %s in Step %s.", param, this.getName()));
			return Collections.emptySet();
		}
		return getTypedProperty(param, Set.class);
	}

	@SuppressWarnings("unchecked")
	public Responses.IOResponse addOutput(String outParam, RDFInstance artifact) {
		if (getDefinition().getExpectedOutput().containsKey(outParam)) {
			String param = SpecificProcessStepType.PREFIX_OUT+outParam;
			//Property<?> prop = instance.getProperty();
			if (getInstanceType().getPropertyType(param).isAssignable(artifact)) {
				getTypedProperty(SpecificProcessStepType.PREFIX_OUT+param, Set.class).add(artifact);
				return IOResponse.okResponse();
			} else {
				String msg = String.format("Cannot add outnput %s to %s with nonmatching artifact type %s of id % %s", param, this.getName(), artifact.getInstanceType().toString(), artifact.getId(), artifact.getName());
				log.warn(msg);
				return IOResponse.errorResponse(msg);
			}
		} else {
			String msg = String.format("Ignoring attempt to add %s to undefined output %s of %s ", artifact.getId(), outParam, this.getName());
			log.warn(msg);
			return IOResponse.errorResponse(msg);
		}
	}

	//only to be used by InterStepMapper or other internal mechanisms
	public void removeOutput(String param, RDFInstance art) {
		getTypedProperty(SpecificProcessStepType.PREFIX_OUT+param, Set.class).remove(art);
	}

	public DecisionNodeInstance getInDNI() {
		return getTypedProperty(AbstractProcessStepType.CoreProperties.inDNI.toString(), DecisionNodeInstance.class);
	}

	public DecisionNodeInstance getOutDNI() {
		return getTypedProperty(AbstractProcessStepType.CoreProperties.outDNI.toString(), DecisionNodeInstance.class);
	}

	public State getExpectedLifecycleState() {
		return expectedSM.getState();
	}

	public State getActualLifecycleState() {
		return actualSM.getState();
	}

	@SuppressWarnings("unchecked")
	public boolean areConstraintsFulfilled(String constraintProperty) {
		// are there all constraint wrappers actually added already
//		if (this.getDefinition() == null)
//			return false;
//		int expQA = this.getDefinition().getQAConstraints().size(); //no longer needed as we create skeletons upon init
//		int actualQA = instance.getPropertyAsMap(constraintProperty).values().size();
//		if (expQA != actualQA)
//			return false; // as long as the expected QA is not the actual number of QA checks, the eval cant be true;
		return getTypedProperty(constraintProperty, Map.class).values().stream()
			.map(ConstraintResultWrapper.class::cast)
			.allMatch(cw -> ((ConstraintResultWrapper)cw).getEvalResult());
	}

	@SuppressWarnings("unchecked")
	public Set<ConstraintResultWrapper> getConstraints(String constraintProperty) {
		return (Set<ConstraintResultWrapper>) getTypedProperty(constraintProperty, Map.class).values().stream()
				.map(ConstraintResultWrapper.class::cast)
				.filter(Objects::nonNull)
				.map(ConstraintResultWrapper.class::cast)
				.collect(Collectors.toSet());
	}

	public List<Events.ProcessChangedEvent> setWorkExpected(boolean isExpected) {
		if (isWorkExpected() != isExpected ) {
			setSingleProperty(AbstractProcessStepType.CoreProperties.isWorkExpected.toString(), isExpected);
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
		if (this.getProcess() != null) { 
			return this.getProcess(); } 
		else { 
			return (ProcessInstance)this; //ugly hack if this is a process without parent		
		}
	}

	public List<Events.ProcessChangedEvent> processConditionsChanged(ConstraintResultWrapper cw) {
		List<ProcessChangedEvent> events;
		boolean newResult = cw.getEvalResult();
		switch(cw.getConstraintSpec().getConditionType()) {
		case ACTIVATION:
			events = processActivationConditionsChange(cw.getRuleResult(), newResult);
			break;
		case CANCELATION:
			events = processCancelConditionsChange(cw.getRuleResult(), newResult);
			break;
		case POSTCONDITION:
			events = processPostConditionsChange(cw.getRuleResult(), newResult);
			break;
		case PRECONDITION:
			events = processPreConditionsChange(cw.getRuleResult(), newResult);
			break;
		case QA:
			events = processQAEvent(cw.getRuleResult(), newResult);
			break;
		case DATAMAPPING: //fallthrough
		default:
			// not supported for setting directly
			events = Collections.emptyList();
			break;
		}
		return events;
	}

	public List<Events.ProcessChangedEvent> processPostConditionsChange(RDFRuleResultWrapper cr, boolean isfulfilled) {
		String id = cr.getEvalWrapper().getDefinition().getRuleDefinition().getURI();
		var cw = (ConstraintResultWrapper) getTypedProperty(AbstractProcessStepType.CoreProperties.postconditions.toString(), Map.class).get(id);		
		cw.setRuleResultIfEmpty(cr);
		cw.setLastChanged(getParentProcessOrThisIfProcessElseNull().getCurrentTimestamp());
		boolean newState = areConstraintsFulfilled(AbstractProcessStepType.CoreProperties.postconditions.toString());
		List<Events.ProcessChangedEvent> events =  setPostConditionsFulfilled(newState);
		if (events.isEmpty())
			return List.of(new Events.PartialConditionFulfillmentChanged(getParentProcessOrThisIfProcessElseNull(), this, Conditions.POSTCONDITION, isfulfilled, cr.getName()));
		else
			return events;
	}

	protected List<Events.ProcessChangedEvent> setPostConditionsFulfilled(boolean isfulfilled) {
		if (arePostCondFulfilled() != isfulfilled) { // a change
			List<Events.ProcessChangedEvent> events = new LinkedList<>();
			ProcessInstance pi = getParentProcessOrThisIfProcessElseNull();
			events.add(new Events.ConditionFulfillmentChanged(pi, this, Conditions.POSTCONDITION, isfulfilled));
			setSingleProperty(AbstractProcessStepType.CoreProperties.processedPostCondFulfilled.toString(),isfulfilled);
			if (isfulfilled && areConstraintsFulfilled(AbstractProcessStepType.CoreProperties.qaState.toString()) && arePreCondFulfilled())
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


	public List<Events.ProcessChangedEvent> processPreConditionsChange(RDFRuleResultWrapper cr, boolean isfulfilled) {
		String id = cr.getEvalWrapper().getDefinition().getRuleDefinition().getURI();
		var cw = (ConstraintResultWrapper) getTypedProperty(AbstractProcessStepType.CoreProperties.preconditions.toString(), Map.class).get(id);
		cw.setRuleResultIfEmpty(cr);
		cw.setLastChanged(getParentProcessOrThisIfProcessElseNull().getCurrentTimestamp());
		boolean newState = areConstraintsFulfilled(AbstractProcessStepType.CoreProperties.preconditions.toString());
		List<Events.ProcessChangedEvent> events =  setPreConditionsFulfilled(newState);
		if (events.isEmpty())
			return List.of(new Events.PartialConditionFulfillmentChanged(getParentProcessOrThisIfProcessElseNull(), this, Conditions.PRECONDITION, isfulfilled, cr.getId()));
		else
			return events;
	}

	//not to be used outside of ProcessStepInstanceFactory
	public List<Events.ProcessChangedEvent> setPreConditionsFulfilled(boolean isfulfilled) {
		if (arePreCondFulfilled() != isfulfilled) {  // a change
			List<Events.ProcessChangedEvent> events = new LinkedList<>();
			ProcessInstance pi = getParentProcessOrThisIfProcessElseNull();
			events.add(new Events.ConditionFulfillmentChanged(pi, this, Conditions.PRECONDITION, isfulfilled));
			setSingleProperty(AbstractProcessStepType.CoreProperties.processedPreCondFulfilled.toString(),isfulfilled);
			if (isfulfilled)  {
				events.addAll(this.trigger(StepLifecycle.Trigger.ENABLE)) ;
				if (arePostCondFulfilled() && areConstraintsFulfilled(AbstractProcessStepType.CoreProperties.qaState.toString()))
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

	public List<Events.ProcessChangedEvent> processCancelConditionsChange(RDFRuleResultWrapper cr, boolean isfulfilled) {
		String id = cr.getEvalWrapper().getDefinition().getRuleDefinition().getURI();
		ConstraintResultWrapper cw = (ConstraintResultWrapper) getTypedProperty(AbstractProcessStepType.CoreProperties.cancelconditions.toString(), Map.class).get(id);
		cw.setRuleResultIfEmpty(cr);
		cw.setLastChanged(getParentProcessOrThisIfProcessElseNull().getCurrentTimestamp());
		boolean newState = areConstraintsFulfilled(AbstractProcessStepType.CoreProperties.cancelconditions.toString());
		List<Events.ProcessChangedEvent> events =  setCancelConditionsFulfilled(newState);
		if (events.isEmpty())
			return List.of(new Events.PartialConditionFulfillmentChanged(getParentProcessOrThisIfProcessElseNull(), this, Conditions.CANCELATION, isfulfilled, cr.getName()));
		else
			return events;
	}


	protected List<Events.ProcessChangedEvent> setCancelConditionsFulfilled(boolean isfulfilled) {
		if (areCancelCondFulfilled() != isfulfilled) {
			List<Events.ProcessChangedEvent> events = new LinkedList<>();
			ProcessInstance pi = getParentProcessOrThisIfProcessElseNull();
			events.add(new Events.ConditionFulfillmentChanged(pi, this, Conditions.CANCELATION, isfulfilled));
			setSingleProperty(AbstractProcessStepType.CoreProperties.processedCancelCondFulfilled.toString(),isfulfilled);
			if (isfulfilled)
					events.addAll(trigger(Trigger.CANCEL));
			else {
				events.addAll(trigger(Trigger.UNCANCEL));
			}
			return events;
		}
		return Collections.emptyList();
	}

	public List<Events.ProcessChangedEvent> processActivationConditionsChange(RDFRuleResultWrapper cr, boolean isFulfilled) {
		String id = cr.getEvalWrapper().getDefinition().getRuleDefinition().getURI();
		ConstraintResultWrapper cw = (ConstraintResultWrapper) getTypedProperty(AbstractProcessStepType.CoreProperties.activationconditions.toString(), Map.class).get(id);
		cw.setRuleResultIfEmpty(cr);
		cw.setLastChanged(getParentProcessOrThisIfProcessElseNull().getCurrentTimestamp());
		boolean newState = areConstraintsFulfilled(AbstractProcessStepType.CoreProperties.activationconditions.toString());
		List<Events.ProcessChangedEvent> events =  setActivationConditionsFulfilled(newState);
		if (events.isEmpty())
			return List.of(new Events.PartialConditionFulfillmentChanged(getParentProcessOrThisIfProcessElseNull(), this, Conditions.ACTIVATION, isFulfilled, cr.getName()));
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
				setSingleProperty(AbstractProcessStepType.CoreProperties.actualLifecycleState.toString(),actualSM.getState().toString());
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
				setSingleProperty(AbstractProcessStepType.CoreProperties.expectedLifecycleState.toString(),expectedSM.getState().toString());
				ProcessInstance pi = getParentProcessOrThisIfProcessElseNull();
				events.add(new Events.StepStateTransitionEvent(pi, this, prevExpState, expectedSM.getState(), false));
				switch(expectedSM.getState()) {
				case ENABLED:
					// handle deviation mitigation --> if actualSM==ACTIVE and expected transitions from Available to Enabled, then should continue to Active
					if (actualSM.getState().equals(State.ACTIVE)) {
						expectedSM.fire(Trigger.ACTIVATE);
						setSingleProperty(AbstractProcessStepType.CoreProperties.expectedLifecycleState.toString(),expectedSM.getState().toString());
						events.add(new Events.StepStateTransitionEvent(pi, this, State.ENABLED, expectedSM.getState(), false));
						// same fore COMPLETED
					} else if (actualSM.getState().equals(State.COMPLETED)) {
						expectedSM.fire(Trigger.MARK_COMPLETE);
						setSingleProperty(AbstractProcessStepType.CoreProperties.expectedLifecycleState.toString(),expectedSM.getState().toString());
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

	@Override
	public String toString() {
		String input = getDefinition().getExpectedInput().keySet().stream()
				.map(eIn -> eIn+"="+getInput(eIn).stream().map(art -> art.getName()).collect(Collectors.joining(",","[","]")) )
				.collect(Collectors.joining("; "));
		String output = getDefinition().getExpectedOutput().keySet().stream()
				.map(eIn -> eIn+"="+getOutput(eIn).stream().map(art -> art.getName()).collect(Collectors.joining(",","[","]")) )
				.collect(Collectors.joining("; "));
		String states = "E:"+expectedSM.getState()+"|A:"+actualSM.getState();
		String process = getProcess() != null ? getProcess().getName() : "NONE";
		String inDNI = getInDNI() != null ? getInDNI().getDefinition().getName() : "NONE" ;
		String outDNI = getOutDNI() != null ? getOutDNI().getDefinition().getName() : "NONE";


		String cond = String.format("[Pre: %s |Post: %s |Canc: %s |QAok: %s |Unsafe: %s |Premature: %s]", arePreCondFulfilled(), arePostCondFulfilled(), areCancelCondFulfilled(), areConstraintsFulfilled(AbstractProcessStepType.CoreProperties.qaState.toString()), isInUnsafeOperationModeDueTo().size(), isInPrematureOperationModeDueTo().size());

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
