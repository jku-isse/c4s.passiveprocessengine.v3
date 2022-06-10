package at.jku.isse.designspace.passiveprocessengine.instance;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import at.jku.isse.designspace.core.model.Workspace;
import at.jku.isse.designspace.core.service.WorkspaceService;
import at.jku.isse.designspace.rule.arl.expressions.Expression;
import at.jku.isse.designspace.rule.arl.expressions.VariableExpression;
import at.jku.isse.designspace.rule.checker.ArlEvaluator;
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
import at.jku.isse.passiveprocessengine.demo.TestArtifacts;
import at.jku.isse.passiveprocessengine.instance.ProcessInstanceChangeProcessor;
import at.jku.isse.passiveprocessengine.instance.ProcessStep;
import at.jku.isse.passiveprocessengine.instance.StepLifecycle.Conditions;

@ExtendWith(SpringExtension.class)
@SpringBootTest
public class RuleAnalysisTests {

	Workspace workspace;
	InstanceType typeStep, typeJira;
	Instance step1, step2;
	Instance jira1, jira2;
//	ProcessInstanceChangeProcessor picp;
//	@Autowired
//	private WorkspaceService workspaceService;

	@BeforeEach
	void setup() {
		//RuleService.setEvaluator(new ArlRuleEvaluator());
		workspace = WorkspaceService.createWorkspace("test", WorkspaceService.PUBLIC_WORKSPACE, WorkspaceService.ANY_USER, null, false, false);
		workspace.setAutoUpdate(true);
		
		typeJira = TestArtifacts.getJiraInstanceType(workspace);
		StepDefinition s1 = StepDefinition.getInstance("S1", workspace);
		//s1.setCondition(Conditions.PRECONDITION, "self.in_story->size() > 0");
		typeStep = ProcessStep.getOrCreateDesignSpaceInstanceType(workspace, s1);
		if (typeStep.getPropertyType("in_story") == null) {
			typeStep.createPropertyType("in_story", Cardinality.LIST, typeJira);
			typeStep.createPropertyType("out_story", Cardinality.LIST, typeJira);
		}
		step1 = workspace.createInstance(typeStep, "Step1");
		step2 = workspace.createInstance(typeStep, "Step2");
		jira1 = workspace.createInstance(typeJira, "Jira1");		
		workspace.concludeTransaction();
	}
	
	@Test
	public void testAnalyseSimpleRule() {
		String arl = "self.in_story->select( ref : <root/types/DemoIssue> | ref.state.equalsIgnoreCase(\'Open\')).size() > 0";
		ArlEvaluator ae = new ArlEvaluator(typeStep, arl);
		printSyntaxTree(ae.syntaxTree, "");
		printOriginalSyntaxTree(ae.syntaxTree, "");
		assert(stripForComparison(ae.syntaxTree.getOriginalARL()).equalsIgnoreCase(stripForComparison(arl)));
	}
	
	@Test
	public void testAnalyseComplexRule() {
		
		String arl = "(self.in_story"
				+ "->asList()"
				+ "->first()"
				+ "->asType(<"+typeJira.getQualifiedName()+">)"
						+ ".requirementIDs"
							+ "->forAll(id | self.out_story->exists(art : <root/types/DemoIssue> | art.name = id))"
			+ " and "
				+ "self.out_story"
				+ "->forAll(out : <root/types/DemoIssue> | self.in_story"
									+ "->asList()"
									+ "->first()"
									+ "->asType(<"+typeJira.getQualifiedName()+">)"
											+ ".requirementIDs"
											+ "->exists(artId | artId = out.name)))";
		ArlEvaluator ae = new ArlEvaluator(typeStep, arl);
		//printSyntaxTree(ae.syntaxTree, "");
		printOriginalSyntaxTree(ae.syntaxTree, "");
		String recovered = ae.syntaxTree.getOriginalARL();
		ArlEvaluator ae2 = new ArlEvaluator(typeStep, recovered);
		printSyntaxTree(ae2.syntaxTree, "");
		assert(stripForComparison(recovered).equalsIgnoreCase(stripForComparison(arl)));
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void testRewriteVarInRule() {
		
		String arl = "self.in_story"
				+ "->asList()"
				+ "->first()"
				+ "->asType(<"+typeJira.getQualifiedName()+">)"
						+ ".requirementIDs"
							+ "->forAll(id | self.out_story->exists(art : <root/types/DemoIssue> | art.name = id))"
			+ " and "
				+ "self.out_story"
				+ "->forAll(out : <root/types/DemoIssue> | self.in_story"
									+ "->asList()"
									+ "->first()"
									+ "->asType(<"+typeJira.getQualifiedName()+">)"
											+ ".requirementIDs"
											+ "->exists(artId | artId = out.name))";
		ArlEvaluator ae = new ArlEvaluator(typeStep, arl);
		ae.parser.currentEnvironment.locals.values().stream()
			.filter(var -> !((VariableExpression)var).name.equals("self"))
			.forEach(var -> ((VariableExpression)var).name = ((VariableExpression)var).name+"1");
		//printSyntaxTree(ae.syntaxTree, "");
		printOriginalSyntaxTree(ae.syntaxTree, "");
		String recovered = ae.syntaxTree.getOriginalARL();
		ArlEvaluator ae2 = new ArlEvaluator(typeStep, recovered);
		printSyntaxTree(ae2.syntaxTree, "");
		
		String arl1 = "(self.in_story"
				+ "->asList()"
				+ "->first()"
				+ "->asType(<"+typeJira.getQualifiedName()+">)"
						+ ".requirementIDs"
							+ "->forAll(id1 | self.out_story->exists(art1 : <root/types/DemoIssue> | art1.name = id1))"
			+ " and "
				+ "self.out_story"
				+ "->forAll(out1 : <root/types/DemoIssue> | self.in_story"
									+ "->asList()"
									+ "->first()"
									+ "->asType(<"+typeJira.getQualifiedName()+">)"
											+ ".requirementIDs"
											+ "->exists(artId1 | artId1 = out1.name)))";
		assert(stripForComparison(recovered).equalsIgnoreCase(stripForComparison(arl1)));
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void testRewriteAndOrComboRule() {
		
		String arl = "((self.in_story"
				+ "->asList()"
				+ "->first()"
				+ "->asType(<"+typeJira.getQualifiedName()+">)"
						+ ".requirementIDs"
							+ "->forAll(id | self.out_story->exists(art : <root/types/DemoIssue> | art.name = id))"
			+ " and "
				+ "self.out_story"
				+ "->forAll(out : <root/types/DemoIssue> | self.in_story"
									+ "->asList()"
									+ "->first()"
									+ "->asType(<"+typeJira.getQualifiedName()+">)"
											+ ".requirementIDs"
											+ "->exists(artId | artId = out.name))"
											+ ") or "
											+ "self.out_story"
											+ "->forAll(out2 : <root/types/DemoIssue> | self.in_story"
																+ "->asList()"
																+ "->first()"
																+ "->asType(<"+typeJira.getQualifiedName()+">)"
																		+ ".requirementIDs"
																		+ "->exists(artId2 | not(artId2 = out2.name))))"								
											;
		ArlEvaluator ae = new ArlEvaluator(typeStep, arl);
		String recovered = ae.syntaxTree.getOriginalARL();
		System.out.println(recovered);
		assert(stripForComparison(recovered).equalsIgnoreCase(stripForComparison(arl)));
	}
	
	private String stripForComparison(String arl) {
		return arl
			.replace("->", "")
			.replace(".", "")
			.replaceAll("[\\n\\t ]", "")
			.trim();
	}
	
	private void printSyntaxTree(Expression ep, String indent) {
		if (ep == null) return;
		System.out.println(String.format("%s %s returns %s", indent, ep.getARL(), ep.getResultType() != null ? ep.getResultType().toString() : "NULL"));
		final String newIndent = indent + "  ";
		ep.getChildren().stream().forEach(child -> printSyntaxTree((Expression) child, newIndent));
	}
	
	private void printOriginalSyntaxTree(Expression ep, String indent) {
		if (ep == null) return;
		System.out.println(String.format("%s %s returns %s", indent, ep.getOriginalARL(),  ep.getResultType() != null ? ep.getResultType().toString() : "NULL"));
		final String newIndent = indent + "  ";
		ep.getChildren().stream().forEach(child -> printOriginalSyntaxTree((Expression) child, newIndent));
	}
}
