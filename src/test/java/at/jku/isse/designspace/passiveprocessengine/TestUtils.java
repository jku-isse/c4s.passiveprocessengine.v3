package at.jku.isse.designspace.passiveprocessengine;

import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.stream.Collectors;

import at.jku.isse.designspace.core.model.Instance;
import at.jku.isse.designspace.core.model.InstanceType;
import at.jku.isse.designspace.rule.checker.ConsistencyUtils;
import at.jku.isse.designspace.rule.model.ConsistencyRuleType;
import at.jku.isse.designspace.rule.model.Rule;
import at.jku.isse.passiveprocessengine.WrapperCache;
import at.jku.isse.passiveprocessengine.definition.ProcessDefinition;
import at.jku.isse.passiveprocessengine.instance.ConstraintWrapper;
import at.jku.isse.passiveprocessengine.instance.ProcessInstance;
import at.jku.isse.passiveprocessengine.instance.ProcessStep;
import at.jku.isse.passiveprocessengine.instance.StepLifecycle.Conditions;

public class TestUtils {

	public static void assertAllConstraintsAreValid(ProcessInstance proc) {
		proc.getProcessSteps().stream()
		.peek(td -> System.out.println("Visiting Step: "+td.getName()))
		.forEach(td -> {
			td.getDefinition().getInputToOutputMappingRules().entrySet().stream().forEach(entry -> {
				InstanceType type = td.getInstance().getProperty("crd_datamapping_"+entry.getKey()).propertyType().referencedInstanceType();
				ConsistencyRuleType crt = (ConsistencyRuleType)type;
				assertTrue(ConsistencyUtils.crdValid(crt));
				String eval = (String) crt.ruleEvaluations().get().stream()
						.map(rule -> ((Rule)rule).result()+"" )
						.collect(Collectors.joining(",","[","]"));
				System.out.println("Checking "+crt.name() +" Result: "+ eval);
			});
			ProcessDefinition pd = td.getProcess() !=null ? td.getProcess().getDefinition() : (ProcessDefinition)td.getDefinition();
			td.getDefinition().getQAConstraints().stream().forEach(entry -> {
				//InstanceType type = td.getInstance().getProperty(ProcessStep.getQASpecId(entry, ProcessStep.getOrCreateDesignSpaceInstanceType(ws, td.getDefinition()))).propertyType().referencedInstanceType();
				String id = ProcessStep.getQASpecId(entry, pd);
				ConstraintWrapper cw = WrapperCache.getWrappedInstance(ConstraintWrapper.class, (Instance) td.getInstance().getPropertyAsMap(ProcessStep.CoreProperties.qaState.toString()).get(id));
				ConsistencyRuleType crt = (ConsistencyRuleType)cw.getCr().getInstanceType();
				assertTrue(ConsistencyUtils.crdValid(crt));
				String eval = (String) crt.ruleEvaluations().get().stream()
								.map(rule -> ((Rule)rule).result()+"" )
								.collect(Collectors.joining(",","[","]"));
				System.out.println("Checking "+crt.name() +" Result: "+ eval);
				
			});
			for (Conditions condition : Conditions.values()) {
				if (td.getDefinition().getCondition(condition).isPresent()) {
					InstanceType type = td.getInstance().getProperty(condition.toString()).propertyType().referencedInstanceType();
					ConsistencyRuleType crt = (ConsistencyRuleType)type;
					assertTrue(ConsistencyUtils.crdValid(crt));
					String eval = (String) crt.ruleEvaluations().get().stream()
							.map(rule -> ((Rule)rule).result()+"" )
							.collect(Collectors.joining(",","[","]"));
					System.out.println("Checking "+crt.name() +" Result: "+ eval);
				}	
			}
	});
		proc.getDefinition().getPrematureTriggers().entrySet().stream()
		.forEach(entry -> {
			if (entry.getValue() != null) {
				String ruleName = ProcessInstance.generatePrematureRuleName(entry.getKey(), proc.getDefinition());
				Collection<InstanceType> ruleDefinitions = proc.getInstance().workspace.its(ConsistencyRuleType.CONSISTENCY_RULE_TYPE).subTypes();
		        if(! ruleDefinitions.isEmpty() && !(ruleDefinitions.stream().filter(inst -> !inst.isDeleted).count() == 0)) {
		        	for(InstanceType crt: ruleDefinitions.stream().filter(inst -> !inst.isDeleted).collect(Collectors.toSet() )){
		        		if (crt.name().equalsIgnoreCase(ruleName)) {
		        			assertTrue(ConsistencyUtils.crdValid((ConsistencyRuleType)crt));
		        			String eval = (String) ((ConsistencyRuleType)crt).ruleEvaluations().get().stream()
									.map(rule -> ((Rule)rule).result()+"" )
									.collect(Collectors.joining(",","[","]"));
							System.out.println("Checking "+crt.name() +" Result: "+ eval);
		        		}
		        	}
		        }
			}
		});
	}
	
	public static void printFullProcessToLog(ProcessInstance proc) {
		printProcessToLog(proc, " ");
	}
	
	public static void printProcessToLog(ProcessInstance proc, String prefix) {
		
		System.out.println(prefix+proc.toString());
		String nextIndent = "  "+prefix;
		proc.getProcessSteps().stream().forEach(step -> {
			if (step instanceof ProcessInstance) {
				printProcessToLog((ProcessInstance) step, nextIndent);
			} else {
				
				System.out.println(nextIndent+step.toString());
			}
		});
		proc.getDecisionNodeInstances().stream().forEach(dni -> System.out.println(nextIndent+dni.toString()));
	}
}
