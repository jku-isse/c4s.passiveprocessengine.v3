package at.jku.isse.passiveprocessengine.definition.serialization;

import at.jku.isse.designspace.core.model.InstanceType;
import at.jku.isse.designspace.core.model.Workspace;
import at.jku.isse.passiveprocessengine.definition.DecisionNodeDefinition;
import at.jku.isse.passiveprocessengine.definition.MappingDefinition;
import at.jku.isse.passiveprocessengine.definition.ProcessDefinition;
import at.jku.isse.passiveprocessengine.definition.QAConstraintSpec;
import at.jku.isse.passiveprocessengine.definition.StepDefinition;
import at.jku.isse.passiveprocessengine.definition.serialization.DTOs.Process;

public class DefinitionTransformer {

	public static ProcessDefinition fromDTO(DTOs.Process procDTO, Workspace ws) {
		ProcessDefinition procDef = ProcessDefinition.getInstance(procDTO.getCode(), ws);
		initProcessFromDTO(procDTO, procDef, ws);
		return procDef;
	}
	
	private static void initProcessFromDTO(DTOs.Process procDTO, ProcessDefinition pDef, Workspace ws) {
		// first DNDs
		procDTO.getDns().stream().forEach(dn -> { 
			DecisionNodeDefinition dnd = pDef.createDecisionNodeDefinition(dn.getCode(), ws);
			dnd.setInflowType(dn.getInflowType());
			dn.getMapping().stream().forEach(m -> 
				dnd.addDataMappingDefinition(
					MappingDefinition.getInstance(m.getFromStep(), m.getFromParam(), m.getToStep(), m.getToParam(), ws)) );
		});
		// then Steps
		procDTO.getSteps().stream().forEach(sd -> {
			StepDefinition sDef = null;
			if (sd instanceof DTOs.Process) { // a subprocess
				sDef = fromDTO((Process) sd, ws);
				sDef.setProcess(pDef);
				pDef.addStepDefinition(sDef);
			} else {
				sDef = pDef.createStepDefinition(sd.getCode() ,ws);
				initStepFromDTO(sd, sDef, ws);
			}
			sDef.setInDND(pDef.getDecisionNodeDefinitionByName(sd.getInDNDid()));
			sDef.setOutDND(pDef.getDecisionNodeDefinitionByName(sd.getOutDNDid()));
		});
		// then process itself
		initStepFromDTO(procDTO, pDef, ws);
	}
	
	private static void initStepFromDTO(DTOs.Step step, StepDefinition pStep, Workspace ws) {
		step.getInput().entrySet().stream().forEach(entry -> pStep.addExpectedInput(entry.getKey(), resolveInstanceType(entry.getValue(), ws)));
		step.getOutput().entrySet().stream().forEach(entry -> pStep.addExpectedOutput(entry.getKey(), resolveInstanceType(entry.getValue(), ws)));
		step.getConditions().entrySet().stream().forEach(entry -> pStep.setCondition(entry.getKey(), entry.getValue()));
		step.getIoMapping().entrySet().stream().forEach(entry -> pStep.addInputToOutputMappingRule(entry.getKey(),  entry.getValue()));
		step.getQaConstraints().stream().forEach(qac -> pStep.addQAConstraint(QAConstraintSpec.createInstance(qac.getCode(), qac.getArlRule(), qac.getDescription(), ws)));
		//FIXME: description field is not used in specification (only in persistance)
	}
	
	private static InstanceType resolveInstanceType(String type, Workspace ws) {
		return ws.debugInstanceTypeFindByName(type);
	}
	
	
}
