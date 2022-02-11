//package at.jku.isse.passiveprocessengine.instance.events;
//
//import java.util.HashMap;
//import java.util.LinkedList;
//import java.util.List;
//import java.util.Map;
//import java.util.UUID;
//import java.util.stream.Collectors;
//import java.util.stream.Stream;
//
//import com.google.gson.annotations.JsonAdapter;
//
//import passiveprocessengine.definition.IWorkflowTask;
//import passiveprocessengine.instance.IWorkflowInstanceObject;
//import passiveprocessengine.persistance.json.ParentEventRef;
//
//public abstract class ProcessChangeEvent {
//
//	protected String id = UUID.randomUUID().toString();
//    protected ChangeType changeType;
//    protected IWorkflowInstanceObject changedObject;
//    protected IWorkflowTask affectedTask;
//    protected transient List<ProcessChangeEvent> cascadingEffect = new LinkedList<>();
//    @JsonAdapter(ParentEventRef.class)
//    protected ProcessChangeEvent parentCause = null;
//    protected Map<String,Object> augmentations = new HashMap<>();
//
////    public WorkflowChangeEvent(IWorkflowInstanceObject changedArtifacts) {
////        this(ChangeType.EMPTY, changedArtifacts);
////    }
//
//    public enum ChangeType {
//        EMPTY,
//        OUTPUT_CHANGED, NEW_OUTPUT, OUTPUT_DELETED,     // output
//        INPUT_CHANGED, NEW_INPUT, INPUT_DELETED,        // input
//        ACTUAL_STATE_CHANGED, EXPECTED_STATE_CHANGED,   // state
//        CREATED,
//        QAEVAL, // constraint fulfillment changed
//        INFLOW_OK, INFLOW_NOTOK, OUTFLOW_TRIGGERED // DNI changes
//        
//    }
//    
//    @Deprecated
//    protected ProcessChangeEvent(ChangeType changeType, IWorkflowInstanceObject changedObject, ProcessChangeEvent parentCause) {
//        this.changeType = changeType;
//        this.changedObject = changedObject;
//        this.parentCause = parentCause;
//        if (changedObject instanceof IWorkflowTask)
//        	affectedTask = (IWorkflowTask) changedObject;
//    }
//
//    protected ProcessChangeEvent(ChangeType changeType, IWorkflowTask changedObject, ProcessChangeEvent parentCause) {
//        this.changeType = changeType;
//        this.changedObject = changedObject;
//        this.affectedTask = changedObject;
//        this.parentCause = parentCause;
//    }
//
//    protected ProcessChangeEvent(ChangeType changeType, IWorkflowInstanceObject changedObject, IWorkflowTask affectedTask, ProcessChangeEvent parentCause) {
//        this.changeType = changeType;
//        this.changedObject = changedObject;
//        this.affectedTask = affectedTask;
//        this.parentCause = parentCause;
//    }
//    
//    public ChangeType getChangeType() {
//        return changeType;
//    }
//
//    public IWorkflowInstanceObject getChangedObject() {
//        return changedObject;
//    }
//
//	public IWorkflowTask getAffectedTask() {
//		return affectedTask;
//	}
//
//	public ProcessChangeEvent getParentCause() {
//		return parentCause;
//	}
//
//
//	public List<ProcessChangeEvent> getCascadingEffect() {
//		return cascadingEffect;
//	}
//
//	public Stream<ProcessChangeEvent> getFlatCascade() {
//		return Stream.concat(Stream.of(this),cascadingEffect.stream().flatMap(event ->  event.getFlatCascade()));
//	}
//	
//	public void printEvents(int indent, StringBuilder sb) {	
//		sb.append(".".repeat(indent)+this.toString()+"\r\n");
//		int newindent = ++indent;
//		this.cascadingEffect.forEach(evt -> evt.printEvents(newindent, sb));
//	}
//
//	@Override
//	public int hashCode() {
//		final int prime = 31;
//		int result = 1;
//		result = prime * result + ((id == null) ? 0 : id.hashCode());
//		return result;
//	}
//
//	@Override
//	public boolean equals(Object obj) {
//		if (this == obj)
//			return true;
//		if (obj == null)
//			return false;
//		if (getClass() != obj.getClass())
//			return false;
//		ProcessChangeEvent other = (ProcessChangeEvent) obj;
//		if (id == null) {
//			if (other.id != null)
//				return false;
//		} else if (!id.equals(other.id))
//			return false;
//		return true;
//	}
//
//	public Map<String, Object> getAugmentations() {
//		return augmentations;
//	}
//
//	@Override
//	public String toString() {
//		String aug = augmentations.size() > 0 ? augmentations.values().stream().map(a ->a.toString()).collect(Collectors.joining("|", "::", "")) : "";
//		return "[" + changeType +aug+ ", changedObj='" + changedObject.getId() + "'";
//	}
//
//	public String getId() {
//		return id;
//	}
//
//}
