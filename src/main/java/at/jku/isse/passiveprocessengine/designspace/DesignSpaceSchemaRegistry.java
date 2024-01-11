package at.jku.isse.passiveprocessengine.designspace;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import at.jku.isse.designspace.core.model.Id;
import at.jku.isse.designspace.core.model.Workspace;
import at.jku.isse.designspace.rule.model.ConsistencyRuleType;
import at.jku.isse.passiveprocessengine.core.Instance;
import at.jku.isse.passiveprocessengine.core.InstanceRepository;
import at.jku.isse.passiveprocessengine.core.InstanceType;
import at.jku.isse.passiveprocessengine.core.RuleDefinition;
import at.jku.isse.passiveprocessengine.core.SchemaRegistry;

public class DesignSpaceSchemaRegistry implements SchemaRegistry, InstanceRepository {

	private Workspace ws;
	private HashMap<Id, Instance> instanceWrappers = new HashMap<>();
	private HashMap<Id, InstanceType> instanceTypeWrappers = new HashMap<>();
	
	public DesignSpaceSchemaRegistry(Workspace ws) {
		this.ws = ws;
	}
	
	@Override
	public Optional<InstanceType> findNonDeletedInstanceTypeById(String fqnTypeId) {
		
		// TODO Auto-generated method stub
		return Optional.empty();
	}

	@Override
	public Set<InstanceType> findAllInstanceTypesById(String fqnTypeId) {
		// TODO Auto-generated method stub
		return Collections.emptySet();
	}

	@Override
	public InstanceType createNewInstanceType(String fqnTypeId, InstanceType... superTypes) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public RuleDefinition getRuleByNameAndContext(String ruleName, InstanceType context) {
		Collection<InstanceType> ruleDefinitions = ws.its(ConsistencyRuleType.CONSISTENCY_RULE_TYPE).subTypes();
        if(ruleDefinitions.isEmpty() || (ruleDefinitions.stream().filter(inst -> !inst.isDeleted).count() == 0))
            return null;
        for(ConsistencyRuleType crd: ruleDefinitions.stream()
        								.filter(inst -> !inst.isDeleted)
        								.filter(ConsistencyRuleType.class::isInstance)
        								.map(ConsistencyRuleType.class::cast)
        								.collect(Collectors.toSet()) ){
            if (crd.name().equalsIgnoreCase(name) && crd.contextInstanceType().equals(context) )
                return crd;
        }
        return null;
	}
	}

	@Override
	public Set<InstanceType> getAllNonDeletedInstanceTypes() {
		return ws.debugInstanceTypes().stream()
			.filter(type -> !type.isDeleted)
			.map(type -> getWrappedType(type))
			.collect(Collectors.toSet());
	}
	
	public InstanceType getWrappedType(at.jku.isse.designspace.core.model.InstanceType type) {
		return instanceTypeWrappers.computeIfAbsent(type.id(), k -> new DesignspaceInstanceTypeWrapper(type, this));
	}

	public Instance getWrappedInstance(at.jku.isse.designspace.core.model.Instance instance) {
		return instanceWrappers.computeIfAbsent(instance.id(), k -> new DesignspaceInstanceWrapper(instance, this));
	}
	
	@Override
	public Instance createInstance(String id, InstanceType type) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void concludeTransaction() {
		ws.concludeTransaction();
	}

	@Override
	public Optional<Instance> findInstanceyById(String id) {
		at.jku.isse.designspace.core.model.Instance inst = ws.debugInstanceFindByName(id);
		if (inst == null) {
			return Optional.of(getWrappedInstance(inst));
		} else
			return Optional.empty();
	}

	@Override
	public Set<Instance> getAllInstancesOfTypeOrSubtype(InstanceType type) {
		// TODO Auto-generated method stub
		return null;
	}

}
