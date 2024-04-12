package at.jku.isse.passiveprocessengine.definition;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import at.jku.isse.designspace.core.model.Cardinality;
import at.jku.isse.designspace.core.model.Instance;
import at.jku.isse.designspace.core.model.InstanceType;
import at.jku.isse.designspace.core.model.MapProperty;
import at.jku.isse.designspace.core.model.PropertyType;
import at.jku.isse.designspace.core.model.SetProperty;
import at.jku.isse.designspace.core.model.Workspace;
import at.jku.isse.designspace.rule.model.ConsistencyRuleType;
import at.jku.isse.passiveprocessengine.ProcessDefinitionScopedElement;
import at.jku.isse.passiveprocessengine.WrapperCache;
import at.jku.isse.passiveprocessengine.analysis.RuleAugmentation;
import at.jku.isse.passiveprocessengine.configurability.ProcessConfigBaseElementFactory;
import at.jku.isse.passiveprocessengine.instance.ProcessStep;
import at.jku.isse.passiveprocessengine.instance.StepLifecycle.Conditions;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class StepDefinition extends ProcessDefinitionScopedElement implements IStepDefinition {

	public static enum CoreProperties {expectedInput, expectedOutput, ioMappingRules, 
							//conditions,
							preconditions, postconditions, cancelconditions, activationconditions,
							qaConstraints,
							inDND, outDND, specOrderIndex,html_url,description,
							hierarchyDepth};
	
	public static final String designspaceTypeId = StepDefinition.class.getSimpleName();
	public static final String NOOPSTEP_PREFIX = "NoOpStep";
	
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
	
	@Deprecated(forRemoval = true)
	public Optional<String> getCondition(Conditions condition) {
		SetProperty<?> propSet = null;		
		switch(condition) {
		case ACTIVATION:
			propSet = instance.getPropertyAsSet(CoreProperties.activationconditions.toString());			
			break;
		case CANCELATION:
			propSet = instance.getPropertyAsSet(CoreProperties.cancelconditions.toString());
			break;
		case POSTCONDITION:
			propSet = instance.getPropertyAsSet(CoreProperties.postconditions.toString());
			break;
		case PRECONDITION:
			propSet = instance.getPropertyAsSet(CoreProperties.preconditions.toString());
			break;
		default:
			break;
		}				
		if (propSet != null && propSet.get() != null) {
			return propSet.get().stream()
					.map(inst -> WrapperCache.getWrappedInstance(ConstraintSpec.class, (Instance) inst))
					.filter(Objects::nonNull)
					.map(spec -> ((ConstraintSpec) spec).getConstraintSpec())
					.findAny();						
		} else 
			return Optional.empty();						
	}
	
	@SuppressWarnings("unchecked")
	public Set<ConstraintSpec> getPreconditions() {
		SetProperty<?> qaSet = instance.getPropertyAsSet(CoreProperties.preconditions.toString());
		if (qaSet != null && qaSet.get() != null) {
			return (Set<ConstraintSpec>) qaSet.get().stream()
					.map(inst -> WrapperCache.getWrappedInstance(ConstraintSpec.class, (Instance) inst))
					.collect(Collectors.toSet());
		} else return Collections.emptySet();
	}
	
	@SuppressWarnings("unchecked")
	public Set<ConstraintSpec> getPostconditions() {
		SetProperty<?> qaSet = instance.getPropertyAsSet(CoreProperties.postconditions.toString());
		if (qaSet != null && qaSet.get() != null) {
			return (Set<ConstraintSpec>) qaSet.get().stream()
					.map(inst -> WrapperCache.getWrappedInstance(ConstraintSpec.class, (Instance) inst))
					.collect(Collectors.toSet());
		} else return Collections.emptySet();
	}
	
	@SuppressWarnings("unchecked")
	public Set<ConstraintSpec> getCancelconditions() {
		SetProperty<?> qaSet = instance.getPropertyAsSet(CoreProperties.cancelconditions.toString());
		if (qaSet != null && qaSet.get() != null) {
			return (Set<ConstraintSpec>) qaSet.get().stream()
					.map(inst -> WrapperCache.getWrappedInstance(ConstraintSpec.class, (Instance) inst))
					.collect(Collectors.toSet());
		} else return Collections.emptySet();
	}
	
	@SuppressWarnings("unchecked")
	public Set<ConstraintSpec> getActivationconditions() {
		SetProperty<?> qaSet = instance.getPropertyAsSet(CoreProperties.activationconditions.toString());
		if (qaSet != null && qaSet.get() != null) {
			return (Set<ConstraintSpec>) qaSet.get().stream()
					.map(inst -> WrapperCache.getWrappedInstance(ConstraintSpec.class, (Instance) inst))
					.collect(Collectors.toSet());
		} else return Collections.emptySet();
	}
	
	@SuppressWarnings("unchecked")
	@Deprecated(forRemoval = true)
	public void setCondition(Conditions condition, String ruleAsString) {
		ConstraintSpec constraint = ConstraintSpec.createInstance(condition, condition+"0", ruleAsString, ruleAsString, 0, false, ws);		
		switch(condition) {
		case ACTIVATION:
			instance.getPropertyAsSet(CoreProperties.activationconditions.toString()).add(constraint.getInstance());
			break;
		case CANCELATION:
			instance.getPropertyAsSet(CoreProperties.cancelconditions.toString()).add(constraint.getInstance());
			break;
		case POSTCONDITION:
			instance.getPropertyAsSet(CoreProperties.postconditions.toString()).add(constraint.getInstance());
			break;
		case PRECONDITION:
			instance.getPropertyAsSet(CoreProperties.preconditions.toString()).add(constraint.getInstance());
			break;
		default:
			break;
		
		}				
	}
	
	@SuppressWarnings("unchecked")
	public void addPrecondition(ConstraintSpec spec) {
		instance.getPropertyAsSet(CoreProperties.preconditions.toString()).add(spec.getInstance());
	}
	
	@SuppressWarnings("unchecked")
	public void addPostcondition(ConstraintSpec spec) {
		instance.getPropertyAsSet(CoreProperties.postconditions.toString()).add(spec.getInstance());
	}
	
	@SuppressWarnings("unchecked")
	public void addCancelcondition(ConstraintSpec spec) {
		instance.getPropertyAsSet(CoreProperties.cancelconditions.toString()).add(spec.getInstance());
	}
	
	@SuppressWarnings("unchecked")
	public void addActivationcondition(ConstraintSpec spec) {
		instance.getPropertyAsSet(CoreProperties.activationconditions.toString()).add(spec.getInstance());
	}

	@SuppressWarnings("unchecked")
	public Set<ConstraintSpec> getQAConstraints() {
		SetProperty<?> qaSet = instance.getPropertyAsSet(CoreProperties.qaConstraints.toString());
		if (qaSet != null && qaSet.get() != null) {
			return (Set<ConstraintSpec>) qaSet.get().stream()
					.map(inst -> WrapperCache.getWrappedInstance(ConstraintSpec.class, (Instance) inst))
					.collect(Collectors.toSet());
		} else return Collections.emptySet();
	}
	
	@SuppressWarnings("unchecked")
	public void addQAConstraint(ConstraintSpec spec) {
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
	
	public void setDepthIndexRecursive(int indexToSet) {				
		instance.getPropertyAsSingle(CoreProperties.hierarchyDepth.toString()).set(indexToSet);
		DecisionNodeDefinition dnd = this.getOutDND();
		if (dnd != null) { //avoid NPE on process without outDND					
			int newIndex = (dnd.getInSteps().size() > 1) ? indexToSet - 1 : indexToSet; // if in branching, reduction of index, otherwise same index as just a sequence				
			if (dnd.getDepthIndex() < newIndex) // this allows to override the index when this is used as a subprocess
				dnd.setDepthIndexRecursive(newIndex);
		}
	}	
	
	public Integer getSpecOrderIndex() {
		return (Integer) instance.getPropertyAsValueOrElse(CoreProperties.specOrderIndex.toString(), () -> -1);
	}	
	
	public Integer getDepthIndex() {
		return (Integer) instance.getPropertyAsValueOrElse(CoreProperties.hierarchyDepth.toString(), () -> -1);
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
	
	private void checkConstraintExists(InstanceType instType, ConstraintSpec spec, Conditions condition, List<ProcessDefinitionError> errors) {
		String name = RuleAugmentation.getConstraintName(condition, spec.getOrderIndex(), instType);
		ConsistencyRuleType crt = getRuleByNameAndContext(name, instType);
		if (crt == null) {
			log.error("Expected Rule for existing process not found: "+name);
			errors.add(new ProcessDefinitionError(this, "Expected Constraint Not Found - Internal Data Corruption", name));				
		} else {
			if (crt.hasRuleError())
				errors.add(new ProcessDefinitionError(this, String.format("Condition % has an error", spec.getName()), crt.ruleError()));
		}
	}
	
	public List<ProcessDefinitionError> checkConstraintValidity(InstanceType processInstType) {
		List<ProcessDefinitionError> errors = new LinkedList<>();
		InstanceType instType = ProcessStep.getOrCreateDesignSpaceInstanceType(ws, this, processInstType);
		
		this.getActivationconditions().stream().forEach(spec -> checkConstraintExists(instType, spec, Conditions.ACTIVATION, errors));
		this.getCancelconditions().stream().forEach(spec -> checkConstraintExists(instType, spec, Conditions.CANCELATION, errors));
		this.getPostconditions().stream().forEach(spec -> checkConstraintExists(instType, spec, Conditions.POSTCONDITION, errors));
		this.getPreconditions().stream().forEach(spec -> checkConstraintExists(instType, spec, Conditions.PRECONDITION, errors));
		
		this.getInputToOutputMappingRules().entrySet().stream()
			.forEach(entry -> {
				String name = ProcessStep.getDataMappingId(entry, this);
				String propName = ProcessStep.CRD_DATAMAPPING_PREFIX+entry.getKey();
				InstanceType stepType = ProcessStep.getOrCreateDesignSpaceInstanceType(ws, this, processInstType);
				PropertyType ioPropType = stepType.getPropertyType(propName);
				InstanceType ruleType = ioPropType.referencedInstanceType();
				if (ruleType == null) 	{							
					log.error("Expected Datamapping Rule for existing process not found: "+name);
					//status.put(name, "Corrupt data - Expected Datamapping Rule not found");
					errors.add(new ProcessDefinitionError(this, "Expected DataMapping Not Found - Internal Data Corruption", name));
				} else {
					ConsistencyRuleType crt = (ConsistencyRuleType)ruleType;
					if (crt.hasRuleError())
						errors.add(new ProcessDefinitionError(this, String.format("DataMapping %s has an error", name), crt.ruleError()));
				}
			});
		//qa constraints:
		ProcessDefinition pd = this.getProcess() !=null ? this.getProcess() : (ProcessDefinition)this;
		this.getQAConstraints().stream()
			.forEach(spec -> {
				String specId = ProcessStep.getQASpecId(spec, pd);
				ConsistencyRuleType crt = getRuleByNameAndContext(specId, instType);//ConsistencyRuleType.consistencyRuleTypeExists(ws,  specId, instType, spec.getQaConstraintSpec());
				if (crt == null) {
					log.error("Expected Rule for existing process not found: "+specId);
					errors.add(new ProcessDefinitionError(this, "Expected QA Constraint Not Found - Internal Data Corruption", specId));
				} else
					if (crt.hasRuleError())
						errors.add(new ProcessDefinitionError(this, String.format("QA Constraint %s has an error", specId), crt.ruleError()));
			});
		return errors;
	}

	
	public List<ProcessDefinitionError> checkStepStructureValidity() {		
		List<ProcessDefinitionError> errors = new LinkedList<>();				
		if (getPostconditions().isEmpty() && !this.getName().startsWith(NOOPSTEP_PREFIX)) {			
			errors.add(new ProcessDefinitionError(this, "No Condition Defined", "Step needs exactly one post condition to signal when a step is considered finished."));
		}
		if (getExpectedInput().isEmpty() && !this.getName().startsWith(NOOPSTEP_PREFIX)) {						
			errors.add(new ProcessDefinitionError(this, "No Input Defined", "Step needs at least one input."));
		}
		getExpectedInput().forEach((in, type) -> { 
			if (type == null) 
				errors.add(new ProcessDefinitionError(this, "Unavailable Type", "Artifact type of input '"+in+"' could not be resolved"));
			});
		getExpectedOutput().forEach((out, type) -> { 
			if (type == null) 
				errors.add(new ProcessDefinitionError(this, "Unavailable Type", "Artifact type of output '"+out+"' could not be resolved"));
			});
		getExpectedOutput().forEach((out, type) -> {
			if (!getInputToOutputMappingRules().containsKey(out))
				errors.add(new ProcessDefinitionError(this, "No Mapping Defined", "Step output '"+out+"' has not datamapping from input defined"));
			});
		
		return errors;
	}
	
	private void deleteRuleIfExists(InstanceType instType, ConstraintSpec spec, Conditions condition ) {
		String name = RuleAugmentation.getConstraintName(condition, spec.getOrderIndex(), instType);
		ConsistencyRuleType crt = getRuleByNameAndContext(name, instType);
		if (crt != null) crt.delete();
	}
	
	@Override
	public void deleteCascading(ProcessConfigBaseElementFactory configFactory) {		
		
		InstanceType instType = ProcessStep.getOrCreateDesignSpaceInstanceType(ws, this, null); // for deletion its ok to not provide the process instance type
		
		this.getActivationconditions().stream().forEach(spec -> deleteRuleIfExists(instType, spec, Conditions.ACTIVATION));
		this.getCancelconditions().stream().forEach(spec -> deleteRuleIfExists(instType, spec, Conditions.CANCELATION));
		this.getPostconditions().stream().forEach(spec -> deleteRuleIfExists(instType, spec, Conditions.POSTCONDITION));
		this.getPreconditions().stream().forEach(spec -> deleteRuleIfExists(instType, spec, Conditions.PRECONDITION));
		
		this.getInputToOutputMappingRules().entrySet().stream()
			.forEach(entry -> {
				String name = ProcessStep.getDataMappingId(entry, this);
				ConsistencyRuleType crt = getRuleByNameAndContext(name, instType);
				if (crt != null) crt.delete();
			});
		//delete qa constraints:
		ProcessDefinition pd = this.getProcess() !=null ? this.getProcess() : (ProcessDefinition)this;
		this.getQAConstraints().stream()
			.forEach(spec -> {
				String specId = ProcessStep.getQASpecId(spec, pd);
				ConsistencyRuleType crt = getRuleByNameAndContext(specId, instType);
				if (crt != null) crt.delete();
				spec.deleteCascading(configFactory);
			});
		
		instType.delete();
		super.deleteCascading(configFactory);
	}


	public static InstanceType getOrCreateDesignSpaceCoreSchema(Workspace ws) {
		Optional<InstanceType> thisType = Optional.ofNullable(ws.TYPES_FOLDER.instanceTypeWithName(designspaceTypeId)); 
//		Optional<InstanceType> thisType = ws.debugInstanceTypes().stream()
//				.filter(it -> !it.isDeleted)
//				.filter(it -> it.name().contentEquals(designspaceTypeId))
//				.findAny();
			if (thisType.isPresent())
				return thisType.get();
			else {
				InstanceType stepType = ws.createInstanceType(designspaceTypeId, ws.TYPES_FOLDER, ProcessDefinitionScopedElement.getOrCreateDesignSpaceCoreSchema(ws));
				stepType.createPropertyType(CoreProperties.qaConstraints.toString(), Cardinality.SET, ConstraintSpec.getOrCreateDesignSpaceCoreSchema(ws));
				stepType.createPropertyType(CoreProperties.expectedInput.toString(), Cardinality.MAP, ws.META_INSTANCE_TYPE);
				stepType.createPropertyType(CoreProperties.expectedOutput.toString(), Cardinality.MAP, ws.META_INSTANCE_TYPE);
				//typeStep.createPropertyType(CoreProperties.conditions.toString(), Cardinality.MAP, Workspace.STRING);
				stepType.createPropertyType(CoreProperties.preconditions.toString(), Cardinality.SET, ConstraintSpec.getOrCreateDesignSpaceCoreSchema(ws));
				stepType.createPropertyType(CoreProperties.postconditions.toString(), Cardinality.SET, ConstraintSpec.getOrCreateDesignSpaceCoreSchema(ws));
				stepType.createPropertyType(CoreProperties.cancelconditions.toString(), Cardinality.SET, ConstraintSpec.getOrCreateDesignSpaceCoreSchema(ws));
				stepType.createPropertyType(CoreProperties.activationconditions.toString(), Cardinality.SET, ConstraintSpec.getOrCreateDesignSpaceCoreSchema(ws));				
				stepType.createPropertyType(CoreProperties.inDND.toString(), Cardinality.SINGLE, Workspace.STRING);
				stepType.createPropertyType(CoreProperties.outDND.toString(), Cardinality.SINGLE, Workspace.STRING);
				stepType.createPropertyType((CoreProperties.ioMappingRules.toString()), Cardinality.MAP, Workspace.STRING);
				stepType.createPropertyType((CoreProperties.specOrderIndex.toString()), Cardinality.SINGLE, Workspace.INTEGER);
				stepType.createPropertyType((CoreProperties.hierarchyDepth.toString()), Cardinality.SINGLE, Workspace.INTEGER);
				stepType.createPropertyType((CoreProperties.html_url.toString()), Cardinality.SINGLE, Workspace.STRING);
				stepType.createPropertyType((CoreProperties.description.toString()), Cardinality.SINGLE, Workspace.STRING);
				return stepType;
			}
	}

	public static StepDefinition getInstance(String stepId, Workspace ws) {
		Instance instance = ws.createInstance(getOrCreateDesignSpaceCoreSchema(ws), stepId);
		return WrapperCache.getWrappedInstance(StepDefinition.class, instance);
	}



}

