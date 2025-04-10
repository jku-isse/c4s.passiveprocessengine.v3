package at.jku.isse.passiveprocessengine.instance.types;

import java.util.Optional;

import at.jku.isse.passiveprocessengine.core.BuildInType;
import at.jku.isse.passiveprocessengine.core.InstanceWrapper;
import at.jku.isse.passiveprocessengine.rdfwrapper.RDFInstance;
import at.jku.isse.passiveprocessengine.rdfwrapper.RDFInstanceType;
import at.jku.isse.passiveprocessengine.core.SchemaRegistry;
import at.jku.isse.passiveprocessengine.core.TypeProviderBase;
import at.jku.isse.passiveprocessengine.definition.activeobjects.ProcessDefinition;
import at.jku.isse.passiveprocessengine.definition.types.ProcessDefinitionType;
import at.jku.isse.passiveprocessengine.instance.activeobjects.ProcessInstance;
import at.jku.isse.passiveprocessengine.instance.activeobjects.ProcessStep;
import at.jku.isse.passiveprocessengine.rdfwrapper.RDFInstanceType;

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
		Optional<RDFInstanceType> thisType = schemaRegistry.findNonDeletedInstanceTypeByFQN(processName);
		if (thisType.isPresent()) {
			//schemaRegistry.registerTypeByName(thisType.get());	
			this.type = thisType.get();
			((RDFInstanceType) type).cacheSuperProperties();
		} else {
			String processAsTaskName = SpecificProcessStepType.getProcessStepName(procDef);
			type = schemaRegistry.createNewInstanceType(processName, schemaRegistry.getTypeByName(processAsTaskName));
			//schemaRegistry.registerTypeByName(type);		
			type.createSinglePropertyType(SpecificProcessInstanceType.CoreProperties.processDefinition.toString(), schemaRegistry.getTypeByName(ProcessDefinitionType.typeId));
			type.createSetPropertyType(SpecificProcessInstanceType.CoreProperties.stepInstances.toString(), schemaRegistry.getTypeByName(AbstractProcessStepType.typeId));
			type.createSetPropertyType(SpecificProcessInstanceType.CoreProperties.decisionNodeInstances.toString(), schemaRegistry.getTypeByName(DecisionNodeInstanceType.typeId));
			type.createSinglePropertyType(SpecificProcessInstanceType.CoreProperties.createdAt.toString(), BuildInType.STRING);			
		}

	}			
			
	public static Class<? extends InstanceWrapper> getMostSpecializedClass(RDFInstance inst) {
		// we have the problem, that the WrapperCache will only return a type we ask for (which might be a general type) rather than the most specialized one, hence we need to obtain that type here
		// we assume that this is used only in here within, and thus that inst is only ProcessDefinition or StepDefinition
		if (inst.getInstanceType().getName().startsWith(typeId)) // its a process
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
