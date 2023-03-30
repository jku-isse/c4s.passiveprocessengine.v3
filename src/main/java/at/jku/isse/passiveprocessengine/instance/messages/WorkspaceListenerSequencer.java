package at.jku.isse.passiveprocessengine.instance.messages;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import at.jku.isse.designspace.core.events.Operation;
import at.jku.isse.designspace.core.model.Workspace;
import at.jku.isse.designspace.core.model.WorkspaceListener;

public class WorkspaceListenerSequencer implements WorkspaceListener {

	List<WorkspaceListener> listeners = Collections.synchronizedList(new LinkedList<>());
	
	
	public WorkspaceListenerSequencer(Workspace ws) {
		ws.workspaceListeners.add(this);
	}

	public void registerListener(WorkspaceListener wsl) {
		listeners.add(wsl);
	}
	
	@Override
	public void handleUpdated(Collection<Operation> operations) {
		listeners.forEach(wsl -> wsl.handleUpdated(operations));
	}
}
