package at.jku.isse.passiveprocessengine;

import java.util.HashMap;
import java.util.Map;

import at.jku.isse.passiveprocessengine.core.FactoryIndex;
import at.jku.isse.passiveprocessengine.core.Instance;
import at.jku.isse.passiveprocessengine.core.InstanceRepository;
import at.jku.isse.passiveprocessengine.core.ProcessDomainTypesRegistry;
import at.jku.isse.passiveprocessengine.core.SchemaRegistry;
import at.jku.isse.passiveprocessengine.definition.factories.DefinitionFactoryIndex;
import at.jku.isse.passiveprocessengine.instance.types.ProcessConfigBaseElementType;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public class Context {

	final InstanceRepository instanceRepository;
	final SchemaRegistry schemaRegistry;
	final ProcessDomainTypesRegistry typesFactory;
	final ProcessConfigBaseElementType configFactory;	
	final FactoryIndex factoryIndex;
				
	private final Map<String, InstanceWrapper> cache = new HashMap<>();
	
	public Context(InstanceRepository instanceRepository, SchemaRegistry schemaRegistry, ProcessDomainTypesRegistry typesFactory,
			ProcessConfigBaseElementType configFactory, FactoryIndex factoryIndex) {
		this.instanceRepository = instanceRepository;
		this.schemaRegistry = schemaRegistry;
		this.typesFactory = typesFactory;
		this.configFactory = configFactory;		
		this.factoryIndex = factoryIndex;
	}

	@SuppressWarnings("unchecked")
	public <T extends InstanceWrapper> T getWrappedInstance(Class<? extends InstanceWrapper> clazz, Instance instance) {
		//assert(instance != null);
		if (instance == null) {
			log.debug("WrapperCache was invoked with null instance");
			return null;
		} else {
		if (cache.containsKey(instance.getId())) {
			InstanceWrapper iw = cache.get(instance.getId());
			if (iw != null) {
				// ensure cast safety
				if (clazz.isInstance(iw)) {
					return (T) iw; // else we reenter this
				} else { // if we, e.g., ask for a ProcessInstance and get a ProcessStep here, we just continue below to create a new wrapper, and store that.
					log.warn(String.format("Found instance %s of type %s but needed of type %s", instance.getId(), iw.getClass().getSimpleName(), clazz.getSimpleName()));
				}
			}
		}
		try {
			// otherwise we take the constructor of that class that takes an Instance object as parameter
			// and create it, passing it the instance object
			// assumption: every managed class implements such an constructor, (otherwise will fail fast here anyway)
			T t = (T) clazz.getConstructor(Instance.class, Context.class).newInstance(instance, this);
			//t.ws = instance.workspace;
			cache.put(instance.getId(), t);
			return t;
		} catch (Exception e) {
			e.printStackTrace();
			log.error(e.getMessage());
		}
		return null;
		}
	}

	public void removeWrapper(String id) {
		cache.remove(id);
	}


}
