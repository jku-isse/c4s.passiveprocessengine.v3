package at.jku.isse.passiveprocessengine.definition.activeobjects;

import java.util.Comparator;

import org.apache.jena.ontapi.model.OntIndividual;

import at.jku.isse.passiveprocessengine.definition.types.ConstraintSpecTypeFactory.CoreProperties;
import at.jku.isse.passiveprocessengine.instance.StepLifecycle.Conditions;
import at.jku.isse.passiveprocessengine.rdfwrapper.NodeToDomainResolver;
import at.jku.isse.passiveprocessengine.rdfwrapper.RDFInstanceType;
import at.jku.isse.passiveprocessengine.rdfwrapper.rule.RDFRuleDefinitionWrapper;
import lombok.NonNull;

public class ConstraintSpec extends  ProcessDefinitionScopedElement{

	public ConstraintSpec(@NonNull OntIndividual element, RDFInstanceType type, @NonNull NodeToDomainResolver resolver) {
		super(element, type, resolver);
	}

	public String getConstraintId() {
		return getName();
	}

	public String getConstraintSpec() {
		return getTypedProperty(CoreProperties.constraintSpec.toString(), String.class);
	}
	
	public String getAugmentedConstraintSpec() {
		return getTypedProperty(CoreProperties.augmentedSpec.toString(), String.class);
	}

	public void setAugmentedConstraintSpec(@NonNull String augmentedArl) {
		setSingleProperty(CoreProperties.augmentedSpec.toString(), augmentedArl);
	}
	
	public String getHumanReadableDescription() {
		return getTypedProperty(CoreProperties.humanReadableDescription.toString(), String.class);
	}

	public Integer getOrderIndex() {
		return getTypedProperty(CoreProperties.constraintSpecOrderIndex.toString(), Integer.class);
	}

	public boolean isOverridable() {
		return getTypedProperty(CoreProperties.isOverridable.toString(), Boolean.class);
	}

	public Conditions getConditionType() {
		return Conditions.valueOf(getTypedProperty(CoreProperties.conditionsType.toString(), String.class));
	}

	public RDFRuleDefinitionWrapper getRuleDefinition() {
		return getTypedProperty(CoreProperties.ruleType.toString(), RDFRuleDefinitionWrapper.class);
	}
	
	@Override
	public void deleteCascading() {
		var rule = getRuleDefinition();
		if (rule != null) {
			rule.delete();
		}
		super.deleteCascading();
	}
	
	public static final Comparator<ConstraintSpec> COMPARATOR_BY_ORDERINDEX = new Comparator<>() {
	@Override
	public int compare(ConstraintSpec o1, ConstraintSpec o2) {
		return o1.getOrderIndex().compareTo(o2.getOrderIndex()) ;
	}};
}
