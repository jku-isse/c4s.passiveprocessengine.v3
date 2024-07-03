package at.jku.isse.passiveprocessengine.definition;

import at.jku.isse.passiveprocessengine.definition.activeobjects.ProcessDefinitionScopedElement;
import lombok.Data;

@Data
public class ProcessDefinitionError {
	public enum Severity {INFO, WARNING, ERROR};
	
	final ProcessDefinitionScopedElement errorScope;
	final String errorType;
	final String errorMsg;
	final Severity severity;
	
	@Override
	public String toString() {
		String proc = errorScope.getProcess() != null ? "in Process '"+errorScope.getProcess().getName()+"'": "";
		return "Definition"+severity+" "+proc+" at " +errorScope.getName() +" ("+ errorScope.getClass().getSimpleName()+") of type <" + errorType + "> with message: "
				+ errorMsg;
	}


}
