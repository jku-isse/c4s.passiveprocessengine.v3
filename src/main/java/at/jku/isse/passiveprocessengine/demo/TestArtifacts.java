package at.jku.isse.passiveprocessengine.demo;

import java.util.Optional;

import at.jku.isse.designspace.core.model.Cardinality;
import at.jku.isse.designspace.core.model.Instance;
import at.jku.isse.designspace.core.model.InstanceType;
import at.jku.isse.designspace.core.model.Workspace;

public class TestArtifacts {

	public static final String DEMOISSUETYPE = "DemoIssue";
	public static enum CoreProperties { requirementIDs, state, requirements, parent }
	public static enum JiraStates { Open, InProgress, Closed, ReadyForReview, Released}
	
	public static InstanceType getJiraInstanceType(Workspace ws) {
		Optional<InstanceType> thisType = ws.debugInstanceTypes().stream()
				.filter(it -> it.name().contentEquals(DEMOISSUETYPE))
				.findAny();
			if (thisType.isPresent())
				return thisType.get();
			else {
				InstanceType typeJira = ws.createInstanceType(DEMOISSUETYPE, ws.TYPES_FOLDER);
				typeJira.createPropertyType(CoreProperties.requirementIDs.toString(), Cardinality.SET, Workspace.STRING);
				typeJira.createPropertyType(CoreProperties.state.toString(), Cardinality.SINGLE, Workspace.STRING);
				typeJira.createPropertyType(CoreProperties.requirements.toString(), Cardinality.SET, typeJira);
				typeJira.createPropertyType(CoreProperties.parent.toString(), Cardinality.SINGLE, typeJira);
				return typeJira;
			}
	}
	
	public static Instance getJiraInstance(Workspace ws, String name, String... reqIds) {
		Instance jira = ws.createInstance(getJiraInstanceType(ws), name);
		setStateToJiraInstance(jira, JiraStates.Open);
		for(String id : reqIds) {
			jira.getPropertyAsSet(TestArtifacts.CoreProperties.requirementIDs.toString()).add(id);
		}
		return jira;
	}
	
	public static void addJiraToJira(Instance jira, Instance jiraToAdd) {
		jira.getPropertyAsSet(TestArtifacts.CoreProperties.requirements.toString()).add(jiraToAdd);
	}
	
	public static void removeJiraFromJira(Instance jira, Instance jiraToRemove) {
		jira.getPropertyAsSet(TestArtifacts.CoreProperties.requirements.toString()).remove(jiraToRemove);
	}
	
	public static void setStateToJiraInstance(Instance inst, JiraStates state) {
		inst.getProperty(CoreProperties.state.toString()).set(state.toString());
	}
	
	public static void addParentToJira(Instance inst, Instance parent) {
		inst.getProperty(CoreProperties.parent.toString()).set(parent);
	}
	
	public static JiraStates getState(Instance inst) {
		String state= (String) inst.getPropertyAsValueOrElse(CoreProperties.state.toString(), () -> JiraStates.Open.toString());
		return JiraStates.valueOf(state);
	}
	
	public static InstanceType getDemoGitIssueType(Workspace ws) {
		InstanceType typeGitDemo = ws.createInstanceType("git_issue", ws.TYPES_FOLDER);
		typeGitDemo.createPropertyType("linkedIssues", Cardinality.SET, typeGitDemo);
		typeGitDemo.createPropertyType("labels", Cardinality.SET, Workspace.STRING);
		typeGitDemo.createPropertyType("state", Cardinality.SINGLE, Workspace.STRING);
		typeGitDemo.createPropertyType("title", Cardinality.SINGLE, Workspace.STRING);
		return typeGitDemo;
	}
	
	public static InstanceType getTestAzureIssueType(Workspace ws) {
		InstanceType typeAzureTest = ws.createInstanceType("azure_workitem", ws.TYPES_FOLDER);
		InstanceType typeAzureStateTest = ws.createInstanceType("azure_workitemstate", ws.TYPES_FOLDER);
		InstanceType typeAzureTypeTest = ws.createInstanceType("azure_workitemstate", ws.TYPES_FOLDER);
		InstanceType typeAzureLinkTypeTest = ws.createInstanceType("workitem_link", ws.TYPES_FOLDER);
		
		typeAzureTest.createPropertyType("relatedItems", Cardinality.SET, typeAzureLinkTypeTest);
		typeAzureTest.createPropertyType("state", Cardinality.SINGLE, typeAzureStateTest);
		typeAzureTest.createPropertyType("workItemType", Cardinality.SINGLE, typeAzureTypeTest);
		
		typeAzureLinkTypeTest.createPropertyType("linkTo", Cardinality.SINGLE, typeAzureTest);
		typeAzureLinkTypeTest.createPropertyType("linkType", Cardinality.SINGLE, typeAzureTypeTest);
		
		
		return typeAzureTest;
	}
}
