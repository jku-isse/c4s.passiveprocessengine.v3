package at.jku.isse.passiveprocessengine.demo;

import at.jku.isse.designspace.core.model.InstanceType;
import at.jku.isse.designspace.core.model.Workspace;
import at.jku.isse.passiveprocessengine.definition.DecisionNodeDefinition;
import at.jku.isse.passiveprocessengine.definition.DecisionNodeDefinition.InFlowType;
import at.jku.isse.passiveprocessengine.definition.MappingDefinition;
import at.jku.isse.passiveprocessengine.definition.ProcessDefinition;
import at.jku.isse.passiveprocessengine.definition.QAConstraintSpec;
import at.jku.isse.passiveprocessengine.definition.StepDefinition;
import at.jku.isse.passiveprocessengine.definition.serialization.DTOs;
import at.jku.isse.passiveprocessengine.definition.serialization.DTOs.DecisionNode;
import at.jku.isse.passiveprocessengine.definition.serialization.DTOs.Mapping;
import at.jku.isse.passiveprocessengine.definition.serialization.DTOs.Process;
import at.jku.isse.passiveprocessengine.definition.serialization.DTOs.Step;
import at.jku.isse.passiveprocessengine.instance.StepLifecycle.Conditions;

public class TestProcesses {

	// process with two parallel subtasks, each taking the same jira issue as input, both completing when that issue is set to closed
	// no qa constraints applied, datamapping only for one subtask, subproc complete when both subtasks are complete (AND cond)
	public static ProcessDefinition getSimpleSubprocessDefinition(Workspace ws) {
		InstanceType typeJira = TestArtifacts.getJiraInstanceType(ws);
		ProcessDefinition procDef = ProcessDefinition.getInstance("subproc1", ws);
		procDef.addExpectedInput("jiraIn", typeJira);	
		procDef.setCondition(Conditions.PRECONDITION, "self.in_jiraIn->size() = 1");
		procDef.addExpectedOutput("jiraOut", typeJira);
		//procDef.setCondition(Conditions.POSTCONDITION, "self.out_jiraOut->size() > 0");
		//no definition how many outputs, there is a possibility to provide output, but completion is upon subtask completion
		DecisionNodeDefinition dnd1 = procDef.createDecisionNodeDefinition("dndSubStart", ws);
		DecisionNodeDefinition dnd2 = procDef.createDecisionNodeDefinition("dndSubEnd", ws);
		StepDefinition sd1 = procDef.createStepDefinition("subtask1", ws);
		sd1.addExpectedInput("jiraIn", typeJira);
		sd1.addExpectedOutput("jiraOut", typeJira);
		sd1.setCondition(Conditions.PRECONDITION, "self.in_jiraIn->size() = 1");
		sd1.setCondition(Conditions.POSTCONDITION, "self.in_jiraIn->forAll( issue | issue.state = 'Closed')");
		sd1.addInputToOutputMappingRule("jiraIn2jiraOut", "self.in_jiraIn->forAll(artIn | self.out_jiraOut->exists(artOut  | artOut = artIn)) and "
				+ " self.out_jiraOut->forAll(artOut2 | self.in_jiraIn->exists(artIn2  | artOut2 = artIn2))"); // ensures both sets are identical in content
		sd1.setInDND(dnd1);
		sd1.setOutDND(dnd2);
		StepDefinition sd2 = procDef.createStepDefinition("subtask2", ws);
		sd2.addExpectedInput("jiraIn", typeJira);
		sd2.setCondition(Conditions.PRECONDITION, "self.in_jiraIn->size() = 1");
		sd2.setCondition(Conditions.POSTCONDITION, "self.in_jiraIn->forAll( issue | issue.state = 'Closed')");
		sd2.setInDND(dnd1);
		sd2.setOutDND(dnd2);
		
		dnd1.addDataMappingDefinition(MappingDefinition.getInstance(procDef.getName(), "jiraIn", sd1.getName(), "jiraIn",  ws)); //into both steps
		dnd1.addDataMappingDefinition(MappingDefinition.getInstance(procDef.getName(), "jiraIn", sd2.getName(), "jiraIn",  ws)); //into both steps
		dnd2.addDataMappingDefinition(MappingDefinition.getInstance(sd1.getName(), "jiraOut", procDef.getName(), "jiraOut",  ws)); //out of the first
		return procDef;
	}

	// simple process, two AND branches, on of the subbranches is a subprocess, no QA, simple datamapping, all using same completion condition of step set to closed
	public static ProcessDefinition getSimpleSuperProcessDefinition(Workspace ws) {
		InstanceType typeJira = TestArtifacts.getJiraInstanceType(ws);
		ProcessDefinition procDef = ProcessDefinition.getInstance("parentproc1", ws);
		procDef.addExpectedInput("jiraIn", typeJira);	
		procDef.setCondition(Conditions.PRECONDITION, "self.in_jiraIn->size() = 1");
		procDef.addExpectedOutput("jiraOut", typeJira);
		//procDef.setCondition(Conditions.POSTCONDITION, "self.out_jiraOut->size() > 0");
		//no definition how many outputs, there is a possibility to provide output, but completion is upon subtask completion
		DecisionNodeDefinition dnd1 = procDef.createDecisionNodeDefinition("dndParentStart", ws);
		DecisionNodeDefinition dnd2 = procDef.createDecisionNodeDefinition("dndParentEnd", ws);
		StepDefinition sd1 = procDef.createStepDefinition("paratask1", ws);
		sd1.addExpectedInput("jiraIn", typeJira);
		sd1.setCondition(Conditions.PRECONDITION, "self.in_jiraIn->size() = 1");
		sd1.setCondition(Conditions.POSTCONDITION, "self.in_jiraIn->forAll( issue | issue.state = 'Closed')");
		sd1.setInDND(dnd1);
		sd1.setOutDND(dnd2);
		StepDefinition sd2 = getSimpleSubprocessDefinition(ws);
		// we need to wire up the step definiton:
		sd2.setProcess(procDef);
		procDef.addStepDefinition(sd2);
		//inputs and output set in process/step definition, pre and post cond as well
		sd2.setInDND(dnd1);
		sd2.setOutDND(dnd2);
		
		dnd1.addDataMappingDefinition(MappingDefinition.getInstance(procDef.getName(), "jiraIn", sd1.getName(), "jiraIn",  ws)); //into both steps
		dnd1.addDataMappingDefinition(MappingDefinition.getInstance(procDef.getName(), "jiraIn", sd2.getName(), "jiraIn",  ws)); //into both steps
		dnd2.addDataMappingDefinition(MappingDefinition.getInstance(sd2.getName(), "jiraOut", procDef.getName(), "jiraOut",  ws)); //out of the second
		return procDef;
	}

	public static ProcessDefinition getSimple2StepProcessDefinition(Workspace ws) {
			InstanceType typeJira = TestArtifacts.getJiraInstanceType(ws);
			ProcessDefinition procDef = ProcessDefinition.getInstance("proc1", ws);
			procDef.addExpectedInput("jiraIn", typeJira);	
			procDef.setCondition(Conditions.PRECONDITION, "self.in_jiraIn->size() = 1");
			procDef.addExpectedOutput("jiraOut", typeJira);
			procDef.setCondition(Conditions.POSTCONDITION, "self.out_jiraOut->size() > 0");
	//		ws.debugInstanceTypes().stream().forEach(it -> System.out.println(it));	
			DecisionNodeDefinition dnd1 = procDef.createDecisionNodeDefinition("dnd1", ws);
			//dnd1.setInflowType(InFlowType.AND); 
			DecisionNodeDefinition dnd2 =  procDef.createDecisionNodeDefinition("dnd2", ws);
			//dnd2.setInflowType(InFlowType.AND); 
			DecisionNodeDefinition dnd3 =  procDef.createDecisionNodeDefinition("dnd3", ws);
			
			StepDefinition sd1 = procDef.createStepDefinition("sd1", ws);
			sd1.addExpectedInput("jiraIn", typeJira);
			sd1.addExpectedOutput("jiraOut", typeJira);
			//sd1.addInputToOutputMappingRule("jiraIn2jiraOut", "self.in_jiraIn->forAll(elem | result->includes(elem) = self.out_jiraOut->excludes(elem))-> size() = 0"); // i.e., the symetricDifference is empty, i.e., the same elements need to be in both lists
			//sd1.addInputToOutputMappingRule("jiraIn2jiraOutTest", "self.in_jiraIn->size() = self.out_jiraOut-> size()"); // i.e., the symetricDifference is empty, i.e., the same elements need to be in both lists
			//->asList()->first().asType('JiraArtifact')
			sd1.addInputToOutputMappingRule("jiraIn2jiraOutTest", 
				"self.in_jiraIn"
					+ "->asList()"
					+ "->first()"
					+ "->asType(<"+typeJira.getQualifiedName()+">)"
							+ ".requirementIDs"
								+ "->forAll(id | self.out_jiraOut->exists(art  | art.name = id))"
				+ " and "
					+ "self.out_jiraOut"
					+ "->forAll(out | self.in_jiraIn"
										+ "->asList()"
										+ "->first()"
										+ "->asType(<"+typeJira.getQualifiedName()+">)"
												+ ".requirementIDs"
												+ "->exists(artId | artId = out.name))"); // for every id in requirements there is an instance with that name, and vice versa
			
			//sd1.addInputToOutputMappingRule("jiraIn2jiraOutTest", "self.in_jiraIn->asList()->first()->asType(<"+typeJira.getQualifiedName()+">).requirementIDs->forAll(id | self.out_jiraOut->exists(art  | art.name = id))"); // for every id in requirements there is an instance with that name
			//sd1.addInputToOutputMappingRule("jiraIn2jiraOutTest", "self.in_jiraIn->forAll(artIn | self.out_jiraOut->exists(artOut  | artOut = artIn)) and "
			//		+ " self.out_jiraOut->forAll(artOut2 | self.in_jiraIn->exists(artIn2  | artOut2 = artIn2))"); // for every id in requirements there is an instance with that name
			
			sd1.setCondition(Conditions.PRECONDITION, "self.in_jiraIn->size() = 1");
			sd1.setCondition(Conditions.POSTCONDITION, "self.out_jiraOut->size() = self.in_jiraIn->asList()->first()->asType(<"+typeJira.getQualifiedName()+">).requirementIDs->size()");
			QAConstraintSpec qa1 = QAConstraintSpec.createInstance("sd1-qa1-state", "self.out_jiraOut->forAll( issue | issue.state = 'Open')", "All issue states must be 'Open'", ws);
			sd1.addQAConstraint(qa1);
			QAConstraintSpec qa2 = QAConstraintSpec.createInstance("sd1-qa2-state", "self.out_jiraOut->forAll( issue | issue.state <> 'InProgress')", "None of the issue states must be 'InProgress'", ws);
			sd1.addQAConstraint(qa2);
			sd1.setInDND(dnd1);
			sd1.setOutDND(dnd2);
			
			StepDefinition sd2 = procDef.createStepDefinition("sd2", ws);
			sd2.addExpectedInput("jiraIn", typeJira);
			sd2.addExpectedOutput("jiraOut", typeJira);
			sd2.setCondition(Conditions.PRECONDITION, "self.in_jiraIn->size() >= 1");
			sd2.setCondition(Conditions.POSTCONDITION, "self.out_jiraOut->size() >= 0");
			sd2.addInputToOutputMappingRule("jiraIn2jiraOut2", "self.in_jiraIn->forAll(artIn | self.out_jiraOut->exists(artOut  | artOut = artIn)) and "
							+ " self.out_jiraOut->forAll(artOut2 | self.in_jiraIn->exists(artIn2  | artOut2 = artIn2))"); // ensures both sets are identical in content
			QAConstraintSpec qa3 = QAConstraintSpec.createInstance("sd2-qa3-state", "self.in_jiraIn->forAll( issue | issue.state = 'Closed')", "All in issue states must be 'Closed'", ws);
			sd2.addQAConstraint(qa3);
			sd2.setInDND(dnd2);
			sd2.setOutDND(dnd3);
			
			
			dnd1.addDataMappingDefinition(MappingDefinition.getInstance(procDef.getName(), "jiraIn", sd1.getName(), "jiraIn",  ws));
			dnd2.addDataMappingDefinition(MappingDefinition.getInstance(sd1.getName(), "jiraOut", sd2.getName(), "jiraIn",  ws));
			return procDef;
		}
	
	public static ProcessDefinition get2StepProcessDefinitionWithSymmetricDiffMapping(Workspace ws) {
		InstanceType typeJira = TestArtifacts.getJiraInstanceType(ws);
		ProcessDefinition procDef = ProcessDefinition.getInstance("proc1", ws);
		procDef.addExpectedInput("jiraIn", typeJira);		
		DecisionNodeDefinition dnd1 = procDef.createDecisionNodeDefinition("dnd1", ws);
		DecisionNodeDefinition dnd2 =  procDef.createDecisionNodeDefinition("dnd2", ws);
		
		StepDefinition sd1 = procDef.createStepDefinition("sd1", ws);
		sd1.addExpectedInput("jiraIn", typeJira);
		sd1.addExpectedOutput("jiraOut", typeJira);

		sd1.addInputToOutputMappingRule("jiraIn2jiraOutTest", 
			"self.in_jiraIn"
				+ "->any()"
				+ "->asType(<"+typeJira.getQualifiedName()+">)"
						+ ".requirements"
							+ "->asSet() "  
							+"->symmetricDifference(self.out_jiraOut) " +  // ->collect(out | out.name) to map to set of names
							"->size() = 0"
							); // for every id in requirements there is an instance with that name, and vice versa

		sd1.setInDND(dnd1);
		sd1.setOutDND(dnd2);
		dnd1.addDataMappingDefinition(MappingDefinition.getInstance(procDef.getName(), "jiraIn", sd1.getName(), "jiraIn",  ws));
		return procDef;
	}

	public static DTOs.Process getSimpleDTOSubprocess(Workspace ws) {
		InstanceType typeJira = TestArtifacts.getJiraInstanceType(ws);
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
		sd1.setInDNDid(dn1.getCode());
		sd1.setOutDNDid(dn2.getCode());
		procD.getSteps().add(sd1);
		
		DTOs.Step sd2 = new DTOs.Step();
		sd2.setCode("subtask2");
		sd2.getInput().put("jiraIn", typeJira.name());
		sd2.getConditions().put(Conditions.PRECONDITION, "self.in_jiraIn->size() = 1");
		sd2.getConditions().put(Conditions.POSTCONDITION, "self.in_jiraIn->forAll( issue | issue.state = 'Closed')"); 
		sd2.setInDNDid(dn1.getCode());
		sd2.setOutDNDid(dn2.getCode());
		procD.getSteps().add(sd2);
	
		dn1.getMapping().add(new DTOs.Mapping(procD.getCode(), "jiraIn", sd1.getCode(), "jiraIn")); //into both steps
		dn1.getMapping().add(new DTOs.Mapping(procD.getCode(), "jiraIn", sd2.getCode(), "jiraIn")); //into both steps
		dn2.getMapping().add(new DTOs.Mapping(sd1.getCode(), "jiraOut", procD.getCode(), "jiraOut")); //out of the first
		return procD;
	}

	public static DTOs.Process getSimpleSuperDTOProcessDefinition(Workspace ws) {
		InstanceType typeJira = TestArtifacts.getJiraInstanceType(ws);
		DTOs.Process procD = new DTOs.Process();
		procD.setCode("TestSerializeParentProc1");
		procD.setDescription("Test for Serialization");
		procD.getInput().put("jiraIn", typeJira.name());
		procD.getOutput().put("jiraOut", typeJira.name());
		procD.getConditions().put(Conditions.PRECONDITION, "self.in_jiraIn->size() = 1");
		DTOs.DecisionNode dn1 = new DTOs.DecisionNode();
		dn1.setCode("dndParentStart");
		dn1.setInflowType(InFlowType.AND);
		DTOs.DecisionNode dn2 = new DTOs.DecisionNode();
		dn2.setCode("dndParentEnd");
		dn2.setInflowType(InFlowType.AND);
		procD.getDns().add(dn1);
		procD.getDns().add(dn2);
		
		DTOs.Step sd1 = new DTOs.Step();
		sd1.setCode("paratask1");
		sd1.getInput().put("jiraIn", typeJira.name());
		sd1.getConditions().put(Conditions.PRECONDITION, "self.in_jiraIn->size() = 1");
		sd1.getConditions().put(Conditions.POSTCONDITION, "self.in_jiraIn->forAll( issue | issue.state = 'Closed')"); 
		sd1.setInDNDid(dn1.getCode());
		sd1.setOutDNDid(dn2.getCode());
		procD.getSteps().add(sd1);
		
		DTOs.Step sd2 = getSimpleDTOSubprocess(ws);
		sd2.setInDNDid(dn1.getCode());
		sd2.setOutDNDid(dn2.getCode());
		procD.getSteps().add(sd2);
		
		dn1.getMapping().add(new DTOs.Mapping(procD.getCode(), "jiraIn", sd1.getCode(), "jiraIn")); //into both steps
		dn1.getMapping().add(new DTOs.Mapping(procD.getCode(), "jiraIn", sd2.getCode(), "jiraIn")); //into both steps
		dn2.getMapping().add(new DTOs.Mapping(sd2.getCode(), "jiraOut", procD.getCode(), "jiraOut")); //out of the second
		
		return procD;
	}

	public static DTOs.Process getMinimalGithubBasedProcess() {
		DTOs.Process procD = new DTOs.Process();
		procD.setCode("DemoMinimalGithubProcesses");
		procD.setDescription("Test Accessing Github");
		procD.getInput().put("issueIn", "git_issue");
		procD.getOutput().put("testcaseOut", "git_issue");
		procD.getConditions().put(Conditions.PRECONDITION, "self.in_issueIn->size() = 1");
		DTOs.DecisionNode dn1 = new DTOs.DecisionNode();
		dn1.setCode("dndGitProcStart");
		dn1.setInflowType(InFlowType.AND);
		DTOs.DecisionNode dn2 = new DTOs.DecisionNode();
		dn2.setCode("dndGitProcEnd");
		dn2.setInflowType(InFlowType.AND);
		procD.getDns().add(dn1);
		procD.getDns().add(dn2);
		
		DTOs.Step sd1 = new DTOs.Step();
		sd1.setCode("single1");
		sd1.getInput().put("issueIn", "git_issue");
		sd1.getOutput().put("testcaseOut", "git_issue");
		sd1.getConditions().put(Conditions.PRECONDITION, "self.in_issueIn->size() = 1");
		sd1.getConditions().put(Conditions.POSTCONDITION, "self.out_testcaseOut->forAll( issue | issue.state = 'Closed')");
		//sd1.getIoMapping().put("issueIn2testcaseOut", "self.in_issueIn->forAll(artIn | self.out_jiraOut->exists(artOut  | artOut = artIn)) and self.out_jiraOut->forAll(artOut2 | self.in_jiraIn->exists(artIn2  | artOut2 = artIn2))"); // ensures both sets are identical in content
		sd1.setInDNDid(dn1.getCode());
		sd1.setOutDNDid(dn2.getCode());
		procD.getSteps().add(sd1);
		
		dn1.getMapping().add(new DTOs.Mapping(procD.getCode(), "issueIn", sd1.getCode(), "issueIn")); //into steps
		dn2.getMapping().add(new DTOs.Mapping(sd1.getCode(), "testcaseOut", procD.getCode(), "testcaseOut")); //out of the second
		
		return procD;
	}
	
	
}
