package at.jku.isse.passiveprocessengine.instance.factories;

import java.util.UUID;

import at.jku.isse.passiveprocessengine.core.FactoryIndex.DomainFactory;
import at.jku.isse.passiveprocessengine.rdfwrapper.RDFInstance;
import at.jku.isse.passiveprocessengine.core.RuleEnabledResolver;
import at.jku.isse.passiveprocessengine.definition.activeobjects.ProcessDefinition;
import at.jku.isse.passiveprocessengine.instance.activeobjects.DecisionNodeInstance;
import at.jku.isse.passiveprocessengine.instance.activeobjects.ProcessInstance;
import at.jku.isse.passiveprocessengine.instance.activeobjects.ProcessStep;
import at.jku.isse.passiveprocessengine.instance.types.SpecificProcessInstanceType;

public class ProcessInstanceFactory extends DomainFactory {
						
	public ProcessInstanceFactory(RuleEnabledResolver context) {
		super(context);		
	}
	
	public static String generateId(ProcessDefinition processDef, String namePostfix) {
		return processDef.getName()+"-"+namePostfix;
	}
	
	public ProcessInstance getInstance(ProcessDefinition processDef, String namePostfix) {
		//TODO: not to create duplicate process instances somehow	
		RDFInstance instance = getContext().getInstanceRepository().createInstance(generateId(processDef, namePostfix)
				, getContext().getSchemaRegistry().findNonDeletedInstanceTypeByFQN(SpecificProcessInstanceType.getProcessName(processDef)));
		ProcessInstance process = getContext().getWrappedInstance(ProcessInstance.class, instance);
		process.inject(getContext().getFactoryIndex().getProcessStepFactory(), getContext().getFactoryIndex().getDecisionNodeInstanceFactory());
		init(process, processDef, null, null);
		return process;
	}

	public ProcessInstance getSubprocessInstance(ProcessDefinition subprocessDef, DecisionNodeInstance inDNI, DecisionNodeInstance outDNI, ProcessInstance scope) {
		RDFInstance instance = getContext().getInstanceRepository().createInstance(generateId(subprocessDef, UUID.randomUUID().toString())
			, getContext().getSchemaRegistry().findNonDeletedInstanceTypeByFQN(SpecificProcessInstanceType.getProcessName(subprocessDef)));
		ProcessInstance process = getContext().getWrappedInstance(ProcessInstance.class, instance);
		process.setProcess(scope);
		process.inject(getContext().getFactoryIndex().getProcessStepFactory(), getContext().getFactoryIndex().getDecisionNodeInstanceFactory());
		init(process, subprocessDef, inDNI, outDNI);
		return process;
	}
	
	private void init(ProcessInstance process, ProcessDefinition pdef, DecisionNodeInstance inDNI, DecisionNodeInstance outDNI) {
		
		// init first DNI, there should be only one. Needs to be checked earlier with definition creation
		// we assume consistent, correct specification/definition here
		process.getInstance().setSingleProperty(SpecificProcessInstanceType.CoreProperties.processDefinition.toString()
				, pdef.getInstance());
		getContext().getFactoryIndex().getProcessStepFactory().init(process, pdef, inDNI, outDNI);		
		
		if (process.isImmediateInstantiateAllStepsEnabled()) {
			// instantiate all steps and thereby the DNIs
			pdef.getStepDefinitions().stream().forEach(sd -> {
				ProcessStep step = process.createAndWireTask(sd);
				//step.getInDNI().tryDataPropagationToPrematurelyTriggeredTask(); no point in triggering as there is no input available at this stage
			});
		} // now also activate first
		pdef.getDecisionNodeDefinitions().stream()
			.filter(dnd -> dnd.getInSteps().size() == 0)
			.forEach(dnd -> {
				DecisionNodeInstance dni = process.getOrCreateDNI(dnd);
				dni.signalStateChanged(process); //dni.tryActivationPropagation(); // to trigger instantiation of initial steps
			});
		// datamapping from proc to DNI is triggered upon adding input, which is not available at this stage
	}

}
