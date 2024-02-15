package at.jku.isse.passiveprocessengine.designspace;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import at.jku.isse.passiveprocessengine.core.PPEInstance;

public class InstanceMapMapper implements Map<String, PPEInstance> {
	private final Map<String, at.jku.isse.designspace.core.model.Element> delegate;
	private final DesignSpaceSchemaRegistry registry;
	
	public InstanceMapMapper(Map<String, at.jku.isse.designspace.core.model.Element> delegate, DesignSpaceSchemaRegistry registry) {
		super();
		this.delegate = delegate;
		this.registry = registry;
	}
	
	public int size() {
		return delegate.size();
	}

	public boolean isEmpty() {
		return delegate.isEmpty();
	}

	public boolean containsKey(Object key) {
		return delegate.containsKey(key);
	}

	public boolean containsValue(Object value) {
		return delegate.containsValue(registry.mapProcessDomainInstanceToDesignspaceInstance((PPEInstance)value));
	}

	public PPEInstance get(Object key) {
		return registry.getWrappedInstance(delegate.get(key));
	}

	public PPEInstance put(String key, PPEInstance value) {
		return registry.getWrappedInstance(delegate.put(key, registry.mapProcessDomainInstanceToDesignspaceInstance(value)));
	}

	public PPEInstance remove(Object key) {
		return registry.getWrappedInstance(delegate.remove(key));
	}

	public void putAll(Map<? extends String, ? extends PPEInstance> m) {
		m.entrySet().stream().forEach(entry -> put(entry.getKey(), entry.getValue()));		
	}

	public void clear() {
		delegate.clear();
	}

	public Set<String> keySet() {
		return delegate.keySet();
	}

	public Collection<PPEInstance> values() {
		return delegate.values().stream()
				.map(dsInst -> registry.getWrappedInstance(dsInst))
				.collect(Collectors.toList());
	}

	public Set<Entry<String, PPEInstance>> entrySet() {							
		return delegate.entrySet().stream()
				.map(entry ->  new AbstractMap.SimpleEntry<String, PPEInstance>(entry.getKey(), registry.getWrappedInstance(entry.getValue())))
				.collect(Collectors.toSet());						
	}

	public boolean equals(Object o) {
		return delegate.equals(o);
	}

	public int hashCode() {
		return delegate.hashCode();
	}

	public PPEInstance getOrDefault(Object key,
			PPEInstance defaultValue) {
		return registry.getWrappedInstance(
				delegate.getOrDefault(key, 
						(at.jku.isse.designspace.core.model.Instance)registry.mapProcessDomainInstanceToDesignspaceInstance((PPEInstance)defaultValue)));
	}

	public void forEach(BiConsumer<? super String, ? super PPEInstance> action) {
		throw new RuntimeException("Not supported");	
		//delegate.forEach(action);
	}

	public void replaceAll(
			BiFunction<? super String, ? super PPEInstance, ? extends PPEInstance> function) {
		throw new RuntimeException("Not supported");	
		//delegate.replaceAll(function);
	}

	public PPEInstance putIfAbsent(String key, PPEInstance value) {
		return registry.getWrappedInstance(
				delegate.putIfAbsent(key, 
						(at.jku.isse.designspace.core.model.Instance)registry.mapProcessDomainInstanceToDesignspaceInstance((PPEInstance)value)));
	}

	public boolean remove(Object key, Object value) {
		return delegate.remove(key, 
				registry.mapProcessDomainInstanceToDesignspaceInstance((PPEInstance)value));
	}

	public boolean replace(String key, PPEInstance oldValue,
			PPEInstance newValue) {
		return delegate.replace(key 
				, (at.jku.isse.designspace.core.model.Instance)registry.mapProcessDomainInstanceToDesignspaceInstance((PPEInstance)oldValue)
				, (at.jku.isse.designspace.core.model.Instance)registry.mapProcessDomainInstanceToDesignspaceInstance((PPEInstance)newValue));
	}

	public PPEInstance replace(String key, PPEInstance value) {
		return registry.getWrappedInstance(
				delegate.replace(key, 
						(at.jku.isse.designspace.core.model.Instance)registry.mapProcessDomainInstanceToDesignspaceInstance((PPEInstance)value)));
	}

	public PPEInstance computeIfAbsent(String key,
			Function<? super String, ? extends PPEInstance> mappingFunction) {
		throw new RuntimeException("Not supported");	
		//return delegate.computeIfAbsent(key, mappingFunction);
	}

	public PPEInstance computeIfPresent(String key,
			BiFunction<? super String, ? super PPEInstance, ? extends PPEInstance> remappingFunction) {
		throw new RuntimeException("Not supported");	
		//return delegate.computeIfPresent(key, remappingFunction);
	}

	public PPEInstance compute(String key,
			BiFunction<? super String, ? super PPEInstance, ? extends PPEInstance> remappingFunction) {
		throw new RuntimeException("Not supported");	
		//return delegate.compute(key, remappingFunction);
	}

	public PPEInstance merge(String key,
			PPEInstance value,
			BiFunction<? super PPEInstance, ? super PPEInstance, ? extends PPEInstance> remappingFunction) {
		throw new RuntimeException("Not supported");	
		//return delegate.merge(key, value, remappingFunction);
	}

	
	
	
}
