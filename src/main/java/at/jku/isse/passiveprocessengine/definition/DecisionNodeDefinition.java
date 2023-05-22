package at.jku.isse.passiveprocessengine.definition;

import java.util.Collections;
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
import at.jku.isse.passiveprocessengine.definition.StepDefinition.CoreProperties;

public class DecisionNodeDefinition extends ProcessDefinitionScopedElement {

	public static enum CoreProperties {inFlowType, dataMappingDefinitions, inSteps, outSteps, hierarchyDepth}
	
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
	public void deleteCascading() {
		this.getMappings().forEach(md -> md.deleteCascading());
		// no instanceType for DNI to delete, all processes use the same one.
		super.deleteCascading();
	}
	
	public static InstanceType getOrCreateDesignSpaceCoreSchema(Workspace ws) {
		Optional<InstanceType> thisType = Optional.ofNullable(ws.TYPES_FOLDER.instanceTypeWithName(designspaceTypeId)); 
		//= ws.debugInstanceTypes().stream()
		//		.filter(it -> it.name().contentEquals(designspaceTypeId))
		//		.findAny();
			if (thisType.isPresent())
				return thisType.get();
			else {
				InstanceType typeStep = ws.createInstanceType(designspaceTypeId, ws.TYPES_FOLDER, ProcessDefinitionScopedElement.getOrCreateDesignSpaceCoreSchema(ws));
				typeStep.createPropertyType(CoreProperties.inFlowType.toString(), Cardinality.SINGLE, Workspace.STRING);
				typeStep.createPropertyType(CoreProperties.inSteps.toString(), Cardinality.SET, StepDefinition.getOrCreateDesignSpaceCoreSchema(ws));
				typeStep.createPropertyType(CoreProperties.outSteps.toString(), Cardinality.SET, StepDefinition.getOrCreateDesignSpaceCoreSchema(ws));
				typeStep.createPropertyType(CoreProperties.dataMappingDefinitions.toString(), Cardinality.SET, MappingDefinition.getOrCreateDesignSpaceCoreSchema(ws));
				typeStep.createPropertyType((CoreProperties.hierarchyDepth.toString()), Cardinality.SINGLE, Workspace.INTEGER);
				return typeStep;
			}
	}

	public static DecisionNodeDefinition getInstance(String dndId, Workspace ws) {
		Instance instance = ws.createInstance(getOrCreateDesignSpaceCoreSchema(ws), dndId);
		// default AND
		instance.getPropertyAsSingle(CoreProperties.inFlowType.toString()).set(InFlowType.AND.toString());
		instance.getPropertyAsSingle(CoreProperties.hierarchyDepth.toString()).set(-1);
		return WrapperCache.getWrappedInstance(DecisionNodeDefinition.class, instance);
	}
	
	public static enum InFlowType {
		AND, OR, XOR;
	}
}
