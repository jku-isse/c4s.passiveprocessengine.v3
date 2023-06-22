package at.jku.isse.passiveprocessengine.definition;

import at.jku.isse.designspace.rule.arl.evaluator.RuleEvaluation;
import at.jku.isse.passiveprocessengine.ProcessDefinitionScopedElement;
import lombok.Data;

@Data
public class ProcessDefinitionError {
	final ProcessDefinitionScopedElement errorScope;
	final String errorType;
	final String errorMsg;
	

	public static class RuleCentricProcessDefinitionError extends ProcessDefinitionError {
		transient RuleEvaluation ruleEvaluation;

		public RuleCentricProcessDefinitionError(ProcessDefinitionScopedElement errorScope, 
				RuleEvaluation ruleEvaluation) {
			super(errorScope, "Consistency Rule Violation: "+ruleEvaluation.getRuleDefinition().getName(), "Violated");
			this.ruleEvaluation = ruleEvaluation;
		}	
	}


	@Override
	public String toString() {
		String proc = errorScope.getProcess() != null ? "in Process '"+errorScope.getProcess().getName()+"'": "";
		return "DefinitionError "+proc+" at " +errorScope.getName() +" ("+ errorScope.getClass().getSimpleName()+") of type <" + errorType + "> with message: "
				+ errorMsg;
	}

	
}
