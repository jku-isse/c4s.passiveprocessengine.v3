package at.jku.isse.passiveprocessengine.definition.registry;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;

import at.jku.isse.passiveprocessengine.definition.registry.ProcessRegistry.ProcessDeployResult;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FilesystemProcessDefinitionLoader {

	public static final JsonDefinitionSerializer serializer = new JsonDefinitionSerializer();

	protected ProcessRegistry registry;

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
                		ProcessDeployResult result = registry.createProcessDefinitionIfNotExisting(procD);
						if (result != null && !result.getDefinitionErrors().isEmpty()) {
							log.warn("Error loading process definition from file system: "+result.getProcDef().getName()+"\r\n"+result.getDefinitionErrors());
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
