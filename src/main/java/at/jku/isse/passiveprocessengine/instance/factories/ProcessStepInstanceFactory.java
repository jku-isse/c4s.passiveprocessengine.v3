package at.jku.isse.passiveprocessengine.instance.factories;

import java.time.ZonedDateTime;
import java.util.Map;
import java.util.UUID;

import at.jku.isse.passiveprocessengine.core.FactoryIndex.DomainFactory;
import at.jku.isse.passiveprocessengine.core.ProcessContext;
import at.jku.isse.passiveprocessengine.core.PPEInstance;
import at.jku.isse.passiveprocessengine.definition.activeobjects.ConstraintSpec;
import at.jku.isse.passiveprocessengine.definition.activeobjects.ProcessDefinition;
import at.jku.isse.passiveprocessengine.definition.activeobjects.StepDefinition;
import at.jku.isse.passiveprocessengine.definition.factories.ProcessDefinitionFactory;
import at.jku.isse.passiveprocessengine.instance.StepLifecycle.Conditions;
import at.jku.isse.passiveprocessengine.instance.activeobjects.ConstraintResultWrapper;
import at.jku.isse.passiveprocessengine.instance.activeobjects.DecisionNodeInstance;
import at.jku.isse.passiveprocessengine.instance.activeobjects.ProcessInstance;
import at.jku.isse.passiveprocessengine.instance.activeobjects.ProcessStep;
import at.jku.isse.passiveprocessengine.instance.types.AbstractProcessStepType;
import at.jku.isse.passiveprocessengine.instance.types.SpecificProcessStepType;
import lombok.NonNull;

public class ProcessStepInstanceFactory extends DomainFactory {
			
	
	public ProcessStepInstanceFactory(ProcessContext context) {
		super(context);		
	}


	public ProcessStep getInstance(@NonNull StepDefinition stepDef, @NonNull DecisionNodeInstance inDNI, @NonNull DecisionNodeInstance outDNI, @NonNull ProcessInstance scope) {
		if (stepDef instanceof ProcessDefinition) { // we have a subprocess
			// we delegate to ProcessInstance
			return getContext().getFactoryIndex().getProcessInstanceFactory().getSubprocessInstance((ProcessDefinition) stepDef, inDNI, outDNI, scope);
		} else {
			String specificStepType = SpecificProcessStepType.getProcessStepName(stepDef);
			PPEInstance instance = getContext().getInstanceRepository().createInstance( stepDef.getName()+"-"+UUID.randomUUID()
				, getContext().getSchemaRegistry().getTypeByName(specificStepType));
			ProcessStep step = getContext().getWrappedInstance(ProcessStep.class, instance);
			step.setProcess(scope);
			init(step, stepDef, inDNI, outDNI);
			return step;
		}
	}
	
	
	@SuppressWarnings("unchecked")
	protected void init(ProcessStep step, StepDefinition sd, DecisionNodeInstance inDNI, DecisionNodeInstance outDNI) {
		PPEInstance instance = step.getInstance();
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
			String qid = ProcessDefinitionFactory.getQASpecId(spec, pd);
			ConstraintResultWrapper cw = getContext().getFactoryIndex().getConstraintResultFactory().createInstance(spec, ZonedDateTime.now(), step, getParentProcessOrThisIfProcessElseNull(step));			
			instance.getTypedProperty(AbstractProcessStepType.CoreProperties.qaState.toString(), Map.class).put(qid, cw.getInstance());
		});
		// init of multi constraint wrappers:
		sd.getPostconditions().stream()
		.sorted(ConstraintSpec.COMPARATOR_BY_ORDERINDEX)
		.forEach(spec -> {
			String specId = ProcessDefinitionFactory.getConstraintName(Conditions.POSTCONDITION, spec.getOrderIndex(), step.getInstance().getInstanceType());
			ConstraintResultWrapper cw = getContext().getFactoryIndex().getConstraintResultFactory().createInstance(spec, ZonedDateTime.now(), step, getParentProcessOrThisIfProcessElseNull(step));
			instance.getTypedProperty(AbstractProcessStepType.CoreProperties.postconditions.toString(), Map.class).put(specId, cw.getInstance());
		});
		sd.getPreconditions().stream()
		.sorted(ConstraintSpec.COMPARATOR_BY_ORDERINDEX)
		.forEach(spec -> {
			String specId = ProcessDefinitionFactory.getConstraintName(Conditions.PRECONDITION, spec.getOrderIndex(), step.getInstance().getInstanceType());
			ConstraintResultWrapper cw = getContext().getFactoryIndex().getConstraintResultFactory().createInstance(spec, ZonedDateTime.now(), step, getParentProcessOrThisIfProcessElseNull(step));
			instance.getTypedProperty(AbstractProcessStepType.CoreProperties.preconditions.toString(), Map.class).put(specId, cw.getInstance());
		});
		sd.getCancelconditions().stream()
		.sorted(ConstraintSpec.COMPARATOR_BY_ORDERINDEX)
		.forEach(spec -> {
			String specId = ProcessDefinitionFactory.getConstraintName(Conditions.CANCELATION, spec.getOrderIndex(), step.getInstance().getInstanceType());
			ConstraintResultWrapper cw = getContext().getFactoryIndex().getConstraintResultFactory().createInstance(spec, ZonedDateTime.now(), step, getParentProcessOrThisIfProcessElseNull(step));
			instance.getTypedProperty(AbstractProcessStepType.CoreProperties.cancelconditions.toString(), Map.class).put(specId, cw.getInstance());
		});
		sd.getActivationconditions().stream()
		.sorted(ConstraintSpec.COMPARATOR_BY_ORDERINDEX)
		.forEach(spec -> {
			String specId = ProcessDefinitionFactory.getConstraintName(Conditions.ACTIVATION, spec.getOrderIndex(), step.getInstance().getInstanceType());
			ConstraintResultWrapper cw = getContext().getFactoryIndex().getConstraintResultFactory().createInstance(spec, ZonedDateTime.now(), step, getParentProcessOrThisIfProcessElseNull(step));
			instance.getTypedProperty(AbstractProcessStepType.CoreProperties.activationconditions.toString(), Map.class).put(specId, cw.getInstance());
		});
	}
	
	private ProcessInstance getParentProcessOrThisIfProcessElseNull(ProcessStep step) {
		return step.getProcess() != null ? step.getProcess() : (ProcessInstance)step; //ugly hack if this is a process without parent
	}
}
