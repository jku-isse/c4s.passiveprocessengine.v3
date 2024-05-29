package at.jku.isse.passiveprocessengine.monitoring;

import at.jku.isse.designspace.core.model.Instance;
import at.jku.isse.designspace.rule.arl.repair.analyzer.ExecutedRepairListener;
import at.jku.isse.designspace.rule.model.ConsistencyRule;
import at.jku.isse.passiveprocessengine.WrapperCache;
import at.jku.isse.passiveprocessengine.instance.ProcessStep;

public class ExecutedRepiarListenerImp implements ExecutedRepairListener{

	public UsageMonitor monitor;
	public ExecutedRepiarListenerImp(UsageMonitor monitor)
	{
		this.monitor=monitor;
	}
	@Override
	public void repairExecuted(ConsistencyRule rule, Instance contextInstance, String repairTemplate) {
		monitor.repairActionExecuted(rule, 
				WrapperCache.getWrappedInstance(ProcessStep.class, contextInstance), repairTemplate, -1);
	}

}
