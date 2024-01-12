package at.jku.isse.passiveprocessengine.core;

import at.jku.isse.passiveprocessengine.InstanceWrapper;
import lombok.NonNull;

public interface DomainTypesRegistry {

	InstanceType getType(Class<? extends InstanceWrapper> clazz);

	void registerType(Class<? extends InstanceWrapper> clazz, InstanceType type);

	void registerTypeByName(InstanceType type);

	InstanceType getTypeByName(String typeName);

}