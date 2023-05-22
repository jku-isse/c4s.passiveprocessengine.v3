package at.jku.isse.passiveprocessengine.instance;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import at.jku.isse.passiveprocessengine.definition.DecisionNodeDefinition;
import at.jku.isse.passiveprocessengine.definition.MappingDefinition;
import at.jku.isse.passiveprocessengine.instance.RuntimeMapping.FlowDir;
import at.jku.isse.passiveprocessengine.instance.StepLifecycle.State;
import at.jku.isse.passiveprocessengine.instance.messages.Events;
import at.jku.isse.passiveprocessengine.instance.messages.Events.DataMappingChangedEvent;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class InterStepDataMapper {
	
	private DecisionNodeDefinition definition; 
	private ProcessInstance process;
	
	public InterStepDataMapper(DecisionNodeDefinition definition, ProcessInstance process) {
		this.definition = definition;
		this.process = process;
	}

	protected List<Events.ProcessChangedEvent> checkAndExecuteDataMappings(boolean isEndOfProcess, boolean isPremature) {
		// all mappings of this DNI resolved:
		Set<RuntimeMapping> templates = definition.getMappings().stream()
				.map(mdef -> resolveMappingDefinitionToTemplates(mdef, isEndOfProcess, isPremature))
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());

		// get desired and actual mappings
		Set<RuntimeMapping> desiredMappings = templates.stream().flatMap(em -> fillViaSource(em)).collect(Collectors.toSet());
		Set<RuntimeMapping> actualMappings = templates.stream().flatMap(em -> fillViaDestination(em)).collect(Collectors.toSet());

		// in desired and actual --> keep
		Set<RuntimeMapping> keepM = desiredMappings.stream().filter(entry -> actualMappings.contains(entry)).collect(Collectors.toSet());
		// in desired and not actual --> add
		Set<RuntimeMapping> newM = desiredMappings.stream().filter(entry -> !actualMappings.contains(entry)).collect(Collectors.toSet());
		// in actual but not desired --> remove
		Set<RuntimeMapping> delM = actualMappings.stream().filter(entry -> !desiredMappings.contains(entry)).collect(Collectors.toSet());
		// tricky bit: the same artifact could be affected by multiple mappings, a positive mapping overrides a negative mapping, no artifact is added twice
		// thus check, any remove entry that is found in desired entry is kept, --> BUT BEWARE: check based on destination step, param, and artifact, but not based on source.
		 // so for each step, param, art, in toRemove --> check if found in toAdd, we do this in an inefficient double loop, but as mappings are expected to be small in size, this should be fine
		Stream<RuntimeMapping> delFinallyM = delM.stream().filter(rmDel -> {
//					return !newM.stream().anyMatch(rmAdd -> (rmAdd.getArtifact() == rmDel.getArtifact() 
//													&& rmAdd.getToStep() == rmDel.getToStep()
//													&& rmAdd.getToParam() == rmDel.getToParam()) );
					return !newM.contains(rmDel); // we have hashCode and equals use only those three properties
				});
		
		//Set<RuntimeMapping> preparedMappings =  prepareDataflow(isEndOfProcess, getDefinition().getMappings());
		// now we know which to add and which to keep, next determine which to remove!
		//Set<RuntimeMapping> combined = Stream.concat(preparedMappings.stream(), 
		//											determineUndoMappingStatus(preparedMappings, getDefinition().getMappings()).stream())
		//       								.collect(Collectors.toSet()); 
		List<Events.ProcessChangedEvent> events = new LinkedList<>();
		keepM.forEach(em -> {
			log.debug("Keep: "+em.toString());
		});
		newM.forEach(em -> {
			log.debug("Add: "+em.toString());
			execute(em);
			events.add(new DataMappingChangedEvent(process)); //TODO: put meaningful data into event
		});
		delFinallyM.forEach(em -> {
			log.debug("Del: "+em.toString());
			undo(em);
			events.add(new DataMappingChangedEvent(process));//TODO: put meaningful data into event
		});
		return events;
	}
	
	private Stream<RuntimeMapping> fillViaSource(RuntimeMapping templateEM) {
			switch(templateEM.getDirection()) {
			case inToIn: //fallthrough
			case inToOut: // for input from process
				return templateEM.getFromStep().getInput(templateEM.getFromParam())
						.stream()
						.map(art -> RuntimeMapping.copyFrom(templateEM).fluentSetArtifact(art));
						
			case outToIn: //fallthrough
			case outToOut: //for output from task
				// we checked for completed task earlier when adding it to the template
				return templateEM.getFromStep().getOutput(templateEM.getFromParam())
															.stream()
															.map(art -> RuntimeMapping.copyFrom(templateEM).fluentSetArtifact(art));							
			default:
				return Stream.empty();
			}
	}

	private Stream<RuntimeMapping> fillViaDestination(RuntimeMapping templateEM) {
		switch(templateEM.getDirection()) {
		case inToIn: //fallthrough
		case outToIn:	
			// we checked for completed task earlier when adding it to the template
			return templateEM.getToStep().getInput(templateEM.getToParam())
														.stream()
														.map(art -> RuntimeMapping.copyFrom(templateEM).fluentSetArtifact(art));
		case outToOut: // fallthrough	
		case inToOut: 
			return templateEM.getToStep().getOutput(templateEM.getToParam())
					.stream()
					.map(art -> RuntimeMapping.copyFrom(templateEM).fluentSetArtifact(art));
		default:
			return Stream.empty();
		}
}
	
	private void execute(RuntimeMapping mapping) {
		//mappings.add(mapping);
		switch (mapping.getDirection()) {		
			case inToIn : //fallthrough
			case outToIn :
				mapping.getToStep().addInput(mapping.getToParam(), mapping.getArtifact());
			//	this.mappings.add(mapping);
				break;
			case inToOut :// fallthrough
			case outToOut :
				mapping.getToStep().addOutput(mapping.getToParam(), mapping.getArtifact());
			//	this.mappings.add(mapping);
				break;
		}
	}
	
	private void undo(RuntimeMapping mapping) {
		switch (mapping.getDirection()) {		
		case inToIn : //fallthrough
		case outToIn :
			mapping.getToStep().removeInput(mapping.getToParam(), mapping.getArtifact());
			break;
		case inToOut :// fallthrough
		case outToOut :
			mapping.getToStep().removeOutput(mapping.getToParam(), mapping.getArtifact());
			break;
		}
	}
	
	private RuntimeMapping resolveMappingDefinitionToTemplates(MappingDefinition mdef, boolean isEndOfProcess, boolean isPremature) {
		RuntimeMapping templateEM = new RuntimeMapping();
		templateEM.setFromParam(mdef.getFromParameter());
		templateEM.setToParam(mdef.getToParameter());
		// first get the source
		process.getProcessSteps().stream()	
		.filter(wft -> wft.getDefinition().getName().equals(mdef.getFromStepType()) )
		.filter(wft -> isPremature || (wft.getExpectedLifecycleState().equals(State.COMPLETED) &&  wft.getActualLifecycleState().equals(State.COMPLETED))) // we only map data for tasks that are indeed completed or we map for a premature step
		.filter(Objects::nonNull)
		.findFirst().ifPresentOrElse( wft -> { if (isEndOfProcess) 
												templateEM.setDirection(FlowDir.outToOut);
											else
												templateEM.setDirection(FlowDir.outToIn);
											templateEM.setFromStep(wft);
									} ,
			() -> {
			if (process.getDefinition().getName().equals(mdef.getFromStepType())) { // the taskId identifies the process
				if (isEndOfProcess) 
					templateEM.setDirection(FlowDir.inToOut);
				else
					templateEM.setDirection(FlowDir.inToIn);
				templateEM.setFromStep(process);
			}
		});
		if (templateEM.getFromStep() == null) // not found, e.g., a step not yet created, can only happen when premature step is not connected to remaining process
			return null;
		
		// now do the destination part
		switch(templateEM.getDirection()) {
		case outToIn: //fallthrough
		case inToIn: // for input from process to input of task
			process.getProcessSteps().stream()	
			.filter(wft -> wft.getDefinition().getName().equals(mdef.getToStepType()) )
			.map(wft -> (ProcessStep)wft)
			.findAny()
			.ifPresent( wft -> { templateEM.setToStep(wft);
							});
			break;
		case inToOut: //fallthrough
		case outToOut: //for output from task
			if (process.getDefinition().getName().equals(mdef.getToStepType())) {
				templateEM.setToStep(process);
			} 
			break;
		default:
			;
		}
		
		if (templateEM.getToStep() == null) { // not found, e.g., a step not yet created, should not happen 
			//(might happen if only one of many subsequent steps are prematurely started, thus the other not yet instantiated)
			return null;
		}
		return templateEM;
	}
	
	protected void delete() {
		this.definition = null;
		this.process = null;
	}
}
