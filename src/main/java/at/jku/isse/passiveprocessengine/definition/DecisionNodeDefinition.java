package at.jku.isse.passiveprocessengine.definition;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import at.jku.isse.designspace.core.model.Cardinality;
import at.jku.isse.designspace.core.model.Instance;
import at.jku.isse.designspace.core.model.InstanceType;
import at.jku.isse.designspace.core.model.SetProperty;
import at.jku.isse.designspace.core.model.Workspace;
import at.jku.isse.passiveprocessengine.ProcessDefinitionScopedElement;
import at.jku.isse.passiveprocessengine.WrapperCache;
import at.jku.isse.passiveprocessengine.configurability.ProcessConfigBaseElementFactory;

public class DecisionNodeDefinition extends ProcessDefinitionScopedElement {

	public static enum CoreProperties {inFlowType, dataMappingDefinitions, inSteps, outSteps, hierarchyDepth, closingDN}
	
	public static final String designspaceTypeId = DecisionNodeDefinition.class.getSimpleName();
	
	public DecisionNodeDefinition(Instance instance) {
		super(instance);
	}

	public void setInflowType(InFlowType ift) {
		instance.getPropertyAsSingle(CoreProperties.inFlowType.toString()).set(ift.toString());
	}
	
	public InFlowType getInFlowType() {
		return InFlowType.valueOf((String) instance.getPropertyAsValueOrElse(CoreProperties.inFlowType.toString(), () -> InFlowType.AND.toString()));
	}
		
	
	public DecisionNodeDefinition getScopeClosingDecisionNodeOrNull() {
		if (this.getOutSteps().isEmpty())
			return null;
		else {			
			Instance dnd = instance.getPropertyAsInstance(CoreProperties.closingDN.toString());
			if (dnd != null)
				return WrapperCache.getWrappedInstance(DecisionNodeDefinition.class, dnd);
			else { 
				DecisionNodeDefinition closingDnd = determineScopeClosingDN();
				instance.getPropertyAsSingle(CoreProperties.closingDN.toString()).set(closingDnd.getInstance());		
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
		instance.getPropertyAsSet(CoreProperties.inSteps.toString()).add(sd.getInstance());
	}
	
	@SuppressWarnings("unchecked")
	protected void addOutStep(StepDefinition sd) {
		instance.getPropertyAsSet(CoreProperties.outSteps.toString()).add(sd.getInstance());
	}
	
	@SuppressWarnings("unchecked")
	public void addDataMappingDefinition(MappingDefinition md) {
		instance.getPropertyAsSet(CoreProperties.dataMappingDefinitions.toString()).add(md.getInstance());
	}
	
	@SuppressWarnings("unchecked")
	public Set<MappingDefinition> getMappings() {
		SetProperty<?> mdSet = instance.getPropertyAsSet(CoreProperties.dataMappingDefinitions.toString());
		if (mdSet != null && mdSet.get() != null) {
			return (Set<MappingDefinition>) mdSet.get().stream()
					.map(inst -> WrapperCache.getWrappedInstance(MappingDefinition.class, (Instance) inst))
					.collect(Collectors.toSet());
		} else return Collections.emptySet();
	}
	
	@SuppressWarnings("unchecked")
	public Set<StepDefinition> getInSteps() {
		return (Set<StepDefinition>) instance.getPropertyAsSet(CoreProperties.inSteps.toString()).stream()
			.filter(Instance.class::isInstance)
			.map(Instance.class::cast)
			.map(inst -> WrapperCache.getWrappedInstance(ProcessDefinition.getMostSpecializedClass((Instance) inst), (Instance) inst))
			.collect(Collectors.toSet());
	}
	
	@SuppressWarnings("unchecked")
	public Set<StepDefinition> getOutSteps() {
		return (Set<StepDefinition>) instance.getPropertyAsSet(CoreProperties.outSteps.toString()).stream()
			.filter(Instance.class::isInstance)
			.map(Instance.class::cast)
			.map(inst -> WrapperCache.getWrappedInstance(ProcessDefinition.getMostSpecializedClass((Instance) inst), (Instance) inst))
			.collect(Collectors.toSet());
	}
	
	public void setDepthIndexRecursive(int indexToSet) {		
		instance.getPropertyAsSingle(CoreProperties.hierarchyDepth.toString()).set(indexToSet);
		int newIndex = this.getOutSteps().size() > 1 ? indexToSet +1 : indexToSet; // we only increase the depth when we branch out	
		this.getOutSteps().stream().forEach(step -> step.setDepthIndexRecursive(newIndex));				
	}
	
	public Integer getDepthIndex() {
		return (Integer) instance.getPropertyAsValueOrElse(CoreProperties.hierarchyDepth.toString(), () -> -1);
	}
	
	@Override
	public void deleteCascading(ProcessConfigBaseElementFactory configFactory) {
		this.getMappings().forEach(md -> md.deleteCascading(configFactory));
		// no instanceType for DNI to delete, all processes use the same one.
		super.deleteCascading(configFactory);
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
			errors.add(new ProcessDefinitionError(this, "InterStepMapping Invalid", reason));
		} else {
			if (fromStep == null) { 
				fromStep = this.getProcess();
				if (!fromStep.getExpectedInput().containsKey(mapping.getFromParameter())) {
					String reason = String.format("Source Process '%s' does not have an input property '%s' to be used as source ", mapping.getFromStepType(), mapping.getFromParameter());
					errors.add(new ProcessDefinitionError(this, "InterStepMapping Invalid", reason));
				}
			} else {
				if (!fromStep.getExpectedOutput().containsKey(mapping.getFromParameter())) {
					String reason = String.format("Source Step '%s' does not have an output property '%s' to be used as source", mapping.getFromStepType(), mapping.getFromParameter());
					errors.add(new ProcessDefinitionError(this, "InterStepMapping Invalid", reason));
				}
			}
		}
		StepDefinition toStep = this.getProcess().getStepDefinitionByName(mapping.getToStepType());
		if (toStep == null && !this.getProcess().getName().equals(mapping.getToStepType())) {
			String reason = String.format("Destination Step '%s' is not a known process or process step", mapping.getToStepType());
			errors.add(new ProcessDefinitionError(this, "InterStepMapping Invalid", reason));
		} else {
			if (toStep == null) { 
				toStep = this.getProcess();
				if (!toStep.getExpectedOutput().containsKey(mapping.getToParameter())) {
					String reason = String.format("Destination Process '%s' does not have an input property '%s' to be used as destination  ", mapping.getToStepType(), mapping.getToParameter());
					errors.add(new ProcessDefinitionError(this, "InterStepMapping Invalid", reason));
				}
			} else {
				if (!toStep.getExpectedInput().containsKey(mapping.getToParameter())) {
					String reason = String.format("Destination Step '%s' does not have an output property '%s' to be used as destination ", mapping.getToStepType(), mapping.getToParameter());
					errors.add(new ProcessDefinitionError(this, "InterStepMapping Invalid", reason));
				}
			}
		}
		return errors;
	}
	
	public static InstanceType getOrCreateDesignSpaceCoreSchema(Workspace ws) {
		Optional<InstanceType> thisType = Optional.ofNullable(ws.TYPES_FOLDER.instanceTypeWithName(designspaceTypeId)); 
		//= ws.debugInstanceTypes().stream()
		//		.filter(it -> it.name().contentEquals(designspaceTypeId))
		//		.findAny();
			if (thisType.isPresent())
				return thisType.get();
			else {
				InstanceType typeDN = ws.createInstanceType(designspaceTypeId, ws.TYPES_FOLDER, ProcessDefinitionScopedElement.getOrCreateDesignSpaceCoreSchema(ws));
				typeDN.createPropertyType(CoreProperties.inFlowType.toString(), Cardinality.SINGLE, Workspace.STRING);
				typeDN.createPropertyType(CoreProperties.inSteps.toString(), Cardinality.SET, StepDefinition.getOrCreateDesignSpaceCoreSchema(ws));
				typeDN.createPropertyType(CoreProperties.outSteps.toString(), Cardinality.SET, StepDefinition.getOrCreateDesignSpaceCoreSchema(ws));
				typeDN.createPropertyType(CoreProperties.dataMappingDefinitions.toString(), Cardinality.SET, MappingDefinition.getOrCreateDesignSpaceCoreSchema(ws));
				typeDN.createPropertyType((CoreProperties.hierarchyDepth.toString()), Cardinality.SINGLE, Workspace.INTEGER);
				typeDN.createPropertyType(CoreProperties.closingDN.toString(), Cardinality.SINGLE, typeDN);
				return typeDN;
			}
	}

	public static DecisionNodeDefinition getInstance(String dndId, Workspace ws) {
		Instance instance = ws.createInstance(getOrCreateDesignSpaceCoreSchema(ws), dndId);
		// default SEQ
		instance.getPropertyAsSingle(CoreProperties.inFlowType.toString()).set(InFlowType.SEQ.toString());
		instance.getPropertyAsSingle(CoreProperties.hierarchyDepth.toString()).set(-1);
		return WrapperCache.getWrappedInstance(DecisionNodeDefinition.class, instance);
	}
	
	public static enum InFlowType {
		AND, OR, XOR, SEQ;
	}
}
