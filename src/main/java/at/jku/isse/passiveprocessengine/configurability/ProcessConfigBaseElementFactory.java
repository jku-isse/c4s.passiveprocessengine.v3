package at.jku.isse.passiveprocessengine.configurability;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import at.jku.isse.designspace.core.model.Cardinality;
import at.jku.isse.designspace.core.model.Folder;
import at.jku.isse.designspace.core.model.Instance;
import at.jku.isse.designspace.core.model.InstanceType;
import at.jku.isse.designspace.core.model.Workspace;
import at.jku.isse.designspace.core.service.WorkspaceService;
import at.jku.isse.designspace.rule.checker.ConsistencyUtils;
import at.jku.isse.passiveprocessengine.ProcessDefinitionScopedElement;
import at.jku.isse.passiveprocessengine.ProcessInstanceScopedElement;
import at.jku.isse.passiveprocessengine.definition.ProcessDefinition;
import at.jku.isse.passiveprocessengine.instance.ProcessException;
import lombok.Data;


public class ProcessConfigBaseElementFactory {
	     			
	private Workspace ws;
	private InstanceType baseType;
	private Folder configFolder;
	public static String TYPENAME = "process_config_base";
	
	public ProcessConfigBaseElementFactory(Workspace ws) {
		this.ws = ws;
		init("processConfig");
	}
	
	private void init(String typesFolderName) {
		Folder typesFolder = ws.TYPES_FOLDER;
        configFolder = typesFolder.subfolder(typesFolderName);
        if (configFolder == null) {
        	configFolder = WorkspaceService.createSubfolder(ws, ws.TYPES_FOLDER, typesFolderName);
        }
        
        baseType = configFolder.instanceTypeWithName(TYPENAME);
        if (baseType == null) {
        	baseType = ws.createInstanceType(TYPENAME, configFolder, ProcessInstanceScopedElement.getOrCreateDesignSpaceCoreSchema(ws));
        	ProcessInstanceScopedElement.addGenericProcessProperty(baseType);
        	baseType.createPropertyType("description", Cardinality.SINGLE, Workspace.STRING);
        }
	}
	
	public InstanceType getBaseType() {
		return baseType;
	}		
	
	public InstanceType getOrCreateProcessSpecificSubtype(String name, ProcessDefinition procDef) {
		String subtypeName = getSubtypeName(name, procDef);
		InstanceType subType = configFolder.instanceTypeWithName(subtypeName);
		if (subType == null) {
			subType = ws.createInstanceType(subtypeName, configFolder, baseType);
			subType.createPropertyType("processDefinition", Cardinality.SINGLE, ProcessDefinitionScopedElement.getOrCreateDesignSpaceCoreSchema(ws));
		}
		return subType;
	}
	
	private String getSubtypeName(String name, ProcessDefinition procDef) {
		return name+"_"+procDef.getName();
	}
			
	public Map<PropertySchemaDTO, Boolean> augmentConfig(Set<PropertySchemaDTO> props, InstanceType configType) {
		Map<PropertySchemaDTO, Boolean> result = new HashMap<>();
		props.forEach(prop -> result.put(prop, prop.addPropertyToType(configType, this)));
		return result;
	}
	
	public Instance createConfigInstance(String name, InstanceType configSubType) {
		// any other logic such as default values etc, not implemented at the moment
		return configSubType.instantiate(name);
	}
	
	public Instance createConfigInstance(String name, String subtypeName) throws ProcessException{
		InstanceType subType = configFolder.instanceTypeWithName(subtypeName);
		if (subType == null) {
			throw new ProcessException("Configuration Subtyp "+subtypeName+" does not exist");
		} else {
			return createConfigInstance(name, subType);
		}
	}
	
	@Data
	public static class PropertySchemaDTO {
		final String name;
		final String instanceType;
		final String cardinality;				
		Object defaultValue; // not supported yet
		boolean isRepairable = true; // not supported yet
		
		public InstanceType getInstanceType(Workspace ws) {
			switch (instanceType) {
				case("STRING"): return Workspace.STRING;
				case("BOOLEAN"): return Workspace.BOOLEAN;
				case("DATE"): return Workspace.DATE;
				case("INTEGER"): return Workspace.INTEGER;
				case("REAL"): return Workspace.REAL;
				default: 
					// complex type, FQN needed
					return ws.instanceTypeWithQualifiedName(instanceType);										
			}
		}
		
		public Cardinality getCardinality() {
			try {
				return Cardinality.valueOf(cardinality);
			} catch (Exception e) {
				return null;
			}
		}
		
		public boolean isValid(Workspace ws) {
			return (getInstanceType(ws) != null && getCardinality() != null);
		}
		
		public boolean addPropertyToType(InstanceType processConfig, ProcessConfigBaseElementFactory factory) {
			if (processConfig != null 
					&& factory != null 
					&& processConfig.getAllSuperTypes().contains(factory.baseType) 
					&& isValid(processConfig.workspace)
					&& processConfig.getPropertyTypes(true, true).stream().map(pt -> pt.name()).noneMatch(pname -> pname.equalsIgnoreCase(name))
					) {
				
				processConfig.createPropertyType(name, getCardinality(), getInstanceType(processConfig.workspace));
				if (!isRepairable()) {
					ConsistencyUtils.setPropertyRepairable(processConfig, name, isRepairable);
				}
				return true;
			} else
				return false;
		}
		
	}
	
	
}
