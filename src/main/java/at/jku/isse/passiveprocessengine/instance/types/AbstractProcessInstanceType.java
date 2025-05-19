package at.jku.isse.passiveprocessengine.instance.types;

import java.util.Optional;

import at.jku.isse.passiveprocessengine.core.AbstractTypeProvider;
import at.jku.isse.passiveprocessengine.instance.activeobjects.ProcessInstance;
import at.jku.isse.passiveprocessengine.rdfwrapper.NodeToDomainResolver;
import at.jku.isse.passiveprocessengine.rdfwrapper.RDFInstanceType;
import at.jku.isse.passiveprocessengine.rdfwrapper.rule.RuleEnabledResolver;

public class AbstractProcessInstanceType extends AbstractTypeProvider {

	private static final String NS = ProcessInstanceScopeType.NS+"/abstractprocess";
		
	public static final String typeId = NS+"#"+ProcessInstance.class.getSimpleName();

	public AbstractProcessInstanceType(RuleEnabledResolver schemaRegistry) {
		super(schemaRegistry);
		Optional<RDFInstanceType> thisType = schemaRegistry.findNonDeletedInstanceTypeByFQN(typeId);
		if (thisType.isPresent()) {
			this.type = thisType.get();
		} else {
			this.type = schemaRegistry.createNewInstanceType(typeId, schemaRegistry.findNonDeletedInstanceTypeByFQN(AbstractProcessStepType.typeId).orElse(null));
		}
	}

	public void produceTypeProperties() {
		// none to create, we just need to have a base process instance type
		type.cacheSuperProperties();
	}
}
