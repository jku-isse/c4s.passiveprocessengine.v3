package at.jku.isse.passiveprocessengine.monitoring;

import at.jku.isse.passiveprocessengine.core.PPEExecutedRepairListener;
import at.jku.isse.passiveprocessengine.rdfwrapper.RDFInstance;
import at.jku.isse.passiveprocessengine.core.RuleEnabledResolver;
import at.jku.isse.passiveprocessengine.core.RuleResult;
import at.jku.isse.passiveprocessengine.instance.activeobjects.ProcessStep;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class ExecutedRepairListenerImpl implements PPEExecutedRepairListener{

	private  final UsageMonitor monitor;
	private final RuleEnabledResolver ctx;

	@Override
	public void repairExecuted(RuleResult ruleEval, RDFInstance contextInstance, String repairTemplate) {
		monitor.repairActionExecuted(ruleEval, ctx.getWrappedInstance(ProcessStep.class, contextInstance), repairTemplate, 0);
	}



}