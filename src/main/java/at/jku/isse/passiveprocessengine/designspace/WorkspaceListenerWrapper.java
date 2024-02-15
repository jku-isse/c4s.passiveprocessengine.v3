package at.jku.isse.passiveprocessengine.designspace;

import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;

import at.jku.isse.designspace.core.events.PropertyUpdate;
import at.jku.isse.designspace.core.events.PropertyUpdateAdd;
import at.jku.isse.designspace.core.events.PropertyUpdateRemove;
import at.jku.isse.designspace.core.foundation.WorkspaceListener;
import at.jku.isse.designspace.core.model.Element;
import at.jku.isse.designspace.core.model.Instance;
import at.jku.isse.designspace.core.model.InstanceType;
import at.jku.isse.designspace.rule.model.ConsistencyRule;
import at.jku.isse.passiveprocessengine.core.PropertyChange;
import at.jku.isse.passiveprocessengine.instance.ProcessInstanceChangeProcessor;
import lombok.NonNull;

public class WorkspaceListenerWrapper implements WorkspaceListener{

	final DesignSpaceSchemaRegistry designspace;
	final ProcessInstanceChangeProcessor eventSink;
	
	public WorkspaceListenerWrapper(DesignSpaceSchemaRegistry designspace, @NonNull ProcessInstanceChangeProcessor eventSink) {
		this.designspace = designspace;
		this.eventSink = eventSink;		
		
	}
	
	public void registerWithWorkspace() {
		designspace.getWorkspace().workspaceListeners.add(this);
	}
	
	@Override
	public void handleUpdated(Collection<Operation> operations) {
		eventSink.handleUpdates(
				operations.stream()
				.filter(PropertyUpdate.class::isInstance)
				.map(PropertyUpdate.class::cast)
				.map(pUpdate -> {
					Element el = designspace.getWorkspace().findElement(pUpdate.elementId());
					if (el instanceof Instance) {
						Object value = pUpdate.value();					
						if (pUpdate instanceof PropertyUpdateAdd) {										
							if (value instanceof Id) {										
								return new PropertyChange.Add(pUpdate.name(), designspace.getWrappedInstance((Instance)el),  mapToMostSpecializedWrapper((Id)value) );
							} else {
								return new PropertyChange.Add(pUpdate.name(), designspace.getWrappedInstance(el), value);
							}
						} else if (pUpdate instanceof PropertyUpdateRemove) {
							if (value instanceof Id) {							
								return new PropertyChange.Remove(pUpdate.name(), designspace.getWrappedInstance((Instance)el),  mapToMostSpecializedWrapper((Id)value));
							} else {
								return new PropertyChange.Remove(pUpdate.name(), designspace.getWrappedInstance(el), value);
							}
						} else {
							if (value instanceof Id) {							
								return new PropertyChange.Set(pUpdate.name(), designspace.getWrappedInstance((Instance)el),  mapToMostSpecializedWrapper((Id)value));
							} else {
								return new PropertyChange.Set(pUpdate.name(), designspace.getWrappedInstance(el), value);
							}
						}
					} else
						return null;
				})
				.filter(Objects::nonNull)
				.collect(Collectors.toSet())
		);				
	}
	
	private Object mapToMostSpecializedWrapper(Id id) {
		Element el = designspace.getWorkspace().findElement(id);
		if (el instanceof ConsistencyRule )
			return designspace.getWrappedRuleResult( (ConsistencyRule)el);			
		else if (el instanceof Instance) 
			return designspace.getWrappedInstance( (Instance)el);
		else if (el instanceof InstanceType)
			return designspace.getWrappedType( (InstanceType)el);
		else
			return null;
	}
	
}
