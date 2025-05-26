package at.jku.isse.passiveprocessengine.instance.factories;

import java.time.ZonedDateTime;
import java.util.Map;
import java.util.UUID;

import at.jku.isse.passiveprocessengine.core.BaseNamespace;
import at.jku.isse.passiveprocessengine.core.FactoryIndex.DomainFactory;
import at.jku.isse.passiveprocessengine.rdfwrapper.RDFInstance;
import at.jku.isse.passiveprocessengine.rdfwrapper.rule.RuleEnabledResolver;
import lombok.NonNull;
import at.jku.isse.passiveprocessengine.definition.activeobjects.ConstraintSpec;
import at.jku.isse.passiveprocessengine.definition.activeobjects.ProcessDefinition;
import at.jku.isse.passiveprocessengine.definition.activeobjects.StepDefinition;
import at.jku.isse.passiveprocessengine.definition.factories.SpecificProcessInstanceTypesFactory;
import at.jku.isse.passiveprocessengine.instance.StepLifecycle.Conditions;
import at.jku.isse.passiveprocessengine.instance.activeobjects.ConstraintResultWrapper;
import at.jku.isse.passiveprocessengine.instance.activeobjects.DecisionNodeInstance;
import at.jku.isse.passiveprocessengine.instance.activeobjects.ProcessInstance;
import at.jku.isse.passiveprocessengine.instance.activeobjects.ProcessStep;
import at.jku.isse.passiveprocessengine.instance.types.AbstractProcessInstanceType;
import at.jku.isse.passiveprocessengine.instance.types.AbstractProcessStepType;
import at.jku.isse.passiveprocessengine.instance.types.ConstraintResultWrapperTypeFactory;
import at.jku.isse.passiveprocessengine.instance.types.DecisionNodeInstanceTypeFactory;
import at.jku.isse.passiveprocessengine.instance.types.SpecificProcessInstanceType;
import at.jku.isse.passiveprocessengine.instance.types.SpecificProcessStepType;

public class ProcessInstanceFactory extends DomainFactory {
		
	public static final String NS = BaseNamespace.NS+"/instance";
	
	private final DecisionNodeInstanceTypeFactory dniFactory;
	final ConstraintResultWrapperTypeFactory crwFactory;
	
	public ProcessInstanceFactory(RuleEnabledResolver context, DecisionNodeInstanceTypeFactory dniFactory, ConstraintResultWrapperTypeFactory crwFactory) {		
		super(context);		
		this.dniFactory = dniFactory;
		this.crwFactory = crwFactory;
	}
	

	
	public ProcessInstance getInstance(ProcessDefinition processDef, String namePostfix) {
		//TODO: not to create duplicate process instances somehow	
		RDFInstance instance = getContext().createInstance(generateProcessId(processDef, namePostfix)
				, getContext().findNonDeletedInstanceTypeByFQN(processDef.getId()).get());
		ProcessInstance process = (ProcessInstance) instance;
		process.inject(this, dniFactory);
		init(process, processDef, null, null);
		return process;
	}

	public ProcessInstance getSubprocessInstance(ProcessDefinition subprocessDef, DecisionNodeInstance inDNI, DecisionNodeInstance outDNI, ProcessInstance scope) {
		RDFInstance instance = getContext().createInstance(generateProcessId(subprocessDef, UUID.randomUUID().toString())
			, getContext().findNonDeletedInstanceTypeByFQN(subprocessDef.getId()).get());
		ProcessInstance process = (ProcessInstance) instance;
		process.setProcess(scope);
		process.inject(this, dniFactory);
		init(process, subprocessDef, inDNI, outDNI);
		return process;
	}
	
	private void init(ProcessInstance process, ProcessDefinition pdef, DecisionNodeInstance inDNI, DecisionNodeInstance outDNI) {
		
		// init first DNI, there should be only one. Needs to be checked earlier with definition creation
		// we assume consistent, correct specification/definition here
		process.setSingleProperty(AbstractProcessInstanceType.CoreProperties.processDefinition.toString()
				, pdef.getInstance());
		this.initProcessStep(process, pdef, inDNI, outDNI);		
		
		// instantiate all steps and thereby the DNIs
		pdef.getStepDefinitions().stream().forEach(sd -> {
			ProcessStep step = process.createAndWireTask(sd);
			//step.getInDNI().tryDataPropagationToPrematurelyTriggeredTask(); no point in triggering as there is no input available at this stage
		});
		 // now also activate first
		pdef.getDecisionNodeDefinitions().stream()
			.filter(dnd -> dnd.getInSteps().isEmpty())
			.forEach(dnd -> {
				DecisionNodeInstance dni = process.getOrCreateDNI(dnd);
				dni.signalStateChanged(process); //dni.tryActivationPropagation(); // to trigger instantiation of initial steps
			});
		// datamapping from proc to DNI is triggered upon adding input, which is not available at this stage
	}
	
	public ProcessStep getStepInstance(@NonNull StepDefinition stepDef, @NonNull DecisionNodeInstance inDNI, @NonNull DecisionNodeInstance outDNI, @NonNull ProcessInstance scope) {
		if (stepDef instanceof ProcessDefinition subprocDef) { // we have a subprocess
			// we delegate to ProcessInstance
			return getSubprocessInstance(subprocDef, inDNI, outDNI, scope);
		} else {
			String specificStepName = generateStepId(stepDef, scope); 
			RDFInstance instance = getContext().createInstance( specificStepName
				, getContext().findNonDeletedInstanceTypeByFQN(stepDef.getId()).get());
			ProcessStep step = (ProcessStep) instance;
			step.setProcess(scope);
			initProcessStep(step, stepDef, inDNI, outDNI);
			return step;
		}
	}
	
	
	@SuppressWarnings("unchecked")
	protected void initProcessStep(ProcessStep step, StepDefinition sd, DecisionNodeInstance inDNI, DecisionNodeInstance outDNI) {
		RDFInstance instance = step;
		if (step.getName().startsWith(StepDefinition.NOOPSTEP_PREFIX)) { // assumes/expects no pre/post cond and no qa
			instance.setSingleProperty(AbstractProcessStepType.CoreProperties.processedPreCondFulfilled.toString(),true);
			instance.setSingleProperty(AbstractProcessStepType.CoreProperties.processedPostCondFulfilled.toString(),true);
			instance.setSingleProperty(AbstractProcessStepType.CoreProperties.isWorkExpected.toString(),false);
		} else {
			instance.setSingleProperty(AbstractProcessStepType.CoreProperties.processedPreCondFulfilled.toString(),false);
			instance.setSingleProperty(AbstractProcessStepType.CoreProperties.processedPostCondFulfilled.toString(),false);
			instance.setSingleProperty(AbstractProcessStepType.CoreProperties.isWorkExpected.toString(),true);
		}
		instance.setSingleProperty(AbstractProcessStepType.CoreProperties.processedCancelCondFulfilled.toString(),false);
		instance.setSingleProperty(AbstractProcessStepType.CoreProperties.processedActivationCondFulfilled.toString(),false);

		instance.setSingleProperty(AbstractProcessStepType.CoreProperties.stepDefinition.toString(),sd.getInstance());
		if (inDNI != null) {
			instance.setSingleProperty(AbstractProcessStepType.CoreProperties.inDNI.toString(),inDNI.getInstance());
			inDNI.addOutStep(step);
		}
		if (outDNI != null) {
			instance.setSingleProperty(AbstractProcessStepType.CoreProperties.outDNI.toString(),outDNI.getInstance());
			outDNI.addInStep(step);
		}
		// only if no input and no preconditions --> automatically go into enabled, (if there is input, then there needs to be a precondition checking for presence of input)
		// but this implies that only manual output can be set as there is no input to derive output from (as there cannot be any io mapping)
		// --> UPDATE: if there is no precondition then we assume the input is optional,
		if (/*DEL-UPDATE: sd.getExpectedInput().isEmpty() &&*/ sd.getPreconditions().isEmpty()) {
			step.setPreConditionsFulfilled(true);
		}
		ProcessDefinition pd = sd.getProcess() !=null ? sd.getProcess() : (ProcessDefinition)sd;
		sd.getQAConstraints().stream()
		.forEach(spec -> {
			//String qid = SpecificProcessInstanceTypesFactory.getQASpecId(spec, pd);
			ConstraintResultWrapper cw = crwFactory.createInstance(spec, ZonedDateTime.now(), step, getParentProcessOrThisIfProcessElseNull(step));			
			instance.getTypedProperty(AbstractProcessStepType.CoreProperties.qaState.toString(), Map.class).put(spec.getId(), cw.getInstance());
		});
		// init of multi constraint wrappers:
		sd.getPostconditions().stream()
		.sorted(ConstraintSpec.COMPARATOR_BY_ORDERINDEX)
		.forEach(spec -> {
			//String specId = SpecificProcessInstanceTypesFactory.getConstraintName(Conditions.POSTCONDITION, spec.getOrderIndex(), step.getInstanceType());
			ConstraintResultWrapper cw = crwFactory.createInstance(spec, ZonedDateTime.now(), step, getParentProcessOrThisIfProcessElseNull(step));
			instance.getTypedProperty(AbstractProcessStepType.CoreProperties.postconditions.toString(), Map.class).put(spec.getId(), cw.getInstance());
		});
		sd.getPreconditions().stream()
		.sorted(ConstraintSpec.COMPARATOR_BY_ORDERINDEX)
		.forEach(spec -> {
			//String specId = SpecificProcessInstanceTypesFactory.getConstraintName(Conditions.PRECONDITION, spec.getOrderIndex(), step.getInstanceType());
			ConstraintResultWrapper cw = crwFactory.createInstance(spec, ZonedDateTime.now(), step, getParentProcessOrThisIfProcessElseNull(step));
			instance.getTypedProperty(AbstractProcessStepType.CoreProperties.preconditions.toString(), Map.class).put(spec.getId(), cw.getInstance());
		});
		sd.getCancelconditions().stream()
		.sorted(ConstraintSpec.COMPARATOR_BY_ORDERINDEX)
		.forEach(spec -> {
			//String specId = SpecificProcessInstanceTypesFactory.getConstraintName(Conditions.CANCELATION, spec.getOrderIndex(), step.getInstanceType());
			ConstraintResultWrapper cw = crwFactory.createInstance(spec, ZonedDateTime.now(), step, getParentProcessOrThisIfProcessElseNull(step));
			instance.getTypedProperty(AbstractProcessStepType.CoreProperties.cancelconditions.toString(), Map.class).put(spec.getId(), cw.getInstance());
		});
		sd.getActivationconditions().stream()
		.sorted(ConstraintSpec.COMPARATOR_BY_ORDERINDEX)
		.forEach(spec -> {
			//String specId = SpecificProcessInstanceTypesFactory.getConstraintName(Conditions.ACTIVATION, spec.getOrderIndex(), step.getInstanceType());
			ConstraintResultWrapper cw = crwFactory.createInstance(spec, ZonedDateTime.now(), step, getParentProcessOrThisIfProcessElseNull(step));
			instance.getTypedProperty(AbstractProcessStepType.CoreProperties.activationconditions.toString(), Map.class).put(spec.getId(), cw.getInstance());
		});
	}
	
	private ProcessInstance getParentProcessOrThisIfProcessElseNull(ProcessStep step) {
		return step.getProcess() != null ? step.getProcess() : (ProcessInstance)step; //ugly hack if this is a process without parent
	}

	public static String generateProcessId(ProcessDefinition processDef, String namePostfix) {		
		return NS+generateProcessNameHierarchy(processDef)+"#"+namePostfix;
	}
	
	private static String generateProcessNameHierarchy(ProcessDefinition procDef) {
		if (procDef.getProcess() == null) {
			return "/"+procDef.getName();
		} else {
			return generateProcessNameHierarchy(procDef.getProcess())+"/"+procDef.getName();
		}
	}
	
	private static String generateStepId(StepDefinition processStep, ProcessInstance procInst) {
		String ns = procInst.getInstance().getNameSpace();
		String procName = procInst.getInstance().getLocalName();
		return ns.substring(0, ns.length()-1) + "/" + procName + "/step#" + processStep.getName();
	}
}
