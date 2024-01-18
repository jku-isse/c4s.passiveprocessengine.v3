package at.jku.isse.passiveprocessengine.designspace;

import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;

import at.jku.isse.designspace.core.events.Operation;
import at.jku.isse.designspace.core.model.Element;
import at.jku.isse.designspace.core.model.Instance;
import at.jku.isse.designspace.core.model.WorkspaceListener;
import at.jku.isse.passiveprocessengine.core.PropertyChange;
import at.jku.isse.passiveprocessengine.instance.ProcessInstanceChangeProcessor;
import lombok.NonNull;
import at.jku.isse.designspace.core.events.PropertyUpdate;
import at.jku.isse.designspace.core.events.PropertyUpdateAdd;
import at.jku.isse.designspace.core.events.PropertyUpdateRemove;

public class WorkspaceListenerWrapper implements WorkspaceListener{

	final DesignSpaceSchemaRegistry designspace;
	final ProcessInstanceChangeProcessor eventSink;
	
	public WorkspaceListenerWrapper(DesignSpaceSchemaRegistry designspace, @NonNull ProcessInstanceChangeProcessor eventSink) {
		this.designspace = designspace;
		this.eventSink = eventSink;		
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
							if (value instanceof Instance) {							
								return new PropertyChange.Add(pUpdate.name(), designspace.getWrappedInstance((Instance)el),  designspace.getWrappedInstance((Instance) value));
							} else {
								return new PropertyChange.Add(pUpdate.name(), designspace.getWrappedInstance(el), value);
							}
						} else if (pUpdate instanceof PropertyUpdateRemove) {
							if (value instanceof Instance) {							
								return new PropertyChange.Remove(pUpdate.name(), designspace.getWrappedInstance((Instance)el),  designspace.getWrappedInstance((Instance) value));
							} else {
								return new PropertyChange.Remove(pUpdate.name(), designspace.getWrappedInstance(el), value);
							}
						} else {
							if (value instanceof Instance) {							
								return new PropertyChange.Set(pUpdate.name(), designspace.getWrappedInstance((Instance)el),  designspace.getWrappedInstance((Instance) value));
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

}
