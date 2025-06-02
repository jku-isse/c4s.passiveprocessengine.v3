package at.jku.isse.passiveprocessengine.persistence;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import at.jku.isse.passiveprocessengine.rdfwrapper.RDFInstance;
import at.jku.isse.passiveprocessengine.TestPPERuntime;
import at.jku.isse.passiveprocessengine.demo.TestDTOProcesses;
import at.jku.isse.passiveprocessengine.instance.activeobjects.DecisionNodeInstance;
import at.jku.isse.passiveprocessengine.instance.activeobjects.ProcessInstance;
import at.jku.isse.passiveprocessengine.rdfwrapper.NodeToDomainResolver;

public class ProcessPersistenceTests {
	
	TestPPERuntime runtime;
	
	@BeforeEach
	public void setup() throws Exception {
		String directory = "./repos/" ;
		File file = new File(directory);
		try {
			FileUtils.deleteDirectory(file);
		} catch (IOException e) {
			System.out.println(String.format("Error removing directory %s : %s", directory, e.getMessage() ));
		}
		runtime = TestPPERuntime.buildPersistentButNoEventDBRuntime();
	}

	protected ProcessInstance createBaseProcess() {
		// create data
		runtime.startWrite();
		RDFInstance jiraB =  runtime.artifactFactory.getJiraInstance("jiraB");
		RDFInstance jiraC = runtime.artifactFactory.getJiraInstance("jiraC");		
		RDFInstance jiraA = runtime.artifactFactory.getJiraInstance("jiraA", jiraB, jiraC);

		ProcessInstance proc =  runtime.instantiateDefaultProcess(runtime.procFactory.getSimple2StepProcessDefinition(), jiraA);		

		runtime.conclude();
		runtime.startRead();
		proc.printProcessToConsole(" ");
		System.out.println("BranchModel has size: "+runtime.branch.getModel().size());
		assertTrue(proc.getProcessSteps().stream()
				.filter(step -> step.getDefinition().getName().equals(TestDTOProcesses.SD1) )
				.allMatch(step -> step.getOutput(TestDTOProcesses.JIRA_OUT).size() == 2));
		runtime.conclude();
		return proc;
	}
	
	@Test
	void testCreatingAndReloadingProcess() throws Exception {		
		var proc = createBaseProcess();
		var procURI = proc.getId();
		// reload data
		runtime = TestPPERuntime.buildPersistentButNoEventDBRuntime();
		runtime.startRead();
		var types = runtime.schemaReg.getAllNonDeletedInstanceTypes();
		//types.forEach(type -> System.out.println(type.getName()));
		assertFalse(types.isEmpty());		
		
		Model diff = runtime.branch.getModel().difference(runtime.loadedModelCopy);
		RDFDataMgr.write(System.out, diff, Lang.TURTLE) ;
		System.out.println("BranchModel has size: "+runtime.branch.getModel().size());
		//RDFDataMgr.write(System.out, branch.getModel(), Lang.TURTLE) ;
		var procRDF = runtime.branch.getModel().getIndividual(procURI);
		assertNotNull(procRDF);
		var optProc = runtime.schemaReg.findInstanceById(procURI);
		assertTrue(optProc.isPresent());
		var process = (ProcessInstance) optProc.get();
		assertNotNull(process);
		process.printProcessToConsole(" ");
		runtime.conclude();
	}
	
	@Test
	void testLoadingAndChangeProcess() throws Exception {
		var proc = createBaseProcess();
		var procURI = proc.getId();
		runtime = TestPPERuntime.buildPersistentButNoEventDBRuntime();
		runtime.startWrite();

		var optProc = runtime.schemaReg.findInstanceById(procURI);
		assertTrue(optProc.isPresent());
		var process = (ProcessInstance) optProc.get();
		assertNotNull(process);
		process.printProcessToConsole(" ");
		var jiraA = runtime.schemaReg.findInstanceById(NodeToDomainResolver.BASE_NS+"jiraA").get();
		var jiraB = runtime.schemaReg.findInstanceById(NodeToDomainResolver.BASE_NS+"jiraB").get();
		process.addInput("jiraIn", jiraB);
		process.removeInput("jiraIn", jiraA);	
		runtime.conclude();
		runtime.startRead();
		process.printProcessToConsole(" ");
		runtime.conclude();
	}
	
	
	
	public static void printFullProcessToLog(ProcessInstance proc) {
		printProcessToLog(proc, " ");
	}
	
	private static void printProcessToLog(ProcessInstance proc, String prefix) {
		
		System.out.println(prefix+proc.toString());
		String nextIndent = "  "+prefix;
		proc.getProcessSteps().stream().forEach(step -> {
			if (step instanceof ProcessInstance) {
				printProcessToLog((ProcessInstance) step, nextIndent);
			} else {
				
				System.out.println(nextIndent+step.toString());
			}
		});
		proc.getDecisionNodeInstances().stream().sorted(DecisionNodeInstance.comparator).forEach(dni -> System.out.println(nextIndent+dni.toString()));
	}
	
	
	
}
