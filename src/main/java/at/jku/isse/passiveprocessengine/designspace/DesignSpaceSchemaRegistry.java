package at.jku.isse.passiveprocessengine.designspace;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import at.jku.isse.designspace.core.model.Id;
import at.jku.isse.designspace.core.model.Workspace;
import at.jku.isse.designspace.rule.checker.ConsistencyUtils;
import at.jku.isse.designspace.rule.model.ConsistencyRuleType;
import at.jku.isse.designspace.rule.service.RuleService;
import at.jku.isse.passiveprocessengine.InstanceWrapper;
import at.jku.isse.passiveprocessengine.core.BuildInType;
import at.jku.isse.passiveprocessengine.core.Instance;
import at.jku.isse.passiveprocessengine.core.InstanceRepository;
import at.jku.isse.passiveprocessengine.core.InstanceType;
import at.jku.isse.passiveprocessengine.core.RuleDefinition;
import at.jku.isse.passiveprocessengine.core.RuleDefinitionFactory;
import at.jku.isse.passiveprocessengine.core.SchemaRegistry;
import lombok.NonNull;

public class DesignSpaceSchemaRegistry implements SchemaRegistry, InstanceRepository, DesignspaceAbstractionMapper, RuleDefinitionFactory {

	private Workspace ws;
	private HashMap<Id, Instance> instanceWrappers = new HashMap<>();
	private HashMap<Id, InstanceType> instanceTypeWrappers = new HashMap<>();
	private Map<String, InstanceType> types = new HashMap<>();
	
	public DesignSpaceSchemaRegistry(Workspace ws) {
		this.ws = ws;
		initBasicTypes();
	}
	
	@Override
	public InstanceType getType(Class<? extends InstanceWrapper> clazz) {
		return types.get(clazz.getName());
	}
	
	@Override
	public void registerType(Class<? extends InstanceWrapper> clazz, InstanceType type) {
		types.put(clazz.getName(), type);
	}
	
	@Override
	public void registerTypeByName(@NonNull InstanceType type) {
		types.put(type.getName(), type);
	}
	
	@Override
	public InstanceType getTypeByName(String typeName) {
		return types.get(typeName);
	}
	

	
	private void initBasicTypes() {		
		instanceTypeWrappers.put(Workspace.STRING.id(), BuildInType.STRING);				
		instanceTypeWrappers.put(Workspace.BOOLEAN.id(), BuildInType.BOOLEAN);
		instanceTypeWrappers.put(Workspace.INTEGER.id(),BuildInType.INTEGER);
		instanceTypeWrappers.put(Workspace.REAL.id(), BuildInType.FLOAT);
		instanceTypeWrappers.put(Workspace.DATE.id(), BuildInType.DATE);
		instanceTypeWrappers.put(ConsistencyRuleType.CONSISTENCY_RULE_TYPE.id(), BuildInType.RULE);
		instanceTypeWrappers.put(ws.META_INSTANCE_TYPE.id(), BuildInType.METATYPE);
	}
	
	@Override
	public Optional<InstanceType> findNonDeletedInstanceTypeById(String fqnTypeId) {
		//TODO: for now just in types folder
		return Optional.ofNullable( getWrappedType(ws.TYPES_FOLDER.instanceTypeWithName(fqnTypeId)));
	}

	@Override
	public Set<InstanceType> findAllInstanceTypesById(String fqnTypeId) {
		//TODO: parse fqn 
		return ws.TYPES_FOLDER.instanceTypes().stream()
			.filter(dsType -> dsType.name().equals(fqnTypeId))
			.map(dsType -> getWrappedType(dsType))
			.collect(Collectors.toSet());		
	}

	@Override
	public InstanceType createNewInstanceType(String fqnTypeId, InstanceType... superTypes) {
		List<at.jku.isse.designspace.core.model.InstanceType> dsSuperTypes = new ArrayList<>();
		for (InstanceType type : superTypes) {
			dsSuperTypes.add(mapProcessDomainInstanceTypeToDesignspaceInstanceType(type));
		}		  		
		//TODO select specific folder based on fqn		
		return getWrappedType(ws.createInstanceType(fqnTypeId, ws.TYPES_FOLDER, dsSuperTypes.toArray(at.jku.isse.designspace.core.model.InstanceType[]::new) ));
	}

	@Override
	public RuleDefinition getRuleByNameAndContext(String ruleName, InstanceType context) {
		at.jku.isse.designspace.core.model.InstanceType dsContext = mapProcessDomainInstanceTypeToDesignspaceInstanceType(context);
		Collection<at.jku.isse.designspace.core.model.InstanceType> ruleDefinitions = ws.its(ConsistencyRuleType.CONSISTENCY_RULE_TYPE).subTypes();
		if(ruleDefinitions.isEmpty() || (ruleDefinitions.stream().filter(inst -> !inst.isDeleted).count() == 0))
			return null;
		for(ConsistencyRuleType crd: ruleDefinitions.stream()
				.filter(inst -> !inst.isDeleted)
				.filter(ConsistencyRuleType.class::isInstance)
				.map(ConsistencyRuleType.class::cast)
				.collect(Collectors.toSet()) ){
			if (crd.name().equalsIgnoreCase(ruleName) && crd.contextInstanceType().equals(dsContext) ) {
				return (RuleDefinition) instanceTypeWrappers.computeIfAbsent(crd.id(), k -> new RuleDefinitionWrapper(crd, this));
			}
		}
		return null;
	}
	

	@Override
	public Set<InstanceType> getAllNonDeletedInstanceTypes() {
		return ws.debugInstanceTypes().stream()
			.filter(type -> !type.isDeleted)
			.map(type -> getWrappedType(type))
			.collect(Collectors.toSet());
	}
	
	public InstanceType getWrappedType(at.jku.isse.designspace.core.model.InstanceType type) {
		if (type == null) 
			return null;
		else
			return instanceTypeWrappers.computeIfAbsent(type.id(), k -> new DesignspaceInstanceTypeWrapper(type, this));
	}

	public Instance getWrappedInstance(at.jku.isse.designspace.core.model.Instance instance) {
		return instanceWrappers.computeIfAbsent(instance.id(), k -> new DesignspaceInstanceWrapper(instance, this));
	}
	
	@Override
	public Instance createInstance(String id, InstanceType type) {
		at.jku.isse.designspace.core.model.InstanceType dsType = mapProcessDomainInstanceTypeToDesignspaceInstanceType(type);
		return getWrappedInstance(ws.createInstance(dsType, id));		
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
		at.jku.isse.designspace.core.model.InstanceType dsType = mapProcessDomainInstanceTypeToDesignspaceInstanceType(type);
		return dsType.instancesIncludingSubtypes().stream().map(dsInstance -> getWrappedInstance(dsInstance)).collect(Collectors.toSet());		
	}

	
	
	@Override
	public at.jku.isse.designspace.core.model.Instance mapProcessDomainInstanceToDesignspaceInstance(
			Instance processDomainInstance) {
		if (processDomainInstance instanceof DesignspaceInstanceWrapper) {
			return ((DesignspaceInstanceWrapper) processDomainInstance).getDelegate();
		} else {
			throw new RuntimeException("Asked to get Designspace Instance from non Designspace wrapper");
		}		
	}

	@Override

	public at.jku.isse.designspace.core.model.InstanceType mapProcessDomainInstanceTypeToDesignspaceInstanceType(
			@NonNull Instance processDomainInstanceType) {
		if (processDomainInstanceType instanceof DesignspaceInstanceTypeWrapper) {
			return ((DesignspaceInstanceTypeWrapper) processDomainInstanceType).getDelegate();					
		} else if (processDomainInstanceType instanceof BuildInType) { 
			if (processDomainInstanceType.equals(BuildInType.STRING)) return Workspace.STRING;
			if (processDomainInstanceType.equals(BuildInType.BOOLEAN)) return Workspace.BOOLEAN;
			if (processDomainInstanceType.equals(BuildInType.INTEGER)) return Workspace.INTEGER;
			if (processDomainInstanceType.equals(BuildInType.FLOAT)) return Workspace.REAL;
			if (processDomainInstanceType.equals(BuildInType.DATE)) return Workspace.DATE;
			if (processDomainInstanceType.equals(BuildInType.RULE)) return ws.its(ConsistencyRuleType.CONSISTENCY_RULE_TYPE) ;
			if (processDomainInstanceType.equals(BuildInType.METATYPE)) return ws.its(ws.META_INSTANCE_TYPE);
			else {
				throw new RuntimeException("Asked to get Designspace InstanceType from unsupported BuildInType ");
			}
		} else {
			throw new RuntimeException("Asked to get Designspace InstanceType from non Designspace wrapper with name: "+ processDomainInstanceType.getName());
		}
	}

	@Override
	public RuleDefinition createInstance(InstanceType type, String ruleName, String ruleExpression) {
		at.jku.isse.designspace.core.model.InstanceType dsType = mapProcessDomainInstanceTypeToDesignspaceInstanceType(type);
		ConsistencyRuleType crt = ConsistencyRuleType.create(ws, dsType, ruleName, ruleExpression);
		RuleDefinitionWrapper ruleDef = new RuleDefinitionWrapper(crt, this);
		instanceTypeWrappers.put(crt.id(), ruleDef);
		return ruleDef;
	}

	@Override
	public void setPropertyRepairable(InstanceType type, String property, boolean isRepairable) {
		at.jku.isse.designspace.core.model.InstanceType dsType = mapProcessDomainInstanceTypeToDesignspaceInstanceType(type);
		ConsistencyUtils.setPropertyRepairable(dsType, property, isRepairable);
	}

}
