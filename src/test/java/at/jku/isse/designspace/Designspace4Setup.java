package at.jku.isse.designspace;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import at.jku.isse.designspace.core.model.Tool;
import at.jku.isse.designspace.core.model.User;
import at.jku.isse.designspace.core.model.Workspace;
import at.jku.isse.designspace.core.service.WorkspaceService;
import at.jku.isse.passiveprocessengine.core.InstanceRepository;
import at.jku.isse.passiveprocessengine.core.SchemaRegistry;
import at.jku.isse.passiveprocessengine.designspace.DesignSpaceSchemaRegistry;
import at.jku.isse.passiveprocessengine.designspace.RuleServiceWrapper;

@ExtendWith(SpringExtension.class)
@SpringBootTest
class Designspace4Setup {


	private DesignSpaceSchemaRegistry designspace;
	public InstanceRepository instanceRepository;
	public SchemaRegistry schemaRegistry;
	public RuleServiceWrapper ruleServiceWrapper;
	
	@BeforeEach
	protected
	void setup() throws Exception {
		Workspace testWS = WorkspaceService.createWorkspace("test", WorkspaceService.PUBLIC_WORKSPACE, new User("Test"), new Tool("test", "v1.0"), false, false);					
		designspace = new DesignSpaceSchemaRegistry(testWS);
		assert(designspace != null);
		schemaRegistry = designspace;	
		instanceRepository = designspace;
		ruleServiceWrapper = new RuleServiceWrapper(designspace);
	}

}
