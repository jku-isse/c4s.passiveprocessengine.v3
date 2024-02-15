package at.jku.isse.passiveprocessengine.core;

import java.util.Optional;
import java.util.Set;

public interface InstanceRepository {

	public PPEInstance createInstance(String id, PPEInstanceType type) ;
	
	public void concludeTransaction() ;
	
	public Optional<PPEInstance> findInstanceyById(String id) ;
	
	public Set<PPEInstance> getAllInstancesOfTypeOrSubtype(PPEInstanceType type) ;
	
//	/**
//	 * @return SchemaRegistry responsible for managing all data types available in this repository
//	 */
//	public SchemaRegistry getSchemaRegistry() {
//		throw new RuntimeException();
//	}
}
