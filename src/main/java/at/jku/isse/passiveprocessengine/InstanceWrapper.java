package at.jku.isse.passiveprocessengine;

import java.util.Collection;
import java.util.stream.Collectors;

import at.jku.isse.designspace.core.model.Instance;
import at.jku.isse.designspace.core.model.InstanceType;
import at.jku.isse.designspace.core.model.Workspace;
import at.jku.isse.designspace.rule.model.ConsistencyRuleType;
import at.jku.isse.passiveprocessengine.configurability.ProcessConfigBaseElementFactory;

public abstract class InstanceWrapper implements IdentifiableElement{
	
	protected transient Instance instance;
	protected transient Workspace ws;
	
	public InstanceWrapper(Instance instance) {
	//	assert instance != null;
		this.instance = instance;
	}
	
	public Instance getInstance() {
		return instance;
	}
	
	public String getId() {
		return instance.id().toString();
	}
	
	@Override
	public String getName() {
		return instance.name();
	}

	public void deleteCascading() {
		WrapperCache.removeWrapper(getInstance().id());
		instance.delete();				
	}
	
	public void deleteCascading(ProcessConfigBaseElementFactory configFactory) {
		WrapperCache.removeWrapper(getInstance().id());
		instance.delete();
	}
	
	protected ConsistencyRuleType getRuleByNameAndContext(String name, InstanceType context) {
		Collection<InstanceType> ruleDefinitions = ws.its(ConsistencyRuleType.CONSISTENCY_RULE_TYPE).subTypes();
        if(ruleDefinitions.isEmpty())
            return null;
        if(ruleDefinitions.stream().filter(inst -> !inst.isDeleted).count() == 0)
            return null;
        for(ConsistencyRuleType crd: ruleDefinitions.stream()
        								.filter(inst -> !inst.isDeleted)
        								.filter(ConsistencyRuleType.class::isInstance)
        								.map(ConsistencyRuleType.class::cast)
        								.collect(Collectors.toSet()) ){            
            if (crd.name().equalsIgnoreCase(name) && crd.contextInstanceType().equals(context) )
                return crd;
        }
        return null;
	}
}
