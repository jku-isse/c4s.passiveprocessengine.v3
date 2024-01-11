package at.jku.isse.passiveprocessengine.core;

import java.util.Optional;
import java.util.Set;

public interface InstanceRepository {

	public Instance createInstance(String id, InstanceType type) ;
	
	public void concludeTransaction() ;
	
	public Optional<Instance> findInstanceyById(String id) ;
	
	public Set<Instance> getAllInstancesOfTypeOrSubtype(InstanceType type) ;
	
//	/**
//	 * @return SchemaRegistry responsible for managing all data types available in this repository
//	 */
//	public SchemaRegistry getSchemaRegistry() {
//		throw new RuntimeException();
//	}
}
