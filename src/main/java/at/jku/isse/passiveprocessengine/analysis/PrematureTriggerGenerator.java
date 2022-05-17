package at.jku.isse.passiveprocessengine.analysis;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import at.jku.isse.passiveprocessengine.analysis.PrematureTriggerGenerator.DataSource.IoType;
import at.jku.isse.passiveprocessengine.definition.DecisionNodeDefinition;
import at.jku.isse.passiveprocessengine.definition.ProcessDefinition;
import at.jku.isse.passiveprocessengine.definition.StepDefinition;
import at.jku.isse.passiveprocessengine.instance.ProcessStep;
import at.jku.isse.passiveprocessengine.instance.StepLifecycle.Conditions;
import lombok.Data;

public class PrematureTriggerGenerator {
	// first trivial level: identify data origin as process input of mere step output
	
	private int varCount = 0;
	
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
				String premConstr = generatePrematureConstraints(step); // now for each step determine first occurrence of each input and output
				if (premConstr != null && premConstr.length() > 0) {
					pd.addPrematureTrigger(step.getName(), premConstr);
				}
			});
		
		
	}
	
	private String generatePrematureConstraints(StepDefinition step) {
		Map<String, DataSource> dSource = new HashMap<>();
		List<String> prematureConstraints = new LinkedList<>();
		
		step.getCondition(Conditions.ACTIVATION).ifPresent(constraint -> {
			Map<String, List<String>> singleUsage = extractStepParameterUsageFromConstraint(step, constraint);
			if (singleUsage.get(OUT).size() > 0) { // we dont support output yet
			
			} else {
				// for all params we determine their source, (non of the params will have this output as source as we check for this above
				boolean allMapped = singleUsage.get(IN).stream()
				.filter(param -> !dSource.keySet().contains(param))
				.map(param -> { 
					Optional<DataSource> sOpt = getFirstOccuranceOfInParam(step, param);
					sOpt.ifPresent(ds -> dSource.put(param, ds));
					return sOpt.isPresent() ? true : false;
					})
				.allMatch(result -> result == true);
				// then lets replace in the constraint the local param with the original one
				if (allMapped) {
					for (String param : step.getExpectedInput().keySet() ) {
						DataSource ds = dSource.get(param); // if null, then not relevant for this constraint
						constraint = constraint.replace("self.in_"+param, dataSource2arlPathFromProcessInstance(ds));
					}
					prematureConstraints.add(constraint);
				}
			}
		});
		step.getCondition(Conditions.POSTCONDITION).ifPresent(constraint -> {
			Map<String, List<String>> singleUsage = extractStepParameterUsageFromConstraint(step, constraint);
			if (singleUsage.get(OUT).size() > 0) { // we dont support output yet
			
			} else {
				// for all params we determine their source, (non of the params will have this output as source as we check for this above
				boolean allMapped = singleUsage.get(IN).stream()
				.filter(param -> !dSource.keySet().contains(param))
				.map(param -> { 
					Optional<DataSource> sOpt = getFirstOccuranceOfInParam(step, param);
					sOpt.ifPresent(ds -> dSource.put(param, ds));
					return sOpt.isPresent() ? true : false;
					})
				.allMatch(result -> result == true);
				// then lets replace in the constraint the local param with the original one
				if (allMapped) {
					for (String param : step.getExpectedInput().keySet() ) {
						DataSource ds = dSource.get(param); // if null, then not relevant for this constraint
						// whenever we need to navigate to a step, we cannot use the same navigation path spec every time as the variable names used on the path need to be different, 
						// hence the following more convoluted approach
						String inParam = "self.in_"+param;
						while (constraint.contains(inParam)) {
							constraint = constraint.replaceFirst(inParam, dataSource2arlPathFromProcessInstance(ds));
						}
					}
					prematureConstraints.add(constraint);
				}
			}
		});
		if (prematureConstraints.size() > 0) {
			return prematureConstraints.stream().collect(Collectors.joining(") \r\n or \r\n(", "(", ")"));
		} else
			return "";
	}
	
	private String dataSource2arlPathFromProcessInstance(DataSource ds) {
		if (ds == null) return "ERROR NULL DATASOURCE";
		switch(ds.getIoType()) {
		case procIn: // we only need to access process input by name, context for these constraints is always the process
			return "self.in_"+ds.getParamName();
		case procOut: // we only need to access process output by name, context for these constraints is always the process
			return "self.out_"+ds.getParamName();
		case stepIn: //FIXME: we need to check the actual process step that has the data in dynamically defined properties that in turn depend on the process name
			varCount++;
			return String.format("self.stepInstances->select(step"+varCount+" | step"+varCount+".stepDefinition.name = '%s') \r\n"
					+ " ->any()->asType(<root/types/"+ProcessStep.getProcessStepName(ds.getSource())+">).in_%s ", ds.getSource().getName(), ds.getParamName());
		case stepOut:
			varCount++;
			return String.format("self.stepInstances->select(step"+varCount+" | step"+varCount+".stepDefinition.name = '%s') \r\n"
					+ " ->any()->asType(<root/types/"+ProcessStep.getProcessStepName(ds.getSource())+">).out_%s ", ds.getSource().getName(), ds.getParamName());
		default:
			return "ERROR NO IOTYPE"; // should never happen
		}
	}
	
	public static String IN = "in";
	public static String OUT = "out";
	
	// returns all in/out parameters that are used in a constraint
	private Map<String, List<String>> extractStepParameterUsageFromConstraint(StepDefinition step, String constraint) {
		Map<String, List<String>> usage = new HashMap<>();
		usage.put(IN, step.getExpectedInput().keySet().stream()
			.filter(param -> constraint.contains("self.in_"+param))
			.collect(Collectors.toList()));
		usage.put(OUT, step.getExpectedOutput().keySet().stream()
				.filter(param -> constraint.contains("self.out_"+param))
				.collect(Collectors.toList()));
		return usage;
	}
	
	private Optional<DataSource> getFirstOccuranceOfInParam(StepDefinition step, String parameter) { // for now, we just return one out of potentially many sources, e.g., in case of OR or XOR branching
		return step.getInDND().getMappings().stream()
			.filter(mapping -> mapping.getToParameter().equals(parameter) && mapping.getToStepType().equals(step.getName()) )
			.map(mapping -> { 
				StepDefinition prevStep = step.getProcess().getStepDefinitionByName(mapping.getFromStepType());
				if (mapping.getFromStepType().equals(step.getProcess().getName()) ) { // we reached the process, stop here for now (we don;t check if we are in a subprocess here for now) 
					return new DataSource(prevStep, mapping.getFromParameter(), IoType.procIn);
				} else { // check the output from a previous step
					return getFirstOccuranceOfOutParam(prevStep, mapping.getFromParameter());
				}
			} )
			.findAny(); //for now just return any found one
	}
	
	private DataSource getFirstOccuranceOfOutParam(StepDefinition step, String param) {
		// no sophisticated further analysis supported yet as we need to analyse the mapping definition how the output depends on the input
		return new DataSource(step, param, IoType.stepOut);
	}
	
	@Data
	public static class DataSource{
		public enum IoType {stepOut, stepIn, procIn, procOut};
		private final StepDefinition source;
		private final String paramName;
		private final IoType ioType;
	}
}
