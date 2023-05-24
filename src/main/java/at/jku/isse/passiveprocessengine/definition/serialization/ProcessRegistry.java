package at.jku.isse.passiveprocessengine.definition.serialization;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import at.jku.isse.designspace.core.model.Instance;
import at.jku.isse.designspace.core.model.InstanceType;
import at.jku.isse.designspace.core.model.Workspace;
import at.jku.isse.passiveprocessengine.WrapperCache;
import at.jku.isse.passiveprocessengine.definition.ProcessDefinition;
import at.jku.isse.passiveprocessengine.instance.ProcessException;
import at.jku.isse.passiveprocessengine.instance.ProcessInstance;
import at.jku.isse.passiveprocessengine.instance.ProcessStep;
import at.jku.isse.passiveprocessengine.instance.messages.Responses.IOResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ProcessRegistry {
	
	protected Workspace ws;
	protected InstanceType procDefType;
	
	protected Set<DTOs.Process> tempStorePD = new HashSet<>();
	protected boolean isInit = false;
	protected Map<String, ProcessInstance> pInstances = new HashMap<>();
	
	public static final String CONFIG_KEY_doGeneratePrematureRules = "doGeneratePrematureRules";
	public static final String CONFIG_KEY_doImmediateInstantiateAllSteps = "doImmediateInstantiateAllSteps";
	
	
	
	public ProcessRegistry() {
		
	}
	
	//Added Code
	public Workspace getWorkspace()
	{
		return this.ws;
	}
	//End
	
	public void inject(Workspace ws) {
		this.ws=ws;
		procDefType = ProcessDefinition.getOrCreateDesignSpaceCoreSchema(ws);	
		ws.debugInstanceTypes().parallelStream().forEach(itype -> log.debug(String.format("Available instance type %s as %s", itype.name(), itype.getQualifiedName())));
		
		isInit = true;
		tempStorePD.forEach(pd -> {
			try {
				storeProcessDefinitionIfNotExists(pd);
			} catch (ProcessException e) {
				e.printStackTrace();
			}
		});
		tempStorePD.clear();
	}
	
	
	public Optional<ProcessDefinition> getProcessDefinition(String name) {
		List<ProcessDefinition> defs = procDefType.instancesIncludingThoseOfSubtypes()
				.filter(inst -> !inst.isDeleted)
				.filter(inst -> inst.name().equals(name))
			.map(inst -> (ProcessDefinition)WrapperCache.getWrappedInstance(ProcessDefinition.class, inst))
			.collect(Collectors.toList());			
		if (defs.isEmpty()) 
			return Optional.empty();
		if (defs.size() > 1) 
			throw new RuntimeException("Duplicate non-deleted Processes: "+name);
		else
			return Optional.ofNullable(defs.get(0));
	}
	
	public ProcessDefinition createOrReplaceProcessDefinition(DTOs.Process process, boolean doReinstantiateExistingProcessInstances) throws ProcessException {
		if (!isInit) { tempStorePD.add(process); return null;} // may occur upon bootup where we dont expect replacement to happen and resort to standard behavior
		String originalCode = process.getCode();
		process.setCode(originalCode+"_STAGING");
		storeProcessDefinitionIfNotExists(process);
		// if we continue here, then no process exception was thrown and we can continue
		// we remove the staging one and replace the original
		removeProcessDefinition(process.getCode());
		// now remove the original if exists, and store as new
		process.setCode(originalCode);
		Optional<ProcessDefinition> prevPD = getProcessDefinition(process.getCode());
		Map<String, Map<String, Set<Instance>>> prevProcInput = new HashMap<>() ;
		if (prevPD.isPresent()) {
			log.debug("Removing existing process definition and instances thereof: "+process.getCode());
			prevProcInput = removeAllProcessInstancesOfProcessDefinition(prevPD.get());
			removeProcessDefinition(process.getCode());
		};
		ProcessDefinition newPD = storeProcessDefinitionIfNotExists(process);
		if (doReinstantiateExistingProcessInstances) {
			List<ProcessException> exceptions = new LinkedList<>();
			prevProcInput.values().stream()
				.forEach(inputSet -> {
					try { 
						this.instantiateProcess(newPD, inputSet);
					} catch (ProcessException e) {
						exceptions.add(e);
					}
				});
			if (!exceptions.isEmpty()) {
				ProcessException ex = new ProcessException("Successfully created Process Definition but unable to reinstantiate processes");
				exceptions.stream().forEach(err -> ex.getErrorMessages().add(err.getMainMessage()));
				throw ex;
			}
		}
		return newPD;
	}
	
	public ProcessDefinition storeProcessDefinitionIfNotExists(DTOs.Process process) throws ProcessException {
		if (!isInit) { tempStorePD.add(process); return null;}
		Optional<ProcessDefinition> optPD = getProcessDefinition(process.getCode());
		if (optPD.isEmpty()) {
			log.debug("Storing new process: "+process.getCode());
			ProcessDefinition pd = DefinitionTransformer.fromDTO(process, ws);						
			boolean doGeneratePrematureRules = false; 
			if (Boolean.parseBoolean(process.getProcessConfig().getOrDefault(CONFIG_KEY_doGeneratePrematureRules, "false")))
				doGeneratePrematureRules = true;
			pd.initializeInstanceTypes(doGeneratePrematureRules);
			boolean doImmediatePropagate = !doGeneratePrematureRules;
			pd.setImmediateDataPropagationEnabled(doImmediatePropagate);
			
			boolean doImmediateInstantiateAllSteps = false; 
			if (Boolean.parseBoolean(process.getProcessConfig().getOrDefault(CONFIG_KEY_doImmediateInstantiateAllSteps, "true")))
				doImmediateInstantiateAllSteps = true;
			pd.setImmediateInstantiateAllStepsEnabled(doImmediateInstantiateAllSteps);
			return pd;
		} else {
			log.debug("Reusing process: "+process.getCode());
			return optPD.get();
		}
	}
	
	private Map<String, Map<String, Set<Instance>>> removeAllProcessInstancesOfProcessDefinition(ProcessDefinition pDef) {
		// to be called before removing the process definition,
		// get the process definition instance type, get all instances thereof, then use the wrapper cache to obtain the process instance
		Map<String, Map<String, Set<Instance>>> prevProcInput = new HashMap<>();
		
		InstanceType specProcDefType = ProcessStep.getOrCreateDesignSpaceInstanceType(pDef.getInstance().workspace, pDef);
		specProcDefType.instancesIncludingThoseOfSubtypes().collect(Collectors.toSet()).stream()
			.filter(inst -> !inst.isDeleted())
			.map(inst -> WrapperCache.getWrappedInstance(ProcessInstance.class, inst))
			.filter(Objects::nonNull)
			.map(ProcessInstance.class::cast)
			.forEach(procInst -> { 
				pInstances.remove(procInst.getName());
				Map<String, Set<Instance>> inputSet = new HashMap<>();
				procInst.getDefinition().getExpectedInput().keySet().stream()
					.forEach(input -> inputSet.put(input, procInst.getInput(input)));
				prevProcInput.put(procInst.getName(), inputSet);
				procInst.deleteCascading(); 
			});
		return prevProcInput;
	}
	
	public void removeProcessDefinition(String name) {
		getProcessDefinition(name).ifPresent(pdef -> { 
		// moved into ProcessDefinition:	WrapperCache.removeWrapper(pdef.getInstance().id());
			pdef.deleteCascading();
			ws.concludeTransaction();
		});  
	}
	
	public Set<String> getAllDefinitionIDs() {
		 return procDefType.instancesIncludingThoseOfSubtypes()
		 	.map(inst -> inst.name())
		 	.collect(Collectors.toSet());
	}
	
	public Set<ProcessDefinition> getAllDefinitions() {
		return procDefType.instancesIncludingThoseOfSubtypes()
			.map(inst -> (ProcessDefinition)WrapperCache.getWrappedInstance(ProcessDefinition.class, inst))
			.collect(Collectors.toSet());
	}
	
	public ProcessInstance instantiateProcess(ProcessDefinition pDef, Map<String, Set<Instance>> input) throws ProcessException {
		// check if all inputs available:
		boolean allInputAvail = pDef.getExpectedInput().keySet().stream().allMatch(expIn -> input.containsKey(expIn));
		if (!allInputAvail)
			throw new ProcessException("Unable to instantiate process due to missing input");
		
		String namePostfix = generateProcessNamePostfix(input);
		ProcessInstance pInst = ProcessInstance.getInstance(ws, pDef, namePostfix);
		List<IOResponse> errResp = input.entrySet().stream()
			.flatMap(entry ->  entry.getValue().stream().map(inputV -> pInst.addInput(entry.getKey(), inputV)))
			.filter(resp -> resp.getError() != null)
			.collect(Collectors.toList());
		
		if (errResp.isEmpty()) {
			pInstances.put(pInst.getName(), pInst);
			ws.concludeTransaction();
			return pInst;
		} else {
			pInst.deleteCascading();
			ws.concludeTransaction();
			ProcessException ex = new ProcessException("Unable to instantiate process");
			errResp.stream().forEach(err -> ex.getErrorMessages().add(err.getError()));
			throw ex;
		}		
	}
	
	public ProcessInstance getProcess(String id) {
		return pInstances.get(id);
	}
	
	public void removeProcess(String id) {
		ProcessInstance pi = pInstances.remove(id);
		if (pi != null) {
			pi.deleteCascading();
			ws.concludeTransaction();
		}
	}
	
	public Set<ProcessInstance> loadPersistedProcesses() {
		Set<ProcessInstance> existingPI = getSubtypesRecursively(ProcessInstance.getOrCreateDesignSpaceCoreSchema(ws))
				//.allSubTypes() // everything that is of steptype (thus also process type) --> NOT YET IMPLEMENTED				
				.stream().filter(stepType -> stepType.name().startsWith(ProcessInstance.designspaceTypeId)) //everthing that is a process type
				.flatMap(procType -> procType.instancesIncludingThoseOfSubtypes()) // everything that is a process instance
				.map(procInst -> WrapperCache.getWrappedInstance(ProcessInstance.class, procInst)) // wrap instance
				.map(procInst -> (ProcessInstance)procInst)
				.collect(Collectors.toSet());
		log.info(String.format("Loaded %s preexisting process instances", existingPI.size()));
		existingPI.stream().forEach(pi -> pInstances.put(pi.getName(), pi));
		return existingPI;
	}
	
	private Set<InstanceType> getSubtypesRecursively(InstanceType type) {
		Set<InstanceType> subTypes = type.subTypes(); 				
        for (InstanceType subType : Set.copyOf(subTypes)) {
            subTypes.addAll( getSubtypesRecursively(subType));
        }
        return subTypes;
	}
	
	public static String generateProcessNamePostfix(Map<String, Set<Instance>> procInput) {
		return procInput.entrySet().stream()
				.flatMap(entry -> entry.getValue().stream())
				.map(inst -> inst.name()).collect(Collectors.joining(",", "[", "]"));
	}
}
