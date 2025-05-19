package at.jku.isse.passiveprocessengine.instance.types;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import at.jku.isse.artifacteventstreaming.schemasupport.Cardinalities;
import at.jku.isse.passiveprocessengine.core.AbstractTypeProvider;
import at.jku.isse.passiveprocessengine.rdfwrapper.NodeToDomainResolver;
import at.jku.isse.passiveprocessengine.rdfwrapper.RDFInstanceType;
import at.jku.isse.passiveprocessengine.rdfwrapper.RDFPropertyType.PrimitiveOrClassType;
import at.jku.isse.passiveprocessengine.definition.activeobjects.ProcessDefinition;
import at.jku.isse.passiveprocessengine.definition.types.ProcessDefinitionTypeFactory;
import at.jku.isse.passiveprocessengine.rdfwrapper.rule.RuleDefinitionService;
import at.jku.isse.passiveprocessengine.rdfwrapper.rule.RuleEnabledResolver;
import lombok.Data;


public class SpecificProcessConfigType extends AbstractTypeProvider {
	
	private ProcessDefinition processDef;
	private String prefix;
	private Set<PropertySchemaDTO> props;
	
	public SpecificProcessConfigType(RuleEnabledResolver schemaRegistry, ProcessDefinition processDef, String prefix, Set<PropertySchemaDTO> props) {
		super(schemaRegistry);
		this.processDef = processDef;
		this.prefix = prefix;
		this.props = props;
	}


	public void produceTypeProperties() {
		String subtypeName = getSubtypeName();
		Optional<RDFInstanceType> thisType = schemaRegistry.findNonDeletedInstanceTypeByFQN(subtypeName);
		if (thisType.isPresent()) {
			type.cacheSuperProperties();
			//schemaRegistry.registerTypeByName(thisType.get());
			this.type = thisType.get();
		} else {
			type = schemaRegistry.createNewInstanceType(subtypeName, schemaRegistry.findNonDeletedInstanceTypeByFQN(ProcessConfigBaseElementType.typeId).get());
			//schemaRegistry.registerTypeByName(type);			
							
			type.createSinglePropertyType("processDefinition", schemaRegistry.findNonDeletedInstanceTypeByFQN(ProcessDefinitionTypeFactory.typeId).get().getAsPropertyType());
			// augment config
			Map<PropertySchemaDTO, Boolean> result = new HashMap<>();
			props.forEach(prop -> result.put(prop, prop.addPropertyToType(type, schemaRegistry)));
			
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

		public PrimitiveOrClassType getInstanceType(NodeToDomainResolver schemaRegistry) {
			switch (instanceType) {
				case("STRING"): return  schemaRegistry.getMetaschemata().getPrimitiveTypesFactory().getStringType();
				case("BOOLEAN"): return schemaRegistry.getMetaschemata().getPrimitiveTypesFactory().getBooleanType();
				case("DATE"): return schemaRegistry.getMetaschemata().getPrimitiveTypesFactory().getDateType();
				case("INTEGER"): return schemaRegistry.getMetaschemata().getPrimitiveTypesFactory().getIntType();
				case("REAL"): return schemaRegistry.getMetaschemata().getPrimitiveTypesFactory().getFloatType();
				default:
					// complex type, FQN needed
					return schemaRegistry.findNonDeletedInstanceTypeByFQN(instanceType).map(el -> el.getAsPropertyType()).orElse(null);
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

		public boolean addPropertyToType(RDFInstanceType processConfig, RuleEnabledResolver schemaRegistry) {
			RDFInstanceType baseType = schemaRegistry.findNonDeletedInstanceTypeByFQN(ProcessConfigBaseElementType.typeId).get();
			
			if (processConfig != null					
					&& processConfig.isOfTypeOrAnySubtype(baseType)
					&& isValid(schemaRegistry)
					&& processConfig.getPropertyType(name) == null
					) {
				// TODO if map/set/list		
				processConfig.createSinglePropertyType(name, getInstanceType(schemaRegistry));
				if (!isRepairable()) {
					schemaRegistry.setPropertyRepairable(processConfig, name, isRepairable);
				}
				return true;
			} else
				return false;
		}

	}




}
