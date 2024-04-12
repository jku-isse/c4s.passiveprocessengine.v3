package at.jku.isse.passiveprocessengine.instance;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import at.jku.isse.designspace.core.events.PropertyUpdateSet;
import at.jku.isse.designspace.core.model.Cardinality;
import at.jku.isse.designspace.core.model.Instance;
import at.jku.isse.designspace.core.model.InstanceType;
import at.jku.isse.designspace.core.model.SetProperty;
import at.jku.isse.designspace.core.model.Workspace;
import at.jku.isse.designspace.rule.model.ConsistencyRule;
import at.jku.isse.designspace.rule.model.ConsistencyRuleType;
import at.jku.isse.passiveprocessengine.InstanceWrapper;
import at.jku.isse.passiveprocessengine.WrapperCache;
import at.jku.isse.passiveprocessengine.definition.DecisionNodeDefinition;
import at.jku.isse.passiveprocessengine.definition.ProcessDefinition;
import at.jku.isse.passiveprocessengine.definition.StepDefinition;
import at.jku.isse.passiveprocessengine.instance.StepLifecycle.Conditions;
import at.jku.isse.passiveprocessengine.instance.StepLifecycle.State;
import at.jku.isse.passiveprocessengine.instance.StepLifecycle.Trigger;
import at.jku.isse.passiveprocessengine.instance.messages.Commands.PrematureStepTriggerCmd;
import at.jku.isse.passiveprocessengine.instance.messages.Commands.ProcessScopedCmd;
import at.jku.isse.passiveprocessengine.instance.messages.Events;
import at.jku.isse.passiveprocessengine.instance.messages.Events.ProcessChangedEvent;
import at.jku.isse.passiveprocessengine.instance.messages.Responses;
import at.jku.isse.passiveprocessengine.instance.messages.Responses.IOResponse;
import lombok.extern.slf4j.Slf4j;


@Slf4j
public class ProcessInstance extends ProcessStep {

	private static final String CRD_PREMATURETRIGGER_PREFIX = "crd_prematuretrigger_";

	static enum CoreProperties {stepInstances, decisionNodeInstances, processDefinition, createdAt};
	
	public static final String designspaceTypeId = ProcessInstance.class.getSimpleName();

	protected ZonedDateTime createdAt;
		
	public ProcessInstance(Instance instance) {
		super(instance);
	}			
	
	public ZonedDateTime getCurrentTimestamp() {
		return ZonedDateTime.now(); //default value, to be replaced with time provider
	}
	
	public ZonedDateTime getCreatedAt() {
		if (createdAt == null) { // load from DS
			String last = (String) instance.getPropertyAsValue(CoreProperties.createdAt.toString());
			createdAt = ZonedDateTime.parse(last);
		}
		return createdAt;
	}
	
	private void setCreatedAt(ZonedDateTime createdAt) {
		instance.getPropertyAsSingle(CoreProperties.createdAt.toString()).set(createdAt.toString());
		this.createdAt = createdAt;
	}
	
	public ProcessScopedCmd prepareRuleEvaluationChange(ConsistencyRule cr, PropertyUpdateSet op) {
		ConsistencyRuleType crt = (ConsistencyRuleType)cr.getInstanceType();
		if (crt.name().startsWith(CRD_PREMATURETRIGGER_PREFIX) ) { 
			log.debug(String.format("Queuing execution of Premature Trigger of step %s , trigger is now %s ", crt.name(), op.value().toString()));
			StepDefinition sd = getDefinition().getStepDefinitionForPrematureConstraint(crt.name());
			if (this.getProcessSteps().stream().anyMatch(step -> step.getDefinition().equals(sd)))
				return null; // as we already have that step instantiated, no need to create further cmds
			else
				return new PrematureStepTriggerCmd(sd, this, Boolean.valueOf(op.value().toString()));
		} else
			return super.prepareRuleEvaluationChange(cr, op);
	}

	public ProcessStep createAndWireTask(StepDefinition sd) {
    	DecisionNodeInstance inDNI = getOrCreateDNI(sd.getInDND());
    	DecisionNodeInstance outDNI = getOrCreateDNI(sd.getOutDND());
    	if (getProcessSteps().stream().noneMatch(t -> t.getDefinition().getId().equals(sd.getId()))) {
        	ProcessStep step = ProcessStep.getInstance(ws, sd, inDNI, outDNI, this);
        	//step.setProcess(this);
        	if (step != null) {
        		this.addProcessStep(step);
        		return step;
        	}
        }
    	return null;
     }
	
    private DecisionNodeInstance getOrCreateDNI(DecisionNodeDefinition dnd) {
    	return this.getDecisionNodeInstances().stream()
    	.filter(dni -> dni.getDefinition().equals(dnd))
    	.findAny().orElseGet(() -> { DecisionNodeInstance dni = DecisionNodeInstance.getInstance(ws, dnd);
    				dni.setProcess(this);
    				this.addDecisionNodeInstance(dni);
    				return dni;
    	});
    }
	    
	public ProcessDefinition getDefinition() {
		return  WrapperCache.getWrappedInstance(ProcessDefinition.class, instance.getPropertyAsInstance(CoreProperties.processDefinition.toString()));
	}
	
	@Override
	public DecisionNodeInstance getInDNI() {
			Instance inst = instance.getPropertyAsInstance(ProcessStep.CoreProperties.inDNI.toString());
			if (inst != null)
				return WrapperCache.getWrappedInstance(DecisionNodeInstance.class, inst);
			else
				return null; 
	}

	@Override
	public DecisionNodeInstance getOutDNI() {
		Instance inst = instance.getPropertyAsInstance(ProcessStep.CoreProperties.outDNI.toString());
		if (inst != null)
			return WrapperCache.getWrappedInstance(DecisionNodeInstance.class, inst);
		else
			return null; 
	}
	
	public Responses.IOResponse removeInput(String inParam, Instance artifact) {
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
	
	public Responses.IOResponse addInput(String inParam, Instance artifact) {
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
		if (step.getActualLifecycleState().equals(State.ENABLED) &&  this.getDefinition().getCondition(Conditions.PRECONDITION).isEmpty()) {
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
			instance.getPropertyAsSingle(ProcessStep.CoreProperties.processedPostCondFulfilled.toString()).set(isfulfilled);
			events.addAll(tryTransitionToCompleted());
		}
		return events;
	}
	
	@Override
	protected List<Events.ProcessChangedEvent> setPreConditionsFulfilled(boolean isfulfilled) {
		if (arePreCondFulfilled() != isfulfilled) {  // a change
			List<Events.ProcessChangedEvent> events = new LinkedList<>();
			ProcessInstance pi = getParentProcessOrThisIfProcessElseNull();
			events.add(new Events.ConditionFulfillmentChanged(pi, this, Conditions.PRECONDITION, isfulfilled));
			instance.getPropertyAsSingle(ProcessStep.CoreProperties.processedPreCondFulfilled.toString()).set(isfulfilled);
			if (isfulfilled)  {
				events.addAll(this.trigger(StepLifecycle.Trigger.ENABLE)) ;
				events.addAll(tryTransitionToCompleted()) ;
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

	private List<Events.ProcessChangedEvent> tryTransitionToCompleted() {
		if (this.getDefinition().getPostconditions().isEmpty()) {
			instance.getPropertyAsSingle(ProcessStep.CoreProperties.processedPostCondFulfilled.toString()).set(true);
		}
		boolean areAllDNIsInflowFulfilled = this.getDecisionNodeInstances().stream().allMatch(dni -> dni.isInflowFulfilled());
		if (arePostCondFulfilled() && areConstraintsFulfilled(ProcessStep.CoreProperties.qaState.toString()) && arePreCondFulfilled() && areAllDNIsInflowFulfilled)  
			return this.trigger(StepLifecycle.Trigger.MARK_COMPLETE) ;
		else
			return this.trigger(StepLifecycle.Trigger.ACTIVATE);
	}
	
	@SuppressWarnings("unchecked")
	private void addProcessStep(ProcessStep step) {
		assert(step != null);
		assert(step.getInstance() != null);
		instance.getPropertyAsSet(CoreProperties.stepInstances.toString()).add(step.getInstance());
	}
	
	@SuppressWarnings("unchecked")
	public Set<ProcessStep> getProcessSteps() {
		SetProperty<?> stepList = instance.getPropertyAsSet(CoreProperties.stepInstances.toString());
		if (stepList != null && stepList.get() != null) {
			return (Set<ProcessStep>) stepList.get().stream()
					.map(inst -> WrapperCache.getWrappedInstance(ProcessInstance.getMostSpecializedClass((Instance) inst), (Instance) inst))
					.collect(Collectors.toSet());	
		} else return Collections.emptySet();
		
	}
	
	@SuppressWarnings("unchecked")
	public Set<DecisionNodeInstance> getDecisionNodeInstances() {
		SetProperty<?> stepList = instance.getPropertyAsSet(CoreProperties.decisionNodeInstances.toString());
		if (stepList != null && stepList.get() != null) {
			return (Set<DecisionNodeInstance>) stepList.get().stream()
					.map(inst -> WrapperCache.getWrappedInstance(DecisionNodeInstance.class, (Instance) inst))
					.collect(Collectors.toSet());	
		} else return Collections.emptySet();
	}
	
	@SuppressWarnings("unchecked")
	private void addDecisionNodeInstance(DecisionNodeInstance dni) {
		instance.getPropertyAsSet(CoreProperties.decisionNodeInstances.toString()).add(dni.getInstance());
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
	
	public List<ProcessScopedCmd> ensureRuleToStateConsistency() {
		List<ProcessScopedCmd> incons = new LinkedList<>();
		incons.addAll(super.ensureRuleToStateConsistency());
		incons.addAll(getProcessSteps().stream()
				.flatMap(step -> step.ensureRuleToStateConsistency().stream())
				.collect(Collectors.toList()));
		return incons;
	}
	
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
	
	public static InstanceType getOrCreateDesignSpaceInstanceType(Workspace ws, ProcessDefinition td) {
		String parentName = td.getProcess() != null ? td.getProcess().getName() : "ROOT";
		String name = designspaceTypeId+td.getName()+parentName;
//		Optional<InstanceType> thisType = ws.debugInstanceTypes().stream()
//				.filter(it -> !it.isDeleted)
//				.filter(it -> it.name().equals(designspaceTypeId+td.getName()))
//				.findAny();
		Optional<InstanceType> thisType = Optional.ofNullable(ws.TYPES_FOLDER.instanceTypeWithName(name)); 
		if (thisType.isPresent())
			return thisType.get();
		else {
			InstanceType typeStep = ws.createInstanceType(name, ws.TYPES_FOLDER, ProcessStep.getOrCreateDesignSpaceInstanceType(ws, td, null)); // here we dont know the parent process, if any at all, so we cant override the type, this needs to be done by the parent process
			typeStep.createPropertyType(CoreProperties.processDefinition.toString(), Cardinality.SINGLE, ProcessDefinition.getOrCreateDesignSpaceCoreSchema(ws));
			typeStep.createPropertyType(CoreProperties.stepInstances.toString(), Cardinality.SET, ProcessStep.getOrCreateDesignSpaceCoreSchema(ws));
			typeStep.createPropertyType(CoreProperties.decisionNodeInstances.toString(), Cardinality.SET, DecisionNodeInstance.getOrCreateDesignSpaceCoreSchema(ws));
			typeStep.createPropertyType(CoreProperties.createdAt.toString(), Cardinality.SINGLE, Workspace.STRING);
			return typeStep;
		}
	}
	
	protected static Class<? extends InstanceWrapper> getMostSpecializedClass(Instance inst) {
		// we have the problem, that the WrapperCache will only return a type we ask for (which might be a general type) rather than the most specialized one, hence we need to obtain that type here
		// we assume that this is used only in here within, and thus that inst is only ProcessDefinition or StepDefinition
		if (inst.getInstanceType().name().startsWith(designspaceTypeId.toString())) // its a process
			return ProcessInstance.class;
		else 
			return ProcessStep.class; // for now only those two types
	}
	
	public static String generatePrematureRuleName(String stepTypeName, ProcessDefinition pd) {
		return CRD_PREMATURETRIGGER_PREFIX+stepTypeName+"_"+pd.getName();
	}
		
	public static ProcessInstance getInstance(Workspace ws, ProcessDefinition sd) {
		return getInstance(ws, sd, UUID.randomUUID().toString());
	}
	
	public static ProcessInstance getInstance(Workspace ws, ProcessDefinition sd, String namePostfix) {
		//TODO: not to create duplicate process instances somehow
		Instance instance = ws.createInstance(getOrCreateDesignSpaceInstanceType(ws, sd), sd.getName()+"_"+namePostfix);		
		ProcessInstance pi = WrapperCache.getWrappedInstance(ProcessInstance.class, instance);
		pi.init(sd, null, null, ws);
		return pi;
	}
	
	public static ProcessInstance getSubprocessInstance(Workspace ws, ProcessDefinition sd, DecisionNodeInstance inDNI, DecisionNodeInstance outDNI, ProcessInstance scope) {
		Instance instance = ws.createInstance(getOrCreateDesignSpaceInstanceType(ws, sd), sd.getName()+"_"+UUID.randomUUID()); 		
		ProcessInstance pi = WrapperCache.getWrappedInstance(ProcessInstance.class, instance);
		pi.setProcess(scope);
		pi.init(sd, inDNI, outDNI, ws);
		return pi;
	}
	
	protected void init(ProcessDefinition pdef, DecisionNodeInstance inDNI, DecisionNodeInstance outDNI, Workspace ws) {		
		// init first DNI, there should be only one. Needs to be checked earlier with definition creation
		// we assume consistent, correct specification/definition here
		instance.getPropertyAsSingle(CoreProperties.processDefinition.toString()).set(pdef.getInstance());
		super.init(ws, pdef, inDNI, outDNI);	
		setCreatedAt(getCurrentTimestamp());
		if (isImmediateInstantiateAllStepsEnabled()) {
			// instantiate all steps and thereby the DNIs
			pdef.getStepDefinitions().stream().forEach(sd -> { 
				ProcessStep step = createAndWireTask(sd); 				
				//step.getInDNI().tryDataPropagationToPrematurelyTriggeredTask(); no point in triggering as there is no input available at this stage
			});			
		} // now also activate first
		pdef.getDecisionNodeDefinitions().stream()
			.filter(dnd -> dnd.getInSteps().size() == 0)
			.forEach(dnd -> {
				DecisionNodeInstance dni = getOrCreateDNI(dnd);		
				dni.signalStateChanged(this); //dni.tryActivationPropagation(); // to trigger instantiation of initial steps				
			});		
		// datamapping from proc to DNI is triggered upon adding input, which is not available at this stage
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