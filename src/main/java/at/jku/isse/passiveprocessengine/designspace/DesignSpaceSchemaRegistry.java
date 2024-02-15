package at.jku.isse.passiveprocessengine.designspace;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import at.jku.isse.designspace.core.foundation.Id;
import at.jku.isse.designspace.core.model.Folder;
import at.jku.isse.designspace.core.model.Instance;
import at.jku.isse.designspace.core.model.InstanceType;
import at.jku.isse.designspace.core.model.LanguageWorkspace;
import at.jku.isse.designspace.core.model.MetaInstanceType;
import at.jku.isse.designspace.core.model.ProjectWorkspace;
import at.jku.isse.designspace.core.model.Workspace;
import at.jku.isse.designspace.rule.checker.ConsistencyUtils;
import at.jku.isse.designspace.rule.model.ConsistencyRule;
import at.jku.isse.designspace.rule.model.ConsistencyRuleType;
import at.jku.isse.passiveprocessengine.InstanceWrapper;
import at.jku.isse.passiveprocessengine.core.BuildInType;
import at.jku.isse.passiveprocessengine.core.PPEInstance;
import at.jku.isse.passiveprocessengine.core.InstanceRepository;
import at.jku.isse.passiveprocessengine.core.PPEInstanceType;
import at.jku.isse.passiveprocessengine.core.RuleDefinition;
import at.jku.isse.passiveprocessengine.core.RuleDefinitionFactory;
import at.jku.isse.passiveprocessengine.core.RuleResult;
import at.jku.isse.passiveprocessengine.core.SchemaRegistry;
import lombok.Getter;
import lombok.NonNull;

public class DesignSpaceSchemaRegistry implements SchemaRegistry, InstanceRepository, DesignspaceAbstractionMapper, RuleDefinitionFactory {

	@Getter
	private final LanguageWorkspace languageWS;
	@Getter
	private final ProjectWorkspace projectWS;
	private HashMap<Long, PPEInstance> instanceWrappers = new HashMap<>();
	private HashMap<Long, PPEInstanceType> instanceTypeWrappers = new HashMap<>();
	private Map<String, PPEInstanceType> types = new HashMap<>();
	private final Folder instancesFolder;
	private final Folder typesFolder;
	
	public DesignSpaceSchemaRegistry(LanguageWorkspace languageWS, ProjectWorkspace projectWS) {
		super();
		this.languageWS = languageWS;
		this.projectWS = projectWS;		
		instancesFolder = Folder.create("ProcessInstancesFolder", projectWS.getFolder());
		typesFolder = Folder.create("ProcessTypesFolder", languageWS.getFolder());
		initBasicTypes();
	}
		
	@Override
	public PPEInstanceType getType(Class<? extends InstanceWrapper> clazz) {
		return types.get(clazz.getName());
	}
	
	@Override
	public void registerType(Class<? extends InstanceWrapper> clazz, PPEInstanceType type) {
		types.put(clazz.getName(), type);
	}
	
	@Override
	public void registerTypeByName(@NonNull PPEInstanceType type) {
		types.put(type.getName(), type);
	}
	
	@Override
	public PPEInstanceType getTypeByName(String typeName) {
		return types.get(typeName);
	}
	

	
	private void initBasicTypes() {		
		instanceTypeWrappers.put(InstanceType.STRING.getId(), BuildInType.STRING);				
		instanceTypeWrappers.put(InstanceType.BOOLEAN.getId(), BuildInType.BOOLEAN);
		instanceTypeWrappers.put(InstanceType.INTEGER.getId(),BuildInType.INTEGER);
		instanceTypeWrappers.put(InstanceType.REAL.getId(), BuildInType.FLOAT);
		instanceTypeWrappers.put(InstanceType.DATE.getId(), BuildInType.DATE);
		instanceTypeWrappers.put(ConsistencyRuleType.META_CONSISTENCY_RULE_TYPE.getId(), BuildInType.RULE);
		instanceTypeWrappers.put(MetaInstanceType.ROOT.getId(), BuildInType.METATYPE);
	}
	
	@Override
	public Optional<PPEInstanceType> findNonDeletedInstanceTypeById(String fqnTypeId) {
		return typesFolder.getInstanceTypes(languageWS).stream()
				.filter(dsType -> dsType.getName().equals(fqnTypeId))
				.filter(dsType -> !dsType.isDeleted())
				.map(dsType -> getWrappedType(dsType))
				.findAny();
	}

	@Override
	public Set<PPEInstanceType> findAllInstanceTypesById(String fqnTypeId) {		
		return typesFolder.getInstanceTypes(languageWS).stream()
			.filter(dsType -> dsType.getName().equals(fqnTypeId))
			.map(dsType -> getWrappedType(dsType))
			.collect(Collectors.toSet());		
	}

	@Override
	public PPEInstanceType createNewInstanceType(String fqnTypeId, PPEInstanceType... superTypes) {
		List<at.jku.isse.designspace.core.model.InstanceType> dsSuperTypes = new ArrayList<>();
		for (PPEInstanceType type : superTypes) {
			dsSuperTypes.add(mapProcessDomainInstanceTypeToDesignspaceInstanceType(type));
		}		  		
		if (dsSuperTypes.isEmpty()) {		
			return getWrappedType(InstanceType.create(languageWS, fqnTypeId, typesFolder));
		} else {
			return getWrappedType(InstanceType.create(languageWS, fqnTypeId, dsSuperTypes.get(0), typesFolder));
		}
	}

	@Override
	public RuleDefinition getRuleByNameAndContext(String ruleName, PPEInstanceType context) {
		at.jku.isse.designspace.core.model.InstanceType dsContext = mapProcessDomainInstanceTypeToDesignspaceInstanceType(context);
		Collection<at.jku.isse.designspace.core.model.InstanceType> ruleDefinitions = ConsistencyRuleType.ROOT_CONSISTENCY_RULE_TYPE.getSubTypes();
		if(ruleDefinitions.isEmpty() || (ruleDefinitions.stream().filter(inst -> !inst.isDeleted()).count() == 0))
			return null;
		for(ConsistencyRuleType crd: ruleDefinitions.stream()
				.filter(inst -> !inst.isDeleted())
				.filter(ConsistencyRuleType.class::isInstance)
				.map(ConsistencyRuleType.class::cast)
				.collect(Collectors.toSet()) ){
			if (crd.getName().equalsIgnoreCase(ruleName) && crd.getContextInstanceType().equals(dsContext) ) {
				return (RuleDefinition) instanceTypeWrappers.computeIfAbsent(crd.getId(), k -> new RuleDefinitionWrapper(crd, this));
			}
		}
		return null;
	}
	

	@Override
	public Set<PPEInstanceType> getAllNonDeletedInstanceTypes() {
		return typesFolder.getInstanceTypes(languageWS).stream()
			.filter(type -> !type.isDeleted())
			.map(type -> getWrappedType(type))
			.collect(Collectors.toSet());
	}
	
	public PPEInstanceType getWrappedType(at.jku.isse.designspace.core.model.InstanceType type) {
		if (type == null) 
			return null;
		else
			return instanceTypeWrappers.computeIfAbsent(type.getId(), k -> new DesignspaceInstanceTypeWrapper(type, this));
	}
	
	public PPEInstance getWrappedInstance(at.jku.isse.designspace.core.model.Element element) {
		if (element == null)
			return null;
		else if (element instanceof at.jku.isse.designspace.core.model.Instance) {
			return  getWrappedInstance((at.jku.isse.designspace.core.model.Instance)element);
		} else 
			return getWrappedType((at.jku.isse.designspace.core.model.InstanceType) element);	
	}

	public PPEInstance getWrappedInstance(at.jku.isse.designspace.core.model.Instance instance) {
		if (instance == null)
			return null;
		else {
			return instanceWrappers.computeIfAbsent(instance.getId(), k -> new DesignspaceInstanceWrapper(instance, this));
		}
	}
		
	
	public RuleResult getWrappedRuleResult(ConsistencyRule instance) {
		if (instance == null)
			return null;
		else {
			PPEInstance wrapper = instanceWrappers.computeIfAbsent(instance.getId(), k -> new DesignspaceRuleResultWrapper(instance, this)); 
			if (!(wrapper instanceof RuleResult)) { // need to rewrapp
				wrapper = new DesignspaceRuleResultWrapper(instance, this);
				instanceWrappers.put(instance.getId(), wrapper);
			}
			return (RuleResult)wrapper;
		}
	
	}
	
	
	@Override
	public PPEInstance createInstance(String id, PPEInstanceType type) {
		at.jku.isse.designspace.core.model.InstanceType dsType = mapProcessDomainInstanceTypeToDesignspaceInstanceType(type);
		return getWrappedInstance(Instance.create(projectWS, dsType, id, instancesFolder));		
	}

	@Override
	public void concludeTransaction() {
		languageWS.commit("");
		projectWS.commit("");
	}

	@Override
	public Optional<PPEInstance> findInstanceyById(String id) {		
		return instancesFolder.getInstances(projectWS).stream()
			.filter(inst -> inst.getName().equals(id))
			.map(inst -> getWrappedInstance(inst))
			.findAny();	
	}

	@Override
	public Set<PPEInstance> getAllInstancesOfTypeOrSubtype(PPEInstanceType type) {
		at.jku.isse.designspace.core.model.InstanceType dsType = mapProcessDomainInstanceTypeToDesignspaceInstanceType(type);
		return dsType.getAllInstantiations(projectWS).stream().map(dsInstance -> getWrappedInstance(dsInstance)).collect(Collectors.toSet());		
	}

	
	
	@Override
	public at.jku.isse.designspace.core.model.Element mapProcessDomainInstanceToDesignspaceInstance(
			PPEInstance processDomainInstance) {
		if (processDomainInstance instanceof DesignspaceInstanceWrapper) {
			return ((DesignspaceInstanceWrapper) processDomainInstance).getDelegate();
		} else 
		if (processDomainInstance instanceof DesignspaceInstanceTypeWrapper) {
			return ((DesignspaceInstanceTypeWrapper) processDomainInstance).getDelegate();
		} 	
		else {
			throw new RuntimeException("Asked to get Designspace Instance from non Designspace wrapper");
		}		
	}

	@Override

	public at.jku.isse.designspace.core.model.InstanceType mapProcessDomainInstanceTypeToDesignspaceInstanceType(
			@NonNull PPEInstance processDomainInstanceType) {
		if (processDomainInstanceType instanceof DesignspaceInstanceTypeWrapper) {
			return ((DesignspaceInstanceTypeWrapper) processDomainInstanceType).getDelegate();					
		} else if (processDomainInstanceType instanceof BuildInType) { 
			if (processDomainInstanceType.equals(BuildInType.STRING)) return InstanceType.STRING;
			if (processDomainInstanceType.equals(BuildInType.BOOLEAN)) return InstanceType.BOOLEAN;
			if (processDomainInstanceType.equals(BuildInType.INTEGER)) return InstanceType.INTEGER;
			if (processDomainInstanceType.equals(BuildInType.FLOAT)) return InstanceType.REAL;
			if (processDomainInstanceType.equals(BuildInType.DATE)) return InstanceType.DATE;
			if (processDomainInstanceType.equals(BuildInType.RULE)) return ConsistencyRuleType.ROOT_CONSISTENCY_RULE_TYPE ;
			if (processDomainInstanceType.equals(BuildInType.METATYPE)) return MetaInstanceType.ROOT;
			else {
				throw new RuntimeException("Asked to get Designspace InstanceType from unsupported BuildInType ");
			}
		} else {
			throw new RuntimeException("Asked to get Designspace InstanceType from non Designspace wrapper with name: "+ processDomainInstanceType.getName());
		}
	}

	@Override
	public RuleDefinition createInstance(PPEInstanceType type, String ruleName, String ruleExpression) {
		at.jku.isse.designspace.core.model.InstanceType dsType = mapProcessDomainInstanceTypeToDesignspaceInstanceType(type);
		ConsistencyRuleType crt = ConsistencyRuleType.create(languageWS, dsType, ruleName, ruleExpression);
		RuleDefinitionWrapper ruleDef = new RuleDefinitionWrapper(crt, this);
		instanceTypeWrappers.put(crt.getId(), ruleDef);
		return ruleDef;
	}

	@Override
	public void setPropertyRepairable(PPEInstanceType type, String property, boolean isRepairable) {
		at.jku.isse.designspace.core.model.InstanceType dsType = mapProcessDomainInstanceTypeToDesignspaceInstanceType(type);
		ConsistencyUtils.setPropertyRepairable(dsType, property, isRepairable);
	}

}
