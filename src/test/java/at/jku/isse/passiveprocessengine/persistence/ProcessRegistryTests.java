package at.jku.isse.passiveprocessengine.persistence;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import at.jku.isse.passiveprocessengine.core.PPEInstance;
import at.jku.isse.passiveprocessengine.core.PPEInstanceType;
import at.jku.isse.passiveprocessengine.core.ProcessEngineConfigurationBuilder;
import at.jku.isse.passiveprocessengine.core.ProcessInstanceChangeListener;
import at.jku.isse.passiveprocessengine.core.RepairTreeProvider;
import at.jku.isse.passiveprocessengine.core.RuleAnalysisService;
import at.jku.isse.passiveprocessengine.core.RuleEvaluationService;
import at.jku.isse.passiveprocessengine.core.SchemaRegistry;
import at.jku.isse.artifacteventstreaming.api.Branch;
import at.jku.isse.artifacteventstreaming.api.BranchStateUpdater;
import at.jku.isse.passiveprocessengine.core.ChangeEventTransformer;
import at.jku.isse.passiveprocessengine.core.InstanceRepository;
import at.jku.isse.passiveprocessengine.definition.activeobjects.ProcessDefinition;
import at.jku.isse.passiveprocessengine.definition.serialization.DTOs;
import at.jku.isse.passiveprocessengine.definition.serialization.DefinitionTransformer;
import at.jku.isse.passiveprocessengine.definition.serialization.ProcessRegistry;
import at.jku.isse.passiveprocessengine.demo.TestArtifacts;
import at.jku.isse.passiveprocessengine.designspace.RewriterFactory;
import at.jku.isse.passiveprocessengine.demo.TestDTOProcesses;
import at.jku.isse.passiveprocessengine.instance.ProcessInstanceChangeProcessor;
import at.jku.isse.passiveprocessengine.instance.activeobjects.DecisionNodeInstance;
import at.jku.isse.passiveprocessengine.instance.activeobjects.ProcessInstance;
import at.jku.isse.passiveprocessengine.instance.messages.EventDistributor;
import at.jku.isse.passiveprocessengine.instance.messages.Responses.IOResponse;
import at.jku.isse.passiveprocessengine.monitoring.CurrentSystemTimeProvider;
import at.jku.isse.passiveprocessengine.monitoring.ProcessQAStatsMonitor;
import at.jku.isse.passiveprocessengine.rdfwrapper.AbstractionMapper;
import at.jku.isse.passiveprocessengine.rdfwrapper.NodeToDomainResolver;
import at.jku.isse.passiveprocessengine.rdfwrapper.RDFWrapperTestSetup;
import lombok.NonNull;

public class ProcessRegistryTests extends ProcessPersistenceTests {

	protected ProcessRegistry procReg;

	@Override 
	public void setup() throws Exception{
		super.setup();
		instanceRepository.startReadTransaction();
		procReg = new ProcessRegistry(context);
		instanceRepository.concludeTransaction();
	}
	
	@Test
	void testCreatingAndRegisterProcessDefinition() throws Exception {		
		RDFWrapperTestSetup.resetPersistence(); // also ensure to delete model from filesystem
		setup(); // manual as we otherwise cant reset data
		instanceRepository.startWriteTransaction();
								
		var deployResult = procReg.createProcessDefinitionIfNotExisting(procFactory.getSimple2StepProcessDefinition());
		instanceRepository.concludeTransaction();
		instanceRepository.startReadTransaction();
		
		System.out.println("BranchModel has size: "+branch.getModel().size());
		assertEquals(0, deployResult.getDefinitionErrors().size());
		assertEquals(0, deployResult.getInstanceErrors().size());
		
	}
	
	
	@Test
	void testLoadingAndRetrieveProcessDefinition() throws Exception {
		RDFWrapperTestSetup.resetPersistence(); // also ensure to delete model from filesystem
		setup(); // manual as we otherwise cant reset data
		startWrite();
		System.out.println("Size before registration: "+branch.getModel().size());			
		var deployResult = procReg.createProcessDefinitionIfNotExisting(procFactory.getSimple2StepProcessDefinition());		
		instanceRepository.concludeTransaction();
		instanceRepository.startWriteTransaction();
		System.out.println("Size after registration: "+branch.getModel().size());
		var defs = procReg.getAllDefinitions(true);
		procReg.removeProcessDefinition(deployResult.getProcDef().getName());
		instanceRepository.concludeTransaction();
		instanceRepository.startReadTransaction();
		System.out.println("Size after removal: "+branch.getModel().size());
		
		assertEquals(1, defs.size());
		
	}
	
	
	
	
	
	
	
	
	
}
