package c4s.passiveprocessengine.v3;

import java.util.Optional;

import at.jku.isse.designspace.sdk.core.model.InstanceType;
import at.jku.isse.designspace.sdk.core.model.Workspace;

public class TestArtifacts {

	public static final String JIRATYPE = "JiraArtifact";
	
	public static InstanceType getJiraInstanceType(Workspace ws) {
		Optional<InstanceType> thisType = ws.debugInstanceTypes().stream()
				.filter(it -> it.name().contentEquals(JIRATYPE))
				.findAny();
			if (thisType.isPresent())
				return thisType.get();
			else {
				InstanceType typeStep = ws.createInstanceType(JIRATYPE);
				
				return typeStep;
			}
	}
}
