package c4s.passiveprocessengine.v3;

import static org.junit.Assert.*;

import org.junit.BeforeClass;
import org.junit.Test;

import at.jku.isse.designspace.sdk.core.model.Cardinality;
import at.jku.isse.designspace.sdk.core.model.Instance;
import at.jku.isse.designspace.sdk.core.model.InstanceType;
import at.jku.isse.designspace.sdk.core.model.ListProperty;
import at.jku.isse.designspace.sdk.core.model.PropertyType;
import at.jku.isse.designspace.sdk.core.model.Workspace;
import at.jku.isse.passiveprocessengine.definition.StepDefinition;
import at.jku.isse.passiveprocessengine.instance.ProcessInstanceChangeListener;
import at.jku.isse.passiveprocessengine.instance.ProcessStep;

public class TestCreateProcessStep {

	static Workspace workspace;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		workspace = DesignspaceConfig.getWorkspace();
		ProcessInstanceChangeListener picl = new ProcessInstanceChangeListener(workspace);
	}

	@Test
	public void testCreateItem() {
		InstanceType typeJira = TestArtifacts.getJiraInstanceType(workspace);
		StepDefinition s1 = StepDefinition.getInstance("S1", workspace);
		InstanceType typeStep = ProcessStep.getOrCreateDesignSpaceCoreSchema(workspace);
		if (typeStep.propertyType("in.story") == null) {
			PropertyType storyPropT = typeStep.createPropertyType("in.story", Cardinality.LIST, typeJira);
		}
		
		workspace.commit();
		Instance step1 = workspace.createInstance("Step1", typeStep);
		Instance jira1 = workspace.createInstance("Jira1", typeJira);
		workspace.commit();
		
		step1.propertyAsList("in.story").add(jira1);
		workspace.commit();
		
		//step1.propertyAsList("in.story").remove(0);
		step1.propertyAsList("in.story").remove(jira1);
		
		workspace.commit();
		assert(step1.propertyAsList("in.story").size() == 0);
	}
	
	@Test
	public void testCreateStepViaSpec() {
		InstanceType typeJira = TestArtifacts.getJiraInstanceType(workspace);
		StepDefinition sd1 = StepDefinition.getInstance("S1", workspace);
		sd1.addExpectedInput("story", typeJira);
		ProcessStep s1 = ProcessStep.getInstance(workspace, sd1);
		Instance jira1 = workspace.createInstance("Jira1", typeJira);
		s1.getInstance().propertyAsList("in.story").add(jira1);
		workspace.commit();
		assert(s1.getInstance().propertyAsList("in.story").size() == 1);
	}

}
