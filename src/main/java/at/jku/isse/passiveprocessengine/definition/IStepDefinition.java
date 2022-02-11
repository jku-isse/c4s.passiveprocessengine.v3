package at.jku.isse.passiveprocessengine.definition;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import at.jku.isse.designspace.sdk.core.model.InstanceType;

public interface IStepDefinition extends IdentifiableElement {
    
	Map<String,InstanceType> getExpectedInput();
	Map<String,InstanceType> getExpectedOutput();
	Map<String, String> getInputToOutputMappingRules();
	
	Optional<String> getPreconditionRule();
	Optional<String> getPostconditionRule();
	Optional<String> getActivationRule();
	Optional<String> getCancelationRule();
	Set<QAConstraintSpec> getQAConstraints();

	DecisionNodeDefinition getOutDND();
	DecisionNodeDefinition getInDND();
}
