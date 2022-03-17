package at.jku.isse.passiveprocessengine.instance;

import at.jku.isse.designspace.core.model.Instance;


public class RuntimeMapping {
	
	public enum FlowDir { outToIn, //from Step output to Step input
	inToIn, // from process input to Step input
	outToOut, // from Step output to process output
	inToOut //from process input to process output
	};
	
	//enum Status { TO_BE_CHECKED, TO_BE_ADDED, TO_BE_REMOVED, CONSISTENT }; 	
		
	ProcessStep fromStep;
	String fromParam;
	Instance art;
	ProcessStep toStep;
	String toParam;
	FlowDir dir = FlowDir.outToIn;
	//Status status = Status.TO_BE_CHECKED;
	
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

//	public Status getStatus() {
//		return status;
//	}
//
//	public void setStatus(Status status) {
//		this.status = status;
//	}
	
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

	@Override
	public String toString() {
		String from = fromStep != null ? fromStep.getName() : "UNKNOWN";
		String to = fromStep != null ? toStep.getName() : "UNKNOWN";
		return "Mapping " + dir + " [art=" + art.name() + " from=" + from + ":" + fromParam + ", to="
				+ to + ":" + toParam + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((art == null) ? 0 : art.id().hashCode());
		result = prime * result + ((toParam == null) ? 0 : toParam.hashCode());
		result = prime * result + ((toStep == null) ? 0 : toStep.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RuntimeMapping other = (RuntimeMapping) obj;
		if (art == null) {
			if (other.art != null)
				return false;
		} else if (!art.equals(other.art))
			return false;
		if (toParam == null) {
			if (other.toParam != null)
				return false;
		} else if (!toParam.equals(other.toParam))
			return false;
		if (toStep == null) {
			if (other.toStep != null)
				return false;
		} else if (!toStep.equals(other.toStep))
			return false;
		return true;
	}


}
