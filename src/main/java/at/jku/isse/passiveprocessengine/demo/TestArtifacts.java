package at.jku.isse.passiveprocessengine.demo;

import java.util.Optional;
import java.util.Set;


import at.jku.isse.passiveprocessengine.rdfwrapper.RDFInstance;
import at.jku.isse.passiveprocessengine.rdfwrapper.RDFInstanceType;
import at.jku.isse.passiveprocessengine.rdfwrapper.CoreTypeFactory;
import at.jku.isse.passiveprocessengine.rdfwrapper.NodeToDomainResolver;

public class TestArtifacts {

	static final String NS = "http://isse.jku.at/demo#";
	public static final String DEMOISSUETYPE = NS+"DemoIssue";
	public enum CoreProperties { state, requirements, bugs, parent, html_url, upstream, downstream		
		;
		
		@Override
		public String toString() {
			return NS+name();
		}
		
		public String getURI() {
			return NS+name();
		}
	}
	public enum JiraStates { Open, InProgress, Closed, ReadyForReview, Released}

	NodeToDomainResolver schemaRegistry;
	
	public TestArtifacts( NodeToDomainResolver schemaRegistry) {
		this.schemaRegistry = schemaRegistry;		
	}
	
	public RDFInstanceType getJiraInstanceType() {
		Optional<RDFInstanceType> thisType = schemaRegistry.findNonDeletedInstanceTypeByFQN(DEMOISSUETYPE);
			if (thisType.isPresent())
				return thisType.get();
			else {
				RDFInstanceType typeJira = schemaRegistry.createNewInstanceType(DEMOISSUETYPE, schemaRegistry.findNonDeletedInstanceTypeByFQN(CoreTypeFactory.BASE_TYPE_URI).orElse(null));				
				typeJira.createSinglePropertyType(CoreProperties.state.toString(), schemaRegistry.getMetaschemata().getPrimitiveTypesFactory().getStringType());
				typeJira.createSetPropertyType(CoreProperties.requirements.toString(), typeJira.getAsPropertyType());
				typeJira.createSetPropertyType(CoreProperties.bugs.toString(),  typeJira.getAsPropertyType());
				typeJira.createSinglePropertyType(CoreProperties.parent.toString(),  typeJira.getAsPropertyType());
				typeJira.createSetPropertyType(CoreProperties.upstream.toString(),  typeJira.getAsPropertyType());
				typeJira.createSetPropertyType(CoreProperties.downstream.toString(),  typeJira.getAsPropertyType());
				return typeJira;
			}
	}

	public RDFInstance getJiraInstance(String name, RDFInstance... reqs) {
		// clear any preexisting ones
		var uri = NodeToDomainResolver.BASE_NS+name;
		var instOpt = schemaRegistry.findInstanceById(uri);
		if (instOpt.isPresent()) {
			instOpt.get().delete();
		}
		
		RDFInstance jira = schemaRegistry.createInstance(name, getJiraInstanceType());
		jira.setSingleProperty(CoreTypeFactory.URL_URI,"http://localhost:7171/home");
		jira.setSingleProperty(CoreTypeFactory.EXTERNAL_TYPE_URI,"none");
		jira.setSingleProperty(CoreTypeFactory.EXTERNAL_DEFAULT_ID_URI, name);
		jira.setSingleProperty(RDFInstanceType.propertyIsFullyFetchedPredicate, true);
		setStateToJiraInstance(jira, JiraStates.Open);
		for(RDFInstance inst : reqs) {
			jira.getTypedProperty(TestArtifacts.CoreProperties.requirements.toString(), Set.class).add(inst);
		}
		return jira;
	}

	public void addReqsToJira(RDFInstance jira, RDFInstance... reqs) {
		for(RDFInstance inst : reqs) {
			jira.getTypedProperty(TestArtifacts.CoreProperties.requirements.toString(), Set.class).add(inst);
		}
	}

	public void addJiraToRequirements(RDFInstance issue, RDFInstance reqToAdd) {
		issue.getTypedProperty(TestArtifacts.CoreProperties.requirements.toString(), Set.class).add(reqToAdd);
	}
	
	public void removeJiraFromReqs(RDFInstance jira, RDFInstance reqToRemove) {
		jira.getTypedProperty(TestArtifacts.CoreProperties.requirements.toString(), Set.class).remove(reqToRemove);
	}

	public void setStateToJiraInstance(RDFInstance inst, JiraStates state) {
		inst.setSingleProperty(CoreProperties.state.toString(), state.toString());
	}

	public void addJiraToJiraBug(RDFInstance jira, RDFInstance bugToAdd) {
		jira.getTypedProperty(TestArtifacts.CoreProperties.bugs.toString(), Set.class).add(bugToAdd);
	}

	public void removeJiraFromJiraBug(RDFInstance jira, RDFInstance bugToRemove) {
		jira.getTypedProperty(TestArtifacts.CoreProperties.bugs.toString(), Set.class).remove(bugToRemove);
	}

	public void addParentToJira(RDFInstance inst, RDFInstance parent) {
		inst.setSingleProperty(CoreProperties.parent.toString(),parent);
	}

	public void addUpstream(RDFInstance inst, RDFInstance toAdd) {
		inst.getTypedProperty(TestArtifacts.CoreProperties.upstream.toString(), Set.class).add(toAdd);
	}

	public void addDownstream(RDFInstance inst, RDFInstance toAdd) {
		inst.getTypedProperty(TestArtifacts.CoreProperties.downstream.toString(), Set.class).add(toAdd);
	}

	public void removeUpstream(RDFInstance inst, RDFInstance toRemove) {
		inst.getTypedProperty(TestArtifacts.CoreProperties.upstream.toString(), Set.class).remove(toRemove);
	}

	public void removeDownstream(RDFInstance inst, RDFInstance toRemove) {
		inst.getTypedProperty(TestArtifacts.CoreProperties.downstream.toString(), Set.class).remove(toRemove);
	}

	public static JiraStates getState(RDFInstance inst) {
		String state= (String) inst.getTypedProperty(CoreProperties.state.toString(), String.class, JiraStates.Open.toString());
		return JiraStates.valueOf(state);
	}

	public static String printProperties(RDFInstance jira) {
		RDFInstance parent = jira.getTypedProperty(TestArtifacts.CoreProperties.parent.toString(), RDFInstance.class);				
		String state = jira.getTypedProperty(TestArtifacts.CoreProperties.state.toString(), String.class);
		Set<RDFInstance> requirements = jira.getTypedProperty(TestArtifacts.CoreProperties.requirements.toString(), Set.class);
		
		StringBuffer sb = new StringBuffer("Issue:"+jira.getName()+"::"+getState(jira)+"\r\n");
		if (parent != null)
			sb.append("  Parent: "+parent.getName()+"::"+getState(parent)+"\r\n");
		requirements.stream().forEach(req -> sb.append("  Req: "+req.getName()+"::"+getState(req)+"\r\n"));
		return sb.toString();
	}
	
	public RDFInstanceType getDemoGitIssueType() {
		RDFInstanceType typeGitDemo = schemaRegistry.createNewInstanceType("git_issue");
		typeGitDemo.createSetPropertyType("linkedIssues", typeGitDemo.getAsPropertyType());
		typeGitDemo.createSetPropertyType("labels", schemaRegistry.getMetaschemata().getPrimitiveTypesFactory().getStringType());
		typeGitDemo.createSinglePropertyType("state", schemaRegistry.getMetaschemata().getPrimitiveTypesFactory().getStringType());
		typeGitDemo.createSinglePropertyType("title", schemaRegistry.getMetaschemata().getPrimitiveTypesFactory().getStringType());
		return typeGitDemo;
	}

	public RDFInstanceType getTestAzureIssueType() {
		RDFInstanceType typeAzureTest = schemaRegistry.createNewInstanceType("azure_workitem");
		RDFInstanceType typeAzureStateTest = schemaRegistry.createNewInstanceType("azure_workitemstate");
		RDFInstanceType typeAzureTypeTest = schemaRegistry.createNewInstanceType("azure_workitemtype");
		RDFInstanceType typeAzureLinkTypeTest = schemaRegistry.createNewInstanceType("workitem_link");

		typeAzureTest.createSetPropertyType("relatedItems", typeAzureLinkTypeTest.getAsPropertyType());
		typeAzureTest.createSinglePropertyType("state", typeAzureStateTest.getAsPropertyType());
		typeAzureTest.createSinglePropertyType("workItemType", typeAzureTypeTest.getAsPropertyType());

		typeAzureLinkTypeTest.createSinglePropertyType("linkTo", typeAzureTest.getAsPropertyType());
		typeAzureLinkTypeTest.createSinglePropertyType("linkType", typeAzureTypeTest.getAsPropertyType());


		return typeAzureTest;
	}
}
