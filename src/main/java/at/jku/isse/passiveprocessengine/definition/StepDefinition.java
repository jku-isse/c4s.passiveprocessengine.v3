package at.jku.isse.passiveprocessengine.definition;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import at.jku.isse.designspace.core.model.Cardinality;
import at.jku.isse.designspace.core.model.Instance;
import at.jku.isse.designspace.core.model.InstanceType;
import at.jku.isse.designspace.core.model.MapProperty;
import at.jku.isse.designspace.core.model.SetProperty;
import at.jku.isse.designspace.core.model.Workspace;
import at.jku.isse.designspace.rule.model.ConsistencyRuleType;
import at.jku.isse.passiveprocessengine.ProcessDefinitionScopedElement;
import at.jku.isse.passiveprocessengine.WrapperCache;
import at.jku.isse.passiveprocessengine.instance.ProcessStep;
import at.jku.isse.passiveprocessengine.instance.StepLifecycle.Conditions;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class StepDefinition extends ProcessDefinitionScopedElement implements IStepDefinition {

	public static enum CoreProperties {expectedInput, expectedOutput, ioMappingRules, 
							conditions,
							qaConstraints,
							inDND, outDND, specOrderIndex,html_url,description};
	
	public static final String designspaceTypeId = StepDefinition.class.getSimpleName();
	
	
	public StepDefinition(Instance instance) {
		super(instance);
	}

	@SuppressWarnings("unchecked")
	public Map<String, InstanceType> getExpectedInput() {
		MapProperty<?> inMap = instance.getPropertyAsMap(CoreProperties.expectedInput.toString());
		if (inMap != null && inMap.get() != null) {
			return ( Map<String, InstanceType>) inMap.get();
		} else return Collections.emptyMap();
	}

	@SuppressWarnings("unchecked")
	public void addExpectedInput(String paramName, InstanceType type) {
		assert(paramName != null);
		assert(type != null);
		instance.getPropertyAsMap(CoreProperties.expectedInput.toString()).put(paramName, type);
	}
	
	@SuppressWarnings("unchecked")
	public Map<String, InstanceType> getExpectedOutput() {
		MapProperty<?> outMap = instance.getPropertyAsMap(CoreProperties.expectedOutput.toString());
		if (outMap != null && outMap.get() != null) {
			return ( Map<String, InstanceType>) outMap.get();
		} else return Collections.emptyMap();
	}

	@SuppressWarnings("unchecked")
	public void addExpectedOutput(String paramName, InstanceType type) {
		assert(paramName != null);
		assert(type != null);
		instance.getPropertyAsMap(CoreProperties.expectedOutput.toString()).put(paramName, type);
	}
	
	public Optional<String> getCondition(Conditions condition) {
		String rule = (String)instance.getPropertyAsMap(CoreProperties.conditions.toString()).get(condition.toString());
		return Optional.ofNullable(rule);
	}
	
	@SuppressWarnings("unchecked")
	public void setCondition(Conditions condition, String ruleAsString) {
		instance.getPropertyAsMap(CoreProperties.conditions.toString()).put(condition.toString(), ruleAsString);
	}

	@SuppressWarnings("unchecked")
	public Set<QAConstraintSpec> getQAConstraints() {
		SetProperty<?> qaSet = instance.getPropertyAsSet(CoreProperties.qaConstraints.toString());
		if (qaSet != null && qaSet.get() != null) {
			return (Set<QAConstraintSpec>) qaSet.get().stream()
					.map(inst -> WrapperCache.getWrappedInstance(QAConstraintSpec.class, (Instance) inst))
					.collect(Collectors.toSet());
		} else return Collections.emptySet();
	}
	
	@SuppressWarnings("unchecked")
	public void addQAConstraint(QAConstraintSpec spec) {
		instance.getPropertyAsSet(CoreProperties.qaConstraints.toString()).add(spec.getInstance());
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public Map<String, String> getInputToOutputMappingRules() {
		Map<?,?> rawMap = instance.getPropertyAsMap(CoreProperties.ioMappingRules.toString()).get();
		//rawMap.entrySet().stream()
		//	.filter(entry -> entry.getKey() instanceof String && entry.getValue() instanceof String)
		//	.map(entry -> (Entry<String,String>) entry)
		//	.
		return (Map<String,String>) rawMap;
	}
	
	@SuppressWarnings("unchecked")
	public void addInputToOutputMappingRule(String ruleId, String rule) {
		instance.getPropertyAsMap(CoreProperties.ioMappingRules.toString()).put(ruleId, rule);
	}
	
	public void setOutDND(DecisionNodeDefinition outDND) {
		// we assume for now, there is no need for rewiring, and we throw an exception if this should be the case
		if (instance.getPropertyAsInstance(CoreProperties.outDND.toString()) != null) {
			String msg = String.format("Rewiring in step %s of decision nodes not supported", this.getName());
			log.error(msg);
			throw new RuntimeException(msg);
		}
		outDND.addInStep(this);
		instance.getPropertyAsSingle(CoreProperties.outDND.toString()).set(outDND.getName());
	}
	
	public DecisionNodeDefinition getOutDND() {
		String id = (String) instance.getPropertyAsValueOrNull(CoreProperties.outDND.toString());
		if (id == null) return null;
		else {
			ProcessDefinition pd = this.getProcess();
			if (pd == null) { // we cannot resolve the id to a decision node without access to the process definition,
				String msg = String.format("StepDefinition %s %s  has no process definition set while accessing outDND, exiting ...", this.getId(), this.getName() );
				log.error(msg);
				throw new RuntimeException(msg); // fail fast and hard
			} 
			DecisionNodeDefinition dnd = pd.getDecisionNodeDefinitionByName(id);
			if (dnd == null) { //if an id is set, then there must be an instance for this, otherwise inconsistent data
				String msg = String.format("StepDefinition %s %s  outDND with id %s which cannot be found in process definition %s , exiting ...", this.getId(), this.getName(), id, pd.getName() );
				log.error(msg);
				throw new RuntimeException(msg); // fail fast and hard
			} else
				return dnd;
		}
	}
	
	public void setInDND(DecisionNodeDefinition inDND) {
		// we assume for now, there is no need for rewiring, and we throw an exception if this should be the case
		if (instance.getPropertyAsInstance(CoreProperties.inDND.toString()) != null) {
			String msg = String.format("Rewiring in step %s of decision nodes not supported", this.getName());
			log.error(msg);
			throw new RuntimeException(msg);
		}
		inDND.addOutStep(this);
		instance.getPropertyAsSingle(CoreProperties.inDND.toString()).set(inDND.getName());		
	}
	
	public DecisionNodeDefinition getInDND() {
		String id = (String) instance.getPropertyAsValueOrNull(CoreProperties.inDND.toString());
		if (id == null) return null;
		else {
			ProcessDefinition pd = this.getProcess();
			if (pd == null) { // we cannot resolve the id to a decision node without access to the process definition,
				String msg = String.format("StepDefinition %s %s  has no process definition set while accessing inDND, exiting ...", this.getId(), this.getName() );
				log.error(msg);
				throw new RuntimeException(msg); // fail fast and hard
			} 
			DecisionNodeDefinition dnd = pd.getDecisionNodeDefinitionByName(id);
			if (dnd == null) { //if an id is set, then there must be an instance for this, otherwise inconsistent data
				String msg = String.format("StepDefinition %s %s  inDND with id %s which cannot be found in process definition %s , exiting ...", this.getId(), this.getName(), id, pd.getName() );
				log.error(msg);
				throw new RuntimeException(msg); // fail fast and hard
			} else
				return dnd;
		}
	}
	
	public void setSpecOrderIndex(int index) {
		instance.getPropertyAsSingle(CoreProperties.specOrderIndex.toString()).set(index);
	}
	
	public Integer getSpecOrderIndex() {
		return (Integer) instance.getPropertyAsValueOrElse(CoreProperties.specOrderIndex.toString(), () -> 0);
	}
	
	public void setHtml_url(String html_url)
	{
		instance.getPropertyAsSingle(CoreProperties.html_url.toString()).set(html_url);
	}
	
	public String getHtml_url()
	{
		return (String) instance.getPropertyAsValueOrElse(CoreProperties.html_url.toString(), () -> "");
	}
	public void setDescription(String des)
	{
		instance.getPropertyAsSingle(CoreProperties.description.toString()).set(des);
	}
	
	public String getDescription()
	{
		return (String) instance.getPropertyAsValueOrElse(CoreProperties.description.toString(), () -> "");
	}
	@Override
	public void deleteCascading() {
		// deleting constraints:
		Map<String, String> status = new HashMap<>();
		InstanceType instType = ProcessStep.getOrCreateDesignSpaceInstanceType(ws, this);
		for (Conditions condition : Conditions.values()) {
			if (this.getCondition(condition).isPresent()) {
				String name = "crd_"+condition+"_"+instType.name();
				ConsistencyRuleType crt = ConsistencyRuleType.consistencyRuleTypeExists(ws,  "crd_"+condition+"_"+instType.name(), instType, this.getCondition(condition).get());
				if (crt != null) crt.delete();
			}	
		}
		this.getInputToOutputMappingRules().entrySet().stream()
			.forEach(entry -> {
				String name = ProcessStep.getDataMappingId(entry, this);
				ConsistencyRuleType crt = ConsistencyRuleType.consistencyRuleTypeExists(ws,  name, instType, entry.getValue());
				if (crt != null) crt.delete();
			});
		//delete qa constraints:
		ProcessDefinition pd = this.getProcess() !=null ? this.getProcess() : (ProcessDefinition)this;
		this.getQAConstraints().stream()
			.forEach(spec -> {
				String specId = ProcessStep.getQASpecId(spec, pd);
				ConsistencyRuleType crt = ConsistencyRuleType.consistencyRuleTypeExists(ws,  specId, instType, spec.getQaConstraintSpec());
				if (crt != null) crt.delete();
			});
		ProcessStep.getOrCreateDesignSpaceInstanceType(instance.workspace, this).delete();
		instance.delete();
	}


	public static InstanceType getOrCreateDesignSpaceCoreSchema(Workspace ws) {
		Optional<InstanceType> thisType = ws.debugInstanceTypes().stream()
				.filter(it -> !it.isDeleted)
				.filter(it -> it.name().contentEquals(designspaceTypeId))
				.findAny();
			if (thisType.isPresent())
				return thisType.get();
			else {
				InstanceType typeStep = ws.createInstanceType(designspaceTypeId, ws.TYPES_FOLDER, ProcessDefinitionScopedElement.getOrCreateDesignSpaceCoreSchema(ws));
				typeStep.createPropertyType(CoreProperties.qaConstraints.toString(), Cardinality.SET, QAConstraintSpec.getOrCreateDesignSpaceCoreSchema(ws));
				typeStep.createPropertyType(CoreProperties.expectedInput.toString(), Cardinality.MAP, ws.META_INSTANCE_TYPE);
				typeStep.createPropertyType(CoreProperties.expectedOutput.toString(), Cardinality.MAP, ws.META_INSTANCE_TYPE);
				typeStep.createPropertyType(CoreProperties.conditions.toString(), Cardinality.MAP, Workspace.STRING);
				typeStep.createPropertyType(CoreProperties.inDND.toString(), Cardinality.SINGLE, Workspace.STRING);
				typeStep.createPropertyType(CoreProperties.outDND.toString(), Cardinality.SINGLE, Workspace.STRING);
				typeStep.createPropertyType((CoreProperties.ioMappingRules.toString()), Cardinality.MAP, Workspace.STRING);
				typeStep.createPropertyType((CoreProperties.specOrderIndex.toString()), Cardinality.SINGLE, Workspace.INTEGER);
				typeStep.createPropertyType((CoreProperties.html_url.toString()), Cardinality.SINGLE, Workspace.STRING);
				typeStep.createPropertyType((CoreProperties.description.toString()), Cardinality.SINGLE, Workspace.STRING);
				return typeStep;
			}
	}

	public static StepDefinition getInstance(String stepId, Workspace ws) {
		Instance instance = ws.createInstance(getOrCreateDesignSpaceCoreSchema(ws), stepId);
		return WrapperCache.getWrappedInstance(StepDefinition.class, instance);
	}



}
