package at.jku.isse.passiveprocessengine.core;

import java.util.Optional;
import java.util.Set;

public interface SchemaRegistry {

	/**
	 * @param fqnTypeId
	 * @return only the single InstanceType of the given typeId and 
	 * when it is not marked as deleted (empty if marked as deleted or no such InstanceType exists)
	 */
	public Optional<InstanceType> findNonDeletedInstanceTypeById(String fqnTypeId);
	
	/**
	 * @param fqnTypeId
	 * @return any InstanceType that currently exists and any former (now marked as deleted) instancetypes having the same typeId
	 */
	public Set<InstanceType> findAllInstanceTypesById(String fqnTypeId);
	
	
	/**
	 * @param fqnTypeId
	 * @param superTypes (optional)
	 * @return a new empty InstanceType, will mark any existing InstanceType as deleted
	 * fqnTypeId will be interpreted as a fully qualified name make use of '.' notation to create a hierarchical namespace (similar to java)
	 */
	public InstanceType createNewInstanceType(String fqnTypeId, InstanceType... superTypes);
	
	
	/**
	 * @param ruleName
	 * @param context
	 * @return returns a RuleDefinition that matches rule name and context and that is not deleted, 
	 * allows for existence of rule of different name but same rule string.
	 */
	public RuleDefinition getRuleByNameAndContext(String ruleName, InstanceType context);
	
	
	/**
	 * @return a set of all currently registered instance types that are not marked as deleted
	 */	
	public Set<InstanceType> getAllNonDeletedInstanceTypes();
}
