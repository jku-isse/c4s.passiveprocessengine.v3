package at.jku.isse.passiveprocessengine.monitoring;

public class RepairFeatureToggle {

	boolean sortEnabled=false;
	boolean restComplexityEnabled=false;
	boolean identifyUndoEnabled=false;
	
	public RepairFeatureToggle() {
		this.sortEnabled = false;
		this.restComplexityEnabled = false;
		this.identifyUndoEnabled = false;
	}
	public RepairFeatureToggle(boolean sortEnabled, boolean restComplexityEnabled,
			boolean identifyUndoEnabled) {
		this.sortEnabled = sortEnabled;
		this.restComplexityEnabled = restComplexityEnabled;
		this.identifyUndoEnabled = identifyUndoEnabled;
	}

	public boolean isSortEnabled() {
		return sortEnabled;
	}
	
	public void setSortEnabled(boolean sortEnabled) {
		this.sortEnabled = sortEnabled;
	}

	public boolean isRestComplexityEnabled() {
		return restComplexityEnabled;
	}

	public void setRestComplexityEnabled(boolean restComplexityEnabled) {
		this.restComplexityEnabled = restComplexityEnabled;
	}

	public boolean isIdentifyUndoEnabled() {
		return identifyUndoEnabled;
	}

	public void setIdentifyUndoEnabled(boolean identifyUndoEnabled) {
		this.identifyUndoEnabled = identifyUndoEnabled;
	}
}
