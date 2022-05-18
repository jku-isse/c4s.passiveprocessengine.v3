package at.jku.isse.passiveprocessengine.analysis;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;

import at.jku.isse.passiveprocessengine.analysis.PrematureTriggerGenerator.DataSource.IoType;
import at.jku.isse.passiveprocessengine.analysis.PrematureTriggerGenerator.StepParameter.IO;
import at.jku.isse.passiveprocessengine.definition.DecisionNodeDefinition;
import at.jku.isse.passiveprocessengine.definition.ProcessDefinition;
import at.jku.isse.passiveprocessengine.definition.StepDefinition;
import at.jku.isse.passiveprocessengine.instance.ProcessStep;
import at.jku.isse.passiveprocessengine.instance.StepLifecycle.Conditions;
import lombok.Data;

public class PrematureTriggerGenerator {
	// first trivial level: identify data origin as process input of mere step output
	
	private int varCount = 0;
	Map<StepParameter, DataSource> dSource = new HashMap<>();
	
	public PrematureTriggerGenerator() {
		
	}
	
	public void generatePrematureConstraints(ProcessDefinition pd) {
		
		DecisionNodeDefinition initDND = pd.getDecisionNodeDefinitions().stream().filter(dnd -> dnd.getInSteps().isEmpty()).findAny().orElse(null);
		if (initDND == null)
			return; // should never be the case as this would mean a full circle in the process or malformed process
		
		pd.getStepDefinitions().stream()
			.filter(step -> !step.getInDND().equals(initDND)) // filter out initial steps, i.e., steps that are anyway the first ones to be made available,
			.filter(step -> (step.getCondition(Conditions.ACTIVATION).isPresent() || step.getCondition(Conditions.POSTCONDITION).isPresent())) // filter out those that have no activation or completion condition, also should not really be the case
			.forEach(step -> {
				dSource.clear();
				String premConstr = generatePrematureConstraints(step); // now for each step determine first occurrence of each input and output
				if (premConstr != null && premConstr.length() > 0) {
					pd.addPrematureTrigger(step.getName(), premConstr);
				}
			});
		
		
	}
	
	private String generatePrematureConstraints(StepDefinition step) {
		
		List<String> prematureConstraints = new LinkedList<>();
		
		step.getCondition(Conditions.ACTIVATION).ifPresent(constraint -> {
			String tempConstr = rewriteConstraint(step, constraint);
			if (tempConstr != null)
				prematureConstraints.add(constraint);
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
		List<StepParameter> singleUsage = extractStepParameterUsageFromConstraint(step, constraint);
			// for all params we determine their source, (none of the params will have this output as source as we check for this above
			boolean allMapped = singleUsage.stream()
			.filter(param -> !dSource.keySet().contains(param))
			.map(param -> { 
				if (param.getIo()==IO.IN) {
				Optional<DataSource> sOpt = getFirstOccuranceOfInParam(step, param, "");
				sOpt.ifPresent(ds -> dSource.put(param, ds));
				return sOpt.isPresent();
				} else {
					DataSource ds = getFirstOccuranceOfOutParam(step, param, "");
					if (ds.getSource() != step) {// if the detectable source is this step, which should not be the case, then we cant do any premature triggering
						dSource.put(param, ds);
						return true;
					} else return false;
				}
			})
			.allMatch(result -> result == true);
			// then lets replace in the constraint the local param with the original one
			if (allMapped) {
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
					DataSource ds = dSource.get(param); 	
					String extParam = param.getIo()==IO.IN ? "self.in_"+param.getName() : "self.out_"+param.getName();
					// create two strings: one before the param to be replaced, the rest after the param 
					// and then replace the param by the path
					String pre = constraint.substring(0, pos);
					String post = constraint.substring(pos+extParam.length());
					constraint = pre + dataSource2arlPathFromProcessInstance(ds) + post;
				}
				return constraint;
			}
			return null;
		
	}
	
	private String dataSource2arlPathFromProcessInstance(DataSource ds) {
		if (ds == null) return "ERROR NULL DATASOURCE";
		String navPath = ensureUniqueVarNames(ds.getNavPath());
		switch(ds.getIoType()) {
		case procIn: // we only need to access process input by name, context for these constraints is always the process
			return "self.in_"+ds.getParamName()+navPath;
		case procOut: // we only need to access process output by name, context for these constraints is always the process
			return "self.out_"+ds.getParamName()+navPath;
		case stepIn: 
			varCount++;
			return String.format("self.stepInstances->select(step"+varCount+" | step"+varCount+".stepDefinition.name = '%s') \r\n"
					+ " ->any()->asType(<root/types/"+ProcessStep.getProcessStepName(ds.getSource())+">).in_%s %s", ds.getSource().getName(), ds.getParamName(), navPath);
		case stepOut:
			varCount++;
			return String.format("self.stepInstances->select(step"+varCount+" | step"+varCount+".stepDefinition.name = '%s') \r\n"
					+ " ->any()->asType(<root/types/"+ProcessStep.getProcessStepName(ds.getSource())+">).out_%s %s", ds.getSource().getName(), ds.getParamName(), navPath);
		default:
			return "ERROR NO IOTYPE"; // should never happen
		}
	}
	
	private String ensureUniqueVarNames(String query) {
		// FIXME: implement: we need to check in any NavPath that it doesnt contain a var name (e.g., in an iteration etc) that occurred before, 
		// i.e., we need unique var names per constraint
		return query;
	}
	
	//public static String IN = "in";
	//public static String OUT = "out";
	
	// returns all in/out parameters that are used in a constraint
	private List<StepParameter> extractStepParameterUsageFromConstraint(StepDefinition step, String constraint) {
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
	
	private Optional<DataSource> getFirstOccuranceOfInParam(StepDefinition step, StepParameter parameter, String prevPath) { // for now, we just return one out of potentially many sources, e.g., in case of OR or XOR branching
		return step.getInDND().getMappings().stream()
			.filter(mapping -> mapping.getToParameter().equals(parameter.getName()) && mapping.getToStepType().equals(step.getName()) )
			.map(mapping -> { 
				if (mapping.getFromStepType().equals(step.getProcess().getName()) ) { // we reached the process, stop here for now (we don;t check if we are in a subprocess here for now) 
					return new DataSource(step, mapping.getFromParameter(), IoType.procIn, prevPath);
				} else { // check the output from a previous step
					StepDefinition prevStep = step.getProcess().getStepDefinitionByName(mapping.getFromStepType());
					return getFirstOccuranceOfOutParam(prevStep, new StepParameter(IO.OUT, mapping.getFromParameter()), prevPath);
				}
			} )
			.findAny(); //for now just return any found one
	}
	
	private DataSource getFirstOccuranceOfOutParam(StepDefinition step, StepParameter outParam, String prevPath) {
		String mapping = step.getInputToOutputMappingRules().get(outParam.getName()); // we assume for now that the mapping name is equal to the out param name, (this will be guaranteed in the future)
		if (mapping != null) { // for now, we need to process the mapping constraints (will not be necessary once these are defines using derived properties)
			int posSym = mapping.indexOf("->symmetricDifference");
			if (posSym > 0) {
				String navPath = mapping.substring(0, posSym); // now lets find which in param this outparam depends on
				//TODO for now lets assume we only need a single in param
				for (String inParam : step.getExpectedInput().keySet()) {
					if (navPath.startsWith("self.in_"+inParam)) { // we found the in
						// TODO BIG assumption: there is no parameter that is identical to another but longer, e.g., paramA vs paramALong i.e., we assume we dont run into such 'collisions'
						navPath = navPath.replaceFirst("self.in_"+inParam, "");
						// TODO the problem is, that any variables used in e.g., an iterator will be available multiple times when this path is used in multiple locations (very very likely).
						// this will result in a non-compiling ARL rule.
						return getFirstOccuranceOfInParam(step, new StepParameter(IO.IN, inParam), navPath+" "+prevPath).orElse(new DataSource(step, inParam, IoType.stepIn, navPath+" "+prevPath));
					}
				}
			}
		}
		return new DataSource(step, outParam.getName(), IoType.stepOut, prevPath);
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
		private final StepDefinition source;
		private final String paramName;
		private final IoType ioType;
		private final String navPath;
	}
}
