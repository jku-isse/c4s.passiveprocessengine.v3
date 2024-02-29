package at.jku.isse;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

import at.jku.isse.designspace.core.foundation.ServiceRegistry;
import at.jku.isse.designspace.core.model.DesignSpace;
import at.jku.isse.designspace.core.model.LanguageWorkspace;
import at.jku.isse.designspace.core.model.ProjectWorkspace;
import at.jku.isse.designspace.rule.checker.ArlRuleEvaluator;
import at.jku.isse.designspace.rule.service.RuleService;


@Configuration
public class PPECoreSpringConfig {

	public static boolean isInit = false;

	private static void init() {
		if (!isInit) {
			DesignSpace.init();
			isInit = true;
		}
	}

	public static void reset() {
		isInit = false;
	}
	
	
	
	@Bean
	public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
		return new PropertySourcesPlaceholderConfigurer();
	}


	@Bean 
	public static LanguageWorkspace getLanguageWorkspace() {
		init();
		return LanguageWorkspace.ROOT;
	}

	@Bean 
	public static ProjectWorkspace getProjectWorkspace() {
		init();
		return ProjectWorkspace.ROOT;
	}

}
