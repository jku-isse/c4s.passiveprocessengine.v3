package at.jku.isse.passiveprocessengine.core;

public interface RuleDefinitionFactory {

	public RuleDefinition createInstance(InstanceType type, String ruleName, String ruleExpression);
	
	public void setPropertyRepairable(InstanceType type, String property, boolean isRepairable);
}
