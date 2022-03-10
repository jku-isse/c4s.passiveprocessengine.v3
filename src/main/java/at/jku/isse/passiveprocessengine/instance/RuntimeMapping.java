package at.jku.isse.passiveprocessengine.instance;

import at.jku.isse.designspace.core.model.Instance;


public class RuntimeMapping {

	enum FlowDir { outToIn, //from Step output to Step input
		inToIn, // from process input to Step input
		outToOut, // from Step output to process output
		inToOut //from process input to process output
		};
	
	enum Status { TO_BE_CHECKED, TO_BE_ADDED, TO_BE_REMOVED, CONSISTENT }; 	
		
	ProcessStep fromStep;
	String fromParam;
	Instance art;
	ProcessStep toStep;
	String toParam;
	FlowDir dir = FlowDir.outToIn;
	Status status = Status.TO_BE_CHECKED;
	
	public RuntimeMapping(ProcessStep fromStep, String fromParam, Instance art, ProcessStep toStep, String toParam) {
		super();
		this.fromStep = fromStep;
		this.fromParam = fromParam;
		this.art = art;
		this.toStep = toStep;
		this.toParam = toParam;
	}
	
	public RuntimeMapping(ProcessStep fromStep, String fromParam, Instance art, ProcessStep toStep, String toParam, FlowDir direction) {
		super();
		this.fromStep = fromStep;
		this.fromParam = fromParam;
		this.art = art;
		this.toStep = toStep;
		this.toParam = toParam;
		this.dir = direction;
	}
	
	public RuntimeMapping() {}

	public ProcessStep getFromStep() {
		return fromStep;
	}

	public void setFromStep(ProcessStep fromStep) {
		this.fromStep = fromStep;
	}

	public String getFromParam() {
		return fromParam;
	}

	public void setFromParam(String fromParam) {
		this.fromParam = fromParam;
	}

	public Instance getArtifact() {
		return art;
	}

	public void setArtifact(Instance art) {
		this.art = art;
	}

	public ProcessStep getToStep() {
		return toStep;
	}

	public void setToStep(ProcessStep toStep) {
		this.toStep = toStep;
	}

	public String getToParam() {
		return toParam;
	}

	public void setToParam(String toParam) {
		this.toParam = toParam;
	}

	public FlowDir getDirection() {
		return dir;
	}

	public void setDirection(FlowDir dir) {
		this.dir = dir;
	}

	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}
	
	public RuntimeMapping fluentSetArtifact(Instance art) {
		this.setArtifact(art);
		return this;
	}
	
	public static RuntimeMapping copyFrom(RuntimeMapping template) {
		return new RuntimeMapping(template.getFromStep(),
								template.getFromParam(),
								template.getArtifact(),
								template.getToStep(),
								template.getToParam(),
								template.getDirection());
	}


}
