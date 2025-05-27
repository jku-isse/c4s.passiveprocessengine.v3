package at.jku.isse.passiveprocessengine.definition.registry;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import at.jku.isse.passiveprocessengine.core.FactoryIndex;
import at.jku.isse.passiveprocessengine.rdfwrapper.NodeToDomainResolver;
import at.jku.isse.passiveprocessengine.rdfwrapper.RDFInstanceType;
import at.jku.isse.passiveprocessengine.rdfwrapper.rule.RuleEnabledResolver;
import at.jku.isse.passiveprocessengine.definition.ProcessDefinitionError;
import at.jku.isse.passiveprocessengine.definition.activeobjects.ConstraintSpec;
import at.jku.isse.passiveprocessengine.definition.activeobjects.DecisionNodeDefinition;
import at.jku.isse.passiveprocessengine.definition.activeobjects.ProcessDefinition;
import at.jku.isse.passiveprocessengine.definition.activeobjects.ProcessDefinitionScopedElement;
import at.jku.isse.passiveprocessengine.definition.activeobjects.StepDefinition;
import at.jku.isse.passiveprocessengine.definition.factories.SpecificProcessInstanceTypesFactory;
import at.jku.isse.passiveprocessengine.definition.registry.DTOs.Constraint;
import at.jku.isse.passiveprocessengine.definition.registry.DTOs.Process;
import at.jku.isse.passiveprocessengine.instance.StepLifecycle.Conditions;
import at.jku.isse.passiveprocessengine.instance.types.SpecificProcessConfigType;
import at.jku.isse.passiveprocessengine.instance.types.SpecificProcessInstanceType;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DefinitionTransformer {

	public static final String CONFIG_KEY_doGeneratePrematureRules = "doGeneratePrematureRules";
	public static final String CONFIG_KEY_doImmediateInstantiateAllSteps = "doImmediateInstantiateAllSteps";
	
	private final FactoryIndex factories;	
	private final RuleEnabledResolver schemaRegistry;
	private final List<ProcessDefinitionError> errors = new LinkedList<>();
	private final DTOs.Process rootProcDTO;		
	
	public DefinitionTransformer(DTOs.Process procDTO, FactoryIndex factories, RuleEnabledResolver schemaRegistry) {
		this.rootProcDTO = procDTO;	
		this.factories = factories;
		this.schemaRegistry = schemaRegistry;
	}
	
	public List<ProcessDefinitionError> getErrors() {
		return errors;
	}
	
	public ProcessDefinition fromDTO(boolean isInStaging)  {		
		ProcessDefinition processDef = initProcessFromDTO(rootProcDTO, 0, isInStaging);		
		
		if (errors.isEmpty()) { //if there are type errors, we dont even try to create rules
			errors.addAll(factories.getSpecificProcessInstanceFactory().initializeInstanceTypes(processDef));
		} else {
			processDef.setIsWithoutBlockingErrors(false);
		}
		return processDef;
	}

	private ProcessDefinition initProcessFromDTO(DTOs.Process procDTO, int depth, boolean isInStaging) {
		ProcessDefinition processDefinition = factories.getProcessDefinitionFactory().createInstance(SpecificProcessInstanceTypesFactory.getProcessDefinitionURI(procDTO.getCode()));
		if (!isInStaging) {
			cleanStagingRewriting(procDTO);
		}
		createOrUpdateConfig(procDTO, processDefinition);

		// first DNDs
		procDTO.getDns().stream().forEach(dn -> {
			DecisionNodeDefinition dnd = processDefinition.createDecisionNodeDefinition(SpecificProcessInstanceTypesFactory.getDecisionNodeDefinitionURI(processDefinition,dn.getCode()));
			dnd.setInflowType(dn.getInflowType());
			// we need to set the mappings only later, in case there are subprocesses which update the mapping name
//			dn.getMapping().stream().forEach(m ->
//				dnd.addDataMappingDefinition(
//					MappingDefinition.getInstance(m.getFromStep(), m.getFromParam(), m.getToStep(), m.getToParam(), ws)) );
		});
		// then Steps
		procDTO.getSteps().stream().forEach(stepDTO -> {
			StepDefinition stepDefinition = null;
			if (stepDTO instanceof DTOs.Process subprocessDTO) { // a subprocess
				stepDefinition = createSubprocess(subprocessDTO, procDTO, isInStaging);
				stepDefinition.setProcess(processDefinition);
				//FIXME: child process instance type will not point to this type of parent process instance type, for accessing any configuration
				processDefinition.addStepDefinition(stepDefinition);
			} else {
				stepDefinition = processDefinition.createAndAddStepDefinition(SpecificProcessInstanceTypesFactory.getProcessStepTypeURI(processDefinition, stepDTO.getCode()));
				initStepFromDTO(stepDTO, stepDefinition);
			}
			stepDefinition.setInDND(processDefinition.getDecisionNodeDefinitionByName(stepDTO.getInDNDid())); // byName is fine here as we are in the context of a single process
			stepDefinition.setOutDND(processDefinition.getDecisionNodeDefinitionByName(stepDTO.getOutDNDid()));
		});
		//then create the DND mappings
		procDTO.getDns().stream()
			.forEach(dn -> {
				DecisionNodeDefinition dnd = processDefinition.getDecisionNodeDefinitionByName(dn.getCode());
				dn.getMapping().stream().forEach(m ->
					dnd.addDataMappingDefinition(
						factories.getMappingDefinitionFactory().getInstance(m.getFromStep(), m.getFromParam(), m.getToStep(), m.getToParam(), processDefinition.getId())) );
		});
		// then process itself
		initStepFromDTO(procDTO, processDefinition);

		processDefinition.setDepthIndexRecursive(depth);
		processDefinition.setElementOrder();
		return processDefinition;
	}

	private void createOrUpdateConfig(DTOs.Process procDTO, ProcessDefinition processDefinition) {
		// first create process config schema if it does not exists
		procDTO.getConfigs().entrySet().stream().forEach(entry -> {
			String configName = entry.getKey();		
			
			SpecificProcessConfigType configProvider = new SpecificProcessConfigType(schemaRegistry, processDefinition, configName, entry.getValue());
			// then add the properties if they dont exist yet
			configProvider.produceTypeProperties();
			
			RDFInstanceType procConfig = schemaRegistry.findNonDeletedInstanceTypeByFQN(configProvider.getSubtypeName()).get(); 
					//factories.getProcessConfigFactory().getOrCreateProcessSpecificSubtype(configName, processDefinition);
			
			//factories.getProcessConfigFactory().augmentConfig(entry.getValue(), procConfig);
			// then add as input to process DTO if it doesnt yet exist, of if so, overrides with most concrete subtype
			procDTO.getInput().put(configName, procConfig.getName());
			//TODO: how this dynamic input setting works with configurations in subprocesses!? (mapping, staging, preset config names, etc.)
		});
	}

	private ProcessDefinition createSubprocess(DTOs.Process subProcess, DTOs.Process parentProc, boolean isInStaging) {
		// first rename the subprocess to be unique and
		String parentProcName = parentProc.getCode();
		String oldSubProcName = subProcess.getCode();
		String newSubProcName = subProcess.getCode()+"-"+parentProcName;
		subProcess.setCode(newSubProcName);
		// then update mappings
		replaceStepNamesInMappings(subProcess, oldSubProcName, newSubProcName); // in the subprocess
		replaceStepNamesInMappings(parentProc, oldSubProcName, newSubProcName); // but also in the parent process, but WONT undo later

		ProcessDefinition pDef = initProcessFromDTO(subProcess, 0, isInStaging);
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

	
	
	private void initStepFromDTO(DTOs.Step stepDTO, StepDefinition step) {
		stepDTO.getInput().entrySet().stream().forEach(entry -> step.addExpectedInput(entry.getKey(), resolveInstanceType(entry.getValue(),step, entry.getKey())));
		stepDTO.getOutput().entrySet().stream().forEach(entry -> step.addExpectedOutput(entry.getKey(), resolveInstanceType(entry.getValue(), step, entry.getKey())));
		//step.getConditions().entrySet().stream().forEach(entry -> pStep.setCondition(entry.getKey(), entry.getValue()));
		stepDTO.getConditions().entrySet().stream().forEach(entry -> {
			entry.getValue().stream().forEach(constraint -> {
				// constraint code cannot be used as id here, as usually unique only local per DTO.Process
				var specId = SpecificProcessInstanceTypesFactory.getSpecURI(entry.getKey(), ""+constraint.getSpecOrderIndex(), step);
				ConstraintSpec spec = factories.getConstraintFactory().createInstance(entry.getKey(), specId, constraint.getArlRule(), constraint.getDescription(), constraint.getSpecOrderIndex(), constraint.isOverridable());
				if (step instanceof ProcessDefinition procDef) {
					spec.setProcess(procDef);
				} else if (step.getProcess() != null) {
					spec.setProcess(step.getProcess());
				}
				switch(entry.getKey()) {
				case ACTIVATION:
					step.addActivationcondition(spec);
					break;
				case CANCELATION:
					step.addCancelcondition(spec);
					break;
				case POSTCONDITION:
					step.addPostcondition(spec);
					break;
				case PRECONDITION:
					step.addPrecondition(spec);
					break;
				default:
					String msg = "Unsupported constraint type: "+entry.getKey();
					errors.add(new ProcessDefinitionError(step, "UnsupportedProcessDefinitionSchema", msg, ProcessDefinitionError.Severity.ERROR));
					log.warn(msg);
					break;
				}
			});
		});
		stepDTO.getIoMapping().entrySet().stream().forEach(entry -> step.addInputToOutputMappingRule(entry.getKey(),  entry.getValue()));
		stepDTO.getQaConstraints().stream().forEach(constraint -> {
			var specId = SpecificProcessInstanceTypesFactory.getSpecURI(Conditions.QA, ""+constraint.getSpecOrderIndex(), step);
			ConstraintSpec spec = factories.getConstraintFactory().createInstance(Conditions.QA, specId, constraint.getArlRule(), constraint.getDescription(), constraint.getSpecOrderIndex(), constraint.isOverridable());
			if (step instanceof ProcessDefinition procDef) {
				spec.setProcess(procDef);
			} else if (step.getProcess() != null) {
				spec.setProcess(step.getProcess());
			}
			step.addQAConstraint(spec); });
		step.setSpecOrderIndex(stepDTO.getSpecOrderIndex());
		step.setHtml_url(stepDTO.getHtml_url());
		step.setDescription(stepDTO.getDescription());
	}


	
	private RDFInstanceType resolveInstanceType(String type, ProcessDefinitionScopedElement el, String param) {
		Optional<RDFInstanceType> iType = schemaRegistry.findNonDeletedInstanceTypeByFQN(type);
		if (iType.isEmpty()) {
			errors.add(new ProcessDefinitionError(el, "Unknown Instance Type", "Input/Output definition "+param+" uses unknown instance type: "+type , ProcessDefinitionError.Severity.ERROR));
			//throw new ProcessException("Process Description uses unknown instance type: "+type);
			return schemaRegistry.resolveToType(schemaRegistry.getMetaschemata().getMetaElements().getMetaClass());
		}
		return iType.get();
	}

	public static DTOs.Process toDTO(ProcessDefinition processDefinition) {
		DTOs.Process proc = Process.builder().build();
		processDefinition.getStepDefinitions().stream().forEach(pStep -> {
			DTOs.Step step = DTOs.Step.builder().build();
			if (pStep instanceof ProcessDefinition subprocDef) {
				proc.getSteps().add(toDTO(subprocDef));
			} else {
				initDTOfromStep(step, pStep);
				proc.getSteps().add(step);
			}

		});
		processDefinition.getDecisionNodeDefinitions().stream().forEach(dnd -> {
			DTOs.DecisionNode dn = DTOs.DecisionNode.builder()
			.code(dnd.getName())
			.inflowType(dnd.getInFlowType())
			.depthIndex(dnd.getDepthIndex())			
			.build();
			dnd.getMappings().stream().forEach(md -> {
				DTOs.Mapping mapping = new DTOs.Mapping(md.getFromStepType(), md.getFromParameter(), md.getToStepType(), md.getToParameter());
				dn.getMapping().add(mapping);				
				//TODO: description
			});						
			proc.getDns().add(dn);
		});
		initDTOfromStep(proc, processDefinition);
		return proc;
	}

	private static void initDTOfromStep(DTOs.Step step, StepDefinition pStep) {
		step.setCode(pStep.getName());
		step.setHtml_url(pStep.getHtml_url());
		step.setDescription(pStep.getDescription());
		step.setSpecOrderIndex(pStep.getSpecOrderIndex());			
		
		if (pStep.getInDND() != null)
			step.setInDNDid(pStep.getInDND().getName());
		if (pStep.getOutDND() != null)
			step.setOutDNDid(pStep.getOutDND().getName());
		pStep.getExpectedInput().entrySet().stream().forEach(entry -> step.getInput().put(entry.getKey(), entry.getValue().getName()));
		pStep.getExpectedOutput().entrySet().stream().forEach(entry -> step.getOutput().put(entry.getKey(), entry.getValue().getName()));
		pStep.getQAConstraints().stream().forEach(spec -> {
			DTOs.Constraint constraint = DTOs.Constraint.builder().arlRule(spec.getConstraintSpec())
			 .code(spec.getConstraintId())
			 .description(spec.getHumanReadableDescription())
			 .specOrderIndex(spec.getOrderIndex())
			 .isOverridable(spec.isOverridable())			 			
			 .build();
			step.getQaConstraints().add(constraint );

		});
		pStep.getInputToOutputMappingRules().entrySet().stream().forEach(entry -> step.getIoMapping().put(entry.getKey(), entry.getValue()));
		//old conditions mapping
//		for (Conditions cond : Conditions.values()) {
//			pStep.getCondition(cond).ifPresent(condARL -> step.getConditions().put(cond, condARL));
//		}
		pStep.getActivationconditions().stream().forEach(spec -> {
			DTOs.Constraint constraint = DTOs.Constraint.builder().arlRule(spec.getConstraintSpec())
					 .code(spec.getConstraintId())
					 .description(spec.getHumanReadableDescription())
					 .specOrderIndex(spec.getOrderIndex())
					 .isOverridable(spec.isOverridable())
					 .build();
			step.getConditions().computeIfAbsent(Conditions.ACTIVATION, k -> new ArrayList<>()).add(constraint);
		});
		pStep.getCancelconditions().stream().forEach(spec -> {
			DTOs.Constraint constraint = DTOs.Constraint.builder().arlRule(spec.getConstraintSpec())
					 .code(spec.getConstraintId())
					 .description(spec.getHumanReadableDescription())
					 .specOrderIndex(spec.getOrderIndex())
					 .isOverridable(spec.isOverridable())
					 .build();
			step.getConditions().computeIfAbsent(Conditions.CANCELATION, k -> new ArrayList<>()).add(constraint);
		});
		pStep.getPreconditions().stream().forEach(spec -> {
			DTOs.Constraint constraint = DTOs.Constraint.builder().arlRule(spec.getConstraintSpec())
					 .code(spec.getConstraintId())
					 .description(spec.getHumanReadableDescription())
					 .specOrderIndex(spec.getOrderIndex())
					 .isOverridable(spec.isOverridable())
					 .build();
			step.getConditions().computeIfAbsent(Conditions.PRECONDITION, k -> new ArrayList<>()).add(constraint);
		});
		pStep.getPostconditions().stream().forEach(spec -> {
			DTOs.Constraint constraint = DTOs.Constraint.builder().arlRule(spec.getConstraintSpec())
					 .code(spec.getConstraintId())
					 .description(spec.getHumanReadableDescription())
					 .specOrderIndex(spec.getOrderIndex())
					 .isOverridable(spec.isOverridable())
					 .build();
			step.getConditions().computeIfAbsent(Conditions.POSTCONDITION, k -> new ArrayList<>()).add(constraint);
		});
	}

//	private static String trimLegacyIOMappingRule(String ruleString) {
//		int posLegacySymDiff = stripForComparison(ruleString).indexOf("symmetricDifference(self.out");
//		if (posLegacySymDiff > 0) {
//			return ruleString.substring(0, posLegacySymDiff);
//		} else
//			return ruleString;
//	}

	public static String stripForComparison(String arl) {
		return arl
			.replace("->", "")
			.replace(".", "")
			.replaceAll("[\\n\\t ]", "")
			.trim();
	}
}
