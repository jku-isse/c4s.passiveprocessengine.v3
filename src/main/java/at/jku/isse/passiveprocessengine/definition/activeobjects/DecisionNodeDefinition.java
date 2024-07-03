package at.jku.isse.passiveprocessengine.definition.activeobjects;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import at.jku.isse.passiveprocessengine.core.ProcessContext;
import at.jku.isse.passiveprocessengine.core.PPEInstance;
import at.jku.isse.passiveprocessengine.definition.ProcessDefinitionError;
import at.jku.isse.passiveprocessengine.definition.types.DecisionNodeDefinitionType;
import at.jku.isse.passiveprocessengine.instance.types.ProcessConfigBaseElementType;

public class DecisionNodeDefinition extends ProcessDefinitionScopedElement {

	public DecisionNodeDefinition(PPEInstance instance, ProcessContext context) {
		super(instance, context);
	}

	public void setInflowType(InFlowType ift) {
		instance.setSingleProperty(DecisionNodeDefinitionType.CoreProperties.inFlowType.toString(), ift.toString());
	}

	public InFlowType getInFlowType() {
		return InFlowType.valueOf(instance.getTypedProperty(DecisionNodeDefinitionType.CoreProperties.inFlowType.toString(), String.class, InFlowType.AND.toString()));
	}


	public DecisionNodeDefinition getScopeClosingDecisionNodeOrNull() {
		if (this.getOutSteps().isEmpty())
			return null;
		else {
			PPEInstance dnd = instance.getTypedProperty(DecisionNodeDefinitionType.CoreProperties.closingDN.toString(), PPEInstance.class);
			if (dnd != null)
				return context.getWrappedInstance(DecisionNodeDefinition.class, dnd);
			else {
				DecisionNodeDefinition closingDnd = determineScopeClosingDN();
				instance.setSingleProperty(DecisionNodeDefinitionType.CoreProperties.closingDN.toString(), closingDnd.getInstance());
				return closingDnd;
			}
		}
	}

	private DecisionNodeDefinition determineScopeClosingDN() {
//		List<Step> nextSteps = getOutStepsOf(dn);
//		if (nextSteps.isEmpty()) return null; // end of the process, closing DN reached
		Set<DecisionNodeDefinition> nextStepOutDNs = this.getOutSteps().stream().map(step -> step.getOutDND()).collect(Collectors.toSet());
		// size must be 1 or greater as we dont allow steps without subsequent DN
		if (nextStepOutDNs.size() == 1) { // implies the scope closing DN as otherwise there need to be multiple opening subscope ones
			return nextStepOutDNs.iterator().next();
		} else {
			Set<DecisionNodeDefinition> sameDepthNodes = new HashSet<>();
			while (sameDepthNodes.size() != 1) {
				sameDepthNodes = nextStepOutDNs.stream().filter(nextDN -> nextDN.getDepthIndex() == this.getDepthIndex()).collect(Collectors.toSet());
				assert(sameDepthNodes.size() <= 1); //closing next nodes can only be on same level or deeper (i.e., larger values)
				if (sameDepthNodes.size() != 1) {
					Set<DecisionNodeDefinition> nextNextStepOutDNs = nextStepOutDNs.stream().map(nextDN -> nextDN.getScopeClosingDecisionNodeOrNull()).collect(Collectors.toSet());
					nextStepOutDNs = nextNextStepOutDNs;
				}
				assert(nextStepOutDNs.size() > 0);
			}
			return sameDepthNodes.iterator().next();
		}
	}

	@SuppressWarnings("unchecked")
	protected void addInStep(StepDefinition sd) {
		instance.getTypedProperty(DecisionNodeDefinitionType.CoreProperties.inSteps.toString(), Set.class).add(sd.getInstance());
	}

	@SuppressWarnings("unchecked")
	protected void addOutStep(StepDefinition sd) {
		instance.getTypedProperty(DecisionNodeDefinitionType.CoreProperties.outSteps.toString(), Set.class).add(sd.getInstance());
	}

	@SuppressWarnings("unchecked")
	public void addDataMappingDefinition(MappingDefinition md) {
		instance.getTypedProperty(DecisionNodeDefinitionType.CoreProperties.dataMappingDefinitions.toString(), Set.class).add(md.getInstance());
	}

	@SuppressWarnings("unchecked")
	public Set<MappingDefinition> getMappings() {
		Set mdSet = instance.getTypedProperty(DecisionNodeDefinitionType.CoreProperties.dataMappingDefinitions.toString(), Set.class);
		if (mdSet != null ) {
			return (Set<MappingDefinition>) mdSet.stream()
					.map(inst -> context.getWrappedInstance(MappingDefinition.class, (PPEInstance) inst))
					.collect(Collectors.toSet());
		} else return Collections.emptySet();
	}

	@SuppressWarnings("unchecked")
	public Set<StepDefinition> getInSteps() {
		return (Set<StepDefinition>) instance.getTypedProperty(DecisionNodeDefinitionType.CoreProperties.inSteps.toString(), Set.class).stream()
			.filter(PPEInstance.class::isInstance)
			.map(PPEInstance.class::cast)
			.map(inst -> context.getWrappedInstance(ProcessDefinition.getMostSpecializedClass((PPEInstance) inst), (PPEInstance) inst))
			.collect(Collectors.toSet());
	}

	@SuppressWarnings("unchecked")
	public Set<StepDefinition> getOutSteps() {
		return (Set<StepDefinition>) instance.getTypedProperty(DecisionNodeDefinitionType.CoreProperties.outSteps.toString(), Set.class).stream()
			.filter(PPEInstance.class::isInstance)
			.map(PPEInstance.class::cast)
			.map(inst -> context.getWrappedInstance(ProcessDefinition.getMostSpecializedClass((PPEInstance) inst), (PPEInstance) inst))
			.collect(Collectors.toSet());
	}

	public void setDepthIndexRecursive(int indexToSet) {
		instance.setSingleProperty(DecisionNodeDefinitionType.CoreProperties.hierarchyDepth.toString(), indexToSet);
		int newIndex = this.getOutSteps().size() > 1 ? indexToSet +1 : indexToSet; // we only increase the depth when we branch out
		this.getOutSteps().stream().forEach(step -> step.setDepthIndexRecursive(newIndex));
	}

	public Integer getDepthIndex() {
		return instance.getTypedProperty(DecisionNodeDefinitionType.CoreProperties.hierarchyDepth.toString(), Integer.class, -1);
	}

	@Override
	public void deleteCascading() {
		this.getMappings().forEach(md -> md.deleteCascading());
		// no instanceType for DNI to delete, all processes use the same one.
		super.deleteCascading();
	}

	public List<ProcessDefinitionError> checkDecisionNodeStructureValidity() {
		 List<ProcessDefinitionError> errors = this.getMappings().stream()
			.flatMap(mapping -> checkResolvable(mapping).stream())
			.collect(Collectors.toList());
		return errors;
	}

	private List<ProcessDefinitionError> checkResolvable(MappingDefinition mapping) {
		List<ProcessDefinitionError> errors = new LinkedList<>();
		StepDefinition fromStep = this.getProcess().getStepDefinitionByName(mapping.getFromStepType());
		if (fromStep == null && !this.getProcess().getName().equals(mapping.getFromStepType())) {
			String reason = String.format("Source Step '%s' is not a known process or process step", mapping.getFromStepType());
			errors.add(new ProcessDefinitionError(this, "InterStepMapping Invalid", reason, ProcessDefinitionError.Severity.ERROR));
		} else {
			if (fromStep == null) {
				fromStep = this.getProcess();
				if (!fromStep.getExpectedInput().containsKey(mapping.getFromParameter())) {
					String reason = String.format("Source Process '%s' does not have an input property '%s' to be used as source ", mapping.getFromStepType(), mapping.getFromParameter());
					errors.add(new ProcessDefinitionError(this, "InterStepMapping Invalid", reason, ProcessDefinitionError.Severity.ERROR));
				}
			} else {
				if (!fromStep.getExpectedOutput().containsKey(mapping.getFromParameter())) {
					String reason = String.format("Source Step '%s' does not have an output property '%s' to be used as source", mapping.getFromStepType(), mapping.getFromParameter());
					errors.add(new ProcessDefinitionError(this, "InterStepMapping Invalid", reason, ProcessDefinitionError.Severity.ERROR));
				}
			}
		}
		StepDefinition toStep = this.getProcess().getStepDefinitionByName(mapping.getToStepType());
		if (toStep == null && !this.getProcess().getName().equals(mapping.getToStepType())) {
			String reason = String.format("Destination Step '%s' is not a known process or process step", mapping.getToStepType());
			errors.add(new ProcessDefinitionError(this, "InterStepMapping Invalid", reason, ProcessDefinitionError.Severity.ERROR));
		} else {
			if (toStep == null) {
				toStep = this.getProcess();
				if (!toStep.getExpectedOutput().containsKey(mapping.getToParameter())) {
					String reason = String.format("Destination Process '%s' does not have an input property '%s' to be used as destination  ", mapping.getToStepType(), mapping.getToParameter());
					errors.add(new ProcessDefinitionError(this, "InterStepMapping Invalid", reason, ProcessDefinitionError.Severity.ERROR));
				}
			} else {
				if (!toStep.getExpectedInput().containsKey(mapping.getToParameter())) {
					String reason = String.format("Destination Step '%s' does not have an output property '%s' to be used as destination ", mapping.getToStepType(), mapping.getToParameter());
					errors.add(new ProcessDefinitionError(this, "InterStepMapping Invalid", reason, ProcessDefinitionError.Severity.ERROR));
				}
			}
		}
		return errors;
	}

	public static enum InFlowType {
		AND, OR, XOR, SEQ;
	}
}
