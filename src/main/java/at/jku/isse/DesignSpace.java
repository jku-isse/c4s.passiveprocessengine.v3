package at.jku.isse;

import at.jku.isse.designspace.core.controlflow.ControlEventEngine;
import at.jku.isse.designspace.core.events.Event;
import at.jku.isse.designspace.core.model.*;
import at.jku.isse.designspace.endpoints.grpc.service.GrpcUtils;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PreDestroy;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

@RestController
@SpringBootApplication
@CrossOrigin(origins = "*")//to allow incoming calls from other ports
public class DesignSpace implements ApplicationListener<ApplicationReadyEvent> {
    public static String persistenceFileName = null;
    public static boolean load = false;
    public static boolean save = false;


    public static void main(String[] args) {
        if (args.length>0) {
            if (args[0].equals("-capture"))
                GrpcUtils.captureFileName=args[1];
            else if (args[0].equals("-replay"))
                GrpcUtils.replayFileName=args[1];
        }

        SpringApplication application = new SpringApplication(DesignSpace.class);
        application.setBanner(new CustomBanner());
        application.run(args);

        var user = User.users.get(1l);
        user.clearNotifications();

        initializePersistence();

        System.out.println("Successfully initialized!");
        System.out.println("======================================================================================================");
    }

    public static void initializePersistence() {
        try {
            ClassLoader contextLoader = Thread.currentThread().getContextClassLoader();

            InputStream inApp = contextLoader.getResourceAsStream("application.properties");
            Properties appProps = new Properties();
            appProps.load(inApp);
            String property = appProps.getProperty("persistence.enabled");
            if (property != null) {
                boolean persistenceEnabled = Boolean.parseBoolean(property);
                if (persistenceEnabled) {
                    ControlEventEngine.initWithPath(PublicWorkspace.persistencePath());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onApplicationEvent(final ApplicationReadyEvent event) {
        Event.setInitialized();

        List<String> filesToReplay =new ArrayList<String>();
        if (PublicWorkspace.replayAtStartup().size()>0) filesToReplay.addAll(PublicWorkspace.replayAtStartup());
        if (GrpcUtils.replayFileName!=null) filesToReplay.add(GrpcUtils.replayFileName);
        GrpcUtils.replayMultipleCaptures(filesToReplay);

    }

    @PreDestroy
    public void onExit() {
        GrpcUtils.closeCapture();
    }

    private static class CustomBanner implements Banner {
        private final String ANSI_BLUE = "\u001B[34m";
        private final String ANSI_RESET = "\u001B[0m";
        private final String ANSI_STYLE_BOLD = "\u001b[1m";

        @Override
        public void printBanner(Environment environment, Class<?> sourceClass, PrintStream out) {
            out.print(ANSI_BLUE);
            out.print(ANSI_STYLE_BOLD);
            out.println("======================================================================================================");
            out.println("  ____                 _                   ____                                    _  _          ___\n" +
                    " |  _ \\    ___   ___  (_)   __ _   _ __   / ___|   _ __     __ _    ___    ___    | || |        / _ \\\n" +
                    " | | | |  / _ \\ / __| | |  / _` | | '_ \\  \\___ \\  | '_ \\   / _` |  / __|  / _ \\   | || |_      | | | |\n" +
                    " | |_| | |  __/ \\__ \\ | | | (_| | | | | |  ___) | | |_) | | (_| | | (__  |  __/   |__   _|  _  | |_| |\n" +
                    " |____/   \\___| |___/ |_|  \\__, | |_| |_| |____/  | .__/   \\__,_|  \\___|  \\___|      |_|   (_)  \\___/\n" +
                    "                           |___/                  |_|");
            String port = "";
            String webserviceport = "";
            String buildTime = "";
            String commitId = "";
            try {
                ClassLoader contextLoader = Thread.currentThread().getContextClassLoader();

                InputStream inApp = contextLoader.getResourceAsStream("application.properties");
                Properties appProps = new Properties();
                appProps.load(inApp);
                port = appProps.getProperty("grpc.port");
                webserviceport = appProps.getProperty("server.port");

                InputStream inGit = contextLoader.getResourceAsStream("git.properties");
                Properties gitProps = new Properties();
                gitProps.load(inGit);
                buildTime = gitProps.getProperty("git.build.time").replace("\\", "");
                commitId = gitProps.getProperty("git.commit.id.abbrev");
            } catch (Exception e) {
                //out.println(e.getMessage());
            }
            out.println("                                                              [GRPC-Port: " + port +"] [Service-Port: " + webserviceport + "]");
            out.println("------------------------------------------------------------------------------------------------------");
            out.println(" Commit:       " + commitId);
            out.println(" Build-Time:   " + buildTime);
            out.println(" Java-Version: " + System.getProperty("java.version"));
            out.println("======================================================================================================");
            out.println(ANSI_RESET);
        }
    }

}
