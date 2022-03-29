package at.jku.isse.passiveprocessengine.definition.serialization;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import at.jku.isse.designspace.core.model.InstanceType;
import at.jku.isse.designspace.core.model.Workspace;
import at.jku.isse.passiveprocessengine.WrapperCache;
import at.jku.isse.passiveprocessengine.definition.ProcessDefinition;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ProcessRegistry {
	
	protected Workspace ws;
	protected InstanceType procDefType;
	
	protected Set<DTOs.Process> cachePD = new HashSet<>();
	protected boolean isInit = false;
	
	public ProcessRegistry() {
		
	}
	
	public void inject(Workspace ws) {
		this.ws=ws;
		procDefType = ProcessDefinition.getOrCreateDesignSpaceCoreSchema(ws);	
		ws.debugInstanceTypes().parallelStream().forEach(itype -> log.debug(String.format("Available instance type %s as %s", itype.name(), itype.getQualifiedName())));
		
		isInit = true;
		cachePD.forEach(pd -> storeProcessDefinitionIfNotExists(pd));
		cachePD.clear();
	}
	
	
	public Optional<ProcessDefinition> getProcessDefinition(String name) {
		return procDefType.getInstancesIncludingThoseOfSubtypes().stream()
			.filter(inst -> inst.name().equals(name))
			.map(inst -> (ProcessDefinition)WrapperCache.getWrappedInstance(ProcessDefinition.class, inst))
			.findAny();
	}
	
	public ProcessDefinition storeProcessDefinitionIfNotExists(DTOs.Process process) {
		if (!isInit) { cachePD.add(process); return null;}
		
		Optional<ProcessDefinition> optPD = getProcessDefinition(process.getCode());
		if (optPD.isEmpty()) {
			return DefinitionTransformer.fromDTO(process, ws);
		} else
			return optPD.get();
	}
	
	public void removeProcessDefinition(String name) {
		getProcessDefinition(name).ifPresent(pdef -> { 
			WrapperCache.removeWrapper(pdef.getInstance().id());
			pdef.getInstance().delete(); });  
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
