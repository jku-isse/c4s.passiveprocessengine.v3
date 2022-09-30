package at.jku.isse.passiveprocessengine.definition.serialization;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import at.jku.isse.passiveprocessengine.definition.DecisionNodeDefinition.InFlowType;
import at.jku.isse.passiveprocessengine.instance.StepLifecycle.Conditions;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

public class DTOs {

	
	public static interface Typed {
		
	}
	
	@ToString(doNotUseGetters = true)
	@Data
	public abstract static class Element implements Typed{
		String _type = this.getClass().getSimpleName();
		String code;
		String description;
	}
	
	@EqualsAndHashCode(callSuper = true)
	@ToString(doNotUseGetters = true, callSuper = true)
	@Data
	public static class QAConstraint extends Element {
		String arlRule;
		int specOrderIndex = 0;
	}
	
	@EqualsAndHashCode(callSuper = true)
	@ToString(doNotUseGetters = true, callSuper = true)
	@Data
	public static class Step extends Element {
		String inDNDid;
		String outDNDid;
		Map<String,String> input = new HashMap<>();
		Map<String,String> output = new HashMap<>();
		Map<String,String> ioMapping = new HashMap<>();
		Map<Conditions,String> conditions = new HashMap<>();
		Set<QAConstraint> qaConstraints = new HashSet<>();
		int specOrderIndex = 0;
		String html_url;
	} 

	@ToString(doNotUseGetters = true)
	@Data
	public static class Mapping {
		String fromStep;
		String fromParam;
		String toStep;
		String toParam;
		
		public Mapping(String fromStepType, String fromParameter, String toStepType, String toParameter) {
			this.fromStep = fromStepType;
			this.fromParam = fromParameter;
			this.toStep = toStepType;
			this.toParam = toParameter;
		}
	}
	
	@EqualsAndHashCode(callSuper = true)
	@ToString(doNotUseGetters = true, callSuper = true)
	@Data
	public static class DecisionNode extends Element {
		InFlowType inflowType = InFlowType.AND; //default value
		Set<Mapping> mapping = new HashSet<>();
	}
	
	@EqualsAndHashCode(callSuper = true)
	@ToString(doNotUseGetters = true, callSuper = true)
	@Data
	public static class Process extends Step {
		List<Step> steps = new LinkedList<>();
		List<DecisionNode> dns = new LinkedList<>();
		Map<String, String> prematureStepConditions = new HashMap<>();
		
		public Step getStepByCode(String code) {
			return steps.stream().filter(step -> step.getCode().equals(code)).findAny().orElse(null);
		}
		
		public DecisionNode getDecisionNodeByCode(String code) {
			return dns.stream().filter(dn -> dn.getCode().equals(code)).findAny().orElse(null);
		}
		
		public DecisionNode getInDNof(Step step) {
			return dns.stream().filter(dn -> dn.getCode().equals(step.getInDNDid())).findAny().orElse(null);
		}	
		
		public DecisionNode getOutDNof(Step step) {
			return dns.stream().filter(dn -> dn.getCode().equals(step.getOutDNDid())).findAny().orElse(null);
		}
		
		public Set<Step> getSuccessorOf(Step step) {
			DecisionNode nextDN = getOutDNof(step);
			if (nextDN == null) return Collections.emptySet();
			return steps.stream().filter(succstep -> succstep.getInDNDid().equals(nextDN.getCode())).collect(Collectors.toSet());			
		}
	}
}
