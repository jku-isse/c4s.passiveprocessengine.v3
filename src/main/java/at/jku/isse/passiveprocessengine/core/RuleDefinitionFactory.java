package at.jku.isse.passiveprocessengine.core;

public interface RuleDefinitionFactory {

	public RuleDefinition createInstance(PPEInstanceType type, String ruleName, String ruleExpression);
	
	public void setPropertyRepairable(PPEInstanceType type, String property, boolean isRepairable);
}
