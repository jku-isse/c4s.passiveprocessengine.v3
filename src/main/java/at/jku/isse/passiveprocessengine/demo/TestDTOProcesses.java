package at.jku.isse.passiveprocessengine.demo;

import java.util.ArrayList;

import org.springframework.boot.autoconfigure.info.ProjectInfoProperties.Build;

import at.jku.isse.passiveprocessengine.core.InstanceType;
import at.jku.isse.passiveprocessengine.definition.activeobjects.DecisionNodeDefinition.InFlowType;
import at.jku.isse.passiveprocessengine.definition.serialization.DTOs;
import at.jku.isse.passiveprocessengine.definition.serialization.DTOs.Constraint;
import at.jku.isse.passiveprocessengine.instance.StepLifecycle.Conditions;

public class TestDTOProcesses {

TestArtifacts artifactFactory;
	
	public TestDTOProcesses(TestArtifacts artifactFactory) {
		this.artifactFactory = artifactFactory;
	}

	public DTOs.Process getSimpleDTOSubprocess( ) {
		InstanceType typeJira = artifactFactory.getJiraInstanceType();
		DTOs.Process procD = DTOs.Process.builder()
		.code("TestSerializeProc1")
		.description("Test for Serialization")
		.build();
		procD.getInput().put("jiraIn", typeJira.getName());
		procD.getOutput().put("jiraOut", typeJira.getName());
		procD.getConditions()
			.computeIfAbsent(Conditions.PRECONDITION, k -> new ArrayList<>())
			.add(Constraint.builder()
					.arlRule("self.in_jiraIn->size() = 1")
					.code("PRE1")
					.build()
					);

		DTOs.DecisionNode dn1 = DTOs.DecisionNode.builder()
				.code("dndSubStart")
				.inflowType(InFlowType.AND)
				.build();
		DTOs.DecisionNode dn2 = DTOs.DecisionNode.builder()
				.code("dndSubEnd")
				.inflowType(InFlowType.AND)
				.build();
		procD.getDns().add(dn1);
		procD.getDns().add(dn2);

		DTOs.Step sd1 = DTOs.Step.builder()
				.code("subtask1")
				.build();
		sd1.getInput().put("jiraIn", typeJira.getName());
		sd1.getOutput().put("jiraOut", typeJira.getName());
		sd1.getConditions()
			.computeIfAbsent(Conditions.PRECONDITION, k -> new ArrayList<>())
			.add(Constraint.builder()
					.arlRule("self.in_jiraIn->size() = 1")
					.code("PRE1")
					.build()
				);
		sd1.getConditions()
			.computeIfAbsent(Conditions.POSTCONDITION, k -> new ArrayList<>())
			.add(Constraint.builder()
					.arlRule("self.in_jiraIn->size() = 1 and self.in_jiraIn->forAll( issue | issue.state = 'Closed')")
					.code("POST1")
					.build());
		sd1.getIoMapping().put("jiraOut", "self.in_jiraIn");//->forAll(artIn | self.out_jiraOut->exists(artOut  | artOut = artIn)) and self.out_jiraOut->forAll(artOut2 | self.in_jiraIn->exists(artIn2  | artOut2 = artIn2))"); // ensures both sets are identical in content
		sd1.setInDNDid(dn1.getCode());
		sd1.setOutDNDid(dn2.getCode());
		procD.getSteps().add(sd1);

		DTOs.Step sd2 = DTOs.Step.builder()
				.code("subtask2")
				.build();
		sd2.getInput().put("jiraIn", typeJira.getName());
		sd2.getConditions()
			.computeIfAbsent(Conditions.PRECONDITION, k -> new ArrayList<>())
			.add(Constraint.builder()
					.arlRule("self.in_jiraIn->size() = 1")
					.code("PRE1")
					.build());
		sd2.getConditions()
			.computeIfAbsent(Conditions.POSTCONDITION, k -> new ArrayList<>())
			.add(Constraint.builder()
					.arlRule("self.in_jiraIn->size() = 1 and self.in_jiraIn->forAll( issue | issue.state = 'Closed')")
					.code("POST1")
					.build());
		sd2.setInDNDid(dn1.getCode());
		sd2.setOutDNDid(dn2.getCode());
		procD.getSteps().add(sd2);

		dn1.getMapping().add(new DTOs.Mapping(procD.getCode(), "jiraIn", sd1.getCode(), "jiraIn")); //into both steps
		dn1.getMapping().add(new DTOs.Mapping(procD.getCode(), "jiraIn", sd2.getCode(), "jiraIn")); //into both steps
		dn2.getMapping().add(new DTOs.Mapping(sd1.getCode(), "jiraOut", procD.getCode(), "jiraOut")); //out of the first
		return procD;
	}

	public DTOs.Process getSimpleSuperDTOProcessDefinition( ) {
		InstanceType typeJira = artifactFactory.getJiraInstanceType();
		DTOs.Process procD = DTOs.Process.builder()
				.code("TestSerializeParentProc1")
				.description("Test for Serialization")
				.build();
		procD.getInput().put("jiraIn", typeJira.getName());
		procD.getOutput().put("jiraOut", typeJira.getName());
		procD.getConditions()
			.computeIfAbsent(Conditions.PRECONDITION, k -> new ArrayList<>())
			.add(Constraint.builder()
					.arlRule("self.in_jiraIn->size() = 1")
					.code("PRE1")
					.build());
		DTOs.DecisionNode dn1 = DTOs.DecisionNode.builder()
				.code("dndParentStart")
				.inflowType(InFlowType.AND)
				.build();
		DTOs.DecisionNode dn2 = DTOs.DecisionNode.builder()
				.code("dndParentEnd")
				.inflowType(InFlowType.AND)
				.build();
		procD.getDns().add(dn1);
		procD.getDns().add(dn2);

		DTOs.Step sd1 = DTOs.Step.builder()
				.code("paratask1")
				.build();
		sd1.getInput().put("jiraIn", typeJira.getName());
		sd1.getConditions()
			.computeIfAbsent(Conditions.PRECONDITION, k -> new ArrayList<>())
			.add(Constraint.builder()
					.arlRule("self.in_jiraIn->size() = 1")
					.code("PRE1")
					.build());
		sd1.getConditions()
			.computeIfAbsent(Conditions.POSTCONDITION, k -> new ArrayList<>())
			.add(Constraint.builder()
					.arlRule("self.in_jiraIn->forAll( issue | issue.state = 'Closed')")
					.code("POST1")
					.build());
		sd1.setInDNDid(dn1.getCode());
		sd1.setOutDNDid(dn2.getCode());
		procD.getSteps().add(sd1);

		DTOs.Step sd2 = getSimpleDTOSubprocess();
		sd2.setInDNDid(dn1.getCode());
		sd2.setOutDNDid(dn2.getCode());
		procD.getSteps().add(sd2);

		dn1.getMapping().add(new DTOs.Mapping(procD.getCode(), "jiraIn", sd1.getCode(), "jiraIn")); //into both steps
		dn1.getMapping().add(new DTOs.Mapping(procD.getCode(), "jiraIn", sd2.getCode(), "jiraIn")); //into both steps
		dn2.getMapping().add(new DTOs.Mapping(sd2.getCode(), "jiraOut", procD.getCode(), "jiraOut")); //out of the second

		return procD;
	}

	
}
