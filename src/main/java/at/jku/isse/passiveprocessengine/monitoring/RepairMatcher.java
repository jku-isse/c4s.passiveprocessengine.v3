package at.jku.isse.passiveprocessengine.monitoring;

import at.jku.isse.designspace.core.events.PropertyUpdate;
import at.jku.isse.designspace.core.events.PropertyUpdateAdd;
import at.jku.isse.designspace.core.events.PropertyUpdateRemove;
import at.jku.isse.designspace.core.events.PropertyUpdateSet;
import at.jku.isse.designspace.core.model.Id;
import at.jku.isse.designspace.core.model.Instance;
import at.jku.isse.designspace.rule.arl.repair.RepairAction;
import at.jku.isse.designspace.rule.arl.repair.UnknownRepairValue;

public class RepairMatcher {

	@SuppressWarnings("rawtypes")
	public static boolean doesOpMatchRepair(RepairAction ra, PropertyUpdate op, Id subject) {
		String propRep = ra.getProperty();
		String propChange = op.name();
		Instance rInst = (Instance) ra.getElement();
		if ((propRep == null) || !propRep.equals(propChange) || !subject.equals(rInst.id())) // if this repair is not about the same subject
			return false;
		switch (ra.getOperator()) {
		case ADD:
			if (op instanceof PropertyUpdateAdd) {
				Object rValue = ra.getValue();
				if (rValue == UnknownRepairValue.UNKNOWN) { // if null, then any value to ADD is fine
					return true;
				} else { // we have a concrete repair that we need to compare values for
					Object opValue = op.value(); // should be an id for instances to be added, otherwise the face
													// value of the property
					if (opValue instanceof Id && rValue instanceof Instance) {
						return opValue.equals(((Instance) rValue).id());
					} else {
						return opValue.equals(rValue);
					}
				}
			}
			break;
		case REMOVE:
			if (op instanceof PropertyUpdateRemove) {
				Object rValue = ra.getValue();
				if (rValue == UnknownRepairValue.UNKNOWN) { // if null, then any value to REMOVE is fine
					return true;
				} else { // we have a concrete repair that we need to compare values for
					Object opValue = op.value(); // should be an id for instances to be removed, otherwise the face value
													// of the property
					if (opValue instanceof Id && rValue instanceof Instance) {
						return opValue.equals(((Instance) rValue).id());
					} else {
						return opValue.equals(rValue);
					}
				}
			}
			break;
		case MOD_EQ:
			if (op instanceof PropertyUpdateSet) {
				Object rValue = ra.getValue();
				Object opValue = op.value(); // for Set operation/repair, both need to be present but could be NULL if
												// that is the intended/desired value or the effect of the operation
				// FIXME HACK //if (rValue == null && opValue == null)
				// return true;
				if (opValue instanceof Id && rValue instanceof Instance) {
					return opValue.equals(((Instance) rValue).id());
				}
				else if (rValue == UnknownRepairValue.UNKNOWN) // if repair suggest to set anything, any value set is fine TODO: (ignoring restrictions for now)
					return true;
				else if (opValue != null)
					return opValue.equals(rValue);
				else {
					return rValue==null;
				}
			}
			break;
		case MOD_GT:
			if (op instanceof PropertyUpdateSet) {
				Object rValue = ra.getValue();
				Object opValue = op.value(); // for Set operation/repair, both need to be present but could be NULL if
												// that is the intended/desired value or the effect of the operation
				if (opValue == null)
					return false;
				if (opValue instanceof Id) {
					return false;
				} else {
					// TODO implement
					return false;
				}
			}
			break;
		case MOD_LT:
			if (op instanceof PropertyUpdateSet) {
				Object rValue = ra.getValue();
				Object opValue = op.value(); // for Set operation/repair, both need to be present but could be NULL if
												// that is the intended/desired value or the effect of the operation
				if (opValue == null)
					return false;
				if (opValue instanceof Id) {
					return false;
				} else {
					// TODO implement
					return false;
				}
			}
			break;
		case MOD_NEQ:
			if (op instanceof PropertyUpdateSet) {
				Object rValue = ra.getValue();
				Object opValue = op.value(); // for Set operation/repair, both need to be present but could be NULL if
												// that is the intended/desired value or the effect of the operation
				if (rValue == null)
					return (opValue != null);
				if (opValue == null)
					return (rValue != null);
				if (opValue instanceof Id && rValue instanceof Instance) {
					return !opValue.equals(((Instance) rValue).id());
				} else {
					return !opValue.equals(rValue);
				}
			}
			break;
		default:
			break;
		}
		return false;
	}
	
	

	public static boolean doesOpMatchInvertedRepair(RepairAction ra, PropertyUpdate op, Id subject) {
		String propRep = ra.getProperty();
		String propChange = op.name();
		Instance rInst = (Instance) ra.getElement(); // repair subject
		if ((propRep == null) || !propRep.equals(propChange) || !subject.equals(rInst.id())) // if this repair is not about the same subject
			return false;
		switch (ra.getOperator()) {
		case ADD:
			if (op instanceof PropertyUpdateRemove) {
				Object rValue = ra.getValue();
				// if this repair is suggesting to add ANY then true as this removal operation
				// matches,
				if (rValue == UnknownRepairValue.UNKNOWN) {
					return true;
				} else { // we have a concrete repair that we need to compare values for
					Object opValue = op.value(); // should be an id for instances to be added, otherwise the face value
													// of the property
					if (opValue instanceof Id && rValue instanceof Instance) {
						return opValue.equals(((Instance) rValue).id());
					} else {
						return opValue.equals(rValue);
					}
				}
			}
			break;
		case REMOVE:
			if (op instanceof PropertyUpdateAdd) { // if repair is suggesting to remove
				Object rValue = ra.getValue();
				// if this repair is suggesting to remove any ANY then true,
				if (rValue == UnknownRepairValue.UNKNOWN) {
					return true;
				} else { // we have a concrete repair that we need to compare values for
					Object opValue = op.value(); // should be an id for instances to be added, otherwise the face value
													// of the property
					if (opValue instanceof Id && rValue instanceof Instance) {
						return opValue.equals(((Instance) rValue).id());
					} else {
						return opValue.equals(rValue);
					}
				}
			}
			break;
		case MOD_EQ:
			if (op instanceof PropertyUpdateSet) {
				Object rValue = ra.getValue();
				Object opValue = op.value(); // for Set operation/repair, both need to be present but could be NULL if
												// that is the intended/desired value or the effect of the operation
				if (opValue == null && rValue == null)
					return false;
				if (opValue == null && rValue != null)
					return true;
				if (rValue == null && opValue != null)
					return true;
				if (opValue instanceof Id && rValue instanceof Instance) {
					return !opValue.equals(((Instance) rValue).id());
				} else {
					return !opValue.equals(rValue);
				}
			}
			break;
		case MOD_GT:
			if (op instanceof PropertyUpdateSet) {
				Object rValue = ra.getValue();
				Object opValue = op.value(); // for Set operation/repair, both need to be present but could be NULL if
												// that is the intended/desired value or the effect of the operation
				if (opValue == null)
					return false;
				if (opValue instanceof Id) {
					return false;
				} else {
					// TODO implement
					return false;
				}
			}
			break;
		case MOD_LT:
			if (op instanceof PropertyUpdateSet) {
				Object rValue = ra.getValue();
				Object opValue = op.value(); // for Set operation/repair, both need to be present but could be NULL if
												// that is the intended/desired value or the effect of the operation
				if (opValue == null)
					return false;
				if (opValue instanceof Id) {
					return false;
				} else {
					// TODO implement
					return false;
				}
			}
			break;
		case MOD_NEQ:
			if (op instanceof PropertyUpdateSet) {
				Object rValue = ra.getValue();
				Object opValue = op.value(); // for Set operation/repair, both need to be present but could be NULL if
												// that is the intended/desired value or the effect of the operation
				if (rValue == null && opValue == null)
					return true;
				if (opValue == null && rValue != null)
					return false;
				if (rValue == null && opValue != null)
					return false;
				if (opValue instanceof Id && rValue instanceof Instance) {
					return opValue.equals(((Instance) rValue).id());
				} else {
					return opValue.equals(rValue);
				}
			}
			break;
		default:
			break;
		}
		return false;
	}
}
