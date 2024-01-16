package at.jku.isse.passiveprocessengine.designspace;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import at.jku.isse.passiveprocessengine.core.Instance;

public class InstanceMapMapper implements Map<String, Instance> {
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
		return delegate.containsValue(registry.mapProcessDomainInstanceToDesignspaceInstance((Instance)value));
	}

	public Instance get(Object key) {
		return registry.getWrappedInstance(delegate.get(key));
	}

	public Instance put(String key, Instance value) {
		return registry.getWrappedInstance(delegate.put(key, registry.mapProcessDomainInstanceToDesignspaceInstance(value)));
	}

	public Instance remove(Object key) {
		return registry.getWrappedInstance(delegate.remove(key));
	}

	public void putAll(Map<? extends String, ? extends Instance> m) {
		m.entrySet().stream().forEach(entry -> put(entry.getKey(), entry.getValue()));		
	}

	public void clear() {
		delegate.clear();
	}

	public Set<String> keySet() {
		return delegate.keySet();
	}

	public Collection<Instance> values() {
		return delegate.values().stream()
				.map(dsInst -> registry.getWrappedInstance(dsInst))
				.collect(Collectors.toList());
	}

	public Set<Entry<String, Instance>> entrySet() {							
		return delegate.entrySet().stream()
				.map(entry ->  new AbstractMap.SimpleEntry<String, Instance>(entry.getKey(), registry.getWrappedInstance(entry.getValue())))
				.collect(Collectors.toSet());						
	}

	public boolean equals(Object o) {
		return delegate.equals(o);
	}

	public int hashCode() {
		return delegate.hashCode();
	}

	public Instance getOrDefault(Object key,
			Instance defaultValue) {
		return registry.getWrappedInstance(
				delegate.getOrDefault(key, 
						(at.jku.isse.designspace.core.model.Instance)registry.mapProcessDomainInstanceToDesignspaceInstance((Instance)defaultValue)));
	}

	public void forEach(BiConsumer<? super String, ? super Instance> action) {
		throw new RuntimeException("Not supported");	
		//delegate.forEach(action);
	}

	public void replaceAll(
			BiFunction<? super String, ? super Instance, ? extends Instance> function) {
		throw new RuntimeException("Not supported");	
		//delegate.replaceAll(function);
	}

	public Instance putIfAbsent(String key, Instance value) {
		return registry.getWrappedInstance(
				delegate.putIfAbsent(key, 
						(at.jku.isse.designspace.core.model.Instance)registry.mapProcessDomainInstanceToDesignspaceInstance((Instance)value)));
	}

	public boolean remove(Object key, Object value) {
		return delegate.remove(key, 
				registry.mapProcessDomainInstanceToDesignspaceInstance((Instance)value));
	}

	public boolean replace(String key, Instance oldValue,
			Instance newValue) {
		return delegate.replace(key 
				, (at.jku.isse.designspace.core.model.Instance)registry.mapProcessDomainInstanceToDesignspaceInstance((Instance)oldValue)
				, (at.jku.isse.designspace.core.model.Instance)registry.mapProcessDomainInstanceToDesignspaceInstance((Instance)newValue));
	}

	public Instance replace(String key, Instance value) {
		return registry.getWrappedInstance(
				delegate.replace(key, 
						(at.jku.isse.designspace.core.model.Instance)registry.mapProcessDomainInstanceToDesignspaceInstance((Instance)value)));
	}

	public Instance computeIfAbsent(String key,
			Function<? super String, ? extends Instance> mappingFunction) {
		throw new RuntimeException("Not supported");	
		//return delegate.computeIfAbsent(key, mappingFunction);
	}

	public Instance computeIfPresent(String key,
			BiFunction<? super String, ? super Instance, ? extends Instance> remappingFunction) {
		throw new RuntimeException("Not supported");	
		//return delegate.computeIfPresent(key, remappingFunction);
	}

	public Instance compute(String key,
			BiFunction<? super String, ? super Instance, ? extends Instance> remappingFunction) {
		throw new RuntimeException("Not supported");	
		//return delegate.compute(key, remappingFunction);
	}

	public Instance merge(String key,
			Instance value,
			BiFunction<? super Instance, ? super Instance, ? extends Instance> remappingFunction) {
		throw new RuntimeException("Not supported");	
		//return delegate.merge(key, value, remappingFunction);
	}

	
	
	
}
