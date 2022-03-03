package at.jku.isse.passiveprocessengine;

import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import at.jku.isse.designspace.core.model.Id;
import at.jku.isse.designspace.core.model.Instance;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WrapperCache {
	
	private static final Map<Id, WeakReference<InstanceWrapper>> cache = new HashMap<>();
	
	
	@SuppressWarnings("unchecked")
	public static <T extends InstanceWrapper> T getWrappedInstance(Class<? extends InstanceWrapper> clazz, Instance instance) {
		assert(instance != null);
		if (cache.containsKey(instance.id()))
			return (T) cache.get(instance.id()).get();
		else
		try {
			// otherwise we take the constructor of that class that takes an Instance object as parameter
			// and create it, passing it the instance object
			// assumption: every managed class implements such an constructor, (otherwise will fail fast here anyway)
			T t = (T) clazz.getConstructor(Instance.class).newInstance(instance);
			t.ws = instance.workspace;
			cache.put(instance.id(), new WeakReference<InstanceWrapper>((InstanceWrapper) t));
			return t;
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
				| NoSuchMethodException | SecurityException e) {
			e.printStackTrace();
			log.error(e.getMessage());
		}
		return null;
	}
}
