package at.jku.isse.passiveprocessengine.instance;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import at.jku.isse.designspace.core.model.Element;
import at.jku.isse.designspace.core.model.Instance;
import at.jku.isse.designspace.core.model.Workspace;
import at.jku.isse.designspace.core.service.WorkspaceService;
import at.jku.isse.designspace.rule.arl.exception.ChangeExecutionException;
import at.jku.isse.designspace.rule.arl.repair.Operator;
import at.jku.isse.designspace.rule.arl.repair.Repair;
import at.jku.isse.designspace.rule.arl.repair.RepairAction;
import at.jku.isse.designspace.rule.arl.repair.RepairNode;
import at.jku.isse.designspace.rule.arl.repair.RepairTreeFilter;
import at.jku.isse.designspace.rule.model.ConsistencyRule;
import at.jku.isse.designspace.rule.model.ConsistencyRuleType;
import at.jku.isse.designspace.rule.service.RuleService;
import at.jku.isse.passiveprocessengine.instance.messages.Events;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class InputToOutputMapper {

	private static RepairTreeFilter rtf = new OutputUpdateRepairTreeFilter();

	@SuppressWarnings("unchecked")
	public static List<Events.ProcessChangedEvent> mapInputToOutputInStepScope(ProcessStep step, ConsistencyRule crule) {
		if (crule.isConsistent()) {
			log.info("MappingRule became consistent while request was queued "+crule.toString());
			return Collections.emptyList(); // nothing to do
		}

		ConsistencyRuleType crt = (ConsistencyRuleType) crule.getInstanceType();
		RepairNode repairTree = RuleService.repairTree(crule);
		
		rtf.filterRepairTree(repairTree);
		//DONE: if there are (only) concrete repair actions (which should be the case with symmetric difference)
		
		
//		Set<Object> objects = step.getDefinition().getExpectedOutput().values().stream()
//			.distinct()
//			.flatMap(type -> type.instancesIncludingThoseOfSubtypes())
//			.collect(Collectors.toSet());
//		if (objects.size() == 0) {
//			log.error(String.format("No candidates for fixing datamapping % found", crt.name() ));
//			return Collections.emptyList();
//		}	
//		// this also works only if all instances that are relevant are somewhere in the designspace prefetched
//		Set<Repair> repairs = null;
//		try {
//		 repairs = //ConsistencyUtils.getConcreteRepairs(null, objects, 1, crt, crule.contextInstance());//.repairTree.getConcreteRepairs(objects, 1);// 
//				 				getConcreteRepairs(objects, 1, crt, crule.contextInstance()); 

		Set<Repair> repairs = repairTree.getRepairs();
		if (repairs == null) {
			String state = crule.isConsistent() ? "CONSISTENT" : "INCONSISTENT";
			log.error("FATAL: No repairs could be created for "+state+" "+crt.name());
			// check if there is a rule error, print that, hence later retry needed
			return Collections.emptyList();
		}
		repairs.stream().findAny().ifPresentOrElse(repair -> {
				log.debug("Executing Datamapping: "+repair);
				try {
					repair.execute();
					//crt.workspace.concludeTransaction();
				} catch (ChangeExecutionException e) {
					log.error("Error executing repair "+repair);
					e.printStackTrace();
				}
				}
				, () -> { 
						log.error("No concrete Repairs found for "+crt.name());
				//TODO: THIS NEEDS TO BE FIXED:		throw new RuntimeException("Datamapping could not be repaired");
					}
				);
		
//		}catch(Exception e) {
//			log.error("Error while obtaining repairtree for" +crt.name() +": "+e.getMessage());
//			e.printStackTrace();
//		}
		return Collections.emptyList(); //FIXME: somehow determine from repairs what was added and removed.
	}
	
//	@SuppressWarnings("rawtypes")
//	private static boolean doesOnlyContainAddOrRemoveOperators(Repair cr) {
//		return cr.getRepairActions().stream()
//				.allMatch(ra -> _onlyAddOrRemoveOperator((RepairAction) ra));
//	}
	
	private static boolean _onlyAddOrRemoveOperator(RepairAction ra) {
		return ra.getOperator().equals(Operator.ADD) || ra.getOperator().equals(Operator.REMOVE);
	}
	
//	@SuppressWarnings("unchecked")
//	private static boolean onlyChangesOutput(Repair cr) {
//		return cr.getRepairActions().stream()
//			.filter(RepairAction.class::isInstance)
//			.map(RepairAction.class::cast)
//			.allMatch(era -> _onlyChangesOutput((RepairAction) era));
//		//return false;
//	}
	
	private static boolean _onlyChangesOutput(RepairAction ra) {
		return ra.getProperty() != null && ra.getProperty().startsWith("out_");
	}
	
	
//	private static boolean pruneRepairNode(RepairNode rn, RepairNode parent) {
//		// we recursively visit all nodes and remove those that are not ADD or REMOVE operations, and those where the property is null or does not start with out_	
//		
//		// first, due to hashCode() problem, reinsert children (otherwise, we cant remove them when necessary)
//		Set<RepairNode> copy = Set.copyOf(rn.getChildren());
//		rn.getChildren().clear();
//		copy.stream().forEach(rnChild -> rn.addChild(rnChild));
//		
//		switch(rn.getNodeType()) {
//		case ALTERNATIVE:
//			// if this is an alternative, ensure that at least one remains
//			List.copyOf(rn.getChildren()).stream()
//				.filter(childRn -> !pruneRepairNode(childRn, rn))
//				.forEach(childRn -> { 
//					rn.removeChild(childRn); });
//			if (rn.getChildren().size() == 1 && parent != null) {// then rewrite tree as this child is the only alternative, unless this is the root node
//				RepairNode child = rn.getChildren().iterator().next();
//				parent.addChild(child);
//				child.setParent(parent);
//				parent.removeChild(rn);
//			}
//			return (rn.getChildren().size() > 0);
//		case SEQUENCE:
//			// ensure all remain valid
//			int preCount = rn.getChildren().size();
//			List.copyOf(rn.getChildren()).stream()
//			.filter(childRn -> !pruneRepairNode(childRn, rn))
//			.forEach(childRn -> { 
//				rn.removeChild(childRn);		
//			});
//			int postCount = rn.getChildren().size();
//			return preCount == postCount;
//		case MULTIVALUE:
//			// not sure what this does
//			// lets fall through
//		case VALUE:
//			boolean doKeep = true;
//			if (rn instanceof RepairAction) {
//				doKeep =	_onlyAddOrRemoveOperator((RepairAction)rn) 
//							&& _onlyChangesOutput((RepairAction)rn);
//			}
//			return doKeep;
//		default:
//			return false;
//		}
//	}
//	
	static Workspace childW = null;

  protected static Set<Repair> getConcreteRepairs(Set<Object> objects, int limit,ConsistencyRuleType crd, Instance contextInstance) {
  if(childW == null)
  	childW = WorkspaceService.createWorkspace("childWorkspace",crd.workspace,
              WorkspaceService.ANY_USER, null, false, false);
  else {
  	childW.update();
      childW.concludeTransaction();
  }
 
  ConsistencyRuleType childCRD =  childW.findElement(crd.id());
 
  RuleService.currentWorkspace = childW;
  RuleService.evaluator.evaluateAll();
  ConsistencyRule inconsistency = childCRD.consistencyRuleEvaluation(contextInstance);
  RuleService.evaluator.evaluate(inconsistency);
  RepairNode repairTree = RuleService.repairTree(inconsistency);
  if(repairTree == null) {
  	RuleService.currentWorkspace = crd.workspace;
  	String state = inconsistency.isConsistent() ? "CONSISTENT" : "INCONSISTENT";
  	log.error("Repairtree is null for "+state+" crd: "+crd);
  	return null;
  }
  RepairTreeFilter rtf = new OutputUpdateRepairTreeFilter();
  rtf.filterRepairTree(repairTree);
  Set<Repair> concreteRepairs = repairTree.getConcreteRepairs(objects.parallelStream().map(obj -> childW.its((Instance)obj)).collect(Collectors.toSet()),limit);
  convertRepairsToWorkspace(crd.workspace,concreteRepairs);
  RuleService.currentWorkspace = crd.workspace;
  return concreteRepairs;
 
}
	
	
//    protected static Set<Repair> getConcreteRepairs(Set<Object> objects, int limit,ConsistencyRuleType crd, Instance contextInstance) {
//        if(childW == null)
//        	childW = WorkspaceService.createWorkspace("childWorkspace",crd.workspace,
//                    WorkspaceService.ANY_USER, null, false, false);
//        else {
//        	childW.update();
//            childW.concludeTransaction();
//        }
//        ConsistencyRuleType childCRD =  childW.findElement(crd.id());
//        RuleService.currentWorkspace = childW;
//        RuleService.evaluator.evaluateAll();
//        ConsistencyRule inconsistency = childCRD.consistencyRuleEvaluation(contextInstance);
//        RuleService.evaluator.evaluate(inconsistency);
//        RepairNode repairTree = RuleService.repairTree(inconsistency);
//        if(repairTree == null || !pruneRepairNode(repairTree, null)) {
//        	RuleService.currentWorkspace = crd.workspace;
//        	return null;
//        }
//        Set<Repair> concreteRepairs = repairTree.getConcreteRepairs(objects.parallelStream().map(obj -> childW.its((Instance)obj)).collect(Collectors.toSet()),limit);
//        convertRepairsToWorkspace(crd.workspace,concreteRepairs);
//        RuleService.currentWorkspace = crd.workspace;
//        return concreteRepairs;
//    }
//    
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
    
    private static class OutputUpdateRepairTreeFilter extends RepairTreeFilter {

		@Override
		public boolean compliesTo(RepairAction repairAction) {
			return _onlyAddOrRemoveOperator(repairAction) && _onlyChangesOutput(repairAction);
		}
    	
    }
}
