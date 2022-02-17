package at.jku.isse.designspace.tests.passiveprocessengine.serverside;

import java.util.Optional;

import at.jku.isse.designspace.core.model.InstanceType;
import at.jku.isse.designspace.core.model.Workspace;

public class TestArtifacts {

	public static final String JIRATYPE = "JiraArtifact";
	
	public static InstanceType getJiraInstanceType(Workspace ws) {
		Optional<InstanceType> thisType = ws.debugInstanceTypes().stream()
				.filter(it -> it.name().contentEquals(JIRATYPE))
				.findAny();
			if (thisType.isPresent())
				return thisType.get();
			else {
				InstanceType typeStep = ws.createInstanceType(JIRATYPE, ws.TYPES_FOLDER);
				
				return typeStep;
			}
	}
}
