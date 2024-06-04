package at.jku.isse.passiveprocessengine.instance;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import at.jku.isse.passiveprocessengine.core.PPEInstance;
import at.jku.isse.passiveprocessengine.core.PPEInstanceType;
import at.jku.isse.passiveprocessengine.core.ProcessContext;
import at.jku.isse.passiveprocessengine.core.ProcessInstanceChangeListener;
import at.jku.isse.passiveprocessengine.core.serialization.PPEInstanceGsonAdapter;
import at.jku.isse.passiveprocessengine.core.serialization.TypeAdapterRegistry;
import at.jku.isse.passiveprocessengine.core.serialization.TypeAdapterRouter;
import at.jku.isse.BaseSpringConfig;
import at.jku.isse.passiveprocessengine.core.ChangeEventTransformer;
import at.jku.isse.passiveprocessengine.definition.activeobjects.ProcessDefinition;
import at.jku.isse.passiveprocessengine.definition.serialization.DTOs;
import at.jku.isse.passiveprocessengine.definition.serialization.DefinitionTransformer;
import at.jku.isse.passiveprocessengine.demo.TestArtifacts;
import at.jku.isse.passiveprocessengine.demo.TestArtifacts.JiraStates;
import at.jku.isse.passiveprocessengine.demo.TestDTOProcesses;
import at.jku.isse.passiveprocessengine.designspace.WorkspaceListenerWrapper;
import at.jku.isse.passiveprocessengine.instance.StepLifecycle.State;
import at.jku.isse.passiveprocessengine.instance.activeobjects.DecisionNodeInstance;
import at.jku.isse.passiveprocessengine.instance.activeobjects.ProcessInstance;
import at.jku.isse.passiveprocessengine.instance.activeobjects.ProcessStep;
import at.jku.isse.passiveprocessengine.instance.messages.EventDistributor;
import at.jku.isse.passiveprocessengine.instance.messages.Responses.IOResponse;
import at.jku.isse.passiveprocessengine.instance.serialization.ProcessInstanceTypeAdapterRegistryFactory;
import at.jku.isse.passiveprocessengine.monitoring.CurrentSystemTimeProvider;
import at.jku.isse.passiveprocessengine.monitoring.ProcessQAStatsMonitor;
import at.jku.isse.passiveprocessengine.monitoring.ProcessStats;
import at.jku.isse.passiveprocessengine.wrappers.DefinitionWrapperTests;
import lombok.NonNull;

@ExtendWith(SpringExtension.class)
@SpringBootTest
public
class InstanceSerializationTests extends InstanceTests {
	
	Gson gson;
	
	@BeforeEach
	public
	void setup() throws Exception {
		super.setup();
		gson = getGson(getTypeAdapterRegistry(configBuilder.getContext()));
	}


	public TypeAdapterRegistry getTypeAdapterRegistry(ProcessContext context) {
		return ProcessInstanceTypeAdapterRegistryFactory.buildRegistry(context);
	}
	
	public Gson getGson(TypeAdapterRegistry typeAdapterRegistry) {
		TypeAdapterRouter router = new TypeAdapterRouter(typeAdapterRegistry);
		Gson gson = new GsonBuilder()
				 .registerTypeHierarchyAdapter(PPEInstance.class, router)
				 .setPrettyPrinting()
				 .create();
		return gson;
	}
	

	@Test
	void testComplexDataMappingSerialization() throws ProcessException {
		PPEInstance jiraB =  artifactFactory.getJiraInstance("jiraB");
		PPEInstance jiraC = artifactFactory.getJiraInstance("jiraC");		
		PPEInstance jiraA = artifactFactory.getJiraInstance("jiraA", jiraB, jiraC);
						
		ProcessInstance proc =  instantiateDefaultProcess(procFactory.getSimple2StepProcessDefinition(), jiraA);		
		super.instanceRepository.concludeTransaction();
		proc.printProcessToConsole(" ");
		assert(proc.getProcessSteps().stream()
				.filter(step -> step.getDefinition().getName().equals(TestDTOProcesses.SD1) )
				.allMatch(step -> step.getOutput(TestDTOProcesses.JIRA_OUT).size() == 2));
		
		String json = gson.toJson(proc.getInstance());
		System.out.println(json);
	}
	
	
	
}
