package at.jku.isse.passiveprocessengine.instance.factories;

import at.jku.isse.passiveprocessengine.Context;
import at.jku.isse.passiveprocessengine.core.InstanceRepository;
import at.jku.isse.passiveprocessengine.definition.types.ProcessDomainTypesRegistry;
import lombok.Data;

@Data
public class FactoryIndex {

	final ConstraintWrapperFactory constraintFactory;
	final DecisionNodeInstanceFactory decisionNodeInstanceFactory;
	final ProcessInstanceFactory processInstanceFactory;
	final ProcessStepInstanceFactory processStepFactory;
	
	
	
	public static abstract class DomainInstanceFactory {
		protected FactoryIndex factoryIndex;
		
		final InstanceRepository repository;
		final Context context;
		final ProcessDomainTypesRegistry typesFactory;
		
		public DomainInstanceFactory(InstanceRepository repository, Context context, ProcessDomainTypesRegistry typesFactory) {
			this.repository = repository;
			this.context = context;
			this.typesFactory = typesFactory;
		}
		
		public void inject(FactoryIndex index) {
			this.factoryIndex = index;
		}
	}
}
