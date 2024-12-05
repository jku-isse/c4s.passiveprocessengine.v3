package at.jku.isse;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

import at.jku.isse.designspace.artifactconnector.core.monitoring.IProgressObserver;
import at.jku.isse.designspace.artifactconnector.core.monitoring.ProgressEntry;
import at.jku.isse.passiveprocessengine.core.DesignspaceTestSetup;
import at.jku.isse.passiveprocessengine.rdfwrapper.RDFWrapperSetup;


@Configuration
public class WrapperTestSpringConfig {

		
	
        @Bean
        public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
                return new PropertySourcesPlaceholderConfigurer();
        }

        @Bean
        public static  IProgressObserver getProgressObserver() {
        	return new IProgressObserver() {

				@Override
				public void dispatchNewEntry(ProgressEntry entry) {
					//noop
				}

				@Override
				public void updatedEntry(ProgressEntry entry) {
					//noop
				}
        		
        	};
        }
               
        @Bean
        public DesignspaceTestSetup getDesignSpaceTestSetup() {
        	return new RDFWrapperSetup();
        }
       
}
