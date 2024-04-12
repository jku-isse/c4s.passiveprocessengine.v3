package at.jku.isse;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.Properties;
import java.util.Set;

import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;

import at.jku.isse.designspace.core.events.Event;
import at.jku.isse.designspace.core.model.Folder;
import at.jku.isse.designspace.core.model.Instance;
import at.jku.isse.designspace.core.model.InstanceType;
import at.jku.isse.designspace.core.model.User;
import at.jku.isse.designspace.core.model.Workspace;
import at.jku.isse.designspace.core.service.ServiceRegistry;
import at.jku.isse.designspace.core.service.WorkspaceService;
import at.jku.isse.designspace.rule.arl.repair.changepropagation.ParallelGraphGenerator;
import at.jku.isse.designspace.rule.model.ConsistencyRuleType;

@SpringBootApplication
public class PPECoreDesignSpace  implements ApplicationListener<ApplicationReadyEvent> {
    public static String persistenceFileName = null;
    public static boolean load = false;
    public static boolean save = false;
    static Workspace workspace;
    static Folder instancesFolder;
    static InstanceType classType, operationType, messageType, transitionType, lifelineType;
    ConsistencyRuleType crd1,crd2,crd3;
    protected Instance classRobotArm, classGripper, operationGrabOrRelease, operationMove, messageGrabOrRelease1,
            messageGrabOrRelease2, transitionGrabOrRelease1, transitionGrabOrRelease2,lifeLine;
    ParallelGraphGenerator parallelGraphGenerator;

    static Set<ConsistencyRuleType> crds;
    static boolean inconsistentPaths,  abstractStates, conflicts, pastChanges, pastIcon;

    public static void main(String[] args) {

        SpringApplication application = new SpringApplication(PPECoreDesignSpace.class);
        application.setBanner(new CustomBanner());
        application.run(args);

        var user = User.users.get(1l);
        user.clearNotifications();

        System.out.println("Successfully initialized!");
        System.out.println("======================================================================================================");
    }

    @Override
    public void onApplicationEvent(final ApplicationReadyEvent event) {
    	ServiceRegistry.initializeAllPersistenceUnawareServices();
        WorkspaceService.PUBLIC_WORKSPACE.concludeTransaction();

        Event.setInitialized();

        ServiceRegistry.initializeAllPersistenceAwareServices();
        WorkspaceService.PUBLIC_WORKSPACE.concludeTransaction();
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
                port = "no longer used"; //appProps.getProperty("grpc.port");
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
