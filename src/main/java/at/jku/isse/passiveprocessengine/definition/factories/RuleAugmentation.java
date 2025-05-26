package at.jku.isse.passiveprocessengine.definition.factories;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import at.jku.isse.passiveprocessengine.rdfwrapper.RDFInstanceType;
import at.jku.isse.passiveprocessengine.rdfwrapper.metaschema.MetaElementFactory;
import at.jku.isse.passiveprocessengine.definition.ProcessDefinitionError;
import at.jku.isse.passiveprocessengine.definition.activeobjects.ConstraintSpec;
import at.jku.isse.passiveprocessengine.definition.activeobjects.ProcessDefinition;
import at.jku.isse.passiveprocessengine.definition.activeobjects.StepDefinition;
import at.jku.isse.passiveprocessengine.definition.types.ConstraintSpecTypeFactory;
import at.jku.isse.passiveprocessengine.instance.StepLifecycle.Conditions;
import at.jku.isse.passiveprocessengine.instance.types.SpecificProcessStepType;
import at.jku.isse.passiveprocessengine.rdfwrapper.rule.RuleDefinitionService;
import at.jku.isse.passiveprocessengine.rdfwrapper.rule.RuleEnabledResolver;
import at.jku.isse.passiveprocessengine.rules.RewriterFactory;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RuleAugmentation {

	private static final String ERROR_AUMENTING_CONSTRAINT_TEMPLATE = "Error aumenting Constraint %s : %s";

	private static final String AUGMENTED_CONSTRAINT_TEMPLATE = "Augmented constraint %s for %s to %s";

	public static final String RESERVED_PROPERTY_STEP_AUGMENTATION_STATUS = "@stepAugmentationStatus";

	//private static final String INSTANCETYPE_PROPERTY_METADATA = "propertyMetadata";
	
	private StepDefinition stepDef;
	private RDFInstanceType stepType;
	
	private RuleEnabledResolver ruleFactory;
	private RewriterFactory ruleRewriter;

	public RuleAugmentation(StepDefinition sd, RDFInstanceType stepType, RuleEnabledResolver ruleFactory, RewriterFactory ruleRewriter) {		
		this.stepDef = sd;
		this.stepType = stepType;
		this.ruleFactory = ruleFactory;
		this.ruleRewriter = ruleRewriter;
	}

	// This works for non-temporal constraints only
	public List<ProcessDefinitionError> augmentAndCreateConditions()  {
		List<ProcessDefinitionError> errors = new LinkedList<>();
		Map<String, String> propertyMetadata = stepType.getTypedProperty(MetaElementFactory.propertyMetadataPredicate_URI, Map.class);
		String augmentationStatus = propertyMetadata.get(RESERVED_PROPERTY_STEP_AUGMENTATION_STATUS);
		if (augmentationStatus != null) {
			log.debug("Skipping augmentation of already augmented step "+stepDef.getName());
			return errors; // then we already augmented this step
		}

		stepDef.getPreconditions().stream()
		.sorted(ConstraintSpec.COMPARATOR_BY_ORDERINDEX)
		.forEach(spec -> {
			createRuleDefinition(errors, spec);
		});
		stepDef.getPostconditions().stream()
		.sorted(ConstraintSpec.COMPARATOR_BY_ORDERINDEX)
		.forEach(spec -> {
			createRuleDefinition(errors, spec);
		});
		stepDef.getCancelconditions().stream()
		.sorted(ConstraintSpec.COMPARATOR_BY_ORDERINDEX)
		.forEach(spec -> {
			createRuleDefinition(errors, spec);
		});
		stepDef.getActivationconditions().stream()
		.sorted(ConstraintSpec.COMPARATOR_BY_ORDERINDEX)
		.forEach(spec -> {
			createRuleDefinition(errors, spec);
		});

		//qa constraints:
		stepDef.getQAConstraints().stream()
			.sorted(ConstraintSpec.COMPARATOR_BY_ORDERINDEX)
			.forEach(spec -> {
				createRuleDefinition(errors, spec);
			});
		
		// datamapping rules , i.e., derived properties 
		stepDef.getInputToOutputMappingRules().entrySet().stream().forEach(entry -> {
			var outPropName = SpecificProcessStepType.PREFIX_OUT+entry.getKey();
			var predicate = stepType.getPropertyType(outPropName).getProperty();
			var ruleId = SpecificProcessInstanceTypesFactory.getDerivedPropertyRuleURI(predicate.getURI());
			var derivingRule = ruleFactory.createDerivedPropertyRule(stepType, ruleId, entry.getValue(), predicate);
			stepDef.getDerivedOutputPropertyRules().add(derivingRule);	// so we have a reference for deleting it again
			if (derivingRule.getRuleDef().hasExpressionError()) {
				errors.add(new ProcessDefinitionError(stepDef, String.format("Error creating derived property rule %s for property %s", ruleId, outPropName), derivingRule.getRuleDef().getExpressionError(), ProcessDefinitionError.Severity.ERROR));
			}
		});

		if (errors.isEmpty())
			propertyMetadata.put(RESERVED_PROPERTY_STEP_AUGMENTATION_STATUS, "success");
		return errors;
	}

	private void createRuleDefinition(List<ProcessDefinitionError> errors, ConstraintSpec spec) {
		String specId = SpecificProcessInstanceTypesFactory.getRuleURI(spec);
		if (spec.getConstraintSpec() != null) {
			String arl = spec.getConstraintSpec();
			try {
				List<StepParameter> singleUsage = extractStepParameterUsageFromConstraint(stepDef, arl);
				arl = ruleRewriter.rewriteConstraint(stepType, arl, singleUsage, stepDef);
				log.debug(String.format(AUGMENTED_CONSTRAINT_TEMPLATE, specId, stepDef.getName(), arl));				
				spec.setAugmentedConstraintSpec(arl);
			} catch(Exception e) {
				errors.add(new ProcessDefinitionError(stepDef, String.format(ERROR_AUMENTING_CONSTRAINT_TEMPLATE, specId, arl), e.getMessage(), ProcessDefinitionError.Severity.ERROR));
			}
			var wrapper = ruleFactory.createInstance(stepType, specId, arl);
			spec.setSingleProperty(ConstraintSpecTypeFactory.CoreProperties.ruleType.toString(), wrapper);
		}
	}

	// experiment with Temporal constraints
//		private void augmentTemporalPrecondition() {
			//Optional<String> preconditionOpt = stepDef.getCondition(Conditions.PRECONDITION);
			//if ( preconditionOpt.isEmpty() || stepDef.getInDND().getInSteps().isEmpty())
			//	return;// there is no precondition or this is the first process step
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

//		}

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
