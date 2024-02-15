package at.jku.isse.passiveprocessengine.designspace;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Spliterator;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import at.jku.isse.passiveprocessengine.core.PPEInstance;

public class InstanceListMapper implements List<PPEInstance>{
	private final List<at.jku.isse.designspace.core.model.Instance> delegate;
	private final DesignSpaceSchemaRegistry registry;
	
	public InstanceListMapper(List<at.jku.isse.designspace.core.model.Instance> delegate, DesignSpaceSchemaRegistry registry) {
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
		return delegate.contains((at.jku.isse.designspace.core.model.Instance)registry.mapProcessDomainInstanceToDesignspaceInstance((PPEInstance)o));
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
		return delegate.add((at.jku.isse.designspace.core.model.Instance)registry.mapProcessDomainInstanceToDesignspaceInstance(e));
	}

	public boolean remove(Object o) {
		return delegate.remove((at.jku.isse.designspace.core.model.Instance)registry.mapProcessDomainInstanceToDesignspaceInstance((PPEInstance)o));
	}

	public boolean containsAll(Collection<?> c) {
		return delegate.containsAll(c);
	}

	public boolean addAll(Collection<? extends PPEInstance> c) {
		return delegate.addAll(c.stream()
				.map(inst -> (at.jku.isse.designspace.core.model.Instance)registry.mapProcessDomainInstanceToDesignspaceInstance(inst))
				.collect(Collectors.toList()));		
	}

	public boolean addAll(int index, Collection<? extends PPEInstance> c) {
		if (c.isEmpty()) return false;
		else {
			List<PPEInstance> list = new ArrayList<>(c);
			Collections.reverse(list);
			list.stream().forEach(element -> add(index, element));
			return false;			
		}
	}

	public boolean removeAll(Collection<?> c) {
		return delegate.removeAll(c.stream()
				.filter(obj -> obj instanceof PPEInstance)
				.map(PPEInstance.class::cast)
				.map(inst -> registry.mapProcessDomainInstanceToDesignspaceInstance(inst)).collect(Collectors.toList()));	
	}

	public boolean retainAll(Collection<?> c) {
		return delegate.retainAll(c.stream()
				.filter(obj -> obj instanceof PPEInstance)
				.map(PPEInstance.class::cast)
				.map(inst -> registry.mapProcessDomainInstanceToDesignspaceInstance(inst)).collect(Collectors.toList()));	
	}

	public void replaceAll(UnaryOperator<PPEInstance> operator) {
		throw new RuntimeException("Not supported");
		//delegate.replaceAll(operator);
	}

	public <T> T[] toArray(IntFunction<T[]> generator) {
		throw new RuntimeException("Not supported");
		//return delegate.toArray(generator);
	}

	public void sort(Comparator<? super PPEInstance> c) {
		throw new RuntimeException("Not supported");
		//delegate.sort(c);
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

	public PPEInstance get(int index) {
		return registry.getWrappedInstance(delegate.get(index));
	}

	public PPEInstance set(int index, PPEInstance element) {
		return registry.getWrappedInstance(delegate.set(index, (at.jku.isse.designspace.core.model.Instance)registry.mapProcessDomainInstanceToDesignspaceInstance((PPEInstance)element)));
	}

	public void add(int index, PPEInstance element) {
		delegate.add(index, (at.jku.isse.designspace.core.model.Instance)registry.mapProcessDomainInstanceToDesignspaceInstance((PPEInstance)element));
	}

	public boolean removeIf(Predicate<? super PPEInstance> filter) {
		throw new RuntimeException("Not supported");
		//return delegate.removeIf(filter);
	}

	public PPEInstance remove(int index) {
		return registry.getWrappedInstance(delegate.remove(index));
	}

	public int indexOf(Object o) {
		return delegate.indexOf((at.jku.isse.designspace.core.model.Instance)registry.mapProcessDomainInstanceToDesignspaceInstance((PPEInstance)o));
	}

	public int lastIndexOf(Object o) {
		return delegate.lastIndexOf((at.jku.isse.designspace.core.model.Instance)registry.mapProcessDomainInstanceToDesignspaceInstance((PPEInstance)o));
	}

	public ListIterator<PPEInstance> listIterator() {
		throw new RuntimeException("Not supported");
		//return delegate.listIterator();
	}

	public ListIterator<PPEInstance> listIterator(int index) {
		throw new RuntimeException("Not supported");
		//return delegate.listIterator(index);
	}

	public List<PPEInstance> subList(int fromIndex, int toIndex) {
		return delegate.subList(fromIndex, toIndex).stream()
				.map(dsInst -> registry.getWrappedInstance(dsInst))
				.collect(Collectors.toList());
	}

	public Spliterator<PPEInstance> spliterator() {
		throw new RuntimeException("Not supported");
		//return delegate.spliterator();
	}

	public Stream<PPEInstance> stream() {
		return delegate.stream().map(dsInst -> registry.getWrappedInstance(dsInst));
	}

	public Stream<PPEInstance> parallelStream() {
		return delegate.parallelStream().map(dsInst -> registry.getWrappedInstance(dsInst));
	}

	
}
