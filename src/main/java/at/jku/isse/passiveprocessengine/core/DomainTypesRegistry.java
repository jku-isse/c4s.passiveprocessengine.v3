package at.jku.isse.passiveprocessengine.core;

import at.jku.isse.passiveprocessengine.InstanceWrapper;

public interface DomainTypesRegistry {

	PPEInstanceType getType(Class<? extends InstanceWrapper> clazz);

	void registerType(Class<? extends InstanceWrapper> clazz, PPEInstanceType type);

	void registerTypeByName(PPEInstanceType type);

	PPEInstanceType getTypeByName(String typeName);

}