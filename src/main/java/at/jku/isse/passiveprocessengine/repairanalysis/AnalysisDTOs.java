package at.jku.isse.passiveprocessengine.repairanalysis;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import at.jku.isse.designspace.core.operations.PropertyValueOperation;
import at.jku.isse.designspace.rule.model.ConsistencyRuleType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Setter;

public class AnalysisDTOs {

	@Setter
	public static class StatsOutput {
		List<SerializableConflict> conflicts;
		Map<String, List<Set<String>>> inconsistencyCausingNonRepairableOperations;
		Map<String, List<Set<String>>> notsuggestedRepairOperations;
		Map<String, List<Integer>> repairSizeStats;
		Map<String, String> unsupportedRepairs;
	}
	
	@Data
	@EqualsAndHashCode(onlyExplicitlyIncluded = true)
	public static class Conflict {
		@EqualsAndHashCode.Include
		private final ConsistencyRuleType posRule;
		@EqualsAndHashCode.Include
		private final ConsistencyRuleType negRule;
		private List<PropertyValueOperation> changes = new LinkedList<>();

		public Conflict add(PropertyValueOperation op) {
			changes.add(op);
			return this;
		}
	}

	@Data
	public static class SerializableConflict {
		private final String posRule;
		private final String negRule;
		private final List<String> changes;

	}

}
