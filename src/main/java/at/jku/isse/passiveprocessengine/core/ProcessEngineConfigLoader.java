package at.jku.isse.passiveprocessengine.core;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ProcessEngineConfigLoader {

	public static final String APPLICATION_PROPERTIES_LOCATION = "./application.properties";
	public static final String OVERRIDINGANALYSIS_ENABLED = "process.overridinganalysis.enabled";
	
	@Getter
	private boolean doOverridingAnalysis = false;
	
	@Getter
	private Properties props = new Properties();
	
	public ProcessEngineConfigLoader() {
		loadProperties();
	}
	
	private Properties loadProperties() {
		//try to load the config from the running directory
		try {
			FileReader reader = new FileReader(APPLICATION_PROPERTIES_LOCATION);
			props.load(reader);
			reader.close();
			doOverridingAnalysis = Boolean.parseBoolean(props.getProperty(OVERRIDINGANALYSIS_ENABLED, "false").trim());								
			return props;
		} catch (FileNotFoundException e) {
			log.warn(String.format("Configuration properties expected at %s but not found"
					, APPLICATION_PROPERTIES_LOCATION));
			return new Properties();
		} catch (IOException e) {
			throw new RuntimeException(String.format("Error loading configuration properties from  %s "
					, APPLICATION_PROPERTIES_LOCATION));
		}
	}
}
