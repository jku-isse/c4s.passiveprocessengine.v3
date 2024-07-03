package at.jku.isse.passiveprocessengine.monitoring;

import at.jku.isse.passiveprocessengine.core.PPEExecutedRepairListener;
import at.jku.isse.passiveprocessengine.core.PPEInstance;
import at.jku.isse.passiveprocessengine.core.ProcessContext;
import at.jku.isse.passiveprocessengine.core.RuleResult;
import at.jku.isse.passiveprocessengine.instance.activeobjects.ProcessStep;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class ExecutedRepairListenerImpl implements PPEExecutedRepairListener{

	private  final UsageMonitor monitor;
	private final ProcessContext ctx;

	@Override
	public void repairExecuted(RuleResult ruleEval, PPEInstance contextInstance, String repairTemplate) {
		monitor.repairActionExecuted(ruleEval, ctx.getWrappedInstance(ProcessStep.class, contextInstance), repairTemplate, 0);
	}



}