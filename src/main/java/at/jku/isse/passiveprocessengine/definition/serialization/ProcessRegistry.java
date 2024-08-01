package at.jku.isse.passiveprocessengine.definition.serialization;

import java.util.AbstractMap.SimpleEntry;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import at.jku.isse.passiveprocessengine.core.ProcessContext;
import at.jku.isse.passiveprocessengine.core.PPEInstance;
import at.jku.isse.passiveprocessengine.core.PPEInstanceType;
import at.jku.isse.passiveprocessengine.definition.ProcessDefinitionError;
import at.jku.isse.passiveprocessengine.definition.activeobjects.ProcessDefinition;
import at.jku.isse.passiveprocessengine.definition.types.ProcessDefinitionType;
import at.jku.isse.passiveprocessengine.instance.ProcessInstanceError;
import at.jku.isse.passiveprocessengine.instance.activeobjects.ProcessInstance;
import at.jku.isse.passiveprocessengine.instance.types.SpecificProcessInstanceType;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ProcessRegistry {

	private final ProcessContext context;


	protected PPEInstanceType processDefinitionType;
	protected PPEInstanceType processInstanceType;

	//protected Set<DTOs.Process> tempStorePD = new HashSet<>();
	//protected boolean isInit = false;
	protected Map<String, ProcessInstance> processInstances = new HashMap<>();
	protected List<ProcessInstance> removedInstances = new LinkedList<>();

	public static final String STAGINGPOSTFIX = "_STAGING";

	public ProcessRegistry(ProcessContext context) {
		this.context = context;
		processDefinitionType = context.getSchemaRegistry().getType(ProcessDefinition.class); // ProcessDefinition.getOrCreateDesignSpaceCoreSchema(ws);
		assert(processDefinitionType != null);
		processInstanceType = context.getSchemaRegistry().getType(ProcessInstance.class); 
		assert(processInstanceType != null);
		context.getSchemaRegistry().getAllNonDeletedInstanceTypes().stream().forEach(itype -> log.debug(String.format("Available instance type %s ", itype.getName())));
		loadPersistedProcesses();
	}
	


//	public void initProcessDefinitions() {		
//		// TODO: restructure designspace so that process type is available upon constructor call
//		context.getSchemaRegistry().getAllNonDeletedInstanceTypes().stream().forEach(itype -> log.debug(String.format("Available instance type %s ", itype.getName())));
//		isInit = true;
//		tempStorePD.forEach(pd -> {
//			SimpleEntry<ProcessDefinition, List<ProcessDefinitionError>> result = storeProcessDefinition(pd, false); // if process already exists do nothing, if not exists and has errors log error, else create process
//			if (!result.getValue().isEmpty()) {
//				log.warn("Error loading process definition from file system: "+result.getKey().getName()+"\r\n"+result.getValue());
//			}
//		});
//		tempStorePD.clear();		
//	}


	public Optional<ProcessDefinition> getProcessDefinition(String stringId, Boolean onlyValid) {
		List<ProcessDefinition> defs = context.getInstanceRepository().getAllInstancesOfTypeOrSubtype(processDefinitionType).stream()
				.filter(inst -> !inst.isMarkedAsDeleted())
				.filter(inst -> (Boolean)inst.getTypedProperty((ProcessDefinitionType.CoreProperties.isWithoutBlockingErrors.toString()), Boolean.class, false) || !onlyValid)
				.filter(inst -> inst.getName().equals(stringId))
				.map(inst -> (ProcessDefinition)context.getWrappedInstance(ProcessDefinition.class, inst))
				.collect(Collectors.toList());
		if (defs.isEmpty())
			return Optional.empty();
		if (defs.size() > 1)
			throw new RuntimeException("Duplicate non-deleted Processes: "+stringId);
		else
			return Optional.ofNullable(defs.get(0));
	}

	public ProcessDeployResult createProcessDefinitionIfNotExisting(DTOs.Process process) {
	//	if (!isInit) { tempStorePD.add(process); return null;} // may occur upon bootup 
		SimpleEntry<ProcessDefinition, List<ProcessDefinitionError>> newPD = storeProcessDefinition(process, false);
		return new ProcessDeployResult(newPD.getKey(), newPD.getValue(), Collections.emptyList());
	}

	public ProcessDeployResult createOrReplaceProcessDefinition(DTOs.Process process, boolean doReinstantiateExistingProcessInstances) {
	//	if (!isInit) { tempStorePD.add(process); return null;} // may occur upon bootup where we dont expect replacement to happen and resort to standard behavior or creating only but not replacing
		String originalCode = process.getCode();
		String tempCode = originalCode+STAGINGPOSTFIX;
		process.setCode(tempCode);
		DefinitionTransformer.replaceStepNamesInMappings(process, originalCode, tempCode);
		SimpleEntry<ProcessDefinition, List<ProcessDefinitionError>> stagedProc = storeProcessDefinition(process, true);
	
		if (!stagedProc.getValue().isEmpty()) {
			//undo step name replacement
			DefinitionTransformer.replaceStepNamesInMappings(process, tempCode, originalCode);
			return new ProcessDeployResult(stagedProc.getKey(), stagedProc.getValue(), Collections.emptyList());
		// if we continue here, then no process error occurred and we can continue
		}
		// we remove the staging one and replace the original
		if (stagedProc.getKey() != null) {
			stagedProc.getKey().deleteCascading();
			context.getInstanceRepository().concludeTransaction();
		}
		// now remove the original if exists, and store as new
		DefinitionTransformer.replaceStepNamesInMappings(process, tempCode, originalCode);
		process.setCode(originalCode);
		Optional<ProcessDefinition> prevPD = getProcessDefinition(process.getCode(), true);
		Map<String, Map<String, Set<PPEInstance>>> prevProcInput = new HashMap<>() ;
		if (prevPD.isPresent()) {
			log.debug("Removing existing process definition and instances thereof: "+process.getCode());
			prevProcInput = removeAllProcessInstancesOfProcessDefinition(prevPD.get());
			removeProcessDefinition(process.getCode());
		}
		SimpleEntry<ProcessDefinition, List<ProcessDefinitionError>> newPD = storeProcessDefinition(process, false);
		// if stages process has no error, so should this be, as its identical.
		List<ProcessInstanceError> pInstErrors = new LinkedList<>();
		if (doReinstantiateExistingProcessInstances) {
			prevProcInput.values().stream()
			.forEach(inputSet -> {
				SimpleEntry<ProcessInstance, List<ProcessInstanceError>> pInst = this.instantiateProcess(newPD.getKey(), inputSet);
				pInstErrors.addAll(pInst.getValue());
			});
		}
		return new ProcessDeployResult(newPD.getKey(), newPD.getValue(), pInstErrors);
	}

	public SimpleEntry<ProcessDefinition, List<ProcessDefinitionError>> storeProcessDefinition(DTOs.Process process, boolean isInStaging) {	
		boolean onlyValid = isInStaging ? false : true;
		Optional<ProcessDefinition> optPD = getProcessDefinition(process.getCode(), onlyValid);
		// in staging we overwrite existing process (as this is an old staged process)

		if (optPD.isPresent()) {
			if (isInStaging) {
				log.debug("Removing old staged process: "+process.getCode()+" before staging new version");
				ProcessDefinition pdef = optPD.get();
				pdef.deleteCascading();
				context.getInstanceRepository().concludeTransaction();
			} else {
				log.debug("Reusing process: "+process.getCode());
				return new SimpleEntry<>(optPD.get(), Collections.emptyList());
			}
		} 
		// no else as if in staging, we continue here in any case
		log.debug("Storing new process: "+process.getCode());
		DefinitionTransformer transformer = new DefinitionTransformer(process, context.getFactoryIndex(), context.getSchemaRegistry());			
		ProcessDefinition pd = transformer.fromDTO(isInStaging);
		List<ProcessDefinitionError> errors = transformer.getErrors();
		/*ProcessOverridingAnalysis poa=new ProcessOverridingAnalysis();
		poa.beginAnalysis(pd,errors ,ws);
		this.setOverride_warnings(errors);*/
		return new SimpleEntry<>(pd, errors);
	}

	/**
	 * 
	 * @param pDef 
	 * @return the map of inputs of all prior existing process instances by key of prior process id
	 */
	public Map<String, Map<String, Set<PPEInstance>>> removeAllProcessInstancesOfProcessDefinition(ProcessDefinition pDef) {
		// to be called before removing the process definition,
		// get the process definition instance type, get all instances thereof, then use the wrapper cache to obtain the process instance
		Map<String, Map<String, Set<PPEInstance>>> prevProcInput = new HashMap<>();

		// we actually dont need to find the process definition type, but the specific process instance type declaration
		PPEInstanceType specProcDefType = context.getSchemaRegistry().getTypeByName(SpecificProcessInstanceType.getProcessName(pDef)); 
		context.getInstanceRepository().getAllInstancesOfTypeOrSubtype(specProcDefType).stream()
		.filter(inst -> !inst.isMarkedAsDeleted())
		.map(inst -> context.getWrappedInstance(ProcessInstance.class, inst))
		.filter(Objects::nonNull)
		.map(ProcessInstance.class::cast)
		.forEach(procInst -> {
			processInstances.remove(procInst.getName());
			Map<String, Set<PPEInstance>> inputSet = new HashMap<>();
			procInst.getDefinition().getExpectedInput().keySet().stream()
			.forEach(input -> inputSet.put(input, procInst.getInput(input)));
			prevProcInput.put(procInst.getName(), inputSet);
			procInst.deleteCascading();
		});
		return prevProcInput;
	}

	public void removeProcessDefinition(String name) {
		getProcessDefinition(name, true).ifPresent(pdef -> {
			pdef.deleteCascading();
			context.getInstanceRepository().concludeTransaction();
		});
	}

	public Set<String> getAllDefinitionIDs(Boolean onlyValid) {
		return getAllDefinitions(onlyValid).stream()
				.map(procDef -> procDef.getName())
				.collect(Collectors.toSet());
	}

	public Set<ProcessDefinition> getAllDefinitions(Boolean onlyValid) {
		return context.getInstanceRepository().getAllInstancesOfTypeOrSubtype(processDefinitionType).stream() 
				.filter(inst -> !inst.isMarkedAsDeleted())
				.filter(inst -> (Boolean)inst.getTypedProperty(ProcessDefinitionType.CoreProperties.isWithoutBlockingErrors.toString(), Boolean.class, false) || !onlyValid)
				.map(inst -> (ProcessDefinition)context.getWrappedInstance(ProcessDefinition.class, inst))
				.collect(Collectors.toSet());
	}

	public SimpleEntry<ProcessInstance, List<ProcessInstanceError>> instantiateProcess(ProcessDefinition processDef, Map<String, Set<PPEInstance>> input) {
		// check if all inputs available:
		List<ProcessInstanceError> errors = new LinkedList<>();
		String namePostfix = generateProcessNamePostfix(input);
		ProcessInstance pInst = context.getFactoryIndex().getProcessInstanceFactory().getInstance(processDef, namePostfix);
		boolean allInputAvail = processDef.getExpectedInput().keySet().stream().allMatch(expextedInput -> input.containsKey(expextedInput));
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
			processInstances.put(pInst.getName(), pInst);
			context.getInstanceRepository().concludeTransaction();
			return new SimpleEntry<>(pInst, errors);
		} else {
			pInst.deleteCascading();
			context.getInstanceRepository().concludeTransaction();
			return new SimpleEntry<>(pInst, errors);
		}
	}

	public ProcessInstance getProcessByName(String name) {
		return processInstances.get(name);
	}

	public boolean removeProcessByName(String name) {
		ProcessInstance pi = processInstances.remove(name);
		if (pi != null) {
			pi.deleteCascading();
			context.getInstanceRepository().concludeTransaction();
			this.removedInstances.add(pi);
			return true;
		}
		return false;
	}

	public List<ProcessInstance> getNonDeletedProcessInstances() {
		List<ProcessInstance> all = new LinkedList<>(processInstances.values());		
		return all;
	}
	
	public List<ProcessInstance> getExistingAndPriorInstances() {
		List<ProcessInstance> all = new LinkedList<>(processInstances.values());
		all.addAll(removedInstances);
		return all;
	}

	protected Set<ProcessInstance> loadPersistedProcesses() {
		Set<ProcessInstance> existingProcessInstances = context.getInstanceRepository().getAllInstancesOfTypeOrSubtype(processInstanceType) //  context.getAllSubtypesRecursively(ProcessStep.getOrCreateDesignSpaceCoreSchema(ws))
				.stream()
				//.stream().filter(stepType -> stepType.name().startsWith(ProcessInstance.designspaceTypeId)) //everthing that is a process type
				//.flatMap(procType -> procType.instancesIncludingThoseOfSubtypes()) // everything that is a process instance
				.map(procInst -> context.getWrappedInstance(ProcessInstance.class, procInst)) // wrap instance
				.map(procInst -> (ProcessInstance)procInst)
				.collect(Collectors.toSet());
		log.info(String.format("Loaded %s preexisting process instances", existingProcessInstances.size()));
		existingProcessInstances.stream().forEach(pi -> processInstances.put(pi.getName(), pi));
		return existingProcessInstances;
	}

//	//TODO move into instancetype
//	private Set<InstanceType> getAllSubtypesRecursively(InstanceType type) {
//		Set<InstanceType> subTypes = type.subTypes();
//		for (InstanceType subType : Set.copyOf(subTypes)) {
//			subTypes.addAll( getAllSubtypesRecursively(subType));
//		}
//		return subTypes;
//	}

	public static String generateProcessNamePostfix(Map<String, Set<PPEInstance>> procInput) {
		return procInput.entrySet().stream()
				.flatMap(entry -> entry.getValue().stream())
				.map(inst -> inst.getName()).collect(Collectors.joining(",", "[", "]"));
	}

	@Data
	public static class ProcessDeployResult {
		final ProcessDefinition procDef;
		final List<ProcessDefinitionError> definitionErrors;
		final List<ProcessInstanceError> instanceErrors;
	}
}
