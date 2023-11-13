package at.jku.isse.passiveprocessengine.definition.serialization;

import at.jku.isse.designspace.core.model.InstanceType;
import at.jku.isse.designspace.core.model.Workspace;
import at.jku.isse.passiveprocessengine.definition.DecisionNodeDefinition;
import at.jku.isse.passiveprocessengine.definition.MappingDefinition;
import at.jku.isse.passiveprocessengine.definition.ProcessDefinition;
import at.jku.isse.passiveprocessengine.definition.QAConstraintSpec;
import at.jku.isse.passiveprocessengine.definition.StepDefinition;
import at.jku.isse.passiveprocessengine.definition.serialization.DTOs.Process;
import at.jku.isse.passiveprocessengine.instance.StepLifecycle.Conditions;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DefinitionTransformer {

	public static ProcessDefinition fromDTO(DTOs.Process procDTO, Workspace ws, boolean isInStaging) {
		ProcessDefinition procDef = ProcessDefinition.getInstance(procDTO.getCode(), ws);
		initProcessFromDTO(procDTO, procDef, ws, 0, isInStaging);
		return procDef;
	}
	
	private static void initProcessFromDTO(DTOs.Process procDTO, ProcessDefinition pDef, Workspace ws, int depth, boolean isInStaging) {
		if (!isInStaging) {
			cleanStagingRewriting(procDTO);
		}
		
		// first DNDs
		procDTO.getDns().stream().forEach(dn -> { 
			DecisionNodeDefinition dnd = pDef.createDecisionNodeDefinition(dn.getCode(), ws);
			dnd.setInflowType(dn.getInflowType());
			// we need to set the mappings only later, in case there are subprocesses which update the mapping name
//			dn.getMapping().stream().forEach(m -> 
//				dnd.addDataMappingDefinition(
//					MappingDefinition.getInstance(m.getFromStep(), m.getFromParam(), m.getToStep(), m.getToParam(), ws)) );
		});
		// then Steps
		procDTO.getSteps().stream().forEach(sd -> {
			StepDefinition sDef = null;
			if (sd instanceof DTOs.Process) { // a subprocess
				sDef = createSubprocess((DTOs.Process)sd, ws, procDTO, isInStaging);
				sDef.setProcess(pDef);
				pDef.addStepDefinition(sDef);
			} else {
				sDef = pDef.createStepDefinition(sd.getCode() ,ws);
				initStepFromDTO(sd, sDef, ws);
			}
			sDef.setInDND(pDef.getDecisionNodeDefinitionByName(sd.getInDNDid()));
			sDef.setOutDND(pDef.getDecisionNodeDefinitionByName(sd.getOutDNDid()));
		});
		//then create the DND mappings
		procDTO.getDns().stream()			
			.forEach(dn -> { 
				DecisionNodeDefinition dnd = pDef.getDecisionNodeDefinitionByName(dn.getCode());
				dn.getMapping().stream().forEach(m -> 
					dnd.addDataMappingDefinition(
						MappingDefinition.getInstance(m.getFromStep(), m.getFromParam(), m.getToStep(), m.getToParam(), ws)) );	
		});
		// then process itself
		initStepFromDTO(procDTO, pDef, ws);
		
		pDef.setDepthIndexRecursive(depth);
		pDef.setElementOrder();
	}
	
	private static ProcessDefinition createSubprocess(DTOs.Process subProcess, Workspace ws, DTOs.Process parentProc, boolean isInStaging) {
		// first rename the subprocess to be unique and
		String parentProcName = parentProc.getCode();
		String oldSubProcName = subProcess.getCode();
		String newSubProcName = subProcess.getCode()+"-"+parentProcName; 
		subProcess.setCode(newSubProcName);		
		// then update mappings
		replaceStepNamesInMappings(subProcess, oldSubProcName, newSubProcName); // in the subprocess
		replaceStepNamesInMappings(parentProc, oldSubProcName, newSubProcName); // but also in the parent process, but WONT undo later 
		
		ProcessDefinition pDef = fromDTO((Process) subProcess, ws, isInStaging);
		//undo mappings and naming
		replaceStepNamesInMappings(subProcess, newSubProcName, oldSubProcName);
//		if (isInStaging) {
//			replaceStepNamesInMappings(parentProc, newSubProcName, oldSubProcName);
//		}
		subProcess.setCode(oldSubProcName);
		
		return pDef;
	}
	
	protected static void cleanStagingRewriting(DTOs.Process process) {
		process.getDns().forEach(dn -> 
		dn.getMapping().stream()
		.filter(mapping -> mapping.getFromStep().endsWith(ProcessRegistry.STAGINGPOSTFIX))
		.forEach(mapping -> { 
			String restoredName = mapping.getFromStep().replace(ProcessRegistry.STAGINGPOSTFIX, "");
			mapping.setFromStep(restoredName); 
		}));
	process.getDns().forEach(dn -> 
		dn.getMapping().stream()
		.filter(mapping -> mapping.getToStep().endsWith(ProcessRegistry.STAGINGPOSTFIX))
		.forEach(mapping -> { 
			String restoredName = mapping.getToStep().replace(ProcessRegistry.STAGINGPOSTFIX, "");
			mapping.setToStep(restoredName); 
		}));
	}
	
	protected static void replaceStepNamesInMappings(DTOs.Process process, String oldStepName, String newStepName) {
		process.getDns().forEach(dn -> 
			dn.getMapping().stream()
			.filter(mapping -> mapping.getFromStep().equals(oldStepName))
			.forEach(mapping -> mapping.setFromStep(newStepName)));
		process.getDns().forEach(dn -> 
			dn.getMapping().stream()
			.filter(mapping -> mapping.getToStep().equals(oldStepName))
			.forEach(mapping -> mapping.setToStep(newStepName)));
	}
	
	private static void initStepFromDTO(DTOs.Step step, StepDefinition pStep, Workspace ws) {
		
		step.getInput().entrySet().stream().forEach(entry -> pStep.addExpectedInput(entry.getKey(), resolveInstanceType(entry.getValue(), ws)));
		step.getOutput().entrySet().stream().forEach(entry -> pStep.addExpectedOutput(entry.getKey(), resolveInstanceType(entry.getValue(), ws)));
		step.getConditions().entrySet().stream().forEach(entry -> pStep.setCondition(entry.getKey(), entry.getValue()));
		step.getIoMapping().entrySet().stream().forEach(entry -> pStep.addInputToOutputMappingRule(entry.getKey(),  trimLegacyIOMappingRule(entry.getValue())));
		step.getQaConstraints().stream().forEach(qac -> { 
			QAConstraintSpec spec = QAConstraintSpec.createInstance(qac.getCode(), qac.getArlRule(), qac.getDescription(), qac.getSpecOrderIndex(), ws);
			if (pStep instanceof ProcessDefinition) {
				spec.setProcess((ProcessDefinition)pStep);
			} else if (pStep.getProcess() != null) {
				spec.setProcess(pStep.getProcess());
			}
			pStep.addQAConstraint(spec); });
		pStep.setSpecOrderIndex(step.getSpecOrderIndex());
		pStep.setHtml_url(step.getHtml_url());
		pStep.setDescription(step.getDescription());		
	}
	
	private static InstanceType resolveInstanceType(String type, Workspace ws) {
		InstanceType iType = ws.debugInstanceTypeFindByName(type);
		if (iType == null) {
			log.warn("Process Description uses unknown instance type: "+type);
		}
		return iType;
	}
	
	public static DTOs.Process toDTO(ProcessDefinition pDef) {
		DTOs.Process proc = new Process();
		pDef.getStepDefinitions().stream().forEach(pStep -> {
			DTOs.Step step = new DTOs.Step();
			if (pStep instanceof ProcessDefinition) {
				proc.getSteps().add(toDTO((ProcessDefinition) pStep));
			} else {
				initDTOfromStep(step, pStep);
				proc.getSteps().add(step);
			}
			
		});
		pDef.getDecisionNodeDefinitions().stream().forEach(dnd -> {
			DTOs.DecisionNode dn = new DTOs.DecisionNode();
			dn.setCode(dnd.getName());
			dn.setInflowType(dnd.getInFlowType());
			dnd.getMappings().stream().forEach(md -> {
				DTOs.Mapping mapping = new DTOs.Mapping(md.getFromStepType(), md.getFromParameter(), md.getToStepType(), md.getToParameter());
				dn.getMapping().add(mapping);
				//TODO: description
			});
			proc.getDns().add(dn);
		});
		initDTOfromStep(proc, pDef);
		return proc;
	}
	
	private static void initDTOfromStep(DTOs.Step step, StepDefinition pStep) {
		step.setCode(pStep.getName());
		if (pStep.getInDND() != null)
			step.setInDNDid(pStep.getInDND().getName());
		if (pStep.getOutDND() != null)
			step.setOutDNDid(pStep.getOutDND().getName());
		pStep.getExpectedInput().entrySet().stream().forEach(entry -> step.getInput().put(entry.getKey(), entry.getValue().name()));
		pStep.getExpectedOutput().entrySet().stream().forEach(entry -> step.getOutput().put(entry.getKey(), entry.getValue().name()));
		pStep.getQAConstraints().stream().forEach(qac -> { 
			DTOs.QAConstraint qa = new DTOs.QAConstraint();
			qa.setArlRule(qac.getQaConstraintSpec());
			qa.setCode(qac.getQaConstraintId());
			qa.setDescription(qac.getHumanReadableDescription());
			qa.setSpecOrderIndex(qac.getOrderIndex());
			step.getQaConstraints().add(qa ); 
		});
		pStep.getInputToOutputMappingRules().entrySet().stream().forEach(entry -> step.getIoMapping().put(entry.getKey(), entry.getValue()));
		for (Conditions cond : Conditions.values()) {
			pStep.getCondition(cond).ifPresent(condARL -> step.getConditions().put(cond, condARL));
		}
		step.setHtml_url(pStep.getHtml_url());
		step.setDescription(pStep.getDescription());
		//TODO: description field
	}
	
	private static String trimLegacyIOMappingRule(String ruleString) {
		int posLegacySymDiff = stripForComparison(ruleString).indexOf("asSet()symmetricDifference(self.out");
		if (posLegacySymDiff > 0) {
			return ruleString.substring(0, posLegacySymDiff);
		} else
			return ruleString;
	}
	
	public static String stripForComparison(String arl) {
		return arl
			.replace("->", "")
			.replace(".", "")
			.replaceAll("[\\n\\t ]", "")
			.trim();
	}
}
