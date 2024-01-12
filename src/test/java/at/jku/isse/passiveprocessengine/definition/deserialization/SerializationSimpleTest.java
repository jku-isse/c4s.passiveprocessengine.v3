package at.jku.isse.passiveprocessengine.definition.deserialization;

import java.util.ArrayList;
import java.util.LinkedList;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import at.jku.isse.designspace.core.model.Workspace;
import at.jku.isse.designspace.core.service.WorkspaceService;
import at.jku.isse.passiveprocessengine.configurability.ProcessConfigBaseElementFactory;
import at.jku.isse.passiveprocessengine.definition.ProcessDefinition;
import at.jku.isse.passiveprocessengine.definition.DecisionNodeDefinition.InFlowType;
import at.jku.isse.passiveprocessengine.definition.serialization.DTOs;
import at.jku.isse.passiveprocessengine.definition.serialization.DefinitionTransformer;
import at.jku.isse.passiveprocessengine.definition.serialization.JsonDefinitionSerializer;
import at.jku.isse.passiveprocessengine.definition.serialization.DTOs.Constraint;
import at.jku.isse.passiveprocessengine.demo.TestArtifacts;
import at.jku.isse.passiveprocessengine.demo.TestProcesses;
import at.jku.isse.passiveprocessengine.instance.StepLifecycle.Conditions;

public class SerializationSimpleTest {

	static JsonDefinitionSerializer json = new JsonDefinitionSerializer();
	
	@Test
	void testDeSerializeHtmlUrlAndDescription()
	{
		DTOs.Process procD = getProcessForSerialization();
		String jsonProc = json.toJson(procD);
		System.out.println(jsonProc);
		DTOs.Process deSer = json.fromJson(jsonProc);		
		assert(deSer.getConditions().get(Conditions.PRECONDITION).size() == procD.getConditions().get(Conditions.PRECONDITION).size());
	}

	public static DTOs.Process getProcessForSerialization() {
		
		DTOs.Process procD = new DTOs.Process();
		procD.setCode("TestSerializeProc1");
		procD.setDescription("Test for Serialization");
		procD.getInput().put("jiraIn", TestArtifacts.DEMOISSUETYPE);
		procD.getOutput().put("jiraOut", TestArtifacts.DEMOISSUETYPE);
		procD.getConditions().computeIfAbsent(Conditions.PRECONDITION, k -> new ArrayList<Constraint>()).add(new Constraint("self.in_jiraIn->size() = 1"));
		
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
		sd1.getInput().put("jiraIn", TestArtifacts.DEMOISSUETYPE);
		sd1.getOutput().put("jiraOut", TestArtifacts.DEMOISSUETYPE);
		sd1.getConditions().computeIfAbsent(Conditions.PRECONDITION, k -> new ArrayList<Constraint>()).add(new Constraint("self.in_jiraIn->size() = 1"));
		sd1.getConditions().computeIfAbsent(Conditions.POSTCONDITION, k -> new ArrayList<Constraint>()).add(new Constraint( "self.in_jiraIn->size() = 1 and self.in_jiraIn->forAll( issue | issue.state = 'Closed')"));
		sd1.getIoMapping().put("jiraOut", "self.in_jiraIn");//->forAll(artIn | self.out_jiraOut->exists(artOut  | artOut = artIn)) and self.out_jiraOut->forAll(artOut2 | self.in_jiraIn->exists(artIn2  | artOut2 = artIn2))"); // ensures both sets are identical in content
		sd1.setInDNDid(dn1.getCode());
		sd1.setOutDNDid(dn2.getCode());
		procD.getSteps().add(sd1);
		
		DTOs.Step sd2 = new DTOs.Step();
		sd2.setCode("subtask2");
		sd2.getInput().put("jiraIn", TestArtifacts.DEMOISSUETYPE);
		sd2.getConditions().computeIfAbsent(Conditions.PRECONDITION, k -> new ArrayList<Constraint>()).add(new Constraint("self.in_jiraIn->size() = 1"));
		sd2.getConditions().computeIfAbsent(Conditions.POSTCONDITION, k -> new ArrayList<Constraint>()).add(new Constraint("self.in_jiraIn->size() = 1 and self.in_jiraIn->forAll( issue | issue.state = 'Closed')")); 
		sd2.setInDNDid(dn1.getCode());
		sd2.setOutDNDid(dn2.getCode());
		procD.getSteps().add(sd2);
	
		dn1.getMapping().add(new DTOs.Mapping(procD.getCode(), "jiraIn", sd1.getCode(), "jiraIn")); //into both steps
		dn1.getMapping().add(new DTOs.Mapping(procD.getCode(), "jiraIn", sd2.getCode(), "jiraIn")); //into both steps
		dn2.getMapping().add(new DTOs.Mapping(sd1.getCode(), "jiraOut", procD.getCode(), "jiraOut")); //out of the first
		
		return procD;
	}
}
