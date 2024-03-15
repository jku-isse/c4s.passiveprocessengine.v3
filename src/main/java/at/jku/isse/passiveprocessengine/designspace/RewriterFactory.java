package at.jku.isse.passiveprocessengine.designspace;

import java.util.List;

import at.jku.isse.passiveprocessengine.core.PPEInstanceType;
import at.jku.isse.passiveprocessengine.definition.activeobjects.StepDefinition;
import at.jku.isse.passiveprocessengine.definition.factories.RuleAugmentation.StepParameter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class RewriterFactory {

	private final DesignspaceAbstractionMapper mapper;
	
	public String rewriteConstraint(PPEInstanceType ruleContext, String constraint, List<StepParameter> singleUsage, StepDefinition stepDef) throws Exception {
		ConstraintRewriter rewriter = new ConstraintRewriter((at.jku.isse.designspace.core.model.InstanceType) mapper.mapProcessDomainInstanceTypeToDesignspaceInstanceType(ruleContext));
		return rewriter.rewriteConstraint(constraint, singleUsage, stepDef);
	}


}
