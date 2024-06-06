package at.jku.isse.passiveprocessengine.instance;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import at.jku.isse.passiveprocessengine.core.PPEInstance;
import at.jku.isse.passiveprocessengine.core.ProcessContext;
import at.jku.isse.passiveprocessengine.core.serialization.TypeAdapterRegistry;
import at.jku.isse.passiveprocessengine.core.serialization.TypeAdapterRouter;
import at.jku.isse.passiveprocessengine.demo.TestDTOProcesses;
import at.jku.isse.passiveprocessengine.instance.activeobjects.ProcessInstance;
import at.jku.isse.passiveprocessengine.instance.serialization.ProcessInstanceTypeAdapterRegistryFactory;

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
		assert(json != null);
	}
	
	
	
}
