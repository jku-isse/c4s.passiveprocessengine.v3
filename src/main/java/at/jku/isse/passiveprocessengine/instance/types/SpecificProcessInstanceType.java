package at.jku.isse.passiveprocessengine.instance.types;

import java.util.Optional;

import at.jku.isse.passiveprocessengine.InstanceWrapper;
import at.jku.isse.passiveprocessengine.core.BuildInType;
import at.jku.isse.passiveprocessengine.core.PPEInstance;
import at.jku.isse.passiveprocessengine.core.PPEInstanceType;
import at.jku.isse.passiveprocessengine.core.SchemaRegistry;
import at.jku.isse.passiveprocessengine.core.TypeProviderBase;
import at.jku.isse.passiveprocessengine.definition.activeobjects.ProcessDefinition;
import at.jku.isse.passiveprocessengine.instance.activeobjects.DecisionNodeInstance;
import at.jku.isse.passiveprocessengine.instance.activeobjects.ProcessInstance;
import at.jku.isse.passiveprocessengine.instance.activeobjects.ProcessStep;

public class SpecificProcessInstanceType extends TypeProviderBase {

	public static enum CoreProperties {stepInstances, decisionNodeInstances, processDefinition, createdAt}

	private final ProcessDefinition procDef;

	public static final String CRD_PREMATURETRIGGER_PREFIX = "crd_prematuretrigger_";
	public static final String typeId = ProcessInstance.class.getSimpleName();
	
		
	public SpecificProcessInstanceType(SchemaRegistry schemaRegistry, ProcessDefinition procDef) {
		super(schemaRegistry);
		this.procDef = procDef;
	}
	
	@Override
	public void produceTypeProperties() {
		String processName = getProcessName(procDef);
		Optional<PPEInstanceType> thisType = schemaRegistry.findNonDeletedInstanceTypeById(processName);
		if (thisType.isPresent()) {
			schemaRegistry.registerTypeByName(thisType.get());	
			this.type = thisType.get();
		} else {
			String processAsTaskName = SpecificProcessStepType.getProcessStepName(procDef);
			type = schemaRegistry.createNewInstanceType(processName, schemaRegistry.getTypeByName(processAsTaskName));
			schemaRegistry.registerTypeByName(type);		

			type.createSinglePropertyType(SpecificProcessInstanceType.CoreProperties.processDefinition.toString(), schemaRegistry.getType(ProcessDefinition.class));
			type.createSetPropertyType(SpecificProcessInstanceType.CoreProperties.stepInstances.toString(), schemaRegistry.getType(ProcessStep.class));
			type.createSetPropertyType(SpecificProcessInstanceType.CoreProperties.decisionNodeInstances.toString(), schemaRegistry.getType(DecisionNodeInstance.class));
			type.createSinglePropertyType(SpecificProcessInstanceType.CoreProperties.createdAt.toString(), BuildInType.STRING);			
		}

	}			
			
	public static Class<? extends InstanceWrapper> getMostSpecializedClass(PPEInstance inst) {
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
