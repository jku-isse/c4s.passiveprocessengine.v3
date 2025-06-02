package at.jku.isse.passiveprocessengine;

import at.jku.isse.artifacteventstreaming.api.exceptions.BranchConfigurationException;
import at.jku.isse.artifacteventstreaming.api.exceptions.PersistenceException;
import at.jku.isse.passiveprocessengine.core.ProcessEngineConfigurationBuilder;
import at.jku.isse.passiveprocessengine.definition.registry.ProcessRegistry;
import at.jku.isse.passiveprocessengine.instance.ProcessInstanceChangeProcessor;
import at.jku.isse.passiveprocessengine.instance.messages.EventDistributor;
import at.jku.isse.passiveprocessengine.monitoring.CurrentSystemTimeProvider;
import at.jku.isse.passiveprocessengine.monitoring.ProcessQAStatsMonitor;
import at.jku.isse.passiveprocessengine.rdfwrapper.PrimitiveTypesFactory;
import at.jku.isse.passiveprocessengine.rdfwrapper.config.AbstractEventStreamingSetup;
import at.jku.isse.passiveprocessengine.rdfwrapper.events.ChangeEventTransformer;
import at.jku.isse.passiveprocessengine.rdfwrapper.events.ChangeListener;
import at.jku.isse.passiveprocessengine.rdfwrapper.rule.RuleEnabledResolver;
import lombok.Getter;

@Getter
public class PPERuntime {

	final AbstractEventStreamingSetup wrapperFactory;
	final ProcessEngineConfigurationBuilder configBuilder;
	public final RuleEnabledResolver schemaReg;
	final PrimitiveTypesFactory primitives;
	final ProcessRegistry procReg;
	final ChangeListener picp;
	final ProcessQAStatsMonitor monitor;
	
	public PPERuntime(AbstractEventStreamingSetup branchWrapper) {
		super();
		this.wrapperFactory = branchWrapper;
		
		schemaReg = wrapperFactory.getResolver();
		primitives = schemaReg.getMetaschemata().getPrimitiveTypesFactory();
		configBuilder = new ProcessEngineConfigurationBuilder(
				schemaReg
				, wrapperFactory.getCoreTypeFactory());
		procReg = new ProcessRegistry(schemaReg, configBuilder.getFactoryIndex());	
		EventDistributor eventDistrib = new EventDistributor();
		monitor = new ProcessQAStatsMonitor(new CurrentSystemTimeProvider());
		eventDistrib.registerHandler(monitor);
		picp = new ProcessInstanceChangeProcessor(schemaReg, eventDistrib);
		ChangeEventTransformer picpWrapper = wrapperFactory.getChangeEventTransformer();
		picpWrapper.registerWithBranch(picp);
	}

	public void signalSetupComplete() throws PersistenceException, BranchConfigurationException {
		wrapperFactory.signalExternalSetupComplete();
	}
}
