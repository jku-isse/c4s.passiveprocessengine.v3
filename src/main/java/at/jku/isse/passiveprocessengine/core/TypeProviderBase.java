package at.jku.isse.passiveprocessengine.core;

public abstract class TypeProviderBase implements TypeProvider {
	final protected SchemaRegistry schemaRegistry;
	
	public TypeProviderBase(SchemaRegistry schemaRegistry) {
		this.schemaRegistry = schemaRegistry;
	}
}