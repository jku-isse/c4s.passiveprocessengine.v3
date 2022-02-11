package at.jku.isse.passiveprocessengine.definition;

import java.util.ArrayList;
import java.util.List;

public class DecisionNodeDefinition {

	private String id;
	private InFlowType inBranchingType = InFlowType.AND;
	//private OutFlowType outBranchingType = OutFlowType.SYNC;

	private List<MappingDefinition> mappings = new ArrayList<>();
	public List<MappingDefinition> getMappings() {return mappings;}
	
	@Deprecated
	public DecisionNodeDefinition() {
		super();
	}
	
	public DecisionNodeDefinition(String id, InFlowType inflow) {
		this.id = id;
		this.inBranchingType = inflow;
	}
		
	public InFlowType getInBranchingType() {
		return inBranchingType;
	}

	public void setInBranchingType(InFlowType inBranchingType) {
		this.inBranchingType = inBranchingType;
	}

//	public OutFlowType getOutBranchingType() {
//		return outBranchingType;
//	}
//
//	public void setOutBranchingType(OutFlowType outBranchingType) {
//		this.outBranchingType = outBranchingType;
//	}

//	public boolean acceptsWorkflowTask(IWorkflowTask wti) {
//		return acceptsWorkflowTaskForInBranches(wti) || acceptsWorkflowTaskForOutBranches(wti);
//	}
//	
//	public boolean acceptsWorkflowTaskForInBranches(IWorkflowTask wti) {
//		return this.getWorkflowDefinition().getTasksFlowingInto(this).stream()
//			.filter(task -> task.equals(wti.getType()))
//			.findFirst().isPresent();	}
//
//	public boolean acceptsWorkflowTaskForOutBranches(IWorkflowTask wti) {
//		return this.getWorkflowDefinition().getTasksFlowingOutOf(this).stream()
//				.filter(task -> task.equals(wti.getType()))
//				.findFirst().isPresent();				
//	}

	

//	public int countIncomingTasks() {
//		return this.getWorkflowDefinition().getTasksFlowingInto(this).size(); 
//	}
//	
//	public int countOutgoingTasks() {
//		return this.getWorkflowDefinition().getTasksFlowingOutOf(this).size(); 
//	}
//
//	
//	public boolean isSequentialConnectingDN() {
//		return countIncomingTasks() <= 1 && countOutgoingTasks() <= 1;
//	}
	
//	public DecisionNodeInstance createInstance(WorkflowInstance wfi) {
//		DecisionNodeInstance dni = new DecisionNodeInstance(this, wfi, getStateMachine());
//
////		dni.addInBranches(
////				inB.stream()
////					.map(b -> b.createInstance(wfi))
////					.map(bi -> {bi.setDecisionNodeInstance(dni); 
////								bi.setTask(wfi.getNewPlaceHolderTask(bi.getBranchDefinition().getTask())); 
////								return bi; })
////					.collect(Collectors.toList()));
////		dni.addOutBranches(
////				outB.stream()
////					.map(b -> b.createInstance(wfi))
////					.map(bi -> {bi.setDecisionNodeInstance(dni); 
////								bi.setTask(wfi.getNewPlaceHolderTask(bi.getBranchDefinition().getTask()));
////								return bi; })
////					.collect(Collectors.toList())); 		
//		return dni;
//	}

	@Override
	public String toString() {
		return "DND [" + id + " : "+inBranchingType+ "]";
	}

	public static enum InFlowType {
		AND, OR, XOR;
	}
	
//	public static enum OutFlowType {
//		SYNC, ASYNC;
//	}
//	
	
	




}
