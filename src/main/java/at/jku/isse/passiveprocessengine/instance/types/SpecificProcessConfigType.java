package at.jku.isse.passiveprocessengine.instance.types;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import at.jku.isse.passiveprocessengine.core.BuildInType;
import at.jku.isse.passiveprocessengine.core.Instance;
import at.jku.isse.passiveprocessengine.core.InstanceType;
import at.jku.isse.passiveprocessengine.core.InstanceType.CARDINALITIES;
import at.jku.isse.passiveprocessengine.core.ProcessDomainTypesRegistry;
import at.jku.isse.passiveprocessengine.core.SchemaRegistry;
import at.jku.isse.passiveprocessengine.core.ProcessDomainTypesRegistry.TypeProvider;
import at.jku.isse.passiveprocessengine.core.RuleDefinitionFactory;
import at.jku.isse.passiveprocessengine.definition.activeobjects.ProcessDefinition;
import at.jku.isse.passiveprocessengine.definition.activeobjects.ProcessDefinitionScopedElement;
import at.jku.isse.passiveprocessengine.definition.serialization.DTOs;
import at.jku.isse.passiveprocessengine.instance.ProcessException;
import at.jku.isse.passiveprocessengine.instance.activeobjects.ProcessInstanceScopedElement;
import lombok.Data;


public class SpecificProcessConfigType implements TypeProvider {
	
	private SchemaRegistry schemaRegistry;
	private ProcessDefinition processDef;
	private String prefix;
	private Set<PropertySchemaDTO> props;
	private RuleDefinitionFactory ruleFactory;
	
	public SpecificProcessConfigType(SchemaRegistry schemaRegistry, ProcessDefinition processDef, String prefix, Set<PropertySchemaDTO> props, RuleDefinitionFactory ruleFactory) {
		this.schemaRegistry = schemaRegistry;
		this.processDef = processDef;
		this.prefix = prefix;
		this.props = props;
		this.ruleFactory = ruleFactory;
	}

	@Override
	public void registerTypeInFactory(ProcessDomainTypesRegistry factory) {
		String subtypeName = getSubtypeName();
		Optional<InstanceType> thisType = schemaRegistry.findNonDeletedInstanceTypeById(subtypeName);
		if (thisType.isPresent())
			factory.registerTypeByName(thisType.get());
		else {
			InstanceType type = schemaRegistry.createNewInstanceType(subtypeName, factory.getTypeByName(ProcessConfigBaseElementType.typeId));
			factory.registerTypeByName(type);			
							
			type.createSinglePropertyType("processDefinition", factory.getType(ProcessDefinitionScopedElement.class));
			// augment config
			Map<PropertySchemaDTO, Boolean> result = new HashMap<>();
			props.forEach(prop -> result.put(prop, prop.addPropertyToType(type, factory, schemaRegistry, ruleFactory)));
		}									
	}

	public String getSubtypeName() {
		return prefix+"_"+processDef.getName();
	}

//	public Map<PropertySchemaDTO, Boolean> augmentConfig(Set<PropertySchemaDTO> props, InstanceType configType) {
//		Map<PropertySchemaDTO, Boolean> result = new HashMap<>();
//		props.forEach(prop -> result.put(prop, prop.addPropertyToType(configType, this)));
//		return result;
//	}



	@Data
	public static class PropertySchemaDTO {
		final String name;
		final String instanceType;
		final String cardinality;
		Object defaultValue; // not supported yet
		boolean isRepairable = true; // not supported yet

		public InstanceType getInstanceType(SchemaRegistry schemaRegistry) {
			switch (instanceType) {
				case("STRING"): return BuildInType.STRING;
				case("BOOLEAN"): return BuildInType.BOOLEAN;
				case("DATE"): return BuildInType.DATE;
				case("INTEGER"): return BuildInType.INTEGER;
				case("REAL"): return BuildInType.FLOAT;
				default:
					// complex type, FQN needed
					return schemaRegistry.findNonDeletedInstanceTypeById(instanceType).orElse(null);
			}
		}

		public CARDINALITIES getCardinality() {
			try {
				return CARDINALITIES.valueOf(cardinality);
			} catch (Exception e) {
				return null;
			}
		}

		public boolean isValid(SchemaRegistry schemaRegistry) {
			return (getInstanceType(schemaRegistry) != null && getCardinality() != null);
		}

		public boolean addPropertyToType(InstanceType processConfig, ProcessDomainTypesRegistry factory, SchemaRegistry schemaRegistry, RuleDefinitionFactory ruleFactory) {
			InstanceType baseType = factory.getTypeByName(ProcessConfigBaseElementType.typeId);
			
			if (processConfig != null
					&& factory != null
					&& processConfig.isOfTypeOrAnySubtype(baseType)
					&& isValid(schemaRegistry)
					&& processConfig.getPropertyType(name) == null
					) {

				// TODO if map/set/list		
				processConfig.createSinglePropertyType(name, getInstanceType(schemaRegistry));
				if (!isRepairable()) {
					ruleFactory.setPropertyRepairable(processConfig, name, isRepairable);
				}
				return true;
			} else
				return false;
		}

	}




}
