package at.jku.isse.passiveprocessengine.definition.activeobjects;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import at.jku.isse.passiveprocessengine.core.ProcessContext;
import at.jku.isse.passiveprocessengine.core.PPEInstance;
import at.jku.isse.passiveprocessengine.core.PPEInstanceType;
import at.jku.isse.passiveprocessengine.core.RuleDefinition;
import at.jku.isse.passiveprocessengine.definition.IStepDefinition;
import at.jku.isse.passiveprocessengine.definition.ProcessDefinitionError;
import at.jku.isse.passiveprocessengine.definition.factories.ProcessDefinitionFactory;
import at.jku.isse.passiveprocessengine.definition.types.ProcessStepDefinitionType;
import at.jku.isse.passiveprocessengine.instance.StepLifecycle.Conditions;
import at.jku.isse.passiveprocessengine.instance.types.SpecificProcessInstanceType;
import at.jku.isse.passiveprocessengine.instance.types.SpecificProcessStepType;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class StepDefinition extends ProcessDefinitionScopedElement implements IStepDefinition {

	public static final String NOOPSTEP_PREFIX = "NoOpStep";

	public StepDefinition(PPEInstance instance, ProcessContext context) {
		super(instance, context);
	}

	@Override
	public Map<String, PPEInstanceType> getExpectedInput() {
		@SuppressWarnings("unchecked")
		Map<String, PPEInstanceType> inMap = instance.getTypedProperty(ProcessStepDefinitionType.CoreProperties.expectedInput.toString(), Map.class);
		if (inMap != null) {
			return inMap;
		} else return Collections.emptyMap();
	}

	@SuppressWarnings("unchecked")
	public void addExpectedInput(String paramName, PPEInstanceType type) {
		assert(paramName != null);
		assert(type != null);
		instance.getTypedProperty(ProcessStepDefinitionType.CoreProperties.expectedInput.toString(), Map.class).put(paramName, type);
	}

	@Override
	@SuppressWarnings("unchecked")
	public Map<String, PPEInstanceType> getExpectedOutput() {
		Map outMap = instance.getTypedProperty(ProcessStepDefinitionType.CoreProperties.expectedOutput.toString(), Map.class);
		if (outMap != null) {
			return outMap;
		} else return Collections.emptyMap();
	}

	@SuppressWarnings("unchecked")
	public void addExpectedOutput(String paramName, PPEInstanceType type) {
		assert(paramName != null);
		assert(type != null);
		instance.getTypedProperty(ProcessStepDefinitionType.CoreProperties.expectedOutput.toString(), Map.class).put(paramName, type);
	}

//	@Override
//	@Deprecated(forRemoval = true)
//	public Optional<String> getCondition(Conditions condition) {
//		Set<?> propSet = null;
//		switch(condition) {
//		case ACTIVATION:
//			propSet = instance.getTypedProperty(ProcessStepDefinitionType.CoreProperties.activationconditions.toString(), Set.class);
//			break;
//		case CANCELATION:
//			propSet = instance.getTypedProperty(ProcessStepDefinitionType.CoreProperties.cancelconditions.toString(), Set.class);
//			break;
//		case POSTCONDITION:
//			propSet = instance.getTypedProperty(ProcessStepDefinitionType.CoreProperties.postconditions.toString(), Set.class);
//			break;
//		case PRECONDITION:
//			propSet = instance.getTypedProperty(ProcessStepDefinitionType.CoreProperties.preconditions.toString(), Set.class);
//			break;
//		default:
//			break;
//		}
//		if (propSet != null ) {
//			return propSet.stream()
//					.map(inst -> context.getWrappedInstance(ConstraintSpec.class, (PPEInstance) inst))
//					.filter(Objects::nonNull)
//					.map(spec -> ((ConstraintSpec) spec).getConstraintSpec())
//					.findAny();
//		} else
//			return Optional.empty();
//	}

	@SuppressWarnings("unchecked")
	public Set<ConstraintSpec> getPreconditions() {
		Set<?> qaSet = instance.getTypedProperty(ProcessStepDefinitionType.CoreProperties.preconditions.toString(), Set.class);
		if (qaSet != null) {
			return qaSet.stream()
					.map(inst -> context.getWrappedInstance(ConstraintSpec.class, (PPEInstance) inst))
					.map(obj -> (ConstraintSpec)obj)
					.collect(Collectors.toSet());
		} else return Collections.emptySet();
	}

	@SuppressWarnings("unchecked")
	public Set<ConstraintSpec> getPostconditions() {
		Set<?> qaSet = instance.getTypedProperty(ProcessStepDefinitionType.CoreProperties.postconditions.toString(), Set.class);
		if (qaSet != null) {
			return qaSet.stream()
					.map(inst -> context.getWrappedInstance(ConstraintSpec.class, (PPEInstance) inst))
					.map(obj -> (ConstraintSpec)obj)
					.collect(Collectors.toSet());
		} else return Collections.emptySet();
	}

	@SuppressWarnings("unchecked")
	public Set<ConstraintSpec> getCancelconditions() {
		Set<?> qaSet = instance.getTypedProperty(ProcessStepDefinitionType.CoreProperties.cancelconditions.toString(), Set.class);
		if (qaSet != null) {
			return qaSet.stream()
					.map(inst -> context.getWrappedInstance(ConstraintSpec.class, (PPEInstance) inst))
					.map(obj -> (ConstraintSpec)obj)
					.collect(Collectors.toSet());
		} else return Collections.emptySet();
	}

	@SuppressWarnings("unchecked")
	public Set<ConstraintSpec> getActivationconditions() {
		Set<?> qaSet = instance.getTypedProperty(ProcessStepDefinitionType.CoreProperties.activationconditions.toString(), Set.class);
		if (qaSet != null) {
			return qaSet.stream()
					.map(inst -> context.getWrappedInstance(ConstraintSpec.class, (PPEInstance) inst))
					.map(obj -> (ConstraintSpec)obj)
					.collect(Collectors.toSet());
		} else return Collections.emptySet();
	}

//	@SuppressWarnings("unchecked")
//	@Deprecated(forRemoval = true)
//	public void setCondition(Conditions condition, String ruleAsString) {
//		ConstraintSpec constraint = getProcessContext().getFactoryIndex().getConstraintFactory().createInstance(condition, condition+"0", ruleAsString, ruleAsString, 0, false);
//		switch(condition) {
//		case ACTIVATION:
//			instance.getTypedProperty(ProcessStepDefinitionType.CoreProperties.activationconditions.toString(), Set.class).add(constraint.getInstance());
//			break;
//		case CANCELATION:
//			instance.getTypedProperty(ProcessStepDefinitionType.CoreProperties.cancelconditions.toString(), Set.class).add(constraint.getInstance());
//			break;
//		case POSTCONDITION:
//			instance.getTypedProperty(ProcessStepDefinitionType.CoreProperties.postconditions.toString(), Set.class).add(constraint.getInstance());
//			break;
//		case PRECONDITION:
//			instance.getTypedProperty(ProcessStepDefinitionType.CoreProperties.preconditions.toString(), Set.class).add(constraint.getInstance());
//			break;
//		default:
//			break;
//
//		}
//	}

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
					.map(inst -> context.getWrappedInstance(ConstraintSpec.class, (PPEInstance) inst))
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
		if (instance.getTypedProperty(ProcessStepDefinitionType.CoreProperties.outDND.toString(), PPEInstance.class) != null) {
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
		if (instance.getTypedProperty(ProcessStepDefinitionType.CoreProperties.inDND.toString(), PPEInstance.class) != null) {
			DecisionNodeDefinition priorDND = context.getWrappedInstance(DecisionNodeDefinition.class, instance.getTypedProperty(ProcessStepDefinitionType.CoreProperties.inDND.toString(), PPEInstance.class));			
			String msg = String.format("InDND already set to %s, Rewiring inDND of step %s to dnd %s not supported", priorDND.getName(), this.getName(), inDND.getName());
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

	public List<ProcessDefinitionError> checkConstraintValidity(PPEInstanceType processInstType) {
		List<ProcessDefinitionError> errors = new LinkedList<>();
		PPEInstanceType instType = this.context.getSchemaRegistry().getTypeByName(SpecificProcessStepType.getProcessStepName(this));
		
		//InstanceType instType = this.instance.getInstanceType(); //ProcessStep.getOrCreateDesignSpaceInstanceType(ws, this, processInstType);

		this.getActivationconditions().stream().forEach(spec -> checkConstraintExists(instType, spec, Conditions.ACTIVATION, errors));
		this.getCancelconditions().stream().forEach(spec -> checkConstraintExists(instType, spec, Conditions.CANCELATION, errors));
		this.getPostconditions().stream().forEach(spec -> checkConstraintExists(instType, spec, Conditions.POSTCONDITION, errors));
		this.getPreconditions().stream().forEach(spec -> checkConstraintExists(instType, spec, Conditions.PRECONDITION, errors));

		// we dont need to check InputToOutput rule for subprocesses as these should produce the output internally/below in their childsteps
		// hence dont check, unless there are no child steps.
		if (this instanceof ProcessDefinition && ((ProcessDefinition)this).getStepDefinitions().size() > 0) {
			log.debug("Skipping checking of Datamapping Rule for Subprocess Step: "+this.getName());
		} else {
			this.getInputToOutputMappingRules().entrySet().stream()
			.forEach(entry -> {
				String name = ProcessDefinitionFactory.getDataMappingId(entry, this);				
				RuleDefinition ruleType = context.getSchemaRegistry().getRuleByNameAndContext(name, instType);
				if (ruleType == null) 	{
					log.error("Expected Datamapping Rule for existing process not found: "+name);
					//status.put(name, "Corrupt data - Expected Datamapping Rule not found");
					errors.add(new ProcessDefinitionError(this, "Expected DataMapping Not Found - Internal Data Corruption", name, ProcessDefinitionError.Severity.ERROR));
				} else {
					RuleDefinition crt = (RuleDefinition)ruleType;
					if (crt.hasRuleError())
						errors.add(new ProcessDefinitionError(this, String.format("DataMapping %s has an error", name), crt.getRuleError(), ProcessDefinitionError.Severity.ERROR));
				}
			});
		}
		//qa constraints:
		ProcessDefinition pd = this.getProcess() !=null ? this.getProcess() : (ProcessDefinition)this;
		this.getQAConstraints().stream()
			.forEach(spec -> {
				String specId = ProcessDefinitionFactory.getQASpecId(spec, pd);
				RuleDefinition crt = context.getSchemaRegistry().getRuleByNameAndContext(specId, instType);//RuleDefinition.RuleDefinitionExists(ws,  specId, instType, spec.getQaConstraintSpec());
				if (crt == null) {
					log.error("Expected Rule for existing process not found: "+specId);
					errors.add(new ProcessDefinitionError(this, "Expected QA Constraint Not Found - Internal Data Corruption", specId, ProcessDefinitionError.Severity.ERROR));
				} else
					if (crt.hasRuleError())
						errors.add(new ProcessDefinitionError(this, String.format("QA Constraint %s has an error", specId), crt.getRuleError(), ProcessDefinitionError.Severity.ERROR));
			});
		return errors;
	}

	private void checkConstraintExists(PPEInstanceType instType, ConstraintSpec spec, Conditions condition, List<ProcessDefinitionError> errors) {
		String name = ProcessDefinitionFactory.getConstraintName(condition, spec.getOrderIndex(), instType);
		RuleDefinition crt = context.getSchemaRegistry().getRuleByNameAndContext(name, instType);
		if (crt == null) {
			log.error("Expected Rule for existing process not found: "+name);
			errors.add(new ProcessDefinitionError(this, "Expected Constraint Not Found - Internal Data Corruption", name, ProcessDefinitionError.Severity.ERROR));
		} else {
			if (crt.hasRuleError())
				errors.add(new ProcessDefinitionError(this, String.format("Condition % has an error", spec.getName()), crt.getRuleError(), ProcessDefinitionError.Severity.ERROR));
		}
	}

	public List<ProcessDefinitionError> checkStepStructureValidity() {
		List<ProcessDefinitionError> errors = new LinkedList<>();
		if (getPostconditions().isEmpty() && !this.getName().startsWith(NOOPSTEP_PREFIX)) {
			errors.add(new ProcessDefinitionError(this, "No Condition Defined", "Step needs at least one post-condition to signal when a step is considered finished.", ProcessDefinitionError.Severity.ERROR));
		}
		if (getExpectedInput().isEmpty() && !this.getName().startsWith(NOOPSTEP_PREFIX)) {
			errors.add(new ProcessDefinitionError(this, "No Input Defined", "Step needs at least one input.", ProcessDefinitionError.Severity.ERROR));
		}
		getExpectedInput().entrySet().stream().forEach(entry -> {
			if (entry.getValue() == null)
				errors.add(new ProcessDefinitionError(this, "Unavailable Type", "Artifact type of input '"+entry.getKey()+"' could not be resolved", ProcessDefinitionError.Severity.ERROR));
			});
		getExpectedOutput().entrySet().stream().forEach(entry -> {
			if (entry.getValue() == null)
				errors.add(new ProcessDefinitionError(this, "Unavailable Type", "Artifact type of output '"+entry.getKey()+"' could not be resolved", ProcessDefinitionError.Severity.ERROR));
			});
		getExpectedOutput().keySet().stream().forEach(out -> {
			if (!getInputToOutputMappingRules().containsKey(out))
				errors.add(new ProcessDefinitionError(this, "No Mapping Defined", "Step output '"+out+"' has no datamapping from input defined", ProcessDefinitionError.Severity.ERROR));
			});

		return errors;
	}

	private void deleteRuleIfExists(PPEInstanceType instType, ConstraintSpec spec, Conditions condition ) {
		String name = ProcessDefinitionFactory.getConstraintName(condition, spec.getOrderIndex(), instType);
		RuleDefinition crt = context.getSchemaRegistry().getRuleByNameAndContext(name, instType);
		if (crt != null) 
			crt.markAsDeleted();
	}

	@Override
	public void deleteCascading() {
		// wring instanceType: we need to get the dynamically generate Instance (the one that is used for the ProcessStep)
		String stepDefName = SpecificProcessStepType.getProcessStepName(this);
		PPEInstanceType instType = this.context.getSchemaRegistry().getTypeByName(stepDefName);
		if (instType != null) { 
			this.getActivationconditions().stream().forEach(spec -> deleteRuleIfExists(instType, spec, Conditions.ACTIVATION));
			this.getCancelconditions().stream().forEach(spec -> deleteRuleIfExists(instType, spec, Conditions.CANCELATION));
			this.getPostconditions().stream().forEach(spec -> deleteRuleIfExists(instType, spec, Conditions.POSTCONDITION));
			this.getPreconditions().stream().forEach(spec -> deleteRuleIfExists(instType, spec, Conditions.PRECONDITION));

			this.getInputToOutputMappingRules().entrySet().stream()
			.forEach(entry -> {
				String name = ProcessDefinitionFactory.getDataMappingId(entry, this);
				RuleDefinition crt = context.getSchemaRegistry().getRuleByNameAndContext(name, instType);
				if (crt != null) crt.markAsDeleted();
			});
			//delete qa constraints:
			ProcessDefinition pd = this.getProcess() !=null ? this.getProcess() : (ProcessDefinition)this;
			this.getQAConstraints().stream()
			.forEach(spec -> {
				String specId = ProcessDefinitionFactory.getQASpecId(spec, pd);
				RuleDefinition crt = context.getSchemaRegistry().getRuleByNameAndContext(specId, instType);
				if (crt != null) 
					crt.markAsDeleted();
				spec.deleteCascading();
			});
			instType.markAsDeleted();
		} // else // we never go around creating that step instance type probably due to errors in the definition
		super.deleteCascading();
	}


	



}

