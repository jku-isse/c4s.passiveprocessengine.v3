package at.jku.isse.passiveprocessengine.definition.serialization;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import at.jku.isse.designspace.core.model.InstanceType;
import at.jku.isse.designspace.core.model.Workspace;
import at.jku.isse.passiveprocessengine.WrapperCache;
import at.jku.isse.passiveprocessengine.definition.ProcessDefinition;
import at.jku.isse.passiveprocessengine.instance.ProcessException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ProcessRegistry {
	
	protected Workspace ws;
	protected InstanceType procDefType;
	
	protected Set<DTOs.Process> cachePD = new HashSet<>();
	protected boolean isInit = false;
	
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
		cachePD.forEach(pd -> {
			try {
				storeProcessDefinitionIfNotExists(pd);
			} catch (ProcessException e) {
				e.printStackTrace();
			}
		});
		cachePD.clear();
	}
	
	
	public Optional<ProcessDefinition> getProcessDefinition(String name) {
		return procDefType.getInstancesIncludingThoseOfSubtypes().stream()
				.filter(inst -> !inst.isDeleted)
				.filter(inst -> inst.name().equals(name))
			.map(inst -> (ProcessDefinition)WrapperCache.getWrappedInstance(ProcessDefinition.class, inst))
			.findAny();
	}
	
	public ProcessDefinition storeProcessDefinitionIfNotExists(DTOs.Process process) throws ProcessException {
		if (!isInit) { cachePD.add(process); return null;}
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
	
	public void removeProcessDefinition(String name) {
		getProcessDefinition(name).ifPresent(pdef -> { 
			WrapperCache.removeWrapper(pdef.getInstance().id());
			pdef.deleteCascading();
			ws.concludeTransaction();
		});  
	}
	
	public Set<String> getAllDefinitionIDs() {
		 return procDefType.getInstancesIncludingThoseOfSubtypes().stream()
		 	.map(inst -> inst.name())
		 	.collect(Collectors.toSet());
	}
	
	public Set<ProcessDefinition> getAllDefinitions() {
		return procDefType.getInstancesIncludingThoseOfSubtypes().stream()
			.map(inst -> (ProcessDefinition)WrapperCache.getWrappedInstance(ProcessDefinition.class, inst))
			.collect(Collectors.toSet());
	}
}
