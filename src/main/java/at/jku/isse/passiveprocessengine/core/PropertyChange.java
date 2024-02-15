package at.jku.isse.passiveprocessengine.core;

import lombok.Data;
import lombok.EqualsAndHashCode;

public class PropertyChange {

	public interface Update {
		
		public String getName();				
		public PPEInstance getInstance();		
		public Object getValue();		
	}
	
	@Data
	public static abstract class UpdateImpl implements Update {
		
		final String name;
		final PPEInstance instance;
		final Object value;
	}
	
	@Data
	@EqualsAndHashCode(callSuper=true)
	public static class Add extends UpdateImpl{

		public Add(String name, PPEInstance instance, Object value) {
			super(name, instance, value);
		}		
	}
	
	@Data
	@EqualsAndHashCode(callSuper=true)
	public static class Remove extends UpdateImpl {

		public Remove(String name, PPEInstance instance, Object value) {
			super(name, instance, value);
		}
	}
	
	@Data
	@EqualsAndHashCode(callSuper=true)
	public static class Set extends UpdateImpl {

		public Set(String name, PPEInstance instance, Object value) {
			super(name, instance, value);			
		}
	}
}
