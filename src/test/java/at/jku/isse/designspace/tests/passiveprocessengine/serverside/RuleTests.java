package at.jku.isse.designspace.tests.passiveprocessengine.serverside;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import at.jku.isse.designspace.core.model.Workspace;
import at.jku.isse.designspace.core.service.WorkspaceService;
import at.jku.isse.designspace.rule.checker.ArlRuleEvaluator;
import at.jku.isse.designspace.rule.checker.ConsistencyUtils;
import at.jku.isse.designspace.rule.model.ConsistencyRule;
import at.jku.isse.designspace.rule.model.ConsistencyRuleType;
import at.jku.isse.designspace.rule.service.RuleService;
import at.jku.isse.designspace.core.model.Cardinality;
import at.jku.isse.designspace.core.model.Instance;
import at.jku.isse.designspace.core.model.InstanceType;
import at.jku.isse.designspace.core.model.PropertyType;
import at.jku.isse.designspace.core.model.SetProperty;
import at.jku.isse.passiveprocessengine.definition.StepDefinition;
import at.jku.isse.passiveprocessengine.instance.ProcessStep;

@ExtendWith(SpringExtension.class)
@SpringBootTest
public class RuleTests {

	Workspace workspace;
	InstanceType typeStep, typeJira;
	Instance step1, step2;
	Instance jira1, jira2;
//	@Autowired
//	private WorkspaceService workspaceService;

	@BeforeEach
	void setup() {
		RuleService.setEvaluator(new ArlRuleEvaluator());
		workspace = WorkspaceService.createWorkspace("test", WorkspaceService.PUBLIC_WORKSPACE, WorkspaceService.ANY_USER, null, false, false);
		typeJira = TestArtifacts.getJiraInstanceType(workspace);
		StepDefinition s1 = StepDefinition.getInstance("S1", workspace);
		typeStep = ProcessStep.getOrCreateDesignSpaceCoreSchema(workspace);
		if (typeStep.getPropertyType("in_story") == null) {
			PropertyType storyPropT = typeStep.createPropertyType("in_story", Cardinality.LIST, typeJira);
		}
		step1 = workspace.createInstance(typeStep, "Step1");
		step2 = workspace.createInstance(typeStep, "Step2");
		jira1 = workspace.createInstance(typeJira, "Jira1");		
		workspace.concludeTransaction();
	}

	@Test
	public void testInsertRule() {
		
		
		
		ConsistencyRuleType crd1 = ConsistencyRuleType.create(workspace, typeStep, "crd1", "self.in_story->size() > 0");
		workspace.concludeTransaction();
		assert ConsistencyUtils.crdValid(crd1);
		
		step2.getPropertyAsList("in_story").add(jira1);
		step1.getPropertyAsList("in_story").add(jira1);
		workspace.concludeTransaction();
		
		assert ConsistencyUtils.creExists(workspace.its(crd1), step1, true, false, true);
		//step1.propertyAsList("in.story").remove(0);
		
		step1.getPropertyAsList("in_story").remove(jira1);
		workspace.concludeTransaction();
		assert ConsistencyUtils.creExists(workspace.its(crd1), step1, false, false, true);
		assert ConsistencyUtils.creExists(workspace.its(crd1), step2, true, false, true);
		assert(step1.getPropertyAsList("in_story").size() == 0);
		SetProperty<ConsistencyRule> cres = crd1.consistencyRuleEvaluations();
		cres.stream().forEach(cre -> cre.contextInstance().name());
		
		jira2 = workspace.createInstance(typeJira, "Jira2");	
		workspace.concludeTransaction();
	}

}
