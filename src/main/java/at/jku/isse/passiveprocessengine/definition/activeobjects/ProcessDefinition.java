package at.jku.isse.passiveprocessengine.definition.activeobjects;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.jena.ontapi.model.OntIndividual;

import at.jku.isse.designspace.rule.arl.evaluator.RuleDefinition;
import at.jku.isse.passiveprocessengine.rdfwrapper.NodeToDomainResolver;
import at.jku.isse.passiveprocessengine.rdfwrapper.RDFElement;
import at.jku.isse.passiveprocessengine.rdfwrapper.RDFInstance;
import at.jku.isse.passiveprocessengine.rdfwrapper.RDFInstanceType;
import at.jku.isse.passiveprocessengine.rdfwrapper.rule.RuleEnabledResolver;
import lombok.Getter;
import lombok.NonNull;
import at.jku.isse.passiveprocessengine.core.FactoryIndex;
import at.jku.isse.passiveprocessengine.definition.factories.SpecificProcessInstanceTypesFactory;
import at.jku.isse.passiveprocessengine.definition.types.DecisionNodeDefinitionTypeFactory;
import at.jku.isse.passiveprocessengine.definition.types.ProcessDefinitionTypeFactory.CoreProperties;
import at.jku.isse.passiveprocessengine.definition.types.StepDefinitionTypeFactory;
import at.jku.isse.passiveprocessengine.instance.StepLifecycle.Conditions;
import at.jku.isse.passiveprocessengine.instance.types.ProcessConfigBaseElementTypeFactory;
import at.jku.isse.passiveprocessengine.instance.types.SpecificProcessInstanceType;
import at.jku.isse.passiveprocessengine.instance.types.SpecificProcessStepType;;

public class ProcessDefinition extends StepDefinition{

	private DecisionNodeDefinitionTypeFactory dndFactory;
	private StepDefinitionTypeFactory stepDefFactory;
	
	
	public ProcessDefinition(@NonNull OntIndividual element, RDFInstanceType type, @NonNull NodeToDomainResolver resolver) {
		super(element, type, resolver);
	}
	
	public void injectFactories(StepDefinitionTypeFactory stepDefFactory, DecisionNodeDefinitionTypeFactory dndFactory) {
		this.dndFactory = dndFactory;
		this.stepDefFactory = stepDefFactory;
	}
	
	@SuppressWarnings("unchecked")
	public List<StepDefinition> getStepDefinitions() {
		return getTypedProperty(CoreProperties.stepDefinitions.toString(), List.class);
//		if (stepList != null) {
//			return stepList.stream()
//					.map(inst -> context.getWrappedInstance(getMostSpecializedClass((RDFInstance)inst), (RDFInstance) inst))
//					.filter(StepDefinition.class::isInstance)
//					.map(StepDefinition.class::cast)
//					.collect(Collectors.toList());
//		} else return Collections.emptyList();
	}

	@SuppressWarnings("unchecked")
	public void addStepDefinition(StepDefinition step) {
		getTypedProperty(CoreProperties.stepDefinitions.toString(), List.class).add(step);
	}

	@SuppressWarnings("unchecked")
	public Set<DecisionNodeDefinition> getDecisionNodeDefinitions() {
		return getTypedProperty(CoreProperties.decisionNodeDefinitions.toString(), Set.class);
//		if (dnSet != null) {
//			return dnSet.stream()
//					.map(inst -> context.getWrappedInstance(DecisionNodeDefinition.class, (RDFInstance) inst))
//					.map(obj ->(DecisionNodeDefinition)obj)
//					.collect(Collectors.toSet());
//		} else return Collections.emptySet();
	}

	@SuppressWarnings("unchecked")
	public void addDecisionNodeDefinition(DecisionNodeDefinition dnd) {
		getTypedProperty(CoreProperties.decisionNodeDefinitions.toString(), Set.class).add(dnd);
	}

	public DecisionNodeDefinition getDecisionNodeDefinitionByName(String localName) {
		return getDecisionNodeDefinitions().stream()
		.filter(dnd -> dnd.getName().equals(localName))
		.findAny().orElse(null);
	}

	public StepDefinition getStepDefinitionByName(String name) {
		return getStepDefinitions().stream()
				.filter(step -> step.getName().equals(name))
				.findAny().orElse(null);
	}

	@Override
	public void setDepthIndexRecursive(int indexToSet) {
		super.setDepthIndexRecursive(indexToSet);
		// make sure we also update the child process steps
		// find first DNI
		DecisionNodeDefinition startDND = this.getDecisionNodeDefinitions().stream().filter(dnd -> dnd.getInSteps().isEmpty()).findFirst().orElseThrow();
		startDND.setDepthIndexRecursive(indexToSet+1);
	}

	@Override
	public void deleteCascading() {
		getDecisionNodeDefinitions().stream().forEach(dnd -> dnd.deleteCascading());
		getStepDefinitions().forEach(sd -> sd.deleteCascading());
		
		// delete configtype
		this.getExpectedInput().entrySet().stream()
		.filter(entry -> entry.getValue().isOfTypeOrAnySubtype(resolver.findNonDeletedInstanceTypeByFQN(ProcessConfigBaseElementTypeFactory.typeId).get()))
		.forEach(configEntry -> {
			RDFInstanceType procConfig = configEntry.getValue().getInstanceType(); // context.getConfigFactory().getOrCreateProcessSpecificSubtype(configEntry.getKey(), this);
			procConfig.delete();
		});
		// wring instanceType: we need to get the dynamically generate Instance (the one that is used for the ProcessInstance)
		//String processDefName = SpecificProcessInstanceType.getProcessDefinitionURI(this);
		super.deleteCascading();
		
		//element.removeProperties();
	}
	
//	protected void deleteRuleIfExists(RDFInstanceType instType, ConstraintSpec spec, Conditions condition, String overrideName ) {
//		String name = SpecificProcessInstanceTypesFactory.CRD_PREFIX+condition+spec.getOrderIndex()+"_"+overrideName;
//		var crt = ((RuleEnabledResolver)resolver).getRuleByNameAndContext(name, instType);
//		if (crt != null) 
//			crt.delete();
//	}

	
	

//	protected static Class<? extends InstanceWrapper> getMostSpecializedClass(RDFInstance inst) {
//		// we have the problem, that the WrapperCache will only return a type we ask for (which might be a general type) rather than the most specialized one, hence we need to obtain that type here
//		// we assume that this is used only in here within, and thus that inst is only ProcessDefinition or StepDefinition
//		if (inst.getInstanceType().getId().startsWith(ProcessDefinitionType.typeId)) // its a process
//			return ProcessDefinition.class;
//		else
//			return StepDefinition.class; // for now only those two types
//	}



	public StepDefinition createAndAddStepDefinition(String stepId) {
		StepDefinition sd = stepDefFactory.createInstance(stepId);
				//StepDefinition.getInstance(stepId, ws); // any other initialization there
		sd.setProcess(this);
		this.addStepDefinition(sd);
		return sd;
	}

	public DecisionNodeDefinition createDecisionNodeDefinition(String dndId) {
		DecisionNodeDefinition dnd = dndFactory.createInstance(dndId); // any other initialization there
		dnd.setProcess(this);
		this.addDecisionNodeDefinition(dnd);
		return dnd;
	}

	public void setIsWithoutBlockingErrors(boolean isWithoutBlockingErrors) {
		setSingleProperty(CoreProperties.isWithoutBlockingErrors.toString(), isWithoutBlockingErrors);
	}

	@Override
	public void setProcOrderIndex(int index) {
		super.setProcOrderIndex(index);
		setElementOrder(); // continue within this (sub)process
	}

	public void setElementOrder() {
		int offset = this.getSpecOrderIndex();
		// 		init dnd index
		this.getDecisionNodeDefinitions().stream().forEach(dnd -> dnd.setProcOrderIndex(this.getSpecOrderIndex()));
		// determine order index
		this.getStepDefinitions().stream()
			.sorted(new Comparator<StepDefinition>() {
				@Override
				public int compare(StepDefinition o1, StepDefinition o2) {
					return Integer.compare(o1.getSpecOrderIndex(), o2.getSpecOrderIndex());
				}})
			.forEach(step -> {
			step.setProcOrderIndex(step.getSpecOrderIndex()+offset);
			step.getOutDND().setProcOrderIndex(step.getSpecOrderIndex()+offset); // every dnd has as order index the largest spec order index of its inSteps
		});
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();		
		getStepDefinitions().stream().map(step -> step.toString()).forEach(stepstr -> sb.append(stepstr+"\r\n"));		
		getDecisionNodeDefinitions().stream().map(dnd -> dnd.toString()).forEach(str -> sb.append(str+"\r\n"));
				
		return String.format("ProcessDefinition %s \r\n %s", this.getId(),  sb.toString());
	}

	
}
