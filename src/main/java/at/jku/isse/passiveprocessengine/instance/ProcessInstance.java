package at.jku.isse.passiveprocessengine.instance;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import at.jku.isse.designspace.core.model.Cardinality;
import at.jku.isse.designspace.core.model.Instance;
import at.jku.isse.designspace.core.model.InstanceType;
import at.jku.isse.designspace.core.model.Property;
import at.jku.isse.designspace.core.model.SetProperty;
import at.jku.isse.designspace.core.model.Workspace;
import at.jku.isse.passiveprocessengine.WrapperCache;
import at.jku.isse.passiveprocessengine.definition.DecisionNodeDefinition;
import at.jku.isse.passiveprocessengine.definition.ProcessDefinition;
import at.jku.isse.passiveprocessengine.definition.StepDefinition;
import lombok.extern.slf4j.Slf4j;


@Slf4j
public class ProcessInstance extends ProcessStep {

	static enum CoreProperties {stepInstances, decisionNodeInstances, processDefinition};
	
	public static final String designspaceTypeId = ProcessInstance.class.getSimpleName();

	
	public ProcessInstance(Instance instance) {
		super(instance);
	}

	protected void createAndWireTask(StepDefinition sd) {
    	DecisionNodeInstance inDNI = getOrCreateDNI(sd.getInDND());
    	DecisionNodeInstance outDNI = getOrCreateDNI(sd.getOutDND());
    	if (getProcessSteps().stream().noneMatch(t -> t.getDefinition().getId().equals(sd.getId()))) {
        	ProcessStep step = ProcessStep.getInstance(ws, sd, inDNI, outDNI);
        	step.setProcess(this);
        	if (step != null)
        		this.addProcessStep(step);
        }
    	//if (wft != null) {
    			//if (this.expectedLifecycleState.equals(State.CANCELED)) {
    			//	newAWOs.addAll(wft.setCanceled(true, cause));
    			//} else if (this.expectedLifecycleState.equals(State.NO_WORK_EXPECTED)) {
    			//	newAWOs.addAll(wft.setWorkExpected(false, cause));
    			//}    			
    	//}
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
	
	public void removeInput(String inParam, Instance artifact) {
		super.removeInput(inParam, artifact);
		// now see if we need to map this to first DNI - we assume all went well
		getDecisionNodeInstances().stream()
		.filter(dni -> dni.getInSteps().size() == 0)
		.forEach(dni -> {
			dni.signalPrevTaskDataChanged(this);
		});
	}
	
	public void addInput(String inParam, Instance artifact) {
		super.addInput(inParam, artifact);
		// now see if we need to map this to first DNI - we assume all went well
		getDecisionNodeInstances().stream()
		.filter(dni -> dni.getInSteps().size() == 0)
		.forEach(dni -> {
			//dni.tryActivationPropagation(); // to trigger mapping to first steps
			dni.signalPrevTaskDataChanged(this);
		});
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
					.map(inst -> WrapperCache.getWrappedInstance(ProcessStep.class, (Instance) inst))
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
	
	public static InstanceType getOrCreateDesignSpaceInstanceType(Workspace ws, ProcessDefinition td) {
		Optional<InstanceType> thisType = ws.debugInstanceTypes().stream()
				.filter(it -> it.name().equals(designspaceTypeId+td.getId()))
				.findAny();
		if (thisType.isPresent())
			return thisType.get();
		else {
			InstanceType typeStep = ws.createInstanceType(designspaceTypeId+td.getName(), ws.TYPES_FOLDER, ProcessStep.getOrCreateDesignSpaceInstanceType(ws, td));
			typeStep.createPropertyType(CoreProperties.processDefinition.toString(), Cardinality.SINGLE, ProcessDefinition.getOrCreateDesignSpaceCoreSchema(ws));
			typeStep.createPropertyType(CoreProperties.stepInstances.toString(), Cardinality.SET, ProcessStep.getOrCreateDesignSpaceCoreSchema(ws));
			typeStep.createPropertyType(CoreProperties.decisionNodeInstances.toString(), Cardinality.SET, DecisionNodeInstance.getOrCreateDesignSpaceCoreSchema(ws));
			return typeStep;
		}
	}
		
	public static ProcessInstance getInstance(Workspace ws, ProcessDefinition sd) {
		Instance instance = ws.createInstance(getOrCreateDesignSpaceInstanceType(ws, sd), sd.getName()+"_"+UUID.randomUUID());
		ProcessInstance pi = WrapperCache.getWrappedInstance(ProcessInstance.class, instance);
		pi.init(sd);
		return pi;
	}
	
	protected void init(ProcessDefinition pdef) {
		// init first DNI, there should be only one. Needs to be checked earlier with definition creation
		// we assume consistent, correct specification/definition here
		super.init(pdef, null, null);
		instance.getPropertyAsSingle(CoreProperties.processDefinition.toString()).set(pdef.getInstance());
		pdef.getDecisionNodeDefinitions().stream()
			.filter(dnd -> dnd.getInSteps().size() == 0)
			.forEach(dnd -> {
				DecisionNodeInstance dni = DecisionNodeInstance.getInstance(ws, dnd);
				dni.setProcess(this);
				this.addDecisionNodeInstance(dni);
				dni.tryActivationPropagation(); // to trigger instantiation of initial steps
			});
		// datamapping from proc to DNI is triggered upon adding input, which is not available at this stage
	}
	
}
