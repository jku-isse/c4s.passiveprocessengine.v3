package at.jku.isse.passiveprocessengine.core;

public interface RuleResult extends PPEInstance {

	Boolean isConsistent();

	PPEInstance getContextInstance();

}
