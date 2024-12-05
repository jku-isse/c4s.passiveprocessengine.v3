package at.jku.isse.passiveprocessengine.instance.serialization;

import static java.util.stream.Collectors.toSet;
import static java.util.stream.Stream.concat;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Stream;

import com.google.gson.stream.JsonWriter;

import at.jku.isse.passiveprocessengine.core.PPEInstance;
import at.jku.isse.passiveprocessengine.core.PPEInstanceType;
import at.jku.isse.passiveprocessengine.core.ProcessContext;
import at.jku.isse.passiveprocessengine.core.serialization.ConfigurablePropertyTypeAdapter;
import at.jku.isse.passiveprocessengine.core.serialization.TypeAdapterRegistry;
import at.jku.isse.passiveprocessengine.definition.activeobjects.StepDefinition;
import at.jku.isse.passiveprocessengine.instance.activeobjects.ProcessStep;
import at.jku.isse.passiveprocessengine.instance.types.AbstractProcessStepType;
import at.jku.isse.passiveprocessengine.instance.types.SpecificProcessStepType;

public class StepTypeAdapter extends ConfigurablePropertyTypeAdapter {
	
	protected final ProcessContext context;
	protected final PPEInstanceType baseStepType;
	
	public StepTypeAdapter(TypeAdapterRegistry registry, 
			Set<String> propertiesToSerializeShallow,
			Set<String> propertiesToSerializeDeep,
			ProcessContext context) {
		super(registry, propertiesToSerializeShallow, propertiesToSerializeDeep);
		this.context = context;
		baseStepType = context.getSchemaRegistry().getTypeByName(AbstractProcessStepType.typeId);
		assert(baseStepType != null);
	}
	
	@Override
	public void write(JsonWriter out, PPEInstance value) throws IOException {
		out.beginObject();
		writeThisInstance(value, out);
		PPEInstanceType type = value.getInstanceType();
		
		Set<String> propertiesToSerializeDeepExtended = extendWithInputOutput(type, value);
		
		type.getPropertyNamesIncludingSuperClasses().stream()
		.filter(propName -> propertiesToSerializeDeepExtended.contains(propName) 
				|| propertiesToSerializeShallow.contains(propName))
		.map(propName -> type.getPropertyType(propName))
		.forEach(propType -> {
			try {
				if (propertiesToSerializeDeepExtended.contains(propType.getName())) {
					deepPropertyWriter.writeProperty(propType, value, out);
				} else {
					propertyWriter.writeProperty(propType, value, out);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
		out.endObject();
	}

	private Set<String> extendWithInputOutput(PPEInstanceType type, PPEInstance instance) {
		if (type.isOfTypeOrAnySubtype(baseStepType)) {
			ProcessStep step = context.getWrappedInstance(ProcessStep.class, instance);
			StepDefinition stepDef = step.getDefinition();
			Stream<String> ioProperties = concat(stepDef.getExpectedInput().keySet().stream()
					.map(param -> SpecificProcessStepType.PREFIX_IN+param),
				stepDef.getExpectedOutput().keySet().stream()
					.map(param -> SpecificProcessStepType.PREFIX_OUT+param));
			return concat(ioProperties, propertiesToSerializeDeep.stream()).collect(toSet());
		} else {
			return propertiesToSerializeDeep;
		}
		
	}
	
	

}
