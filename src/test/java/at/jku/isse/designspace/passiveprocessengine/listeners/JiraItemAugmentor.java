package at.jku.isse.designspace.passiveprocessengine.listeners;

import java.util.Collection;

import at.jku.isse.designspace.core.events.ElementCreate;
import at.jku.isse.designspace.core.events.PropertyUpdateSet;
import at.jku.isse.designspace.core.foundation.WorkspaceListener;
import at.jku.isse.designspace.core.model.Instance;
import at.jku.isse.designspace.core.model.InstanceType;
import at.jku.isse.designspace.core.model.ReservedNames;
import at.jku.isse.designspace.core.model.Workspace;
import at.jku.isse.designspace.core.service.WorkspaceService;
import at.jku.isse.passiveprocessengine.demo.TestArtifacts;



public class JiraItemAugmentor implements WorkspaceListener {

	public static final String JIRA2JAMALINKPROPERTYNAME = "jamaItem";
	public static final String JAMA2JIRALINKPROPERTYNAME = "jiraIssue";
	public static final String JIRA2JAMAIDPROPERTYNAME = "jamaId";
	
	private Workspace ws;
	
	private InstanceType jiraConcreteType; // not the base type as we need to extend the one that contains the cross tool link to jama
	private InstanceType jamaBaseType; // as we dont know what type is linked to from Jira, we link to base jama type
	
	private boolean isSchemaUpdated = false;	
	
	
	public JiraItemAugmentor(Workspace ws) {
		this.ws = ws;
		init();
	}
	
	
	private void init() {
		//jamaBaseType = ws.debugInstanceTypeFindByName("jamaItem");
		jamaBaseType = TestArtifacts.getJiraInstanceType(ws); //TODO: replace with lookup
		
		// after a reboot, the types with augmentation might already exist,
		InstanceType parentType = ws.debugInstanceTypeFindByName("ProcessDefinitionScopedElement");
		if (parentType != null && !parentType.subTypes().isEmpty()) {
			jiraConcreteType = parentType.subTypes().iterator().next();
			// check if has property
			if (jiraConcreteType.hasProperty(ReservedNames.PROPERTY_DEFINITION_PREFIX+JIRA2JAMALINKPROPERTYNAME)) {		
				isSchemaUpdated = true;
			}
		}
	}
	
	@Override
	public void handleUpdated(Collection<Operation> arg0) {
	
		arg0.stream().forEach(op -> {
			if (op instanceof ElementCreate && !isSchemaUpdated) {
				handleElementCreate((ElementCreate) op);
			} else if (op instanceof PropertyUpdateSet && isSchemaUpdated) {
				handlePropertyUpdateSet((PropertyUpdateSet) op);
			}
		});				
	}

	private void handleElementCreate(ElementCreate op) {
		// we need to update the schema with additional property or type JamaItem, 
		// everything hardcoded for now
  	    // also this is done only at the beginning when hopefully no jira instance yet exist (should be save to assume a this code is called only upon property creation)		
		if (op.instanceTypeId().value()==2l) {
			InstanceType instType = (InstanceType)ws.findElement(op.elementId());
			// look for property created events, within an instance type that has JiraBase as the parent
			if (isSubclassOfJira(instType)) {
				// obtain jira schema
				jiraConcreteType = instType;											
				// check if has cross link property, if not
				if (!jiraConcreteType.hasProperty(ReservedNames.PROPERTY_DEFINITION_PREFIX+JIRA2JAMALINKPROPERTYNAME)) {				
					// create cross link property
					WorkspaceService.createOpposablePropertyType(ws, jiraConcreteType, JIRA2JAMALINKPROPERTYNAME, Cardinality.SINGLE, jamaBaseType, JAMA2JIRALINKPROPERTYNAME, Cardinality.SINGLE);		
					Workspace.logger.info("Jira2Jama Bridge: augmented Jira Subclass "+jiraConcreteType.name());
					ws.concludeTransaction();
				} 
					// and set flag to no longer check for property creations
					isSchemaUpdated = true;
							
			}
		}	
	}	
	
	private boolean isSubclassOfJira(InstanceType instType) {
		return instType.getAllSuperTypes().stream()
		.map(superT -> superT.name())
		.anyMatch(superName -> superName.equalsIgnoreCase("ProcessDefinitionScopedElement"));
	}	
	
	private void handlePropertyUpdateSet(PropertyUpdateSet op) {
		if (op.name().equalsIgnoreCase(JIRA2JAMAIDPROPERTYNAME)) {
			Instance inst = ws.findElement(op.elementId());
			if (inst.hasProperty(JIRA2JAMALINKPROPERTYNAME)) {
				setJiraToJamaCrossLink(inst, op.value());
			}
		}
	}
	
	private void setJiraToJamaCrossLink(Instance jiraItem, Object jamaKey) {
		if (jamaKey == null || jamaKey.toString().length() == 0) {
			// remove
			jiraItem.getProperty(JIRA2JAMALINKPROPERTYNAME).set(null);
		} else {			
			// TODO and resolve it to a jama item
			Instance jamaInst = null;				
			// and set it to the link property
			try {
				jiraItem.getProperty(JIRA2JAMALINKPROPERTYNAME).set(jamaInst);
			} catch (IllegalArgumentException ie) {
                Workspace.logger.debug("Jira2Jama Bridge: " + jamaKey + " could not be translated and assigne to crosslink "+ie.getMessage());
            }
		}
	}
	
}
