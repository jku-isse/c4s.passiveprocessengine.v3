package at.jku.isse.passiveprocessengine.demo;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import at.jku.isse.passiveprocessengine.core.InstanceType;
import at.jku.isse.passiveprocessengine.definition.activeobjects.DecisionNodeDefinition.InFlowType;
import at.jku.isse.passiveprocessengine.definition.serialization.DTOs;
import at.jku.isse.passiveprocessengine.definition.serialization.DTOs.*;

import at.jku.isse.passiveprocessengine.instance.StepLifecycle.Conditions;

public class TestDTOProcesses {

	public static final String SD2 = "step2";
	public static final String SD1 = "step1";
	public static final String DND2 = "dnd2";
	public static final String JIRA_OUT = "jiraOut";
	public static final String JIRA_IN = "jiraIn";
	public static final String DND_SUB_END = "dndSubEnd";
	public static final String DND_SUB_START = "dndSubStart";
	final TestArtifacts artifactFactory;
	final InstanceType typeJira;
	final String jiraFQN;
	
	private static final AtomicInteger conditionCounter = new AtomicInteger(0);

	public TestDTOProcesses(TestArtifacts artifactFactory) {
		this.artifactFactory = artifactFactory;
		typeJira = artifactFactory.getJiraInstanceType();
		jiraFQN = "root/types/"+typeJira.getName();
	}

	protected DTOs.Process buildDefaultProcessSkeleton(String processName) {
		DTOs.Process procD = DTOs.Process.builder()
				.code(processName)
				.description("Testing with "+processName)		
				.specOrderIndex(conditionCounter.getAndIncrement())
				.build();
		buildDefaultStartAndEndDecisionNode(procD);
		return procD;
	}
	
	protected void buildDefaultStartAndEndDecisionNode(DTOs.Process procD) {
		DTOs.DecisionNode dn1 = DTOs.DecisionNode.builder()
				.code(DND_SUB_START)
				.inflowType(InFlowType.AND)
				.build();
		DTOs.DecisionNode dn2 = DTOs.DecisionNode.builder()
				.code(DND_SUB_END)
				.inflowType(InFlowType.AND)
				.build();
		procD.getDns().add(dn1);
		procD.getDns().add(dn2);
	}
	
	protected void buildAndIncludeCondition(DTOs.Step step, Conditions condition, String arl) {
		step.getConditions()
		.computeIfAbsent(condition, k -> new ArrayList<>())
		.add(Constraint.builder()
				.arlRule(arl)
				.code(condition.toString()+conditionCounter.incrementAndGet())
				.specOrderIndex(conditionCounter.incrementAndGet())
				.build()
			);
	}
	
	protected void buildAndIncludeQA(Step step, String arl) {
		step.getQaConstraints().add(Constraint.builder()
				.arlRule(arl)
				.code("QA"+conditionCounter.incrementAndGet())
				.specOrderIndex(conditionCounter.incrementAndGet())
				.build());
	}
	
	protected void buildInAndOutDNs(DTOs.Step step, DTOs.DecisionNode inNode, DTOs.DecisionNode outNode) {
		step.setInDNDid(inNode.getCode());
		step.setOutDNDid(outNode.getCode());
	}
	
	protected DTOs.Step buildAndIncludeStep(String name, DTOs.Process parent) {
		DTOs.Step step = DTOs.Step.builder()
				.code(name)
				.specOrderIndex(conditionCounter.getAndIncrement())
				.build();
		parent.getSteps().add(step);
		return step;
	}
	
	protected DecisionNode buildAndIncludeDecisionNode(String name, DTOs.Process parent) {
		DecisionNode dn = DTOs.DecisionNode.builder()
				.code(name)
			.inflowType(InFlowType.AND)
			.build();
		parent.getDns().add(dn);
		return dn;
	}
	
	protected void buildJiraInput(String paraName, Step step) {
		step.getInput().put(paraName, typeJira.getName());
	}
	
	protected void buildJiraOutput(String paraName, Step step) {
		step.getOutput().put(paraName, typeJira.getName());
	}
	
	public DTOs.Process getSimpleDTOSubprocess( ) {
		
		DTOs.Process procD = buildDefaultProcessSkeleton("TestSerializeProc1");
		procD.getInput().put(JIRA_IN, typeJira.getName());
		procD.getOutput().put(JIRA_OUT, typeJira.getName());
		buildAndIncludeCondition(procD, Conditions.PRECONDITION,"self.in_jiraIn->size() = 1");					
		
		DTOs.Step sd1 = DTOs.Step.builder().code("subtask1").build();
		sd1.getInput().put(JIRA_IN, typeJira.getName());
		sd1.getOutput().put(JIRA_OUT, typeJira.getName());
		buildAndIncludeCondition(sd1, Conditions.PRECONDITION, "self.in_jiraIn->size() = 1");
		buildAndIncludeCondition(sd1, Conditions.POSTCONDITION,"self.in_jiraIn->size() = 1 and self.in_jiraIn->forAll( issue | issue.state = 'Closed')");
		sd1.getIoMapping().put(JIRA_OUT, "self.in_jiraIn");//->forAll(artIn | self.out_jiraOut->exists(artOut  | artOut = artIn)) and self.out_jiraOut->forAll(artOut2 | self.in_jiraIn->exists(artIn2  | artOut2 = artIn2))"); // ensures both sets are identical in content
		sd1.setInDNDid(DND_SUB_START);
		sd1.setOutDNDid(DND_SUB_END);
		procD.getSteps().add(sd1);

		DTOs.Step sd2 = DTOs.Step.builder().code("subtask2").build();
		sd2.getInput().put(JIRA_IN, typeJira.getName());
		buildAndIncludeCondition(sd2, Conditions.PRECONDITION, "self.in_jiraIn->size() = 1");
		buildAndIncludeCondition(sd2, Conditions.POSTCONDITION,  "self.in_jiraIn->size() = 1 and self.in_jiraIn->forAll( issue | issue.state = 'Closed')");					
		sd2.setInDNDid(DND_SUB_START);
		sd2.setOutDNDid(DND_SUB_END);
		procD.getSteps().add(sd2);

		DTOs.DecisionNode dn1 = procD.getDecisionNodeByCode(DND_SUB_START);
		DTOs.DecisionNode dn2 = procD.getDecisionNodeByCode(DND_SUB_END);
		dn1.getMapping().add(new DTOs.Mapping(procD.getCode(), JIRA_IN, sd1.getCode(), JIRA_IN)); //into both steps
		dn1.getMapping().add(new DTOs.Mapping(procD.getCode(), JIRA_IN, sd2.getCode(), JIRA_IN)); //into both steps
		dn2.getMapping().add(new DTOs.Mapping(sd1.getCode(), JIRA_OUT, procD.getCode(), JIRA_OUT)); //out of the first
		return procD;
	}

	public DTOs.Process getSimpleSuperDTOProcessDefinition( ) {	
		DTOs.Process procD = buildDefaultProcessSkeleton("TestParentProc1");
		procD.getInput().put(JIRA_IN, typeJira.getName());
		procD.getOutput().put(JIRA_OUT, typeJira.getName());
		buildAndIncludeCondition(procD, Conditions.PRECONDITION,"self.in_jiraIn->size() = 1");			
		DTOs.DecisionNode dn1 = procD.getDecisionNodeByCode(DND_SUB_START);
		DTOs.DecisionNode dn2 = procD.getDecisionNodeByCode(DND_SUB_END);
		
		DTOs.Step sd1 = buildAndIncludeStep("paratask1", procD);
		sd1.getInput().put(JIRA_IN, typeJira.getName());		
		buildAndIncludeCondition(sd1, Conditions.PRECONDITION, "self.in_jiraIn->size() = 1");
		buildAndIncludeCondition(sd1, Conditions.POSTCONDITION,"self.in_jiraIn->size() = 1 and self.in_jiraIn->forAll( issue | issue.state = 'Closed')");		
		buildInAndOutDNs(sd1, dn1, dn2);
		
		DTOs.Step sd2 = getSimpleDTOSubprocess();
		buildInAndOutDNs(sd2, dn1, dn2);
		procD.getSteps().add(sd2);

		dn1.getMapping().add(new DTOs.Mapping(procD.getCode(), JIRA_IN, sd1.getCode(), JIRA_IN)); //into both steps
		dn1.getMapping().add(new DTOs.Mapping(procD.getCode(), JIRA_IN, sd2.getCode(), JIRA_IN)); //into both steps
		dn2.getMapping().add(new DTOs.Mapping(sd2.getCode(), JIRA_OUT, procD.getCode(), JIRA_OUT)); //out of the second

		return procD;
	}

	
	public DTOs.Process getSimple2StepProcessDefinition( ) {
		
		DTOs.Process procD = buildDefaultProcessSkeleton("SimpleProc");
		procD.getInput().put(JIRA_IN, typeJira.getName());
		procD.getOutput().put(JIRA_OUT, typeJira.getName());
		buildAndIncludeCondition(procD, Conditions.PRECONDITION,"self.in_jiraIn->size() = 1");
		buildAndIncludeCondition(procD, Conditions.POSTCONDITION,"self.out_jiraOut->size() > 0");
						
		DTOs.DecisionNode dnd1 = procD.getDecisionNodeByCode(DND_SUB_START);
		DTOs.DecisionNode dnd3 = procD.getDecisionNodeByCode(DND_SUB_END);
		DTOs.DecisionNode dnd2 = buildAndIncludeDecisionNode(DND2, procD);
				
		Step sd1 = buildAndIncludeStep(SD1, procD);
		buildJiraInput(JIRA_IN, sd1);
		buildJiraOutput(JIRA_OUT, sd1);
		sd1.getIoMapping().put(JIRA_OUT,
			"self.in_jiraIn"
				+ "->asList()"
				+ "->first()"
				+ "->asType(<"+jiraFQN+">)"
						+ ".requirements");
		
		buildAndIncludeCondition(sd1, Conditions.PRECONDITION, "self.in_jiraIn->size() = 1");
		buildAndIncludeCondition(sd1, Conditions.POSTCONDITION,"self.out_jiraOut->size() = self.in_jiraIn->asList()->first()->asType(<"+jiraFQN+">).requirements->size()");
		
		buildAndIncludeQA(sd1, "self.out_jiraOut->forAll( issue | issue.state = 'Open')"); //, "All issue states must be 'Open'"	
		buildAndIncludeQA(sd1,"self.out_jiraOut->forAll( issue | issue.state <> 'InProgress')" ); //, "None of the issue states must be 'InProgress'"		
		buildInAndOutDNs(sd1, dnd1, dnd2); 		

		Step sd2 = buildAndIncludeStep(SD2, procD);
		buildJiraInput(JIRA_IN, sd2);
		buildJiraOutput(JIRA_OUT, sd2);
		buildAndIncludeCondition(sd2, Conditions.PRECONDITION, "self.in_jiraIn->size() = 1");
		buildAndIncludeCondition(sd2, Conditions.POSTCONDITION, "self.out_jiraOut->size() >= 0");
		sd2.getIoMapping().put(JIRA_OUT, "self.in_jiraIn");// ensures both sets are identical in content

		buildAndIncludeQA( sd2, "self.in_jiraIn->forAll( issue | issue.state = 'Closed')"); //, "All in issue states must be 'Closed'",3 );
		buildInAndOutDNs(sd2, dnd2, dnd3);		

		dnd1.getMapping().add(new DTOs.Mapping(procD.getCode(), JIRA_IN, sd1.getCode(), JIRA_IN));
		dnd2.getMapping().add(new DTOs.Mapping(sd1.getCode(), JIRA_OUT, sd2.getCode(), JIRA_IN));
		dnd3.getMapping().add(new DTOs.Mapping(sd2.getCode(), JIRA_OUT, procD.getCode(), JIRA_OUT));		
		return procD;
	}
	
}
