package at.jku.isse.passiveprocessengine.instance;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import at.jku.isse.designspace.rule.arl.exception.ChangeExecutionException;
import at.jku.isse.designspace.rule.arl.repair.Operator;
import at.jku.isse.designspace.rule.arl.repair.Repair;
import at.jku.isse.designspace.rule.arl.repair.RepairAction;
import at.jku.isse.designspace.rule.arl.repair.RepairNode;
import at.jku.isse.designspace.rule.arl.repair.RepairTreeFilter;
import at.jku.isse.passiveprocessengine.core.RuleResult;
import at.jku.isse.passiveprocessengine.designspace.RuleServiceWrapper;
import at.jku.isse.passiveprocessengine.instance.activeobjects.ProcessStep;
import at.jku.isse.passiveprocessengine.instance.messages.Events;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class InputToOutputMapper {

	private static RepairTreeFilter rtf = new OutputUpdateRepairTreeFilter();

	private RuleServiceWrapper ruleService;
	
	public InputToOutputMapper(RuleServiceWrapper ruleService) {
		this.ruleService = ruleService;
	}
	
	
	@SuppressWarnings("unchecked")
	public List<Events.ProcessChangedEvent> mapInputToOutputInStepScope(ProcessStep step, RuleResult ruleResult) {
		if (ruleResult.isConsistent()) {
			log.info("MappingRule became consistent while request was queued "+ruleResult.toString());
			return Collections.emptyList(); // nothing to do
		}

		RepairNode repairTree = ruleService.getRepairTree(ruleResult);
		rtf.filterRepairTree(repairTree);
		//DONE: if there are (only) concrete repair actions (which should be the case with symmetric difference)

		Set<Repair> repairs = repairTree.getRepairs();
		if (repairs == null) {
			String state = ruleResult.isConsistent() ? "CONSISTENT" : "INCONSISTENT";
			log.error("FATAL: No repairs could be created for "+state+" "+ruleResult.getInstanceType().getName());
			// check if there is a rule error, print that, hence later retry needed
			return Collections.emptyList();
		} else {
			repairs.stream().findAny().ifPresentOrElse(repair -> {
				log.debug("Executing Datamapping: "+repair);
				try {
					repair.execute();
				} catch (ChangeExecutionException e) {
					log.error("Error executing repair "+repair);
					e.printStackTrace();
				}
			}
			, () -> {
				log.error("No concrete Repairs found for "+ruleResult.getInstanceType().getName());
				//TODO: THIS NEEDS TO BE FIXED:		throw new RuntimeException("Datamapping could not be repaired");
			}
					);

			return Collections.emptyList(); //FIXME: somehow determine from repairs what was added and removed.
		}
	}

	private static boolean _onlyAddOrRemoveOperator(RepairAction ra) {
		return ra.getOperator().equals(Operator.ADD) || ra.getOperator().equals(Operator.REMOVE);
	}

	private static boolean _onlyChangesOutput(RepairAction ra) {
		return ra.getProperty() != null && ra.getProperty().startsWith("out_");
	}

    private static class OutputUpdateRepairTreeFilter extends RepairTreeFilter {
		@Override
		public boolean compliesTo(RepairAction repairAction) {
			return _onlyAddOrRemoveOperator(repairAction) && _onlyChangesOutput(repairAction);
		}

    }
}
