package at.jku.isse.passiveprocessengine.core;

import java.util.Collections;
import java.util.Set;

public abstract class BuildInType implements InstanceType {

	@Override
	public InstanceType getInstanceType() {
		return null;
	}

	@Override
	public void markAsDeleted() {
		// not supported
	}

	@Override
	public boolean isMarkedAsDeleted() {
		return false;
	}

	@Override
	public void setSingleProperty(String property, Object value) {
		// no op
	}

	@Override
	public <T> T getTypedProperty(String property, Class<T> clazz) {
		//no op
		return null;
	}

	@Override
	public <T> T getTypedProperty(String property, Class<T> clazz, T defaultValue) {
		// no op
		return null;
	}

	@Override
	public void createListPropertyType(String name, InstanceType complexType) {
		// no op
	}

	@Override
	public void createSetPropertyType(String name, InstanceType complexType) {
		// no op
	}
	
	@Override
	public void createMapPropertyType(String name, InstanceType keyType, InstanceType valueType) {
		// no op
	}
	
	@Override
	public void createSinglePropertyType(String name, InstanceType complexType) {
		// no op
	}
	
	@Override
	public boolean isOfTypeOrAnySubtype(InstanceType instanceToCompareTo) {		
		return false;
	}
	
	@Override
	public PropertyType getPropertyType(String propertyName) {
		// no op
		return null;
	}
	
	@Override
	public Set<InstanceType> getAllSubtypesRecursively() {
		return Collections.emptySet();
	}

	@Override
	public String getName() {
		return getId();
	}

	public static InstanceType STRING = new BuildInType() {
		@Override
		public String getId() {
			return "STRING";
		}
	};
	
	public static InstanceType INTEGER = new BuildInType() {
		@Override
		public String getId() {
			return "INTEGER";
		}
	};
	
	public static InstanceType BOOLEAN = new BuildInType() {
		@Override
		public String getId() {
			return "BOOLEAN";
		}
	};
	
	public static InstanceType FLOAT = new BuildInType() {
		@Override
		public String getId() {
			return "FLOAT";
		}
	};
	
	public static InstanceType DATE = new BuildInType() {
		@Override
		public String getId() {
			return "DATE";
		}
	};
	
	public static InstanceType RULE = new BuildInType() {
		@Override
		public String getId() {
			return "RULE";
		}
	};
	
	public static InstanceType METATYPE = new BuildInType() {
		@Override
		public String getId() {
			return "METATYPE";
		}
	};
	
}
