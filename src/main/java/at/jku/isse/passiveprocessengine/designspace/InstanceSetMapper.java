package at.jku.isse.passiveprocessengine.designspace;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import at.jku.isse.passiveprocessengine.core.PPEInstance;

public class InstanceSetMapper implements Set<PPEInstance>{
	private final Set<at.jku.isse.designspace.core.model.Instance> delegate;
	private final DesignSpaceSchemaRegistry registry;
	
	public InstanceSetMapper(Set<at.jku.isse.designspace.core.model.Instance> delegate, DesignSpaceSchemaRegistry registry) {
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

	public boolean contains(Object o) {
		if (o instanceof PPEInstance) {
			return delegate.contains(registry.mapProcessDomainInstanceToDesignspaceInstance((PPEInstance)o));
		} else {
			return delegate.contains(o);
		}
	}

	public Iterator<PPEInstance> iterator() {
		throw new RuntimeException("Not supported");	
	}

	public Object[] toArray() {
		throw new RuntimeException("Not supported");
		//FIXME
		//return delegate.toArray();
	}

	public <T> T[] toArray(T[] a) {
		throw new RuntimeException("Not supported");
		//FIXME
		//return delegate.toArray(a);
	}

	public boolean add(PPEInstance e) {
		return delegate.add((at.jku.isse.designspace.core.model.Instance)registry.mapProcessDomainInstanceToDesignspaceInstance((PPEInstance)e));
	}

	public boolean remove(Object o) {
		return delegate.remove((at.jku.isse.designspace.core.model.Instance)registry.mapProcessDomainInstanceToDesignspaceInstance((PPEInstance)o));
	}

	public boolean containsAll(Collection<?> c) {
		return delegate.containsAll(c.stream()
				.filter(obj -> obj instanceof PPEInstance)
				.map(PPEInstance.class::cast)
				.map(inst -> registry.mapProcessDomainInstanceToDesignspaceInstance(inst)).collect(Collectors.toList()));	
	}

	public boolean addAll(Collection<? extends PPEInstance> c) {
		return delegate.addAll(c.stream().map(inst -> (at.jku.isse.designspace.core.model.Instance)registry.mapProcessDomainInstanceToDesignspaceInstance(inst)).collect(Collectors.toList()));
	}

	public boolean retainAll(Collection<?> c) {
		return delegate.retainAll(c.stream()
				.filter(obj -> obj instanceof PPEInstance)
				.map(PPEInstance.class::cast)
				.map(inst -> registry.mapProcessDomainInstanceToDesignspaceInstance(inst)).collect(Collectors.toList()));
	}

	public boolean removeAll(Collection<?> c) {
		return delegate.removeAll(c.stream()
				.filter(obj -> obj instanceof PPEInstance)
				.map(PPEInstance.class::cast)
				.map(inst -> registry.mapProcessDomainInstanceToDesignspaceInstance(inst)).collect(Collectors.toList()));
	}

	public void clear() {
		delegate.clear();
	}

	public boolean equals(Object o) {
		return delegate.equals(o);
	}

	public int hashCode() {
		return delegate.hashCode();
	}

	public Spliterator<PPEInstance> spliterator() {
		throw new RuntimeException("Not supported");
		//return delegate.spliterator();
	}

	public <T> T[] toArray(IntFunction<T[]> generator) {
		throw new RuntimeException("Not supported");
		//return delegate.toArray(generator);
	}

	public boolean removeIf(Predicate<? super PPEInstance> filter) {
		throw new RuntimeException("Not supported");
		//return delegate.removeIf(filter);
	}

	public Stream<PPEInstance> stream() {
		return delegate.stream().map(dsInst -> registry.getWrappedInstance(dsInst));
	}

	public Stream<PPEInstance> parallelStream() {
		return delegate.parallelStream().map(dsInst -> registry.getWrappedInstance(dsInst));
	}

	



	
	
	
}
