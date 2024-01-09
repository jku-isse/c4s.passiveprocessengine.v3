package at.jku.isse.passiveprocessengine.definition;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import at.jku.isse.designspace.core.model.Instance;
import at.jku.isse.designspace.core.model.InstanceType;
import at.jku.isse.designspace.core.model.Workspace;
import at.jku.isse.designspace.core.service.WorkspaceService;
import at.jku.isse.passiveprocessengine.definition.activeobjects.DecisionNodeDefinition;
import at.jku.isse.passiveprocessengine.definition.activeobjects.MappingDefinition;
import at.jku.isse.passiveprocessengine.definition.activeobjects.ProcessDefinition;
import at.jku.isse.passiveprocessengine.definition.activeobjects.StepDefinition;
import at.jku.isse.passiveprocessengine.definition.activeobjects.DecisionNodeDefinition.InFlowType;
import at.jku.isse.passiveprocessengine.demo.TestArtifacts;
import at.jku.isse.passiveprocessengine.demo.TestProcesses;
import at.jku.isse.passiveprocessengine.instance.StepLifecycle.Conditions;

@ExtendWith(SpringExtension.class)
@SpringBootTest
class DefinitionTests {

	Workspace ws;
	static InstanceType typeJira;
	
	@BeforeEach
	void setup() throws Exception {
		ws = WorkspaceService.createWorkspace("test", WorkspaceService.PUBLIC_WORKSPACE, WorkspaceService.ANY_USER, null, false, false);
		ws.setAutoUpdate(true);
		typeJira = TestArtifacts.getJiraInstanceType(ws);
	}

	@Test
	void testCreateSpec() {
		
		ProcessDefinition procDef = ProcessDefinition.getInstance("proc1", ws);
		procDef.addExpectedInput("jiraIn", typeJira);	
		assert(typeJira != null);
		assert(procDef.getExpectedInput().get("jiraIn").equals(typeJira));
		
//		ws.debugInstanceTypes().stream().forEach(it -> System.out.println(it));	
		DecisionNodeDefinition dnd1 = procDef.createDecisionNodeDefinition("dnd1", ws);
		DecisionNodeDefinition dnd2 =  procDef.createDecisionNodeDefinition("dnd2", ws);
		dnd2.setInflowType(InFlowType.AND); // set by default
		DecisionNodeDefinition dnd3 =  procDef.createDecisionNodeDefinition("dnd3", ws);
		procDef.addDecisionNodeDefinition(dnd1);
		procDef.addDecisionNodeDefinition(dnd2);
		procDef.addDecisionNodeDefinition(dnd3);
		
		StepDefinition sd1 = procDef.createStepDefinition("sd1", ws);
		sd1.addExpectedInput("jiraIn", typeJira);
		assert(sd1.getExpectedInput().get("jiraIn").equals(typeJira));
		sd1.addExpectedOutput("jiraOut", typeJira);
		assert(sd1.getExpectedOutput().get("jiraOut").equals(typeJira));
		sd1.addInputToOutputMappingRule("jiraIn2jiraOut", "some rule yet to be defined");
		sd1.setCondition(Conditions.PRECONDITION, "self.in_jiraIn->size() > 0");
		sd1.setInDND(dnd1);
		sd1.setOutDND(dnd2);
		
		StepDefinition sd2 = procDef.createStepDefinition("sd2", ws);
		sd2.setInDND(dnd2);
		sd2.setOutDND(dnd3);
		
		dnd1.addDataMappingDefinition(MappingDefinition.getInstance(procDef.getName(), "jiraIn", sd1.getName(), "jiraIn", ws));
		dnd2.addDataMappingDefinition(MappingDefinition.getInstance(sd1.getName(), "jiraOut", sd2.getName(), "jiraIn",ws));
		
		assertTrue(procDef.getDecisionNodeDefinitions().size() == 3);
		assertTrue(procDef.getStepDefinitions().size() == 2);
		assertTrue(sd1.getCondition(Conditions.PRECONDITION).get().length() > 0);
		assertTrue(sd1.getOutDND().equals(dnd2));
		assertTrue(sd2.getInDND().equals(dnd2));
		assertTrue(sd1.getInputToOutputMappingRules().containsKey("jiraIn2jiraOut"));
		assertTrue(sd1.getExpectedInput().get("jiraIn").equals(typeJira));
		assertTrue(dnd1.getMappings().size() == 1);
		assertTrue(dnd2.getMappings().size() == 1);
	}

	@Test
	void testHierarchyIndex() throws Exception {
		ProcessDefinition pDef = TestProcesses.getSimpleSuperProcessDefinition(ws);
		pDef.getDecisionNodeDefinitions().parallelStream().forEach(dnd -> System.out.println(dnd.getName()+" "+dnd.getDepthIndex()));
		pDef.getStepDefinitions().parallelStream().forEach(step -> System.out.println(step.getName()+" "+step.getDepthIndex()));
		
	}

}
