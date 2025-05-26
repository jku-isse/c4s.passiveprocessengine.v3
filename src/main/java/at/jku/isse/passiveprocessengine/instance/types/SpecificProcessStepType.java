package at.jku.isse.passiveprocessengine.instance.types;

import java.util.Optional;

import at.jku.isse.passiveprocessengine.rdfwrapper.RDFInstanceType;
import at.jku.isse.passiveprocessengine.rdfwrapper.rule.RuleEnabledResolver;
import at.jku.isse.passiveprocessengine.core.AbstractTypeProvider;
import at.jku.isse.passiveprocessengine.definition.activeobjects.StepDefinition;
import at.jku.isse.passiveprocessengine.instance.activeobjects.ProcessStep;
import lombok.NonNull;

public class SpecificProcessStepType extends AbstractTypeProvider {

	private final StepDefinition stepDef;
	private final RDFInstanceType processType;
	//private final ProcessInstanceScopeTypeFactory scopeFactory;
	
	public static final String PREFIX_OUT = "out_";
	public static final String PREFIX_IN = "in_";

		
	public SpecificProcessStepType(RuleEnabledResolver schemaRegistry, StepDefinition stepDef, @NonNull RDFInstanceType processType) {
		super(schemaRegistry);
		this.stepDef = stepDef;
		this.processType = processType;
	//	this.scopeFactory = scopeFactory;
	}
	
//	public SpecificProcessStepType(RuleEnabledResolver schemaRegistry, StepDefinition stepDef, ProcessInstanceScopeTypeFactory scopeFactory) {
//		super(schemaRegistry);
//		this.stepDef = stepDef;	
//		this.processType = null;
//		this.scopeFactory = scopeFactory;
//	}
	
	public void produceTypeProperties() {
		//String stepName = SpecificProcessStepType.getProcessStepName(processType, stepDef);
		Optional<RDFInstanceType> thisType = schemaRegistry.findNonDeletedInstanceTypeByFQN(stepDef.getId());
		if (thisType.isPresent()) {
			thisType.get().cacheSuperProperties();
			type = thisType.get();	
		} else {
			//RDFInstanceType type = processType == null ? 
			//	schemaRegistry.createNewInstanceType(stepName, schemaRegistry.findNonDeletedInstanceTypeByFQN(AbstractProcessInstanceType.typeId).get()) :			
			type = schemaRegistry.createNewInstanceType(stepDef.getId(), schemaRegistry.findNonDeletedInstanceTypeByFQN(AbstractProcessStepType.typeId).get());			
			
			stepDef.getExpectedInput().entrySet().stream()
			.forEach(entry -> {
				type.createSetPropertyType(PREFIX_IN+entry.getKey(), entry.getValue().getAsPropertyType());
			});
			stepDef.getExpectedOutput().entrySet().stream()
			.forEach(entry -> {
				type.createSetPropertyType(PREFIX_OUT+entry.getKey(), entry.getValue().getAsPropertyType());
				// derived property rule created in RuleAugmentation class! (to keep rule creation and error checking together
			});
			// DONE IN ProcessDefinitionschemaRegistry
			//			stepDef.getInputToOutputMappingRules().entrySet().stream()
//			.forEach(entry -> {
//				if (entry.getValue() != null) {
//					RuleDefinition crt = this. RuleDefinition.create(type, getDataMappingId(entry, stepDef), completeDatamappingRule(entry.getKey(), entry.getValue()));
//					// do we really need to create those properties? or is creating just the rules ok? probably not
//					//type.createPropertyType(CRD_DATAMAPPING_PREFIX+entry.getKey(), Cardinality.SINGLE, crt);
//				}
//			});
			// override process property type to actual process so we can access its config when needed
			//if (processType != null) {
				if (type.getPropertyType(ProcessInstanceScopeTypeFactory.CoreProperties.process.toString()) == null) {
					type.createSinglePropertyType(ProcessInstanceScopeTypeFactory.CoreProperties.process.toString(), processType.getAsPropertyType());
				}
				//else
				//typeStep.getPropertyType(ProcessInstanceScopedElement.CoreProperties.process.toString()).setInstanceType(processType);
			//} else {
			//	scopeFactory.addGenericProcessProperty(type);
			//}
		}
		metaElements.registerInstanceSpecificClass(type.getId(), ProcessStep.class);
	}
	
//	public static String getProcessStepName(StepDefinition sd) {
////		if (sd instanceof ProcessDefinition procDef) {
////			return SpecificProcessInstanceType.getProcessName(procDef);
////		}
//		String procName = sd.getProcess() != null ? sd.getProcess().getName() : "ROOTPROCESS";
//		return AbstractProcessStepType.typeId+"-"+sd.getName()+"-"+procName;
//	}
	

}
