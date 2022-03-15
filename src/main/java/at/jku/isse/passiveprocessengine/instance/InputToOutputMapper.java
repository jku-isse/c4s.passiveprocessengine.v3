package at.jku.isse.passiveprocessengine.instance;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import at.jku.isse.designspace.core.model.Element;
import at.jku.isse.designspace.core.model.Instance;
import at.jku.isse.designspace.core.model.Workspace;
import at.jku.isse.designspace.core.service.WorkspaceService;
import at.jku.isse.designspace.rule.arl.exception.ChangeExecutionException;
import at.jku.isse.designspace.rule.arl.repair.AbstractRepairAction;
import at.jku.isse.designspace.rule.arl.repair.ExecutableRepairAction;
import at.jku.isse.designspace.rule.arl.repair.Operator;
import at.jku.isse.designspace.rule.arl.repair.Repair;
import at.jku.isse.designspace.rule.arl.repair.RepairAction;
import at.jku.isse.designspace.rule.arl.repair.RepairNode;
import at.jku.isse.designspace.rule.checker.ConsistencyUtils;
import at.jku.isse.designspace.rule.model.ConsistencyRule;
import at.jku.isse.designspace.rule.model.ConsistencyRuleType;
import at.jku.isse.designspace.rule.service.RuleService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class InputToOutputMapper {



	@SuppressWarnings("unchecked")
	public static void mapInputToOutputInStepScope(ProcessStep step, ConsistencyRule crule) {
		if (crule.isConsistent()) return; // nothing to do


		ConsistencyRuleType crt = (ConsistencyRuleType) crule.getInstanceType();
		RepairNode rn = RuleService.repairTree(crule);

		if (!pruneRepairNode(rn, null)) {
			log.error("No repairs remain to fix InputToOutputMapping: "+crt.name());
			return;
		}
		log.info("Pruned RepairTree for : "+crt.name());
		ConsistencyUtils.printRepairTree(rn);

		Set<Object> objects = (Set<Object>) step.getDefinition().getExpectedOutput().values().stream()
				.distinct()
				.flatMap(type -> type.getInstancesIncludingThoseOfSubtypes().get().stream())
				.collect(Collectors.toSet());
		// this also works only if all instances that are relevant are somewhere in the designspace prefetched

		Set<Repair> repairs = getConcreteRepairs(objects, 1, crt, crule.contextInstance());
		//Set<Repair> repairs = rn.getConcreteRepairs(objects, false, false); 

		//Set<Repair> validConcreteRepairs = repairs.stream()
		//.filter(a -> a.isExecutable()) // get concrete repairs only
		//.filter(cr -> doesOnlyContainAddOrRemoveOperators(cr)) // only add/remove of elements allowed
		//.filter(cr -> onlyChangesOutput(cr)) // filter out anything that touches something other than the output
		//.collect(Collectors.toSet());

		repairs.stream().findAny().ifPresentOrElse(repair -> {
					log.debug("Executing Datamapping: "+repair);
					try {
						repair.execute();
						crt.workspace.concludeTransaction();
					} catch (ChangeExecutionException e) {
						log.error("Error executing repair "+repair);
						e.printStackTrace();
					}
				}
				, () -> log.error("No concrete Repairs found for "+crt.name())
		);


	}

	@SuppressWarnings("rawtypes")
	private static boolean doesOnlyContainAddOrRemoveOperators(Repair cr) {
		return cr.getRepairActions().stream()
				.allMatch(ra -> _onlyAddOrRemoveOperator((RepairAction) ra));
	}

	private static boolean _onlyAddOrRemoveOperator(RepairAction ra) {
		return ra.getOperator().equals(Operator.ADD) || ra.getOperator().equals(Operator.REMOVE);
	}

	@SuppressWarnings("unchecked")
	private static boolean onlyChangesOutput(Repair cr) {
		return cr.getRepairActions().stream()
				.filter(ExecutableRepairAction.class::isInstance)
				.map(ExecutableRepairAction.class::cast)
				.allMatch(era -> _onlyChangesOutput((RepairAction) era));
		//return false;
	}

	private static boolean _onlyChangesOutput(RepairAction ra) {
		return ra.getProperty() != null && ra.getProperty().startsWith("out_");
	}

	private static boolean pruneRepairNode(RepairNode rn, RepairNode parent) {
		// we recursively visit all nodes and remove those that are not ADD or REMOVE operations, and those where the property is null or does not start with out_	

		// first, due to hashCode() problem, reinsert children (otherwise, we cant remove them when necessary)
		Set<RepairNode> copy = Set.copyOf(rn.getChildren());
		rn.getChildren().clear();
		copy.stream().forEach(rnChild -> rn.addChild(rnChild));

		switch(rn.getNodeType()) {
			case ALTERNATIVE:
				// if this is an alternative, ensure that at least one remains
				List.copyOf(rn.getChildren()).stream()
						.filter(childRn -> !pruneRepairNode(childRn, rn))
						.forEach(childRn -> {
							rn.removeChild(childRn); });
				if (rn.getChildren().size() == 1 && parent != null) {// then rewrite tree as this child is the only alternative, unless this is the root node
					RepairNode child = rn.getChildren().iterator().next();
					parent.addChild(child);
					child.setParent(parent);
					parent.removeChild(rn);
				}
				return (rn.getChildren().size() > 0);
			case SEQUENCE:
				// ensure all remain valid
				int preCount = rn.getChildren().size();
				List.copyOf(rn.getChildren()).stream()
						.filter(childRn -> !pruneRepairNode(childRn, rn))
						.forEach(childRn -> {
							rn.removeChild(childRn);
						});
				int postCount = rn.getChildren().size();
				return preCount == postCount;
			case MULTIVALUE:
				// not sure what this does
				// lets fall through
			case VALUE:
				boolean doKeep = true;
				if (rn instanceof RepairAction) {
					doKeep =	_onlyAddOrRemoveOperator((RepairAction)rn)
							&& _onlyChangesOutput((RepairAction)rn);
				}
				return doKeep;
			default:
				return false;
		}
	}

	public static Set<Repair> getConcreteRepairs(Set<Object> objects, int limit,ConsistencyRuleType crd, Instance contextInstance) {
		ConsistencyRuleType childCRD = getChildWorkspaceConsistencyRule(crd);
		childW.update();
		childW.concludeTransaction();
		RuleService.currentWorkspace = childW;
		RuleService.evaluator.evaluateAll();
		ConsistencyRule inconsistency = childCRD.consistencyRuleEvaluation(contextInstance);
		RuleService.evaluator.evaluate(inconsistency);
		RepairNode repairTree = RuleService.repairTree(inconsistency);
		if(repairTree == null || !pruneRepairNode(repairTree, null))
			return null;
		if (repairTree.isExecutable())
			return repairTree.getRepairs();

		Set<Repair> concreteRepairs = repairTree.getConcreteRepairs(objects.parallelStream().map(obj -> childW.its((Instance)obj)).collect(Collectors.toSet()),limit);
		convertRepairsToWorkspace(crd.workspace,concreteRepairs);
		RuleService.currentWorkspace = crd.workspace;
		return concreteRepairs;
	}

	static Workspace childW = null;

	protected static ConsistencyRuleType getChildWorkspaceConsistencyRule(ConsistencyRuleType crd){
		if(childW == null)
			createChildWorkspace(crd.workspace);
		Object childInconsistency =  childW.findElement(crd.id());
		return (ConsistencyRuleType) childInconsistency;
	}

	private static void createChildWorkspace(Workspace workspace) {
		childW = WorkspaceService.createWorkspace("childWorkspace", workspace,
				WorkspaceService.ANY_USER, null, false, false);
//        childW.update();
//        childW.concludeTransaction();
	}

	private static void convertRepairsToWorkspace(Workspace targetWorkspace, Set<Repair> repairs){
		for (Repair repair : repairs) {
			for (Object o : repair.getRepairActions()) {
				RepairAction action = (RepairAction) o;
				ConsistencyRule cre = (ConsistencyRule) action.getInconsistency();
				action.setInconsistency(targetWorkspace.findElement(cre.id()));
				Element e = (Element) action.getElement();
				if (e != null)
					action.setElement(targetWorkspace.findElement(e.id()));
				Object value = action.getValue();
				Instance instanceValue;
				if (value instanceof Instance) {
					instanceValue = (Instance) value;
					action.setValue(targetWorkspace.findElement(instanceValue.id()));
				}
			}
		}
	}
}
