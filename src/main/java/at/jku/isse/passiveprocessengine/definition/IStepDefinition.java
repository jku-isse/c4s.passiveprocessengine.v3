package at.jku.isse.passiveprocessengine.definition;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import at.jku.isse.passiveprocessengine.core.PPEInstanceType;
import at.jku.isse.passiveprocessengine.core.NameIdentifiableElement;
import at.jku.isse.passiveprocessengine.definition.activeobjects.ConstraintSpec;
import at.jku.isse.passiveprocessengine.definition.activeobjects.DecisionNodeDefinition;
import at.jku.isse.passiveprocessengine.instance.StepLifecycle.Conditions;

public interface IStepDefinition extends NameIdentifiableElement {

	Map<String,PPEInstanceType> getExpectedInput();
	Map<String,PPEInstanceType> getExpectedOutput();
	Map<String, String> getInputToOutputMappingRules();

	//Optional<String> getCondition(Conditions condition);
	Set<ConstraintSpec> getQAConstraints();

	DecisionNodeDefinition getOutDND();
	DecisionNodeDefinition getInDND();
}
