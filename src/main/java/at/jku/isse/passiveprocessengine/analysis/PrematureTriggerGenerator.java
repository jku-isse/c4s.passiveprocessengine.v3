package at.jku.isse.passiveprocessengine.analysis;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;

import at.jku.isse.designspace.core.model.InstanceType;
import at.jku.isse.designspace.core.model.Workspace;
import at.jku.isse.designspace.rule.arl.expressions.VariableExpression;
import at.jku.isse.designspace.rule.checker.ArlEvaluator;
import at.jku.isse.designspace.rule.model.ConsistencyRuleType;
import at.jku.isse.passiveprocessengine.analysis.PrematureTriggerGenerator.DataSource.IoType;
import at.jku.isse.passiveprocessengine.analysis.PrematureTriggerGenerator.StepParameter.IO;
import at.jku.isse.passiveprocessengine.definition.DecisionNodeDefinition;
import at.jku.isse.passiveprocessengine.definition.ProcessDefinition;
import at.jku.isse.passiveprocessengine.definition.StepDefinition;
import at.jku.isse.passiveprocessengine.instance.ProcessInstance;
import at.jku.isse.passiveprocessengine.instance.ProcessStep;
import at.jku.isse.passiveprocessengine.instance.StepLifecycle.Conditions;
import lombok.Data;

public class PrematureTriggerGenerator {
	// first trivial level: identify data origin as process input of mere step output
	
	private Workspace ws;
	private ProcessDefinition pd;
	private InstanceType procInstType;
	private int varCount = 0;
	//Map<StepParameter, DataSource> dSource = new HashMap<>();
	
	public PrematureTriggerGenerator(Workspace ws, ProcessDefinition pd) {
		this.ws = ws;
		this.pd = pd;
		procInstType = ProcessInstance.getOrCreateDesignSpaceInstanceType(ws, pd);
	}
	
	public void generatePrematureConstraints() {
		
		DecisionNodeDefinition initDND = pd.getDecisionNodeDefinitions().stream().filter(dnd -> dnd.getInSteps().isEmpty()).findAny().orElse(null);
		if (initDND == null)
			return; // should never be the case as this would mean a full circle in the process or malformed process
		
		pd.getStepDefinitions().stream()
			.filter(step -> !step.getInDND().equals(initDND)) // filter out initial steps, i.e., steps that are anyway the first ones to be made available,
			.filter(step -> (step.getCondition(Conditions.ACTIVATION).isPresent() || step.getCondition(Conditions.POSTCONDITION).isPresent())) // filter out those that have no activation or completion condition, also should not really be the case
			.forEach(step -> {
			//	dSource.clear();
				String premConstr = generatePrematureConstraints(step); // now for each step determine first occurrence of each input and output
				if (premConstr != null && premConstr.length() > 0) {
					pd.addPrematureTrigger(step.getName(), premConstr);
				}
			});
		InstanceType procType = ProcessInstance.getOrCreateDesignSpaceInstanceType(ws, pd);
		pd.getPrematureTriggers().entrySet().stream()
		.forEach(entry -> {
			if (entry.getValue() != null) {
				String ruleName = ProcessInstance.generatePrematureRuleName(entry.getKey(), pd);
				ConsistencyRuleType crt = ConsistencyRuleType.create(ws, procType, ruleName, entry.getValue());
				//typeStep.createPropertyType("crd_premature_"+entry.getKey(), Cardinality.SINGLE, crt);	// not sure we need a property here				
				pd.setPrematureConstraintNameStepDefinition(ruleName, entry.getKey());
			}
		});
	}
	
	private String generatePrematureConstraints(StepDefinition step) {
		
		List<String> prematureConstraints = new LinkedList<>();
		
		step.getCondition(Conditions.ACTIVATION).ifPresent(constraint -> {
			
			String tempConstr = rewriteConstraint(step, constraint);
			if (tempConstr != null)
				prematureConstraints.add(tempConstr);
		});
		step.getCondition(Conditions.POSTCONDITION).ifPresent(constraint -> {
			String tempConstr = rewriteConstraint(step, constraint);
			if (tempConstr != null)
				prematureConstraints.add(tempConstr);
		});
		if (prematureConstraints.size() > 0) {
			return prematureConstraints.stream().collect(Collectors.joining(") \r\n or \r\n(", "(", ")"));
		} else
			return "";
	}
	
	private String rewriteConstraint(StepDefinition step, String constraint) {
		// we recreate the constraint to ensure we have all the types in iterators available
		InstanceType stepType = ProcessStep.getOrCreateDesignSpaceInstanceType(ws, step, null); // process type is already known at this point, no need to provide again, hence null is ok
		ArlEvaluator ae = new ArlEvaluator(stepType, constraint);
		constraint = ae.syntaxTree.getOriginalARL(0, false);

		List<StepParameter> singleUsage = extractStepParameterUsageFromConstraint(step, constraint);
		// we need to obtain for every in and out param that we have a source for the location, and then replace from the back every this location with the path from the source
		// every param can only be at a unique set of position, not shared with any other param, hence location/position index can serve as key
		Map<Integer, StepParameter> loc2param = new HashMap<>();
		for (StepParameter param : singleUsage ) {
			String extParam = param.getIo()==IO.IN ? "self.in_"+param.getName() : "self.out_"+param.getName();
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
			//				DataSource ds = dSource.get(param); 	
			String extParam = param.getIo()==IO.IN ? "self.in_"+param.getName() : "self.out_"+param.getName();
			// create two strings: one before the param to be replaced, the rest after the param 
			// and then replace the param by the path
			String pre = constraint.substring(0, pos);
			String post = constraint.substring(pos+extParam.length());
			String replacement = param.getIo()==IO.IN ? combinePaths(getFirstOccurancesOfInParam(step, param)) : getFirstOccuranceOfOutParam(step, param).navPath;
			constraint = pre + replacement + post;
		}
		// ensure the new constraint is correct and has unique var names
		return ensureUniqueVarNames(constraint, procInstType);

	}
	
//	private String dataSource2arlPathFromProcessInstance(DataSource ds) {
//		if (ds == null) return "ERROR NULL DATASOURCE";
//		switch(ds.getIoType()) {
//		case procIn: // we only need to access process input by name, context for these constraints is always the process
//			return  ensureUniqueVarNames("self.in_"+ds.getParamName()+ds.navPath, ProcessInstance.getOrCreateDesignSpaceInstanceType(ws, (ProcessDefinition)ds.getSource()));
//		case procOut: // we only need to access process output by name, context for these constraints is always the process
//			return  ensureUniqueVarNames("self.out_"+ds.getParamName()+ds.navPath, ProcessInstance.getOrCreateDesignSpaceInstanceType(ws, (ProcessDefinition)ds.getSource()));
//		case stepIn: 
//			varCount++;
//			return ensureUniqueVarNames(String.format("self.stepInstances->select(step"+varCount+" | step"+varCount+".stepDefinition.name = '%s') \r\n"
//					+ " ->any()->asType(<root/types/"+ProcessStep.getProcessStepName(ds.getSource())+">).in_%s %s", ds.getSource().getName(), ds.getParamName(), ds.navPath),
//					ProcessInstance.getOrCreateDesignSpaceInstanceType(ws, ds.getSource().getProcess()));
//		case stepOut:
//			varCount++;
//			return ensureUniqueVarNames(String.format("self.stepInstances->select(step"+varCount+" | step"+varCount+".stepDefinition.name = '%s') \r\n"
//					+ " ->any()->asType(<root/types/"+ProcessStep.getProcessStepName(ds.getSource())+">).out_%s %s", ds.getSource().getName(), ds.getParamName(), ds.navPath),
//					ProcessInstance.getOrCreateDesignSpaceInstanceType(ws, ds.getSource().getProcess()));
//		default:
//			return "ERROR NO IOTYPE"; // should never happen
//		}
//	}
	
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
	
	// returns all in/out parameters that are used in a constraint
	protected static List<StepParameter> extractStepParameterUsageFromConstraint(StepDefinition step, String constraint) {
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
	
	private Set<DataSource> getFirstOccurancesOfInParam(StepDefinition step, StepParameter parameter) { // for now, we just return one out of potentially many sources, e.g., in case of OR or XOR branching
		Set<DataSource> dsSet = step.getInDND().getMappings().stream()
			.filter(mapping -> mapping.getToParameter().equals(parameter.getName()) && mapping.getToStepType().equals(step.getName()) )
			.map(mapping -> { 
				if (mapping.getFromStepType().equals(step.getProcess().getName()) ) { // we reached the process, stop here for now (we don;t check if we are in a subprocess here for now) 
					DataSource localDS = new DataSource(step.getProcess(), mapping.getFromParameter(), IoType.procIn, "self.in_"+mapping.getFromParameter());
					localDS.getUpstreamSources().add(localDS);
					return localDS;
				} else { // check the output from a previous step
					StepDefinition prevStep = step.getProcess().getStepDefinitionByName(mapping.getFromStepType());
					return getFirstOccuranceOfOutParam(prevStep, new StepParameter(IO.OUT, mapping.getFromParameter()));
				}
			} )
			.collect(Collectors.toSet());
		if (dsSet.isEmpty()) {
				varCount++;
				String fullPath = ensureUniqueVarNames(String.format("self.stepInstances->select(step"+varCount+" | step"+varCount+".stepDefinition.name = '%s') \r\n"
						+ " ->any()->asType(<root/types/"+ProcessStep.getProcessStepName(step)+">).in_%s ", step.getName(), parameter.getName()),
						procInstType);
				DataSource local = new DataSource(step, parameter.getName(), IoType.stepIn, fullPath);
				local.getUpstreamSources().add(local); //its its own root node
				return Set.of(local);
		} else return dsSet;
	}
	
	private DataSource getFirstOccuranceOfOutParam(StepDefinition step, StepParameter outParam) {
		String mapping = step.getInputToOutputMappingRules().get(outParam.getName()); // we assume for now that the mapping name is equal to the out param name, (this will be guaranteed in the future)
		if (mapping != null) { // for now, we need to process the mapping constraints (will not be necessary once these are defines using derived properties)
			InstanceType stepType = ProcessStep.getOrCreateDesignSpaceInstanceType(ws, step, null); // process type is already known at this point, no need to provide again, hence null is ok
			ArlEvaluator ae = new ArlEvaluator(stepType, mapping);
			mapping = ae.syntaxTree.getOriginalARL(0, false);;
			
			//int posSym = Math.max(mapping.indexOf("->symmetricDifference"), mapping.indexOf(".symmetricDifference"));
			//if (posSym > 0) {
				String navPath = mapping;//.substring(0, posSym); // now lets find which in param this outparam depends on
				// we assume, only inparams are used in datamapping, i.e., we dont derive some output and then derive additional output from that!
				Map<Integer, String> loc2param = new HashMap<>();
				
				for (String inParam : step.getExpectedInput().keySet()) {
					String extParam = "self.in_"+inParam;
					// find all locations of these inparam
					// TODO BIG assumption: there is no parameter that is identical to another but longer, e.g., paramA vs paramALong i.e., we assume we dont run into such 'collisions'
					int lastFound = 0;
					while (true) {
						lastFound = navPath.indexOf(extParam, lastFound);
						if (lastFound >= 0) {
							loc2param.put(lastFound, inParam);
							lastFound++;
						} else
							break;
					}
				}
				Set<DataSource> rootSources = new HashSet<>();
				for( int pos : Lists.reverse(loc2param.keySet().stream().sorted().collect(Collectors.toList()))) {
					String param = loc2param.get(pos);
					String extParam = "self.in_"+param;
					// create two strings: one before the param to be replaced, the rest after the param 
					// and then replace the param by the path
					String pre = navPath.substring(0, pos);
					String post = navPath.substring(pos+extParam.length());
					Set<DataSource> dsSet = getFirstOccurancesOfInParam(step, new StepParameter(IO.IN, param));
					String replacement =  combinePaths(dsSet);
					navPath = pre + replacement + post;
					rootSources.addAll(dsSet.stream().flatMap(ds -> ds.getUpstreamSources().stream()).collect(Collectors.toSet()));
				}
				// and rewrite, then return
				String fullPath = ensureUniqueVarNames(navPath, procInstType);
				DataSource thisDS = new DataSource(step, outParam.getName(), IoType.stepOut, fullPath);
				thisDS.upstreamSources.addAll(rootSources);
				return thisDS;
			//}
		}
		// otherwise prepare the path to this outparam
		varCount++;
		String fullPath = ensureUniqueVarNames(String.format("self.stepInstances->select(step"+varCount+" | step"+varCount+".stepDefinition.name = '%s') \r\n"
				+ " ->any()->asType(<root/types/"+ProcessStep.getProcessStepName(step)+">).out_%s ", step.getName(), outParam.getName()),
				procInstType);
		DataSource local = new DataSource(step, outParam.getName(), IoType.stepOut, fullPath);
		local.getUpstreamSources().add(local); //its its own root node
		return local;
	}
	
	private String combinePaths(Set<DataSource> dsSet) {
		if (dsSet.isEmpty()) return "";
		if (dsSet.size() == 1) return dsSet.stream().findAny().get().navPath;
		else {
			//determine overlap of root sources, if same source, then we dont need to union anything as we are or-ing anyway. --> might filter out candidates that lead to premature trigger, lets keep union.
			List<DataSource> dsList = dsSet.stream().sorted(new SourceSizeComparator()).collect(Collectors.toList());
//			List<DataSource> dsList = new ArrayList<>();
//			DataSource largestDS = fullList.get(0);
//			dsList.add(largestDS);
//			for (int i = 1; i < dsList.size(); i++ ) {
//				// if one item is not subset of largest list, add to dsList, else drop
//				DataSource smallerDS = fullList.get(i);
//				if (largestDS.getUpstreamSources().containsAll(smallerDS.getUpstreamSources()))
//					; //drop 
//				else
//					dsList.add(smallerDS);
//			}
//			if (dsList.size() == 1) return dsSet.stream().findAny().get().navPath;
			
			StringBuffer prefix= new StringBuffer();
			StringBuffer union = new StringBuffer();
			union.append(dsList.get(0).navPath);
			for (int i = 1; i < dsList.size(); i++ ) {
				prefix.append("(");
				union.append(".union("+dsList.get(i).navPath+"))");
			}
			prefix.append(union);
			return prefix.toString();
		}
	}
	
	public class SourceSizeComparator implements Comparator<DataSource> {
		@Override
		public int compare(DataSource o1, DataSource o2) {
			return Integer.compare(o1.getUpstreamSources().size(), o2.getUpstreamSources().size());
		}
	}
	
	@Data 
	public static class StepParameter {
		public enum IO {IN, OUT};
		private final IO io;
		private final String name;
	}
	
	
	@Data
	public static class DataSource{
		public enum IoType {stepOut, stepIn, procIn, procOut};
		private final StepDefinition local;
		private Set<DataSource> upstreamSources = new HashSet<>();
		private final String paramName;
		private final IoType ioType;
		private final String navPath;
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
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
			result = prime * result + ((local == null) ? 0 : local.getId().hashCode());
			return result;
		}
	}
}
