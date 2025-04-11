package at.jku.isse.passiveprocessengine.persistence;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.jena.ontapi.OntModelFactory;
import org.apache.jena.ontapi.OntSpecification;
import org.apache.jena.ontapi.model.OntModel;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import at.jku.isse.passiveprocessengine.rdfwrapper.RDFInstance;
import at.jku.isse.passiveprocessengine.rdfwrapper.RDFInstanceType;
import at.jku.isse.passiveprocessengine.core.ProcessEngineConfigurationBuilder;
import at.jku.isse.passiveprocessengine.core.RepairTreeProvider;
import at.jku.isse.passiveprocessengine.core.RuleAnalysisService;
import at.jku.isse.passiveprocessengine.core.NodeToDomainResolver;
import at.jku.isse.artifacteventstreaming.api.Branch;
import at.jku.isse.artifacteventstreaming.api.BranchStateUpdater;
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
import at.jku.isse.passiveprocessengine.rdfwrapper.config.RDFWrapperTestSetup;
import at.jku.isse.passiveprocessengine.rdfwrapper.events.ChangeEventTransformer;
import at.jku.isse.passiveprocessengine.rdfwrapper.events.ChangeListener;
import at.jku.isse.passiveprocessengine.rdfwrapper.rule.RuleEvaluationService;
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
		RDFWrapperTestSetup.resetPersistence(); //
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
		RDFWrapperTestSetup.resetPersistence(); 
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
	
	@Test
	void testCreatingAndRegisterAndDeleteProcessDefinition() throws Exception {		
		RDFWrapperTestSetup.resetPersistence(); //
		setup(); // manual as we otherwise cant reset data
		startWrite();
		var modelBegin = OntModelFactory.createModel( OntSpecification.OWL2_DL_MEM_BUILTIN_RDFS_INF );
		modelBegin.add(branch.getModel());
		var sizeBegin = branch.getModel().size();
		//deploy						
		var process = procFactory.getSimple2StepProcessDefinition();
		var deployResult = procReg.createProcessDefinitionIfNotExisting(process);
		conclude();
		
		startWrite();
		System.out.println("BranchModel has size: "+branch.getModel().size());
		assertEquals(0, deployResult.getDefinitionErrors().size());
		assertEquals(0, deployResult.getInstanceErrors().size());
		// remove
		var procDef = procReg.getProcessDefinition(process.getCode(), true);
		assertNotNull(procDef);
		
		procReg.removeProcessDefinition(process.getCode());
		var sizeEnd = branch.getModel().size();
		conclude();
		if (sizeEnd != sizeBegin) {
			printDiff(branch.getModel(), modelBegin);
		}
		assertEquals(sizeBegin, sizeEnd);
	}
	
	@Test
	void testCreatingAndRegisterAndDeleteProcessDefinitionWithNewInstances() throws Exception {		
		RDFWrapperTestSetup.resetPersistence(); //
		setup(); // manual as we otherwise cant reset data
		startWrite();
		var modelBegin = OntModelFactory.createModel( OntSpecification.OWL2_DL_MEM_BUILTIN_RDFS_INF );
		modelBegin.add(branch.getModel());
		var sizeBegin = branch.getModel().size();
		//deploy						
		var process = procFactory.getSimple2StepProcessDefinition();
		var deployResult = procReg.createProcessDefinitionIfNotExisting(process);
		var sizeMiddle = branch.getModel().size();
		var modelMiddle = OntModelFactory.createModel( OntSpecification.OWL2_DL_MEM_BUILTIN_RDFS_INF );
		modelMiddle.add(branch.getModel());
		conclude();
		
		RDFWrapperTestSetup.prepareForPersistedReloadWithoutDataRemoval();
		setup();
		startWrite();
		var sizeMiddleReloaded = branch.getModel().size();
		if (sizeMiddleReloaded != sizeMiddle) {
			printDiff(branch.getModel(), modelMiddle);
		}
		assertEquals(sizeMiddle, sizeMiddleReloaded);
		var defList = procReg.getAllDefinitionIDs(true);
		assertEquals(1, defList.size());
		
		var procDef = procReg.getProcessDefinition(process.getCode(), true);
		assertNotNull(procDef);
		

		
		procReg.removeProcessDefinition(process.getCode());
		var sizeEnd = branch.getModel().size();
		conclude();
		if (sizeEnd != sizeBegin) {
			printDiff(branch.getModel(), modelBegin);
		}
		assertEquals(sizeBegin, sizeEnd);
	}
	
	
	private void printDiff(OntModel modelBegin, OntModel model) {
		startRead();
		var modelDiff = model.size() > modelBegin.size() 
				? model.difference(modelBegin) 
				: modelBegin.difference(model);
		RDFDataMgr.write(System.out, modelDiff, Lang.TURTLE) ;
		conclude();
	}
	
	
}
