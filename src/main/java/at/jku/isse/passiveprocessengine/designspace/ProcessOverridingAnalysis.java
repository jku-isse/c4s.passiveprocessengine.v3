package at.jku.isse.passiveprocessengine.designspace;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import at.jku.isse.passiveprocessengine.core.PPEInstanceType;
import at.jku.isse.passiveprocessengine.core.ProcessContext;
import at.jku.isse.passiveprocessengine.core.RuleAnalysisService.OverrideAnalysisSession;
import at.jku.isse.passiveprocessengine.definition.ProcessDefinitionError;
import at.jku.isse.passiveprocessengine.definition.activeobjects.ConstraintSpec;
import at.jku.isse.passiveprocessengine.definition.activeobjects.ProcessDefinition;
import at.jku.isse.passiveprocessengine.definition.factories.ProcessDefinitionFactory;
import at.jku.isse.passiveprocessengine.instance.StepLifecycle.Conditions;
import at.jku.isse.passiveprocessengine.instance.types.SpecificProcessStepType;


public class ProcessOverridingAnalysis {
	private static final String CONSTRAINT_OVERRIDING_WARNING = "ConstraintOverridingWarning";

	private final ProcessContext context;
	
	public ProcessOverridingAnalysis(ProcessContext context) {
		this.context = context;
	}

	
	public List<ProcessDefinitionError> beginAnalysis(ProcessDefinition process, List<ProcessDefinitionError> warnings) {
		OverrideAnalysisSession oas = context.getRuleAnalysisService().createSession();		
		process.getStepDefinitions().stream().forEach(sd->{
			PPEInstanceType stepType = context.getSchemaRegistry().getTypeByName(SpecificProcessStepType.getProcessStepName(sd));
			if(sd instanceof ProcessDefinition) // process inside process?? 
			{// Might have to circle back to this if we get nested processes.
			}
			else
			{
				// 1: Extraction of IO Mappings of the Step
				// extractingIOMappings(sd);
				// 2: Going through Constraints In Order
				// 2.1: Pre Conditions
				sd.getPreconditions().stream().forEach(pre->{
					
					String specId = ProcessDefinitionFactory.getConstraintName(Conditions.PRECONDITION, pre.getOrderIndex(), stepType);
					String arl=pre.getConstraintSpec();
					//String arl=pre.getAugmentedConstraintSpec();
					if (arl!= null && pre.isOverridable()) {						
 						oas.generateOverridingData(stepType,specId,arl,pre.getName());
					}
					else if(arl!= null && !pre.isOverridable() && oas.isCurrentRuleAnalyzable())
					{
						inferImpact(oas, stepType, specId, arl, pre, warnings);
					}
				});
				// 2.2: QA Constraints 
				sd.getQAConstraints().stream().forEach(qa->{
					String specId = ProcessDefinitionFactory.getConstraintName(Conditions.QA, qa.getOrderIndex(), stepType);
					String arl=qa.getConstraintSpec();
					//String arl=qa.getAugmentedConstraintSpec();
					if(arl!=null && !qa.isOverridable() && oas.isCurrentRuleAnalyzable()) 
					{
						//arl=embeddingIOMappingIntoConstraint(arl);
						inferImpact(oas, stepType, specId, arl, qa, warnings);
					}
					else if(arl!=null && qa.isOverridable())
					{
						oas.generateOverridingData(stepType,specId,arl,qa.getName());
					}
				});
				// 2.3: Post Conditions 
				sd.getPostconditions().stream().forEach(post->{
					String specId = ProcessDefinitionFactory.getConstraintName(Conditions.POSTCONDITION, post.getOrderIndex(), stepType);
					String arl=post.getConstraintSpec();
					//String arl=post.getAugmentedConstraintSpec();
					if(arl!=null && !post.isOverridable() && oas.isCurrentRuleAnalyzable()) 
					{
						//arl=embeddingIOMappingIntoConstraint(arl);
						inferImpact(oas, stepType, specId, arl, post, warnings);
					}
					else if(arl!=null && post.isOverridable())
					{
						oas.generateOverridingData(stepType,specId,arl,post.getName());
					}
				});
			}
		});
		return warnings;
	}		
	
	public void inferImpact(OverrideAnalysisSession oas, PPEInstanceType stepType,String specId,String arl,ConstraintSpec rule,List<ProcessDefinitionError> warnings)
	{
		Map<String, Integer> impactResult = oas.inferImpact(stepType, specId, arl);						
		impactResult.entrySet().stream().forEach( entry -> {	
			//entry.getKey() is the effecting constraint
			if(entry.getValue()==0)
			{
				warnings.add(new ProcessDefinitionError(rule, CONSTRAINT_OVERRIDING_WARNING , stepType.getName()+" "+rule.getName()+" 'IsOverrideable' property might require to be ENABLED.", ProcessDefinitionError.Severity.INFO));
			}
			else if(entry.getValue()==-1)
			{
				warnings.add(new ProcessDefinitionError(rule, CONSTRAINT_OVERRIDING_WARNING ,stepType.getName()+" "+rule.getName()+" 'IsOverrideable' property must be ENABLED.", ProcessDefinitionError.Severity.WARNING));
			}
		});
	}
	

//	public String embeddingIOMappingIntoConstraint(String arl)
//	{
//		for (Map.Entry<String, String> mapping : mappingFromConstraintToItemType.entrySet()) {
//			if(arl.contains(mapping.getKey()))
//			{
//				arl=arl.replace(mapping.getKey(), mapping.getValue());
//			}
//		}
//		return arl;
//	}
//
//
//
//	public void extractingIOMappings(StepDefinition sd)
//	{
//		Map<String, String> IoMappings= sd.getInputToOutputMappingRules();
//		if(IoMappings.size()>0)
//		{
//			for (Map.Entry<String, String> mapping : IoMappings.entrySet()) {
//				String key = mapping.getKey();
//				String value = mapping.getValue();
//				mappingFromConstraintToItemType.put("self.out_"+key, value);
//			}
//		}	
//	}




}

