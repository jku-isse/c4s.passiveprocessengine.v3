package at.jku.isse.passiveprocessengine.definition.activeobjects;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.jena.ontapi.model.OntIndividual;

import at.jku.isse.passiveprocessengine.rdfwrapper.NodeToDomainResolver;
import at.jku.isse.passiveprocessengine.rdfwrapper.RDFInstanceType;
import at.jku.isse.passiveprocessengine.rdfwrapper.rule.RDFRuleDefinitionWrapper;
import at.jku.isse.passiveprocessengine.rdfwrapper.rule.RuleEnabledResolver;
import at.jku.isse.passiveprocessengine.definition.ProcessDefinitionError;
import at.jku.isse.passiveprocessengine.definition.factories.SpecificProcessInstanceTypesFactory;
import at.jku.isse.passiveprocessengine.definition.types.StepDefinitionTypeFactory;
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
		Map<String, RDFInstanceType> inMap = getTypedProperty(StepDefinitionTypeFactory.CoreProperties.expectedInput.toString(), Map.class);
		if (inMap != null) {
			return inMap;
		} else return Collections.emptyMap();
	}

	@SuppressWarnings("unchecked")
	public void addExpectedInput(@NonNull String paramName, @NonNull RDFInstanceType type) {
		getTypedProperty(StepDefinitionTypeFactory.CoreProperties.expectedInput.toString(), Map.class).put(paramName, type);
	}


	@SuppressWarnings("unchecked")
	public Map<String, RDFInstanceType> getExpectedOutput() {
		Map outMap = getTypedProperty(StepDefinitionTypeFactory.CoreProperties.expectedOutput.toString(), Map.class);
		if (outMap != null) {
			return outMap;
		} else return Collections.emptyMap();
	}

	@SuppressWarnings("unchecked")
	public void addExpectedOutput(@NonNull String paramName, @NonNull RDFInstanceType type) {
		getTypedProperty(StepDefinitionTypeFactory.CoreProperties.expectedOutput.toString(), Map.class).put(paramName, type);
	}

	@SuppressWarnings("unchecked")
	public Set<ConstraintSpec> getPreconditions() {
		return getTypedProperty(StepDefinitionTypeFactory.CoreProperties.preconditions.toString(), Set.class);
//		if (qaSet != null) {
//			return qaSet.stream()
//					.map(inst -> context.getWrappedInstance(ConstraintSpec.class, (RDFInstance) inst))
//					.map(obj -> (ConstraintSpec)obj)
//					.collect(Collectors.toSet());
//		} else return Collections.emptySet();
	}

	@SuppressWarnings("unchecked")
	public Set<ConstraintSpec> getPostconditions() {
		return getTypedProperty(StepDefinitionTypeFactory.CoreProperties.postconditions.toString(), Set.class);
//		if (qaSet != null) {
//			return qaSet.stream()
//					.map(inst -> context.getWrappedInstance(ConstraintSpec.class, (RDFInstance) inst))
//					.map(obj -> (ConstraintSpec)obj)
//					.collect(Collectors.toSet());
//		} else return Collections.emptySet();
	}

	@SuppressWarnings("unchecked")
	public Set<ConstraintSpec> getCancelconditions() {
		return getTypedProperty(StepDefinitionTypeFactory.CoreProperties.cancelconditions.toString(), Set.class);
//		if (qaSet != null) {
//			return qaSet.stream()
//					.map(inst -> context.getWrappedInstance(ConstraintSpec.class, (RDFInstance) inst))
//					.map(obj -> (ConstraintSpec)obj)
//					.collect(Collectors.toSet());
//		} else return Collections.emptySet();
	}

	@SuppressWarnings("unchecked")
	public Set<ConstraintSpec> getActivationconditions() {
		return getTypedProperty(StepDefinitionTypeFactory.CoreProperties.activationconditions.toString(), Set.class);
//		if (qaSet != null) {
//			return qaSet.stream()
//					.map(inst -> context.getWrappedInstance(ConstraintSpec.class, (RDFInstance) inst))
//					.map(obj -> (ConstraintSpec)obj)
//					.collect(Collectors.toSet());
//		} else return Collections.emptySet();
	}



	@SuppressWarnings("unchecked")
	public void addPrecondition(ConstraintSpec spec) {
		 getTypedProperty(StepDefinitionTypeFactory.CoreProperties.preconditions.toString(), Set.class).add(spec);
	}

	@SuppressWarnings("unchecked")
	public void addPostcondition(ConstraintSpec spec) {
		 getTypedProperty(StepDefinitionTypeFactory.CoreProperties.postconditions.toString(), Set.class).add(spec);
	}

	@SuppressWarnings("unchecked")
	public void addCancelcondition(ConstraintSpec spec) {
		 getTypedProperty(StepDefinitionTypeFactory.CoreProperties.cancelconditions.toString(), Set.class).add(spec);
	}

	@SuppressWarnings("unchecked")
	public void addActivationcondition(ConstraintSpec spec) {
		 getTypedProperty(StepDefinitionTypeFactory.CoreProperties.activationconditions.toString(), Set.class).add(spec);
	}

	@SuppressWarnings("unchecked")
	public Set<ConstraintSpec> getQAConstraints() {
		return getTypedProperty(StepDefinitionTypeFactory.CoreProperties.qaConstraints.toString(), Set.class);
//		if (qaSet != null ) {
//			return  qaSet.stream()
//					.map(inst -> context.getWrappedInstance(ConstraintSpec.class, (RDFInstance) inst))
//					.map(obj -> (ConstraintSpec)obj)
//					.collect(Collectors.toSet());
//		} else return Collections.emptySet();
	}

	@SuppressWarnings("unchecked")
	public void addQAConstraint(ConstraintSpec spec) {
		 getTypedProperty(StepDefinitionTypeFactory.CoreProperties.qaConstraints.toString(), Set.class).add(spec);
	}

	@SuppressWarnings("unchecked")
	public Map<String, String> getInputToOutputMappingRules() {
		Map<?,?> rawMap =  getTypedProperty(StepDefinitionTypeFactory.CoreProperties.ioMappingRules.toString(), Map.class);
		return (Map<String,String>) rawMap;
	}

	@SuppressWarnings("unchecked")
	public void addInputToOutputMappingRule(String ruleId, String rule) {
		 getTypedProperty(StepDefinitionTypeFactory.CoreProperties.ioMappingRules.toString(), Map.class).put(ruleId, rule);
	}

	public void setOutDND(DecisionNodeDefinition outDND) {
		// we assume for now, there is no need for rewiring, and we throw an exception if this should be the case
		if ( getTypedProperty(StepDefinitionTypeFactory.CoreProperties.outDND.toString(), String.class) != null) {
			String priorDND =   getTypedProperty(StepDefinitionTypeFactory.CoreProperties.outDND.toString(), String.class);		
			//if (!priorDND.equals(outDND.getName())) {
				String msg = String.format("OutDND already set to %s, Rewiring outDND of step %s to dnd %s not supported", priorDND, this.getName(), outDND.getName());
				log.error(msg);
				throw new RuntimeException(msg);
			//}
		}
		outDND.addInStep(this);
		 setSingleProperty(StepDefinitionTypeFactory.CoreProperties.outDND.toString(), outDND.getName());
	}


	public DecisionNodeDefinition getOutDND() {
		String id =  getTypedProperty(StepDefinitionTypeFactory.CoreProperties.outDND.toString(), String.class);
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
		if ( getTypedProperty(StepDefinitionTypeFactory.CoreProperties.inDND.toString(), String.class) != null) {
			String priorDND =   getTypedProperty(StepDefinitionTypeFactory.CoreProperties.inDND.toString(), String.class);			
			//if (!priorDND.equals(inDND.getName())) {
				String msg = String.format("InDND already set to %s, Rewiring inDND of step %s to dnd %s not supported", priorDND, this.getName(), inDND.getName());
				log.error(msg);
				throw new RuntimeException(msg);
			//}
		}
		inDND.addOutStep(this);
		 setSingleProperty(StepDefinitionTypeFactory.CoreProperties.inDND.toString(),inDND.getName());
	}


	public DecisionNodeDefinition getInDND() {
		String id =  getTypedProperty(StepDefinitionTypeFactory.CoreProperties.inDND.toString(), String.class);
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
		 setSingleProperty(StepDefinitionTypeFactory.CoreProperties.specOrderIndex.toString(), index);
	}

	public void setDepthIndexRecursive(int indexToSet) {
		 setSingleProperty(StepDefinitionTypeFactory.CoreProperties.stepHierarchyDepth.toString(), indexToSet);
		DecisionNodeDefinition dnd = this.getOutDND();
		if (dnd != null) { //avoid NPE on process without outDND
			int newIndex = (dnd.getInSteps().size() > 1) ? indexToSet - 1 : indexToSet; // if in branching, reduction of index, otherwise same index as just a sequence
			if (dnd.getDepthIndex() < newIndex) // this allows to override the index when this is used as a subprocess
				dnd.setDepthIndexRecursive(newIndex);
		}
	}

	public Integer getSpecOrderIndex() {
		return  getTypedProperty(StepDefinitionTypeFactory.CoreProperties.specOrderIndex.toString(), Integer.class, -1);
	}

	public Integer getDepthIndex() {
		return  getTypedProperty(StepDefinitionTypeFactory.CoreProperties.stepHierarchyDepth.toString(), Integer.class, -1);
	}

	public void setHtml_url(String html_url)
	{
		 setSingleProperty(StepDefinitionTypeFactory.CoreProperties.html_url.toString(), html_url);
	}

	public String getHtml_url()
	{
		return  getTypedProperty(StepDefinitionTypeFactory.CoreProperties.html_url.toString(), String.class, "");
	}
	public void setDescription(String des)
	{
		 setSingleProperty(StepDefinitionTypeFactory.CoreProperties.description.toString(), des);
	}

	public String getDescription()
	{
		return getTypedProperty(StepDefinitionTypeFactory.CoreProperties.description.toString(), String.class, "");
	}
	
	public Set<RDFRuleDefinitionWrapper> getDerivedOutputPropertyRules() {
		return getTypedProperty(StepDefinitionTypeFactory.CoreProperties.derivedPropertyRules.toString(), Set.class, Collections.emptySet());
	}

	public List<ProcessDefinitionError> checkConstraintValidity() {
		List<ProcessDefinitionError> errors = new LinkedList<>();
		
			this.getActivationconditions().stream().forEach(spec -> checkConstraintExists(spec, Conditions.ACTIVATION, errors));
			this.getCancelconditions().stream().forEach(spec -> checkConstraintExists(spec, Conditions.CANCELATION, errors));
			this.getPostconditions().stream().forEach(spec -> checkConstraintExists(spec, Conditions.POSTCONDITION, errors));
			this.getPreconditions().stream().forEach(spec -> checkConstraintExists(spec, Conditions.PRECONDITION, errors));
		
		// we dont need to check InputToOutput rule for subprocesses as these should produce the output internally/below in their childsteps
		// hence dont check, unless there are no child steps.
		if (this instanceof ProcessDefinition processDefinition && !processDefinition.getStepDefinitions().isEmpty()) {
			log.debug("Skipping checking of Datamapping Rule for Subprocess Step: "+this.getName());
		} else {
			this.getInputToOutputMappingRules().entrySet().stream()
			.forEach(entry -> {
				var outPropName = SpecificProcessStepType.PREFIX_OUT+entry.getKey();
				var thisAsType = resolver.findNonDeletedInstanceTypeByFQN(this.getId()).get();
				var predicate = thisAsType.getPropertyType(outPropName);
				if (predicate == null) {
					log.error("Expected Out Property not found: "+outPropName+" in process step "+this.getName());
					//status.put(name, "Corrupt data - Expected Datamapping Rule not found");
					errors.add(new ProcessDefinitionError(this, "Expected Out Property not found", outPropName, ProcessDefinitionError.Severity.ERROR));
				} else {
				
				var ruleId = SpecificProcessInstanceTypesFactory.getDerivedPropertyRuleURI(predicate.getProperty().getURI());				
				var foundOpt = this.getDerivedOutputPropertyRules().stream()
							.filter(rule -> rule.getRuleDef().getRuleDefinition().getURI().equals(ruleId))
							.findAny();
				if (foundOpt.isEmpty()) 	{
					log.error("Expected Datamapping Rule for existing process not found: "+ruleId);
					//status.put(name, "Corrupt data - Expected Datamapping Rule not found");
					errors.add(new ProcessDefinitionError(this, "Expected DataMapping Not Found - Internal Data Corruption", ruleId, ProcessDefinitionError.Severity.ERROR));
				} else {
					if (foundOpt.get().getRuleDef().hasExpressionError())
						errors.add(new ProcessDefinitionError(this, String.format("DataMapping %s has an error", ruleId), foundOpt.get().getRuleDef().getExpressionError(), ProcessDefinitionError.Severity.ERROR));
				}
				}
			});
		}
		//qa constraints:
		//ProcessDefinition pd = this.getProcess() !=null ? this.getProcess() : (ProcessDefinition)this;
		this.getQAConstraints().stream()
			.forEach(spec -> checkConstraintExists(spec, Conditions.PRECONDITION, errors));
		return errors;
	}

	private void checkConstraintExists(ConstraintSpec spec, Conditions condition, List<ProcessDefinitionError> errors) {
//		String name = SpecificProcessInstanceTypesFactory.getConstraintName(condition, spec.getOrderIndex(), instType);
//		RDFRuleDefinitionWrapper crt = ((RuleEnabledResolver)resolver).getRuleByNameAndContext(name, instType);
		var crt = spec.getRuleDefinition();
		if (crt == null) {
			log.error("Expected Rule for existing process not found: "+spec.getId());
			errors.add(new ProcessDefinitionError(this, "Expected Constraint Not Found - Internal Data Corruption", spec.getId(), ProcessDefinitionError.Severity.ERROR));
		} else {
			if (crt.getRuleDef().hasExpressionError())
				errors.add(new ProcessDefinitionError(this, String.format("Condition %s has an error", spec.getName()), crt.getRuleDef().getExpressionError(), ProcessDefinitionError.Severity.ERROR));
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
//		String name = SpecificProcessInstanceTypesFactory.getConstraintName(condition, spec.getOrderIndex(), instType);
//		RDFRuleDefinitionWrapper crt = ((RuleEnabledResolver)resolver).getRuleByNameAndContext(name, instType);
		var crt = spec.getRuleDefinition();
		if (crt != null) 
			crt.delete();
	}

	@Override
	public void deleteCascading() {
		// wring instanceType: we need to get the dynamically generate InstanceType (the one that is used for the ProcessStep)
	//	String stepDefName = SpecificProcessStepType.getProcessStepName(this);
		var instTypeOpt = this.resolver.findNonDeletedInstanceTypeByFQN(this.getId());
		instTypeOpt.ifPresent(instType -> { 
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
			// deleted derived property rules
			this.getDerivedOutputPropertyRules().stream().forEach(def -> def.delete());	
			//delete qa constraints:
			ProcessDefinition pd = this.getProcess() !=null ? this.getProcess() : (ProcessDefinition)this;
			this.getQAConstraints().stream()
			.forEach(spec -> { deleteRuleIfExists(instType, spec, Conditions.PRECONDITION); //delete the rule 
				spec.deleteCascading(); // delete the rule spec!
			});
			instType.delete();
		}); // else // we never go around creating that step instance type probably due to errors in the definition
		super.deleteCascading();
	}


	@Override
	public String toString() {
		return "StepDefinition [getExpectedInput()=" + getExpectedInput() + ", getExpectedOutput()="
				+ getExpectedOutput() + ", getPreconditions()=" + getPreconditions() + ", getPostconditions()="
				+ getPostconditions() + ", getCancelconditions()=" + getCancelconditions()
				+ ", getActivationconditions()=" + getActivationconditions() + ", getQAConstraints()="
				+ getQAConstraints() + ", getInputToOutputMappingRules()=" + getInputToOutputMappingRules()
				+ ", getDerivedOutputPropertyRules()=" + getDerivedOutputPropertyRules() + "]";
	}




}

