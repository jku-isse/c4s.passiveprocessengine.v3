package at.jku.isse.passiveprocessengine.demo;

import java.util.Optional;

import at.jku.isse.designspace.core.model.Cardinality;
import at.jku.isse.designspace.core.model.Instance;
import at.jku.isse.designspace.core.model.InstanceType;
import at.jku.isse.designspace.core.model.Workspace;

public class TestArtifacts {

	public static final String DEMOISSUETYPE = "DemoIssue";
	public static enum CoreProperties { requirementIDs, state }
	public static enum JiraStates { Open, InProgress, Closed}
	
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
	
	public static void setStateToJiraInstance(Instance inst, JiraStates state) {
		inst.getProperty(CoreProperties.state.toString()).set(state.toString());
	}
	
	public static JiraStates getState(Instance inst) {
		String state= (String) inst.getPropertyAsValueOrElse(CoreProperties.state.toString(), () -> JiraStates.Open.toString());
		return JiraStates.valueOf(state);
	}
}
