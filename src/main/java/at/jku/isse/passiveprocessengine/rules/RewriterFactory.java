package at.jku.isse.passiveprocessengine.rules;

import java.util.List;

import at.jku.isse.artifacteventstreaming.rule.RuleSchemaProvider;
import at.jku.isse.passiveprocessengine.rdfwrapper.RDFInstanceType;
import at.jku.isse.passiveprocessengine.rdfwrapper.rule.RuleEnabledResolver;
import at.jku.isse.passiveprocessengine.definition.activeobjects.StepDefinition;
import at.jku.isse.passiveprocessengine.definition.factories.RuleAugmentation.StepParameter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class RewriterFactory {

	private final RuleEnabledResolver mapper;
	private final RuleSchemaProvider ruleSchema;
	
	public String rewriteConstraint(RDFInstanceType ruleContext, String constraint, List<StepParameter> singleUsage, StepDefinition stepDef) throws Exception {
		ConstraintRewriter rewriter = new ConstraintRewriter(mapper.mapProcessDomainInstanceTypeToOntClass(ruleContext), ruleSchema);
		return rewriter.rewriteConstraint(constraint, singleUsage, stepDef);
	}

}
