package at.jku.isse.passiveprocessengine.utils;

import java.util.Collection;
import java.util.stream.Collectors;

import at.jku.isse.designspace.core.model.InstanceType;
import at.jku.isse.designspace.rule.model.ConsistencyRuleType;


public class RuleUtils {

	
//	protected ConsistencyRuleType getRuleByNameAndContext(String name, InstanceType context) {
//		Collection<InstanceType> ruleDefinitions = ws.its(ConsistencyRuleType.CONSISTENCY_RULE_TYPE).subTypes();
//        if(ruleDefinitions.isEmpty() || (ruleDefinitions.stream().filter(inst -> !inst.isDeleted).count() == 0))
//            return null;
//        for(ConsistencyRuleType crd: ruleDefinitions.stream()
//        								.filter(inst -> !inst.isDeleted)
//        								.filter(ConsistencyRuleType.class::isInstance)
//        								.map(ConsistencyRuleType.class::cast)
//        								.collect(Collectors.toSet()) ){
//            if (crd.name().equalsIgnoreCase(name) && crd.contextInstanceType().equals(context) )
//                return crd;
//        }
//        return null;
//	}
}
