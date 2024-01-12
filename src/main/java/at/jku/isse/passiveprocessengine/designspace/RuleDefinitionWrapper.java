package at.jku.isse.passiveprocessengine.designspace;

import at.jku.isse.designspace.rule.model.ConsistencyRuleType;
import at.jku.isse.passiveprocessengine.core.RuleDefinition;

public class RuleDefinitionWrapper extends DesignspaceInstanceTypeWrapper implements RuleDefinition {

	public RuleDefinitionWrapper(ConsistencyRuleType delegate, DesignSpaceSchemaRegistry dsSchemaRegistry) {
		super(delegate, dsSchemaRegistry);				
	}

	@Override
	public boolean hasRuleError() {
		return ((ConsistencyRuleType)delegate).hasRuleError();
	}

	@Override
	public String getRuleError() {
		return ((ConsistencyRuleType)delegate).ruleError();
	}
	
	
	
}
