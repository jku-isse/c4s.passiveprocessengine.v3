package at.jku.isse.passiveprocessengine.definition;

import java.util.Map;
import java.util.Set;

import at.jku.isse.passiveprocessengine.core.NameIdentifiableElement;
import at.jku.isse.passiveprocessengine.rdfwrapper.RDFInstanceType;
import at.jku.isse.passiveprocessengine.definition.activeobjects.ConstraintSpec;
import at.jku.isse.passiveprocessengine.definition.activeobjects.DecisionNodeDefinition;

public interface IStepDefinition extends NameIdentifiableElement {

	Map<String,RDFInstanceType> getExpectedInput();
	Map<String,RDFInstanceType> getExpectedOutput();
	Map<String, String> getInputToOutputMappingRules();

	//Optional<String> getCondition(Conditions condition);
	Set<ConstraintSpec> getQAConstraints();

	DecisionNodeDefinition getOutDND();
	DecisionNodeDefinition getInDND();
}
