package at.jku.isse.passiveprocessengine.instance;

import java.util.Optional;
import java.util.UUID;

import com.github.oxo42.stateless4j.StateMachine;

import at.jku.isse.designspace.core.events.PropertyUpdateSet;
import at.jku.isse.designspace.core.model.Cardinality;
import at.jku.isse.designspace.core.model.Element;
import at.jku.isse.designspace.core.model.Instance;
import at.jku.isse.designspace.core.model.InstanceType;
import at.jku.isse.designspace.core.model.Property;
import at.jku.isse.designspace.core.model.Workspace;
import at.jku.isse.designspace.rule.checker.ConsistencyUtils;
import at.jku.isse.designspace.rule.model.ConsistencyRule;
import at.jku.isse.designspace.rule.model.ConsistencyRuleType;
import at.jku.isse.passiveprocessengine.IdentifiableElement;
import at.jku.isse.passiveprocessengine.InstanceWrapper;
import at.jku.isse.passiveprocessengine.ProcessScopedElement;
import at.jku.isse.passiveprocessengine.WrapperCache;
import at.jku.isse.passiveprocessengine.definition.IStepDefinition;
import at.jku.isse.passiveprocessengine.definition.StepDefinition;
import at.jku.isse.passiveprocessengine.instance.StepLifecycle.Conditions;
import at.jku.isse.passiveprocessengine.instance.StepLifecycle.State;
import at.jku.isse.passiveprocessengine.instance.commands.Commands.*;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ProcessStep extends ProcessScopedElement{

	static enum CoreProperties {actualLifecycleState, expectedLifecycleState};
	
	public static final String designspaceTypeId = ProcessStep.class.getSimpleName();
	
	protected transient StateMachine<StepLifecycle.State, StepLifecycle.Triggers> actualSM;
	protected transient StateMachine<StepLifecycle.State, StepLifecycle.Triggers> expectedSM;
	//protected transient IStepDefinition td;
	

	
	
	
	public ProcessStep(Instance instance) {
		super(instance);
		initLifecycleStates();
	}
	
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
	
	public IdentifiableCmd processRuleEvaluationChange(ConsistencyRule cr, PropertyUpdateSet op) {
		// now here we have to distinguish what this evaluation change implies
		ConsistencyRuleType crt = (ConsistencyRuleType)cr.getInstanceType();
		
		if (crt.name().startsWith("crd_PRECONDITION_")) { //FIXME better matching needed
			log.debug(String.format("Step %s has precondition evaluate to %s", this.getName(), op.value().toString()));
			return new ConditionChangedCmd(this, Conditions.PRECONDITION, Boolean.valueOf(op.value().toString()));
		} else {
		// if premature conditions, then delegate to process instance, resp often will need to be on process level anyway
			// if postcondition do here
			// if cancelation condition do here
			// if activation cond do here
			
			// input to putput mappings
			log.debug(String.format("Step %s has rule %s evaluate to %s", this.getName(), crt.name(), op.value().toString()));
		}
		return null;
	}
	
	public void processIOChangeEvent() {
		// here obtain any event that relates to this task such as adding, removing, changing inputs or output
		// then trigger cascading
	}
	
	public State getExpectedLifecycleState() {
		return expectedSM.getState();
	}
	
	public static InstanceType getOrCreateDesignSpaceCoreSchema(Workspace ws) {
		Optional<InstanceType> thisType = ws.debugInstanceTypes().stream()
			.filter(it -> it.name().equals(ProcessStep.designspaceTypeId))
			.findAny();
		if (thisType.isPresent())
			return thisType.get();
		else {
			InstanceType typeStep = ws.createInstanceType(ProcessStep.designspaceTypeId, ws.TYPES_FOLDER, ProcessScopedElement.getOrCreateDesignSpaceCoreSchema(ws));
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
						typeStep.createPropertyType("in_"+entry.getKey(), Cardinality.LIST, entry.getValue());
				});
			td.getExpectedOutput().entrySet().stream()
			.forEach(entry -> {
					typeStep.createPropertyType("out_"+entry.getKey(), Cardinality.LIST, entry.getValue());
			});
			
			for (Conditions condition : Conditions.values()) {
				if (td.getCondition(condition).isPresent()) {
					ConsistencyRuleType crd = ConsistencyRuleType.create(ws, typeStep, "crd_"+condition+"_"+typeStep.name(), td.getCondition(condition).get());
					assert ConsistencyUtils.crdValid(crd);
					typeStep.createPropertyType(condition.toString(), Cardinality.SINGLE, crd);
				}	
			}
			
			
			//TODO: instantiate all of pre/post/activation/cancelation rules
			//TODO: instantiate all of input to output mapping rules (incl repairs, i.e., the actual mapping logic)
			return typeStep;
		}
	}

	public static ProcessStep getInstance(Workspace ws, IStepDefinition sd) {
		Instance instance = ws.createInstance(getOrCreateDesignSpaceInstanceType(ws, sd), sd.getName()+UUID.randomUUID());
		return WrapperCache.getWrappedInstance(ProcessStep.class, instance);
	}


}
