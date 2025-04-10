package at.jku.isse.passiveprocessengine.designspace;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import at.jku.isse.artifacteventstreaming.rule.RuleSchemaProvider;
import at.jku.isse.passiveprocessengine.rdfwrapper.RDFInstanceType;
import at.jku.isse.passiveprocessengine.core.ProcessContext;
import at.jku.isse.passiveprocessengine.definition.ProcessDefinitionError;
import at.jku.isse.passiveprocessengine.definition.activeobjects.ProcessDefinition;
import at.jku.isse.passiveprocessengine.definition.activeobjects.StepDefinition;
import at.jku.isse.passiveprocessengine.definition.factories.RuleAugmentation.StepParameter;
import at.jku.isse.passiveprocessengine.rdfwrapper.AbstractionMapper;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class RewriterFactory {

	private final AbstractionMapper mapper;
	private final boolean doOverridingAnalysis;
	private final RuleSchemaProvider ruleSchema;
	
	public String rewriteConstraint(RDFInstanceType ruleContext, String constraint, List<StepParameter> singleUsage, StepDefinition stepDef) throws Exception {
		ConstraintRewriter rewriter = new ConstraintRewriter(mapper.mapProcessDomainInstanceTypeToOntClass(ruleContext), ruleSchema);
		return rewriter.rewriteConstraint(constraint, singleUsage, stepDef);
	}

	public List<ProcessDefinitionError> checkOverriding(ProcessDefinition processDefinition, ProcessContext context) {
		if (doOverridingAnalysis) {
			ProcessOverridingAnalysis poa = new ProcessOverridingAnalysis(context);
			return poa.beginAnalysis(processDefinition, new ArrayList<>());
		} else
			return Collections.emptyList();
	}
}
