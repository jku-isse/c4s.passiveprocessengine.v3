package at.jku.isse.passiveprocessengine.core;

import java.util.Collections;
import java.util.Set;

public abstract class BuildInType implements PPEInstanceType {

	@Override
	public PPEInstanceType getInstanceType() {
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
	public void createListPropertyType(String name, PPEInstanceType complexType) {
		// no op
	}

	@Override
	public void createSetPropertyType(String name, PPEInstanceType complexType) {
		// no op
	}
	
	@Override
	public void createMapPropertyType(String name, PPEInstanceType keyType, PPEInstanceType valueType) {
		// no op
	}
	
	@Override
	public void createSinglePropertyType(String name, PPEInstanceType complexType) {
		// no op
	}
	
	@Override
	public boolean isOfTypeOrAnySubtype(PPEInstanceType instanceToCompareTo) {		
		return false;
	}
	
	@Override
	public PPEPropertyType getPropertyType(String propertyName) {
		// no op
		return null;
	}
	
	@Override
	public void setInstanceType(PPEInstanceType childType) {
		// noop (cannot override the instancetype of a build in type
	}
	
	@Override
	public Set<PPEInstanceType> getAllSubtypesRecursively() {
		return Collections.emptySet();
	}

	@Override
	public String getName() {
		return getId();
	}

	public static PPEInstanceType STRING = new BuildInType() {
		@Override
		public String getId() {
			return "STRING";
		}


	};
	
	public static PPEInstanceType INTEGER = new BuildInType() {
		@Override
		public String getId() {
			return "INTEGER";
		}
	};
	
	public static PPEInstanceType BOOLEAN = new BuildInType() {
		@Override
		public String getId() {
			return "BOOLEAN";
		}
	};
	
	public static PPEInstanceType FLOAT = new BuildInType() {
		@Override
		public String getId() {
			return "FLOAT";
		}
	};
	
	public static PPEInstanceType DATE = new BuildInType() {
		@Override
		public String getId() {
			return "DATE";
		}
	};
	
	public static PPEInstanceType RULE = new BuildInType() {
		@Override
		public String getId() {
			return "RULE";
		}
	};
	
	public static PPEInstanceType METATYPE = new BuildInType() {
		@Override
		public String getId() {
			return "METATYPE";
		}
	};
	
}
