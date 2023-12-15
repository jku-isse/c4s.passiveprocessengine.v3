package at.jku.isse.passiveprocessengine.core;

public interface RuleType extends InstanceType {

	public boolean hasRuleError();
	public String getRuleError();
}
