package at.jku.isse.passiveprocessengine.definition.types;

import java.util.Optional;

import at.jku.isse.passiveprocessengine.core.AbstractTypeProvider;
import at.jku.isse.passiveprocessengine.definition.activeobjects.ProcessDefinition;
import at.jku.isse.passiveprocessengine.rdfwrapper.NodeToDomainResolver;
import at.jku.isse.passiveprocessengine.rdfwrapper.RDFInstanceType;
import at.jku.isse.passiveprocessengine.rdfwrapper.rule.RuleEnabledResolver;

public class ProcessDefinitionTypeFactory extends AbstractTypeProvider {

	private static final String NS = ProcessDefinitionScopeType.NS+"/processdefinition#";	
	
	public enum CoreProperties {decisionNodeDefinitions, stepDefinitions, isWithoutBlockingErrors
	;	
		@Override
		public String toString() {
			return NS+name();
		}
		
		public String getURI() {
			return NS+name();
		}
	}
	
	public static final String typeId = ProcessDefinitionScopeType.NS+"#"+ProcessDefinitionTypeFactory.class.getSimpleName();
	
	public ProcessDefinitionTypeFactory(RuleEnabledResolver schemaRegistry) {
		super(schemaRegistry);
		Optional<RDFInstanceType> thisType = schemaRegistry.findNonDeletedInstanceTypeByFQN(typeId);
		if (thisType.isPresent()) {
			this.type = thisType.get();
		} else {
			this.type = schemaRegistry.createNewInstanceType(typeId, schemaRegistry.findNonDeletedInstanceTypeByFQN(StepDefinitionTypeFactory.typeId).orElse(null));		
		}
		metaElements.registerInstanceSpecificClass(typeId, ProcessDefinition.class);
	}
	
	public void produceTypeProperties() {
		type.cacheSuperProperties();
		type.createListPropertyType(CoreProperties.stepDefinitions.toString(),
				schemaRegistry.findNonDeletedInstanceTypeByFQN(StepDefinitionTypeFactory.typeId)
				.map(vtype->vtype.getAsPropertyType())
				.orElse(null));
		type.createSetPropertyType(CoreProperties.decisionNodeDefinitions.toString(),  
				schemaRegistry.findNonDeletedInstanceTypeByFQN(DecisionNodeDefinitionTypeFactory.typeId)
				.map(vtype->vtype.getAsPropertyType())
				.orElse(null));
		type.createSinglePropertyType(CoreProperties.isWithoutBlockingErrors.toString(), primitives.getBooleanType());		

	}
	
	/**
	 * 
	 * @param processId
	 * @return Basic, empty process definition structure. Creation of rules and specific process instance types requires calling 'initializeInstanceTypes'
	 */
	public ProcessDefinition createInstance(String processId) {
		ProcessDefinition instance = (ProcessDefinition) schemaRegistry.createInstance(processId, type);
		instance.setSingleProperty(ProcessDefinitionTypeFactory.CoreProperties.isWithoutBlockingErrors.toString(), false);
		return instance;				
	}

}
