package at.jku.isse.passiveprocessengine.instance;

import java.util.Optional;
import java.util.UUID;

import com.github.oxo42.stateless4j.StateMachine;

import at.jku.isse.designspace.core.model.Cardinality;
import at.jku.isse.designspace.core.model.Instance;
import at.jku.isse.designspace.core.model.InstanceType;
import at.jku.isse.designspace.core.model.Workspace;
import at.jku.isse.passiveprocessengine.IdentifiableElement;
import at.jku.isse.passiveprocessengine.definition.IStepDefinition;
import at.jku.isse.passiveprocessengine.definition.InstanceWrapper;
import at.jku.isse.passiveprocessengine.definition.StepDefinition;
import at.jku.isse.passiveprocessengine.definition.WrapperCache;
import at.jku.isse.passiveprocessengine.instance.StepLifecycle.State;

public class ProcessStep implements IdentifiableElement, InstanceWrapper{

	static enum CoreProperties {actualLifecycleState, expectedLifecycleState};
	
	public static final String designspaceTypeId = ProcessStep.class.getSimpleName();
	
	//protected StepLifecycle.State actualLifecycleState = StepLifecycle.State.AVAILABLE; //default state
	//protected StepLifecycle.State expectedLifecycleState = StepLifecycle.State.AVAILABLE; //default state
	protected transient StateMachine<StepLifecycle.State, StepLifecycle.Triggers> actualSM;
	protected transient StateMachine<StepLifecycle.State, StepLifecycle.Triggers> expectedSM;
	//protected transient IStepDefinition td;
	
	protected transient Instance instance;
	
	public ProcessStep(Instance instance) {
		assert instance != null;
		this.instance=instance;
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
	
	public void processIOChangeEvent() {
		// here obtain any event that relates to this task such as adding, removing, changing inputs or output
		// then trigger cascading
	}
	
	@Override
	public String getId() {
		return instance.id().toString();
	}

	public State getExpectedLifecycleState() {
		// TODO Auto-generated method stub
		return expectedSM.getState();
	}
	
	@Override
	public Instance getInstance() {
		return instance;
	}
	
	public static InstanceType getOrCreateDesignSpaceCoreSchema(Workspace ws) {
		Optional<InstanceType> thisType = ws.debugInstanceTypes().stream()
			.filter(it -> it.name().equals(ProcessStep.designspaceTypeId))
			.findAny();
		if (thisType.isPresent())
			return thisType.get();
		else {
			InstanceType typeStep = ws.createInstanceType(ProcessStep.designspaceTypeId, ws.TYPES_FOLDER);
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
			InstanceType typeStep = ws.createInstanceType(designspaceTypeId+td.getId(), ws.TYPES_FOLDER, superType);
			td.getExpectedInput().entrySet().stream()
				.forEach(entry -> {
						typeStep.createPropertyType("in_"+entry.getKey(), Cardinality.LIST, entry.getValue());
				});
			td.getExpectedOutput().entrySet().stream()
			.forEach(entry -> {
					typeStep.createPropertyType("out_"+entry.getKey(), Cardinality.LIST, entry.getValue());
			});
			//TODO: instantiate all of pre/post/activation/cancelation rules
			//TODO: instantiate all of input to output mapping rules (incl repairs, i.e., the actual mapping logic)
			return typeStep;
		}
	}

	public static ProcessStep getInstance(Workspace ws, StepDefinition sd) {
		Instance instance = ws.createInstance(getOrCreateDesignSpaceInstanceType(ws, sd), sd.getId()+UUID.randomUUID());
		return WrapperCache.getWrappedInstance(ProcessStep.class, instance);
	}


}
