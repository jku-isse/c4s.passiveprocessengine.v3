package at.jku.isse.passiveprocessengine.definition.serialization;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import at.jku.isse.designspace.core.model.Folder;
import at.jku.isse.designspace.core.model.InstanceType;
import at.jku.isse.designspace.core.model.Workspace;
import at.jku.isse.passiveprocessengine.ProcessDefinitionScopedElement;
import at.jku.isse.passiveprocessengine.configurability.ProcessConfigBaseElementFactory;
import at.jku.isse.passiveprocessengine.definition.ConstraintSpec;
import at.jku.isse.passiveprocessengine.definition.DecisionNodeDefinition;
import at.jku.isse.passiveprocessengine.definition.MappingDefinition;
import at.jku.isse.passiveprocessengine.definition.ProcessDefinition;
import at.jku.isse.passiveprocessengine.definition.ProcessDefinitionError;
import at.jku.isse.passiveprocessengine.definition.StepDefinition;
import at.jku.isse.passiveprocessengine.definition.serialization.DTOs.Constraint;
import at.jku.isse.passiveprocessengine.definition.serialization.DTOs.Process;
import at.jku.isse.passiveprocessengine.instance.StepLifecycle.Conditions;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DefinitionTransformer {

	public static ProcessDefinition fromDTO(DTOs.Process procDTO, Workspace ws, boolean isInStaging, List<ProcessDefinitionError> errors, ProcessConfigBaseElementFactory configFactory)  {
		ProcessDefinition procDef = ProcessDefinition.getInstance(procDTO.getCode(), ws);
		initProcessFromDTO(procDTO, procDef, ws, 0, isInStaging, errors, configFactory);
		return procDef;
	}
	
	private static void initProcessFromDTO(DTOs.Process procDTO, ProcessDefinition pDef, Workspace ws, int depth, boolean isInStaging, List<ProcessDefinitionError> errors, ProcessConfigBaseElementFactory configFactory) {
		if (!isInStaging) {
			cleanStagingRewriting(procDTO);
		}			
		createOrUpdateConfig(procDTO, pDef, configFactory);
		
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
				sDef = createSubprocess((DTOs.Process)sd, ws, procDTO, isInStaging, errors, configFactory);
				sDef.setProcess(pDef);
				//FIXME: child process instance type will not point to this type of parent process instance type, for accessing any configuration
				pDef.addStepDefinition(sDef);				
			} else {
				sDef = pDef.createStepDefinition(sd.getCode() ,ws);
				initStepFromDTO(sd, sDef, ws, errors);				
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
		initStepFromDTO(procDTO, pDef, ws, errors);
		
		pDef.setDepthIndexRecursive(depth);
		pDef.setElementOrder();
	}
	
	private static void createOrUpdateConfig(Process procDTO, ProcessDefinition pDef, ProcessConfigBaseElementFactory configFactory) {
		// first create process config schema if it does not exists		
		procDTO.getConfigs().entrySet().stream().forEach(entry -> {
			String configName = entry.getKey();
			InstanceType procConfig = configFactory.getOrCreateProcessSpecificSubtype(configName, pDef);
			// then add the properties if they dont exist yet
			configFactory.augmentConfig(entry.getValue(), procConfig);
			// then add as input to process DTO if it doesnt yet exist, of if so, overrides with most concrete subtype						
			procDTO.getInput().put(configName, procConfig.name());
			//TODO: how this dynamic input setting works with configurations in subprocesses!? (mapping, staging, preset config names, etc.)
		});		
	}

	private static ProcessDefinition createSubprocess(DTOs.Process subProcess, Workspace ws, DTOs.Process parentProc, boolean isInStaging, List<ProcessDefinitionError> errors, ProcessConfigBaseElementFactory configFactory) {
		// first rename the subprocess to be unique and
		String parentProcName = parentProc.getCode();
		String oldSubProcName = subProcess.getCode();
		String newSubProcName = subProcess.getCode()+"-"+parentProcName; 
		subProcess.setCode(newSubProcName);		
		// then update mappings
		replaceStepNamesInMappings(subProcess, oldSubProcName, newSubProcName); // in the subprocess
		replaceStepNamesInMappings(parentProc, oldSubProcName, newSubProcName); // but also in the parent process, but WONT undo later 
		
		ProcessDefinition pDef = fromDTO((Process) subProcess, ws, isInStaging, errors, configFactory);
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
	
	private static void initStepFromDTO(DTOs.Step step, StepDefinition pStep, Workspace ws, List<ProcessDefinitionError> errors) {	
		step.getInput().entrySet().stream().forEach(entry -> pStep.addExpectedInput(entry.getKey(), resolveInstanceType(entry.getValue(), ws, errors, pStep, entry.getKey())));
		step.getOutput().entrySet().stream().forEach(entry -> pStep.addExpectedOutput(entry.getKey(), resolveInstanceType(entry.getValue(), ws, errors, pStep, entry.getKey())));
		//step.getConditions().entrySet().stream().forEach(entry -> pStep.setCondition(entry.getKey(), entry.getValue()));		
		step.getConditions().entrySet().stream().forEach(entry -> {
			entry.getValue().stream().forEach(constraint -> {
				ConstraintSpec spec = ConstraintSpec.createInstance(entry.getKey(), constraint.getCode(), constraint.getArlRule(), constraint.getDescription(), constraint.getSpecOrderIndex(), constraint.isOverridable(), ws);
				if (pStep instanceof ProcessDefinition) {
					spec.setProcess((ProcessDefinition)pStep);
				} else if (pStep.getProcess() != null) {
					spec.setProcess(pStep.getProcess());
				}
				switch(entry.getKey()) {
				case ACTIVATION:
					pStep.addActivationcondition(spec);
					break;
				case CANCELATION:
					pStep.addCancelcondition(spec);
					break;
				case POSTCONDITION:
					pStep.addPostcondition(spec);
					break;
				case PRECONDITION:
					pStep.addPrecondition(spec);
					break;
				default:
					log.warn("Unsupported constraint type: "+entry.getKey());
					break;
				}				
			});
		});		
		step.getIoMapping().entrySet().stream().forEach(entry -> pStep.addInputToOutputMappingRule(entry.getKey(),  trimLegacyIOMappingRule(entry.getValue())));
		step.getQaConstraints().stream().forEach(constraint -> { 
			ConstraintSpec spec = ConstraintSpec.createInstance(Conditions.QA, constraint.getCode(), constraint.getArlRule(), constraint.getDescription(), constraint.getSpecOrderIndex(), constraint.isOverridable(), ws);
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
	
	private static InstanceType resolveInstanceType(String type, Workspace ws, List<ProcessDefinitionError> errors, ProcessDefinitionScopedElement el, String param) {
		// search in types folder and below for type			
		// InstanceType iType = // this returns also deleted types ws.debugInstanceTypeFindByName(type);
		InstanceType iType = searchInFolderAndBelow(type, ws.TYPES_FOLDER);
		if (iType == null) {
			errors.add(new ProcessDefinitionError(el, "Unknown Instance Type", "Input/Output definition "+param+" uses unknown instance type: "+type ));
			//throw new ProcessException("Process Description uses unknown instance type: "+type);
		}
		return iType;
	}
	
	private static InstanceType searchInFolderAndBelow(String type, Folder toSearch) {
		return toSearch.instanceTypes().stream()
			.filter(iType -> iType.name().equals(type))
			.filter(iType -> !iType.isDeleted())
			.findAny().orElseGet(() -> {
				return toSearch.subfolders().stream()
						.map(folder -> searchInFolderAndBelow(type, folder))
						.filter(Objects::nonNull)
						.findAny().orElse(null);
			});
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
			DTOs.Constraint qa = new DTOs.Constraint(qac.getConstraintSpec());			
			qa.setCode(qac.getConstraintId());
			qa.setDescription(qac.getHumanReadableDescription());
			qa.setSpecOrderIndex(qac.getOrderIndex());
			qa.setOverridable(qac.isOverridable());
			step.getQaConstraints().add(qa ); 
			
		});
		pStep.getInputToOutputMappingRules().entrySet().stream().forEach(entry -> step.getIoMapping().put(entry.getKey(), entry.getValue()));
		//old conditions mapping
//		for (Conditions cond : Conditions.values()) {
//			pStep.getCondition(cond).ifPresent(condARL -> step.getConditions().put(cond, condARL));
//		}
		pStep.getActivationconditions().stream().forEach(spec -> {
			Constraint constraint = new Constraint(spec.getConstraintSpec());
			constraint.setCode(spec.getName());
			constraint.setDescription(spec.getHumanReadableDescription());
			constraint.setSpecOrderIndex(spec.getOrderIndex());
			constraint.setOverridable(spec.isOverridable());
			step.getConditions().computeIfAbsent(Conditions.ACTIVATION, k -> new ArrayList<Constraint>()).add(constraint);
		});
		pStep.getCancelconditions().stream().forEach(spec -> {
			Constraint constraint = new Constraint(spec.getConstraintSpec());
			constraint.setCode(spec.getName());
			constraint.setDescription(spec.getHumanReadableDescription());
			constraint.setSpecOrderIndex(spec.getOrderIndex());
			constraint.setOverridable(spec.isOverridable());
			step.getConditions().computeIfAbsent(Conditions.CANCELATION, k -> new ArrayList<Constraint>()).add(constraint);
		});
		pStep.getPreconditions().stream().forEach(spec -> {
			Constraint constraint = new Constraint(spec.getConstraintSpec());
			constraint.setCode(spec.getName());
			constraint.setDescription(spec.getHumanReadableDescription());
			constraint.setSpecOrderIndex(spec.getOrderIndex());
			constraint.setOverridable(spec.isOverridable());
			step.getConditions().computeIfAbsent(Conditions.PRECONDITION, k -> new ArrayList<Constraint>()).add(constraint);
		});
		pStep.getPostconditions().stream().forEach(spec -> {
			Constraint constraint = new Constraint(spec.getConstraintSpec());
			constraint.setCode(spec.getName());
			constraint.setDescription(spec.getHumanReadableDescription());
			constraint.setSpecOrderIndex(spec.getOrderIndex());
			constraint.setOverridable(spec.isOverridable());
			step.getConditions().computeIfAbsent(Conditions.POSTCONDITION, k -> new ArrayList<Constraint>()).add(constraint);
		});		

		
		step.setHtml_url(pStep.getHtml_url());
		step.setDescription(pStep.getDescription());
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
