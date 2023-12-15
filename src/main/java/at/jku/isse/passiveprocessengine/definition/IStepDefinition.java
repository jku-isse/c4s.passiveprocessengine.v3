package at.jku.isse.passiveprocessengine.definition;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import at.jku.isse.designspace.core.model.InstanceType;
import at.jku.isse.passiveprocessengine.IdentifiableElement;
import at.jku.isse.passiveprocessengine.instance.StepLifecycle.Conditions;

public interface IStepDefinition extends IdentifiableElement {
    
	Map<String,InstanceType> getExpectedInput();
	Map<String,InstanceType> getExpectedOutput();
	Map<String, String> getInputToOutputMappingRules();
	
	Optional<String> getCondition(Conditions condition);
	Set<ConstraintSpec> getQAConstraints();

	DecisionNodeDefinition getOutDND();
	DecisionNodeDefinition getInDND();
}
