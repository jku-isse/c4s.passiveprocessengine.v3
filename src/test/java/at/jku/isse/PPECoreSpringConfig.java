package at.jku.isse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

import at.jku.isse.designspace.core.model.Tool;
import at.jku.isse.designspace.core.model.User;
import at.jku.isse.designspace.core.model.Workspace;
import at.jku.isse.designspace.core.service.WorkspaceService;
//import at.jku.isse.designspace.jira.service.IJiraService;
import at.jku.isse.passiveprocessengine.monitoring.ProcessQAStatsMonitor;
import at.jku.isse.passiveprocessengine.monitoring.UsageMonitor;


@Configuration
public class PPECoreSpringConfig {

        @Bean
        public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
                return new PropertySourcesPlaceholderConfigurer();
        }

    	       
//        @Bean
//        Workspace getWorkspace(IJiraService jira) {        	
//    		//return WorkspaceService.createWorkspace("test", WorkspaceService.PUBLIC_WORKSPACE,new User("test"), new Tool("test", "v1"), false, false);
//        	return WorkspaceService.PUBLIC_WORKSPACE;
//    	}
        
}
