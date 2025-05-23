package at.jku.isse.passiveprocessengine.definition.registry;

import java.util.AbstractMap.SimpleEntry;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import at.jku.isse.passiveprocessengine.rdfwrapper.RDFElement;
import at.jku.isse.passiveprocessengine.rdfwrapper.RDFInstance;
import at.jku.isse.passiveprocessengine.rdfwrapper.RDFInstanceType;
import at.jku.isse.passiveprocessengine.rdfwrapper.rule.RuleEnabledResolver;
import at.jku.isse.passiveprocessengine.core.FactoryIndex;
import at.jku.isse.passiveprocessengine.definition.ProcessDefinitionError;
import at.jku.isse.passiveprocessengine.definition.activeobjects.ProcessDefinition;
import at.jku.isse.passiveprocessengine.definition.types.ProcessDefinitionTypeFactory;
import at.jku.isse.passiveprocessengine.instance.ProcessInstanceError;
import at.jku.isse.passiveprocessengine.instance.activeobjects.ProcessInstance;
import at.jku.isse.passiveprocessengine.instance.factories.ProcessInstanceFactory;
import at.jku.isse.passiveprocessengine.instance.types.AbstractProcessInstanceType;
import at.jku.isse.passiveprocessengine.instance.types.SpecificProcessInstanceType;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ProcessRegistry {

	private final RuleEnabledResolver context;
	private final FactoryIndex factoryIndex;

	protected RDFInstanceType processDefinitionType;
	protected RDFInstanceType processInstanceType;

	protected Map<String, ProcessInstance> processInstances = new HashMap<>();
	protected List<ProcessInstance> removedInstances = new LinkedList<>();

	public static final String STAGINGPOSTFIX = "-STAGING";

	public ProcessRegistry(RuleEnabledResolver context, FactoryIndex factoryIndex) {
		this.context = context;
		this.factoryIndex = factoryIndex;
		var processDefinitionTypeOpt = context.findNonDeletedInstanceTypeByFQN(ProcessDefinitionTypeFactory.typeId); // ProcessDefinition.getOrCreateDesignSpaceCoreSchema(ws);				
		var processInstanceTypeOpt = context.findNonDeletedInstanceTypeByFQN(AbstractProcessInstanceType.typeId); 		
		if (processDefinitionTypeOpt.isEmpty() || processInstanceTypeOpt.isEmpty()) {
			var msg = "Expected Basic Types for ProcessDefinition and/or ProcessInstance are null";
			log.error(msg);
			throw new RuntimeException(msg);
		} else {
			processDefinitionType = processDefinitionTypeOpt.get();
			processInstanceType = processDefinitionTypeOpt.get();
		}		
		context.getAllNonDeletedInstanceTypes().stream().forEach(itype -> log.debug(String.format("Available instance type %s ", itype.getName())));
		loadPersistedProcesses();
	}
	
	public Optional<ProcessDefinition> getProcessDefinition(String stringId, Boolean onlyValid) {
		var allProcDefs = context.getAllInstancesOfTypeOrSubtype(processDefinitionType);
		List<ProcessDefinition> defs = allProcDefs.stream()
				.filter(inst -> !inst.isMarkedAsDeleted())
				.filter(inst -> inst.getTypedProperty((ProcessDefinitionTypeFactory.CoreProperties.isWithoutBlockingErrors.toString()), Boolean.class, false) || !onlyValid)
				.filter(inst -> inst.getName().equals(stringId))
				.map(ProcessDefinition.class::cast) // the NodeToDomainResolver already creates the most specific java class type, here ProcessDefinition
				.map(procDef -> { procDef.injectFactories(factoryIndex.getStepDefinitionFactory()
						, factoryIndex.getDecisionNodeDefinitionFactory()); return procDef; })
				.toList();
		if (defs.isEmpty())
			return Optional.empty();
		if (defs.size() > 1)
			throw new RuntimeException("Duplicate non-deleted Processes: "+stringId);
		else
			return Optional.ofNullable(defs.get(0));
	}

	public ProcessDeployResult createProcessDefinitionIfNotExisting(DTOs.Process process) {
		Optional<ProcessDefinition> optPD = getProcessDefinition(process.getCode(), true);
		if (optPD.isPresent()) {
			return new ProcessDeployResult(optPD.get(), Collections.emptyList(), Collections.emptyList());
		}
		
		SimpleEntry<ProcessDefinition, List<ProcessDefinitionError>> newPD = storeProcessDefinition(process, true);
		return new ProcessDeployResult(newPD.getKey(), newPD.getValue(), Collections.emptyList());
	}

	public ProcessDeployResult createOrReplaceProcessDefinition(DTOs.Process process, boolean doReinstantiateExistingProcessInstances) {
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
		}
		// now remove the original if exists, and store as new
		DefinitionTransformer.replaceStepNamesInMappings(process, tempCode, originalCode);
		process.setCode(originalCode);
		Optional<ProcessDefinition> prevPD = getProcessDefinition(process.getCode(), true);
		Map<String, Map<String, Set<RDFInstance>>> prevProcInput = new HashMap<>() ;
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
				//context.concludeTransaction();
			} else {
				log.debug("Reusing process: "+process.getCode());
				return new SimpleEntry<>(optPD.get(), Collections.emptyList());
			}
		} 
		// no else as if in staging, we continue here in any case
		log.debug("Storing new process: "+process.getCode());
		DefinitionTransformer transformer = new DefinitionTransformer(process, factoryIndex, context);			
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
	public Map<String, Map<String, Set<RDFInstance>>> removeAllProcessInstancesOfProcessDefinition(ProcessDefinition pDef) {
		// to be called before removing the process definition,
		// get the process definition instance type, get all instances thereof, then use the wrapper cache to obtain the process instance
		Map<String, Map<String, Set<RDFInstance>>> prevProcInput = new HashMap<>();

		// we actually dont need to find the process definition type, but the specific process instance type declaration
		var specProcDefTypeOpt = context.findNonDeletedInstanceTypeByFQN(SpecificProcessInstanceType.getProcessName(pDef));
		if (specProcDefTypeOpt.isPresent()) {					
			context.getAllInstancesOfTypeOrSubtype(specProcDefTypeOpt.get()).stream()
			.filter(inst -> !inst.isMarkedAsDeleted())
			.map(ProcessInstance.class::cast)
			.filter(Objects::nonNull)
			.map(ProcessInstance.class::cast)
			.forEach(procInst -> {
				processInstances.remove(procInst.getName());
				Map<String, Set<RDFInstance>> inputSet = new HashMap<>();
				procInst.getDefinition().getExpectedInput().keySet().stream()
				.forEach(input -> inputSet.put(input, procInst.getInput(input)));
				prevProcInput.put(procInst.getName(), inputSet);
				procInst.deleteCascading();
		});
		}
		return prevProcInput;
	}

	public void removeProcessDefinition(String name) {
		getProcessDefinition(name, true).ifPresent(pdef -> {
			pdef.deleteCascading();
		});
	}

	public Set<String> getAllDefinitionIDs(Boolean onlyValid) {
		return getAllDefinitions(onlyValid).stream()
				.map(procDef -> procDef.getName())
				.collect(Collectors.toSet());
	}

	public Set<ProcessDefinition> getAllDefinitions(Boolean onlyValid) {
		var allDefs =  context.getAllInstancesOfTypeOrSubtype(processDefinitionType);
		return allDefs.stream() 
				.filter(inst -> !inst.isMarkedAsDeleted())
				.filter(inst -> inst.getTypedProperty(ProcessDefinitionTypeFactory.CoreProperties.isWithoutBlockingErrors.toString(), Boolean.class, false) || !onlyValid)
				.map(ProcessDefinition.class::cast)
				.collect(Collectors.toSet());
	}

	public boolean existsProcess(ProcessDefinition processDef, Map<String, Set<RDFInstance>> input) {
		var namePostfix = generateProcessNamePostfix(input);
		var id = ProcessInstanceFactory.generateId(processDef, namePostfix);
		return this.getProcessByName(id) != null;
	}
	
	public SimpleEntry<ProcessInstance, List<ProcessInstanceError>> instantiateProcess(ProcessDefinition processDef, Map<String, Set<RDFInstance>> input) {
		// check if all inputs available:
		List<ProcessInstanceError> errors = new LinkedList<>();
		String namePostfix = generateProcessNamePostfix(input);
		ProcessInstance pInst = factoryIndex.getProcessInstanceFactory().getInstance(processDef, namePostfix);
		boolean allInputAvail = processDef.getExpectedInput().keySet().stream().allMatch(input::containsKey);
		if (!allInputAvail)
			errors.add(new ProcessInstanceError(pInst, "Input Invalid",  "Unable to instantiate process due to missing input"));
		else {
			errors.addAll(input.entrySet().stream()
					.flatMap(entry ->  entry.getValue().stream().map(inputV -> pInst.addInput(entry.getKey(), inputV)))
					.filter(resp -> resp.getError() != null)
					.map(resp -> new ProcessInstanceError(pInst, "Input Invalid", resp.getError()))
					.toList());
		}
		if (errors.isEmpty()) {
			processInstances.put(pInst.getName(), pInst);
			return new SimpleEntry<>(pInst, errors);
		} else {
			pInst.deleteCascading();
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
			this.removedInstances.add(pi);
			return true;
		}
		return false;
	}

	public List<ProcessInstance> getNonDeletedProcessInstances() {
		List<ProcessInstance> all = new LinkedList<>(processInstances.values());		
		return all;
	}
	
	@Deprecated
	public List<ProcessInstance> getExistingAndPriorInstances() {
		List<ProcessInstance> all = new LinkedList<>(processInstances.values());
		all.addAll(removedInstances);
		return all;
	}
	
	public Collection<ProcessInstance> getProcessInstances() {
		return processInstances.values();
	}

	protected Set<ProcessInstance> loadPersistedProcesses() {
		Set<ProcessInstance> existingProcessInstances = context.getAllInstancesOfTypeOrSubtype(processInstanceType) //  context.getAllSubtypesRecursively(ProcessStep.getOrCreateDesignSpaceCoreSchema(ws))
				.stream()
				//.stream().filter(stepType -> stepType.name().startsWith(ProcessInstance.designspaceTypeId)) //everthing that is a process type
				//.flatMap(procType -> procType.instancesIncludingThoseOfSubtypes()) // everything that is a process instance
				.map(ProcessInstance.class::cast) // wrap instance				
				.collect(Collectors.toSet());
		log.info(String.format("Loaded %s preexisting process instances", existingProcessInstances.size()));
		existingProcessInstances.stream().forEach(pi -> processInstances.put(pi.getName(), pi));
		return existingProcessInstances;
	}

	public static String generateProcessNamePostfix(Map<String, Set<RDFInstance>> procInput) {
		return procInput.entrySet().stream()
				.flatMap(entry -> entry.getValue().stream())
				.map(RDFElement::getName)
				.collect(Collectors.joining("-"));
	}

	@Data
	public static class ProcessDeployResult {
		final ProcessDefinition procDef;
		final List<ProcessDefinitionError> definitionErrors;
		final List<ProcessInstanceError> instanceErrors;
	}
}
