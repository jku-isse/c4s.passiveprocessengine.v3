package at.jku.isse.passiveprocessengine.designspace;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import at.jku.isse.passiveprocessengine.core.Instance;

public class InstanceSetMapper{
	private final Set<at.jku.isse.designspace.core.model.Instance> delegate;
	private final DesignSpaceSchemaRegistry registry;
	
	public InstanceSetMapper(Set<at.jku.isse.designspace.core.model.Instance> delegate, DesignSpaceSchemaRegistry registry) {
		super();
		this.delegate = delegate;
		this.registry = registry;
	}
	
	public void forEach(Consumer<Instance> action) {
		throw new RuntimeException("Not supported");
	}

	public int size() {
		return delegate.size();
	}

	public boolean isEmpty() {
		return delegate.isEmpty();
	}

	public boolean contains(Object o) {
		if (o instanceof Instance) {
			return delegate.contains(registry.mapProcessDomainInstanceToDesignspaceInstance((Instance)o));
		} else {
			return delegate.contains(o);
		}
	}

	public Iterator<at.jku.isse.designspace.core.model.Instance> iterator() {
		return delegate.iterator();
	}

	public Object[] toArray() {
		return delegate.toArray();
	}

	public <T> T[] toArray(T[] a) {
		return delegate.toArray(a);
	}

	public boolean add(Instance e) {
		return delegate.add(registry.mapProcessDomainInstanceToDesignspaceInstance((Instance)e));
	}

	public boolean remove(Object o) {
		return delegate.remove(o);
	}

	public boolean containsAll(Collection<?> c) {
		//return delegate.containsAll(c);
		throw new RuntimeException("Not supported");
	}

	public boolean addAll(Collection<? extends Instance> c) {
		return delegate.addAll(c.stream().map(inst -> registry.mapProcessDomainInstanceToDesignspaceInstance(inst)).collect(Collectors.toList()));
	}

	public boolean retainAll(Collection<Instance> c) {
		return delegate.retainAll(c.stream().map(inst -> registry.mapProcessDomainInstanceToDesignspaceInstance(inst)).collect(Collectors.toList()));
	}

	public boolean removeAll(Collection<Instance> c) {
		return delegate.removeAll(c.stream().map(inst -> registry.mapProcessDomainInstanceToDesignspaceInstance(inst)).collect(Collectors.toList()));
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

	public Spliterator<at.jku.isse.designspace.core.model.Instance> spliterator() {
		throw new RuntimeException("Not supported");
		//return delegate.spliterator();
	}

	public <T> T[] toArray(IntFunction<T[]> generator) {
		throw new RuntimeException("Not supported");
		//return delegate.toArray(generator);
	}

	public boolean removeIf(Predicate<? super at.jku.isse.designspace.core.model.Instance> filter) {
		throw new RuntimeException("Not supported");
		//return delegate.removeIf(filter);
	}

	public Stream<at.jku.isse.designspace.core.model.Instance> stream() {
		return delegate.stream();
	}

	public Stream<at.jku.isse.designspace.core.model.Instance> parallelStream() {
		return delegate.parallelStream();
	}

	



	
	
	
}
