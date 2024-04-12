package at.jku.isse.passiveprocessengine.definition.serialization;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.AbstractMap.SimpleEntry;
import java.util.List;

import at.jku.isse.passiveprocessengine.definition.ProcessDefinition;
import at.jku.isse.passiveprocessengine.definition.ProcessDefinitionError;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FilesystemProcessDefinitionLoader {

	public static final JsonDefinitionSerializer serializer = new JsonDefinitionSerializer();

	
	protected ProcessRegistry registry;
	
	// Added Code
	public ProcessRegistry getRegistry()
	{
		return this.registry;
	}
	// END
	
	public FilesystemProcessDefinitionLoader(ProcessRegistry registry) {
		this.registry = registry;
	}

	public int registerAll() {
		int i = 0;
        // external resources (same directory as JAR)
        try {
            File directory = new File("./processdefinitions/"); // no longer files in separate directories, all in a single one now
            FileFilter fileFilter = new FileFilter() {
				@Override
				public boolean accept(File pathname) {
					return pathname.toString().endsWith(".json");
				}}; //new WildcardFileFilter("*.json");
            File[] jsonFiles = directory.listFiles(fileFilter);
            for (File jsonFile : jsonFiles) {
                byte[] encoded = Files.readAllBytes(jsonFile.toPath());
                DTOs.Process procD = serializer.fromJson(new String(encoded, Charset.defaultCharset()));
                if (procD != null) {
                	try {
                		SimpleEntry<ProcessDefinition, List<ProcessDefinitionError>>  result = registry.storeProcessDefinition(procD, false);
						if (result != null && !result.getValue().isEmpty()) {
							log.warn("Error loading process definition from file system: "+result.getKey().getName()+"\r\n"+result.getValue());
						} else if (result == null) {
							log.info("Loading of process definition "+procD.getCode()+" defered to when workspace is available");
						}
                		i++;
					} catch (NullPointerException e) {
						e.printStackTrace();
						log.error(e.getMessage());
					}
                	
                }
            }
        } catch (NullPointerException | IOException e) {
            log.warn("No external process definitions found in ./processdefinitions/ ");
        }

        log.info("registered {} process definitions from folder ./processdefinitions/", i);
        return i;
	}

}
