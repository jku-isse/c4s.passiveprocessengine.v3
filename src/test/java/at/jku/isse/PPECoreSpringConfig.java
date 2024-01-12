package at.jku.isse;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

import at.jku.isse.passiveprocessengine.instance.messages.EventDistributor;
import at.jku.isse.passiveprocessengine.monitoring.CurrentSystemTimeProvider;
import at.jku.isse.passiveprocessengine.monitoring.ITimeStampProvider;
import at.jku.isse.passiveprocessengine.monitoring.ProcessMonitor;
import at.jku.isse.passiveprocessengine.monitoring.ProcessQAStatsMonitor;
import at.jku.isse.passiveprocessengine.monitoring.UsageMonitor;


@Configuration
public class PPECoreSpringConfig {

        @Bean
        public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
                return new PropertySourcesPlaceholderConfigurer();
        }
    	     
        @Bean 
        public ITimeStampProvider getTimeStampProvider() {
        	return new CurrentSystemTimeProvider();
        }
        
        @Bean 
        public UsageMonitor getUsageMonitor(ITimeStampProvider timeprovider) {
        	return new UsageMonitor(timeprovider);
        }
        
        @Bean
        public ProcessQAStatsMonitor getProcessQAStatsMonitor(EventDistributor ed) {
        	ProcessQAStatsMonitor qaMonitor = new ProcessQAStatsMonitor();
        	ed.registerHandler(qaMonitor);
        	return qaMonitor;
        }
        
        @Bean
        public EventDistributor getEventDistributor(ITimeStampProvider timeprovider) {
        	EventDistributor ed = new EventDistributor();
        	ed.registerHandler(new ProcessMonitor(timeprovider));
        	return ed;
        }
}
