package at.jku.isse;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;


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
