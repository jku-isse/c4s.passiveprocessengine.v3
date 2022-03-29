package at.jku.isse.passiveprocessengine.instance;

import java.time.ZonedDateTime;

import at.jku.isse.designspace.core.model.Instance;
import at.jku.isse.designspace.rule.model.ConsistencyRule;
import at.jku.isse.passiveprocessengine.ProcessInstanceScopedElement;
import at.jku.isse.passiveprocessengine.definition.QAConstraintSpec;
import lombok.Data;

@Data
public class ConstraintWrapper extends ProcessInstanceScopedElement {

	ConsistencyRule cr;
	Boolean evalResult;
	ZonedDateTime lastChanged;
	QAConstraintSpec qaSpec;

	public ConstraintWrapper(Instance instance, QAConstraintSpec qaSpec, ConsistencyRule cr, Boolean evalResult, ZonedDateTime lastChanged, ProcessInstance proc) {
		super(instance);
		this.cr = cr;
		this.qaSpec = qaSpec;
		this.evalResult = evalResult;
		this.lastChanged = lastChanged;
		this.setProcess(proc);
	}
	
	public void deleteCascading() {
		this.getInstance().delete();
	}
	
}
