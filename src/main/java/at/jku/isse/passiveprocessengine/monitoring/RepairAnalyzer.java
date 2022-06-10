package at.jku.isse.passiveprocessengine.monitoring;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import at.jku.isse.designspace.core.events.Operation;
import at.jku.isse.designspace.core.events.PropertyUpdate;
import at.jku.isse.designspace.core.events.PropertyUpdateAdd;
import at.jku.isse.designspace.core.events.PropertyUpdateRemove;
import at.jku.isse.designspace.core.events.PropertyUpdateSet;
import at.jku.isse.designspace.core.model.Element;
import at.jku.isse.designspace.core.model.Id;
import at.jku.isse.designspace.core.model.Instance;
import at.jku.isse.designspace.core.model.InstanceType;
import at.jku.isse.designspace.core.model.Workspace;
import at.jku.isse.designspace.core.model.WorkspaceListener;
import at.jku.isse.designspace.rule.arl.repair.RepairAction;
import at.jku.isse.designspace.rule.arl.repair.RepairNode;
import at.jku.isse.designspace.rule.arl.repair.RepairTreeFilter;
import at.jku.isse.designspace.rule.arl.repair.SideEffect;
import at.jku.isse.designspace.rule.arl.repair.SideEffect.Type;
import at.jku.isse.designspace.rule.model.ConsistencyRule;
import at.jku.isse.designspace.rule.model.ConsistencyRuleType;
import at.jku.isse.designspace.rule.model.ReservedNames;
import at.jku.isse.designspace.rule.service.RuleService;
import at.jku.isse.passiveprocessengine.instance.ProcessInstance;
import at.jku.isse.passiveprocessengine.instance.ProcessStep;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RepairAnalyzer implements WorkspaceListener {

	Workspace ws;
	Map<ConsistencyRule, Boolean> changedRuleResult = new HashMap<>();
	Map<PropertyUpdate, Set<SideEffect<ConsistencyRule>>> latestImpact = new LinkedHashMap<>();
	Map<PropertyUpdate, Set<SideEffect<ConsistencyRule>>> collectedImpact = new LinkedHashMap<>();
	Map<ConsistencyRule, RepairNode> repairForRule = new HashMap<>();
	List<PropertyUpdate> queuedUpdates = Collections.synchronizedList(new LinkedList<PropertyUpdate>());
	Map<ConsistencyRuleType, List<Integer>> repairSizeStats = new HashMap<>();

	public RepairAnalyzer(Workspace ws) {
		this.ws = ws;
		ws.workspaceListeners.add(this);
	}
	
	// whenever a rule was reevaluated, we need to update the repair tree
	// rule consistent before => now inconsistent: generate/store repair tree - operation is negativ with respect to this rule
	// consistent --> consistent : no updated of repair tree - operation is neutral with respect to this rule
	// inconsistent -> consistent: check which repairs in the tree where applied, mark tree as complete, operation is positive with respect to this rule
	// inconsistent -> inconsistent: if repair tree the same - operation is neutral, if repair tree larger - operation is negativ (unless operation matches part of the repair, then other operation caused further inconsistencies)

	
	public Map<PropertyUpdate, Set<SideEffect<ConsistencyRule>>> getImpact() {
		return collectedImpact;
	}

	@Override
	public void handleUpdated(List<Operation> operations) {
		determineChangedRuleEvaluations(operations);
		// if changedRuleResult is empty, then there has been no rule changes and this notification was about other operations, queue those that are relevant
		// if change results is not empty, then this was a notification after rules have fired and we wont see any other artifact/instance changes
		// so in any case lets add to queuedUpdates:
		queuedUpdates.addAll(
				operations.stream()
				.filter(PropertyUpdate.class::isInstance)
				.map(PropertyUpdate.class::cast)
				.filter(op -> isRelevant(op))
				.collect(Collectors.toList())
			);
		
		// but only process further if rules have changed:
		if (changedRuleResult.size() > 0) {
			queuedUpdates.stream()
				.forEach(op -> determinePreliminaryImpact(op));

			determineConflictingSideEffects();
			determineConflictCausingNonRepairableOperations();
			determineNotsuggestedRepairOperations();
			
		
			// prep for next round: for all unfulfilled constraints that where potentially affected now by changes, we store the current repair tree
			changedRuleResult.keySet().stream().filter(cre -> !cre.isConsistent()).forEach(cre -> { 
				RepairNode rn = RuleService.repairTree(cre);
				rtf.filterRepairTree(rn);
				repairForRule.put(cre, rn); 
				// we also calculate stats on how many repairs for this type of rule we suggested
				ConsistencyRuleType type = (ConsistencyRuleType) cre.getInstanceType();
				repairSizeStats.compute(type, (k,v) -> v == null ? new LinkedList<>() : v).add(rn.getRepairActions().size());
				});

			// cleanup:
			queuedUpdates.clear();
			changedRuleResult.clear();
			// we also need to clear the impact
			collectedImpact.putAll(latestImpact);
			latestImpact.clear();
		}
	}	



	private void determineChangedRuleEvaluations(List<Operation> operations) {
		// has the status/result of this rule changed
		operations.stream()
		.filter(PropertyUpdateSet.class::isInstance)
		.map(PropertyUpdateSet.class::cast)
		.filter(op -> op.name().equals("result"))
		.forEach(op -> {
			Id id = op.elementId();
			Element el = ws.findElement(id);
			String name = el.getInstanceType().name();
			if (el instanceof ConsistencyRule 
					&& el.getInstanceType().name().startsWith("crd")
					&& !el.getInstanceType().name().startsWith("crd_datamapping")) {
				ConsistencyRule cre = (ConsistencyRule)el; 
				changedRuleResult.put(cre, true);
				// now how to deal with repairs:
				if (!cre.isConsistent()) {
					// if the rule was so far fulfilled, then some prior change violated it, and we need to determine which change that was by looking at the repair tree
					// hence lets obtain the repair tree. But filter it down to relevant repairs
					RepairNode rn = RuleService.repairTree(cre);
					rtf.filterRepairTree(rn);
					repairForRule.put(cre, rn);
				} else {
					// now the rule is fulfilled, and we want to check which action might have contributed to the fulfillement, hence we keep the old repair tree, we should have one stored
				}
				
				//
			}
		});
	}
	
	private boolean isRelevant(PropertyUpdate op) {
		// for each operation, we need to filter out any irrelevant properties
		// e.g., changes on constraint rules, step status, step input and step output (as these are set indirectly via other datatransfer and datamapping rules)
		if (op.name().contains("/@")) return false; // not interested in meta properties
		Id id = op.elementId();
		Element el = ws.findElement(id);
		if (el instanceof ConsistencyRule) return false; // not interested in those here

		InstanceType type = el.getInstanceType();
		if (type == null) return false; // we are only interested in our types
		if (type.name().startsWith(ProcessStep.designspaceTypeId)
				|| type.name().startsWith(ProcessInstance.designspaceTypeId))
			return false; //not interested in changes to the process or step itself
		return true;
	}

	@SuppressWarnings("unchecked")
	private void determinePreliminaryImpact(PropertyUpdate op) {
		// determine if this property caused reeval of a rule, if so check the rule result change
		Id id = op.elementId();
		Element el = ws.findElement(id);
		String ruleScopeProperty = op.name()+"/"+ReservedNames.RULE_EVALUATIONS_IN_SCOPE;
		if (!el.hasProperty(ruleScopeProperty) || el.getPropertyAsSet(ruleScopeProperty).isEmpty())
			return;
		else {
			el.getPropertyAsSet(ruleScopeProperty).get().stream()
			.filter(ConsistencyRule.class::isInstance) // all should be ConsistencyRules
			.map(inst -> (ConsistencyRule)inst)
			.filter(cr -> ((ConsistencyRule)cr).getInstanceType().name().startsWith("crd") && !((ConsistencyRule)cr).getInstanceType().name().startsWith("crd_datamapping"))
			.forEach(cr -> { 
				ConsistencyRule cre = (ConsistencyRule)cr;
				changedRuleResult.putIfAbsent(cre, false);
				// now lets create the impact/sideeffect
				latestImpact.compute(op, (k,v) -> v == null ? new HashSet<>() : v).add(new SideEffect<ConsistencyRule>(cre, determineType(op, cre)));
			});
		}
	}

	private SideEffect.Type determineType(PropertyUpdate op, ConsistencyRule cr) {
		Id id = op.elementId();
		// if cr has changed 
		if (changedRuleResult.get(cr) == true) {
			// and now is fulfilled
			if (cr.isConsistent()) {
				RepairNode rNodeOld = repairForRule.get(cr); // only be null if this is the first eval and was never negative in previous rounds
				// was this change truly causing (part of ) the repair, see if it occurs in the repair tree TODO: (and no other action covered it so far). if so --> positive
				if (rNodeOld == null || rNodeOld.getRepairActions().stream().anyMatch(ra -> doesOpMatchRepair(ra, op, id))) 
					return Type.POSITIVE;
				else
					return Type.NONE; 
			}	else { // no longer consistent
				RepairNode rNodeNow = RuleService.repairTree(cr);
				rtf.filterRepairTree(rNodeNow); // we need to filter out irrelevant repairs
				//  was this change truly causing (or part of) the inconsistency, see if its inverse action occurs in the repair tree. if so --> negative
				if (rNodeNow.getRepairActions().stream().anyMatch(ra -> doesOpMatchInvertedRepair(ra, op, id))) 
					return Type.NEGATIVE;
				else
					return Type.NONE; 
			}
		} else { 
			if (cr.isConsistent()) // all is still fine, no further analysis needed
				return Type.NONE;
			else {
				// rule hasn;t changed, TODO thats not to say, that change might now have introduced another inconsistency
				// but we would need to determine this first
				RepairNode rNodeOld = repairForRule.get(cr); // must not be null if its still negative
				assert(rNodeOld != null);
				// this rNodeOld is the prior one, not for the current rule state
				RepairNode rNodeNow = RuleService.repairTree(cr);
				rtf.filterRepairTree(rNodeNow); // we need to filter out irrelevant repairs
				
				// look whether old repair nodes included this operation, if so then positive
				if (rNodeOld.getRepairActions().stream().anyMatch(ra -> doesOpMatchRepair(ra, op, id))) 
					return Type.POSITIVE;
				// look whether new repair nodes include this inverse operation, if so then negative
				if (rNodeNow.getRepairActions().stream().anyMatch(ra -> doesOpMatchInvertedRepair(ra, op, id))) 
					return Type.NEGATIVE;
				//else
				return Type.NONE;
			}
		}	
	}

	@SuppressWarnings("rawtypes")
	private boolean doesOpMatchRepair(RepairAction ra, PropertyUpdate op, Id subject) {
		String propRep = ra.getProperty();
		String propChange = op.name();
		Instance rInst = (Instance)ra.getElement();
		if (propRep == null) // the removal repair which in our case is never a sensible option
			return false;
		if (!propRep.equals(propChange)) // if this repair is not about the same property
			return false;
		if (!subject.equals(rInst.id())) // if this repair is not about the same subject
			return false;
		switch(ra.getOperator()) {
		case ADD:
			if (op instanceof PropertyUpdateAdd) { 
				Object rValue = ra.getValue();
				if (rValue == null) { // if null, then any value to ADD is ok
					return true;
				} else { // we have a concrete repair that we need to compare values for
					Object opValue = op.value(); // should be an id for instances to be removed, otherwise the face value of the property
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
				if (rValue == null) { // if null, then any value to REMOVE is ok
					return true;
				} else { // we have a concrete repair that we need to compare values for
					Object opValue = op.value(); // should be an id for instances to be added, otherwise the face value of the property
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
				Object opValue = op.value(); // for Set operation/repair, both need to be present but could be NULL if that is the intended/desired value or the effect of the operation
				if (rValue == null && opValue == null)
					return true;
				if (opValue instanceof Id && rValue instanceof Instance) {
					return opValue.equals(((Instance) rValue).id());
				} else {
					return opValue.equals(rValue);
				}
			}
			break;
		case MOD_GT:
			if (op instanceof PropertyUpdateSet) {
				Object rValue = ra.getValue();
				Object opValue = op.value(); // for Set operation/repair, both need to be present but could be NULL if that is the intended/desired value or the effect of the operation
				if (opValue == null)
					return false;
				if (opValue instanceof Id) {
					return false;
				} else {
					//TODO implement
					return false;
				}
			}
			break;
		case MOD_LT:
			if (op instanceof PropertyUpdateSet) {
				Object rValue = ra.getValue();
				Object opValue = op.value(); // for Set operation/repair, both need to be present but could be NULL if that is the intended/desired value or the effect of the operation
				if (opValue == null)
					return false;
				if (opValue instanceof Id) {
					return false;
				} else {
					//TODO implement
					return false;
				}
			}
			break;
		case MOD_NEQ:
			if (op instanceof PropertyUpdateSet) {
				Object rValue = ra.getValue();
				Object opValue = op.value(); // for Set operation/repair, both need to be present but could be NULL if that is the intended/desired value or the effect of the operation
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
	
	private boolean doesOpMatchInvertedRepair(RepairAction ra, PropertyUpdate op, Id subject) {
		String propRep = ra.getProperty();
		String propChange = op.name();
		Instance rInst = (Instance)ra.getElement(); // repair subject
		if (propRep == null) // the removal repair which in our case is never a sensible option
			return false;
		if (!propRep.equals(propChange)) // if this repair is not about the same property
			return false;
		if (!subject.equals(rInst.id())) // if this repair is not about the same subject
			return false;
		switch(ra.getOperator()) {
		case ADD:
			if (op instanceof PropertyUpdateRemove) { 
				Object rValue = ra.getValue();
				// if this repair is suggesting to add ANY then true as this removal operation matches, 
				if (rValue == null) { 
					return true;
				} else { // we have a concrete repair that we need to compare values for
					Object opValue = op.value(); // should be an id for instances to be added, otherwise the face value of the property
					if (opValue instanceof Id && rValue instanceof Instance) {
						return opValue.equals(((Instance) rValue).id());
					} else {
						return opValue.equals(rValue);
					}
				}
			}
			break;
		case REMOVE:
			if (op instanceof PropertyUpdateAdd && ra.getValue() == null) { // if repair is suggesting to remove
				Object rValue = ra.getValue();
				// if this repair is suggesting to remove any ANY then true, 
				if (rValue == null) { 
					return true;
				} else { // we have a concrete repair that we need to compare values for
					Object opValue = op.value(); // should be an id for instances to be added, otherwise the face value of the property
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
				Object opValue = op.value(); // for Set operation/repair, both need to be present but could be NULL if that is the intended/desired value or the effect of the operation
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
		case MOD_GT:
			if (op instanceof PropertyUpdateSet) {
				Object rValue = ra.getValue();
				Object opValue = op.value(); // for Set operation/repair, both need to be present but could be NULL if that is the intended/desired value or the effect of the operation
				if (opValue == null)
					return false;
				if (opValue instanceof Id) {
					return false;
				} else {
					//TODO implement
					return false;
				}
			}
			break;
		case MOD_LT:
			if (op instanceof PropertyUpdateSet) {
				Object rValue = ra.getValue();
				Object opValue = op.value(); // for Set operation/repair, both need to be present but could be NULL if that is the intended/desired value or the effect of the operation
				if (opValue == null)
					return false;
				if (opValue instanceof Id) {
					return false;
				} else {
					//TODO implement
					return false;
				}
			}
			break;
		case MOD_NEQ:
			if (op instanceof PropertyUpdateSet) {
				Object rValue = ra.getValue();
				Object opValue = op.value(); // for Set operation/repair, both need to be present but could be NULL if that is the intended/desired value or the effect of the operation
				if (rValue == null && opValue == null)
					return true;
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
	
	
	private void determineConflictCausingNonRepairableOperations() {
		// check for every consistency rule that is now newly inconsistent whether there is at least one NEGATIVE impact, 
		changedRuleResult.entrySet().stream()
			.filter(entry -> entry.getValue() == true)
			.filter(entry -> !entry.getKey().isConsistent())
			.map(entry -> entry.getKey())
			.forEach(cre -> {
					boolean hasNegImpact = latestImpact.values().stream()
									.flatMap(set -> set.stream())
									.filter(eff -> eff.getInconsistency().equals(cre))
									.anyMatch(eff -> eff.getSideEffectType().equals(Type.NEGATIVE));
					if (!hasNegImpact)
						log.warn("Inconsistent rule has no repair for any of the action(s) that where part of the root cause: "+cre);
				}
			);
	}
	
	private void determineNotsuggestedRepairOperations() {
		// check for every consistency rule that is now newly consistent whether there is at least on POSITIVE impact
		changedRuleResult.entrySet().stream()
		.filter(entry -> entry.getValue() == true)
		.filter(entry -> entry.getKey().isConsistent())
		.map(entry -> entry.getKey())
		.forEach(cre -> {
				boolean hasPosImpact = latestImpact.values().stream()
								.flatMap(set -> set.stream())
								.filter(eff -> eff.getInconsistency().equals(cre))
								.anyMatch(eff -> eff.getSideEffectType().equals(Type.POSITIVE));
				if (!hasPosImpact)
					log.warn("Consistent rule had no repair suggested for any of the action(s) that repaired the rule: "+cre);
			}
		);
	}
	
	private void determineConflictingSideEffects() {
		// for each CRE instance, check if there are positive and negative effects
		// for each operation, check if it contains conflicting effects
		latestImpact.entrySet().stream()
		.forEach(entry -> {
			Map<Type, List<SideEffect<ConsistencyRule>>> effects = entry.getValue().stream()
					.collect(Collectors.groupingBy(SideEffect<ConsistencyRule>::getSideEffectType));
			if (effects.getOrDefault(Type.NEGATIVE, Collections.emptyList()).size() > 0 && effects.getOrDefault(Type.POSITIVE, Collections.emptyList()).size() > 0 ) {
				// we have conflicting effects
				log.debug(entry.getKey() + " has following conflicting effects \r\n" +
				" POS: "+effects.get(Type.POSITIVE).stream().map(se -> se.getInconsistency().name()).collect(Collectors.joining(", ", "[", "]")) + "\r\n" +
				" NEG: "+effects.get(Type.NEGATIVE).stream().map(se -> se.getInconsistency().name()).collect(Collectors.joining(", ", "[", "]")) );
			}
		});
	}

	public void printImpact() {
		collectedImpact.entrySet().stream()
		.forEach(entry -> {
			Map<Type, List<SideEffect<ConsistencyRule>>> effects = entry.getValue().stream()
					.collect(Collectors.groupingBy(SideEffect<ConsistencyRule>::getSideEffectType));
			log.debug(entry.getKey() + " has following effects:");
			effects.entrySet().stream().forEach(entry2 -> 
				log.debug(entry2.getKey() +" on "+entry2.getValue().stream().map(se -> se.getInconsistency().name()).collect(Collectors.joining(", ", "[", "]"))));
		});
	}
	
	private static RepairTreeFilter rtf = new QARepairTreeFilter();
	
	private static class QARepairTreeFilter extends RepairTreeFilter {

		@Override
		public boolean compliesTo(RepairAction ra) {
			return ra.getProperty() != null 
					&& !ra.getProperty().startsWith("out_") // no change to input or output
					&& !ra.getProperty().startsWith("in_")
					&& !ra.getProperty().equalsIgnoreCase("name"); // typically used to describe key or id outside of designspace
		
		}
		
	}
	
	public void printRepairSizeStats() {
		repairSizeStats.entrySet().stream().forEach(entry -> {
			// lets print size first, for quicker viewing
			log.debug(entry.getValue().stream().map(count -> count.toString()).collect(Collectors.joining(", ", "[", "]"))+" : "+entry.getKey());
		});
		
		
		collectedImpact.entrySet().stream()
		.forEach(entry -> {
			Map<Type, List<SideEffect<ConsistencyRule>>> effects = entry.getValue().stream()
					.collect(Collectors.groupingBy(SideEffect<ConsistencyRule>::getSideEffectType));
			log.debug(entry.getKey() + " has following effects:");
			effects.entrySet().stream().forEach(entry2 -> 
				log.debug(entry2.getKey() +" on "+entry2.getValue().stream().map(se -> se.getInconsistency().name()).collect(Collectors.joining(", ", "[", "]"))));
		});
	}
	
}
