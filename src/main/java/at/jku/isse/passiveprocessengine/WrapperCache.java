package at.jku.isse.passiveprocessengine;

import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import at.jku.isse.designspace.sdk.core.model.Id;
import at.jku.isse.designspace.sdk.core.model.Instance;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WrapperCache {
	
	private static final Map<Id, WeakReference<InstanceWrapper>> cache = new HashMap<>();
	
	
	@SuppressWarnings("unchecked")
	public static <T> T getWrappedInstance(Class<? extends InstanceWrapper> clazz, Instance instance) {
		if (cache.containsKey(instance.id()))
			return (T) cache.get(instance.id()).get();
		else
		try {
			T t = (T) clazz.getConstructor(Instance.class).newInstance(instance);
			cache.put(instance.id(), new WeakReference<InstanceWrapper>((InstanceWrapper) t));
			return t;
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
				| NoSuchMethodException | SecurityException e) {
			e.printStackTrace();
			log.warn(e.getMessage());
		}
		return null;
	}
}
