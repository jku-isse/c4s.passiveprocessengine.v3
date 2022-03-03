package at.jku.isse.passiveprocessengine.instance;

import java.util.Set;

import at.jku.isse.designspace.rule.arl.exception.ChangeExecutionException;
import at.jku.isse.designspace.rule.arl.repair.Repair;
import at.jku.isse.designspace.rule.arl.repair.RepairNode;
import at.jku.isse.designspace.rule.model.ConsistencyRule;
import at.jku.isse.designspace.rule.service.RuleService;

public class InputToOutputMapper {

	
	
	public static void mapInputToOutputInStepScope(ProcessStep step, ConsistencyRule violatedCR) {
		if (violatedCR.isConsistent()) return; // nothing to do
		
		RepairNode rn = RuleService.repairTree(violatedCR);
		
		Set<Repair> actions = rn.generateRepairsSample();
		actions.stream().forEach(a -> {
			System.out.println(a);	
			if (a.isExecutable()) {
				try {
					a.execute();
				} catch (ChangeExecutionException e) {
					e.printStackTrace();
				}
			}
		});
	}
}
