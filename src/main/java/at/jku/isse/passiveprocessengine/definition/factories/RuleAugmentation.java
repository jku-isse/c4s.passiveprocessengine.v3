package at.jku.isse.passiveprocessengine.definition.factories;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import at.jku.isse.passiveprocessengine.analysis.PrematureTriggerGenerator;
import at.jku.isse.passiveprocessengine.analysis.PrematureTriggerGenerator.DataSource;
import at.jku.isse.passiveprocessengine.analysis.PrematureTriggerGenerator.DataSource.IoType;
import at.jku.isse.passiveprocessengine.analysis.PrematureTriggerGenerator.StepParameter;
import at.jku.isse.passiveprocessengine.core.InstanceType;
import at.jku.isse.passiveprocessengine.core.RuleDefinition;
import at.jku.isse.passiveprocessengine.core.RuleDefinitionFactory;
import at.jku.isse.passiveprocessengine.definition.ProcessDefinitionError;
import at.jku.isse.passiveprocessengine.definition.activeobjects.ConstraintSpec;
import at.jku.isse.passiveprocessengine.definition.activeobjects.ProcessDefinition;
import at.jku.isse.passiveprocessengine.definition.activeobjects.StepDefinition;
import at.jku.isse.passiveprocessengine.instance.StepLifecycle.Conditions;
import at.jku.isse.passiveprocessengine.instance.types.SpecificProcessStepType;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RuleAugmentation {

	public static final String RESERVED_PROPERTY_STEP_AUGMENTATION_STATUS = "@stepAugmentationStatus";

	private StepDefinition stepDef;
	private InstanceType stepType;
	private int varCount = 0;
	private RuleDefinitionFactory ruleFactory;

	public RuleAugmentation(StepDefinition sd, InstanceType stepType, RuleDefinitionFactory ruleFactory) {		
		this.stepDef = sd;
		this.stepType = stepType;
		this.ruleFactory = ruleFactory;
	}

	// This works for non-temporal constraints only
	public List<ProcessDefinitionError> augmentAndCreateConditions()  {
		List<ProcessDefinitionError> errors = new LinkedList<>();
		Map<String, String> propertyMetadata = stepType.getTypedProperty("INSTANCETYPE_PROPERTY_METADATA", Map.class);
		String augmentationStatus = propertyMetadata.get(RESERVED_PROPERTY_STEP_AUGMENTATION_STATUS);
		if (augmentationStatus != null) {
			log.debug("Skipping augmentation of already augmented step "+stepDef.getName());
			return errors; // then we already augmented this step
		}

		stepDef.getPreconditions().stream()
		.sorted(ConstraintSpec.COMPARATOR_BY_ORDERINDEX)
		.forEach(spec -> {
			String specId = ProcessDefinitionFactory.getConstraintName(Conditions.PRECONDITION, spec.getOrderIndex(), stepType);
			if (spec.getConstraintSpec() != null) {
				String arl = spec.getConstraintSpec();
				try {
					arl = rewriteConstraint(arl);
					log.debug(String.format("Augmented constraint %s for %s to %s", specId, stepDef.getName(), arl));
				} catch(Exception e) {
					errors.add(new ProcessDefinitionError(stepDef, String.format("Error aumenting Constraint %s : %s", specId, arl), e.getMessage()));
				}
				RuleDefinition crt =  ruleFactory.createInstance(stepType, specId, arl);
				//TODO check if that needs to be stored, references anywhere
			}
		});
		stepDef.getPostconditions().stream()
		.sorted(ConstraintSpec.COMPARATOR_BY_ORDERINDEX)
		.forEach(spec -> {
			String specId = ProcessDefinitionFactory.getConstraintName(Conditions.POSTCONDITION, spec.getOrderIndex(), stepType);
			if (spec.getConstraintSpec() != null) {
				String arl = spec.getConstraintSpec();
				try {
					arl = rewriteConstraint(arl);
					log.debug(String.format("Augmented constraint %s for %s to %s", specId, stepDef.getName(), arl));
				} catch(Exception e) {
					errors.add(new ProcessDefinitionError(stepDef, String.format("Error aumenting Constraint %s : %s", specId, arl), e.getMessage()));
				}
				RuleDefinition crt =  ruleFactory.createInstance( stepType, specId, arl);
			}
		});
		stepDef.getCancelconditions().stream()
		.sorted(ConstraintSpec.COMPARATOR_BY_ORDERINDEX)
		.forEach(spec -> {
			String specId = ProcessDefinitionFactory.getConstraintName(Conditions.CANCELATION, spec.getOrderIndex(), stepType);
			if (spec.getConstraintSpec() != null) {
				String arl = spec.getConstraintSpec();
				try {
					arl = rewriteConstraint(arl);
					log.debug(String.format("Augmented constraint %s for %s to %s", specId, stepDef.getName(), arl));
				} catch(Exception e) {
					errors.add(new ProcessDefinitionError(stepDef, String.format("Error aumenting Constraint %s : %s", specId, arl), e.getMessage()));
				}
				RuleDefinition crt =  ruleFactory.createInstance( stepType, specId, arl);
			}
		});
		stepDef.getActivationconditions().stream()
		.sorted(ConstraintSpec.COMPARATOR_BY_ORDERINDEX)
		.forEach(spec -> {
			String specId = ProcessDefinitionFactory.getConstraintName(Conditions.ACTIVATION, spec.getOrderIndex(), stepType);
			if (spec.getConstraintSpec() != null) {
				String arl = spec.getConstraintSpec();
				try {
					arl = rewriteConstraint(arl);
					log.debug(String.format("Augmented constraint %s for %s to %s", specId, stepDef.getName(), arl));
				} catch(Exception e) {
					errors.add(new ProcessDefinitionError(stepDef, String.format("Error aumenting Constraint %s : %s", specId, arl), e.getMessage()));
				}
				RuleDefinition crt =  ruleFactory.createInstance( stepType, specId, arl);
			}
		});

		//qa constraints:
		ProcessDefinition pd = stepDef.getProcess() !=null ? stepDef.getProcess() : (ProcessDefinition)stepDef;
		stepDef.getQAConstraints().stream()
			.sorted(ConstraintSpec.COMPARATOR_BY_ORDERINDEX)
			.forEach(spec -> {
				String specId = ProcessDefinitionFactory.getQASpecId(spec, pd);
				if (spec.getConstraintSpec() != null) {
					String arl = spec.getConstraintSpec();
					try {
						arl = rewriteConstraint(arl);
						log.debug(String.format("Augmented QA for %s to %s", stepDef.getName(), arl));
					} catch(Exception e) {
						errors.add(new ProcessDefinitionError(stepDef, String.format("Error aumenting QA Constraint %s : %s", spec.getConstraintId(), arl), e.getMessage()));
					}
					RuleDefinition crt =  ruleFactory.createInstance(stepType, specId, arl);
				}
			});

		if (errors.isEmpty())
			propertyMetadata.put(RESERVED_PROPERTY_STEP_AUGMENTATION_STATUS, "success");
		return errors;
	}

	private String rewriteConstraint(String constraint) throws Exception {
		// we recreate the constraint to ensure we have all the types in iterators available
		//TODO: catch any exceptions here
		ArlEvaluator ae = new ArlEvaluator(stepType, constraint);
		constraint = ae.syntaxTree.getOriginalARL(0, false);

		List<StepParameter> singleUsage = PrematureTriggerGenerator.extractStepParameterUsageFromConstraint(stepDef, constraint);
		// we need to obtain for every in and out param that we have a source for the location, and then replace from the back every this location with the path from the source
		// every param can only be at a unique set of position, not shared with any other param, hence location/position index can serve as key
		Map<Integer, StepParameter> loc2param = new HashMap<>();
		for (StepParameter param : singleUsage ) {
			if (param.getIo()==at.jku.isse.passiveprocessengine.analysis.PrematureTriggerGenerator.StepParameter.IO.IN) continue; // we only replace out parameters
			String extParam = "self.out_"+param.getName();
			int lastFound = 0;
			while (true) {
				lastFound = constraint.indexOf(extParam, lastFound);
				if (lastFound >= 0) {
					loc2param.put(lastFound, param);
					lastFound++;
				} else
					break;
			}
		}
		// now check which pos and thus param goes first for replacement.
		List<Integer> paramList = loc2param.keySet().stream().sorted().collect(Collectors.toList());
		Collections.reverse(paramList);
		for( int pos : paramList) {
			StepParameter param = loc2param.get(pos);

			String extParam = "self.out_"+param.getName();
			// create two strings: one before the param to be replaced, the rest after the param
			// and then replace the param by the path
			String pre = constraint.substring(0, pos);
			String post = constraint.substring(pos+extParam.length());
			try {
				String replacement = getFirstOccuranceOfOutParam(stepDef, param).getNavPath();
				constraint = pre + replacement + post;
			} catch(IllegalArgumentException ex) {
				constraint = pre + extParam + post; // i.e, no replacement
			}

		}
		// ensure the new constraint is correct
		ae = new ArlEvaluator(stepType, constraint);
		constraint = ae.syntaxTree.getOriginalARL(0, false);
		return constraint;
	}

	private String ensureUniqueVarNames(String query, InstanceType typeStep) {
		// we need to check in any NavPath that it doesnt contain a var name (e.g., in an iteration etc) that occurred before,
		// i.e., we need unique var names per constraint
		ArlEvaluator ae = new ArlEvaluator(typeStep, query);
		varCount++;
		ae.parser.currentEnvironment.locals.values().stream()
			.filter(var -> !((VariableExpression)var).name.equals("self"))
			.forEach(var -> ((VariableExpression)var).name = ((VariableExpression)var).name+"_"+varCount);
		String rewritten = ae.syntaxTree.getOriginalARL(0, false);
		return rewritten;
	}

	private DataSource getFirstOccuranceOfOutParam(StepDefinition step, StepParameter outParam) throws IllegalArgumentException{
		String mapping = step.getInputToOutputMappingRules().get(outParam.getName()); // we assume for now that the mapping name is equal to the out param name, (this will be guaranteed in the future)
		if (mapping != null) { // for now, we need to process the mapping constraints (will not be necessary once these are defines using derived properties)
			//InstanceType stepType = ProcessStep.getOrCreateDesignSpaceInstanceType(ws, step);
			ArlEvaluator ae = new ArlEvaluator(stepType, mapping);
			mapping = ae.syntaxTree.getOriginalARL(0, false);

			//int posSym = Math.max(mapping.indexOf("->symmetricDifference"), mapping.indexOf(".symmetricDifference")); //Symmetric difference is removed upon loading from DTOs and added extra upon creating mapping rules
			//if (posSym > 0) {
				String navPath = mapping;//.substring(0, posSym); // now lets find which in param this outparam depends on
				// we assume, only inparams are used in datamapping, i.e., we dont derive some output and then derive additional output from that!
				// and rewrite, then return
				String fullPath = ensureUniqueVarNames(navPath, stepType);
				DataSource thisDS = new DataSource(step, outParam.getName(), IoType.stepIn, fullPath);
				return thisDS;
			//}
		}
		// otherwise keep this outparam
		return new DataSource(step, outParam.getName(), IoType.stepOut, "self.out_"+outParam.getName());
	}

	// experiment with Temporal constraints
	private void augmentTemporalPrecondition() {
		Optional<String> preconditionOpt = stepDef.getCondition(Conditions.PRECONDITION);
		if ( preconditionOpt.isEmpty() || stepDef.getInDND().getInSteps().isEmpty())
			return;// there is no precondition or this is the first process step
		// for each prior step, if the completion status changes, then we reenable
		// rule is something like for an AND:
		// eventually(always( self.inDNI.inSteps->forAll(priorStep | priorStep.expectedLifecycleState='COMPLETED') -> eventually(EXISTINGPRECONDITION) ))
		// once the prior step is complete (or complete again) then the step is ready as soon as the EXISTINGPRECONDITION holds, and
		// then we dont care if it no longer holds once it has hold, as we assume that any change to the input is reflected also in a change of the prior step's post conditions
		// for an OR:
		// eventually(always( self.inDNI.inSteps->select(priorStep | priorStep.expectedLifecycleState='COMPLETED' and priorStep.actualLifecycleState='COMPLETED').size()>0 -> eventually(EXISTINGPRECONDITION) )
		//NOTE: this however will not result in failure as long at least one step is fulfilled, if one wants to signal that redoing a particular step should cause a rework of a subsequent step, then an OR is not suitable, but rather an AND is necessary
		// rule for an XOR:
		// eventually(always( self.inDNI.inSteps->select(priorStep | priorStep.expectedLifecycleState='COMPLETED' and priorStep.actualLifecycleState='COMPLETED').size()=1 -> eventually(EXISTINGPRECONDITION) )
		//TODO: check is a switch over between two branches always results in a violation of this constraint --> is DNI.isInflowFulfilled temporarily violated when this happens?

		//we can just simplify this by checking the inDNI isInflowFullfilled flag
		// eventually(always(self.inDNI.isInflowFulfilled=True -> eventually(EXISTINGPRECONDITION) ))

	}
}
