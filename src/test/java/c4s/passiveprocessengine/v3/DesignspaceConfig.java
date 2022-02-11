package c4s.passiveprocessengine.v3;

import java.util.Optional;

import at.jku.isse.designspace.sdk.core.DesignSpace;
import at.jku.isse.designspace.sdk.core.model.User;
import at.jku.isse.designspace.sdk.core.model.Workspace;

public class DesignspaceConfig {

	
	public static Workspace getWorkspace() {
		User user = DesignSpace.registerUser("testuser");
		DesignSpace.connectToWorkspace(DesignSpace.PUBLIC_WORKSPACE());
		Optional<Workspace> existingWorkspace = DesignSpace.allWorkspaces(user).stream().filter(workspace_ -> workspace_.name().equals("at.jku.designspace.sdk.test." + user.name)).findAny();
		if (existingWorkspace.isEmpty()) {
			return DesignSpace.createWorkspace("at.jku.designspace.sdk.test." + user.name, DesignSpace.PUBLIC_WORKSPACE(), user, null, true, true);
		} else {
			Workspace workspace = existingWorkspace.get();
			DesignSpace.connectToWorkspace(workspace);
			return workspace;
		}
	}
}
