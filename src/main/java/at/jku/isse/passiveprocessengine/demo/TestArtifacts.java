package at.jku.isse.passiveprocessengine.demo;

import java.util.Optional;
import java.util.Set;

import at.jku.isse.passiveprocessengine.core.BuildInType;
import at.jku.isse.passiveprocessengine.core.Instance;
import at.jku.isse.passiveprocessengine.core.InstanceRepository;
import at.jku.isse.passiveprocessengine.core.InstanceType;
import at.jku.isse.passiveprocessengine.core.SchemaRegistry;

public class TestArtifacts {

	public static final String DEMOISSUETYPE = "DemoIssue";
	public static enum CoreProperties { state, requirements, bugs, parent, html_url, upstream, downstream }
	public static enum JiraStates { Open, InProgress, Closed, ReadyForReview, Released}

	InstanceRepository repository;
	SchemaRegistry schemaRegistry;
	
	public TestArtifacts(InstanceRepository repository, SchemaRegistry schemaRegistry) {
		this.repository = repository;
		this.schemaRegistry = schemaRegistry;		
	}
	
	public InstanceType getJiraInstanceType() {
		Optional<InstanceType> thisType = schemaRegistry.findNonDeletedInstanceTypeById(DEMOISSUETYPE);
			if (thisType.isPresent())
				return thisType.get();
			else {
				InstanceType typeJira = schemaRegistry.createNewInstanceType(DEMOISSUETYPE);				
				typeJira.createSinglePropertyType(CoreProperties.state.toString(), BuildInType.STRING);
				typeJira.createSetPropertyType(CoreProperties.requirements.toString(), typeJira);
				typeJira.createSetPropertyType(CoreProperties.bugs.toString(),  typeJira);
				typeJira.createSinglePropertyType(CoreProperties.parent.toString(),  typeJira);
				typeJira.createSetPropertyType(CoreProperties.upstream.toString(),  typeJira);
				typeJira.createSetPropertyType(CoreProperties.downstream.toString(),  typeJira);
				//typeJira.createOpposablePropertyType(CoreProperties.upstream.toString(), Cardinality.SET, typeJira, CoreProperties.downstream.toString(), Cardinality.SET);				
				typeJira.createSinglePropertyType(CoreProperties.html_url.toString(), BuildInType.STRING);
				return typeJira;
			}
	}

	public Instance getJiraInstance(String name, Instance... reqs) {
		Instance jira = repository.createInstance(name, getJiraInstanceType());
		jira.setSingleProperty(CoreProperties.html_url.toString(),"http://localhost:7171/home");
		setStateToJiraInstance(jira, JiraStates.Open);
		for(Instance inst : reqs) {
			jira.getTypedProperty(TestArtifacts.CoreProperties.requirements.toString(), Set.class).add(inst);
		}
		return jira;
	}

	public void addReqsToJira(Instance jira, Instance... reqs) {
		for(Instance inst : reqs) {
			jira.getTypedProperty(TestArtifacts.CoreProperties.requirements.toString(), Set.class).add(inst);
		}
	}

	public void addJiraToRequirements(Instance issue, Instance reqToAdd) {
		issue.getTypedProperty(TestArtifacts.CoreProperties.requirements.toString(), Set.class).add(reqToAdd);
	}
	
	public void removeJiraFromReqs(Instance jira, Instance reqToRemove) {
		jira.getTypedProperty(TestArtifacts.CoreProperties.requirements.toString(), Set.class).remove(reqToRemove);
	}

	public void setStateToJiraInstance(Instance inst, JiraStates state) {
		inst.setSingleProperty(CoreProperties.state.toString(), state.toString());
	}

	public void addJiraToJiraBug(Instance jira, Instance bugToAdd) {
		jira.getTypedProperty(TestArtifacts.CoreProperties.bugs.toString(), Set.class).add(bugToAdd);
	}

	public void removeJiraFromJiraBug(Instance jira, Instance bugToRemove) {
		jira.getTypedProperty(TestArtifacts.CoreProperties.bugs.toString(), Set.class).remove(bugToRemove);
	}

	public void addParentToJira(Instance inst, Instance parent) {
		inst.setSingleProperty(CoreProperties.parent.toString(),parent);
	}

	public void addUpstream(Instance inst, Instance toAdd) {
		inst.getTypedProperty(TestArtifacts.CoreProperties.upstream.toString(), Set.class).add(toAdd);
	}

	public void addDownstream(Instance inst, Instance toAdd) {
		inst.getTypedProperty(TestArtifacts.CoreProperties.downstream.toString(), Set.class).add(toAdd);
	}

	public void removeUpstream(Instance inst, Instance toRemove) {
		inst.getTypedProperty(TestArtifacts.CoreProperties.upstream.toString(), Set.class).remove(toRemove);
	}

	public void removeDownstream(Instance inst, Instance toRemove) {
		inst.getTypedProperty(TestArtifacts.CoreProperties.downstream.toString(), Set.class).remove(toRemove);
	}

	public static JiraStates getState(Instance inst) {
		String state= (String) inst.getTypedProperty(CoreProperties.state.toString(), String.class, JiraStates.Open.toString());
		return JiraStates.valueOf(state);
	}

	public InstanceType getDemoGitIssueType() {
		InstanceType typeGitDemo = schemaRegistry.createNewInstanceType("git_issue");
		typeGitDemo.createSetPropertyType("linkedIssues", typeGitDemo);
		typeGitDemo.createSetPropertyType("labels", BuildInType.STRING);
		typeGitDemo.createSinglePropertyType("state", BuildInType.STRING);
		typeGitDemo.createSinglePropertyType("title",  BuildInType.STRING);
		return typeGitDemo;
	}

	public InstanceType getTestAzureIssueType() {
		InstanceType typeAzureTest = schemaRegistry.createNewInstanceType("azure_workitem");
		InstanceType typeAzureStateTest = schemaRegistry.createNewInstanceType("azure_workitemstate");
		InstanceType typeAzureTypeTest = schemaRegistry.createNewInstanceType("azure_workitemtype");
		InstanceType typeAzureLinkTypeTest = schemaRegistry.createNewInstanceType("workitem_link");

		typeAzureTest.createSetPropertyType("relatedItems", typeAzureLinkTypeTest);
		typeAzureTest.createSinglePropertyType("state", typeAzureStateTest);
		typeAzureTest.createSinglePropertyType("workItemType", typeAzureTypeTest);

		typeAzureLinkTypeTest.createSinglePropertyType("linkTo", typeAzureTest);
		typeAzureLinkTypeTest.createSinglePropertyType("linkType", typeAzureTypeTest);


		return typeAzureTest;
	}
}
