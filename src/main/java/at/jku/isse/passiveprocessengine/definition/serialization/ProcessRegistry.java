package at.jku.isse.passiveprocessengine.definition.serialization;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import at.jku.isse.designspace.core.model.InstanceType;
import at.jku.isse.designspace.core.model.Workspace;
import at.jku.isse.passiveprocessengine.WrapperCache;
import at.jku.isse.passiveprocessengine.definition.ProcessDefinition;

public class ProcessRegistry {
	
	protected Workspace ws;
	protected InstanceType procDefType;
	
	public ProcessRegistry(Workspace ws) {
		this.ws=ws;
		procDefType = ProcessDefinition.getOrCreateDesignSpaceCoreSchema(ws);
	}
	
	public Optional<ProcessDefinition> getProcessDefinition(String name) {
		return procDefType.getInstancesIncludingThoseOfSubtypes().stream()
			.filter(inst -> inst.name().equals(name))
			.map(inst -> (ProcessDefinition)WrapperCache.getWrappedInstance(ProcessDefinition.class, inst))
			.findAny();
	}
	
	public ProcessDefinition storeProcessDefinitionIfNotExists(DTOs.Process process) {
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
