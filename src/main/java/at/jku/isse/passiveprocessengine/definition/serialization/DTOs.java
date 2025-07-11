package at.jku.isse.passiveprocessengine.definition.serialization;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import at.jku.isse.passiveprocessengine.definition.activeobjects.DecisionNodeDefinition.InFlowType;
import at.jku.isse.passiveprocessengine.instance.StepLifecycle.Conditions;
import at.jku.isse.passiveprocessengine.instance.types.SpecificProcessConfigType.PropertySchemaDTO;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

public class DTOs {


	public static interface Typed {

	}

	@ToString(doNotUseGetters = true)
	@Data	
	@SuperBuilder
	public abstract static class Element implements Typed{
		final String _type = this.getClass().getSimpleName();
		private String code;
		@Builder.Default String description = "";

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if ((obj == null) || (getClass() != obj.getClass()))
				return false;
			Element other = (Element) obj;
			return Objects.equals(code, other.code);
		}
		@Override
		public int hashCode() {
			return Objects.hash(code);
		}
	}

	public static class ConstraintBySpecOrderComparator implements Comparator<Constraint> {

		@Override
		public int compare(Constraint o1, Constraint o2) {
			return Integer.compare(o1.getSpecOrderIndex(), o2.getSpecOrderIndex());
		}				
	}
	
	public static ConstraintBySpecOrderComparator constraintComparator = new ConstraintBySpecOrderComparator();
	
	@ToString(doNotUseGetters = true, callSuper = true)
	@Data
	@SuperBuilder
	public static class Constraint extends Element {
		final String arlRule;
		@Builder.Default int specOrderIndex = 0;
		@Builder.Default boolean isOverridable = false;

		@Override
		public boolean equals(Object obj) {
			return super.equals(obj);
		}

		@Override
		public int hashCode() {
			return super.hashCode();
		}
		
		public String getCode() {
			if (super.getCode() == null)
				return arlRule;
			else 
				return super.getCode();
		}
	}

	//@EqualsAndHashCode(callSuper = true)
	@ToString(doNotUseGetters = true, callSuper = true)
	@Data
	@SuperBuilder
	public static class Step extends Element {
		String inDNDid;
		String outDNDid;
		final Map<String,String> input = new TreeMap<>();
		final Map<String,String> output = new TreeMap<>();
		final Map<String,String> ioMapping = new TreeMap<>();
		final Map<Conditions,List<Constraint>> conditions = new TreeMap<>();
		final Set<Constraint> qaConstraints = new TreeSet<>(constraintComparator);
		@Builder.Default int specOrderIndex = 0;
		@Builder.Default String html_url = "";

		protected void toPlantUML(StringBuffer sb) {
			String errorMsgs = output.entrySet().stream()
					.filter(entry -> !ioMapping.containsKey(entry.getKey())) // find any output without mapping
					.map(entry -> "No IOMapping for output: "+entry.getKey())
					.collect(Collectors.joining("\r\n","\r\n", ""));
			String highlight = errorMsgs.length() > 5 ? "#red" :"";

			String stepUML = String.format("\r\n %s:%s %s ;", highlight, this.getCode(), errorMsgs);
			sb.append(stepUML);
			sb.append("\r\n note left");
	        this.input.forEach((var, type) -> sb.append("\r\n   in: "+var));
	        this.output.forEach((var, type) -> sb.append("\r\n   out: "+var));
	        sb.append("\r\n end note");
			if (!this.qaConstraints.isEmpty()) {
//				sb.append("\r\nnote right");
//	        	this.qaConstraints.forEach(qac -> sb.append("\r\nQA: "+qac.getCode()));
//	        	sb.append("\r\nend note");
				sb.append(this.qaConstraints.stream()
						.map(qac -> " QA: "+qac.getCode() )
						.collect(Collectors.joining("\r\n ", "\r\n  -> ", ";")));
			}
		}

		protected void toPlantUMLDataflow(StringBuffer sb) {
			sb.append("\r\n  class \""+this.getCode()+"\"");
		}

		@Override
		public boolean equals(Object obj) {
			return super.equals(obj);
		}

		@Override
		public int hashCode() {
			return super.hashCode();
		}
	}

	
	public static Comparator<Mapping> mappingComparatorByAllFields = Comparator.comparing(Mapping::getFromStep)
			.thenComparing(Mapping::getFromParam)
			.thenComparing(Mapping::getToStep)
			.thenComparing(Mapping::getToParam);
	
	
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

	public static class DecisionNodeByNameComparator implements Comparator<DecisionNode> {

		@Override
		public int compare(DecisionNode o1, DecisionNode o2) {			
			return o1.getCode().compareTo(o2.getCode());
		}		
	}
	
	public static DecisionNodeByNameComparator dnComparator = new DecisionNodeByNameComparator();
	
	@ToString(doNotUseGetters = true, callSuper = true)
	@Data
	@SuperBuilder
	public static class DecisionNode extends Element {
		@Builder.Default InFlowType inflowType = InFlowType.SEQ; //default value
		final Set<Mapping> mapping = new TreeSet<>(mappingComparatorByAllFields);
		@Builder.Default int depthIndex = -1;

		protected void toPlantUMLDataflow(StringBuffer sb) {
			mapping.forEach(m -> sb.append("\r\n \""+m.getFromStep()+"\" -down-> \""+m.getToStep()+"\" : \""+m.getToParam()+"\""));
		}

		@Override
		public boolean equals(Object obj) {
			return super.equals(obj);
		}

		@Override
		public int hashCode() {
			return super.hashCode();
		}
	}

	@ToString(doNotUseGetters = true, callSuper = true)
	@Data
	@SuperBuilder
	public static class Process extends Step {
		final List<Step> steps = new LinkedList<>();
		final Set<DecisionNode> dns = new TreeSet<>(dnComparator);
		final Map<String, String> prematureStepConditions = new TreeMap<>();
		final Map<String, String> processConfig = new TreeMap<>();
		final Map<String, Set<PropertySchemaDTO>> configs = new TreeMap<>();

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

		public List<Step> getOutStepsOf(DecisionNode dn) {
			return steps.stream().filter(step -> step.getInDNDid().equals(dn.getCode())).collect(Collectors.toList());
		}

		public List<Step> getInStepsOf(DecisionNode dn) {
			return steps.stream().filter(step -> step.getOutDNDid().equals(dn.getCode())).collect(Collectors.toList());
		}

		public DecisionNode getEntryNode() {
			return dns.stream().filter(dn -> getInStepsOf(dn).size() == 0).findAny().get();
		}

		public DecisionNode getExitNode() {
			return dns.stream().filter(dn -> getOutStepsOf(dn).size() == 0).findAny().get();
		}

		public void calculateDecisionNodeDepthIndex(int startIndex) {
			setDNDepthIndexRecursive(getEntryNode(), startIndex);
		}

		private void setDNDepthIndexRecursive(DecisionNode dn, int index) {
			dn.setDepthIndex(index);
			int newIndex = this.getOutStepsOf(dn).size() > 1 ? index+1 : index; // we only increase the depth when we branch out
			this.getOutStepsOf(dn).stream().forEach(step -> setDNviaStepDepthIndexRecursive(step, newIndex));
		}

		private void setDNviaStepDepthIndexRecursive(Step step, int index) {
			DecisionNode dnd = this.getOutDNof(step);
			int newIndex = (this.getInStepsOf(dnd).size() > 1) ? index - 1 : index; // if in branching, reduction of index, otherwise same index as just a sequence
			if (dnd.getDepthIndex() < newIndex) // this allows to override the index when this is used as a subprocess
				this.setDNDepthIndexRecursive(dnd, newIndex);
			if (step instanceof Process) { // set depth on subprocess
				((Process) step).calculateDecisionNodeDepthIndex(index+1);
			}
		}

		public DecisionNode getScopeClosingDN(DecisionNode dn) {
			List<Step> nextSteps = getOutStepsOf(dn);
			if (nextSteps.isEmpty()) return null; // end of the process, closing DN reached
			Set<DecisionNode> nextStepOutDNs = nextSteps.stream().map(step -> getOutDNof(step)).collect(Collectors.toSet());
			// size must be 1 or greater as we dont allow steps without subsequent DN
			if (nextStepOutDNs.size() == 1) { // implies the scope closing DN as otherwise there need to be multiple opening subscope ones
				return nextStepOutDNs.iterator().next();
			} else {
				Set<DecisionNode> sameDepthNodes = new TreeSet<>(dnComparator);
				while (sameDepthNodes.size() != 1) {
					sameDepthNodes = nextStepOutDNs.stream().filter(nextDN -> nextDN.getDepthIndex() == dn.getDepthIndex()).collect(Collectors.toSet());
					assert(sameDepthNodes.size() <= 1); //closing next nodes can only be on same level or deeper (i.e., larger values)
					if (sameDepthNodes.size() != 1) {
						Set<DecisionNode> nextNextStepOutDNs = nextStepOutDNs.stream().map(nextDN -> getScopeClosingDN(nextDN)).collect(Collectors.toSet());
						nextStepOutDNs = nextNextStepOutDNs;
					}
					assert(nextStepOutDNs.size() > 0);
				}
				return sameDepthNodes.iterator().next();
			}
		}

		@Override
		public boolean equals(Object obj) {
			return super.equals(obj);
		}

		@Override
		public int hashCode() {
			return super.hashCode();
		}

		public String toPlantUMLDataflowAsClassDiagram() {
			StringBuffer sb = new StringBuffer("@startuml\r\n skin rose \r\n title Dataflow "+this.getCode()+"\r\n");
			toPlantUMLDataflow(sb);
			sb.append("\r\n@enduml");
			return sb.toString();
		}

		@Override
		protected void toPlantUMLDataflow(StringBuffer sb) {
			sb.append("\r\npackage \""+this.getCode()+"\"");
			sb.append(" {");
			this.steps.forEach(step -> step.toPlantUMLDataflow(sb));
			sb.append("\r\n}");

			sb.append("\r\n");
			this.dns.forEach(node -> node.toPlantUMLDataflow(sb));
		}


		public String toPlantUMLActivityDiagram() {
			StringBuffer sb = new StringBuffer("@startuml\r\n"
					+ "start\r\n");
			toPlantUML(sb);
			sb.append("\r\nend"
					+ "\r\n@enduml");
			return sb.toString();
		}

		@Override
		protected void toPlantUML(StringBuffer sb) {
			sb.append("\r\npackage "+this.getCode());
			sb.append("{");

			sb.append("\r\n");
			if (input.size() > 0 && output.size() > 0) {
				sb.append("\r\n note");
				this.input.forEach((var, type) -> sb.append("\r\n  in: "+var));
				this.output.forEach((var, type) -> sb.append("\r\n  out: "+var));
				sb.append("\r\n end note");
			}
			if (!this.qaConstraints.isEmpty()) {
				//sb.append("\r\nnote right");
				sb.append(this.qaConstraints.stream()
						.map(qac -> "QA: "+qac.getCode() )
						.collect(Collectors.joining("\r\n ", "\r\n  -> ", ";")));
	        	//sb.append("\r\nend note");
			}

			steps.sort(new Comparator<Step>() {
				@Override
				public int compare(Step o1, Step o2) {
					return Integer.compare(o1.getSpecOrderIndex(), o2.getSpecOrderIndex());
				}
			});
			if (steps.size() > 0) {
				Step first = steps.get(0);
				DecisionNode startDN = this.getInDNof(first);
				if (startDN.getDepthIndex() <= 0)
					this.calculateDecisionNodeDepthIndex(1);
				DecisionNode nextDN = startDN;
				do {
					nextDN = toPlantUMLsubscope(nextDN, sb);
				} while (nextDN != null);
			}
			sb.append("\r\n}");
		}

		// returns the closing DecisionNode
		private DecisionNode toPlantUMLsubscope(DecisionNode inDN, StringBuffer sb) {

			List<Step> subsequentSteps = getOutStepsOf(inDN);
			if (subsequentSteps.isEmpty()) return null; // reached end of process
			if (subsequentSteps.size() == 1) {
				Step step = subsequentSteps.get(0);
				// write out process step
				step.toPlantUML(sb);
				return this.getOutDNof(step);
			} else {
				// find end scope DN
				DecisionNode closingDN = getScopeClosingDN(inDN);
				AtomicInteger count = new AtomicInteger(0);
				// process steps
				subsequentSteps.stream().map(nextStep -> {
					if (count.getAndIncrement() == 0)
						sb.append("\r\nfork");
					else {
						sb.append("\r\nfork again");
					}
					nextStep.toPlantUML(sb);
					DecisionNode nextDN = this.getOutDNof(nextStep);
					if (!nextDN.equals(closingDN)) { // found a subscope
						DecisionNode nextNode = toPlantUMLsubscope(nextDN, sb); // but what to do with the returned DNs
						if (nextNode != closingDN)
							return nextNode;
						else
							return null;
					} else
						return null;
				})
				.filter(Objects::nonNull)
				.distinct()
				.forEach(nextNode -> {
					toPlantUMLsubscope(nextNode, sb);
				});

				sb.append("\r\nend fork {"+closingDN.getInflowType()+"}");

				// recursive call to outdn if this is scope as further subscopes
				return closingDN;
			}
		}
	}
}
