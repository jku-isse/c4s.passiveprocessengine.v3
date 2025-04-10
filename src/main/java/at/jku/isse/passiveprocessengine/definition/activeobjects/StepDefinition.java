package at.jku.isse.passiveprocessengine.definition.activeobjects;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.jena.ontapi.model.OntIndividual;

import at.jku.isse.designspace.rule.arl.evaluator.RuleDefinition;
import at.jku.isse.passiveprocessengine.rdfwrapper.NodeToDomainResolver;
import at.jku.isse.passiveprocessengine.rdfwrapper.RDFInstance;
import at.jku.isse.passiveprocessengine.rdfwrapper.RDFInstanceType;
import at.jku.isse.passiveprocessengine.core.ProcessContext;
import at.jku.isse.passiveprocessengine.definition.IStepDefinition;
import at.jku.isse.passiveprocessengine.definition.ProcessDefinitionError;
import at.jku.isse.passiveprocessengine.definition.factories.ProcessDefinitionFactory;
import at.jku.isse.passiveprocessengine.definition.types.ProcessStepDefinitionType;
import at.jku.isse.passiveprocessengine.instance.StepLifecycle.Conditions;
import at.jku.isse.passiveprocessengine.instance.types.SpecificProcessStepType;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class StepDefinition extends ProcessDefinitionScopedElement {

	public static final String NOOPSTEP_PREFIX = "NoOpStep";

	public StepDefinition(@NonNull OntIndividual element, RDFInstanceType type, @NonNull NodeToDomainResolver resolver) {
		super(element, type, resolver);
	}


	public Map<String, RDFInstanceType> getExpectedInput() {
		@SuppressWarnings("unchecked")
		Map<String, RDFInstanceType> inMap = getTypedProperty(ProcessStepDefinitionType.CoreProperties.expectedInput.toString(), Map.class);
		if (inMap != null) {
			return inMap;
		} else return Collections.emptyMap();
	}

	@SuppressWarnings("unchecked")
	public void addExpectedInput(@NonNull String paramName, @NonNull RDFInstanceType type) {
		getTypedProperty(ProcessStepDefinitionType.CoreProperties.expectedInput.toString(), Map.class).put(paramName, type);
	}


	@SuppressWarnings("unchecked")
	public Map<String, RDFInstanceType> getExpectedOutput() {
		Map outMap = getTypedProperty(ProcessStepDefinitionType.CoreProperties.expectedOutput.toString(), Map.class);
		if (outMap != null) {
			return outMap;
		} else return Collections.emptyMap();
	}

	@SuppressWarnings("unchecked")
	public void addExpectedOutput(@NonNull String paramName, @NonNull RDFInstanceType type) {
		getTypedProperty(ProcessStepDefinitionType.CoreProperties.expectedOutput.toString(), Map.class).put(paramName, type);
	}

	@SuppressWarnings("unchecked")
	public Set<ConstraintSpec> getPreconditions() {
		return getTypedProperty(ProcessStepDefinitionType.CoreProperties.preconditions.toString(), Set.class);
//		if (qaSet != null) {
//			return qaSet.stream()
//					.map(inst -> context.getWrappedInstance(ConstraintSpec.class, (RDFInstance) inst))
//					.map(obj -> (ConstraintSpec)obj)
//					.collect(Collectors.toSet());
//		} else return Collections.emptySet();
	}

	@SuppressWarnings("unchecked")
	public Set<ConstraintSpec> getPostconditions() {
		return getTypedProperty(ProcessStepDefinitionType.CoreProperties.postconditions.toString(), Set.class);
//		if (qaSet != null) {
//			return qaSet.stream()
//					.map(inst -> context.getWrappedInstance(ConstraintSpec.class, (RDFInstance) inst))
//					.map(obj -> (ConstraintSpec)obj)
//					.collect(Collectors.toSet());
//		} else return Collections.emptySet();
	}

	@SuppressWarnings("unchecked")
	public Set<ConstraintSpec> getCancelconditions() {
		return getTypedProperty(ProcessStepDefinitionType.CoreProperties.cancelconditions.toString(), Set.class);
//		if (qaSet != null) {
//			return qaSet.stream()
//					.map(inst -> context.getWrappedInstance(ConstraintSpec.class, (RDFInstance) inst))
//					.map(obj -> (ConstraintSpec)obj)
//					.collect(Collectors.toSet());
//		} else return Collections.emptySet();
	}

	@SuppressWarnings("unchecked")
	public Set<ConstraintSpec> getActivationconditions() {
		return getTypedProperty(ProcessStepDefinitionType.CoreProperties.activationconditions.toString(), Set.class);
//		if (qaSet != null) {
//			return qaSet.stream()
//					.map(inst -> context.getWrappedInstance(ConstraintSpec.class, (RDFInstance) inst))
//					.map(obj -> (ConstraintSpec)obj)
//					.collect(Collectors.toSet());
//		} else return Collections.emptySet();
	}

//	@SuppressWarnings("unchecked")
//	@Deprecated(forRemoval = true)
//	public void setCondition(Conditions condition, String ruleAsString) {
//		ConstraintSpec constraint = getProcessContext().getFactoryIndex().getConstraintFactory().createInstance(condition, condition+"0", ruleAsString, ruleAsString, 0, false);
//		switch(condition) {
//		case ACTIVATION:
//			 getTypedProperty(ProcessStepDefinitionType.CoreProperties.activationconditions.toString(), Set.class).add(constraint.getInstance());
//			break;
//		case CANCELATION:
//			 getTypedProperty(ProcessStepDefinitionType.CoreProperties.cancelconditions.toString(), Set.class).add(constraint.getInstance());
//			break;
//		case POSTCONDITION:
//			 getTypedProperty(ProcessStepDefinitionType.CoreProperties.postconditions.toString(), Set.class).add(constraint.getInstance());
//			break;
//		case PRECONDITION:
//			 getTypedProperty(ProcessStepDefinitionType.CoreProperties.preconditions.toString(), Set.class).add(constraint.getInstance());
//			break;
//		default:
//			break;
//
//		}
//	}

	@SuppressWarnings("unchecked")
	public void addPrecondition(ConstraintSpec spec) {
		 getTypedProperty(ProcessStepDefinitionType.CoreProperties.preconditions.toString(), Set.class).add(spec.getInstance());
	}

	@SuppressWarnings("unchecked")
	public void addPostcondition(ConstraintSpec spec) {
		 getTypedProperty(ProcessStepDefinitionType.CoreProperties.postconditions.toString(), Set.class).add(spec.getInstance());
	}

	@SuppressWarnings("unchecked")
	public void addCancelcondition(ConstraintSpec spec) {
		 getTypedProperty(ProcessStepDefinitionType.CoreProperties.cancelconditions.toString(), Set.class).add(spec.getInstance());
	}

	@SuppressWarnings("unchecked")
	public void addActivationcondition(ConstraintSpec spec) {
		 getTypedProperty(ProcessStepDefinitionType.CoreProperties.activationconditions.toString(), Set.class).add(spec.getInstance());
	}

	@SuppressWarnings("unchecked")
	public Set<ConstraintSpec> getQAConstraints() {
		return getTypedProperty(ProcessStepDefinitionType.CoreProperties.qaConstraints.toString(), Set.class);
//		if (qaSet != null ) {
//			return  qaSet.stream()
//					.map(inst -> context.getWrappedInstance(ConstraintSpec.class, (RDFInstance) inst))
//					.map(obj -> (ConstraintSpec)obj)
//					.collect(Collectors.toSet());
//		} else return Collections.emptySet();
	}

	@SuppressWarnings("unchecked")
	public void addQAConstraint(ConstraintSpec spec) {
		 getTypedProperty(ProcessStepDefinitionType.CoreProperties.qaConstraints.toString(), Set.class).add(spec.getInstance());
	}

	@SuppressWarnings("unchecked")
	public Map<String, String> getInputToOutputMappingRules() {
		Map<?,?> rawMap =  getTypedProperty(ProcessStepDefinitionType.CoreProperties.ioMappingRules.toString(), Map.class);
		return (Map<String,String>) rawMap;
	}

	@SuppressWarnings("unchecked")
	public void addInputToOutputMappingRule(String ruleId, String rule) {
		 getTypedProperty(ProcessStepDefinitionType.CoreProperties.ioMappingRules.toString(), Map.class).put(ruleId, rule);
	}

	public void setOutDND(DecisionNodeDefinition outDND) {
		// we assume for now, there is no need for rewiring, and we throw an exception if this should be the case
		if ( getTypedProperty(ProcessStepDefinitionType.CoreProperties.outDND.toString(), String.class) != null) {
			String priorDND =   getTypedProperty(ProcessStepDefinitionType.CoreProperties.outDND.toString(), String.class);		
			//if (!priorDND.equals(outDND.getName())) {
				String msg = String.format("OutDND already set to %s, Rewiring outDND of step %s to dnd %s not supported", priorDND, this.getName(), outDND.getName());
				log.error(msg);
				throw new RuntimeException(msg);
			//}
		}
		outDND.addInStep(this);
		 setSingleProperty(ProcessStepDefinitionType.CoreProperties.outDND.toString(), outDND.getName());
	}


	public DecisionNodeDefinition getOutDND() {
		String id =  getTypedProperty(ProcessStepDefinitionType.CoreProperties.outDND.toString(), String.class);
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
		if ( getTypedProperty(ProcessStepDefinitionType.CoreProperties.inDND.toString(), String.class) != null) {
			String priorDND =   getTypedProperty(ProcessStepDefinitionType.CoreProperties.inDND.toString(), String.class);			
			//if (!priorDND.equals(inDND.getName())) {
				String msg = String.format("InDND already set to %s, Rewiring inDND of step %s to dnd %s not supported", priorDND, this.getName(), inDND.getName());
				log.error(msg);
				throw new RuntimeException(msg);
			//}
		}
		inDND.addOutStep(this);
		 setSingleProperty(ProcessStepDefinitionType.CoreProperties.inDND.toString(),inDND.getName());
	}


	public DecisionNodeDefinition getInDND() {
		String id =  getTypedProperty(ProcessStepDefinitionType.CoreProperties.inDND.toString(), String.class);
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
		 setSingleProperty(ProcessStepDefinitionType.CoreProperties.specOrderIndex.toString(), index);
	}

	public void setDepthIndexRecursive(int indexToSet) {
		 setSingleProperty(ProcessStepDefinitionType.CoreProperties.stepHierarchyDepth.toString(), indexToSet);
		DecisionNodeDefinition dnd = this.getOutDND();
		if (dnd != null) { //avoid NPE on process without outDND
			int newIndex = (dnd.getInSteps().size() > 1) ? indexToSet - 1 : indexToSet; // if in branching, reduction of index, otherwise same index as just a sequence
			if (dnd.getDepthIndex() < newIndex) // this allows to override the index when this is used as a subprocess
				dnd.setDepthIndexRecursive(newIndex);
		}
	}

	public Integer getSpecOrderIndex() {
		return  getTypedProperty(ProcessStepDefinitionType.CoreProperties.specOrderIndex.toString(), Integer.class, -1);
	}

	public Integer getDepthIndex() {
		return  getTypedProperty(ProcessStepDefinitionType.CoreProperties.stepHierarchyDepth.toString(), Integer.class, -1);
	}

	public void setHtml_url(String html_url)
	{
		 setSingleProperty(ProcessStepDefinitionType.CoreProperties.html_url.toString(), html_url);
	}

	public String getHtml_url()
	{
		return  getTypedProperty(ProcessStepDefinitionType.CoreProperties.html_url.toString(), String.class, "");
	}
	public void setDescription(String des)
	{
		 setSingleProperty(ProcessStepDefinitionType.CoreProperties.description.toString(), des);
	}

	public String getDescription()
	{
		return (String)  getTypedProperty(ProcessStepDefinitionType.CoreProperties.description.toString(), String.class, "");
	}

	public List<ProcessDefinitionError> checkConstraintValidity(RDFInstanceType processInstType) {
		List<ProcessDefinitionError> errors = new LinkedList<>();
		RDFInstanceType instType = this.context.getSchemaRegistry().getTypeByName(SpecificProcessStepType.getProcessStepName(this));
		
		//InstanceType instType = this. getInstanceType(); //ProcessStep.getOrCreateDesignSpaceInstanceType(ws, this, processInstType);

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

	private void checkConstraintExists(RDFInstanceType instType, ConstraintSpec spec, Conditions condition, List<ProcessDefinitionError> errors) {
		String name = ProcessDefinitionFactory.getConstraintName(condition, spec.getOrderIndex(), instType);
		RuleDefinition crt = context.getSchemaRegistry().getRuleByNameAndContext(name, instType);
		if (crt == null) {
			log.error("Expected Rule for existing process not found: "+name);
			errors.add(new ProcessDefinitionError(this, "Expected Constraint Not Found - Internal Data Corruption", name, ProcessDefinitionError.Severity.ERROR));
		} else {
			if (crt.hasRuleError())
				errors.add(new ProcessDefinitionError(this, String.format("Condition %s has an error", spec.getName()), crt.getRuleError(), ProcessDefinitionError.Severity.ERROR));
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

	private void deleteRuleIfExists(RDFInstanceType instType, ConstraintSpec spec, Conditions condition ) {
		String name = ProcessDefinitionFactory.getConstraintName(condition, spec.getOrderIndex(), instType);
		RuleDefinition crt = context.getSchemaRegistry().getRuleByNameAndContext(name, instType);
		if (crt != null) 
			crt.delete();
	}

	public void deleteCascading() {
		// wring instanceType: we need to get the dynamically generate InstanceType (the one that is used for the ProcessStep)
		String stepDefName = SpecificProcessStepType.getProcessStepName(this);
		RDFInstanceType instType = this.context.getSchemaRegistry().getTypeByName(stepDefName);
		if (instType != null) { 
			this.getActivationconditions().stream().forEach(spec -> { deleteRuleIfExists(instType, spec, Conditions.ACTIVATION); //delete the rule 
				spec.deleteCascading(); // delete the rule spec!
			});
			this.getCancelconditions().stream().forEach(spec -> { deleteRuleIfExists(instType, spec, Conditions.CANCELATION); //delete the rule 
				spec.deleteCascading(); // delete the rule spec!
			});
			this.getPostconditions().stream().forEach(spec -> { deleteRuleIfExists(instType, spec, Conditions.POSTCONDITION); //delete the rule 
				spec.deleteCascading(); // delete the rule spec!
			});
			this.getPreconditions().stream().forEach(spec -> { deleteRuleIfExists(instType, spec, Conditions.PRECONDITION); //delete the rule 
				spec.deleteCascading(); // delete the rule spec!
			});

			this.getInputToOutputMappingRules().entrySet().stream()
			.forEach(entry -> {
				String name = ProcessDefinitionFactory.getDataMappingId(entry, this);
				RuleDefinition crt = context.getSchemaRegistry().getRuleByNameAndContext(name, instType);
				if (crt != null) crt.delete();
			});
			//delete qa constraints:
			ProcessDefinition pd = this.getProcess() !=null ? this.getProcess() : (ProcessDefinition)this;
			this.getQAConstraints().stream()
			.forEach(spec -> {
				String specId = ProcessDefinitionFactory.getQASpecId(spec, pd);
				RuleDefinition crt = context.getSchemaRegistry().getRuleByNameAndContext(specId, instType);
				if (crt != null) 
					crt.delete();
				spec.deleteCascading();
			});
			instType.delete();
		} // else // we never go around creating that step instance type probably due to errors in the definition
		super.deleteCascading();
	}


	



}

