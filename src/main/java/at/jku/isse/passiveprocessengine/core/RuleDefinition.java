package at.jku.isse.passiveprocessengine.core;

public interface RuleDefinition extends InstanceType {

	public boolean hasRuleError();
	public String getRuleError();
}
