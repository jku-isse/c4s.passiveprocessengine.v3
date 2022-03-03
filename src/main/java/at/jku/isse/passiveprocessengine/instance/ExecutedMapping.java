package at.jku.isse.passiveprocessengine.instance;

import at.jku.isse.designspace.core.model.Instance;


public class ExecutedMapping {

	enum DIR { outToIn, //from Step output to Step input
		inToIn, // from process input to Step input
		outToOut, // from Step output to process output
		inToOut //from process input to process output
		};
	
	ProcessStep fromStep;
	String fromParam;
	Instance art;
	ProcessStep toStep;
	String toParam;
	DIR dir = DIR.outToIn;
	
	public ExecutedMapping(ProcessStep fromStep, String fromParam, Instance art, ProcessStep toStep, String toParam) {
		super();
		this.fromStep = fromStep;
		this.fromParam = fromParam;
		this.art = art;
		this.toStep = toStep;
		this.toParam = toParam;
	}
	
	public ExecutedMapping(ProcessStep fromStep, String fromParam, Instance art, ProcessStep toStep, String toParam, DIR direction) {
		super();
		this.fromStep = fromStep;
		this.fromParam = fromParam;
		this.art = art;
		this.toStep = toStep;
		this.toParam = toParam;
		this.dir = direction;
	}
	
	public ExecutedMapping() {}

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

	public DIR getDirection() {
		return dir;
	}

	public void setDirection(DIR dir) {
		this.dir = dir;
	}

	
	public ExecutedMapping fluentSetArtifact(Instance art) {
		this.setArtifact(art);
		return this;
	}
	
	public static ExecutedMapping copyFrom(ExecutedMapping template) {
		return new ExecutedMapping(template.getFromStep(),
								template.getFromParam(),
								template.getArtifact(),
								template.getToStep(),
								template.getToParam(),
								template.getDirection());
	}
}
