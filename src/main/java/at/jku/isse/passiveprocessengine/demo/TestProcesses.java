package at.jku.isse.passiveprocessengine.demo;

import java.util.ArrayList;

import at.jku.isse.passiveprocessengine.core.PPEInstanceType;
import at.jku.isse.passiveprocessengine.definition.activeobjects.ConstraintSpec;
import at.jku.isse.passiveprocessengine.definition.activeobjects.DecisionNodeDefinition;
import at.jku.isse.passiveprocessengine.definition.activeobjects.DecisionNodeDefinition.InFlowType;
import at.jku.isse.passiveprocessengine.definition.activeobjects.MappingDefinition;
import at.jku.isse.passiveprocessengine.definition.activeobjects.ProcessDefinition;
import at.jku.isse.passiveprocessengine.definition.activeobjects.StepDefinition;
import at.jku.isse.passiveprocessengine.definition.serialization.DTOs;
import at.jku.isse.passiveprocessengine.definition.serialization.DTOs.Constraint;
import at.jku.isse.passiveprocessengine.instance.ProcessException;
import at.jku.isse.passiveprocessengine.instance.StepLifecycle.Conditions;

public class TestProcesses {

	TestArtifacts artifactFactory;
	
	public TestProcesses(TestArtifacts artifactFactory) {
		this.artifactFactory = artifactFactory;
	}
	
	// process with two parallel subtasks, each taking the same jira issue as input, both completing when that issue is set to closed
	// no qa constraints applied, datamapping only for one subtask, subproc complete when both subtasks are complete (AND cond)
	public ProcessDefinition getSimpleSubprocessDefinition(boolean doInitType) throws ProcessException {
		PPEInstanceType typeJira = artifactFactory.getJiraInstanceType();
		ProcessDefinition procDef = ProcessDefinition.getInstance("subproc1");
		procDef.addExpectedInput("jiraIn", typeJira);
		procDef.setCondition(Conditions.PRECONDITION, "self.in_jiraIn->size() = 1");
		procDef.addExpectedOutput("jiraOut", typeJira);
		//procDef.setCondition(Conditions.POSTCONDITION, "self.out_jiraOut->size() > 0");
		//no definition how many outputs, there is a possibility to provide output, but completion is upon subtask completion
		DecisionNodeDefinition dnd1 = procDef.createDecisionNodeDefinition("dndSubStart" );
		DecisionNodeDefinition dnd2 = procDef.createDecisionNodeDefinition("dndSubEnd" );
		StepDefinition sd1 = procDef.createStepDefinition("subtask1" );
		sd1.addExpectedInput("jiraIn", typeJira);
		sd1.addExpectedOutput("jiraOut", typeJira);
		sd1.setCondition(Conditions.PRECONDITION, "self.in_jiraIn->size() = 1");
		sd1.setCondition(Conditions.POSTCONDITION, "self.in_jiraIn->forAll( issue | issue.state = 'Closed')");
		sd1.addInputToOutputMappingRule("jiraOut", "self.in_jiraIn"); //->forAll(artIn | self.out_jiraOut->exists(artOut  | artOut = artIn)) and "
				//+ " self.out_jiraOut->forAll(artOut2 | self.in_jiraIn->exists(artIn2  | artOut2 = artIn2))"); // ensures both sets are identical in content
		sd1.setInDND(dnd1);
		sd1.setOutDND(dnd2);
		sd1.setSpecOrderIndex(11);
		StepDefinition sd2 = procDef.createStepDefinition("subtask2" );
		sd2.addExpectedInput("jiraIn", typeJira);
		sd2.setCondition(Conditions.PRECONDITION, "self.in_jiraIn->size() = 1");
		sd2.setCondition(Conditions.POSTCONDITION, "self.in_jiraIn->forAll( issue | issue.state = 'Closed')");
		sd2.setInDND(dnd1);
		sd2.setOutDND(dnd2);
		sd2.setSpecOrderIndex(12);
		dnd1.addDataMappingDefinition(MappingDefinition.getInstance(procDef.getName(), "jiraIn", sd1.getName(), "jiraIn",  ws)); //into both steps
		dnd1.addDataMappingDefinition(MappingDefinition.getInstance(procDef.getName(), "jiraIn", sd2.getName(), "jiraIn",  ws)); //into both steps
		dnd1.setDepthIndexRecursive(4);
		dnd2.addDataMappingDefinition(MappingDefinition.getInstance(sd1.getName(), "jiraOut", procDef.getName(), "jiraOut",  ws)); //out of the first
		dnd2.setDepthIndexRecursive(5);
		if (doInitType)
			procDef.initializeInstanceTypes(false);
		procDef.isImmediateInstantiateAllStepsEnabled(false); //ensure old behavior
		return procDef;
	}

	// simple process, two AND branches, on of the subbranches is a subprocess, no QA, simple datamapping, all using same completion condition of step set to closed
	public ProcessDefinition getSimpleSuperProcessDefinition( ) throws ProcessException {
		PPEInstanceType typeJira = artifactFactory.getJiraInstanceType();
		ProcessDefinition procDef = ProcessDefinition.getInstance("parentproc1" );
		procDef.addExpectedInput("jiraIn", typeJira);
		procDef.setCondition(Conditions.PRECONDITION, "self.in_jiraIn->size() = 1");
		procDef.addExpectedOutput("jiraOut", typeJira);
		//procDef.setCondition(Conditions.POSTCONDITION, "self.out_jiraOut->size() > 0");
		//no definition how many outputs, there is a possibility to provide output, but completion is upon subtask completion
		DecisionNodeDefinition dnd1 = procDef.createDecisionNodeDefinition("dndParentStart" );
		DecisionNodeDefinition dnd2 = procDef.createDecisionNodeDefinition("dndParentEnd" );
		StepDefinition sd1 = procDef.createStepDefinition("paratask1" );
		sd1.addExpectedInput("jiraIn", typeJira);
		sd1.setCondition(Conditions.PRECONDITION, "self.in_jiraIn->size() = 1");
		sd1.setCondition(Conditions.POSTCONDITION, "self.in_jiraIn->forAll( issue | issue.state = 'Closed')");
		sd1.setInDND(dnd1);
		sd1.setOutDND(dnd2);
		sd1.setSpecOrderIndex(1);
		StepDefinition sd2 = getSimpleSubprocessDefinition(ws, false);
		// we need to wire up the step definiton:
		sd2.setProcess(procDef);
		procDef.addStepDefinition(sd2);
		//inputs and output set in process/step definition, pre and post cond as well
		sd2.setInDND(dnd1);
		sd2.setOutDND(dnd2);
		sd2.setSpecOrderIndex(2);

		dnd1.addDataMappingDefinition(MappingDefinition.getInstance(procDef.getName(), "jiraIn", sd1.getName(), "jiraIn",  ws)); //into both steps
		dnd1.addDataMappingDefinition(MappingDefinition.getInstance(procDef.getName(), "jiraIn", sd2.getName(), "jiraIn",  ws)); //into both steps
		dnd1.setDepthIndexRecursive(1);
		dnd2.addDataMappingDefinition(MappingDefinition.getInstance(sd2.getName(), "jiraOut", procDef.getName(), "jiraOut",  ws)); //out of the second
		dnd2.setDepthIndexRecursive(2);
		procDef.initializeInstanceTypes(false);
		procDef.isImmediateInstantiateAllStepsEnabled(false); //ensure old behavior
		return procDef;
	}

	

	public ProcessDefinition getComplexSingleStepProcessDefinition( ) throws ProcessException {
		PPEInstanceType typeJira = artifactFactory.getJiraInstanceType();
		ProcessDefinition procDef = ProcessDefinition.getInstance("proc1" );
		procDef.addExpectedInput("jiraIn", typeJira);
		procDef.addExpectedOutput("jiraOut", typeJira);
		DecisionNodeDefinition dnd1 = procDef.createDecisionNodeDefinition("dnd1" );
		DecisionNodeDefinition dnd2 =  procDef.createDecisionNodeDefinition("dnd2" );

		StepDefinition sd1 = procDef.createStepDefinition("sd1" );
		sd1.addExpectedInput("jiraIn", typeJira);
		sd1.addExpectedOutput("jiraOut", typeJira);
		sd1.addInputToOutputMappingRule("jiraOut",
				"self.in_jiraIn"
					+ "->any()"
					+ "->asType(<"+typeJira.getQualifiedName()+">)"
							+ ".requirements"
							//	+ "->asSet() "
							//	+"->symmetricDifference(self.out_jiraOut) " +
							//	"->size() = 0"
								);
		sd1.setCondition(Conditions.PRECONDITION, "self.in_jiraIn->size() = 1 "
				+ "and self.in_jiraIn->forAll( issue | issue.state = 'Open') ");
		sd1.setCondition(Conditions.POSTCONDITION, "self.out_jiraOut->forAll( issue | issue.state = 'Closed') "
				+ "and self.out_jiraOut->size() > 0 "
				+ "and self.in_jiraIn->forAll( issue2 | issue2.state <> 'InProgress') ");
		//QAConstraintSpec qa2 = QAConstraintSpec.createInstance("sd1-qa2-state", "self.out_jiraIn->forAll( issue | issue.state <> 'InProgress')", "None of the issue states must be 'InProgress'", 2,ws);
		//sd1.addQAConstraint(qa2);
		sd1.setInDND(dnd1);
		sd1.setOutDND(dnd2);
		sd1.setSpecOrderIndex(1);

		dnd1.addDataMappingDefinition(MappingDefinition.getInstance(procDef.getName(), "jiraIn", sd1.getName(), "jiraIn",  ws));
		procDef.initializeInstanceTypes(false);
		procDef.isImmediateInstantiateAllStepsEnabled(false); //ensure old behavior
		return procDef;
	}

	public ProcessDefinition get2StepProcessDefinitionWithSymmetricDiffMapping( ) throws ProcessException {
		PPEInstanceType typeJira = artifactFactory.getJiraInstanceType();
		ProcessDefinition procDef = ProcessDefinition.getInstance("proc1" );
		procDef.addExpectedInput("jiraIn", typeJira);
		DecisionNodeDefinition dnd1 = procDef.createDecisionNodeDefinition("dnd1" );
		DecisionNodeDefinition dnd2 =  procDef.createDecisionNodeDefinition("dnd2" );

		StepDefinition sd1 = procDef.createStepDefinition("sd1" );
		sd1.addExpectedInput("jiraIn", typeJira);
		sd1.addExpectedOutput("jiraOut", typeJira);

		sd1.addInputToOutputMappingRule("jiraOut",
			"self.in_jiraIn"
				+ "->any()"
				+ "->asType(<"+typeJira.getQualifiedName()+">)"
						+ ".requirements"
							//+ "->asSet() "  //AUTOMATICALLY ADDED
							//+"->symmetricDifference(self.out_jiraOut) " +
							//"->size() = 0"
							);
		sd1.setInDND(dnd1);
		sd1.setOutDND(dnd2);
		sd1.setSpecOrderIndex(1);
		dnd1.addDataMappingDefinition(MappingDefinition.getInstance(procDef.getName(), "jiraIn", sd1.getName(), "jiraIn",  ws));
		procDef.initializeInstanceTypes(false);
		procDef.isImmediateInstantiateAllStepsEnabled(false); //ensure old behavior
		return procDef;
	}

	public ProcessDefinition get2StepProcessDefinitionWithUnionMapping( ) throws ProcessException {
		PPEInstanceType typeJira = artifactFactory.getJiraInstanceType();
		ProcessDefinition procDef = ProcessDefinition.getInstance("proc1" );
		procDef.addExpectedInput("jiraIn", typeJira);
		procDef.addExpectedInput("jiraIn2", typeJira);
		DecisionNodeDefinition dnd1 = procDef.createDecisionNodeDefinition("dnd1" );
		DecisionNodeDefinition dnd2 =  procDef.createDecisionNodeDefinition("dnd2" );

		StepDefinition sd1 = procDef.createStepDefinition("sd1" );
		sd1.addExpectedInput("jiraIn", typeJira);
		sd1.addExpectedInput("jiraIn2", typeJira);
		sd1.addExpectedOutput("jiraOut", typeJira);

		sd1.addInputToOutputMappingRule("jiraOut",
			"self.in_jiraIn2->union(self.in_jiraIn"
				+ "->any()"
				+ "->asType(<"+typeJira.getQualifiedName()+">)"
						+ ".requirements)"
						//	+ "->asSet()) "
						//	+"->symmetricDifference(self.out_jiraOut) " +
						//	"->size() = 0"
							);

		sd1.setInDND(dnd1);
		sd1.setOutDND(dnd2);
		sd1.setSpecOrderIndex(1);
		dnd1.addDataMappingDefinition(MappingDefinition.getInstance(procDef.getName(), "jiraIn", sd1.getName(), "jiraIn",  ws));
		dnd1.addDataMappingDefinition(MappingDefinition.getInstance(procDef.getName(), "jiraIn2", sd1.getName(), "jiraIn2",  ws));
		procDef.initializeInstanceTypes(false);
		procDef.isImmediateInstantiateAllStepsEnabled(false); //ensure old behavior
		return procDef;
	}

	public ProcessDefinition get2StepProcessDefinitionWithExistsCheck( ) throws ProcessException {
		PPEInstanceType typeJira = artifactFactory.getJiraInstanceType();
		ProcessDefinition procDef = ProcessDefinition.getInstance("proc1" );
		procDef.addExpectedInput("jiraIn", typeJira);
		procDef.addExpectedInput("jiraIn2", typeJira);
		DecisionNodeDefinition dnd1 = procDef.createDecisionNodeDefinition("dnd1" );
		DecisionNodeDefinition dnd2 =  procDef.createDecisionNodeDefinition("dnd2" );

		StepDefinition sd1 = procDef.createStepDefinition("sd1" );
		sd1.addExpectedInput("jiraIn", typeJira);
		sd1.addExpectedInput("jiraIn2", typeJira);
		sd1.addExpectedOutput("jiraOut", typeJira);

		sd1.setCondition(Conditions.PRECONDITION, "self.in_jiraIn2->union( \r\n"
				+ "self.in_jiraIn->any()->asType(<"+typeJira.getQualifiedName()+">).requirements \r\n"
				+ ") \r\n" // combined set of instances
				+ "->exists(req  | req.state='Open')");


		sd1.setCondition(Conditions.POSTCONDITION, "self.in_jiraIn2->union( \r\n"
				+ "self.in_jiraIn->any()->asType(<"+typeJira.getQualifiedName()+">).requirements \r\n"
				+ " 	->select(req | req.parent.isDefined() ) \r\n"
				+ " 	->collect(req2 | req2.parent) \r\n"
				+ ") \r\n" // combined set of instances
				+ "	->exists(parent : <"+typeJira.getQualifiedName()+"> | parent.state='Closed')");

		sd1.setInDND(dnd1);
		sd1.setOutDND(dnd2);
		sd1.setSpecOrderIndex(1);
		dnd1.addDataMappingDefinition(MappingDefinition.getInstance(procDef.getName(), "jiraIn", sd1.getName(), "jiraIn",  ws));
		dnd1.addDataMappingDefinition(MappingDefinition.getInstance(procDef.getName(), "jiraIn2", sd1.getName(), "jiraIn2",  ws));
		procDef.initializeInstanceTypes(false);
		procDef.isImmediateInstantiateAllStepsEnabled(false); //ensure old behavior
		return procDef;
	}

	public ProcessDefinition getSingleStepProcessDefinitionWithOutput( ) throws ProcessException {
		PPEInstanceType typeJira = artifactFactory.getJiraInstanceType();
		ProcessDefinition procDef = ProcessDefinition.getInstance("proc1" );
		procDef.addExpectedInput("jiraIn", typeJira);

		DecisionNodeDefinition dnd1 = procDef.createDecisionNodeDefinition("dnd1" );
		DecisionNodeDefinition dnd2 =  procDef.createDecisionNodeDefinition("dnd2" );

		StepDefinition sd1 = procDef.createStepDefinition("sd1" );
		sd1.addExpectedInput("jiraIn", typeJira);
		sd1.addExpectedOutput("jiraOut", typeJira);

		sd1.setCondition(Conditions.PRECONDITION, "self.in_jiraIn->forAll( \r\n"
				+ "issue | issue.state<>'Open')");

		sd1.addInputToOutputMappingRule("jiraOut",
				"self.in_jiraIn->collect(issue | issue.parent)"
							//	+ "->asSet()) "
							//	+"->symmetricDifference(self.out_jiraOut) " +
							//	"->size() = 0"
								);

		sd1.setCondition(Conditions.POSTCONDITION, "self.out_jiraOut->size() > 0 and self.out_jiraOut->forAll( \r\n"
				+ "issue | issue.state='Closed')");

		sd1.setInDND(dnd1);
		sd1.setOutDND(dnd2);
		sd1.setSpecOrderIndex(1);
		dnd1.addDataMappingDefinition(MappingDefinition.getInstance(procDef.getName(), "jiraIn", sd1.getName(), "jiraIn",  ws));

		procDef.initializeInstanceTypes(false);
		procDef.isImmediateInstantiateAllStepsEnabled(false); //ensure old behavior
		return procDef;
	}

	public ProcessDefinition getSimpleXORDefinition( ) throws ProcessException {
		PPEInstanceType typeJira = artifactFactory.getJiraInstanceType();
		ProcessDefinition procDef = ProcessDefinition.getInstance("xorproc1" );
		procDef.addExpectedInput("jiraIn", typeJira);
		procDef.addExpectedOutput("jiraOut", typeJira);
		//procDef.setCondition(Conditions.POSTCONDITION, "self.out_jiraOut->size() > 0");
		//no definition how many outputs, there is a possibility to provide output, but completion is upon subtask completion
		DecisionNodeDefinition dnd1 = procDef.createDecisionNodeDefinition("dndXORStart" );
		DecisionNodeDefinition dnd2 = procDef.createDecisionNodeDefinition("dndXOREnd" );
		dnd2.setInflowType(InFlowType.XOR);
		StepDefinition sd1 = procDef.createStepDefinition("alt1" );
		sd1.addExpectedInput("jiraIn", typeJira);
		sd1.addExpectedOutput("jiraOut", typeJira);
		sd1.setCondition(Conditions.PRECONDITION, "self.in_jiraIn->size() = 1");
		sd1.setCondition(Conditions.POSTCONDITION, "self.out_jiraOut->size() > 0 and self.out_jiraOut->forAll( issue | issue.state = 'Closed')");
		sd1.addInputToOutputMappingRule("jiraOut", "self.in_jiraIn->any()->asType(<"+typeJira.getQualifiedName()+">).bugs"); //->forAll(artIn | self.out_jiraOut->exists(artOut  | artOut = artIn)) and "
				//+ " self.out_jiraOut->forAll(artOut2 | self.in_jiraIn->exists(artIn2  | artOut2 = artIn2))"); // ensures both sets are identical in content
		sd1.setInDND(dnd1);
		sd1.setOutDND(dnd2);
		sd1.setSpecOrderIndex(11);
		StepDefinition sd2 = procDef.createStepDefinition("alt2" );
		sd2.addExpectedInput("jiraIn", typeJira);
		sd2.addExpectedOutput("jiraOut", typeJira);
		sd2.setCondition(Conditions.PRECONDITION, "self.in_jiraIn->size() = 1");
		sd2.setCondition(Conditions.POSTCONDITION, "self.out_jiraOut->size() > 0 and self.out_jiraOut->forAll( issue | issue.state = 'InProgress')");
		sd2.setCondition(Conditions.CANCELATION, "self.out_jiraOut->size() > 0 and self.out_jiraOut->forAll( issue | issue.state = 'Released')");
		sd2.addInputToOutputMappingRule("jiraOut", "self.in_jiraIn->any()->asType(<"+typeJira.getQualifiedName()+">).requirements");
		sd2.setInDND(dnd1);
		sd2.setOutDND(dnd2);
		sd2.setSpecOrderIndex(12);
		dnd1.addDataMappingDefinition(MappingDefinition.getInstance(procDef.getName(), "jiraIn", sd1.getName(), "jiraIn",  ws)); //into both steps
		dnd1.addDataMappingDefinition(MappingDefinition.getInstance(procDef.getName(), "jiraIn", sd2.getName(), "jiraIn",  ws)); //into both steps
		dnd1.setDepthIndexRecursive(4);
		dnd2.addDataMappingDefinition(MappingDefinition.getInstance(sd1.getName(), "jiraOut", procDef.getName(), "jiraOut",  ws)); //out of the first
		dnd2.addDataMappingDefinition(MappingDefinition.getInstance(sd2.getName(), "jiraOut", procDef.getName(), "jiraOut",  ws)); //out of the second
		dnd2.setDepthIndexRecursive(5);

		procDef.initializeInstanceTypes(false);
		procDef.isImmediateInstantiateAllStepsEnabled(false); //ensure old behavior
		return procDef;
	}

	public ProcessDefinition getSimpleTemporalProcessDefinitionWithoutQA( ) throws ProcessException {
		PPEInstanceType typeJira = artifactFactory.getJiraInstanceType();
		ProcessDefinition procDef = ProcessDefinition.getInstance("temporal1" );
		procDef.addExpectedInput("jiraIn", typeJira);
		DecisionNodeDefinition dnd1 = procDef.createDecisionNodeDefinition("start1" );
		DecisionNodeDefinition dnd2 =  procDef.createDecisionNodeDefinition("end2" );
		StepDefinition sd1 = procDef.createStepDefinition("step1" );
		sd1.addExpectedInput("jiraIn", typeJira);
		sd1.setInDND(dnd1);
		sd1.setOutDND(dnd2);
		sd1.setSpecOrderIndex(1);

		sd1.setCondition(Conditions.PRECONDITION, "self.in_jiraIn->size() > 0");
		sd1.setCondition(Conditions.POSTCONDITION, "self.in_jiraIn->forAll( issue1 | issue1.requirements->size() > 0) and \r\n"
												 + "self.in_jiraIn->forAll( issue | issue.requirements\r\n"
												 				//+ "->forAll(req | eventually(req.state = 'ReadyForReview') and eventually( always( req.state = 'ReadyForReview' ,  req.state = 'Released'))) ) ");
																	+ "->forAll(req | eventually(req.state = 'ReadyForReview') and eventually( everytime( req.state = 'ReadyForReview' ,  eventually(req.state = 'Released')))) ) ");
		dnd1.addDataMappingDefinition(MappingDefinition.getInstance(procDef.getName(), "jiraIn", sd1.getName(), "jiraIn",  ws));
		procDef.setDepthIndexRecursive(0);
		procDef.initializeInstanceTypes(false);
		procDef.isImmediateInstantiateAllStepsEnabled(true); // ensure new behavior
		return procDef;
	}

	public ProcessDefinition getSimpleTemporalProcessDefinitionWithQA( ) throws ProcessException {
		PPEInstanceType typeJira = artifactFactory.getJiraInstanceType();
		ProcessDefinition procDef = ProcessDefinition.getInstance("temporal1" );
		procDef.addExpectedInput("jiraIn", typeJira);
		DecisionNodeDefinition dnd1 = procDef.createDecisionNodeDefinition("start1" );
		DecisionNodeDefinition dnd2 =  procDef.createDecisionNodeDefinition("end2" );
		StepDefinition sd1 = procDef.createStepDefinition("step1" );
		sd1.addExpectedInput("jiraIn", typeJira);
		sd1.setInDND(dnd1);
		sd1.setOutDND(dnd2);
		sd1.setSpecOrderIndex(1);

		sd1.setCondition(Conditions.PRECONDITION, "self.in_jiraIn->size() > 0");
		sd1.setCondition(Conditions.POSTCONDITION, "self.in_jiraIn->forAll( issue1 | issue1.requirements->size() > 0) and \r\n"
												 + "self.in_jiraIn->forAll( issue | issue.requirements\r\n"
																					+ "->forAll(req | eventually(req.state = 'ReadyForReview') and eventually( everytime( req.state = 'ReadyForReview' ,  eventually(req.state = 'Released')))))");
		//until(not(b), a) and always(b, next(until(not(b), a)))
		ConstraintSpec qa3 = ConstraintSpec.createInstance(Conditions.QA, "sd1-reviewAlwaysBeforeReleased"
				, "self.in_jiraIn->forAll( issue | issue.requirements\r\n"
						+ "->forAll(req | until(req.state <> 'Released' , req.state = 'ReadyForReview') \r\n"
						+ "					and everytime( req.state = 'Released', next( asSoonAs( req.state <> 'Released',  until( req.state <> 'Released', req.state = 'ReadyForReview') ) ) ) ) ) ", "All linked requirements must be in state ReadyForReview before being in state Released",3 );
		sd1.addQAConstraint(qa3);

		dnd1.addDataMappingDefinition(MappingDefinition.getInstance(procDef.getName(), "jiraIn", sd1.getName(), "jiraIn",  ws));
		procDef.setDepthIndexRecursive(0);
		procDef.initializeInstanceTypes(false);
		procDef.isImmediateInstantiateAllStepsEnabled(true); // ensure new behavior
		return procDef;
	}

	// not ( eventually(a, next( eventually(a))))
	public ProcessDefinition getSimpleTemporalProcessDefinitionWithSequenceAbsence( ) throws ProcessException {
		PPEInstanceType typeJira = artifactFactory.getJiraInstanceType();
		ProcessDefinition procDef = ProcessDefinition.getInstance("temporal1" );
		procDef.addExpectedInput("jiraIn", typeJira);
		DecisionNodeDefinition dnd1 = procDef.createDecisionNodeDefinition("start1" );
		DecisionNodeDefinition dnd2 =  procDef.createDecisionNodeDefinition("end2" );
		StepDefinition sd1 = procDef.createStepDefinition("step1" );
		sd1.addExpectedInput("jiraIn", typeJira);
		sd1.setInDND(dnd1);
		sd1.setOutDND(dnd2);
		sd1.setSpecOrderIndex(1);

		sd1.setCondition(Conditions.PRECONDITION, "self.in_jiraIn->size() > 0");
		sd1.setCondition(Conditions.POSTCONDITION, "self.in_jiraIn->forAll( issue1 | issue1.requirements->size() > 0) and \r\n"
												 + "self.in_jiraIn->forAll( issue | issue.requirements\r\n"
													//+ "->forAll(req | eventually(req.state = 'Released') and not(eventually(req.state = 'Released' , (next( eventually(req.state = 'Released') ) ) ) ) ) )");
																					+ "->forAll(req | eventually(req.state = 'Released') and asSoonAs(req.state = 'Released' , always(req.state = 'Released') or not (next( asSoonAs(req.state <> 'Released' , req.state = 'Released') ) )) ) )");

		dnd1.addDataMappingDefinition(MappingDefinition.getInstance(procDef.getName(), "jiraIn", sd1.getName(), "jiraIn",  ws));
		procDef.setDepthIndexRecursive(0);
		procDef.initializeInstanceTypes(false);
		procDef.isImmediateInstantiateAllStepsEnabled(true); // ensure new behavior
		return procDef;
	}

	public DTOs.Process getSimpleDTOSubprocess( ) {
		PPEInstanceType typeJira = artifactFactory.getJiraInstanceType();
		DTOs.Process procD = new DTOs.Process();
		procD.setCode("TestSerializeProc1");
		procD.setDescription("Test for Serialization");
		procD.getInput().put("jiraIn", typeJira.getName());
		procD.getOutput().put("jiraOut", typeJira.getName());
		procD.getConditions().computeIfAbsent(Conditions.PRECONDITION, k -> new ArrayList<>()).add(new Constraint("self.in_jiraIn->size() = 1"));

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
		sd1.getInput().put("jiraIn", typeJira.getName());
		sd1.getOutput().put("jiraOut", typeJira.getName());
		sd1.getConditions().computeIfAbsent(Conditions.PRECONDITION, k -> new ArrayList<>()).add(new Constraint("self.in_jiraIn->size() = 1"));
		sd1.getConditions().computeIfAbsent(Conditions.POSTCONDITION, k -> new ArrayList<>()).add(new Constraint( "self.in_jiraIn->size() = 1 and self.in_jiraIn->forAll( issue | issue.state = 'Closed')"));
		sd1.getIoMapping().put("jiraOut", "self.in_jiraIn");//->forAll(artIn | self.out_jiraOut->exists(artOut  | artOut = artIn)) and self.out_jiraOut->forAll(artOut2 | self.in_jiraIn->exists(artIn2  | artOut2 = artIn2))"); // ensures both sets are identical in content
		sd1.setInDNDid(dn1.getCode());
		sd1.setOutDNDid(dn2.getCode());
		procD.getSteps().add(sd1);

		DTOs.Step sd2 = new DTOs.Step();
		sd2.setCode("subtask2");
		sd2.getInput().put("jiraIn", typeJira.getName());
		sd2.getConditions().computeIfAbsent(Conditions.PRECONDITION, k -> new ArrayList<>()).add(new Constraint("self.in_jiraIn->size() = 1"));
		sd2.getConditions().computeIfAbsent(Conditions.POSTCONDITION, k -> new ArrayList<>()).add(new Constraint("self.in_jiraIn->size() = 1 and self.in_jiraIn->forAll( issue | issue.state = 'Closed')"));
		sd2.setInDNDid(dn1.getCode());
		sd2.setOutDNDid(dn2.getCode());
		procD.getSteps().add(sd2);

		dn1.getMapping().add(new DTOs.Mapping(procD.getCode(), "jiraIn", sd1.getCode(), "jiraIn")); //into both steps
		dn1.getMapping().add(new DTOs.Mapping(procD.getCode(), "jiraIn", sd2.getCode(), "jiraIn")); //into both steps
		dn2.getMapping().add(new DTOs.Mapping(sd1.getCode(), "jiraOut", procD.getCode(), "jiraOut")); //out of the first

		return procD;
	}

	public DTOs.Process getSimpleSuperDTOProcessDefinition( ) {
		PPEInstanceType typeJira = artifactFactory.getJiraInstanceType();
		DTOs.Process procD = new DTOs.Process();
		procD.setCode("TestSerializeParentProc1");
		procD.setDescription("Test for Serialization");
		procD.getInput().put("jiraIn", typeJira.getName());
		procD.getOutput().put("jiraOut", typeJira.getName());
		procD.getConditions().computeIfAbsent(Conditions.PRECONDITION, k -> new ArrayList<>()).add(new Constraint( "self.in_jiraIn->size() = 1"));
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
		sd1.getInput().put("jiraIn", typeJira.getName());
		sd1.getConditions().computeIfAbsent(Conditions.PRECONDITION, k -> new ArrayList<>()).add(new Constraint( "self.in_jiraIn->size() = 1"));
		sd1.getConditions().computeIfAbsent(Conditions.POSTCONDITION, k -> new ArrayList<>()).add(new Constraint( "self.in_jiraIn->forAll( issue | issue.state = 'Closed')"));
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

	public DTOs.Process getMinimalGithubBasedProcess() {
		DTOs.Process procD = new DTOs.Process();
		procD.setCode("DemoMinimalGithubProcesses");
		procD.setDescription("Test Accessing Github");
		procD.getInput().put("issueIn", "git_issue");
		procD.getOutput().put("testcaseOut", "git_issue");
		procD.getConditions().computeIfAbsent(Conditions.PRECONDITION, k -> new ArrayList<>()).add(new Constraint( "self.in_issueIn->size() = 1"));
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
		sd1.getConditions().computeIfAbsent(Conditions.PRECONDITION, k -> new ArrayList<>()).add(new Constraint( "self.in_issueIn->size() = 1"));
		sd1.getConditions().computeIfAbsent(Conditions.POSTCONDITION, k -> new ArrayList<>()).add(new Constraint( "self.out_testcaseOut->forAll( issue | issue.state = 'Closed')"));
		//sd1.getIoMapping().put("issueIn2testcaseOut", "self.in_issueIn->forAll(artIn | self.out_jiraOut->exists(artOut  | artOut = artIn)) and self.out_jiraOut->forAll(artOut2 | self.in_jiraIn->exists(artIn2  | artOut2 = artIn2))"); // ensures both sets are identical in content
		sd1.setInDNDid(dn1.getCode());
		sd1.setOutDNDid(dn2.getCode());
		procD.getSteps().add(sd1);

		dn1.getMapping().add(new DTOs.Mapping(procD.getCode(), "issueIn", sd1.getCode(), "issueIn")); //into steps
		dn2.getMapping().add(new DTOs.Mapping(sd1.getCode(), "testcaseOut", procD.getCode(), "testcaseOut")); //out of the second

		return procD;
	}


}
