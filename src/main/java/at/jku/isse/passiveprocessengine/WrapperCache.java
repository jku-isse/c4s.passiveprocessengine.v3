package at.jku.isse.passiveprocessengine;

import java.util.HashMap;
import java.util.Map;

import at.jku.isse.designspace.core.model.Id;
import at.jku.isse.designspace.core.model.Instance;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WrapperCache {
	
//	private static final Map<Id, WeakReference<InstanceWrapper>> cache = new HashMap<>();
//	
//	
//	@SuppressWarnings("unchecked")
//	public static <T extends InstanceWrapper> T getWrappedInstance(Class<? extends InstanceWrapper> clazz, Instance instance) {
//		//assert(instance != null);
//		if (instance == null) { 
//			log.error("WrapperCache was invoked with null instance");
//			return null;
//		} else {
//		if (cache.containsKey(instance.id())) {
//			InstanceWrapper iw = cache.get(instance.id()).get();
//			if (iw != null) {
//				// ensure cast safety
//				if (clazz.isInstance(iw)) {
//					return (T) iw; // else we reenter this	
//				} // if we, e.g., ask for a ProcessInstance and get a ProcessStep here, we just continue below to create a new wrapper, and store that.									
//			}
//		}
//		try {
//			// otherwise we take the constructor of that class that takes an Instance object as parameter
//			// and create it, passing it the instance object
//			// assumption: every managed class implements such an constructor, (otherwise will fail fast here anyway)
//			T t = (T) clazz.getConstructor(Instance.class).newInstance(instance);
//			t.ws = instance.workspace;
//			cache.put(instance.id(), new WeakReference<InstanceWrapper>((InstanceWrapper) t));
//			return t;
//		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
//				| NoSuchMethodException | SecurityException e) {
//			e.printStackTrace();
//			log.error(e.getMessage());
//		}
//		return null;
//		}
//	}
	
	private static final Map<Id, InstanceWrapper> cache = new HashMap<>();
	
	
	@SuppressWarnings("unchecked")
	public static <T extends InstanceWrapper> T getWrappedInstance(Class<? extends InstanceWrapper> clazz, Instance instance) {
		//assert(instance != null);
		if (instance == null) { 
			log.debug("WrapperCache was invoked with null instance");
			return null;
		} else {
		if (cache.containsKey(instance.id())) {
			InstanceWrapper iw = cache.get(instance.id());
			if (iw != null) {
				// ensure cast safety
				if (clazz.isInstance(iw)) {
					return (T) iw; // else we reenter this	
				} else { // if we, e.g., ask for a ProcessInstance and get a ProcessStep here, we just continue below to create a new wrapper, and store that.
					log.warn(String.format("Found instance %s of type %s but needed of type %s", instance.name(), iw.getClass().getSimpleName(), clazz.getSimpleName()));
				}
			}
		}
		try {
			// otherwise we take the constructor of that class that takes an Instance object as parameter
			// and create it, passing it the instance object
			// assumption: every managed class implements such an constructor, (otherwise will fail fast here anyway)
			T t = (T) clazz.getConstructor(Instance.class).newInstance(instance);
			t.ws = instance.workspace;
			cache.put(instance.id(), (InstanceWrapper) t);
			return t;
		} catch (Exception e) {
			e.printStackTrace();
			log.error(e.getMessage());
		}
		return null;
		}
	}
	
	public static void removeWrapper(Id id) {
		cache.remove(id);
	}
}
