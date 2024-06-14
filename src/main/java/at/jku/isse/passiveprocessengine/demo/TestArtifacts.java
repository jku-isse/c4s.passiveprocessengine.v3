package at.jku.isse.passiveprocessengine.demo;

import java.util.Optional;
import java.util.Set;

import at.jku.isse.designspace.artifactconnector.core.repository.CoreTypeFactory;
import at.jku.isse.passiveprocessengine.core.BuildInType;
import at.jku.isse.passiveprocessengine.core.PPEInstance;
import at.jku.isse.passiveprocessengine.core.InstanceRepository;
import at.jku.isse.passiveprocessengine.core.PPEInstanceType;
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
	
	public PPEInstanceType getJiraInstanceType() {
		Optional<PPEInstanceType> thisType = schemaRegistry.findNonDeletedInstanceTypeByFQN(DEMOISSUETYPE);
			if (thisType.isPresent())
				return thisType.get();
			else {
				PPEInstanceType typeJira = schemaRegistry.createNewInstanceType(DEMOISSUETYPE, schemaRegistry.getTypeByName(CoreTypeFactory.BASE_TYPE_NAME));				
				schemaRegistry.registerTypeByName(typeJira);
				typeJira.createSinglePropertyType(CoreProperties.state.toString(), BuildInType.STRING);
				typeJira.createSetPropertyType(CoreProperties.requirements.toString(), typeJira);
				typeJira.createSetPropertyType(CoreProperties.bugs.toString(),  typeJira);
				typeJira.createSinglePropertyType(CoreProperties.parent.toString(),  typeJira);
				typeJira.createSetPropertyType(CoreProperties.upstream.toString(),  typeJira);
				typeJira.createSetPropertyType(CoreProperties.downstream.toString(),  typeJira);
				//typeJira.createOpposablePropertyType(CoreProperties.upstream.toString(), Cardinality.SET, typeJira, CoreProperties.downstream.toString(), Cardinality.SET);				
				//typeJira.createSinglePropertyType(CoreProperties.html_url.toString(), BuildInType.STRING);
				return typeJira;
			}
	}

	public PPEInstance getJiraInstance(String name, PPEInstance... reqs) {
		PPEInstance jira = repository.createInstance(name, getJiraInstanceType());
		jira.setSingleProperty(CoreTypeFactory.URL.toString(),"http://localhost:7171/home");
		jira.setSingleProperty(CoreTypeFactory.EXTERNAL_TYPE.toString(),"none");
		jira.setSingleProperty(CoreTypeFactory.EXTERNAL_DEFAULT_ID.toString(), name);
		setStateToJiraInstance(jira, JiraStates.Open);
		for(PPEInstance inst : reqs) {
			jira.getTypedProperty(TestArtifacts.CoreProperties.requirements.toString(), Set.class).add(inst);
		}
		return jira;
	}

	public void addReqsToJira(PPEInstance jira, PPEInstance... reqs) {
		for(PPEInstance inst : reqs) {
			jira.getTypedProperty(TestArtifacts.CoreProperties.requirements.toString(), Set.class).add(inst);
		}
	}

	public void addJiraToRequirements(PPEInstance issue, PPEInstance reqToAdd) {
		issue.getTypedProperty(TestArtifacts.CoreProperties.requirements.toString(), Set.class).add(reqToAdd);
	}
	
	public void removeJiraFromReqs(PPEInstance jira, PPEInstance reqToRemove) {
		jira.getTypedProperty(TestArtifacts.CoreProperties.requirements.toString(), Set.class).remove(reqToRemove);
	}

	public void setStateToJiraInstance(PPEInstance inst, JiraStates state) {
		inst.setSingleProperty(CoreProperties.state.toString(), state.toString());
	}

	public void addJiraToJiraBug(PPEInstance jira, PPEInstance bugToAdd) {
		jira.getTypedProperty(TestArtifacts.CoreProperties.bugs.toString(), Set.class).add(bugToAdd);
	}

	public void removeJiraFromJiraBug(PPEInstance jira, PPEInstance bugToRemove) {
		jira.getTypedProperty(TestArtifacts.CoreProperties.bugs.toString(), Set.class).remove(bugToRemove);
	}

	public void addParentToJira(PPEInstance inst, PPEInstance parent) {
		inst.setSingleProperty(CoreProperties.parent.toString(),parent);
	}

	public void addUpstream(PPEInstance inst, PPEInstance toAdd) {
		inst.getTypedProperty(TestArtifacts.CoreProperties.upstream.toString(), Set.class).add(toAdd);
	}

	public void addDownstream(PPEInstance inst, PPEInstance toAdd) {
		inst.getTypedProperty(TestArtifacts.CoreProperties.downstream.toString(), Set.class).add(toAdd);
	}

	public void removeUpstream(PPEInstance inst, PPEInstance toRemove) {
		inst.getTypedProperty(TestArtifacts.CoreProperties.upstream.toString(), Set.class).remove(toRemove);
	}

	public void removeDownstream(PPEInstance inst, PPEInstance toRemove) {
		inst.getTypedProperty(TestArtifacts.CoreProperties.downstream.toString(), Set.class).remove(toRemove);
	}

	public static JiraStates getState(PPEInstance inst) {
		String state= (String) inst.getTypedProperty(CoreProperties.state.toString(), String.class, JiraStates.Open.toString());
		return JiraStates.valueOf(state);
	}

	public static String printProperties(PPEInstance jira) {
		PPEInstance parent = jira.getTypedProperty(TestArtifacts.CoreProperties.parent.toString(), PPEInstance.class);				
		String state = jira.getTypedProperty(TestArtifacts.CoreProperties.state.toString(), String.class);
		Set<PPEInstance> requirements = jira.getTypedProperty(TestArtifacts.CoreProperties.requirements.toString(), Set.class);
		
		StringBuffer sb = new StringBuffer("Issue:"+jira.getName()+"::"+getState(jira)+"\r\n");
		if (parent != null)
			sb.append("  Parent: "+parent.getName()+"::"+getState(parent)+"\r\n");
		requirements.stream().forEach(req -> sb.append("  Req: "+req.getName()+"::"+getState(req)+"\r\n"));
		return sb.toString();
	}
	
	public PPEInstanceType getDemoGitIssueType() {
		PPEInstanceType typeGitDemo = schemaRegistry.createNewInstanceType("git_issue");
		typeGitDemo.createSetPropertyType("linkedIssues", typeGitDemo);
		typeGitDemo.createSetPropertyType("labels", BuildInType.STRING);
		typeGitDemo.createSinglePropertyType("state", BuildInType.STRING);
		typeGitDemo.createSinglePropertyType("title",  BuildInType.STRING);
		return typeGitDemo;
	}

	public PPEInstanceType getTestAzureIssueType() {
		PPEInstanceType typeAzureTest = schemaRegistry.createNewInstanceType("azure_workitem");
		PPEInstanceType typeAzureStateTest = schemaRegistry.createNewInstanceType("azure_workitemstate");
		PPEInstanceType typeAzureTypeTest = schemaRegistry.createNewInstanceType("azure_workitemtype");
		PPEInstanceType typeAzureLinkTypeTest = schemaRegistry.createNewInstanceType("workitem_link");

		typeAzureTest.createSetPropertyType("relatedItems", typeAzureLinkTypeTest);
		typeAzureTest.createSinglePropertyType("state", typeAzureStateTest);
		typeAzureTest.createSinglePropertyType("workItemType", typeAzureTypeTest);

		typeAzureLinkTypeTest.createSinglePropertyType("linkTo", typeAzureTest);
		typeAzureLinkTypeTest.createSinglePropertyType("linkType", typeAzureTypeTest);


		return typeAzureTest;
	}
}
