package at.jku.isse.passiveprocessengine.persistence;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import java.util.Set;

import org.apache.jena.ontapi.OntModelFactory;
import org.junit.jupiter.api.Test;

import at.jku.isse.passiveprocessengine.TestPPERuntime;
import at.jku.isse.passiveprocessengine.TestUtils;
import at.jku.isse.passiveprocessengine.demo.TestDTOProcesses;
import at.jku.isse.passiveprocessengine.rdfwrapper.RDFInstance;

public class ProcessRegistryTests extends ProcessPersistenceTests {
	
	
	
	@Test
	void testCreatingRegisterAndDeleteProcessDefinition() throws Exception {		
		
		var initModel = OntModelFactory.createModel();
		
		runtime.startWrite();
		System.out.println("Size before initial adding: "+runtime.branch.getModel().size());
		initModel.add(runtime.branch.getModel());
		var initialSize = runtime.branch.getModel().size();
		System.out.println("BranchModel has size: "+initialSize);
		
		var result = runtime.getProcReg().createProcessDefinitionIfNotExisting(runtime.procFactory.getSimple2StepProcessDefinition());
		assertTrue(result.getDefinitionErrors().isEmpty());
		assertEquals(0, result.getDefinitionErrors().size());
		assertEquals(0, result.getInstanceErrors().size());
		var procDefURI = result.getProcDef().getId();
		runtime.conclude();
	
		// reload data
		runtime = TestPPERuntime.buildPersistentButNoEventDBRuntime();
		runtime.startWrite();
		System.out.println("Size before re-registration: "+runtime.branch.getModel().size());			
		var procDef = runtime.getProcReg().getProcessDefinition(procDefURI, true);
		assertNotNull(procDef);

		System.out.println("Size after re-registration: "+runtime.branch.getModel().size());
		var defs = runtime.getProcReg().getAllDefinitions(true);
		assertEquals(1, defs.size());
		runtime.getProcReg().removeProcessDefinition(procDefURI);
		runtime.conclude();
		runtime.startRead();
		var finalSize = runtime.branch.getModel().size();
		System.out.println("Size after removal: "+finalSize);
		
		if (initialSize != finalSize) {
			TestUtils.printDiff(initModel, runtime.branch.getModel(), true);
		}
		
		defs = runtime.getProcReg().getAllDefinitions(true);
		assertEquals(initialSize, finalSize);
		assertEquals(0, defs.size());
		runtime.conclude();
	}
	
	@Test
	void testCreatingRegisterAndDeleteProcessInstance() throws Exception {		
		
		var initModel = OntModelFactory.createModel();
		
		runtime.startWrite();
		RDFInstance jiraB =  runtime.artifactFactory.getJiraInstance("jiraB");
		RDFInstance jiraC = runtime.artifactFactory.getJiraInstance("jiraC");		
		RDFInstance jiraA = runtime.artifactFactory.getJiraInstance("jiraA", jiraB, jiraC);
		
		
		var result = runtime.getProcReg().createProcessDefinitionIfNotExisting(runtime.procFactory.getSimple2StepProcessDefinition());
		assertTrue(result.getDefinitionErrors().isEmpty());
		assertEquals(0, result.getDefinitionErrors().size());
		assertEquals(0, result.getInstanceErrors().size());
		
		System.out.println("Size before initial adding: "+runtime.branch.getModel().size());
		initModel.add(runtime.branch.getModel());
		var initialSize = runtime.branch.getModel().size();
		System.out.println("BranchModel has size: "+initialSize);
		
		// create the process instance
		var procResult = runtime.getProcReg().instantiateProcess(result.getProcDef(), Map.of(TestDTOProcesses.JIRA_IN, Set.of(jiraA)));
		assertEquals(0, procResult.getValue().size());
		assertNotNull(procResult.getKey());
		var procId = procResult.getKey().getId();
		runtime.conclude();
		runtime.startRead();
		System.out.println("Size before reloading: "+runtime.branch.getModel().size());	
		runtime.conclude();
		
		// reload data
		runtime = TestPPERuntime.buildPersistentButNoEventDBRuntime();
		runtime.startWrite();
		System.out.println("Size after reloading: "+runtime.branch.getModel().size());
		var procs = runtime.getProcReg().getProcessInstances().stream().toList();
		assertEquals(1, procs.size());
		assertEquals(procId, procs.get(0).getId());
		procs.get(0).printProcessToConsole(" ");
		System.out.println("Size after process refetching: "+runtime.branch.getModel().size());
		
		runtime.getProcReg().removeProcessByName(procs.get(0).getName());
		runtime.conclude(); //we need to let the rule engine clean up first before comparing model content
		
		runtime.startRead();
		var finalSize = runtime.branch.getModel().size();
		System.out.println("Size after removal: "+finalSize);
		if (initialSize != finalSize) {
			TestUtils.printDiff(initModel, runtime.branch.getModel(), true);
		}
		procs = runtime.getProcReg().getProcessInstances().stream().toList();
		assertEquals(0, procs.size());
		runtime.conclude();
	}
	

	@Test
	void testCreatingRegisterAndDeleteProcessDefinitionWithProcessInstance() throws Exception {		
		
		var initModel = OntModelFactory.createModel();
		
		runtime.startWrite();
		RDFInstance jiraB =  runtime.artifactFactory.getJiraInstance("jiraB");
		RDFInstance jiraC = runtime.artifactFactory.getJiraInstance("jiraC");		
		RDFInstance jiraA = runtime.artifactFactory.getJiraInstance("jiraA", jiraB, jiraC);
		System.out.println("Size before initial adding: "+runtime.branch.getModel().size());
		initModel.add(runtime.branch.getModel());
		var initialSize = runtime.branch.getModel().size();
		System.out.println("BranchModel has size: "+initialSize);
		
		var result = runtime.getProcReg().createProcessDefinitionIfNotExisting(runtime.procFactory.getSimple2StepProcessDefinition());
		assertTrue(result.getDefinitionErrors().isEmpty());
		assertEquals(0, result.getDefinitionErrors().size());
		assertEquals(0, result.getInstanceErrors().size());
		var procDefURI = result.getProcDef().getId();
		// create the process instance
		
		
		var procResult = runtime.getProcReg().instantiateProcess(result.getProcDef(), Map.of(TestDTOProcesses.JIRA_IN, Set.of(jiraA)));
		assertEquals(0, procResult.getValue().size());
		assertNotNull(procResult.getKey());
		var procId = procResult.getKey().getId();
		runtime.conclude();
	
		// reload data
		runtime = TestPPERuntime.buildPersistentButNoEventDBRuntime();
		runtime.startWrite();
		System.out.println("Size before re-registration: "+runtime.branch.getModel().size());			
		var procDef = runtime.getProcReg().getProcessDefinition(procDefURI, true);
		assertNotNull(procDef);

		System.out.println("Size after re-registration: "+runtime.branch.getModel().size());
		var defs = runtime.getProcReg().getAllDefinitions(true);
		assertEquals(1, defs.size());
		var procs = runtime.getProcReg().getProcessInstances().stream().toList();
		assertEquals(1, procs.size());
		assertEquals(procId, procs.get(0).getId());
		procs.get(0).printProcessToConsole(" ");
		System.out.println("Size after process refetching: "+runtime.branch.getModel().size());
		
		runtime.getProcReg().removeProcessDefinition(procDefURI);
		runtime.conclude();
		runtime.startRead();
		var finalSize = runtime.branch.getModel().size();
		System.out.println("Size after removal: "+finalSize);
		
		if (initialSize != finalSize) {
			TestUtils.printDiff(initModel, runtime.branch.getModel(), true);
		}
		
		defs = runtime.getProcReg().getAllDefinitions(true);
		assertEquals(initialSize, finalSize);
		assertEquals(0, defs.size());
		runtime.conclude();
	}
	

	
	
}
