package at.jku.isse.passiveprocessengine.analysis;

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;

import at.jku.isse.designspace.core.model.InstanceType;
import at.jku.isse.designspace.core.model.MapProperty;
import at.jku.isse.designspace.core.model.ReservedNames;
import at.jku.isse.designspace.core.model.Workspace;
import at.jku.isse.designspace.rule.arl.expressions.VariableExpression;
import at.jku.isse.designspace.rule.checker.ArlEvaluator;
import at.jku.isse.designspace.rule.model.ConsistencyRuleType;
import at.jku.isse.passiveprocessengine.analysis.PrematureTriggerGenerator.DataSource;
import at.jku.isse.passiveprocessengine.analysis.PrematureTriggerGenerator.DataSource.IoType;
import at.jku.isse.passiveprocessengine.analysis.PrematureTriggerGenerator.StepParameter;
import at.jku.isse.passiveprocessengine.definition.ConstraintSpec;
import at.jku.isse.passiveprocessengine.definition.ProcessDefinition;
import at.jku.isse.passiveprocessengine.definition.ProcessDefinitionError;
import at.jku.isse.passiveprocessengine.definition.StepDefinition;
import at.jku.isse.passiveprocessengine.instance.ProcessStep;
import at.jku.isse.passiveprocessengine.instance.StepLifecycle.Conditions;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RuleAugmentation {

	public static final String RESERVED_PROPERTY_STEP_AUGMENTATION_STATUS = "@stepAugmentationStatus";
	
	private Workspace ws;
	private StepDefinition sd;
	private InstanceType stepType;
	private int varCount = 0;
	
	public RuleAugmentation(Workspace ws, StepDefinition sd, InstanceType stepType) {
		this.ws = ws;
		this.sd = sd;
		this.stepType = stepType;
	}
	
	public static Comparator<ConstraintSpec> CONSTRAINTCOMPARATOR = new Comparator<ConstraintSpec>() {
		@Override
		public int compare(ConstraintSpec o1, ConstraintSpec o2) {					
			return o1.getOrderIndex().compareTo(o2.getOrderIndex()) ;
		}};
	
	// This works for non-temporal constraints only
	public List<ProcessDefinitionError> augmentAndCreateConditions()  {
		List<ProcessDefinitionError> errors = new LinkedList<>();
		MapProperty<String> propertyMetadata = stepType.getPropertyAsMap(ReservedNames.INSTANCETYPE_PROPERTY_METADATA);
		String augmentationStatus = propertyMetadata.get(RESERVED_PROPERTY_STEP_AUGMENTATION_STATUS);
		if (augmentationStatus != null) {
			log.debug("Skipping augmentation of already augmented step "+sd.getName());
			return errors; // then we already augmented this step
		}
		
		sd.getPreconditions().stream()
		.sorted(CONSTRAINTCOMPARATOR)
		.forEach(spec -> {
			String specId = getConstraintName(Conditions.PRECONDITION, spec.getOrderIndex(), stepType);
			if (spec.getConstraintSpec() != null) {
				String arl = spec.getConstraintSpec();
				try {  
					arl = rewriteConstraint(arl);
					log.debug(String.format("Augmented constraint %s for %s to %s", specId, sd.getName(), arl));
				} catch(Exception e) {
					errors.add(new ProcessDefinitionError(sd, String.format("Error aumenting Constraint %s : %s", specId, arl), e.getMessage()));
				}
				ConsistencyRuleType crt = ConsistencyRuleType.create(ws, stepType, specId, arl);
				//TODO check if that needs to be stored, references anywhere
			}
		});
		sd.getPostconditions().stream()
		.sorted(CONSTRAINTCOMPARATOR)
		.forEach(spec -> {
			String specId = getConstraintName(Conditions.POSTCONDITION, spec.getOrderIndex(), stepType);
			if (spec.getConstraintSpec() != null) {
				String arl = spec.getConstraintSpec();
				try {  
					arl = rewriteConstraint(arl);
					log.debug(String.format("Augmented constraint %s for %s to %s", specId, sd.getName(), arl));
				} catch(Exception e) {
					errors.add(new ProcessDefinitionError(sd, String.format("Error aumenting Constraint %s : %s", specId, arl), e.getMessage()));
				}
				ConsistencyRuleType crt = ConsistencyRuleType.create(ws, stepType, specId, arl);
				//TODO check if that needs to be stored, references anywhere
			}
		});
		sd.getCancelconditions().stream()
		.sorted(CONSTRAINTCOMPARATOR)
		.forEach(spec -> {
			String specId = getConstraintName(Conditions.CANCELATION, spec.getOrderIndex(), stepType);
			if (spec.getConstraintSpec() != null) {
				String arl = spec.getConstraintSpec();
				try {  
					arl = rewriteConstraint(arl);
					log.debug(String.format("Augmented constraint %s for %s to %s", specId, sd.getName(), arl));
				} catch(Exception e) {
					errors.add(new ProcessDefinitionError(sd, String.format("Error aumenting Constraint %s : %s", specId, arl), e.getMessage()));
				}
				ConsistencyRuleType crt = ConsistencyRuleType.create(ws, stepType, specId, arl);
				//TODO check if that needs to be stored, references anywhere
			}
		});
		sd.getActivationconditions().stream()
		.sorted(CONSTRAINTCOMPARATOR)
		.forEach(spec -> {
			String specId = getConstraintName(Conditions.ACTIVATION, spec.getOrderIndex(), stepType);
			if (spec.getConstraintSpec() != null) {
				String arl = spec.getConstraintSpec();
				try {  
					arl = rewriteConstraint(arl);
					log.debug(String.format("Augmented constraint %s for %s to %s", specId, sd.getName(), arl));
				} catch(Exception e) {
					errors.add(new ProcessDefinitionError(sd, String.format("Error aumenting Constraint %s : %s", specId, arl), e.getMessage()));
				}
				ConsistencyRuleType crt = ConsistencyRuleType.create(ws, stepType, specId, arl);
				//TODO check if that needs to be stored, references anywhere
			}
		});
						
		//old constraints support
//		for (Conditions condition : Conditions.values()) {
//			if (sd.getCondition(condition).isPresent()) {
//				if (sd.getCondition(condition).get() != null) {
//					//with subprocesses we might have run augmentation before, thus check and skip				
//					if (stepType.getPropertyType(condition.toString()) != null)//
//						break;					
//					// as the output cannot be changed directly
//					// repairing the output makes no sense, but rather its datamapping definition, so we replace all out_xxx occurences with the datamapping definition
//					String arl = sd.getCondition(condition).get();
//					try {
//						arl = rewriteConstraint(arl);
//						log.debug(String.format("Augmented %s for %s to %s", condition, sd.getName(), arl));					
//					// if the error is from augmentation, still keep the original rule (with perhaps not as useful repairs)
//					// if the original rule has error, then this will be captured later anyway.
//					ConsistencyRuleType crd = ConsistencyRuleType.create(ws, stepType, getConstraintName(condition, stepType), arl);
//					// not evaluated yet here, assert ConsistencyUtils.crdValid(crd);																					
//					 stepType.createPropertyType(condition.toString(), Cardinality.SINGLE, crd);
//					} catch(Exception e) {
//						errors.add(new ProcessDefinitionError(sd, String.format("Error aumenting condition %s : %s", condition, arl), e.getMessage()));
//						//pex.getErrorMessages().add(String.format("Error aumenting %s : %s", arl, e.getMessage()));
//					}
//				}
//			}	
//		}
								
		//qa constraints:
		ProcessDefinition pd = sd.getProcess() !=null ? sd.getProcess() : (ProcessDefinition)sd;
		sd.getQAConstraints().stream()
			.sorted(CONSTRAINTCOMPARATOR)
			.forEach(spec -> {
				String specId = ProcessStep.getQASpecId(spec, pd);
				if (spec.getConstraintSpec() != null) {
					String arl = spec.getConstraintSpec();
					try {  
						arl = rewriteConstraint(arl);
						log.debug(String.format("Augmented QA for %s to %s", sd.getName(), arl));
					} catch(Exception e) {
						errors.add(new ProcessDefinitionError(sd, String.format("Error aumenting QA Constraint %s : %s", spec.getConstraintId(), arl), e.getMessage()));
					}
					ConsistencyRuleType crt = ConsistencyRuleType.create(ws, stepType, specId, arl);
				}
			});
				
		if (errors.isEmpty())
			propertyMetadata.put(RESERVED_PROPERTY_STEP_AUGMENTATION_STATUS, "success");
		return errors;
	}
	
	public static String getConstraintName(Conditions condition, int specOrderIndex, InstanceType stepType) {
		return "crd_"+condition+specOrderIndex+"_"+stepType.name();
	}
	
	public static String getConstraintName(Conditions condition, InstanceType stepType) {
		//return "crd_"+condition+"_"+stepType.name();
		return getConstraintName(condition, 0, stepType);
	}
	
	private String rewriteConstraint(String constraint) throws Exception {
		// we recreate the constraint to ensure we have all the types in iterators available
		//TODO: catch any exceptions here
		ArlEvaluator ae = new ArlEvaluator(stepType, constraint);
		constraint = ae.syntaxTree.getOriginalARL(0, false);

		List<StepParameter> singleUsage = PrematureTriggerGenerator.extractStepParameterUsageFromConstraint(sd, constraint);
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
		for( int pos : Lists.reverse(loc2param.keySet().stream().sorted().collect(Collectors.toList()))) {
			StepParameter param = loc2param.get(pos);
			
			String extParam = "self.out_"+param.getName();
			// create two strings: one before the param to be replaced, the rest after the param 
			// and then replace the param by the path
			String pre = constraint.substring(0, pos);
			String post = constraint.substring(pos+extParam.length());
			try {
				String replacement = getFirstOccuranceOfOutParam(sd, param).getNavPath();
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
		Optional<String> preconditionOpt = sd.getCondition(Conditions.PRECONDITION);
		if ( preconditionOpt.isEmpty() || sd.getInDND().getInSteps().isEmpty()) 			
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
