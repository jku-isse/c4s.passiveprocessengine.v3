package at.jku.isse.passiveprocessengine.crossbranch;

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import at.jku.isse.artifacteventstreaming.api.Branch;
import at.jku.isse.passiveprocessengine.TestPPERuntime;
import at.jku.isse.passiveprocessengine.definition.activeobjects.ProcessDefinition;
import at.jku.isse.passiveprocessengine.demo.TestDTOProcesses;
import at.jku.isse.passiveprocessengine.instance.activeobjects.ProcessInstance;
import at.jku.isse.passiveprocessengine.instance.messages.Responses.IOResponse;
import at.jku.isse.passiveprocessengine.rdfwrapper.RDFInstance;
import lombok.NonNull;

@TestInstance(Lifecycle.PER_CLASS)
class CrossBranchProcessStreamingTests {
	TestPPERuntime backendRuntime;
	TestPPERuntime frontendRuntime;
	CountDownLatch latch;
	SyncForTestingService serviceOut;
	Branch branchSource;	
	Branch branchDestination;
	
	@BeforeEach
	void setup() throws Exception {
		var runtimes = TestPPERuntime.buildBackendAndFrontEndInMemoryRuntime();
		backendRuntime = runtimes.get(0);
		frontendRuntime = runtimes.get(1);
		branchSource = backendRuntime.branch;
		branchDestination = frontendRuntime.branch;
	}

	@Test
	void testCreateProcDefinition() throws Exception {
		// setup test interceptor service
		latch = new CountDownLatch(1); // number of commits we will dispatch (incl any schema creation commit in the setup method)
		serviceOut = new SyncForTestingService("OutDestination", latch, branchDestination.getBranchResource().getModel());
		branchDestination.appendOutgoingCommitDistributer(serviceOut);	
				
		backendRuntime.startWrite();
		var result = backendRuntime.getProcReg().createProcessDefinitionIfNotExisting(backendRuntime.procFactory.getSimple2StepProcessDefinition());
		backendRuntime.conclude();
		
		//wait for changes to have propagated
		boolean success = latch.await(5000, TimeUnit.SECONDS);
		assert(success);
		
		var procDefs = frontendRuntime.getProcReg().getAllDefinitions(true);
		assertEquals(1, procDefs.size());
		assertEquals(result.getProcDef().getId(), procDefs.stream().findAny().get().getId());
	}
	
	@Test
	void testCreateAndInstantiateProcDefinition() throws Exception {
		// setup test interceptor service
		latch = new CountDownLatch(2); // number of commits we will dispatch (incl any schema creation commit in the setup method)
		serviceOut = new SyncForTestingService("OutDestination", latch, branchDestination.getBranchResource().getModel());
		branchDestination.appendOutgoingCommitDistributer(serviceOut);	
				
		backendRuntime.startWrite();
		var result = backendRuntime.getProcReg().createProcessDefinitionIfNotExisting(backendRuntime.procFactory.getSimple2StepProcessDefinition());
		var artifactFactory = backendRuntime.artifactFactory;
		RDFInstance jiraB =  artifactFactory.getJiraInstance("jiraB");
		RDFInstance jiraC = artifactFactory.getJiraInstance("jiraC");		
		RDFInstance jiraA = artifactFactory.getJiraInstance("jiraA", jiraB, jiraC);
		var input = Map.of(TestDTOProcesses.JIRA_IN, Set.of(jiraA));		
		backendRuntime.conclude();
		
		backendRuntime.startWrite();				
		var instResult = backendRuntime.getProcReg().instantiateProcess(result.getProcDef(), input);
		assertEquals(0, instResult.getValue().size());
		var procInstId = instResult.getKey().getId();
		backendRuntime.conclude();
		
		//wait for changes to have propagated
		boolean success = latch.await(5000, TimeUnit.SECONDS);
		assert(success);
		
		var procCopy = frontendRuntime.getProcReg().getProcessById(procInstId);
		assertNotNull(procCopy);
		
	}
	

}


