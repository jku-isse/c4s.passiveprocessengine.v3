package at.jku.isse.passiveprocessengine.instance.activeobjects;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import at.jku.isse.passiveprocessengine.core.ProcessContext;
import at.jku.isse.passiveprocessengine.core.PPEInstance;
import at.jku.isse.passiveprocessengine.core.PropertyChange;
import at.jku.isse.passiveprocessengine.core.RuleDefinition;
import at.jku.isse.passiveprocessengine.core.RuleResult;
import at.jku.isse.passiveprocessengine.definition.activeobjects.DecisionNodeDefinition;
import at.jku.isse.passiveprocessengine.definition.activeobjects.ProcessDefinition;
import at.jku.isse.passiveprocessengine.definition.activeobjects.StepDefinition;
import at.jku.isse.passiveprocessengine.instance.StepLifecycle.Conditions;
import at.jku.isse.passiveprocessengine.instance.StepLifecycle.State;
import at.jku.isse.passiveprocessengine.instance.StepLifecycle.Trigger;
import at.jku.isse.passiveprocessengine.instance.factories.DecisionNodeInstanceFactory;
import at.jku.isse.passiveprocessengine.instance.factories.ProcessStepInstanceFactory;
import at.jku.isse.passiveprocessengine.instance.messages.Commands.PrematureStepTriggerCmd;
import at.jku.isse.passiveprocessengine.instance.messages.Commands.ProcessScopedCmd;
import at.jku.isse.passiveprocessengine.instance.messages.Events;
import at.jku.isse.passiveprocessengine.instance.messages.Events.ProcessChangedEvent;
import at.jku.isse.passiveprocessengine.instance.messages.Responses;
import at.jku.isse.passiveprocessengine.instance.messages.Responses.IOResponse;
import at.jku.isse.passiveprocessengine.instance.types.AbstractProcessStepType;
import at.jku.isse.passiveprocessengine.instance.types.SpecificProcessInstanceType;
import lombok.extern.slf4j.Slf4j;


@Slf4j
public class ProcessInstance extends ProcessStep {

	protected transient ZonedDateTime createdAt;
	private transient ProcessStepInstanceFactory stepFactory;
	private transient DecisionNodeInstanceFactory decisionNodeFactory;

	public ProcessInstance(PPEInstance instance, ProcessContext context) {
		super(instance, context);
		setCreatedAt(getCurrentTimestamp());
	}

	// only to be used by factory
	public void inject(ProcessStepInstanceFactory stepFactory, DecisionNodeInstanceFactory decisionNodeFactory) {
		this.stepFactory = stepFactory;
		this.decisionNodeFactory = decisionNodeFactory;
	}
	
	public ZonedDateTime getCurrentTimestamp() {
		return ZonedDateTime.now(); //default value, to be replaced with time provider
	}

	public ZonedDateTime getCreatedAt() {
		if (createdAt == null) { // load from DS
			String last = instance.getTypedProperty(SpecificProcessInstanceType.CoreProperties.createdAt.toString(), String.class);
			createdAt = ZonedDateTime.parse(last);
		}
		return createdAt;
	}

	private void setCreatedAt(ZonedDateTime createdAt) {
		instance.setSingleProperty(SpecificProcessInstanceType.CoreProperties.createdAt.toString(), createdAt.toString());
		this.createdAt = createdAt;
	}

	@Override
	public ProcessScopedCmd prepareRuleEvaluationChange(RuleResult ruleResult, PropertyChange.Set op) {
		RuleDefinition crt = (RuleDefinition)ruleResult.getInstanceType();
		if (crt.getName().startsWith(SpecificProcessInstanceType.CRD_PREMATURETRIGGER_PREFIX) ) {
			log.debug(String.format("Queuing execution of Premature Trigger of step %s , trigger is now %s ", crt.getName(), op.getValue().toString()));
			StepDefinition sd = getDefinition().getStepDefinitionForPrematureConstraint(crt.getName());
			if (this.getProcessSteps().stream().anyMatch(step -> step.getDefinition().equals(sd)))
				return null; // as we already have that step instantiated, no need to create further cmds
			else
				return new PrematureStepTriggerCmd(sd, this, Boolean.valueOf(op.getValue().toString()));
		} else
			return super.prepareRuleEvaluationChange(ruleResult, op);
	}

	public ProcessStep createAndWireTask(StepDefinition sd) {
    	DecisionNodeInstance inDNI = getOrCreateDNI(sd.getInDND());
    	DecisionNodeInstance outDNI = getOrCreateDNI(sd.getOutDND());
    	if (getProcessSteps().stream().noneMatch(t -> t.getDefinition().getName().equals(sd.getName()))) {
        	ProcessStep step = stepFactory.getInstance(sd, inDNI, outDNI, this);
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
		return  context.getWrappedInstance(ProcessDefinition.class, instance.getTypedProperty(SpecificProcessInstanceType.CoreProperties.processDefinition.toString(), PPEInstance.class));
	}

	@Override
	public DecisionNodeInstance getInDNI() {
			PPEInstance inst = instance.getTypedProperty(AbstractProcessStepType.CoreProperties.inDNI.toString(), PPEInstance.class);
			if (inst != null)
				return context.getWrappedInstance(DecisionNodeInstance.class, inst);
			else
				return null;
	}

	@Override
	public DecisionNodeInstance getOutDNI() {
		PPEInstance inst = instance.getTypedProperty(AbstractProcessStepType.CoreProperties.outDNI.toString(), PPEInstance.class);
		if (inst != null)
			return context.getWrappedInstance(DecisionNodeInstance.class, inst);
		else
			return null;
	}

	@Override
	public Responses.IOResponse removeInput(String inParam, PPEInstance artifact) {
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
	public Responses.IOResponse addInput(String inParam, PPEInstance artifact) {
		IOResponse isOk = super.addInput(inParam, artifact);
		if (isOk.getError() == null) {
			// now see if we need to map this to first DNI - we assume all went well
			getDecisionNodeInstances().stream()
			.filter(dni -> this.isImmediateInstantiateAllStepsEnabled() || dni.getInSteps().size() == 0)
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
			instance.setSingleProperty(AbstractProcessStepType.CoreProperties.processedPostCondFulfilled.toString(), isfulfilled);
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
			instance.setSingleProperty(AbstractProcessStepType.CoreProperties.processedPreCondFulfilled.toString(), isfulfilled);
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
			instance.setSingleProperty(AbstractProcessStepType.CoreProperties.processedPostCondFulfilled.toString(), true);
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
		instance.getTypedProperty(SpecificProcessInstanceType.CoreProperties.stepInstances.toString(), Set.class).add(step.getInstance());
	}

	@SuppressWarnings("unchecked")
	public Set<ProcessStep> getProcessSteps() {
		Set<?> stepList = instance.getTypedProperty(SpecificProcessInstanceType.CoreProperties.stepInstances.toString(), Set.class);
		if (stepList != null) {
			return (Set<ProcessStep>) stepList.stream()
					.map(inst -> getProcessContext().getWrappedInstance(SpecificProcessInstanceType.getMostSpecializedClass((PPEInstance) inst), (PPEInstance) inst))
					.map(obj -> (ProcessStep)obj)
					.collect(Collectors.toSet());
		} else return Collections.emptySet();

	}

	@SuppressWarnings("unchecked")
	public Set<DecisionNodeInstance> getDecisionNodeInstances() {
		Set<?> dniSet = instance.getTypedProperty(SpecificProcessInstanceType.CoreProperties.decisionNodeInstances.toString(), Set.class);
		if (dniSet != null ) {
			return (Set<DecisionNodeInstance>) dniSet.stream()
					.map(inst -> context.getWrappedInstance(DecisionNodeInstance.class, (PPEInstance) inst))
					.map(obj -> (DecisionNodeInstance)obj)
					.collect(Collectors.toSet());
		} else 
			return Collections.emptySet();
	}

	@SuppressWarnings("unchecked")
	private void addDecisionNodeInstance(DecisionNodeInstance dni) {
		instance.getTypedProperty(SpecificProcessInstanceType.CoreProperties.decisionNodeInstances.toString(), Set.class).add(dni.getInstance());
	}

	public Set<DecisionNodeInstance> getInstantiatedDNIsHavingStepsOutputAsInput(ProcessStep step, String output) {
		String stepType = step.getDefinition().getName();
		return this.getDecisionNodeInstances().stream()
			.filter(dni -> dni.getDefinition().getMappings().stream()
					.anyMatch(md -> md.getFromStepType().equals(stepType) && md.getFromParameter().equals(output)))
			.collect(Collectors.toSet());
	}

	public boolean isImmediateDataPropagationEnabled() {
		if (getProcess() == null)
			return getDefinition() != null ? getDefinition().isImmediateDataPropagationEnabled() : false;
		else
			return getProcess().isImmediateDataPropagationEnabled();
	}

	public boolean isImmediateInstantiateAllStepsEnabled() {
		if (getProcess() == null)
			return getDefinition() != null ? getDefinition().isImmediateInstantiateAllStepsEnabled() : false;
		else
			return getProcess().isImmediateInstantiateAllStepsEnabled();
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

//	@Override //SHOULD NOT BE NECESSARY ANYMORE
//	public List<ProcessScopedCmd> ensureRuleToStateConsistency() {
//		List<ProcessScopedCmd> incons = new LinkedList<>();
//		incons.addAll(super.ensureRuleToStateConsistency());
//		incons.addAll(getProcessSteps().stream()
//				.flatMap(step -> step.ensureRuleToStateConsistency().stream())
//				.collect(Collectors.toList()));
//		return incons;
//	}

//	public static List<ProcessDefinitionError> getConstraintValidityStatus(Workspace ws, ProcessDefinition pd) {
//		List<ProcessDefinitionError> errors = new LinkedList<>();
//		errors.addAll(pd.checkConstraintValidity());
//		InstanceType instType = getOrCreateDesignSpaceInstanceType(ws, pd);
//		//premature constraints:
//		pd.getPrematureTriggers().entrySet().stream()
//			.forEach(entry -> {
//				String ruleId = generatePrematureRuleName(entry.getKey(), pd);
//				ConsistencyRuleType crt = ConsistencyRuleType.consistencyRuleTypeExists(ws,  ruleId, instType, entry.getValue());
//				if (crt == null) {
//					log.error("Expected Rule for existing process not found: "+ruleId);
//					errors.add(new ProcessDefinitionError(pd, "Expected Premature Trigger Rule Not Found - Internal Data Corruption", ruleId));
//				} else
//					if (crt.hasRuleError())
//						errors.add(new ProcessDefinitionError(pd, String.format("Premature Trigger Rule % has an error", ruleId), crt.ruleError()));
//			});
//		return errors;
//	}

//	public static InstanceType getOrCreateDesignSpaceInstanceType(Workspace ws, ProcessDefinition td) {
//		String parentName = td.getProcess() != null ? td.getProcess().getName() : "ROOT";
//		String name = SpecificProcessInstanceType.typeId+td.getName()+parentName;
////		Optional<InstanceType> thisType = ws.debugInstanceTypes().stream()
////				.filter(it -> !it.isDeleted)
////				.filter(it -> it.name().equals(designspaceTypeId+td.getName()))
////				.findAny();
//		Optional<InstanceType> thisType = Optional.ofNullable(ws.TYPES_FOLDER.instanceTypeWithName(name));
//		if (thisType.isPresent())
//			return thisType.get();
//		else {
//			InstanceType typeStep = ws.createInstanceType(name, ws.TYPES_FOLDER, AbstractProcessStepType.ProcessStep.getOrCreateDesignSpaceInstanceType(ws, td, null)); // here we dont know the parent process, if any at all, so we cant override the type, this needs to be done by the parent process
//			typeStep.createPropertyType(SpecificProcessInstanceType.CoreProperties.processDefinition.toString(), Cardinality.SINGLE, ProcessDefinition.getOrCreateDesignSpaceCoreSchema(ws));
//			typeStep.createPropertyType(SpecificProcessInstanceType.CoreProperties.stepInstances.toString(), Cardinality.SET, AbstractProcessStepType.ProcessStep.getOrCreateDesignSpaceCoreSchema(ws));
//			typeStep.createPropertyType(SpecificProcessInstanceType.CoreProperties.decisionNodeInstances.toString(), Cardinality.SET, DecisionNodeInstance.getOrCreateDesignSpaceCoreSchema(ws));
//			typeStep.createPropertyType(SpecificProcessInstanceType.CoreProperties.createdAt.toString(), Cardinality.SINGLE, Workspace.STRING);
//			return typeStep;
//		}
//	}

	

//	public static ProcessInstance getInstance(Workspace ws, ProcessDefinition sd) {
//		return getInstance(ws, sd, UUID.randomUUID().toString());
//	}

//	public static ProcessInstance getInstance(Workspace ws, ProcessDefinition sd, String namePostfix) {
//		//TODO: not to create duplicate process instances somehow
//		Instance instance = ws.createInstance(getOrCreateDesignSpaceInstanceType(ws, sd), sd.getName()+"_"+namePostfix);
//		ProcessInstance pi = context.getWrappedInstance(ProcessInstance.class, instance);
//		pi.init(sd, null, null, ws);
//		return pi;
//	}
//
//	public static ProcessInstance getSubprocessInstance(Workspace ws, ProcessDefinition sd, DecisionNodeInstance inDNI, DecisionNodeInstance outDNI, ProcessInstance scope) {
//		Instance instance = ws.createInstance(getOrCreateDesignSpaceInstanceType(ws, sd), sd.getName()+"_"+UUID.randomUUID());
//		ProcessInstance pi = Context.getWrappedInstance(ProcessInstance.class, instance);
//		pi.setProcess(scope);
//		pi.init(sd, inDNI, outDNI, ws);
//		return pi;
//	}




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
