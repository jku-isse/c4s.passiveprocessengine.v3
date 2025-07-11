package at.jku.isse.passiveprocessengine.designspace;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import at.jku.isse.designspace.core.model.InstanceType;
import at.jku.isse.designspace.rule.arl.expressions.VariableExpression;
import at.jku.isse.designspace.rule.checker.ArlEvaluator;
import at.jku.isse.passiveprocessengine.definition.activeobjects.StepDefinition;
import at.jku.isse.passiveprocessengine.definition.factories.RuleAugmentation.StepParameter;
import lombok.Data;


public class ConstraintRewriter {
	
	private final InstanceType ruleContext;
	private static final AtomicInteger varCount = new AtomicInteger(0);
	
	public ConstraintRewriter(InstanceType ruleContext) {
		this.ruleContext = ruleContext;
	}
	
	
	public String rewriteConstraint(String constraint, List<StepParameter> singleUsage, StepDefinition stepDef) throws Exception {		
		// we recreate the constraint to ensure we have all the types in iterators available
		ArlEvaluator ae = new ArlEvaluator(ruleContext, constraint);
		constraint = ae.syntaxTree.getOriginalARL(0, false);

		
		// we need to obtain for every in and out param that we have a source for the location, and then replace from the back every this location with the path from the source
		// every param can only be at a unique set of position, not shared with any other param, hence location/position index can serve as key
		Map<Integer, StepParameter> loc2param = new HashMap<>();
		for (StepParameter param : singleUsage ) {
			if (param.getIo()==StepParameter.IO.IN) continue; // we only replace out parameters
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
		ae = new ArlEvaluator(ruleContext, constraint);
		constraint = ae.syntaxTree.getOriginalARL(0, false);
		return constraint;
	}

	private String ensureUniqueVarNames(String query, InstanceType typeStep) {
		// we need to check in any NavPath that it doesnt contain a var name (e.g., in an iteration etc) that occurred before,
		// i.e., we need unique var names per constraint
		ArlEvaluator ae = new ArlEvaluator(typeStep, query);
		int varCountLocal = varCount.incrementAndGet();
		ae.parser.currentEnvironment.locals.values().stream()
			.filter(var -> !((VariableExpression)var).name.equals("self"))
			.forEach(var -> ((VariableExpression)var).name = ((VariableExpression)var).name+"_"+varCountLocal);
		String rewritten = ae.syntaxTree.getOriginalARL(0, false);
		return rewritten;
	}

	private DataSource getFirstOccuranceOfOutParam(StepDefinition step, StepParameter outParam) throws IllegalArgumentException{
		String mapping = step.getInputToOutputMappingRules().get(outParam.getName()); // we assume for now that the mapping name is equal to the out param name, (this will be guaranteed in the future)
		if (mapping != null) { // for now, we need to process the mapping constraints (will not be necessary once these are defines using derived properties)
			//InstanceType stepType = ProcessStep.getOrCreateDesignSpaceInstanceType(ws, step);
			ArlEvaluator ae = new ArlEvaluator(ruleContext, mapping);
			mapping = ae.syntaxTree.getOriginalARL(0, false);

			//int posSym = Math.max(mapping.indexOf("->symmetricDifference"), mapping.indexOf(".symmetricDifference")); //Symmetric difference is removed upon loading from DTOs and added extra upon creating mapping rules
			//if (posSym > 0) {
				String navPath = mapping;//.substring(0, posSym); // now lets find which in param this outparam depends on
				// we assume, only inparams are used in datamapping, i.e., we dont derive some output and then derive additional output from that!
				// and rewrite, then return
				String fullPath = ensureUniqueVarNames(navPath, ruleContext);
				DataSource thisDS = new DataSource(step, outParam.getName(), at.jku.isse.passiveprocessengine.designspace.ConstraintRewriter.DataSource.IoType.stepIn, fullPath);
				return thisDS;
			//}
		}
		// otherwise keep this outparam
		return new DataSource(step, outParam.getName(), at.jku.isse.passiveprocessengine.designspace.ConstraintRewriter.DataSource.IoType.stepOut, "self.out_"+outParam.getName());
	}

	
	
	


	public class SourceSizeComparator implements Comparator<DataSource> {
		@Override
		public int compare(DataSource o1, DataSource o2) {
			return Integer.compare(o1.getUpstreamSources().size(), o2.getUpstreamSources().size());
		}
	}
	
	

	@Data
	public static class DataSource{
		public enum IoType {stepOut, stepIn, procIn, procOut}
		private final StepDefinition local;
		private Set<DataSource> upstreamSources = new HashSet<>();
		private final String paramName;
		private final IoType ioType;
		private final String navPath;
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if ((obj == null) || (getClass() != obj.getClass()))
				return false;
			DataSource other = (DataSource) obj;
			if (ioType != other.ioType)
				return false;
			if (paramName == null) {
				if (other.paramName != null)
					return false;
			} else if (!paramName.equals(other.paramName))
				return false;
			if (local == null) {
				if (other.local != null)
					return false;
			} else if (!local.getName().equals(other.local.getName()))
				return false;
			return true;
		}
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((ioType == null) ? 0 : ioType.hashCode());
			result = prime * result + ((paramName == null) ? 0 : paramName.hashCode());
			result = prime * result + ((local == null) ? 0 : local.getName().hashCode());
			return result;
		}
	}
}
