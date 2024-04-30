package at.jku.isse.designspace;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import at.jku.isse.BaseSpringConfig;
import at.jku.isse.designspace.core.foundation.ServiceRegistry;
import at.jku.isse.designspace.core.model.LanguageWorkspace;
import at.jku.isse.designspace.core.model.ProjectWorkspace;
import at.jku.isse.designspace.rule.checker.ArlRuleEvaluator;
import at.jku.isse.designspace.rule.service.RuleService;
import at.jku.isse.passiveprocessengine.core.InstanceRepository;
import at.jku.isse.passiveprocessengine.core.SchemaRegistry;
import at.jku.isse.passiveprocessengine.designspace.DesignSpaceSchemaRegistry;
import at.jku.isse.passiveprocessengine.designspace.RuleServiceWrapper;

@ExtendWith(SpringExtension.class)
@SpringBootTest
class Designspace4Setup {

	
	@Autowired
	private DesignSpaceSchemaRegistry designspace;
	public InstanceRepository instanceRepository;
	public SchemaRegistry schemaRegistry;
	public RuleServiceWrapper ruleServiceWrapper;
	
	@BeforeEach
	protected
	void setup() throws Exception {
		assert(designspace != null);
		schemaRegistry = designspace;	
		instanceRepository = designspace;
		ruleServiceWrapper = new RuleServiceWrapper(designspace);
		BaseSpringConfig.reset();
	}

}
