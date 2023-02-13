package at.jku.isse.passiveprocessengine.monitoring;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import at.jku.isse.designspace.core.events.Operation;
import at.jku.isse.designspace.core.events.PropertyUpdate;
import at.jku.isse.designspace.core.events.PropertyUpdateAdd;
import at.jku.isse.designspace.core.events.PropertyUpdateRemove;
import at.jku.isse.designspace.core.events.PropertyUpdateSet;
import at.jku.isse.designspace.core.model.Element;
import at.jku.isse.designspace.core.model.Id;
import at.jku.isse.designspace.core.model.Instance;
import at.jku.isse.designspace.core.model.InstanceType;
import at.jku.isse.designspace.core.model.Property;
import at.jku.isse.designspace.core.model.SetProperty;
import at.jku.isse.designspace.core.model.Workspace;
import at.jku.isse.designspace.core.model.WorkspaceListener;
import at.jku.isse.designspace.rule.arl.evaluator.EvaluationNode;
import at.jku.isse.designspace.rule.arl.exception.RepairException;
import at.jku.isse.designspace.rule.arl.repair.RepairAction;
import at.jku.isse.designspace.rule.arl.repair.RepairNode;
import at.jku.isse.designspace.rule.arl.repair.RepairTreeFilter;
import at.jku.isse.designspace.rule.arl.repair.SideEffect;
import at.jku.isse.designspace.rule.arl.repair.UnknownRepairValue;
import at.jku.isse.designspace.rule.arl.repair.SideEffect.Type;
import at.jku.isse.designspace.rule.model.ConsistencyRule;
import at.jku.isse.designspace.rule.model.ConsistencyRuleType;
import at.jku.isse.designspace.rule.model.ReservedNames;
import at.jku.isse.designspace.rule.model.Rule;
import at.jku.isse.designspace.rule.repair.Evaluation.RepairAction_LogDS;
import at.jku.isse.designspace.rule.repair.Evaluation.RepairAction_LogStats;
import at.jku.isse.designspace.rule.repair.order.CRE_DS;
import at.jku.isse.designspace.rule.repair.order.NodeCounter;
import at.jku.isse.designspace.rule.repair.order.OperationStats;
import at.jku.isse.designspace.rule.repair.order.RepairNodeScorer;
import at.jku.isse.designspace.rule.repair.order.RepairStats;
import at.jku.isse.designspace.rule.repair.order.RepairTreeSorter;
import at.jku.isse.designspace.rule.repair.order.Repair_template;
import at.jku.isse.designspace.rule.service.RuleService;
import at.jku.isse.passiveprocessengine.instance.ProcessInstance;
import at.jku.isse.passiveprocessengine.instance.ProcessStep;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RepairAnalyzer implements WorkspaceListener {

	Workspace ws;
	// which rules have now a changed result (either now fulfilled, or now
	// unfulfilled)
	Map<ConsistencyRule, Boolean> changedRuleResult = new HashMap<>();
	// impact in this iteration (collected changes without rule changes)
	Map<PropertyUpdate, Set<SideEffect<ConsistencyRule>>> latestImpact = new LinkedHashMap<>();
	// impact since initialization
	Map<PropertyUpdate, Set<SideEffect<ConsistencyRule>>> collectedImpact = new LinkedHashMap<>();
	// last/latest repair for rule (updated at the end of previous iteration)
	Map<ConsistencyRule, RepairNode> repairForRule = new HashMap<>();
	// collecting all updates as long as no rule result changes,i.e., no rule was
	// reevaluated resulting in a change
	List<PropertyUpdate> queuedUpdates = Collections.synchronizedList(new LinkedList<PropertyUpdate>());
	// how many repair nodes are suggested for each rule, we collect across all rule
	// instances, per type
	Map<ConsistencyRuleType, List<Integer>> repairSizeStats = new HashMap<>();
	// which rules are part of a conflict, due to which operation
	Map<String, Conflict> conflicts = new HashMap<>();
	Map<ConsistencyRuleType, List<Set<PropertyUpdate>>> inconsistencyCausingNonRepairableOperations = new HashMap<>();
	Map<ConsistencyRuleType, List<Set<PropertyUpdate>>> notsuggestedRepairOperations = new HashMap<>();
	Map<String, String> unsupportedRepairs = new HashMap<>();

	// Added field
	NodeCounter node_counter;
	Map<ConsistencyRule, Set<RepairAction>> unselectedRepairstemp = new HashMap<>();
	RepairAction_LogStats selected_ra_Log = new RepairAction_LogStats();
	RepairAction_LogStats unselected_ra_Log = new RepairAction_LogStats();
	RepairNodeScorer scorer;
	ITimeStampProvider time;
	// end

	public RepairAnalyzer(Workspace ws, RepairStats rs, RepairNodeScorer scorer, ITimeStampProvider timeprovider) {

		this.ws = ws;
		this.node_counter = new NodeCounter(rs);
		this.scorer = scorer;
		this.time=timeprovider;
	}

	public void inject(Workspace ws2) {
		this.ws = ws2;
	}
	/*
	 * whenever a rule was reevaluated, we need to update the repair tree 1) rule
	 * consistent before => now inconsistent: generate/store repair tree -operation
	 * is negative with respect to this rule 2) consistent --> consistent : no
	 * updated of repair tree - operation is neutral with respect to this rule 3)
	 * inconsistent -> consistent: check which repairs in the tree where applied,
	 * mark tree as complete, operation is positive with respect to this rule 4)
	 * inconsistent -> inconsistent: if repair tree the same - operation is neutral,
	 * if repair tree larger - operation is negative (unless operation matches part
	 * of the repair, then other operation caused further inconsistencies)
	 */

	public Map<PropertyUpdate, Set<SideEffect<ConsistencyRule>>> getImpact() {
		return collectedImpact;
	}

	@Override
	public void handleUpdated(List<Operation> operations) {
		
		
		determineChangedRuleEvaluations(operations);
		// if changedRuleResult is empty, then there has been no rule changes and this
		// notification was about other operations, queue those that are relevant
		// if change results is not empty, then this was a notification after rules have
		// fired and we wont see any other artifact/instance changes
		// so in any case lets add to queuedUpdates:
		queuedUpdates.addAll(operations.stream().filter(PropertyUpdate.class::isInstance)
				.map(PropertyUpdate.class::cast).filter(op -> isRelevant(op)).collect(Collectors.toList()));
		// but only process further if rules have changed:
		if (changedRuleResult.size() > 0) {
			queuedUpdates.stream().forEach(op -> determinePreliminaryImpact(op));
			determineConflictingSideEffects();
			determineConflictCausingNonRepairableOperations(); // this and the following method measure the same effect,
																// upon inconsistency appearance, the other upon repair.
			determineNotsuggestedRepairOperations();
			this.calculateStats(latestImpact, repairForRule);
			// prepare for next round: for all unfulfilled constraints that were potentially
			// affected now by changes, we store the current repair tree
			changedRuleResult.keySet().stream().filter(cre -> !cre.isConsistent()).forEach(cre -> {
				try {
					RepairNode rn = RuleService.repairTree(cre);
					// Print the tree
					rtf.filterRepairTree(rn);
					repairForRule.put(cre, rn);
					Set<RepairAction> ras = rn.getRepairActions();
					// we also calculate statistics on how many repairs for this type of rule we
					// suggested
					ConsistencyRuleType type = (ConsistencyRuleType) cre.getInstanceType();
					repairSizeStats.compute(type, (k, v) -> v == null ? new LinkedList<>() : v).add(ras.size());
				} catch (RepairException e) {
					unsupportedRepairs.put(cre.getInstanceType().name(), e.getMessage());
					e.printStackTrace();
				}
			});
			// cleanup:
			queuedUpdates.clear();
			changedRuleResult.clear();
			// we also need to clear the impact
			collectedImpact.putAll(latestImpact);
			// end call
			latestImpact.clear();
		}
	}

	public ITimeStampProvider getTime() {
		return time;
	}

	public void setTime(ReplayTimeProvider time) {
		this.time = time;
	}

	private void determineChangedRuleEvaluations(List<Operation> operations) {
		// has the status/result of this rule changed
		operations.stream().filter(PropertyUpdateSet.class::isInstance).map(PropertyUpdateSet.class::cast)
				.filter(op -> op.name().equals("result")).forEach(op -> {
					Id id = op.elementId();
					Element el = ws.findElement(id);
					String name = el.getInstanceType().name();
					if (el instanceof ConsistencyRule && el.getInstanceType().name().startsWith("crd")
							&& !el.getInstanceType().name().startsWith("crd_datamapping")
							&& !el.getInstanceType().name().startsWith("crd_prematuretrigger")) {
						ConsistencyRule cre = (ConsistencyRule) el;
						changedRuleResult.put(cre, true);
						// now how to deal with repairs:
						if (!cre.isConsistent()) {
							// if the rule was so far fulfilled, then some prior change violated it, and we
							// need to determine which change that was by looking at the repair tree
							// hence lets obtain the current repair tree. But filter it down to relevant
							// repairs
							try {
								RepairNode rn = RuleService.repairTree(cre);
								rtf.filterRepairTree(rn);
								repairForRule.put(cre, rn);
							} catch (RepairException e) {
								unsupportedRepairs.put(cre.getInstanceType().name(), e.getMessage());
								e.printStackTrace();
							}
						} else {
							// now the rule is fulfilled, and we want to check which action might have
							// contributed to the fulfillment, hence we keep the old repair tree, we should
							// have one stored
						}
					}
				});
	}

	private boolean isRelevant(PropertyUpdate op) {
		// for each operation, we need to filter out any irrelevant properties
		// e.g., changes on constraint rules, step status, step input and step output
		// (as these are set indirectly via other datatransfer and datamapping rules)
		if (op.name().contains("/@"))
			return false; // not interested in meta properties
		Id id = op.elementId();
		Element el = ws.findElement(id);
		if (el instanceof ConsistencyRule)
			return false; // not interested in those here

		InstanceType type = el.getInstanceType();
		if (type == null)
			return false; // we are only interested in our types
		if (type.name().startsWith(ProcessStep.designspaceTypeId)
				|| type.name().startsWith(ProcessInstance.designspaceTypeId))
			return false; // not interested in changes to the process or step itself
		return true;
	}

	@SuppressWarnings("unchecked")
	private void determinePreliminaryImpact(PropertyUpdate op) {
		// determine if this property caused re-evaluation of a rule, if so check the
		// rule result change
		Id id = op.elementId();
		Element el = ws.findElement(id);
		String ruleScopeProperty = op.name() + "/" + ReservedNames.RULE_EVALUATIONS_IN_SCOPE;
		if (!el.hasProperty(ruleScopeProperty) || el.getPropertyAsSet(ruleScopeProperty).isEmpty())
			return;
		else {
			el.getPropertyAsSet(ruleScopeProperty).get().stream().filter(ConsistencyRule.class::isInstance) // all
																											// should be
																											// ConsistencyRules
					.map(inst -> (ConsistencyRule) inst)
					.filter(cr -> ((ConsistencyRule) cr).getInstanceType().name().startsWith("crd")
							&& !((ConsistencyRule) cr).getInstanceType().name().startsWith("crd_datamapping")
							&& !((ConsistencyRule) cr).getInstanceType().name().startsWith("crd_prematuretrigger"))
					.forEach(cr -> {
						ConsistencyRule cre = (ConsistencyRule) cr;
						changedRuleResult.putIfAbsent(cre, false);
						// now lets create the impact/side-effect
						latestImpact.compute(op, (k, v) -> v == null ? new HashSet<>() : v)
								.add(new SideEffect<ConsistencyRule>(cre, determineType(op, cre)));
					});
		}
	}

	private SideEffect.Type determineType(PropertyUpdate op, ConsistencyRule cr) {
		Id id = op.elementId();
		// System.out.println("Rule Qualified Name:
		// "+cr.ruleDefinition().getQualifiedName());
		// System.out.println("Rule Definition: "+cr.ruleDefinition().toString());
		// if cr has changed
		if (changedRuleResult.get(cr) == true) {
			// and now is fulfilled
			if (cr.isConsistent()) {
				RepairNode rNodeOld = repairForRule.get(cr); // only be null if this is the first eval and was never
																// negative in previous rounds
				// was this change truly causing (part of ) the repair, see if it occurs in the
				// repair tree TODO: (and no other action covered it so far). if so --> positive
				if (rNodeOld == null
						|| rNodeOld.getRepairActions().stream().anyMatch(ra -> doesOpMatchRepair(ra, op, id)))
					return Type.POSITIVE;
				else
					return Type.NONE;
			} else { // no longer consistent
				try {
					// name and instance of the rule to be printed here

					RepairNode rNodeNow = RuleService.repairTree(cr);
					rtf.filterRepairTree(rNodeNow); // we need to filter out irrelevant repairs
					// was this change truly causing (or part of) the inconsistency, see if its
					// inverse action occurs in the repair tree. if so --> negative
					if (rNodeNow.getRepairActions().stream().anyMatch(ra -> doesOpMatchInvertedRepair(ra, op, id)))
						return Type.NEGATIVE;
					else
						return Type.NONE;
				} catch (RepairException e) {
					unsupportedRepairs.put(cr.getInstanceType().name(), e.getMessage());
					e.printStackTrace();
					return Type.ERROR; // misuse of ERROR, as this is not about the repair but the being able to repair
										// in the first place.
				}
			}
		} else {
			if (cr.isConsistent()) // all is still fine, no further analysis needed
				return Type.NONE;
			else {
				// rule result hasn't changed, thats not to say, that change might now have
				// introduced another inconsistency
				// but we would need to determine this first
				RepairNode rNodeOld = repairForRule.get(cr); // must not be null if its still negative
				// assert(rNodeOld != null); //FIXME: NOT USED AS WRONGLY DETECTED NULL
				// repairnode for some reason
				if (rNodeOld == null) {
					return Type.NONE; // FIXME: hack to avoid NPE for now
				}
				// this rNodeOld is the prior one, not for the current rule state
				// look whether old repair nodes included this operation, if so then positive
				if (rNodeOld.getRepairActions().stream().anyMatch(ra -> doesOpMatchRepair(ra, op, id)))
					return Type.POSITIVE;
				// else
				try {
					RepairNode rNodeNow = RuleService.repairTree(cr);
					rtf.filterRepairTree(rNodeNow); // we need to filter out irrelevant repairs
					// look whether new repair nodes include this inverse operation, if so then
					// negative
					if (rNodeNow.getRepairActions().stream().anyMatch(ra -> doesOpMatchInvertedRepair(ra, op, id)))
						return Type.NEGATIVE;
					// else
					return Type.NONE;
				} catch (RepairException e) {
					unsupportedRepairs.put(cr.getInstanceType().name(), e.getMessage());
					e.printStackTrace();
					return Type.ERROR; // misuse of ERROR, as this is not about the repair but the being able to repair
										// in the first place.
				}

			}
		}
	}

	@SuppressWarnings("rawtypes")
	private boolean doesOpMatchRepair(RepairAction ra, PropertyUpdate op, Id subject) {
		String propRep = ra.getProperty();
		String propChange = op.name();
		Instance rInst = (Instance) ra.getElement();
		if (propRep == null) // the removal repair which in our case is never a sensible option
			return false;
		if (!propRep.equals(propChange)) // if this repair is not about the same property
			return false;
		if (!subject.equals(rInst.id())) // if this repair is not about the same subject
			return false;
		switch (ra.getOperator()) {
		case ADD:
			if (op instanceof PropertyUpdateAdd) {
				Object rValue = ra.getValue();
				if (rValue == UnknownRepairValue.UNKNOWN) { // if null, then any value to ADD is fine
					return true;
				} else { // we have a concrete repair that we need to compare values for
					Object opValue = op.value(); // should be an id for instances to be removed, otherwise the face
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

	private boolean doesOpMatchInvertedRepair(RepairAction ra, PropertyUpdate op, Id subject) {
		String propRep = ra.getProperty();
		String propChange = op.name();
		Instance rInst = (Instance) ra.getElement(); // repair subject
		if (propRep == null) // the removal repair which in our case is never a sensible option
			return false;
		if (!propRep.equals(propChange)) // if this repair is not about the same property
			return false;
		if (!subject.equals(rInst.id())) // if this repair is not about the same subject
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

	private void determineConflictCausingNonRepairableOperations() {
		// check for every consistency rule that is now newly inconsistent whether there
		// is at least one NEGATIVE impact,
		changedRuleResult.entrySet().stream().filter(entry -> entry.getValue() == true)
				.filter(entry -> !entry.getKey().isConsistent())
				.map(entry -> entry.getKey())
				.forEach(cre -> {
					boolean hasNegImpact = latestImpact.values().stream().flatMap(set -> set.stream())
							.filter(eff -> eff.getInconsistency().equals(cre))
							.anyMatch(eff -> eff.getSideEffectType().equals(Type.NEGATIVE));
					if (!hasNegImpact) {
						// any impact must have been considered Type.NONE, we want to know which, thus
						// collect those operations
						Set<PropertyUpdate> causes = latestImpact.entrySet().stream()
								.filter(entry -> entry.getValue().stream().map(se -> se.getInconsistency().id())
										.anyMatch(incon -> incon.equals(cre.id())))
								.map(entry -> entry.getKey()).collect(Collectors.toSet());
						// if causes are empty, this might happen when step is created with input and QA
						// is unfulfilled, then its not the cause of any user, and this list is empty
						if (!causes.isEmpty()) {
							inconsistencyCausingNonRepairableOperations
									.compute((ConsistencyRuleType) cre.getInstanceType(),
											(k, v) -> v == null ? new LinkedList<>() : v)
									.add(causes);
							log.warn("Inconsistent rule has no repair for any of the action(s) ["
									+ causes.stream().map(cause -> cause.toString()).collect(Collectors.joining(","))
									+ "] that where part of the root inconsistency cause of: " + cre.name());
						}
					}
				});
	}

	private void determineNotsuggestedRepairOperations() {
		// check for every consistency rule that is now newly consistent whether there
		// is at least on POSITIVE impact
		changedRuleResult.entrySet().stream().filter(entry -> entry.getValue() == true)
				.filter(entry -> entry.getKey().isConsistent()).map(entry -> entry.getKey()).forEach(cre -> {
					boolean hasPosImpact = latestImpact.values().stream().flatMap(set -> set.stream())
							.filter(eff -> eff.getInconsistency().equals(cre))
							.anyMatch(eff -> eff.getSideEffectType().equals(Type.POSITIVE));
					if (!hasPosImpact) {
						// any impact must therefore have Type.NONE, we want to know which, thus collect
						// those operations
						Set<PropertyUpdate> causes = latestImpact.entrySet().stream()
								.filter(entry -> entry.getValue().stream().map(se -> se.getInconsistency())
										.anyMatch(incon -> incon.equals(cre)))
								.map(entry -> entry.getKey()).collect(Collectors.toSet());
						// if causes are empty, this might happen when step is created with input and QA
						// is unfulfilled, then its not the cause of any user, and this list is empty
						if (!causes.isEmpty()) {
							notsuggestedRepairOperations.compute((ConsistencyRuleType) cre.getInstanceType(),
									(k, v) -> v == null ? new LinkedList<>() : v).add(causes);
							log.warn("Consistent rule had no repair suggested for any of the action(s) ["
									+ causes.stream().map(cause -> cause.toString()).collect(Collectors.joining(","))
									+ "] that repaired the rule: " + cre.name());
						}
					}
				});
	}

	private void determineConflictingSideEffects() {
		// for each CRE instance, check if there are positive and negative effects
		// for each operation, check if it contains conflicting effects
		latestImpact.entrySet().stream().forEach(entry -> {
			Map<Type, List<SideEffect<ConsistencyRule>>> effects = entry.getValue().stream()
					.collect(Collectors.groupingBy(SideEffect<ConsistencyRule>::getSideEffectType));
			if (effects.getOrDefault(Type.NEGATIVE, Collections.emptyList()).size() > 0
					&& effects.getOrDefault(Type.POSITIVE, Collections.emptyList()).size() > 0) {
				// we have conflicting effects
				log.debug(entry.getKey() + " has following conflicting effects " + " POS: "
						+ effects.get(Type.POSITIVE).stream().map(se -> se.getInconsistency().name())
								.collect(Collectors.joining(", ", "[", "]"))
						+ " NEG: " + effects.get(Type.NEGATIVE).stream().map(se -> se.getInconsistency().name())
								.collect(Collectors.joining(", ", "[", "]")));
				effects.get(Type.POSITIVE).stream()
						.map(se -> (ConsistencyRuleType) se.getInconsistency().getInstanceType())
						.forEach(crt -> effects.get(Type.NEGATIVE).stream()
								.map(seNeg -> (ConsistencyRuleType) seNeg.getInconsistency().getInstanceType())
								.forEach(crtNeg -> {
									conflicts
											.compute(crt.name() + crtNeg.name(),
													(k, v) -> v == null ? new Conflict(crt, crtNeg) : v)
											.add(entry.getKey());
								})); // k, (k,v) -> v == null ? new LinkedList<>() : v
			}
		});
	}

	public void printImpact() {
		collectedImpact.entrySet().stream().forEach(entry -> {
			Map<Type, List<SideEffect<ConsistencyRule>>> effects = entry.getValue().stream()
					.collect(Collectors.groupingBy(SideEffect<ConsistencyRule>::getSideEffectType));
			log.debug(entry.getKey() + " has following effects:");
			effects.entrySet().stream().forEach(entry2 -> log.debug(entry2.getKey() + " on " + entry2.getValue()
					.stream().map(se -> se.getInconsistency().name()).collect(Collectors.joining(", ", "[", "]"))));
		});
	}

	// Code by AB

	public void calculateStats(Map<PropertyUpdate, Set<SideEffect<ConsistencyRule>>> latestImpact,
			Map<ConsistencyRule, RepairNode> repairForRule) {
		// temp();
		latestImpact.entrySet().stream().forEach(entryL -> {
			// Set of effects
			Set<SideEffect<ConsistencyRule>> effects = entryL.getValue();
			// Client Operation
			PropertyUpdate clientop = entryL.getKey();
			for(SideEffect<ConsistencyRule> se_cre: effects) {
				ConsistencyRule cre = se_cre.getInconsistency();
				/*
				 * Checks if the current operation is the inverse of any previous operation. if
				 * yes; then removes the previous operation. As in other words, the user has
				 * undo the operation
				 */
				/*if (se_cre.getSideEffectType() != SideEffect.Type.NONE)
					checkClientOP(cre, clientop);*/
				/*Instance stepInst = cre.contextInstance();
				Property nameInst=cre.getProperty("name");
				Instance procInst = stepInst.getPropertyAsInstance("process");
				if(procInst.name().equals("dronology-task-v2_UAV-169"))
				{
					System.out.println("Process= "+procInst.name());
					System.out.println("CP= "+clientop.toString());
				}*/
				// In Case the side effect type is positive
				if (se_cre.getSideEffectType() == SideEffect.Type.POSITIVE) {
					calculatepositiveSideEffect(se_cre, clientop);
				}
			}
		});
	}

	public void calculatepositiveSideEffect(SideEffect<ConsistencyRule> se_cre, PropertyUpdate clientop) {
		ConsistencyRule cre = se_cre.getInconsistency();
		RepairNode rn = repairForRule.get(cre);
		double highestRank=-1;
		/*
		 * if the repair node is not null which means that the repair for the cre was
		 * suggested
		 */
		if (rn != null) {
			RepairTreeSorter rts = new RepairTreeSorter(node_counter.getRepairStats(), scorer);
			rts.setScoreAndRanks(rn);
			highestRank=rts.getMaxRank(rn);
			if(unselectedRepairstemp.containsKey(cre))
			{
				for(RepairAction ra: rn.getRepairActions())
				{
					if(!unselectedRepairstemp.get(cre).contains(ra) && !node_counter.doesRAExistAgainstCRE(cre, ra))
						unselectedRepairstemp.get(cre).add(ra);
				}
			}
			else
			{
				unselectedRepairstemp.put(cre, rn.getRepairActions());
			}
			// Traverse through repair actions to find the one which have been chosen by the
			// client
			for(RepairAction ra: rn.getRepairActions()) {
				// Checks if clientop matches the repair suggested by the repair tree
				if (this.doesOpMatchRepair(ra, clientop, clientop.elementId())) {
					// add the matched ra into the list
					node_counter.addCREClientOP(se_cre.getInconsistency(), clientop, ra);
					if(unselectedRepairstemp.containsKey(cre))
					{
						unselectedRepairstemp.get(cre).remove(ra);
					}
								
				}		
			}
		}
		if (cre.isConsistent()) // CRE is fulfilled
		{
			int loc = node_counter.getCRELocation(cre);
			if (loc != -1) // Means repair action does exist and we had a repairTree
			{
				double r=highestRank;
				Map<RepairAction, PropertyUpdate> allClientOp = node_counter.getClientOp(loc);
				allClientOp.entrySet().stream().forEach(entry -> {
					RepairAction_LogDS temp = new RepairAction_LogDS();
					temp.setCRE(cre);
					RepairAction raTemp=entry.getKey();
					temp.setRa(raTemp);
					Repair_template rt=new Repair_template();
					rt=rt.toRepairTemplate(raTemp);
					temp.setOriginalARL(rt.getOriginalARL());
					temp.setOperator(rt.getOperator());
					if(rt.getConcreteValue()!=null)
						temp.setConcreteValue(rt.getConcreteValue());
					else
						temp.setConcreteValue(null);
					temp.setClientOp(entry.getValue());
					temp.setRank(entry.getKey().getRank());
					temp.setScore(entry.getKey().getScore());
					OffsetDateTime odt_=this.time.getLastChangeTimeStamp();
					if(odt_!=null)
					{
						temp.setDate(odt_.toLocalDate().toString());
						temp.setTime(odt_.toLocalTime().toString());
					}
					else
					{
						temp.setDate("");
						temp.setTime("");
					}
					temp.setHighest_rank(r);
					selected_ra_Log.addData(temp);
				});
			} else // Means we didn't have the repair tree
			{
				RepairAction_LogDS temp = new RepairAction_LogDS();
				temp.setCRE(cre);
				temp.setRa(null);
				temp.setConcreteValue(null);
				temp.setOriginalARL(null);
				temp.setOperator(null);
				temp.setClientOp(clientop);
				temp.setRank(0);
				temp.setHighest_rank(0);
				temp.setScore(0);
				OffsetDateTime odt_=this.time.getLastChangeTimeStamp();
				if(odt_!=null)
				{
					temp.setDate(odt_.toLocalDate().toString());
					temp.setTime(odt_.toLocalTime().toString());
				}
				else
				{
					temp.setDate("");
					temp.setTime("");
				}
				selected_ra_Log.addData(temp);
			}
			// ra_Log.viewData();
			cre_Fulfilled(cre);
		}
	}

	public void cre_Fulfilled(ConsistencyRule cre) {
		/*
		 * Now the rule has been fulfilled so we need to store the repair templates
		 * against all the operations that leaded to the cre fulfillment. ToDo: the
		 * client operation list might contain the operations which were done by the
		 * user against the cre but didn't take part into the fulfillment of the cre.
		 * Will also be part of the repair template for now. Sticky notes have an
		 * example of them.
		 */
		int loc = node_counter.getCRELocation(cre);
		/*
		 * loc will give -1 if the cre doesn't exist in the list in which case we do not
		 * need to anything. but if it returns the location of the cre than we need to
		 * store the repair template and remove the cre from the list
		 */
		if (loc != -1) {
			node_counter.addtoSelectedRepairScore(loc);
			// Add the unselected repairs into the list as well as into the log
			Set<RepairAction> ras = unselectedRepairstemp.get(cre);
			if(ras!=null)
			{
			for(RepairAction ra:ras) {
				RepairAction_LogDS temp = new RepairAction_LogDS();
				temp.setCRE(cre);
				// adding to the templates
				node_counter.addtoUnSelectedRepairScore(ra);
				// adding to the log
				temp.setRa(ra);
				Repair_template rt=new Repair_template();
				rt=rt.toRepairTemplate(ra);
				//System.out.println(rt.getOriginalARL()+" "+rt.getOperator()+" "+rt.getConcreteValue());
				temp.setOriginalARL(rt.getOriginalARL());
				temp.setOperator(rt.getOperator());
				if(rt.getConcreteValue()!=null)
					temp.setConcreteValue(rt.getConcreteValue());
				else
					temp.setConcreteValue(null);
				temp.setClientOp(null);
				temp.setRank(ra.getRank());
				temp.setScore(ra.getScore());
				temp.setDate("");
				temp.setTime("");
				temp.setHighest_rank(0);
				unselected_ra_Log.addData(temp);
			}
			}
			node_counter.removeCRE(loc);
			unselectedRepairstemp.remove(cre);
		}
	}

	public void checkClientOP(ConsistencyRule cre, PropertyUpdate clientop) {
		int loc_cre = node_counter.getCRELocation(cre);
		if (loc_cre != -1) // Means the CRE exists in the list.
		{
			// Check if the current op is the inverse of any previous ones.
			Map<RepairAction, PropertyUpdate> cre_clientOP = node_counter.getClientOp(loc_cre);
			cre_clientOP.entrySet().stream().forEach(entryC_OP -> {
				PropertyUpdate prevOP = entryC_OP.getValue();
				// code to check if the current operation is the inverse of any previous ones.
				if (doesOperationsAreInverse(clientop, prevOP)) {
					// the operations are inverse of each other against same CRE
					cre_clientOP.remove(entryC_OP.getKey(), entryC_OP.getValue());
				}
			});
		} // else CRE doesn't exist
	}

	public boolean doesOperationsAreInverse(PropertyUpdate pu1, PropertyUpdate pu2) {
		if (pu1.elementId() == pu2.elementId()) {
			if (pu1.name().equals(pu2.name())) {
				if (pu1 instanceof PropertyUpdateAdd && pu2 instanceof PropertyUpdateRemove)
					return true;
				else if (pu2 instanceof PropertyUpdateAdd && pu1 instanceof PropertyUpdateRemove)
					return true;
				return false;
			}
			return false;
		}
		return false;
	}
	// Code By AB
	// End

	private static RepairTreeFilter rtf = new QARepairTreeFilter();

	private static class QARepairTreeFilter extends RepairTreeFilter {
		@Override
		public boolean compliesTo(RepairAction ra) {
			// FIXME: lets not suggest any repairs that cannot be navigated to in an
			// external tool.
			if (ra.getElement() == null)
				return false;
			Instance artifact = (Instance) ra.getElement();
			if (!artifact.hasProperty("html_url") || artifact.getPropertyAsValue("html_url") == null)
				return false;
			else
				return ra.getProperty() != null && !ra.getProperty().startsWith("out_") // no change to input or output
						&& !ra.getProperty().startsWith("in_") && !ra.getProperty().equalsIgnoreCase("name"); // typically
																												// used
																												// to
																												// describe
																												// key
																												// or id
																												// outside
																												// of
																												// designspace

		}
	}

	public void printRepairSizeStats() {
		repairSizeStats.entrySet().stream().forEach(entry -> {
			// lets print size first, for quicker viewing
			log.debug(
					entry.getValue().stream().map(count -> count.toString()).collect(Collectors.joining(", ", "[", "]"))
							+ " : " + entry.getKey());
		});
		collectedImpact.entrySet().stream().forEach(entry -> {
			Map<Type, List<SideEffect<ConsistencyRule>>> effects = entry.getValue().stream()
					.collect(Collectors.groupingBy(SideEffect<ConsistencyRule>::getSideEffectType));
			log.debug(entry.getKey() + " has following effects:");
			effects.entrySet().stream().forEach(entry2 -> log.debug(entry2.getKey() + " on " + entry2.getValue()
					.stream().map(se -> se.getInconsistency().name()).collect(Collectors.joining(", ", "[", "]"))));
		});
	}

	public void reset() {
		inconsistencyCausingNonRepairableOperations.clear();
		notsuggestedRepairOperations.clear();
		repairSizeStats.clear();
		conflicts.clear();
	}

	public StatsOutput getSerializableStats() {
		StatsOutput out = new StatsOutput();
		// conflicts
		out.setConflicts(conflicts.values().stream()
				.map(conf -> new SerializableConflict(conf.getPosRule().name(), conf.getNegRule().name(),
						conf.getChanges().stream().map(op -> op.name()).collect(Collectors.toList())))
				.collect(Collectors.toList()));
		// nonrepairableOperation
		Map<String, List<Set<String>>> serConflictCausingNonRepairableOperations = new HashMap<>();
		inconsistencyCausingNonRepairableOperations.entrySet().stream()
				.forEach(entry -> serConflictCausingNonRepairableOperations.put(entry.getKey().name(),
						convert(entry.getValue())));
		out.setInconsistencyCausingNonRepairableOperations(serConflictCausingNonRepairableOperations);
		// notsuggestedRepairs
		Map<String, List<Set<String>>> serNotsuggestedRepairOperations = new HashMap<>();
		notsuggestedRepairOperations.entrySet().stream().forEach(
				entry -> serNotsuggestedRepairOperations.put(entry.getKey().name(), convert(entry.getValue())));
		out.setNotsuggestedRepairOperations(serNotsuggestedRepairOperations);
		Map<String, List<Integer>> serRepairSizeStats = new HashMap<>();
		repairSizeStats.entrySet().stream()
				.forEach(entry -> serRepairSizeStats.put(entry.getKey().name(), entry.getValue()));
		out.setRepairSizeStats(serRepairSizeStats);
		out.setUnsupportedRepairs(this.unsupportedRepairs);
		return out;
	}

	private static Gson gson = new GsonBuilder().setPrettyPrinting().create();

	public String stats2Json(StatsOutput out) {
		return gson.toJson(out);
	}

	private List<Set<String>> convert(List<Set<PropertyUpdate>> nestedSet) {
		return nestedSet.stream().map(set -> set.stream().map(op -> op.name()).collect(Collectors.toSet()))
				.collect(Collectors.toList());
	}

	@Data
	@EqualsAndHashCode(onlyExplicitlyIncluded = true)
	public static class Conflict {
		@EqualsAndHashCode.Include
		private final ConsistencyRuleType posRule;
		@EqualsAndHashCode.Include
		private final ConsistencyRuleType negRule;
		private List<PropertyUpdate> changes = new LinkedList<>();

		public Conflict add(PropertyUpdate op) {
			changes.add(op);
			return this;
		}
	}

	@Data
	private static class SerializableConflict {
		private final String posRule;
		private final String negRule;
		private final List<String> changes;

	}

	@Setter
	public static class StatsOutput {
		List<SerializableConflict> conflicts;
		Map<String, List<Set<String>>> inconsistencyCausingNonRepairableOperations;
		Map<String, List<Set<String>>> notsuggestedRepairOperations;
		Map<String, List<Integer>> repairSizeStats;
		Map<String, String> unsupportedRepairs;
	}

	public RepairAction_LogStats getSelected_RepairLogStats() {
		return this.selected_ra_Log;
	}
	
	public RepairAction_LogStats getUnSelected_RepairLogStats() {
		return this.unselected_ra_Log;
	}

	public NodeCounter getnodeCounter() {
		return this.node_counter;
	}

	public RepairNodeScorer getRepairNodeScorer() {
		return this.scorer;
	}

	public void setRepairNodeScorer(RepairNodeScorer scorer) {
		this.scorer = scorer;
	}

	public Map<ConsistencyRule, Set<RepairAction>> getUnselectedRepairstemp() {
		return unselectedRepairstemp;
	}	

}
