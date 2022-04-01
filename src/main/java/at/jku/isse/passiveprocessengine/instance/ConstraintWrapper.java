package at.jku.isse.passiveprocessengine.instance;

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

import at.jku.isse.designspace.core.model.Instance;
import at.jku.isse.designspace.core.model.InstanceType;
import at.jku.isse.designspace.core.model.Workspace;
import at.jku.isse.designspace.rule.model.ConsistencyRule;
import at.jku.isse.passiveprocessengine.ProcessInstanceScopedElement;
import at.jku.isse.passiveprocessengine.definition.QAConstraintSpec;
import lombok.Data;

@Data
public class ConstraintWrapper extends ProcessInstanceScopedElement {

	
	public static final String designspaceTypeId = ConstraintWrapper.class.getSimpleName();
	// TODO make these instance properties
	ConsistencyRule cr;
	Boolean evalResult;
	ZonedDateTime lastChanged;
	QAConstraintSpec qaSpec;

	public ConstraintWrapper(Instance instance) {
		super(instance);
		
	}
	
	public void deleteCascading() {
		this.getInstance().delete();
	}
	
	public static InstanceType getOrCreateDesignSpaceInstanceType(Workspace ws){
		Optional<InstanceType> thisType = ws.debugInstanceTypes().stream()
				.filter(it -> it.name().equals(designspaceTypeId))
				.findAny();
			if (thisType.isPresent())
				return thisType.get();
			else {
				InstanceType typeStep = ws.createInstanceType(designspaceTypeId, ws.TYPES_FOLDER, ProcessInstanceScopedElement.getOrCreateDesignSpaceCoreSchema(ws));
				return typeStep;
			}
	}
	
	public static ConstraintWrapper getInstance(Workspace ws, QAConstraintSpec qaSpec, ConsistencyRule cr, Boolean evalResult, ZonedDateTime lastChanged, ProcessInstance proc) {
		Instance inst = ws.createInstance(getOrCreateDesignSpaceInstanceType(ws), qaSpec.getId()+proc.getName()+"_"+UUID.randomUUID());
		ConstraintWrapper cw = new ConstraintWrapper(inst);
		cw.cr = cr;
		cw.qaSpec = qaSpec;
		cw.evalResult = evalResult;
		cw.lastChanged = lastChanged;
		cw.setProcess(proc);
		return cw;
	}
	
}
