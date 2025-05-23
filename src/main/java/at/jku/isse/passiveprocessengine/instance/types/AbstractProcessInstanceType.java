package at.jku.isse.passiveprocessengine.instance.types;

import java.util.Optional;

import at.jku.isse.passiveprocessengine.core.AbstractTypeProvider;
import at.jku.isse.passiveprocessengine.definition.types.ProcessDefinitionTypeFactory;
import at.jku.isse.passiveprocessengine.instance.activeobjects.ProcessInstance;
import at.jku.isse.passiveprocessengine.rdfwrapper.RDFInstanceType;
import at.jku.isse.passiveprocessengine.rdfwrapper.rule.RuleEnabledResolver;

public class AbstractProcessInstanceType extends AbstractTypeProvider {

	private static final String NS = ProcessInstanceScopeTypeFactory.NS+"/abstractprocess";
	
	public enum CoreProperties {stepInstances, decisionNodeInstances, processDefinition, createdAt;
		
		@Override
		public String toString() {
			return NS+name();
		}
		
		public String getURI() {
			return NS+name();
		}
	}
		
	public static final String typeId = NS+"#"+ProcessInstance.class.getSimpleName();

	public AbstractProcessInstanceType(RuleEnabledResolver schemaRegistry) {
		super(schemaRegistry);
		Optional<RDFInstanceType> thisType = schemaRegistry.findNonDeletedInstanceTypeByFQN(typeId);
		if (thisType.isPresent()) {
			this.type = thisType.get();
		} else {
			this.type = schemaRegistry.createNewInstanceType(typeId, schemaRegistry.findNonDeletedInstanceTypeByFQN(AbstractProcessStepType.typeId).orElseThrow());
		}
	}

	public void produceTypeProperties() {
		// none to create, we just need to have a base process instance type
		type.cacheSuperProperties();
		type.createSinglePropertyType(CoreProperties.processDefinition.toString(), 
				schemaRegistry.findNonDeletedInstanceTypeByFQN(ProcessDefinitionTypeFactory.typeId)
				.map(vtype->vtype.getAsPropertyType())
				.orElseThrow());
		type.createSetPropertyType(CoreProperties.stepInstances.toString(), 
				schemaRegistry.findNonDeletedInstanceTypeByFQN(AbstractProcessStepType.typeId)
				.map(vtype->vtype.getAsPropertyType())
				.orElseThrow());
		type.createSetPropertyType(CoreProperties.decisionNodeInstances.toString(), 
				schemaRegistry.findNonDeletedInstanceTypeByFQN(DecisionNodeInstanceTypeFactory.typeId)
				.map(vtype->vtype.getAsPropertyType())
				.orElseThrow());
		type.createSinglePropertyType(CoreProperties.createdAt.toString(), primitives.getStringType());	
	}
}
