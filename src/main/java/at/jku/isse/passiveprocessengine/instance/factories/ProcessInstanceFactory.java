package at.jku.isse.passiveprocessengine.instance.factories;

import java.util.UUID;

import at.jku.isse.passiveprocessengine.Context;
import at.jku.isse.passiveprocessengine.core.Instance;
import at.jku.isse.passiveprocessengine.core.InstanceRepository;
import at.jku.isse.passiveprocessengine.definition.activeobjects.ProcessDefinition;
import at.jku.isse.passiveprocessengine.definition.types.ProcessDomainTypesRegistry;
import at.jku.isse.passiveprocessengine.instance.activeobjects.DecisionNodeInstance;
import at.jku.isse.passiveprocessengine.instance.activeobjects.ProcessInstance;
import at.jku.isse.passiveprocessengine.instance.activeobjects.ProcessStep;
import at.jku.isse.passiveprocessengine.instance.factories.FactoryIndex.DomainInstanceFactory;
import at.jku.isse.passiveprocessengine.instance.types.SpecificProcessInstanceType;

public class ProcessInstanceFactory extends DomainInstanceFactory {
						
	public ProcessInstanceFactory(InstanceRepository repository, Context context,
			ProcessDomainTypesRegistry typesFactory) {
		super(repository, context, typesFactory);		
	}
	
	public ProcessInstance getInstance(ProcessDefinition processDef, String namePostfix) {
		//TODO: not to create duplicate process instances somehow		
		Instance instance = repository.createInstance(processDef.getName()+"_"+namePostfix, typesFactory.getTypeByName(SpecificProcessInstanceType.getProcessName(processDef)));
		ProcessInstance process = context.getWrappedInstance(ProcessInstance.class, instance);
		process.inject(factoryIndex.getProcessStepFactory(), factoryIndex.getDecisionNodeInstanceFactory());
		init(process, processDef, null, null);
		return process;
	}

	public ProcessInstance getSubprocessInstance(ProcessDefinition subprocessDef, DecisionNodeInstance inDNI, DecisionNodeInstance outDNI, ProcessInstance scope) {
		Instance instance = repository.createInstance(subprocessDef.getName()+"_"+UUID.randomUUID(), typesFactory.getTypeByName(SpecificProcessInstanceType.getProcessName(subprocessDef)));
		ProcessInstance process = context.getWrappedInstance(ProcessInstance.class, instance);
		process.setProcess(scope);
		process.inject(factoryIndex.getProcessStepFactory(), factoryIndex.getDecisionNodeInstanceFactory());
		init(process, subprocessDef, inDNI, outDNI);
		return process;
	}
	
	private void init(ProcessInstance process, ProcessDefinition pdef, DecisionNodeInstance inDNI, DecisionNodeInstance outDNI) {
		
		// init first DNI, there should be only one. Needs to be checked earlier with definition creation
		// we assume consistent, correct specification/definition here
		process.getInstance().setSingleProperty(SpecificProcessInstanceType.CoreProperties.processDefinition.toString(), pdef.getInstance());
		factoryIndex.getProcessStepFactory().init(process, pdef, inDNI, outDNI);		
		
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
