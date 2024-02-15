package at.jku.isse.passiveprocessengine.core;

public interface RuleDefinition extends PPEInstanceType {

	public boolean hasRuleError();
	public String getRuleError();
	
}
