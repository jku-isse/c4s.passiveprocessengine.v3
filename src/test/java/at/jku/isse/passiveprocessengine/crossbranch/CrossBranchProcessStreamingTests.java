package at.jku.isse.passiveprocessengine.crossbranch;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import at.jku.isse.artifacteventstreaming.api.Branch;
import at.jku.isse.passiveprocessengine.TestPPERuntime;

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
}


