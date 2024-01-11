package at.jku.isse.passiveprocessengine.designspace;

import java.util.List;

import at.jku.isse.designspace.rule.arl.repair.RepairNode;
import at.jku.isse.designspace.rule.model.ConsistencyRule;
import at.jku.isse.designspace.rule.service.RuleService;
import at.jku.isse.passiveprocessengine.core.InstanceType;
import at.jku.isse.passiveprocessengine.core.RuleResult;
import at.jku.isse.passiveprocessengine.definition.activeobjects.StepDefinition;
import at.jku.isse.passiveprocessengine.definition.factories.RuleAugmentation.StepParameter;

public class RuleServiceWrapper {

	DesignspaceAbstractionMapper mapper;
	
	
	public RepairNode repairTree(RuleResult ruleResult) {
		return RuleService.repairTree((ConsistencyRule) mapper.mapProcessDomainInstanceToDesignspaceInstance(ruleResult));
	}

	public String rewriteConstraint(InstanceType ruleContext, String constraint, List<StepParameter> singleUsage, StepDefinition stepDef) throws Exception {
		ConstraintRewriter rewriter = new ConstraintRewriter(mapper.mapProcessDomainInstanceTypeToDesignspaceInstanceType(ruleContext));
		return rewriter.rewriteConstraint(constraint, singleUsage, stepDef);
	}
	
	
	
	
}
