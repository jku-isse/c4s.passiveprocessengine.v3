package at.jku.isse.passiveprocessengine.designspace;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import at.jku.isse.designspace.core.foundation.Value;
import at.jku.isse.designspace.core.foundation.WorkspaceListener;
import at.jku.isse.designspace.core.model.Change;
import at.jku.isse.designspace.core.model.Element;
import at.jku.isse.designspace.core.model.Instance;
import at.jku.isse.designspace.core.model.InstanceType;
import at.jku.isse.designspace.core.model.Workspace;
import at.jku.isse.designspace.core.operations.ElementOperation;
import at.jku.isse.designspace.core.operations.PropertyValueOperation;
import at.jku.isse.designspace.core.operations.WorkspaceOperation;
import at.jku.isse.designspace.core.operations.propertyvalue.AddIndexedPropertyValue;
import at.jku.isse.designspace.core.operations.propertyvalue.AddMappedPropertyValue;
import at.jku.isse.designspace.core.operations.propertyvalue.AddPropertyValue;
import at.jku.isse.designspace.core.operations.propertyvalue.RemoveIndexedPropertyValue;
import at.jku.isse.designspace.core.operations.propertyvalue.RemoveMappedPropertyValue;
import at.jku.isse.designspace.core.operations.propertyvalue.RemovePropertyValue;
import at.jku.isse.designspace.core.operations.propertyvalue.SetIndexedPropertyValue;
import at.jku.isse.designspace.core.operations.propertyvalue.SetPropertyValue;
import at.jku.isse.designspace.core.operations.workspace.WorkspaceChangeOperation;
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
		designspace.getProjectWS().addListenerForThisWorkspace(this);
	}
	
	@Override
	public void notifyWorkspaceOperation(Workspace workspace, WorkspaceOperation wsOperation) {
		if (wsOperation instanceof WorkspaceChangeOperation) {
            WorkspaceChangeOperation workspaceChangeOperation = (WorkspaceChangeOperation) wsOperation;                                               
            workspaceChangeOperation.getChanges().stream()
            	.flatMap(change -> {
            		return change.getExternalOperations().stream();
            	})
            	.filter(PropertyValueOperation.class::isInstance)
            	.map(PropertyValueOperation.class::cast)
            	.map(pUpdate -> {
            		String propName = workspace.getPropertyType(pUpdate.propertyTypeId).getName();
            		Object value = pUpdate.getValueObject(workspace);
            		Element subject = workspace.getElement(pUpdate.elementId);
            		if (pUpdate instanceof AddPropertyValue 
            				|| pUpdate instanceof AddMappedPropertyValue
            				|| pUpdate instanceof AddIndexedPropertyValue) {
            			if (value instanceof Element) {
            				return new PropertyChange.Add(propName, designspace.getWrappedInstance((Element) mapToMostSpecializedWrapper(subject)), mapToMostSpecializedWrapper((Element)value));
            			} else {
            				return new PropertyChange.Add(propName, designspace.getWrappedInstance((Element) mapToMostSpecializedWrapper(subject)), value);                			
            			}            			            			
            		}
            		else if (pUpdate instanceof RemovePropertyValue 
            				|| pUpdate instanceof RemoveMappedPropertyValue
            				|| pUpdate instanceof RemoveIndexedPropertyValue) {
            			if (value instanceof Element) {
            				return new PropertyChange.Remove(propName, designspace.getWrappedInstance((Element) mapToMostSpecializedWrapper(subject)), mapToMostSpecializedWrapper((Element)value));
            			} else {
            				return new PropertyChange.Remove(propName, designspace.getWrappedInstance((Element) mapToMostSpecializedWrapper(subject)), value);                			
            			}
            		} else if (pUpdate instanceof SetIndexedPropertyValue 
            				|| pUpdate instanceof SetPropertyValue) {
            			if (value instanceof Element) {
            				return new PropertyChange.Set(propName, designspace.getWrappedInstance((Element) mapToMostSpecializedWrapper(subject)), mapToMostSpecializedWrapper((Element)value));
            			} else {
            				return new PropertyChange.Set(propName, designspace.getWrappedInstance((Element) mapToMostSpecializedWrapper(subject)), value);                			
            			}
            		}            	
            		return null;
            	})
            	
            	.filter(Objects::nonNull)
				.collect(Collectors.toSet());
		}
	}
	
//	@Override
//	public void handleUpdated(Collection<Operation> operations) {
//		eventSink.handleUpdates(
//				operations.stream()
//				.filter(PropertyUpdate.class::isInstance)
//				.map(PropertyUpdate.class::cast)
//				.map(pUpdate -> {
//					Element el = designspace.getWorkspace().findElement(pUpdate.elementId());
//					if (el instanceof Instance) {
//						Object value = pUpdate.value();					
//						if (pUpdate instanceof PropertyUpdateAdd) {										
//							if (value instanceof Id) {										
//								return new PropertyChange.Add(pUpdate.name(), designspace.getWrappedInstance((PPEInstance)el),  mapToMostSpecializedWrapper((Id)value) );
//							} else {
//								return new PropertyChange.Add(pUpdate.name(), designspace.getWrappedInstance(el), value);
//							}
//						} else if (pUpdate instanceof PropertyUpdateRemove) {
//							if (value instanceof Id) {							
//								return new PropertyChange.Remove(pUpdate.name(), designspace.getWrappedInstance((PPEInstance)el),  mapToMostSpecializedWrapper((Id)value));
//							} else {
//								return new PropertyChange.Remove(pUpdate.name(), designspace.getWrappedInstance(el), value);
//							}
//						} else {
//							if (value instanceof Id) {							
//								return new PropertyChange.Set(pUpdate.name(), designspace.getWrappedInstance((PPEInstance)el),  mapToMostSpecializedWrapper((Id)value));
//							} else {
//								return new PropertyChange.Set(pUpdate.name(), designspace.getWrappedInstance(el), value);
//							}
//						}
//					} else
//						return null;
//				})
//				.filter(Objects::nonNull)
//				.collect(Collectors.toSet())
//		);				
//	}
//	
//	private Object mapToMostSpecializedWrapper(long id) {
//		Element el = designspace.getProjectWS().getElement(id);
//		return mapToMostSpecializedWrapper(el);
//	}

	private Object mapToMostSpecializedWrapper(Element el) {		
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
