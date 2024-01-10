package at.jku.isse.passiveprocessengine.core;

import lombok.Data;

public class PropertyChange {

	public interface Update {
		
		public String getName();				
		public Instance getInstance();		
		public Object getValue();		
	}
	
	@Data
	private static abstract class UpdateImpl implements Update {
		
		final String name;
		final Instance instance;
		final Object value;
	}
	
	public static class Add extends UpdateImpl{

		public Add(String name, Instance instance, Object value) {
			super(name, instance, value);
		}		
	}
	
	public static class Remove extends UpdateImpl {

		public Remove(String name, Instance instance, Object value) {
			super(name, instance, value);
		}
	}
	
	public static class Set extends UpdateImpl {

		public Set(String name, Instance instance, Object value) {
			super(name, instance, value);			
		}
	}
}
