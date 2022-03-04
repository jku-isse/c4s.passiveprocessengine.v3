package at.jku.isse.passiveprocessengine.instance;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.github.oxo42.stateless4j.StateMachine;

import at.jku.isse.designspace.core.events.PropertyUpdateSet;
import at.jku.isse.designspace.core.model.Cardinality;
import at.jku.isse.designspace.core.model.Instance;
import at.jku.isse.designspace.core.model.InstanceType;
import at.jku.isse.designspace.core.model.Property;
import at.jku.isse.designspace.core.model.Workspace;
import at.jku.isse.designspace.rule.checker.ConsistencyUtils;
import at.jku.isse.designspace.rule.model.ConsistencyRule;
import at.jku.isse.designspace.rule.model.ConsistencyRuleType;
import at.jku.isse.passiveprocessengine.ProcessInstanceScopedElement;
import at.jku.isse.passiveprocessengine.WrapperCache;
import at.jku.isse.passiveprocessengine.definition.IStepDefinition;
import at.jku.isse.passiveprocessengine.definition.StepDefinition;
import at.jku.isse.passiveprocessengine.instance.StepLifecycle.Conditions;
import at.jku.isse.passiveprocessengine.instance.StepLifecycle.State;
import at.jku.isse.passiveprocessengine.instance.StepLifecycle.Trigger;
import at.jku.isse.passiveprocessengine.instance.commands.Commands.*;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ProcessStep extends ProcessInstanceScopedElement{

	static enum CoreProperties {actualLifecycleState, expectedLifecycleState, stepDefinition, inDNI, outDNI};
	
	public static final String designspaceTypeId = ProcessStep.class.getSimpleName();
	
	protected transient StateMachine<StepLifecycle.State, StepLifecycle.Trigger> actualSM;
	protected transient StateMachine<StepLifecycle.State, StepLifecycle.Trigger> expectedSM;
	
	public ProcessStep(Instance instance) {
		super(instance);
		initLifecycleStates();
	}
	
	//FIXME: check if those transient properties are correctly reset upon loading
	protected transient boolean arePreCondFulfilled = false;
	protected transient boolean arePostCondFulfilled = false;
	protected transient boolean areCancelCondFulfilled = false;
	protected transient boolean isWorkExpected = true;
	
	
	private void initLifecycleStates() {
		
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
	}
	
	public TrackableCmd processRuleEvaluationChange(ConsistencyRule cr, PropertyUpdateSet op) {
		// now here we have to distinguish what this evaluation change implies
		ConsistencyRuleType crt = (ConsistencyRuleType)cr.getInstanceType();
		Conditions cond = determineCondition(crt);
		if (cond != null ) {
			log.debug(String.format("Step %s has %s evaluate to %s", this.getName(), cond, op.value().toString()));
			return new ConditionChangedCmd(this, cond, Boolean.valueOf(op.value().toString()));
		} else {
		// if premature conditions, then delegate to process instance, resp often will need to be on process level anyway
		
			// input to putput mappings
			if (crt.name().startsWith("crd_datamapping") ) { // an unfulfilled datamapping rules
				if (Boolean.valueOf(op.value().toString()) == false) {
				// now we need to "repair" this, i.e., set the output accordingly
					log.debug(String.format("Datamapping %s will be repaired", this.getName(), crt.name()));
					InputToOutputMapper.mapInputToOutputInStepScope(this, cr);
				} else {
					log.debug(String.format("Datamapping %s now consistent", this.getName(), crt.name()));
				}
			} else
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
			if (!crt.name().startsWith("crd_datamapping_"))
					log.error("Unknown consistency rule: "+crt.name());
			return null;
		}
	}
	
	public void processIOChangeEvent() {
		// here obtain any event that relates to this task such as adding, removing, changing inputs or output
		// then trigger cascading
	}
	
	public StepDefinition getDefinition() {
		return  WrapperCache.getWrappedInstance(StepDefinition.class, instance.getPropertyAsInstance(CoreProperties.stepDefinition.toString()));
		
	}
	
	@SuppressWarnings("unchecked")
	public Set<Instance> getInput(String param) {
		return (Set<Instance>) instance.getPropertyAsSet("in_"+param).get();
	}
	
	@SuppressWarnings("unchecked")
	public void addInput(String inParam, Instance artifact) {
		if (getDefinition().getExpectedInput().containsKey(inParam)) {
			Property<?> prop = instance.getProperty("in_"+inParam);
			if (prop.propertyType.isAssignable(artifact)) {
				instance.getPropertyAsSet("in_"+inParam).add(artifact);
			} else {
				log.warn(String.format("Cannot add input %s to %s with nonmatching artifact type %s of id % %s", inParam, this.getName(), artifact.getInstanceType().toString(), artifact.id(), artifact.name()));
			}
		} else {
			// additionally Somehow notify about wrong param access
			log.warn(String.format("Ignoring attempt to add unexpected input %s to %s", inParam, this.getName()));
		}
	}
	
	@SuppressWarnings("unchecked")
	public Set<Instance> getOutput(String param) {
		return (Set<Instance>) instance.getPropertyAsSet("out_"+param).get();
	}
	
	@SuppressWarnings("unchecked")
	public void addOutput(String param, Instance art) {
		instance.getPropertyAsSet("out_"+param).add(art);
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
	
	public void setWorkExpected(boolean isExpected) {
		if (isWorkExpected != isExpected ) {
				isWorkExpected = isExpected;	
				if (!isWorkExpected) {
					trigger(Trigger.HALT);
				}
				else {
				if (arePostCondFulfilled && areQAconstraintsFulfilled())
					trigger(Trigger.MARK_COMPLETE_REPAIR);
				if (arePreCondFulfilled)
					trigger(Trigger.ENABLE);
				if (actualSM.isInState(State.ACTIVE))
					trigger(Trigger.ACTIVATE_REPAIR);
				else
					trigger(Trigger.RESET);
				}
			}
	}
	
	public boolean areQAconstraintsFulfilled() {
		//TODO implement, iterate through links to quality eval rule evaluations
		// check if all are fulfilled
		return true;
	}
	
	public void setPostConditionsFulfilled(boolean isfulfilled) {
		if (arePostCondFulfilled != isfulfilled) { // a change
			arePostCondFulfilled = isfulfilled;
			if (isfulfilled && areQAconstraintsFulfilled())  
				this.trigger(StepLifecycle.Trigger.MARK_COMPLETE) ;
			if (!isfulfilled && actualSM.isInState(State.COMPLETED)) 
				this.trigger(StepLifecycle.Trigger.ACTIVATE);
		}
	}
	
	public void setPreConditionsFulfilled(boolean isfulfilled) {
		if (arePreCondFulfilled != isfulfilled) {  // a change
			arePreCondFulfilled = isfulfilled;
			if (isfulfilled)  
				this.trigger(StepLifecycle.Trigger.ENABLE) ;
			else 
				this.trigger(StepLifecycle.Trigger.RESET);
		}
	}
	
	public void setCancelConditionsFulfilled(boolean isfulfilled) {
		if (areCancelCondFulfilled != isfulfilled) {
			areCancelCondFulfilled = isfulfilled;
			if (isfulfilled)
				trigger(Trigger.CANCEL);
			else { // check which is the new state:
				// we cant use actual state as this might be deviating multiple ways (e.g., we should now be in available, but actual state is in active
				if (!isWorkExpected)
					trigger(Trigger.HALT); //other steps are prefered/used at the moment
				if (arePostCondFulfilled && areQAconstraintsFulfilled())
					trigger(Trigger.MARK_COMPLETE_REPAIR);
				if (arePreCondFulfilled)
					trigger(Trigger.ENABLE);
				if (actualSM.isInState(State.ACTIVE))
					trigger(Trigger.ACTIVATE_REPAIR);
				else
					trigger(Trigger.RESET);
			}
		}
	}
	
	public void setActivationConditionsFulfilled() {
		trigger(Trigger.ACTIVATE);
	}
	
	protected void trigger(Trigger event) {
		// trigger expectedTransition:
		if (event.equals(Trigger.ACTIVATE)) {
			if (expectedSM.isInState(State.CANCELED) || expectedSM.isInState(State.NO_WORK_EXPECTED))
				event = Trigger.ACTIVATE_DEVIATING;
		}
		else if (event.equals(Trigger.MARK_COMPLETE))
			if (expectedSM.isInState(State.CANCELED) || expectedSM.isInState(State.NO_WORK_EXPECTED))
				event = Trigger.MARK_COMPLETE_DEVIATING;

		boolean tryProgress = false;		
		if (actualSM.canFire(event)) {
			State prevActualLifecycleState = actualSM.getState();
			actualSM.fire(event);
			State actualLifecycleState = actualSM.getState();
			if (actualLifecycleState != prevActualLifecycleState) { // state transition
				instance.getPropertyAsSingle(CoreProperties.actualLifecycleState.toString()).set(actualSM.getState().toString());
				if (actualSM.getState().equals(State.ACTIVE) && this.getProcess() != null) {
					getProcess().setActivationConditionsFulfilled();
				}
				tryProgress = true;				
			}
		} else {
			log.info(String.format("Step %s received (and ignored) for 'expectedSM' unexpected Event %s for State %s ", this.getName(),  event,  actualSM.getState()));			
		}		

		if (this.expectedSM.canFire(event)) {
			State prevExpState = expectedSM.getState();
			expectedSM.fire(event);
			if (expectedSM.getState() != prevExpState) { // state transition
				instance.getPropertyAsSingle(CoreProperties.expectedLifecycleState.toString()).set(expectedSM.getState().toString());
				switch(expectedSM.getState()) {
				case ENABLED:					
					// handle deviation mitigation --> if actualSM==ACTIVE and expected transitions from Available to Enabled, then should continue to Active
					if (actualSM.getState().equals(State.ACTIVE)) {
						expectedSM.fire(Trigger.ACTIVATE);
						// same fore COMPLETED
					} else if (actualSM.getState().equals(State.COMPLETED)) {
						expectedSM.fire(Trigger.MARK_COMPLETE);
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
				this.getOutDNI().tryInConditionsFullfilled();
			} 
		}
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
			return typeStep;
		}
	}
	
	public static InstanceType getOrCreateDesignSpaceInstanceType(Workspace ws, IStepDefinition td) {
		Optional<InstanceType> thisType = ws.debugInstanceTypes().stream()
				.filter(it -> it.name().equals(designspaceTypeId+td.getId()))
				.findAny();
		if (thisType.isPresent())
			return thisType.get();
		else {
			InstanceType superType = getOrCreateDesignSpaceCoreSchema(ws);
			InstanceType typeStep = ws.createInstanceType(designspaceTypeId+td.getName(), ws.TYPES_FOLDER, superType);
			td.getExpectedInput().entrySet().stream()
				.forEach(entry -> {
						typeStep.createPropertyType("in_"+entry.getKey(), Cardinality.SET, entry.getValue());
				});
			td.getExpectedOutput().entrySet().stream()
			.forEach(entry -> {
					typeStep.createPropertyType("out_"+entry.getKey(), Cardinality.SET, entry.getValue());
			});
			
			for (Conditions condition : Conditions.values()) {
				if (td.getCondition(condition).isPresent()) {
					ConsistencyRuleType crd = ConsistencyRuleType.create(ws, typeStep, "crd_"+condition+"_"+typeStep.name(), td.getCondition(condition).get());
					assert ConsistencyUtils.crdValid(crd);
					typeStep.createPropertyType(condition.toString(), Cardinality.SINGLE, crd);
				}	
			}
			td.getInputToOutputMappingRules().entrySet().stream()
				.forEach(entry -> {
					ConsistencyRuleType crt = ConsistencyRuleType.create(ws, typeStep, "crd_datamapping_"+entry.getKey()+"_"+typeStep.name(), entry.getValue());
					assert ConsistencyUtils.crdValid(crt);
					typeStep.createPropertyType("crd_datamapping_"+entry.getKey(), Cardinality.SINGLE, crt);
				});
			
			//TODO: instantiate all of input to output mapping rules (incl repairs, i.e., the actual mapping logic)
			return typeStep;
		}
	}

	protected static ProcessStep getInstance(Workspace ws, StepDefinition sd, DecisionNodeInstance inDNI, DecisionNodeInstance outDNI) {
		assert(sd != null);
		assert(inDNI != null);
		assert(outDNI != null);
		Instance instance = ws.createInstance(getOrCreateDesignSpaceInstanceType(ws, sd), sd.getName()+"_"+UUID.randomUUID());
		ProcessStep step = WrapperCache.getWrappedInstance(ProcessStep.class, instance);
		step.init(sd, inDNI, outDNI);
		return step;
	}

	protected void init(StepDefinition sd, DecisionNodeInstance inDNI, DecisionNodeInstance outDNI) {
		instance.getPropertyAsSingle(CoreProperties.stepDefinition.toString()).set(sd.getInstance());
		if (inDNI != null) {
			instance.getPropertyAsSingle(CoreProperties.inDNI.toString()).set(inDNI.getInstance());
			inDNI.addOutStep(this);
		}
		if (outDNI != null) {
			instance.getPropertyAsSingle(CoreProperties.outDNI.toString()).set(inDNI.getInstance());
			outDNI.addInStep(this);		
		}
		// FIXME: better realized via bidirectional properties
//		for (Conditions condition : Conditions.values()) {
//			if (sd.getCondition(condition).isPresent()) {
//				PropertyType pt = instance.getInstanceType().getPropertyType(condition.toString());
//				ConsistencyRuleType crd = (ConsistencyRuleType) pt.getInstanceType();
//				
//			}
//		}
		
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
		
		return "Step ["+ getName() + " "+states+" "+input+" "+output+" in Proc: " + process +" DS: " +getInstance().toString();
	}




}
