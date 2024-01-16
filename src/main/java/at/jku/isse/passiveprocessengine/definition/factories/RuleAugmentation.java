package at.jku.isse.passiveprocessengine.definition.factories;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import at.jku.isse.passiveprocessengine.core.InstanceType;
import at.jku.isse.passiveprocessengine.core.RuleDefinition;
import at.jku.isse.passiveprocessengine.core.RuleDefinitionFactory;
import at.jku.isse.passiveprocessengine.definition.ProcessDefinitionError;
import at.jku.isse.passiveprocessengine.definition.activeobjects.ConstraintSpec;
import at.jku.isse.passiveprocessengine.definition.activeobjects.ProcessDefinition;
import at.jku.isse.passiveprocessengine.definition.activeobjects.StepDefinition;
import at.jku.isse.passiveprocessengine.designspace.RuleServiceWrapper;
import at.jku.isse.passiveprocessengine.instance.StepLifecycle.Conditions;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RuleAugmentation {

	public static final String RESERVED_PROPERTY_STEP_AUGMENTATION_STATUS = "@stepAugmentationStatus";

	private static final String INSTANCETYPE_PROPERTY_METADATA = "@propertyMetadata";
	
	private StepDefinition stepDef;
	private InstanceType stepType;
	
	private RuleDefinitionFactory ruleFactory;
	private RuleServiceWrapper ruleService;

	public RuleAugmentation(StepDefinition sd, InstanceType stepType, RuleDefinitionFactory ruleFactory, RuleServiceWrapper ruleService) {		
		this.stepDef = sd;
		this.stepType = stepType;
		this.ruleFactory = ruleFactory;
		this.ruleService = ruleService;
	}

	// This works for non-temporal constraints only
	public List<ProcessDefinitionError> augmentAndCreateConditions()  {
		List<ProcessDefinitionError> errors = new LinkedList<>();
		Map<String, String> propertyMetadata = stepType.getTypedProperty(INSTANCETYPE_PROPERTY_METADATA, Map.class);
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
					List<StepParameter> singleUsage = extractStepParameterUsageFromConstraint(stepDef, arl);
					arl = ruleService.rewriteConstraint(stepType, arl, singleUsage, stepDef);
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
					List<StepParameter> singleUsage = extractStepParameterUsageFromConstraint(stepDef, arl);
					arl = ruleService.rewriteConstraint(stepType, arl, singleUsage, stepDef);
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
					List<StepParameter> singleUsage = extractStepParameterUsageFromConstraint(stepDef, arl);
					arl = ruleService.rewriteConstraint(stepType, arl, singleUsage, stepDef);
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
					List<StepParameter> singleUsage = extractStepParameterUsageFromConstraint(stepDef, arl);
					arl = ruleService.rewriteConstraint(stepType, arl, singleUsage, stepDef);
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
						List<StepParameter> singleUsage = extractStepParameterUsageFromConstraint(stepDef, arl);
						arl = ruleService.rewriteConstraint(stepType, arl, singleUsage, stepDef);
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

		// returns all in/out parameters that are used in a constraint
		public static List<StepParameter> extractStepParameterUsageFromConstraint(StepDefinition step, String constraint) {
			List<StepParameter> usage = new LinkedList<>();
			usage.addAll(step.getExpectedInput().keySet().stream()
				.filter(param -> constraint.contains("self.in_"+param))
				.map(param -> new StepParameter(StepParameter.IO.IN, param))
				.collect(Collectors.toList()));
			usage.addAll(step.getExpectedOutput().keySet().stream()
					.filter(param -> constraint.contains("self.out_"+param))
					.map(param -> new StepParameter(StepParameter.IO.OUT, param))
					.collect(Collectors.toList()));
			return usage;
		}
		
		@Data
		public static class StepParameter {
			public enum IO {IN, OUT}
			private final IO io;
			private final String name;
		}

}
