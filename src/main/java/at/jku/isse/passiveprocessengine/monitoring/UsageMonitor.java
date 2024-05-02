package at.jku.isse.passiveprocessengine.monitoring;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.jku.isse.designspace.rule.arl.repair.RepairNode;
import at.jku.isse.passiveprocessengine.core.RepairTreeProvider;
import at.jku.isse.passiveprocessengine.core.RuleResult;
import at.jku.isse.passiveprocessengine.instance.activeobjects.ConstraintResultWrapper;
import at.jku.isse.passiveprocessengine.instance.activeobjects.ProcessInstance;
import at.jku.isse.passiveprocessengine.instance.activeobjects.ProcessStep;
import net.logstash.logback.argument.StructuredArgument;

public class UsageMonitor {

	private ITimeStampProvider timeProvider;
	private RepairTreeProvider ruleService;

	private final Logger monitor = LoggerFactory.getLogger("monitor.usage");

	public static enum UsageEvents {ProcessViewed, StepViewed, ConstraintViewed, GuidanceExecuted, ProcessDeleted, ProcessCreated}
	public static enum LogProperties {rootProcessInstanceId, processInstanceId, processDefinitionId, stepDefinitionId, evalResult, guidanceSize, constraintId, repairTemplate, repairRank, originTime, eventType, userId}

	public UsageMonitor(ITimeStampProvider timeProvider, RepairTreeProvider ruleService) {
		this.timeProvider = timeProvider;
		this.ruleService = ruleService;
	}

	private StructuredArgument getTime() {
		return kv(LogProperties.originTime.toString(), timeProvider.getLastChangeTimeStamp().toString());
	}

	protected static String getRootProcessInstanceId(ProcessInstance proc) {
		if (proc.getProcess() != null)
			return getRootProcessInstanceId(proc.getProcess());
		else
			return proc.getName();
	}

	private List<StructuredArgument> getDefaultArguments(ProcessInstance proc, String userId, String eventType) {
		List<StructuredArgument> args = new LinkedList<>();
		args.add(kv(LogProperties.rootProcessInstanceId.toString(), getRootProcessInstanceId(proc)));
		args.add(kv(LogProperties.processInstanceId.toString(), proc.getName()));
		args.add(kv(LogProperties.processDefinitionId.toString(), proc.getDefinition().getName()));
		args.add(kv(LogProperties.userId.toString(), userId != null ? userId : "anonymous" ));
		args.add(getTime());
		args.add(kv(LogProperties.eventType.toString(), eventType));
		return args;
	}

	public void processCreated(ProcessInstance proc, String userId) {
		List<StructuredArgument> args = getDefaultArguments(proc, userId, UsageEvents.ProcessCreated.toString());
		monitor.info("Process created", args.toArray());
	}

	public void processViewed(ProcessInstance proc, String userId) {
		List<StructuredArgument> args = getDefaultArguments(proc, userId, UsageEvents.ProcessViewed.toString());
		monitor.info("Process viewed", args.toArray());
	}

	public void processDeleted(ProcessInstance proc, String userId) {
		List<StructuredArgument> args = getDefaultArguments(proc, userId, UsageEvents.ProcessDeleted.toString());
		monitor.info("Process deleted", args.toArray());
	}

	public void stepViewed(ProcessStep step, String userId) {
		List<StructuredArgument> args = getDefaultArguments(step.getProcess(), userId, UsageEvents.StepViewed.toString());
		args.add(kv(LogProperties.stepDefinitionId.toString(), step.getDefinition().getName()));
		monitor.info("Step viewed", args.toArray());
	}

	public void constraintedViewed(ConstraintResultWrapper cw, String userId) { //if not fulfilled, implies that repairtree was loaded
		int repairCount = 0;
		if (!cw.getEvalResult() && cw.getRuleResult() != null) {
			RepairNode repairTree = (RepairNode) ruleService.getRepairTree(cw.getRuleResult());
			repairCount = repairTree.getRepairActions().size();
			// TODO: obtain also maxRank?
		}
		List<StructuredArgument> args = getDefaultArguments(cw.getProcess(), userId, UsageEvents.ConstraintViewed.toString());
		args.add(kv(LogProperties.constraintId.toString(), cw.getConstraintSpec().getConstraintId()));
		args.add(kv(LogProperties.evalResult.toString(), cw.getEvalResult()));
		args.add(kv(LogProperties.guidanceSize.toString(), repairCount));
		monitor.info("Constraint viewed", args.toArray());
	}

	public void repairActionExecuted(RuleResult processScopedRuleResult, ProcessStep step, String selectedRepairTemplate, int rank) {
		List<StructuredArgument> args = getDefaultArguments(step.getProcess(), null, UsageEvents.GuidanceExecuted.toString());
		args.add(kv(LogProperties.repairTemplate.toString(), selectedRepairTemplate));
		args.add(kv(LogProperties.constraintId.toString(), processScopedRuleResult.getInstanceType().getName()));
		args.add(kv(LogProperties.repairRank.toString(), rank));
		monitor.debug("Guidance executed", args.toArray());

	}



}
