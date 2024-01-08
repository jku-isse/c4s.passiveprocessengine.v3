package at.jku.isse.passiveprocessengine.designspace;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import at.jku.isse.passiveprocessengine.core.InstanceType;
import at.jku.isse.passiveprocessengine.core.SchemaRegistry;

public class DesignSpaceSchemaRegistry implements SchemaRegistry {

	private Workspace ws;
	
	public DesignSpaceSchemaRegistry(Workspace ws) {
		this.ws = ws;
	}
	
	@Override
	public Optional<InstanceType> findNonDeletedInstanceTypeById(String fqnTypeId) {
		// TODO Auto-generated method stub
		return Optional.empty();
	}

	@Override
	public Set<InstanceType> findAllInstanceTypesById(String fqnTypeId) {
		// TODO Auto-generated method stub
		return Collections.emptySet();
	}

	@Override
	public InstanceType createNewInstanceType(String fqnTypeId) {
		// TODO Auto-generated method stub
		return null;
	}

}
