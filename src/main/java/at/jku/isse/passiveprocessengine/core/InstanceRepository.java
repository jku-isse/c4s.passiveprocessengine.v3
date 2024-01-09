package at.jku.isse.passiveprocessengine.core;

import java.util.Optional;
import java.util.Set;

public class InstanceRepository {

	
	public Instance createInstance(String id, InstanceType type) {
		throw new RuntimeException();
	}
	
	public void concludeTransaction() {
		throw new RuntimeException();
	}
	
	public Optional<Instance> findInstanceyById(String id) {
		throw new RuntimeException();
	}
	
	public Set<Instance> getAllInstancesOfTypeOrSubtype(InstanceType type) {
		throw new RuntimeException();
	}
	
	/**
	 * @return SchemaRegistry responsible for managing all data types available in this repository
	 */
	public SchemaRegistry getSchemaRegistry() {
		throw new RuntimeException();
	}
}
