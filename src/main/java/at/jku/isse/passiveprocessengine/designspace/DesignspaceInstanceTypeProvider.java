package at.jku.isse.passiveprocessengine.designspace;

import java.util.Optional;

import at.jku.isse.passiveprocessengine.core.InstanceType;
import at.jku.isse.passiveprocessengine.definition.DefinitionFactory;
import at.jku.isse.passiveprocessengine.definition.DefinitionFactory.TypeProvider;
import at.jku.isse.passiveprocessengine.definition.ProcessDefinitionScopedElement;
import at.jku.isse.passiveprocessengine.definition.ProcessDefinitionScopedElement.CoreProperties;

public class DesignspaceInstanceTypeProvider {

	private Workspace ws;
	
	public DesignpaceInstanceTypeProvider(Workspace workspace) {
		this.ws = workspace;
	}
	
	public DefinitionFactory getTypeDefinitionFactory() {
		DefinitionFactory factory = new DefinitionFactory();
		factory.registerBaseType(ProcessDefinitionScopedElement.class, new TypeProvider() {

			@Override
			public InstanceType getType() {
				Optional<InstanceType> thisType = Optional.ofNullable(ws.TYPES_FOLDER.instanceTypeWithName(ProcessDefinitionScopedElement.typeId));
				if (thisType.isPresent())
					return thisType.get();
				else {
					InstanceType typeStep = ws.createInstanceType(ProcessDefinitionScopedElement.typeId, ws.TYPES_FOLDER);
					typeStep.createPropertyType(ProcessDefinitionScopedElement.CoreProperties.process.toString(), Cardinality.SINGLE, typeStep);
					typeStep.createPropertyType((ProcessDefinitionScopedElement.CoreProperties.orderIndex.toString()), Cardinality.SINGLE, Workspace.INTEGER);
					return typeStep;
				}
			}
			
		});
	}
}
