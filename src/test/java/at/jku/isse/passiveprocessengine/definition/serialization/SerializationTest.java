package at.jku.isse.passiveprocessengine.definition.serialization;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.tomcat.util.codec.binary.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import at.jku.isse.designspace.core.model.InstanceType;
import at.jku.isse.designspace.core.model.Workspace;
import at.jku.isse.designspace.core.service.WorkspaceService;
import at.jku.isse.designspace.rule.checker.ArlRuleEvaluator;
import at.jku.isse.designspace.rule.service.RuleService;
import at.jku.isse.passiveprocessengine.definition.MappingDefinition;
import at.jku.isse.passiveprocessengine.definition.ProcessDefinition;
import at.jku.isse.passiveprocessengine.definition.DecisionNodeDefinition.InFlowType;
import at.jku.isse.passiveprocessengine.instance.ProcessInstanceChangeProcessor;
import at.jku.isse.passiveprocessengine.instance.TestArtifacts;
import at.jku.isse.passiveprocessengine.instance.StepLifecycle.Conditions;

@ExtendWith(SpringExtension.class)
@SpringBootTest
class SerializationTest {

	static Workspace ws;
	static InstanceType typeJira;
	static JsonDefinitionSerializer json = new JsonDefinitionSerializer();
	
	@BeforeEach
	void setup() throws Exception {
		ws = WorkspaceService.createWorkspace("test", WorkspaceService.PUBLIC_WORKSPACE, WorkspaceService.ANY_USER, null, false, false);
		typeJira = TestArtifacts.getJiraInstanceType(ws);
	}

	@Test
	void testSerializeAndBackSimpleProcessDefinition() {
		DTOs.Process procD = getSimpleSubprocess(ws);
		String jsonProc = json.toJson(procD);
		System.out.println(jsonProc);
		DTOs.Process deSer = json.fromJson(jsonProc);
		ProcessDefinition procDef = DefinitionTransformer.fromDTO(deSer, ws);
		assert(procDef.getName().equals(procD.code));
		assert(procDef.getStepDefinitions().size() == procD.getSteps().size());
		assert(procDef.getCondition(Conditions.PRECONDITION).get().equals(procD.getConditions().get(Conditions.PRECONDITION)));
	}
	
	
	public static DTOs.Process getSimpleSubprocess(Workspace ws) {
		DTOs.Process procD = new DTOs.Process();
		procD.setCode("TestSerializeProc1");
		procD.setDescription("Test for Serialization");
		procD.getInput().put("jiraIn", typeJira.name());
		procD.getOutput().put("jiraOut", typeJira.name());
		procD.getConditions().put(Conditions.PRECONDITION, "self.in_jiraIn->size() = 1");
		
		DTOs.DecisionNode dn1 = new DTOs.DecisionNode();
		dn1.setCode("dndSubStart");
		dn1.setInflowType(InFlowType.AND);
		DTOs.DecisionNode dn2 = new DTOs.DecisionNode();
		dn2.setCode("dndSubEnd");
		dn2.setInflowType(InFlowType.AND);
		procD.getDns().add(dn1);
		procD.getDns().add(dn2);
		
		DTOs.Step sd1 = new DTOs.Step();
		sd1.setCode("subtask1");
		sd1.getInput().put("jiraIn", typeJira.name());
		sd1.getOutput().put("jiraOut", typeJira.name());
		sd1.getConditions().put(Conditions.PRECONDITION, "self.in_jiraIn->size() = 1");
		sd1.getConditions().put(Conditions.POSTCONDITION, "self.in_jiraIn->forAll( issue | issue.state = 'Closed')");
		sd1.getIoMapping().put("jiraIn2jiraOut", "self.in_jiraIn->forAll(artIn | self.out_jiraOut->exists(artOut  | artOut = artIn)) and self.out_jiraOut->forAll(artOut2 | self.in_jiraIn->exists(artIn2  | artOut2 = artIn2))"); // ensures both sets are identical in content
		sd1.setInDNDid(dn1.code);
		sd1.setOutDNDid(dn2.code);
		procD.getSteps().add(sd1);
		
		DTOs.Step sd2 = new DTOs.Step();
		sd2.setCode("subtask2");
		sd2.getInput().put("jiraIn", typeJira.name());
		sd2.getConditions().put(Conditions.PRECONDITION, "self.in_jiraIn->size() = 1");
		sd2.getConditions().put(Conditions.POSTCONDITION, "self.in_jiraIn->forAll( issue | issue.state = 'Closed')"); 
		sd2.setInDNDid(dn1.code);
		sd2.setOutDNDid(dn2.code);
		procD.getSteps().add(sd2);
	
		dn1.getMapping().add(new DTOs.Mapping(procD.getCode(), "jiraIn", sd1.getCode(), "jiraIn")); //into both steps
		dn1.getMapping().add(new DTOs.Mapping(procD.getCode(), "jiraIn", sd2.getCode(), "jiraIn")); //into both steps
		dn2.getMapping().add(new DTOs.Mapping(sd1.getCode(), "jiraOut", procD.getCode(), "jiraOut")); //out of the first
		return procD;
	}
}
