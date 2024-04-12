package at.jku.isse.passiveprocessengine.monitoring;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.jku.isse.designspace.rule.arl.repair.RepairNode;
import at.jku.isse.designspace.rule.model.ConsistencyRule;
import at.jku.isse.designspace.rule.service.RuleService;
import at.jku.isse.passiveprocessengine.instance.ConstraintWrapper;
import at.jku.isse.passiveprocessengine.instance.ProcessInstance;
import at.jku.isse.passiveprocessengine.instance.ProcessStep;
import net.logstash.logback.argument.StructuredArgument;

public class UsageMonitor {

	private ITimeStampProvider timeProvider;
	
	private final Logger monitor = LoggerFactory.getLogger("monitor.usage");
	
	public static enum UsageEvents {ProcessViewed, StepViewed, ConstraintViewed, GuidanceExecuted, ProcessDeleted, ProcessCreated};
	public static enum LogProperties {rootProcessInstanceId, processInstanceId, processDefinitionId, stepDefinitionId, evalResult, guidanceSize, constraintId, repairTemplate, repairRank, originTime, eventType, userId}
	
	public UsageMonitor(ITimeStampProvider timeProvider) {
		this.timeProvider = timeProvider;
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
	
	public void constraintedViewed(ConstraintWrapper cw, String userId) { //if not fulfilled, implies that repairtree was loaded
		int repairCount = 0;
		if (!cw.getEvalResult() && cw.getCr() != null) {
			RepairNode repairTree = RuleService.repairTree(cw.getCr());
			repairCount = repairTree.getRepairActions().size();
			// TODO: obtain also maxRank?
		}
		List<StructuredArgument> args = getDefaultArguments(cw.getProcess(), userId, UsageEvents.ConstraintViewed.toString());
		args.add(kv(LogProperties.constraintId.toString(), cw.getSpec().getConstraintId())); 
		args.add(kv(LogProperties.evalResult.toString(), cw.getEvalResult()));
		args.add(kv(LogProperties.guidanceSize.toString(), repairCount));
		monitor.info("Constraint viewed", args.toArray());
	}
	
	public void repairActionExecuted(ConsistencyRule processScopedRule, ProcessStep step, String selectedRepairTemplate, int rank) { 
		List<StructuredArgument> args = getDefaultArguments(step.getProcess(), null, UsageEvents.GuidanceExecuted.toString());
		args.add(kv(LogProperties.repairTemplate.toString(), selectedRepairTemplate));
		args.add(kv(LogProperties.constraintId.toString(), processScopedRule.consistencyRuleDefinition().name()));
		args.add(kv(LogProperties.repairRank.toString(), rank));
		monitor.debug("Guidance executed", args.toArray());
		
	}
	
	
	
}
