package at.jku.isse.passiveprocessengine.instance.types;

import java.util.Optional;

import at.jku.isse.passiveprocessengine.core.AbstractTypeProvider;
import at.jku.isse.passiveprocessengine.rdfwrapper.NodeToDomainResolver;
import at.jku.isse.passiveprocessengine.rdfwrapper.RDFInstance;
import at.jku.isse.passiveprocessengine.rdfwrapper.RDFInstanceType;
import at.jku.isse.passiveprocessengine.rdfwrapper.rule.RuleEnabledResolver;
import at.jku.isse.passiveprocessengine.definition.activeobjects.ProcessDefinition;
import at.jku.isse.passiveprocessengine.definition.types.ProcessDefinitionTypeFactory;
import at.jku.isse.passiveprocessengine.instance.activeobjects.ProcessInstance;
import at.jku.isse.passiveprocessengine.instance.activeobjects.ProcessInstanceScopedElement;
import at.jku.isse.passiveprocessengine.instance.activeobjects.ProcessStep;


public class SpecificProcessInstanceType extends AbstractTypeProvider {

	private final ProcessDefinition procDef;
	private final ProcessInstanceScopeTypeFactory scopeFactory;
//
//	public static final String CRD_PREMATURETRIGGER_PREFIX = "crd_prematuretrigger_";
//	public static final String typeId = ProcessInstance.class.getSimpleName();
	
		
	public SpecificProcessInstanceType(RuleEnabledResolver schemaRegistry, ProcessDefinition procDef, ProcessInstanceScopeTypeFactory scopeFactory) {
		super(schemaRegistry);
		this.procDef = procDef;
		this.scopeFactory = scopeFactory;
	}
	
	public void produceTypeProperties() {
		//String processName = getProcessDefinitionURI(procDef);
		Optional<RDFInstanceType> thisType = schemaRegistry.findNonDeletedInstanceTypeByFQN(procDef.getId()); // process definition id = process instance type id
		if (thisType.isPresent()) {	
			this.type = thisType.get();
			type.cacheSuperProperties();
		} else {
			//String processAsTaskName = SpecificProcessStepType.getProcessStepName(procDef);
			type = schemaRegistry.createNewInstanceType(procDef.getId(), schemaRegistry.findNonDeletedInstanceTypeByFQN(AbstractProcessInstanceType.typeId).get());
			procDef.getExpectedInput().entrySet().stream()
			.forEach(entry -> {
				type.createSetPropertyType(SpecificProcessStepType.PREFIX_IN+entry.getKey(), entry.getValue().getAsPropertyType());
			});
			procDef.getExpectedOutput().entrySet().stream()
			.forEach(entry -> {
				type.createSetPropertyType(SpecificProcessStepType.PREFIX_OUT+entry.getKey(), entry.getValue().getAsPropertyType());
			}); // no derived property here, as the result/output comes from the process' steps
			scopeFactory.addGenericProcessProperty(type);
		}
		metaElements.registerInstanceSpecificClass(type.getId(), ProcessInstance.class);
	}			
			
//	public static Class<? extends ProcessInstanceScopedElement> getMostSpecializedClass(RDFInstance inst) {
//		// we have the problem, that the WrapperCache will only return a type we ask for (which might be a general type) rather than the most specialized one, hence we need to obtain that type here
//		// we assume that this is used only in here within, and thus that inst is only ProcessDefinition or StepDefinition
//		if (inst.getInstanceType().getName().startsWith(typeId)) // its a process
//			return ProcessInstance.class;
//		else
//			return ProcessStep.class; // for now only those two types
//	}

//	public static String getProcessDefinitionURI(ProcessDefinition processDefinition) {
//		String parentName =  processDefinition.getProcess() != null ? processDefinition.getProcess().getName() : "ROOT";
//		String name = typeId+processDefinition.getName()+parentName;
//		return name;
//	}
	


//	public static String generatePrematureRuleName(String stepTypeName, ProcessDefinition processDef) {
//		return CRD_PREMATURETRIGGER_PREFIX+stepTypeName+"_"+processDef.getName();
//	}
}
