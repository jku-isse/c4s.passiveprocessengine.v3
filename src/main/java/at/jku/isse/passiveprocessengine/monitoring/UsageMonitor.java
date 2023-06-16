package at.jku.isse.passiveprocessengine.monitoring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.jku.isse.designspace.rule.arl.repair.RepairNode;
import at.jku.isse.designspace.rule.model.ConsistencyRule;
import at.jku.isse.designspace.rule.service.RuleService;
import at.jku.isse.passiveprocessengine.instance.ConstraintWrapper;
import at.jku.isse.passiveprocessengine.instance.ProcessInstance;
import at.jku.isse.passiveprocessengine.instance.ProcessStep;
import net.logstash.logback.argument.StructuredArgument;

import static net.logstash.logback.argument.StructuredArguments.kv;

public class UsageMonitor {

	private ITimeStampProvider timeProvider;
	
	private final Logger monitor = LoggerFactory.getLogger("monitor.usage");
	
	public static enum UsageEvents {ProcessViewed, StepViewed, ConstraintViewed, GuidanceExecuted};
	public static enum LogProperties {processInstanceId, processDefinitionId, stepDefinitionId, evalResult, guidanceSize, constraintId, repairTemplate, repairRank, originTime, eventType, userId}
	
	public UsageMonitor(ITimeStampProvider timeProvider) {
		this.timeProvider = timeProvider;
	}
	
	private StructuredArgument getTime() {
		return kv(LogProperties.originTime.toString(), timeProvider.getLastChangeTimeStamp().toString());
	}
	
	public void processViewed(ProcessInstance proc) {		
		monitor.info("Process {} viewed with {}", kv(LogProperties.processInstanceId.toString(), proc.getName()), 
												kv(LogProperties.processDefinitionId.toString(), proc.getDefinition().getName()), 
												getTime(),
												kv(LogProperties.eventType.toString(), UsageEvents.ProcessViewed.toString())
		);		
	}
	
	public void stepViewed(ProcessStep step, String userId) {
		monitor.info("Step {} viewed within process {} with {}", kv(LogProperties.stepDefinitionId.toString(), step.getDefinition().getName()), 
																	kv(LogProperties.processInstanceId.toString(), step.getProcess().getName()), 
																	kv(LogProperties.processDefinitionId.toString(), step.getProcess().getDefinition().getName()), 
																	getTime(), 
																	kv(LogProperties.eventType.toString(), UsageEvents.StepViewed.toString()),
																	kv(LogProperties.userId.toString(), userId != null ? userId : "anonymous" ) );
	}
	
	public void constraintedViewed(ConstraintWrapper cw, String userId) { //if not fulfilled, implies that repairtree was loaded
		int repairCount = 0;
		if (!cw.getEvalResult() && cw.getCr() != null) {
			RepairNode repairTree = RuleService.repairTree(cw.getCr());
			repairCount = repairTree.getRepairActions().size();
			// TODO: obtain also maxRank?
		}
		monitor.info("Constraint {} ( {} {} ) viewed within process {} with {}", kv(LogProperties.constraintId.toString(), cw.getQaSpec().getQaConstraintId()), 
																							kv(LogProperties.evalResult.toString(), cw.getEvalResult()), 
																							kv(LogProperties.guidanceSize.toString(), repairCount),
																							kv(LogProperties.processInstanceId.toString(), cw.getProcess().getName()), 
																							kv(LogProperties.processDefinitionId.toString(), cw.getProcess().getDefinition().getName()), 
																							getTime(),
																							kv(LogProperties.eventType.toString(), UsageEvents.ConstraintViewed.toString()),
																							kv(LogProperties.userId.toString(), userId != null ? userId : "anonymous" )
																							);
	}
	
	public void repairActionExecuted(ConsistencyRule processScopedRule, ProcessStep step, String selectedRepairTemplate, int rank) { 
		monitor.debug("Guidance {} Executed for constraint {} at {} within process {} of type {}", kv(LogProperties.repairTemplate.toString(), selectedRepairTemplate), 
				kv(LogProperties.constraintId.toString(), processScopedRule.consistencyRuleDefinition().name()),
				kv(LogProperties.repairRank.toString(), rank),
				kv(LogProperties.processInstanceId.toString(), step.getProcess().getName()), 
				kv(LogProperties.processDefinitionId.toString(), step.getProcess().getDefinition().getName()),
				getTime(), kv(LogProperties.eventType.toString(), UsageEvents.GuidanceExecuted.toString()));
		
	}
	
	
	
}
