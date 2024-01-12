package at.jku.isse.passiveprocessengine.instance.types;

import java.util.Optional;

import at.jku.isse.passiveprocessengine.InstanceWrapper;
import at.jku.isse.passiveprocessengine.core.BuildInType;
import at.jku.isse.passiveprocessengine.core.Instance;
import at.jku.isse.passiveprocessengine.core.InstanceType;
import at.jku.isse.passiveprocessengine.core.SchemaRegistry;
import at.jku.isse.passiveprocessengine.core.TypeProvider;
import at.jku.isse.passiveprocessengine.definition.activeobjects.ProcessDefinition;
import at.jku.isse.passiveprocessengine.instance.activeobjects.DecisionNodeInstance;
import at.jku.isse.passiveprocessengine.instance.activeobjects.ProcessInstance;
import at.jku.isse.passiveprocessengine.instance.activeobjects.ProcessStep;

public class SpecificProcessInstanceType implements TypeProvider {

	public static enum CoreProperties {stepInstances, decisionNodeInstances, processDefinition, createdAt}

	private SchemaRegistry schemaRegistry;
	
	private final ProcessDefinition procDef;

	public static final String CRD_PREMATURETRIGGER_PREFIX = "crd_prematuretrigger_";

	public static final String typeId = ProcessInstance.class.getSimpleName();
	
		
	public SpecificProcessInstanceType(SchemaRegistry schemaRegistry, ProcessDefinition procDef) {
		this.schemaRegistry = schemaRegistry;
		this.procDef = procDef;
	}
	
	@Override
	public void produceTypeProperties() {
		String processName = getProcessName(procDef);
		Optional<InstanceType> thisType = schemaRegistry.findNonDeletedInstanceTypeById(processName);
		if (thisType.isPresent())
			factory.registerTypeByName(thisType.get());	
		else {
			String processAsTaskName = SpecificProcessStepType.getProcessStepName(procDef);
			InstanceType type = schemaRegistry.createNewInstanceType(processName, factory.getTypeByName(processAsTaskName));
			factory.registerTypeByName(type);		

			type.createSinglePropertyType(SpecificProcessInstanceType.CoreProperties.processDefinition.toString(), factory.getType(ProcessDefinition.class));
			type.createSetPropertyType(SpecificProcessInstanceType.CoreProperties.stepInstances.toString(), factory.getType(ProcessStep.class));
			type.createSetPropertyType(SpecificProcessInstanceType.CoreProperties.decisionNodeInstances.toString(), factory.getType(DecisionNodeInstance.class));
			type.createSinglePropertyType(SpecificProcessInstanceType.CoreProperties.createdAt.toString(), BuildInType.STRING);			
		}

	}			
			
	public static Class<? extends InstanceWrapper> getMostSpecializedClass(Instance inst) {
		// we have the problem, that the WrapperCache will only return a type we ask for (which might be a general type) rather than the most specialized one, hence we need to obtain that type here
		// we assume that this is used only in here within, and thus that inst is only ProcessDefinition or StepDefinition
		if (inst.getInstanceType().getName().startsWith(typeId.toString())) // its a process
			return ProcessInstance.class;
		else
			return ProcessStep.class; // for now only those two types
	}

	public static String getProcessName(ProcessDefinition processDefinition) {
		String parentName =  processDefinition.getProcess() != null ? processDefinition.getProcess().getName() : "ROOT";
		String name = typeId+processDefinition.getName()+parentName;
		return name;
	}

	public static String generatePrematureRuleName(String stepTypeName, ProcessDefinition processDef) {
		return CRD_PREMATURETRIGGER_PREFIX+stepTypeName+"_"+processDef.getName();
	}
}
