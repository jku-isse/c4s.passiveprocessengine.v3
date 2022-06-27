package at.jku.isse.passiveprocessengine.analysis;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;

import at.jku.isse.designspace.core.model.Cardinality;
import at.jku.isse.designspace.core.model.InstanceType;
import at.jku.isse.designspace.core.model.Workspace;
import at.jku.isse.designspace.rule.arl.expressions.VariableExpression;
import at.jku.isse.designspace.rule.checker.ArlEvaluator;
import at.jku.isse.designspace.rule.model.ConsistencyRuleType;
import at.jku.isse.passiveprocessengine.analysis.PrematureTriggerGenerator.DataSource;
import at.jku.isse.passiveprocessengine.analysis.PrematureTriggerGenerator.DataSource.IoType;
import at.jku.isse.passiveprocessengine.analysis.PrematureTriggerGenerator.StepParameter;
import at.jku.isse.passiveprocessengine.definition.ProcessDefinition;
import at.jku.isse.passiveprocessengine.definition.StepDefinition;
import at.jku.isse.passiveprocessengine.instance.ProcessException;
import at.jku.isse.passiveprocessengine.instance.ProcessStep;
import at.jku.isse.passiveprocessengine.instance.StepLifecycle.Conditions;

public class RuleAugmentation {

	
	private Workspace ws;
	private StepDefinition sd;
	private InstanceType stepType;
	private int varCount = 0;
	
	public RuleAugmentation(Workspace ws, StepDefinition sd, InstanceType stepType) {
		this.ws = ws;
		this.sd = sd;
		this.stepType = stepType;
	}
	
	public void augmentConditions() throws ProcessException {
		ProcessException pex = new ProcessException("Error augmenting transition conditions and/or QA constraints");
		for (Conditions condition : Conditions.values()) {
			if (sd.getCondition(condition).isPresent()) {
				if (sd.getCondition(condition).get() != null) {
					// as the output cannot be changed directly (except in rare cases when set manually)
					// repairing the output makes no sense, but rather its datamapping definition, so we replace all out_xxx occurences with the datamapping definition
					String arl = sd.getCondition(condition).get();
					try {
						arl = rewriteConstraint(arl);
					} catch(Exception e) {
						pex.getErrorMessages().add(String.format("Error aumenting %s : %s", arl, e.getMessage()));
					}
					// if the error is from augmentation, still keep the original rule (with perhaps not as useful repairs)
					// if the original rule has error, then this will be captured later anyway.
					ConsistencyRuleType crd = ConsistencyRuleType.create(ws, stepType, "crd_"+condition+"_"+stepType.name(), arl);
					// not evaluated yet here, assert ConsistencyUtils.crdValid(crd);
					stepType.createPropertyType(condition.toString(), Cardinality.SINGLE, crd);
				}
			}	
		}
		//qa constraints:
		ProcessDefinition pd = sd.getProcess() !=null ? sd.getProcess() : (ProcessDefinition)sd;
		sd.getQAConstraints().stream()
			.forEach(spec -> {
				String specId = ProcessStep.getQASpecId(spec, pd);
				if (spec.getQaConstraintSpec() != null) {
					String arl = spec.getQaConstraintSpec();
//					try {  TODO reactivate after study once any() operator works
//						arl = rewriteConstraint(arl);
//					} catch(Exception e) {
//						pex.getErrorMessages().add(String.format("Error aumenting %s : %s", arl, e.getMessage()));
//					}
					ConsistencyRuleType crt = ConsistencyRuleType.create(ws, stepType, specId, arl);
				}
			});
		
		if (!pex.getErrorMessages().isEmpty())
			throw pex;
	}
	
	private String rewriteConstraint(String constraint) throws Exception {
		// we recreate the constraint to ensure we have all the types in iterators available
		//TODO: catch any exceptions here
		ArlEvaluator ae = new ArlEvaluator(stepType, constraint);
		constraint = ae.syntaxTree.getOriginalARL();

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
		constraint = ae.syntaxTree.getOriginalARL();
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
		String rewritten = ae.syntaxTree.getOriginalARL();
		return rewritten;
	}
	
	private DataSource getFirstOccuranceOfOutParam(StepDefinition step, StepParameter outParam) throws IllegalArgumentException{
		String mapping = step.getInputToOutputMappingRules().get(outParam.getName()); // we assume for now that the mapping name is equal to the out param name, (this will be guaranteed in the future)
		if (mapping != null) { // for now, we need to process the mapping constraints (will not be necessary once these are defines using derived properties)
			//InstanceType stepType = ProcessStep.getOrCreateDesignSpaceInstanceType(ws, step);
			ArlEvaluator ae = new ArlEvaluator(stepType, mapping);
			mapping = ae.syntaxTree.getOriginalARL();
			
			int posSym = Math.max(mapping.indexOf("->symmetricDifference"), mapping.indexOf(".symmetricDifference"));
			if (posSym > 0) {
				String navPath = mapping.substring(0, posSym); // now lets find which in param this outparam depends on
				// we assume, only inparams are used in datamapping, i.e., we dont derive some output and then derive additional output from that!
				// and rewrite, then return
				String fullPath = ensureUniqueVarNames(navPath, stepType);
				DataSource thisDS = new DataSource(step, outParam.getName(), IoType.stepIn, fullPath);
				return thisDS;
			}
		}
		// otherwise keep this outparam
		return new DataSource(step, outParam.getName(), IoType.stepOut, "self.out_"+outParam.getName());
	}
	
}
