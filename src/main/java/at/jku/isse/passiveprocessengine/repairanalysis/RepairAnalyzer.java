package at.jku.isse.passiveprocessengine.repairanalysis;

import static org.assertj.core.api.Assertions.entry;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import at.jku.isse.designspace.core.events.PropertyUpdate;
import at.jku.isse.designspace.core.model.Instance;
import at.jku.isse.designspace.rule.arl.evaluator.RuleEvaluationIterationMetadata;
import at.jku.isse.designspace.rule.arl.evaluator.RuleEvaluationListener;
import at.jku.isse.designspace.rule.arl.exception.RepairException;
import at.jku.isse.designspace.rule.arl.repair.ContextualizedPositiveSideEffect;
import at.jku.isse.designspace.rule.arl.repair.RepairAction;
import at.jku.isse.designspace.rule.arl.repair.RepairNode;
import at.jku.isse.designspace.rule.arl.repair.RepairTreeFilter;
import at.jku.isse.designspace.rule.arl.repair.SideEffect;
import at.jku.isse.designspace.rule.arl.repair.SideEffect.Type;
import at.jku.isse.designspace.rule.arl.repair.order.Event_DS;
import at.jku.isse.designspace.rule.arl.repair.order.ProcessChangeEvents;
import at.jku.isse.designspace.rule.arl.repair.order.RepairNodeScorer;
import at.jku.isse.designspace.rule.arl.repair.order.RepairStats;
import at.jku.isse.designspace.rule.arl.repair.order.RepairTreeSorter;
import at.jku.isse.designspace.rule.arl.repair.order.Repair_template;
import at.jku.isse.designspace.rule.model.ConsistencyRule;
import at.jku.isse.designspace.rule.model.ConsistencyRuleType;
import at.jku.isse.designspace.rule.model.Rule;
import at.jku.isse.designspace.rule.service.RuleService;
import at.jku.isse.passiveprocessengine.Context;
import at.jku.isse.passiveprocessengine.designspace.DesignSpaceSchemaRegistry;
import at.jku.isse.passiveprocessengine.instance.activeobjects.ProcessStep;
import at.jku.isse.passiveprocessengine.monitoring.ITimeStampProvider;
import at.jku.isse.passiveprocessengine.monitoring.RepairMatcher;
import at.jku.isse.passiveprocessengine.monitoring.UsageMonitor;
import at.jku.isse.passiveprocessengine.repairanalysis.AnalysisDTOs.Conflict;
import at.jku.isse.passiveprocessengine.repairanalysis.AnalysisDTOs.SerializableConflict;
import at.jku.isse.passiveprocessengine.repairanalysis.AnalysisDTOs.StatsOutput;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RepairAnalyzer implements RuleEvaluationListener {

	// which rules have now a changed result (either now fulfilled, or now
	// unfulfilled)
	Map<ConsistencyRule, Boolean> changedRuleResult = new HashMap<>();
	// impact in this iteration (collected changes without rule changes)
	Map<PropertyUpdate, Set<SideEffect<ConsistencyRule>>> latestImpact = new LinkedHashMap<>();
	// impact since initialization
	Map<PropertyUpdate, Set<SideEffect<ConsistencyRule>>> collectedImpact = new LinkedHashMap<>();
	// last/latest repair for rule (updated at the end of previous iteration)
	Map<ConsistencyRule, RepairNode> repairForRule = new HashMap<>();

	// how many repair nodes are suggested for each rule, we collect across all rule
	// instances, per type
	Map<ConsistencyRuleType, List<Integer>> repairSizeStats = new HashMap<>();
	// which rules are part of a conflict, due to which operation
	Map<String, Conflict> conflicts = new HashMap<>();
	Map<ConsistencyRuleType, List<Set<PropertyUpdate>>> inconsistencyCausingNonRepairableOperations = new HashMap<>();
	Map<ConsistencyRuleType, List<Set<PropertyUpdate>>> notsuggestedRepairOperations = new HashMap<>();
	Map<String, String> unsupportedRepairs = new HashMap<>();

	final UsageMonitor monitor;
	// Added field
	ProcessChangeEvents pce;
	final RepairNodeScorer scorer;
	final ITimeStampProvider time;
	// end
	final DesignSpaceSchemaRegistry designspace;
	final Context context;

	public RepairAnalyzer(RepairStats rs, RepairNodeScorer scorer, ITimeStampProvider timeprovider, UsageMonitor monitor, DesignSpaceSchemaRegistry designspace, Context context) {
	
		this.pce=new ProcessChangeEvents(rs);
		this.scorer = scorer;
		this.time=timeprovider;
		this.monitor = monitor;
		this.context = context;
		this.designspace = designspace;
	}


	public Map<PropertyUpdate, Set<SideEffect<ConsistencyRule>>> getImpact() {
		return collectedImpact;
	}

	/**
	 * whenever a rule was reevaluated, we need to update the repair tree 
	 * 1) rule consistent before => now inconsistent: 
	 * 		generate/store repair tree -operation is negative with respect to this rule 
	 * 2) consistent --> consistent: 
	 * 	no updated of repair tree - operation is neutral with respect to this rule 
	 * 3) inconsistent -> consistent: 
	 * 		check which repairs in the tree where applied,
	 * 		mark tree as complete, operation is positive with respect to this rule 
	 * 4) inconsistent -> inconsistent: 
	 * 		if repair tree the same - operation is neutral,
	 * 		if repair tree larger - operation is negative (unless operation matches part
	 * 		of the repair, then other operation caused further inconsistencies)
	 */
	@Override
	public void signalRuleEvaluationFinished(Set<RuleEvaluationIterationMetadata> iterationMetadata) {
		// here we get only those operations that cause a reevaluation, regardless if outcome changed or not,
		// we only need to check if the rule is one we want to analyse (i.e., here no mapping rules, just pre/post/qa cond)

		// fill legacy datastructures
		determineChangedRuleEvaluationsAndImpact(iterationMetadata);

		determineConflictingSideEffects();
		determineConflictCausingNonRepairableOperations(); // this and the following method measure the same effect,
															// upon inconsistency appearance, the other upon repair.
		determineNotsuggestedRepairOperations();
		logPositiveRepairs();
		this.processLatestChanges();


		// prepare for next round: for all unfulfilled constraints that were potentially
		// affected now by changes, we store the current repair tree
		changedRuleResult.keySet().stream()
		.filter(cre -> !cre.isConsistent())
		.forEach(cre -> {
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
		// also for any that are now consistent remove the repair tree entry
		changedRuleResult.keySet().stream()
		.filter(cre -> cre.isConsistent())
		.forEach(cre -> {
			repairForRule.remove(cre);
		});

		// cleanup:
		//queuedUpdates.clear();
		changedRuleResult.clear();
		// we also need to clear the impact
		collectedImpact.putAll(latestImpact);
		// end call
		latestImpact.clear();
	}

	/*
	 * replaces deterimeChangeRuleEvaluations(Operations) and isRelevant(operation) and determinePreliminaryImpact(operation)
	 * */
	private void determineChangedRuleEvaluationsAndImpact(Set<RuleEvaluationIterationMetadata> changeSet) {
		changeSet.stream()
		.filter(reim -> {
			Rule el = reim.getRule();
			if (el instanceof ConsistencyRule && el.getInstanceType().name().startsWith("crd")
					&& !el.getInstanceType().name().startsWith("crd_datamapping")
					&& !el.getInstanceType().name().startsWith("crd_prematuretrigger")) {
				return true;
			} else return false;
		})
		.forEach(reim -> {
				ConsistencyRule cre = (ConsistencyRule) reim.getRule();
				if (reim.getHasEvaluationOutcomeChanged()) {
					changedRuleResult.put(cre, true);
					if (!cre.isConsistent() && repairForRule.containsKey(cre)) // for newly inconsistent rule, we should not have an old repair tree cached
						log.error("Old Repair tree available for "+cre);
				} else
					changedRuleResult.put(cre, false);
				// now how to deal with repairs:
//				if (reim.getHasEvaluationOutcomeChanged() && !cre.isConsistent()) {
//					// if the rule was so far fulfilled, then some prior change violated it, and we
//					// need to determine which change that was by looking at the repair tree
//					// hence lets obtain the current repair tree. But filter it down to relevant
//					// repairs
//					try {
//						RepairNode rn = RuleService.repairTree(cre);
//						rtf.filterRepairTree(rn);
//						assert(rn != null);
//						repairForRule.put(cre, rn);
//					} catch (RepairException e) {
//						unsupportedRepairs.put(cre.getInstanceType().name(), e.getMessage());
//						e.printStackTrace();
//					}
//				} else {
//					// now the rule is fulfilled, and we want to check which action might have
//					// contributed to the fulfillment, hence we keep the old repair tree, we should
//					// have one stored
//				}
				// now determine the operation's impact
				reim.getEvaluationTriggers().stream()
					.filter(PropertyUpdate.class::isInstance)
					.map(PropertyUpdate.class::cast)
					.forEach(op -> {
						latestImpact.compute(op, (k, v) -> v == null ? new HashSet<>() : v)
									.add(determineSideEffectType(op, cre));
				});
		});
	}

	private SideEffect<ConsistencyRule> determineSideEffectType(PropertyUpdate op, ConsistencyRule cr) {
		Id id = op.elementId();
		// if cr has changed
		if (changedRuleResult.get(cr)) {
			// and now is fulfilled
			if (cr.isConsistent()) {
				RepairNode rNodeOld = repairForRule.get(cr); // only be null if this is the first eval and was never
																// negative in previous rounds
				// was this change truly causing (part of ) the repair, see if it occurs in the
				// repair tree TODO: (and no other action covered it so far). if so --> positive
				if (rNodeOld == null) //the first time evaluating with fulfillment, thus no real true repair.
					return new SideEffect<>(cr, Type.POSITIVE); // but counted as such to avoid nonsuggestedRepairs to not incorrectly flag this.
				else {
					Optional<RepairAction> optRA = rNodeOld.getRepairActions().stream().filter(ra -> RepairMatcher.doesOpMatchRepair(ra, op, id)).findAny();
					if (optRA.isPresent())
						return new ContextualizedPositiveSideEffect<>(cr, Type.POSITIVE, optRA.get()); //new SideEffect<ConsistencyRule>(cre, determineType(op, cre))
					else
						return new SideEffect<>(cr,Type.NONE);
				}
			} else { // no longer consistent
				try {
					// name and instance of the rule to be printed here
					RepairNode rNodeNow = RuleService.repairTree(cr);
					rtf.filterRepairTree(rNodeNow); // we need to filter out irrelevant repairs
					// was this change truly causing (or part of) the inconsistency, see if its
					// inverse action occurs in the repair tree. if so --> negative
					if (rNodeNow.getRepairActions().stream().anyMatch(ra -> RepairMatcher.doesOpMatchInvertedRepair(ra, op, id)))
						return new SideEffect<>(cr,Type.NEGATIVE);
					else
						return new SideEffect<>(cr,Type.NONE);
				} catch (RepairException e) {
					unsupportedRepairs.put(cr.getInstanceType().name(), e.getMessage());
					e.printStackTrace();
					return new SideEffect<>(cr,Type.ERROR); // misuse of ERROR, as this is not about the repair but the being able to repair
										// in the first place.
				}
			}
		} else {
			if (cr.isConsistent()) // all is still fine, no further analysis needed
				return new SideEffect<>(cr,Type.NONE);
			else {
				// rule result hasn't changed, thats not to say, that change might not now have
				// introduced another inconsistency
				// but we would need to determine this first
				RepairNode rNodeOld = repairForRule.get(cr); // must not be null if its still negative,
				//except if this is the first rule evaluation that results also in unfulfilled, which means no change in outcome
				// typically this is happening upon artifact instantiation, hence we dont consider that creation as negative
//				assert(rNodeOld != null);
				if (rNodeOld == null) {
					return new SideEffect<>(cr,Type.NONE); //
				}
				try {
					RepairNode rNodeNow = RuleService.repairTree(cr);
					rtf.filterRepairTree(rNodeNow); // we need to filter out irrelevant repairs
				// this rNodeOld is the prior one, not for the current rule state
				// look whether old repair nodes included this operation , if so then positive
					Optional<RepairAction> optRA = rNodeOld.getRepairActions().stream().filter(ra -> RepairMatcher.doesOpMatchRepair(ra, op, id)).findAny();
					if (optRA.isPresent()) {
						if (rNodeNow.getRepairActions().stream().anyMatch(ra -> RepairMatcher.doesOpMatchRepair(ra, op, id)) ) // if the new repair tree also includes the repair, then the action matched but was not successful (probably due to restrictions not being fulfilled)
							return new SideEffect<>(cr,Type.NONE);
						else
							return new ContextualizedPositiveSideEffect<>(cr, Type.POSITIVE, optRA.get());
					} // else
					// look whether new repair nodes include this inverse operation, but the old one did not, (i.e., the repair was added) if so then
					// negative
					if (rNodeNow.getRepairActions().stream().anyMatch(ra -> RepairMatcher.doesOpMatchInvertedRepair(ra, op, id)) &&
							!rNodeOld.getRepairActions().stream().anyMatch(ra -> RepairMatcher.doesOpMatchInvertedRepair(ra, op, id))) {
						return new SideEffect<>(cr,Type.NEGATIVE);
					} else {// else: e.g., constraint req that status = open, but status was in progress and now is released, thus no improvement, but no further decline either, hence NONE
						return new SideEffect<>(cr,Type.NONE);
					}
				} catch (RepairException e) {
					unsupportedRepairs.put(cr.getInstanceType().name(), e.getMessage());
					e.printStackTrace();
					return new SideEffect<>(cr,Type.ERROR); // misuse of ERROR, as this is not about the repair but the being able to repair
										// in the first place.
				}
			}
		}
	}



	private void logPositiveRepairs() {
		// for each CRE we check if there are positive effects:
		latestImpact.entrySet().stream().forEach(entry -> {
			Map<Type, List<SideEffect<ConsistencyRule>>> effects = entry.getValue().stream()
					.collect(Collectors.groupingBy(SideEffect<ConsistencyRule>::getSideEffectType));
			if (effects.getOrDefault(Type.POSITIVE, Collections.emptyList()).size() > 0) {
				// we have at least one positive effects

				effects.get(Type.POSITIVE).stream()
						.filter(se -> se instanceof ContextualizedPositiveSideEffect<?>)
						.forEach(se -> 	monitor.repairActionExecuted(designspace.getWrappedRuleResult(se.getInconsistency()),
															context.getWrappedInstance(ProcessStep.class, designspace.getWrappedInstance(se.getInconsistency().contextInstance())),
															Repair_template.toRepairTemplate(((ContextualizedPositiveSideEffect<ConsistencyRule>) se).getMatchingRepair()).asString(),
															-1)
						);
			}
		});
	}

	private void determineConflictCausingNonRepairableOperations() {
		// check for every consistency rule that is now newly inconsistent whether there
		// is at least one NEGATIVE impact,
		changedRuleResult.entrySet().stream().filter(entry -> entry.getValue())
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
							log.warn("Newly Inconsistent rule has no repair for any of the action(s) ["
									+ causes.stream().map(cause -> cause.toString()).collect(Collectors.joining(","))
									+ "] that where part of the root inconsistency cause of: " + cre.name());
						}
					}
				});
	}

	private void determineNotsuggestedRepairOperations() {
		// check for every consistency rule that is now newly consistent whether there
		// is at least one POSITIVE impact
		changedRuleResult.entrySet().stream().filter(entry -> entry.getValue())
				.filter(entry -> entry.getKey().isConsistent())
				.map(entry -> entry.getKey()).forEach(cre -> {
					boolean hasPosImpact = latestImpact.values().stream()
							.flatMap(set -> set.stream())
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
							RepairNode rNodeOld = repairForRule.get(cre);
							Instance changedInst = designspace.getWorkspace().findElement(causes.iterator().next().elementId());
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
						.map(se -> se.getInconsistency())
						.forEach(crt -> effects.get(Type.NEGATIVE).stream()
								.map(seNeg -> seNeg.getInconsistency())
								.forEach(crtNeg -> { // crtName.name().equalsIgnoreCase("crd_qaspec_SRStoTCtrace_SubWP-frq-v3") && crtNeg.name().equalsIgnoreCase("crd_qaspec_SRStoFUtrace_SubWP-frq-v3")
//									RepairNode rNodeNew = RuleService.repairTree(crt);// used for debugging only
//									RepairNode rNodeOld = repairForRule.get(crt);
									ConsistencyRuleType crtType = (ConsistencyRuleType) crt.getInstanceType();
									ConsistencyRuleType crtNegType = (ConsistencyRuleType) crtNeg.getInstanceType();
									conflicts
											.compute(crt.name() + crtNeg.name(),
													(k, v) -> v == null ? new Conflict(crtType, crtNegType) : v)
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

	public void processLatestChanges() {

		latestImpact.entrySet().stream().forEach(entryPU->{

			for(SideEffect<ConsistencyRule> se_cre: entryPU.getValue())
			{
				/*Storing the data of all changes along with  their details i.e. effectType, constraint, operation, process, etc. */
				ConsistencyRule cre = se_cre.getInconsistency();
				PPEInstance stepInst = cre.contextInstance();
				String rule=cre.getProperty("name").getValue().toString();
				Event_DS event=new Event_DS(entryPU.getKey(), null, se_cre,cre, cre.isConsistent(), stepInst, null, time.getLastChangeTimeStamp(), 0, 0, 0);
				this.pce.addAllExecuteEventLog(event);
				this.pce.identifyUndo(event);
				//ToDo: generate Signals for these special calls
				if(se_cre.getSideEffectType()==SideEffect.Type.POSITIVE)
				{
					/*For ranking we are only counting these changes w.r.t the constraint. As they are the ones
					 * that are leading the rule towards the fulfillment.*/
					updateCRE_matrix(se_cre, entryPU.getKey(),stepInst,time.getLastChangeTimeStamp());
					if(cre.isConsistent())// change lead to cre fulfillment
					{
						this.pce.updateRepairTemplateScores(cre);
					}
				}
				else if(se_cre.getSideEffectType()==SideEffect.Type.NEGATIVE)
				{
					/*The change has lead the constraint away from fulfillment or have introduced more inconsistencies.*/
				}
				else if(se_cre.getSideEffectType()==SideEffect.Type.NONE)
				{
					/*The change has no effect neither fulfilling nor unfulfilling.*/
				}
			}

		});
	}

	public void updateCRE_matrix(SideEffect<ConsistencyRule> se_cre, PropertyUpdate clientop, Instance stepInst, OffsetDateTime dateTime)
	{
		ConsistencyRule cre = se_cre.getInconsistency();
		String rule=cre.getProperty("name").getValue().toString();
		RepairNode rn = repairForRule.get(cre);
		int highestRank=-1;
		/*
		 * if the repair node is not null that means the repair for the cre was suggested
		 */
		if (rn != null) {
			RepairTreeSorter rts=new RepairTreeSorter(this.pce.getRs(), scorer);
			rts.updateTreeOnScores(rn,rule);
			highestRank=rts.getMaxRank(rn);
			for(RepairAction ra: rn.getRepairActions()) {
				// Checks if clientop matches the repair suggested by the repair tree
				if (RepairMatcher.doesOpMatchRepair(ra, clientop, clientop.elementId()))
				{
					Repair_template rt=new Repair_template();
					rt=Repair_template.toRepairTemplate(ra);
					Event_DS event=new Event_DS(clientop, ra, se_cre,cre, cre.isConsistent(), stepInst, rt, dateTime, highestRank,ra.getRank(),ra.getScore());
					this.pce.addCRE_CurrentEventList(event);
					this.pce.updateExecutedEventLog(se_cre,clientop,stepInst,dateTime,rt,ra);
				}
				else // Storing the repairs suggested but not selected by the developer.
				{
					Repair_template rt=new Repair_template();
					rt=Repair_template.toRepairTemplate(ra);
					Event_DS event=new Event_DS(null, ra,null, cre, cre.isConsistent(), stepInst, rt, time.getLastChangeTimeStamp(),
							highestRank, ra.getRank(), ra.getScore());
					this.pce.addUnSelectRepairLog(event);
				}
			}
		}
	}

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
		//	return true;
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
						conf.getChanges().stream().map(op -> op.name()+"_"+op.getClass().getSimpleName()).collect(Collectors.toList())))
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
		return nestedSet.stream().map(set -> set.stream().map(op -> op.name()+"_"+op.getClass().getSimpleName()).collect(Collectors.toSet()))
				.collect(Collectors.toList());
	}



	public RepairNodeScorer getRepairNodeScorer() {
		return this.scorer;
	}

//	public void setRepairNodeScorer(RepairNodeScorer scorer) {
//		this.scorer = scorer;
//	}

	public ProcessChangeEvents getTd() {
		return this.pce;
	}

	public void setTd(ProcessChangeEvents td) {
		this.pce = td;
	}
	public ITimeStampProvider getTime() {
		return time;
	}

//	public void setTime(ReplayTimeProvider time) {
//		this.time = time;
//	}



}
