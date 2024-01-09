package at.jku.isse.passiveprocessengine.definition.activeobjects;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import at.jku.isse.passiveprocessengine.Context;
import at.jku.isse.passiveprocessengine.analysis.RuleAugmentation;
import at.jku.isse.passiveprocessengine.configurability.ProcessConfigBaseElementFactory;
import at.jku.isse.passiveprocessengine.core.Instance;
import at.jku.isse.passiveprocessengine.core.InstanceType;
import at.jku.isse.passiveprocessengine.core.InstanceType.PropertyType;
import at.jku.isse.passiveprocessengine.core.RuleDefinition;
import at.jku.isse.passiveprocessengine.definition.IStepDefinition;
import at.jku.isse.passiveprocessengine.definition.ProcessDefinitionError;
import at.jku.isse.passiveprocessengine.definition.types.ProcessStepDefinitionType;
import at.jku.isse.passiveprocessengine.instance.ProcessStep;
import at.jku.isse.passiveprocessengine.instance.StepLifecycle.Conditions;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class StepDefinition extends ProcessDefinitionScopedElement implements IStepDefinition {

	public static final String NOOPSTEP_PREFIX = "NoOpStep";

	public StepDefinition(Instance instance, Context context) {
		super(instance, context);
	}

	@Override
	public Map<String, InstanceType> getExpectedInput() {
		@SuppressWarnings("unchecked")
		Map<String, InstanceType> inMap = instance.getTypedProperty(ProcessStepDefinitionType.CoreProperties.expectedInput.toString(), Map.class);
		if (inMap != null) {
			return inMap;
		} else return Collections.emptyMap();
	}

	@SuppressWarnings("unchecked")
	public void addExpectedInput(String paramName, InstanceType type) {
		assert(paramName != null);
		assert(type != null);
		instance.getTypedProperty(ProcessStepDefinitionType.CoreProperties.expectedInput.toString(), Map.class).put(paramName, type);
	}

	@Override
	@SuppressWarnings("unchecked")
	public Map<String, InstanceType> getExpectedOutput() {
		Map outMap = instance.getTypedProperty(ProcessStepDefinitionType.CoreProperties.expectedOutput.toString(), Map.class);
		if (outMap != null) {
			return outMap;
		} else return Collections.emptyMap();
	}

	@SuppressWarnings("unchecked")
	public void addExpectedOutput(String paramName, InstanceType type) {
		assert(paramName != null);
		assert(type != null);
		instance.getTypedProperty(ProcessStepDefinitionType.CoreProperties.expectedOutput.toString(), Map.class).put(paramName, type);
	}

	@Override
	@Deprecated(forRemoval = true)
	public Optional<String> getCondition(Conditions condition) {
		Set<?> propSet = null;
		switch(condition) {
		case ACTIVATION:
			propSet = instance.getTypedProperty(ProcessStepDefinitionType.CoreProperties.activationconditions.toString(), Set.class);
			break;
		case CANCELATION:
			propSet = instance.getTypedProperty(ProcessStepDefinitionType.CoreProperties.cancelconditions.toString(), Set.class);
			break;
		case POSTCONDITION:
			propSet = instance.getTypedProperty(ProcessStepDefinitionType.CoreProperties.postconditions.toString(), Set.class);
			break;
		case PRECONDITION:
			propSet = instance.getTypedProperty(ProcessStepDefinitionType.CoreProperties.preconditions.toString(), Set.class);
			break;
		default:
			break;
		}
		if (propSet != null ) {
			return propSet.stream()
					.map(inst -> context.getWrappedInstance(ConstraintSpec.class, (Instance) inst))
					.filter(Objects::nonNull)
					.map(spec -> ((ConstraintSpec) spec).getConstraintSpec())
					.findAny();
		} else
			return Optional.empty();
	}

	@SuppressWarnings("unchecked")
	public Set<ConstraintSpec> getPreconditions() {
		Set<?> qaSet = instance.getTypedProperty(ProcessStepDefinitionType.CoreProperties.preconditions.toString(), Set.class);
		if (qaSet != null) {
			return qaSet.stream()
					.map(inst -> context.getWrappedInstance(ConstraintSpec.class, (Instance) inst))
					.map(obj -> (ConstraintSpec)obj)
					.collect(Collectors.toSet());
		} else return Collections.emptySet();
	}

	@SuppressWarnings("unchecked")
	public Set<ConstraintSpec> getPostconditions() {
		Set<?> qaSet = instance.getTypedProperty(ProcessStepDefinitionType.CoreProperties.postconditions.toString(), Set.class);
		if (qaSet != null) {
			return qaSet.stream()
					.map(inst -> context.getWrappedInstance(ConstraintSpec.class, (Instance) inst))
					.map(obj -> (ConstraintSpec)obj)
					.collect(Collectors.toSet());
		} else return Collections.emptySet();
	}

	@SuppressWarnings("unchecked")
	public Set<ConstraintSpec> getCancelconditions() {
		Set<?> qaSet = instance.getTypedProperty(ProcessStepDefinitionType.CoreProperties.cancelconditions.toString(), Set.class);
		if (qaSet != null) {
			return qaSet.stream()
					.map(inst -> context.getWrappedInstance(ConstraintSpec.class, (Instance) inst))
					.map(obj -> (ConstraintSpec)obj)
					.collect(Collectors.toSet());
		} else return Collections.emptySet();
	}

	@SuppressWarnings("unchecked")
	public Set<ConstraintSpec> getActivationconditions() {
		Set<?> qaSet = instance.getTypedProperty(ProcessStepDefinitionType.CoreProperties.activationconditions.toString(), Set.class);
		if (qaSet != null) {
			return qaSet.stream()
					.map(inst -> context.getWrappedInstance(ConstraintSpec.class, (Instance) inst))
					.map(obj -> (ConstraintSpec)obj)
					.collect(Collectors.toSet());
		} else return Collections.emptySet();
	}

	@SuppressWarnings("unchecked")
	@Deprecated(forRemoval = true)
	public void setCondition(Conditions condition, String ruleAsString) {
		ConstraintSpec constraint = context.getDefinitionFactoryIndex().getConstraintFactory().createInstance(condition, condition+"0", ruleAsString, ruleAsString, 0, false);
		switch(condition) {
		case ACTIVATION:
			instance.getTypedProperty(ProcessStepDefinitionType.CoreProperties.activationconditions.toString(), Set.class).add(constraint);
			break;
		case CANCELATION:
			instance.getTypedProperty(ProcessStepDefinitionType.CoreProperties.cancelconditions.toString(), Set.class).add(constraint);
			break;
		case POSTCONDITION:
			instance.getTypedProperty(ProcessStepDefinitionType.CoreProperties.postconditions.toString(), Set.class).add(constraint);
			break;
		case PRECONDITION:
			instance.getTypedProperty(ProcessStepDefinitionType.CoreProperties.preconditions.toString(), Set.class).add(constraint);
			break;
		default:
			break;

		}
	}

	@SuppressWarnings("unchecked")
	public void addPrecondition(ConstraintSpec spec) {
		instance.getTypedProperty(ProcessStepDefinitionType.CoreProperties.preconditions.toString(), Set.class).add(spec.getInstance());
	}

	@SuppressWarnings("unchecked")
	public void addPostcondition(ConstraintSpec spec) {
		instance.getTypedProperty(ProcessStepDefinitionType.CoreProperties.postconditions.toString(), Set.class).add(spec.getInstance());
	}

	@SuppressWarnings("unchecked")
	public void addCancelcondition(ConstraintSpec spec) {
		instance.getTypedProperty(ProcessStepDefinitionType.CoreProperties.cancelconditions.toString(), Set.class).add(spec.getInstance());
	}

	@SuppressWarnings("unchecked")
	public void addActivationcondition(ConstraintSpec spec) {
		instance.getTypedProperty(ProcessStepDefinitionType.CoreProperties.activationconditions.toString(), Set.class).add(spec.getInstance());
	}

	@Override
	@SuppressWarnings("unchecked")
	public Set<ConstraintSpec> getQAConstraints() {
		Set<?> qaSet = instance.getTypedProperty(ProcessStepDefinitionType.CoreProperties.qaConstraints.toString(), Set.class);
		if (qaSet != null ) {
			return  qaSet.stream()
					.map(inst -> context.getWrappedInstance(ConstraintSpec.class, (Instance) inst))
					.map(obj -> (ConstraintSpec)obj)
					.collect(Collectors.toSet());
		} else return Collections.emptySet();
	}

	@SuppressWarnings("unchecked")
	public void addQAConstraint(ConstraintSpec spec) {
		instance.getTypedProperty(ProcessStepDefinitionType.CoreProperties.qaConstraints.toString(), Set.class).add(spec.getInstance());
	}

	@SuppressWarnings("unchecked")
	@Override
	public Map<String, String> getInputToOutputMappingRules() {
		Map<?,?> rawMap = instance.getTypedProperty(ProcessStepDefinitionType.CoreProperties.ioMappingRules.toString(), Map.class);
		return (Map<String,String>) rawMap;
	}

	@SuppressWarnings("unchecked")
	public void addInputToOutputMappingRule(String ruleId, String rule) {
		instance.getTypedProperty(ProcessStepDefinitionType.CoreProperties.ioMappingRules.toString(), Map.class).put(ruleId, rule);
	}

	public void setOutDND(DecisionNodeDefinition outDND) {
		// we assume for now, there is no need for rewiring, and we throw an exception if this should be the case
		if (instance.getTypedProperty(ProcessStepDefinitionType.CoreProperties.outDND.toString(), Instance.class) != null) {
			String msg = String.format("Rewiring in step %s of decision nodes not supported", this.getName());
			log.error(msg);
			throw new RuntimeException(msg);
		}
		outDND.addInStep(this);
		instance.setSingleProperty(ProcessStepDefinitionType.CoreProperties.outDND.toString(), outDND.getName());
	}

	@Override
	public DecisionNodeDefinition getOutDND() {
		String id = instance.getTypedProperty(ProcessStepDefinitionType.CoreProperties.outDND.toString(), String.class);
		if (id == null) return null;
		else {
			ProcessDefinition pd = this.getProcess();
			if (pd == null) { // we cannot resolve the id to a decision node without access to the process definition,
				String msg = String.format("StepDefinition %s  has no process definition set while accessing outDND, exiting ...", this.getName() );
				log.error(msg);
				throw new RuntimeException(msg); // fail fast and hard
			}
			DecisionNodeDefinition dnd = pd.getDecisionNodeDefinitionByName(id);
			if (dnd == null) { //if an id is set, then there must be an instance for this, otherwise inconsistent data
				String msg = String.format("StepDefinition %s  outDND with id %s which cannot be found in process definition %s , exiting ...", this.getName(), id, pd.getName() );
				log.error(msg);
				throw new RuntimeException(msg); // fail fast and hard
			} else
				return dnd;
		}
	}

	public void setInDND(DecisionNodeDefinition inDND) {
		// we assume for now, there is no need for rewiring, and we throw an exception if this should be the case
		if (instance.getTypedProperty(ProcessStepDefinitionType.CoreProperties.inDND.toString(), Instance.class) != null) {
			String msg = String.format("Rewiring in step %s of decision nodes not supported", this.getName());
			log.error(msg);
			throw new RuntimeException(msg);
		}
		inDND.addOutStep(this);
		instance.setSingleProperty(ProcessStepDefinitionType.CoreProperties.inDND.toString(),inDND.getName());
	}

	@Override
	public DecisionNodeDefinition getInDND() {
		String id = instance.getTypedProperty(ProcessStepDefinitionType.CoreProperties.inDND.toString(), String.class);
		if (id == null) return null;
		else {
			ProcessDefinition pd = this.getProcess();
			if (pd == null) { // we cannot resolve the id to a decision node without access to the process definition,
				String msg = String.format("StepDefinition %s %s  has no process definition set while accessing inDND, exiting ...", this.getName(), this.getName() );
				log.error(msg);
				throw new RuntimeException(msg); // fail fast and hard
			}
			DecisionNodeDefinition dnd = pd.getDecisionNodeDefinitionByName(id);
			if (dnd == null) { //if an id is set, then there must be an instance for this, otherwise inconsistent data
				String msg = String.format("StepDefinition %s %s  inDND with id %s which cannot be found in process definition %s , exiting ...", this.getName(), this.getName(), id, pd.getName() );
				log.error(msg);
				throw new RuntimeException(msg); // fail fast and hard
			} else
				return dnd;
		}
	}

	public void setSpecOrderIndex(int index) {
		instance.setSingleProperty(ProcessStepDefinitionType.CoreProperties.specOrderIndex.toString(), index);
	}

	public void setDepthIndexRecursive(int indexToSet) {
		instance.setSingleProperty(ProcessStepDefinitionType.CoreProperties.hierarchyDepth.toString(), indexToSet);
		DecisionNodeDefinition dnd = this.getOutDND();
		if (dnd != null) { //avoid NPE on process without outDND
			int newIndex = (dnd.getInSteps().size() > 1) ? indexToSet - 1 : indexToSet; // if in branching, reduction of index, otherwise same index as just a sequence
			if (dnd.getDepthIndex() < newIndex) // this allows to override the index when this is used as a subprocess
				dnd.setDepthIndexRecursive(newIndex);
		}
	}

	public Integer getSpecOrderIndex() {
		return instance.getTypedProperty(ProcessStepDefinitionType.CoreProperties.specOrderIndex.toString(), Integer.class, -1);
	}

	public Integer getDepthIndex() {
		return instance.getTypedProperty(ProcessStepDefinitionType.CoreProperties.hierarchyDepth.toString(), Integer.class, -1);
	}

	public void setHtml_url(String html_url)
	{
		instance.setSingleProperty(ProcessStepDefinitionType.CoreProperties.html_url.toString(), html_url);
	}

	public String getHtml_url()
	{
		return instance.getTypedProperty(ProcessStepDefinitionType.CoreProperties.html_url.toString(), String.class, "");
	}
	public void setDescription(String des)
	{
		instance.setSingleProperty(ProcessStepDefinitionType.CoreProperties.description.toString(), des);
	}

	public String getDescription()
	{
		return (String) instance.getTypedProperty(ProcessStepDefinitionType.CoreProperties.description.toString(), String.class, "");
	}

	private void checkConstraintExists(InstanceType instType, ConstraintSpec spec, Conditions condition, List<ProcessDefinitionError> errors) {
		String name = RuleAugmentation.getConstraintName(condition, spec.getOrderIndex(), instType);
		RuleDefinition crt = context.getSchemaRegistry().getRuleByNameAndContext(name, instType);
		if (crt == null) {
			log.error("Expected Rule for existing process not found: "+name);
			errors.add(new ProcessDefinitionError(this, "Expected Constraint Not Found - Internal Data Corruption", name));
		} else {
			if (crt.hasRuleError())
				errors.add(new ProcessDefinitionError(this, String.format("Condition % has an error", spec.getName()), crt.getRuleError()));
		}
	}

	public List<ProcessDefinitionError> checkConstraintValidity(InstanceType processInstType) {
		List<ProcessDefinitionError> errors = new LinkedList<>();
		InstanceType instType = this.instance.getInstanceType(); //ProcessStep.getOrCreateDesignSpaceInstanceType(ws, this, processInstType);

		this.getActivationconditions().stream().forEach(spec -> checkConstraintExists(instType, spec, Conditions.ACTIVATION, errors));
		this.getCancelconditions().stream().forEach(spec -> checkConstraintExists(instType, spec, Conditions.CANCELATION, errors));
		this.getPostconditions().stream().forEach(spec -> checkConstraintExists(instType, spec, Conditions.POSTCONDITION, errors));
		this.getPreconditions().stream().forEach(spec -> checkConstraintExists(instType, spec, Conditions.PRECONDITION, errors));

		this.getInputToOutputMappingRules().entrySet().stream()
			.forEach(entry -> {
				String name = ProcessStep.getDataMappingId(entry, this);
				String propName = ProcessStep.CRD_DATAMAPPING_PREFIX+entry.getKey();
				//InstanceType stepType = ProcessStep.getOrCreateDesignSpaceInstanceType(ws, this, processInstType);
				PropertyType ioPropType = instType.getPropertyType(propName);
				InstanceType ruleType = ioPropType.getInstanceType();
				if (ruleType == null) 	{
					log.error("Expected Datamapping Rule for existing process not found: "+name);
					//status.put(name, "Corrupt data - Expected Datamapping Rule not found");
					errors.add(new ProcessDefinitionError(this, "Expected DataMapping Not Found - Internal Data Corruption", name));
				} else {
					RuleDefinition crt = (RuleDefinition)ruleType;
					if (crt.hasRuleError())
						errors.add(new ProcessDefinitionError(this, String.format("DataMapping %s has an error", name), crt.getRuleError()));
				}
			});
		//qa constraints:
		ProcessDefinition pd = this.getProcess() !=null ? this.getProcess() : (ProcessDefinition)this;
		this.getQAConstraints().stream()
			.forEach(spec -> {
				String specId = ProcessStep.getQASpecId(spec, pd);
				RuleDefinition crt = context.getSchemaRegistry().getRuleByNameAndContext(specId, instType);//RuleDefinition.RuleDefinitionExists(ws,  specId, instType, spec.getQaConstraintSpec());
				if (crt == null) {
					log.error("Expected Rule for existing process not found: "+specId);
					errors.add(new ProcessDefinitionError(this, "Expected QA Constraint Not Found - Internal Data Corruption", specId));
				} else
					if (crt.hasRuleError())
						errors.add(new ProcessDefinitionError(this, String.format("QA Constraint %s has an error", specId), crt.getRuleError()));
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
		RuleDefinition crt = context.getSchemaRegistry().getRuleByNameAndContext(name, instType);
		if (crt != null) 
			crt.markAsDeleted();
	}

	@Override
	public void deleteCascading(ProcessConfigBaseElementFactory configFactory) {

		InstanceType instType = this.instance.getInstanceType();//ProcessStep.getOrCreateDesignSpaceInstanceType(ws, this, null); // for deletion its ok to not provide the process instance type

		this.getActivationconditions().stream().forEach(spec -> deleteRuleIfExists(instType, spec, Conditions.ACTIVATION));
		this.getCancelconditions().stream().forEach(spec -> deleteRuleIfExists(instType, spec, Conditions.CANCELATION));
		this.getPostconditions().stream().forEach(spec -> deleteRuleIfExists(instType, spec, Conditions.POSTCONDITION));
		this.getPreconditions().stream().forEach(spec -> deleteRuleIfExists(instType, spec, Conditions.PRECONDITION));

		this.getInputToOutputMappingRules().entrySet().stream()
			.forEach(entry -> {
				String name = ProcessStep.getDataMappingId(entry, this);
				RuleDefinition crt = context.getSchemaRegistry().getRuleByNameAndContext(name, instType);
				if (crt != null) crt.markAsDeleted();
			});
		//delete qa constraints:
		ProcessDefinition pd = this.getProcess() !=null ? this.getProcess() : (ProcessDefinition)this;
		this.getQAConstraints().stream()
			.forEach(spec -> {
				String specId = ProcessStep.getQASpecId(spec, pd);
				RuleDefinition crt = context.getSchemaRegistry().getRuleByNameAndContext(specId, instType);
				if (crt != null) 
					crt.markAsDeleted();
				spec.deleteCascading(configFactory);
			});

		instType.markAsDeleted();
		super.deleteCascading(configFactory);
	}


	



}

