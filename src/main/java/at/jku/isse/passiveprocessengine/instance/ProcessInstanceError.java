package at.jku.isse.passiveprocessengine.instance;

import lombok.Data;

@Data
public class ProcessInstanceError {
	final ProcessInstanceScopedElement errorScope;
	final String errorType;
	final String errorMsg;


	@Override
	public String toString() {
		return "ProcessInstanceError at " + errorScope.getName() + " of type <" + errorType + "> with message: "
				+ errorMsg;
	}


}
