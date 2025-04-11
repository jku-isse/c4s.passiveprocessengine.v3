package at.jku.isse.passiveprocessengine.instance.types;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import at.jku.isse.artifacteventstreaming.schemasupport.Cardinalities;
import at.jku.isse.passiveprocessengine.core.BuildInType;
import at.jku.isse.passiveprocessengine.core.DomainTypesRegistry;
import at.jku.isse.passiveprocessengine.rdfwrapper.RDFInstanceType;
import at.jku.isse.passiveprocessengine.core.NodeToDomainResolver;
import at.jku.isse.passiveprocessengine.core.TypeProviderBase;
import at.jku.isse.passiveprocessengine.definition.activeobjects.ProcessDefinition;
import at.jku.isse.passiveprocessengine.definition.types.ProcessDefinitionType;
import at.jku.isse.passiveprocessengine.rdfwrapper.RDFInstanceType;
import at.jku.isse.passiveprocessengine.rdfwrapper.rule.RuleDefinitionService;
import lombok.Data;


public class SpecificProcessConfigType extends TypeProviderBase {
	
	private NodeToDomainResolver schemaRegistry;
	private ProcessDefinition processDef;
	private String prefix;
	private Set<PropertySchemaDTO> props;
	private RuleDefinitionService ruleFactory;
	
	public SpecificProcessConfigType(NodeToDomainResolver schemaRegistry, ProcessDefinition processDef, String prefix, Set<PropertySchemaDTO> props, RuleDefinitionService ruleFactory) {
		super(schemaRegistry);
		this.processDef = processDef;
		this.prefix = prefix;
		this.props = props;
		this.ruleFactory = ruleFactory;
	}

	@Override
	public void produceTypeProperties() {
		String subtypeName = getSubtypeName();
		Optional<RDFInstanceType> thisType = schemaRegistry.findNonDeletedInstanceTypeByFQN(subtypeName);
		if (thisType.isPresent()) {
			((RDFInstanceType) type).cacheSuperProperties();
			//schemaRegistry.registerTypeByName(thisType.get());
			this.type = thisType.get();
		} else {
			type = schemaRegistry.createNewInstanceType(subtypeName, schemaRegistry.findNonDeletedInstanceTypeByFQN(ProcessConfigBaseElementType.typeId));
			//schemaRegistry.registerTypeByName(type);			
							
			type.createSinglePropertyType("processDefinition", schemaRegistry.findNonDeletedInstanceTypeByFQN(ProcessDefinitionType.typeId));
			// augment config
			Map<PropertySchemaDTO, Boolean> result = new HashMap<>();
			props.forEach(prop -> result.put(prop, prop.addPropertyToType(type, schemaRegistry, schemaRegistry, ruleFactory)));
			
		}									
	}

	public String getSubtypeName() {
		return prefix+"_"+processDef.getName();
	}

	@Data
	public static class PropertySchemaDTO {
		final String name;
		final String instanceType;
		final String cardinality;
		Object defaultValue; // not supported yet
		boolean isRepairable = true; // not supported yet

		public RDFInstanceType getInstanceType(NodeToDomainResolver schemaRegistry) {
			switch (instanceType) {
				case("STRING"): return BuildInType.STRING;
				case("BOOLEAN"): return BuildInType.BOOLEAN;
				case("DATE"): return BuildInType.DATE;
				case("INTEGER"): return BuildInType.INTEGER;
				case("REAL"): return BuildInType.FLOAT;
				default:
					// complex type, FQN needed
					return schemaRegistry.findNonDeletedInstanceTypeByFQN(instanceType).orElse(null);
			}
		}

		public Cardinalities getCardinality() {
			try {
				return Cardinalities.valueOf(cardinality);
			} catch (Exception e) {
				return null;
			}
		}

		public boolean isValid(NodeToDomainResolver schemaRegistry) {
			return (getInstanceType(schemaRegistry) != null && getCardinality() != null);
		}

		public boolean addPropertyToType(RDFInstanceType processConfig, DomainTypesRegistry factory, NodeToDomainResolver schemaRegistry, RuleDefinitionService ruleFactory) {
			RDFInstanceType baseType = factory.findNonDeletedInstanceTypeByFQN(ProcessConfigBaseElementType.typeId);
			
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
