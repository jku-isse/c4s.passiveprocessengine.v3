package at.jku.isse.passiveprocessengine.definition.serialization;

import java.util.AbstractMap.SimpleEntry;
import java.util.Collections;
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
import at.jku.isse.passiveprocessengine.configurability.ProcessConfigBaseElementFactory;
import at.jku.isse.passiveprocessengine.definition.ProcessDefinition;
import at.jku.isse.passiveprocessengine.definition.ProcessDefinitionError;
import at.jku.isse.passiveprocessengine.instance.ProcessInstance;
import at.jku.isse.passiveprocessengine.instance.ProcessInstanceError;
import at.jku.isse.passiveprocessengine.instance.ProcessStep;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ProcessRegistry {
	
	
	
	protected Workspace ws;
	protected InstanceType procDefType;
	private ProcessConfigBaseElementFactory configFactory;
	
	protected Set<DTOs.Process> tempStorePD = new HashSet<>();
	protected boolean isInit = false;
	protected Map<String, ProcessInstance> pInstances = new HashMap<>();
	protected List<ProcessInstance> removedInstances = new LinkedList<>();
	
	public static final String CONFIG_KEY_doGeneratePrematureRules = "doGeneratePrematureRules";
	public static final String CONFIG_KEY_doImmediateInstantiateAllSteps = "doImmediateInstantiateAllSteps";
	
	public static final String STAGINGPOSTFIX = "_STAGING";
	
	public ProcessRegistry() {
		
	}
	
	//Added Code
	public Workspace getWorkspace()
	{
		return this.ws;
	}
	//End
	
	public void inject(Workspace ws, ProcessConfigBaseElementFactory configFactory) {
		this.ws=ws;
		this.configFactory = configFactory;
		procDefType = ProcessDefinition.getOrCreateDesignSpaceCoreSchema(ws);	
		ws.debugInstanceTypes().stream().forEach(itype -> log.debug(String.format("Available instance type %s as %s", itype.name(), itype.getQualifiedName())));
		
		isInit = true;
		tempStorePD.forEach(pd -> {
			SimpleEntry<ProcessDefinition, List<ProcessDefinitionError>> result = storeProcessDefinition(pd, false);
			if (!result.getValue().isEmpty()) {
				log.warn("Error loading process definition from file system: "+result.getKey().getName()+"\r\n"+result.getValue());
			}
		});
		tempStorePD.clear();
	}
	
	
	public Optional<ProcessDefinition> getProcessDefinition(String name, Boolean onlyValid) {
		List<ProcessDefinition> defs = procDefType.instancesIncludingThoseOfSubtypes()
				.filter(inst -> !inst.isDeleted)
				.filter(inst -> (Boolean)inst.getPropertyAsValueOrElse((ProcessDefinition.CoreProperties.isWithoutBlockingErrors.toString()), () -> "false") || !onlyValid)
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
	
	public ProcessDeployResult createOrReplaceProcessDefinition(DTOs.Process process, boolean doReinstantiateExistingProcessInstances) {
		if (!isInit) { tempStorePD.add(process); return null;} // may occur upon bootup where we dont expect replacement to happen and resort to standard behavior
		String originalCode = process.getCode();
		String tempCode = originalCode+STAGINGPOSTFIX;
		process.setCode(tempCode);
		DefinitionTransformer.replaceStepNamesInMappings(process, originalCode, tempCode);
		SimpleEntry<ProcessDefinition, List<ProcessDefinitionError>> stagedProc = storeProcessDefinition(process, true);
		if (!stagedProc.getValue().isEmpty())
			return new ProcessDeployResult(stagedProc.getKey(), stagedProc.getValue(), Collections.emptyList());
		// if we continue here, then no process error occurred and we can continue
		// we remove the staging one and replace the original
		if (stagedProc.getKey() != null) {
			stagedProc.getKey().deleteCascading(configFactory);
			ws.concludeTransaction();  
		}
		//removeProcessDefinition(process.getCode());
		// now remove the original if exists, and store as new
		DefinitionTransformer.replaceStepNamesInMappings(process, tempCode, originalCode);
		process.setCode(originalCode);
		Optional<ProcessDefinition> prevPD = getProcessDefinition(process.getCode(), true);
		Map<String, Map<String, Set<Instance>>> prevProcInput = new HashMap<>() ;
		if (prevPD.isPresent()) {
			log.debug("Removing existing process definition and instances thereof: "+process.getCode());
			prevProcInput = removeAllProcessInstancesOfProcessDefinition(prevPD.get());
			removeProcessDefinition(process.getCode());
		};
		SimpleEntry<ProcessDefinition, List<ProcessDefinitionError>> newPD = storeProcessDefinition(process, false);
		// if stages process has no error, so should this be, as its identical.
		List<ProcessInstanceError> pInstErrors = new LinkedList<>();
		if (doReinstantiateExistingProcessInstances) {
			//List<ProcessException> exceptions = new LinkedList<>();
			prevProcInput.values().stream()
				.forEach(inputSet -> {
					SimpleEntry<ProcessInstance, List<ProcessInstanceError>> pInst = this.instantiateProcess(newPD.getKey(), inputSet);
					pInstErrors.addAll(pInst.getValue());
				});
//			if (!exceptions.isEmpty()) {
//				ProcessException ex = new ProcessException("Successfully created Process Definition but unable to reinstantiate processes");
//				exceptions.stream().forEach(err -> ex.getErrorMessages().add(err.getMainMessage()));
//				throw ex;
//			}
		}
		return new ProcessDeployResult(newPD.getKey(), newPD.getValue(), pInstErrors);
	}
	
	public SimpleEntry<ProcessDefinition, List<ProcessDefinitionError>> storeProcessDefinition(DTOs.Process process, boolean isInStaging) {
		if (!isInit) { tempStorePD.add(process); return null;}
		
		boolean onlyValid = isInStaging ? false : true;
		Optional<ProcessDefinition> optPD = getProcessDefinition(process.getCode(), onlyValid);
		// in staging we overwrite existing process (as this is an old staged process)

		if (optPD.isPresent()) {
			if (isInStaging) {
				log.debug("Removing old staged process: "+process.getCode()+" before staging new version");
				ProcessDefinition pdef = optPD.get();
				pdef.deleteCascading(configFactory);
				ws.concludeTransaction();  
			} else {
				log.debug("Reusing process: "+process.getCode());
				return new SimpleEntry<>(optPD.get(), Collections.emptyList());
			}
		}
//		if (optPD.isEmpty()) {
			log.debug("Storing new process: "+process.getCode());
			List<ProcessDefinitionError> errors = new LinkedList<>();
			ProcessDefinition pd = DefinitionTransformer.fromDTO(process, ws, isInStaging, errors, configFactory);						
			if (errors.isEmpty()) { //if there are type errors, we dont even try to create rules
				boolean doGeneratePrematureRules = false; 
				if (Boolean.parseBoolean(process.getProcessConfig().getOrDefault(CONFIG_KEY_doGeneratePrematureRules, "false")))
					doGeneratePrematureRules = true;
				errors.addAll(pd.initializeInstanceTypes(doGeneratePrematureRules));
				boolean doImmediatePropagate = !doGeneratePrematureRules;
				pd.setImmediateDataPropagationEnabled(doImmediatePropagate);

				boolean doImmediateInstantiateAllSteps = false; 
				if (Boolean.parseBoolean(process.getProcessConfig().getOrDefault(CONFIG_KEY_doImmediateInstantiateAllSteps, "true")))
					doImmediateInstantiateAllSteps = true;
				pd.setImmediateInstantiateAllStepsEnabled(doImmediateInstantiateAllSteps);
			} else {
				pd.setIsWithoutBlockingErrors(false);
			}
			return new SimpleEntry<>(pd, errors);
//		} else {
//			log.debug("Reusing process: "+process.getCode());
//			return new SimpleEntry<>(optPD.get(), Collections.emptyList());
//		}
	}
	
	public Map<String, Map<String, Set<Instance>>> removeAllProcessInstancesOfProcessDefinition(ProcessDefinition pDef) {
		// to be called before removing the process definition,
		// get the process definition instance type, get all instances thereof, then use the wrapper cache to obtain the process instance
		Map<String, Map<String, Set<Instance>>> prevProcInput = new HashMap<>();
		
		InstanceType specProcDefType = ProcessStep.getOrCreateDesignSpaceInstanceType(pDef.getInstance().workspace, pDef, null); // for deletion its ok to not provide the process instance type
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
		getProcessDefinition(name, true).ifPresent(pdef -> { 
		// moved into ProcessDefinition:	WrapperCache.removeWrapper(pdef.getInstance().id());
			pdef.deleteCascading(configFactory);
			ws.concludeTransaction();
		});  
	}
	
	public Set<String> getAllDefinitionIDs(Boolean onlyValid) {
		 return procDefType.instancesIncludingThoseOfSubtypes()
			.filter(inst -> !inst.isDeleted())
			.filter(inst -> (Boolean)inst.getPropertyAsValue((ProcessDefinition.CoreProperties.isWithoutBlockingErrors.toString())) || !onlyValid)
		 	.map(inst -> inst.name())
		 	.collect(Collectors.toSet());
	}
	
	public Set<ProcessDefinition> getAllDefinitions(Boolean onlyValid) {
		return procDefType.instancesIncludingThoseOfSubtypes()
			.filter(inst -> !inst.isDeleted())
			.filter(inst -> (Boolean)inst.getPropertyAsValueOrElse(ProcessDefinition.CoreProperties.isWithoutBlockingErrors.toString(), () -> false) || !onlyValid)
			.map(inst -> (ProcessDefinition)WrapperCache.getWrappedInstance(ProcessDefinition.class, inst))
			.collect(Collectors.toSet());
	}
	
	public SimpleEntry<ProcessInstance, List<ProcessInstanceError>> instantiateProcess(ProcessDefinition pDef, Map<String, Set<Instance>> input) {
		// check if all inputs available:
		List<ProcessInstanceError> errors = new LinkedList<>();
		String namePostfix = generateProcessNamePostfix(input);
		ProcessInstance pInst = ProcessInstance.getInstance(ws, pDef, namePostfix);
		boolean allInputAvail = pDef.getExpectedInput().keySet().stream().allMatch(expIn -> input.containsKey(expIn));
		if (!allInputAvail)
			errors.add(new ProcessInstanceError(pInst, "Input Invalid",  "Unable to instantiate process due to missing input"));
		else {
			errors.addAll(input.entrySet().stream()
					.flatMap(entry ->  entry.getValue().stream().map(inputV -> pInst.addInput(entry.getKey(), inputV)))
					.filter(resp -> resp.getError() != null)
					.map(resp -> new ProcessInstanceError(pInst, "Input Invalid", resp.getError()))
					.collect(Collectors.toList()));
		}
		if (errors.isEmpty()) {
			pInstances.put(pInst.getName(), pInst);
			ws.concludeTransaction();
			return new SimpleEntry<>(pInst, errors);
		} else {
			pInst.deleteCascading();
			ws.concludeTransaction();
			return new SimpleEntry<>(pInst, errors);
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
			this.removedInstances.add(pi);
		}
	}
	
	public List<ProcessInstance> getExistingAndPriorInstances() {
		List<ProcessInstance> all = new LinkedList<>(pInstances.values());
		all.addAll(removedInstances);
		return all;
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

	@Data
	public static class ProcessDeployResult {
		final ProcessDefinition procDef;
		final List<ProcessDefinitionError> definitionErrors;
		final List<ProcessInstanceError> instanceErrors;
	}
}
