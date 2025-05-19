package at.jku.isse.passiveprocessengine.instance.activeobjects;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.jena.ontapi.model.OntIndividual;

import at.jku.isse.passiveprocessengine.rdfwrapper.RDFInstance;
import at.jku.isse.passiveprocessengine.rdfwrapper.RDFInstanceType;
import at.jku.isse.passiveprocessengine.definition.activeobjects.DecisionNodeDefinition;
import at.jku.isse.passiveprocessengine.definition.activeobjects.ProcessDefinition;
import at.jku.isse.passiveprocessengine.definition.activeobjects.StepDefinition;
import at.jku.isse.passiveprocessengine.instance.StepLifecycle.Conditions;
import at.jku.isse.passiveprocessengine.instance.StepLifecycle.State;
import at.jku.isse.passiveprocessengine.instance.StepLifecycle.Trigger;
import at.jku.isse.passiveprocessengine.instance.factories.DecisionNodeInstanceFactory;
import at.jku.isse.passiveprocessengine.instance.factories.ProcessInstanceFactory;
import at.jku.isse.passiveprocessengine.instance.messages.Events;
import at.jku.isse.passiveprocessengine.instance.messages.Events.ProcessChangedEvent;
import at.jku.isse.passiveprocessengine.instance.messages.Responses;
import at.jku.isse.passiveprocessengine.instance.messages.Responses.IOResponse;
import at.jku.isse.passiveprocessengine.instance.types.AbstractProcessStepType;
import at.jku.isse.passiveprocessengine.instance.types.SpecificProcessInstanceType;
import at.jku.isse.passiveprocessengine.rdfwrapper.rule.RuleEnabledResolver;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;


@Slf4j
public class ProcessInstance extends ProcessStep {

	protected ZonedDateTime createdAt;
	private ProcessInstanceFactory stepFactory;
	private DecisionNodeInstanceFactory decisionNodeFactory;

	public ProcessInstance(@NonNull OntIndividual element, @NonNull RDFInstanceType type, @NonNull RuleEnabledResolver context) {
		super(element, type, context);
		if (getCreatedAt() == null) { // truely null, otherwise just loading from persistance layer
			setCreatedAt(getCurrentTimestamp());
		}
	}

	// only to be used by factory
	public void inject(ProcessInstanceFactory stepFactory, DecisionNodeInstanceFactory decisionNodeFactory) {
		this.stepFactory = stepFactory;
		this.decisionNodeFactory = decisionNodeFactory;
	}
	
	public ZonedDateTime getCurrentTimestamp() {
		return ZonedDateTime.now(); //default value, to be replaced with time provider
	}

	public ZonedDateTime getCreatedAt() {
		if (createdAt == null) { // load from DS
			String last = getTypedProperty(SpecificProcessInstanceType.CoreProperties.createdAt.toString(), String.class);
			if (last != null && last.length() > 0 ) {
				createdAt = ZonedDateTime.parse(last);
			} else {
				return null;
			}
		}
		return createdAt;
	}

	private void setCreatedAt(ZonedDateTime createdAt) {
		setSingleProperty(SpecificProcessInstanceType.CoreProperties.createdAt.toString(), createdAt.toString());
		this.createdAt = createdAt;
	}

	public ProcessStep createAndWireTask(StepDefinition sd) {
    	DecisionNodeInstance inDNI = getOrCreateDNI(sd.getInDND());
    	DecisionNodeInstance outDNI = getOrCreateDNI(sd.getOutDND());
    	if (getProcessSteps().stream().noneMatch(t -> t.getDefinition().getName().equals(sd.getName()))) {
        	ProcessStep step = stepFactory.getStepInstance(sd, inDNI, outDNI, this);
        	//step.setProcess(this);
        	if (step != null) {
        		this.addProcessStep(step);
        		return step;
        	}
        }
    	return null;
     }

	// only to be used by factory
    public DecisionNodeInstance getOrCreateDNI(DecisionNodeDefinition dnd) {
    	return this.getDecisionNodeInstances().stream()
    	.filter(dni -> dni.getDefinition().equals(dnd))
    	.findAny().orElseGet(() -> { DecisionNodeInstance dni = decisionNodeFactory.getInstance(dnd);
    				dni.setProcess(this);
    				this.addDecisionNodeInstance(dni);
    				return dni;
    	});
    }

	@Override
	public ProcessDefinition getDefinition() {
		return getTypedProperty(SpecificProcessInstanceType.CoreProperties.processDefinition.toString(), ProcessDefinition.class);		
	}

	@Override
	public DecisionNodeInstance getInDNI() {
		return getTypedProperty(AbstractProcessStepType.CoreProperties.inDNI.toString(), DecisionNodeInstance.class);			
	}

	@Override
	public DecisionNodeInstance getOutDNI() {
		return getTypedProperty(AbstractProcessStepType.CoreProperties.outDNI.toString(), DecisionNodeInstance.class);		
	}

	@Override
	public Responses.IOResponse removeInput(String inParam, RDFInstance artifact) {
		IOResponse isOk = super.removeInput(inParam, artifact);
		if (isOk.getError() == null) {
			// now see if we need to map this to first DNI - we assume all went well
			getDecisionNodeInstances().stream()
			.filter(dni -> dni.getInSteps().size() == 0)
			.forEach(dni -> {
				dni.checkAndExecuteDataMappings(false, false); //dni.signalPrevTaskDataChanged(this);
			});
		}
		return isOk;
	}

	@Override
	public Responses.IOResponse addInput(String inParam, RDFInstance artifact) {
		IOResponse isOk = super.addInput(inParam, artifact);
		if (isOk.getError() == null) {
			// now see if we need to map this to first DNI - we assume all went well
			getDecisionNodeInstances().stream()
			.filter(dni -> dni.getInSteps().isEmpty())
			//when all steps are immediately enabled trigger all dnis to propagate, just to be on the safe side, we would actually only need to trigger those that obtain data from this param at process level
			// otherwise just first
			.forEach(dni -> {
				dni.checkAndExecuteDataMappings(false, false);//dni.tryActivationPropagation(); // to trigger mapping to first steps
			});
		}
		return isOk;
	}

	protected List<Events.ProcessChangedEvent> signalChildStepStateChanged(ProcessStep step) {
		if (step.getActualLifecycleState().equals(State.ENABLED) &&  this.getDefinition().getPreconditions().isEmpty()) {
			if (this.getActualLifecycleState().equals(State.COMPLETED))
				return this.setActivationConditionsFulfilled(true); // we are back in an enabled state, let the process know that its not COMPLETED anymore
			else
				return this.setPreConditionsFulfilled(true); // ensure the process is also in an enabled state
		} else
			if (step.getActualLifecycleState().equals(State.ACTIVE) && !this.getActualLifecycleState().equals(State.ACTIVE)) {
				return this.setActivationConditionsFulfilled(true);
			} else if (step.getActualLifecycleState().equals(State.COMPLETED) && !this.getActualLifecycleState().equals(State.COMPLETED)) {
				// lets see if we can also progress to complete, or otherwise activate process
				return this.tryTransitionToCompleted();
			}
		return Collections.emptyList();
	}

	protected List<Events.ProcessChangedEvent> signalDNIChanged(DecisionNodeInstance dni) {
		// we do a combination of postconditions and steps status
		if (dni.isInflowFulfilled())
			return tryTransitionToCompleted();
		else
			return this.trigger(Trigger.ACTIVATE);


//		if (this.getDefinition().getCondition(Conditions.POSTCONDITION).isPresent()) // we have our own process specific post conditions
//			return Collections.emptyList(); // then we dont care about substep status, and rely only on post condition

//		if (!dni.isInflowFulfilled()) // something not ready yet, so we just cannot be ready yet, perhaps we are not ready anyway
//			return this.setPostConditionsFulfilled(false); //we can only do this as there is no explicit process-level postcondition specified
//		if (dni.isInflowFulfilled() && dni.getDefinition().getOutSteps().size() == 0 && this.areQAconstraintsFulfilled()) {
//			List<Events.ProcessChangedEvent> events = this.setPostConditionsFulfilled(true);
//			if (events.size() > 0)
//				return events;
//			else
//				return trigger(Trigger.MARK_COMPLETE); // the last DNI is fulfilled, and our conditions are all fulfilled
//		} else
//			return Collections.emptyList();
	}



	@Override
	protected List<ProcessChangedEvent> setPostConditionsFulfilled(boolean isfulfilled) {
		if (!isfulfilled) // regular step behavior for unfulfilled postcond
			return super.setPostConditionsFulfilled(false);
		// if now fulfilled, check with substeps
		List<Events.ProcessChangedEvent> events = new LinkedList<>();
		if (arePostCondFulfilled() != isfulfilled) { // a change
			ProcessInstance pi = getParentProcessOrThisIfProcessElseNull();
			events.add(new Events.ConditionFulfillmentChanged(pi, this, Conditions.POSTCONDITION, isfulfilled));
			setSingleProperty(AbstractProcessStepType.CoreProperties.processedPostCondFulfilled.toString(), isfulfilled);
			events.addAll(tryTransitionToCompleted());
		}
		return events;
	}

	@Override
	public List<Events.ProcessChangedEvent> setPreConditionsFulfilled(boolean isfulfilled) {
		if (arePreCondFulfilled() != isfulfilled) {  // a change
			List<Events.ProcessChangedEvent> events = new LinkedList<>();
			ProcessInstance pi = getParentProcessOrThisIfProcessElseNull();
			events.add(new Events.ConditionFulfillmentChanged(pi, this, Conditions.PRECONDITION, isfulfilled));
			setSingleProperty(AbstractProcessStepType.CoreProperties.processedPreCondFulfilled.toString(), isfulfilled);
			if (isfulfilled)  {
				events.addAll(this.trigger(Trigger.ENABLE)) ;
				events.addAll(tryTransitionToCompleted()) ;
			}
			else {
				//if (!actualSM.isInState(State.CANCELED)) // no need to check any longer as CANCELED state only reacts to uncancel triggers
				events.addAll(this.trigger(Trigger.RESET));
				// we stay in cancelled even if there are preconditions no longer fulfilled,
				// if we are no longer cancelled, and precond do not hold, then reset
			}
			return events;
		}
		return Collections.emptyList();
	}

	private List<Events.ProcessChangedEvent> tryTransitionToCompleted() {
		if (this.getDefinition().getPostconditions().isEmpty()) {
			setSingleProperty(AbstractProcessStepType.CoreProperties.processedPostCondFulfilled.toString(), true);
		}
		boolean areAllDNIsInflowFulfilled = this.getDecisionNodeInstances().stream().allMatch(dni -> dni.isInflowFulfilled());
		if (arePostCondFulfilled() && areConstraintsFulfilled(AbstractProcessStepType.CoreProperties.qaState.toString()) && arePreCondFulfilled() && areAllDNIsInflowFulfilled)
			return this.trigger(Trigger.MARK_COMPLETE) ;
		else
			return this.trigger(Trigger.ACTIVATE);
	}

	@SuppressWarnings("unchecked")
	private void addProcessStep(ProcessStep step) {
		assert(step != null);
		assert(step.getInstance() != null);
		getTypedProperty(SpecificProcessInstanceType.CoreProperties.stepInstances.toString(), Set.class).add(step.getInstance());
	}

	public Set<ProcessStep> getProcessSteps() {
		Set<?> stepList = getTypedProperty(SpecificProcessInstanceType.CoreProperties.stepInstances.toString(), Set.class);
		if (stepList != null) {
			return stepList.stream()
//					.map(inst -> getProcessContext().getWrappedInstance(SpecificProcessInstanceType.getMostSpecializedClass((RDFInstance) inst), (RDFInstance) inst))
//					.map(obj -> (ProcessStep)obj)
					.map(ProcessStep.class::cast)
					.collect(Collectors.toSet());
		} else return Collections.emptySet();

	}

	public Set<DecisionNodeInstance> getDecisionNodeInstances() {
		Set<?> dniSet = getTypedProperty(SpecificProcessInstanceType.CoreProperties.decisionNodeInstances.toString(), Set.class);
		if (dniSet != null ) {
			return dniSet.stream()
					.map(DecisionNodeInstance.class::cast)					
					.collect(Collectors.toSet());
		} else 
			return Collections.emptySet();
	}

	@SuppressWarnings("unchecked")
	private void addDecisionNodeInstance(DecisionNodeInstance dni) {
		getTypedProperty(SpecificProcessInstanceType.CoreProperties.decisionNodeInstances.toString(), Set.class).add(dni.getInstance());
	}

	public Set<DecisionNodeInstance> getInstantiatedDNIsHavingStepsOutputAsInput(ProcessStep step, String output) {
		String stepType = step.getDefinition().getName();
		return this.getDecisionNodeInstances().stream()
			.filter(dni -> dni.getDefinition().getMappings().stream()
					.anyMatch(md -> md.getFromStepType().equals(stepType) && md.getFromParameter().equals(output)))
			.collect(Collectors.toSet());
	}

	@Override
	public void deleteCascading() {
		// remove any lower-level instances this step is managing
		// DNIs and Steps
		getDecisionNodeInstances().forEach(dni -> dni.deleteCascading());
		getProcessSteps().forEach(step -> step.deleteCascading());
		// we are not deleting input and output artifacts as we are just referencing them!
		// TODO: should we delete configurations?
		// finally delete self via super call
		super.deleteCascading();
	}

	public void printProcessToConsole(String prefix) {

		System.out.println(prefix+this.toString());
		String nextIndent = "  "+prefix;
		this.getProcessSteps().stream().forEach(step -> {
			if (step instanceof ProcessInstance) {
				((ProcessInstance) step).printProcessToConsole(nextIndent);
			} else {

				System.out.println(nextIndent+step.toString());
			}
		});
		this.getDecisionNodeInstances().stream().forEach(dni -> System.out.println(nextIndent+dni.toString()));
	}
}
