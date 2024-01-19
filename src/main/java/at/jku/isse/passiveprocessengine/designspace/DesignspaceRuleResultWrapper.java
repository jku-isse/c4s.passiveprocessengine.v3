package at.jku.isse.passiveprocessengine.designspace;

import at.jku.isse.designspace.rule.model.ConsistencyRule;
import at.jku.isse.passiveprocessengine.core.Instance;
import at.jku.isse.passiveprocessengine.core.RuleResult;

public class DesignspaceRuleResultWrapper extends DesignspaceInstanceWrapper implements RuleResult {
	
	public DesignspaceRuleResultWrapper(ConsistencyRule delegate,
			DesignSpaceSchemaRegistry dsSchemaRegistry) {
		super(delegate, dsSchemaRegistry);
		
	}

	@Override
	public Boolean isConsistent() {
		return ((ConsistencyRule)delegate).isConsistent();
	}

	@Override
	public Instance getContextInstance() {
		return dsSchemaRegistry.getWrappedInstance(((ConsistencyRule)delegate).contextInstance());		
	}

	@Override
	public String toString() {
		return getDelegate().toString();
	}
	
	
}
