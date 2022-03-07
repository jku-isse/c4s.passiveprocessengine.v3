package at.jku.isse.passiveprocessengine.instance;

import java.util.Set;
import java.util.stream.Collectors;

import at.jku.isse.designspace.rule.arl.exception.ChangeExecutionException;
import at.jku.isse.designspace.rule.arl.repair.AbstractRepairAction;
import at.jku.isse.designspace.rule.arl.repair.ExecutableRepairAction;
import at.jku.isse.designspace.rule.arl.repair.Operator;
import at.jku.isse.designspace.rule.arl.repair.Repair;
import at.jku.isse.designspace.rule.arl.repair.RepairNode;
import at.jku.isse.designspace.rule.model.ConsistencyRule;
import at.jku.isse.designspace.rule.model.ConsistencyRuleType;
import at.jku.isse.designspace.rule.service.RuleService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class InputToOutputMapper {

	

	@SuppressWarnings("unchecked")
	public static void mapInputToOutputInStepScope(ProcessStep step, ConsistencyRule crule) {
		if (crule.isConsistent()) return; // nothing to do

		
		ConsistencyRuleType crt = (ConsistencyRuleType) crule.getInstanceType();
		RepairNode rn = RuleService.repairTree(crule);
		
		Set<Object> objects = (Set<Object>) step.getDefinition().getExpectedOutput().values().stream()
			.distinct()
			.flatMap(type -> type.getInstancesIncludingThoseOfSubtypes().get().stream())
			.collect(Collectors.toSet());
		
		//Set<Object> objects = step.getInstance().workspace.debugInstanceTypeFindByName("JiraArtifact").getInstancesIncludingThoseOfSubtypes().get();
		// obtain based on outProperty --> need to check which outproperty this is
		// this also works only if all instances that are relevant are somewhere in the designspace prefetched
		
		//rn.getConcreteRepairs(step.getInstance().getPropertyAsSet("in_jiraIn").get(), violatedCR, false, false);
		//Set<Object> objects = Set.copyOf(step.getInstance().getPropertyAsSet("in_jiraIn").get());
		Set<Repair> repairs = rn.getConcreteRepairs(objects, crule, true, false); 
		//Set<Repair> repairs = rn.getRepairs();
		//rn.generateRepairsSample();
		Set<Repair> validConcreteRepairs = repairs.stream()
		.filter(a -> a.isExecutable()) // get concrete repairs only
		//.filter(ConsistencyRepair.class::isInstance)
		//.map(ConsistencyRepair.class::cast)
		.filter(cr -> doesOnlyContainAddOrRemoveOperators(cr)) // only add/remove of elements in output allowed
		.filter(cr -> !doesChangeInput(cr)) // filter out any that touches the input, only changing of output allowed
		.collect(Collectors.toSet());
		
		validConcreteRepairs.stream().findFirst()
		.ifPresent(a -> {	
			try {
				log.debug("Executing Datamapping: "+a);
				a.execute();
			} catch (ChangeExecutionException e) {
				e.printStackTrace();
			}
		});
	}
	
	@SuppressWarnings("rawtypes")
	private static boolean doesOnlyContainAddOrRemoveOperators(Repair cr) {
		return cr.getActions().stream()
				//.map(ra -> (RepairAction)ra)
				.allMatch(ra -> ((AbstractRepairAction) ra).getOperator().equals(Operator.ADD) 
							|| ((AbstractRepairAction) ra).getOperator().equals(Operator.REMOVE));
	}
	
	@SuppressWarnings("unchecked")
	private static boolean doesChangeInput(Repair cr) {
		return cr.getActions().stream()
			.filter(ExecutableRepairAction.class::isInstance)
			.map(ExecutableRepairAction.class::cast)
			.anyMatch(era -> ((ExecutableRepairAction)era).getProperty() == null || ((ExecutableRepairAction)era).getProperty().startsWith("in_"));
		//return false;
	}
}
