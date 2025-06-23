package at.jku.isse.passiveprocessengine;

import static org.junit.Assert.assertTrue;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.jena.ontapi.OntModelFactory;
import org.apache.jena.ontapi.model.OntModel;

import at.jku.isse.artifacteventstreaming.api.Branch;
import at.jku.isse.artifacteventstreaming.api.exceptions.BranchConfigurationException;
import at.jku.isse.artifacteventstreaming.api.exceptions.PersistenceException;
import at.jku.isse.passiveprocessengine.definition.activeobjects.ProcessDefinition;
import at.jku.isse.passiveprocessengine.definition.registry.DTOs;
import at.jku.isse.passiveprocessengine.demo.TestArtifacts;
import at.jku.isse.passiveprocessengine.demo.TestDTOProcesses;
import at.jku.isse.passiveprocessengine.instance.activeobjects.ProcessInstance;
import at.jku.isse.passiveprocessengine.instance.messages.Responses.IOResponse;
import at.jku.isse.passiveprocessengine.rdfwrapper.RDFInstance;
import at.jku.isse.passiveprocessengine.rdfwrapper.RDFInstanceType;
import at.jku.isse.passiveprocessengine.rdfwrapper.config.AbstractEventStreamingSetup;
import at.jku.isse.passiveprocessengine.rdfwrapper.config.FrontendEventStreamingWrapperFactory;
import at.jku.isse.passiveprocessengine.rdfwrapper.config.InMemoryEventStreamingSetupFactory;
import at.jku.isse.passiveprocessengine.rdfwrapper.config.PersistedEventStreamingSetupFactory;

public class TestPPERuntime extends PPERuntime {

	public static final String REPOURI = "http://ppetests";
	AtomicInteger procInstanceCounter = new AtomicInteger(0);
	public final Branch branch;
	public TestDTOProcesses procFactory;	
	public TestArtifacts artifactFactory;
	public RDFInstanceType typeJira;
	public OntModel loadedModelCopy = OntModelFactory.createModel();
	
	public TestPPERuntime(AbstractEventStreamingSetup branchWrapper) {
		super(branchWrapper);
		this.branch = wrapperFactory.getBranch();
		artifactFactory = new TestArtifacts(schemaReg);
		procFactory = new TestDTOProcesses(artifactFactory);
		typeJira = artifactFactory.getJiraInstanceType();
	}

	public ProcessInstance instantiateDefaultProcess(DTOs.Process procDTO, RDFInstance... inputs) {
		var result = procReg.createProcessDefinitionIfNotExisting(procDTO);
		assertTrue(result.getDefinitionErrors().isEmpty());
		ProcessDefinition procDef = result.getProcDef();		
		conclude();
		startWrite();

		ProcessInstance procInstance = configBuilder.getFactoryIndex().getProcessInstanceFactory().getInstance(procDef, "TEST"+procInstanceCounter.incrementAndGet());
		assert(procInstance != null);
		conclude();
		startWrite();
		for (RDFInstance input : inputs) {
			IOResponse resp = procInstance.addInput(TestDTOProcesses.JIRA_IN, input);
			assert(resp.getError() == null);
		}
		return procInstance;
	}
	
	public void startRead() {
		schemaReg.startReadTransaction();
	}
	
	public void startWrite() {
		schemaReg.startWriteTransaction();
	}
	
	public void conclude() {
		schemaReg.concludeTransaction();
	}
	
	public static TestPPERuntime buildInMemoryRuntime() throws Exception {
		var wrapperFactory = new InMemoryEventStreamingSetupFactory.FactoryBuilder()
				.withBranchName("main")
				.withRepoURI(new URI(REPOURI)).build();
		var runtime = new TestPPERuntime(wrapperFactory);
		runtime.signalSetupComplete();
		runtime.startRead();
		runtime.loadedModelCopy.add(runtime.branch.getModel());
		runtime.conclude();
		return runtime;
	}
	
	public static TestPPERuntime buildPersistentButNoEventDBRuntime() throws Exception {
		var wrapperFactory = new PersistedEventStreamingSetupFactory.TripleStoreOnlyFactoryBuilder()
				.withBranchName("main")
				.withRepoURI(new URI(REPOURI)).build();
		var runtime = new TestPPERuntime(wrapperFactory);
		runtime.signalSetupComplete();
		runtime.startRead();
		runtime.loadedModelCopy.add(runtime.branch.getModel());
		runtime.conclude();
		return runtime;
	}
	
	public static TestPPERuntime buildWithProvidedStreamingFactory(AbstractEventStreamingSetup streamingFactory) throws Exception {
		var runtime = new TestPPERuntime(streamingFactory);
		runtime.signalSetupComplete();
		runtime.startRead();
		runtime.loadedModelCopy.add(runtime.branch.getModel());
		runtime.conclude();
		return runtime;
	}
	
	public static List<TestPPERuntime> buildBackendAndFrontEndInMemoryRuntime() throws Exception {
		var backendWrapperFactory = new InMemoryEventStreamingSetupFactory.FactoryBuilder()
				.withBranchName("backend")
				.withRepoURI(new URI(REPOURI)).build();
		var frontendWrapperFactory = new FrontendEventStreamingWrapperFactory.FactoryBuilder(backendWrapperFactory).build();
		
		var frontendRuntime = new TestPPERuntime(frontendWrapperFactory);
		frontendRuntime.signalSetupComplete();
		frontendRuntime.loadedModelCopy.add(frontendRuntime.branch.getModel());
		
		var backendRuntime = new TestPPERuntime(backendWrapperFactory);
		backendRuntime.signalSetupComplete();
		backendRuntime.startRead();
		backendRuntime.loadedModelCopy.add(backendRuntime.branch.getModel());
		backendRuntime.conclude();
		
		return List.of(backendRuntime, frontendRuntime);
	}
}
