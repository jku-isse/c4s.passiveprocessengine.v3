package at.jku.isse.designspace;

import at.jku.isse.designspace.core.controlflow.ControlEventEngine;
import at.jku.isse.designspace.core.events.Event;
import at.jku.isse.designspace.core.trees.collaboration.CollaborationTree;
import at.jku.isse.designspace.endpoints.grpc.service.GrpcUtils;
import at.jku.isse.designspace.rule.arl.repair.Operator;
import at.jku.isse.designspace.rule.arl.repair.changepropagation.Change;
import at.jku.isse.designspace.rule.arl.repair.changepropagation.ModelState;
import at.jku.isse.designspace.rule.arl.repair.changepropagation.ParallelGraphGenerator;
import at.jku.isse.designspace.rule.checker.ArlRuleEvaluator;
import at.jku.isse.designspace.rule.model.ConsistencyRule;
import at.jku.isse.designspace.rule.model.ConsistencyRuleType;
import at.jku.isse.designspace.rule.service.RuleService;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;

import at.jku.isse.designspace.core.model.*;
import at.jku.isse.designspace.core.service.WorkspaceService;
import at.jku.isse.designspace.core.trees.operation.OperationTree;

import org.springframework.boot.Banner;
import org.springframework.core.env.Environment;


import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import javax.annotation.PreDestroy;

@SpringBootApplication
public class DesignSpace  implements ApplicationListener<ApplicationReadyEvent> {
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
        if (args.length>0) {
            if (args[0].equals("-capture"))
                GrpcUtils.captureFileName=args[1];
            else if (args[0].equals("-replay"))
                GrpcUtils.replayFileName=args[1];
        }

        SpringApplication application = new SpringApplication(DesignSpace.class);
        application.setBanner(new CustomBanner());
        application.run(args);

        //ControlEventEngine.initWithPath(PublicWorkspace.persistencePath());

        var user = User.users.get(1l);
        user.clearNotifications();

        System.out.println("Successfully initialized!");
        System.out.println("======================================================================================================");
    }

    @Override
    public void onApplicationEvent(final ApplicationReadyEvent event) {
        Event.setInitialized();

        List<String> filesToReplay =new ArrayList<String>();
        if (PublicWorkspace.replayAtStartup().size()>0) filesToReplay.addAll(PublicWorkspace.replayAtStartup());
        if (GrpcUtils.replayFileName!=null) filesToReplay.add(GrpcUtils.replayFileName);
        GrpcUtils.replayMultipleCaptures(filesToReplay);

        //init1();
        //init2();
        //init3();
        //init4();
        //init5();
        //init6();
        //init7();
        //init8();
        //createTestScenario(); // scenarios to test the type view and the instance view in dashboard
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

    private void createTestScenario() {
        Workspace ws = WorkspaceService.createWorkspace(
                "test scenario",
                WorkspaceService.PUBLIC_WORKSPACE,
                WorkspaceService.PUBLIC_WORKSPACE.user(),
                null,
                false,
                false
        );
       
    }

    private static void init9(){
        var user = WorkspaceService.registerUser("user");
        Workspace wsABCDE = WorkspaceService.createWorkspace("wsABCDE", null, user, null, false, false);
        InstanceType instanceType = WorkspaceService.createInstanceType(wsABCDE, "Type", wsABCDE.TYPES_FOLDER);
        wsABCDE.concludeTransaction();

        Workspace wsAB = WorkspaceService.createWorkspace("wsAB", wsABCDE, user, null, false, false);
        Workspace wsCD = WorkspaceService.createWorkspace("wsCD", wsABCDE, user, null, false, false);
        Workspace wsA = WorkspaceService.createWorkspace("wsA", wsAB, user, null, false, false);
        Workspace wsB = WorkspaceService.createWorkspace("wsB", wsAB, user, null, false, false);
        Workspace wsC = WorkspaceService.createWorkspace("wsC", wsCD, user, null, false, false);
        Workspace wsD = WorkspaceService.createWorkspace("wsD", wsCD, user, null, false, false);
        Workspace wsE = WorkspaceService.createWorkspace("wsE", wsABCDE, user, null, false, false);
        Workspace wsA1 = WorkspaceService.createWorkspace("wsA1", wsA, user, null, false, false);
        Workspace wsA2 = WorkspaceService.createWorkspace("wsA2", wsA, user, null, false, false);

        Instance iwsABCDE = wsABCDE.createInstance(instanceType, "iwsABCDE");
        Instance iwsAB = wsAB.createInstance(wsAB.its(instanceType), "iwsAB");
        Instance iwsA = wsA.createInstance(wsA.its(instanceType), "iwsA");
        Instance iwsA1 = wsA1.createInstance(wsA1.its(instanceType), "iwsA1");
        Instance iwsA2 = wsA2.createInstance(wsA2.its(instanceType), "iwsA2");
        Instance iwsB = wsB.createInstance(wsB.its(instanceType), "iwsB");
        Instance iwsCD = wsCD.createInstance(wsCD.its(instanceType), "iwsCD");
        Instance iwsC = wsC.createInstance(wsC.its(instanceType), "iwsC");
        Instance iwsD = wsD.createInstance(wsD.its(instanceType), "iwsD");
        Instance iwsE = wsE.createInstance(wsE.its(instanceType), "iwsE");


        wsA1.concludeTransaction();
        wsA1.commit();

        //wsE.concludeTransaction();
        //wsE.commit();

        //wsCD.concludeTransaction();
        //wsCD.commit();


        //wsA2.update();
        // wsC.update();
        //wsCD.update();

        //wsE.update();
    }

    private static void init1(){
        var type = WorkspaceService.PUBLIC_WORKSPACE.createInstanceType("person",WorkspaceService.PUBLIC_WORKSPACE.TYPES_FOLDER);
        var proptype = type.createPropertyType("age",Cardinality.SINGLE,WorkspaceService.PUBLIC_WORKSPACE.INTEGER);
        var proptypeList = type.createPropertyType("animals",Cardinality.LIST,WorkspaceService.PUBLIC_WORKSPACE.STRING);
        var instance = WorkspaceService.PUBLIC_WORKSPACE.createInstance(type, "First");
        var instanceS = WorkspaceService.PUBLIC_WORKSPACE.createInstance(type, "second");
        WorkspaceService.PUBLIC_WORKSPACE.concludeTransaction();

        Workspace A = WorkspaceService.createWorkspace("A", WorkspaceService.PUBLIC_WORKSPACE, WorkspaceService.PUBLIC_WORKSPACE.user(), null, false, false);

        instanceS.getPropertyAsSingle("age").set(111);
        instanceS.getPropertyAsSingle("age").set(222);
        instance.getPropertyAsSingle("age").set(10);
        instance.getPropertyAsSingle("age").set(20);
        WorkspaceService.PUBLIC_WORKSPACE.concludeTransaction();

        var instanceA = A.findElement(instance.id());
        instanceA.getPropertyAsSingle("age").set(5);
        instanceA.getPropertyAsSingle("age").set(8);
        A.concludeTransaction();

        Workspace AA = WorkspaceService.createWorkspace("AA", A, A.user(), null, false, false);
        var instanceAA = AA.findElement(instance.id());
        instanceAA.getPropertyAsSingle("age").set(0);
        AA.concludeTransaction();

        Workspace AB = WorkspaceService.createWorkspace("AB", A, A.user(), null, false, false);
        var instanceAB = AB.findElement(instance.id());
        instanceAB.getPropertyAsSingle("age").set(12);
        AB.createInstance(AB.its(type), "ABfirst");
        AB.concludeTransaction();

        instanceA.getPropertyAsSingle("age").set(1);

        instanceA.getPropertyAsList("animals").add("dog");

        A.concludeTransaction();

        instanceA.getPropertyAsList("animals").add("cat");
        instanceA.getPropertyAsList("animals").add("tiger");
        instanceA.getPropertyAsList("animals").add("snake");


        var countOperations = OperationTree.getInstance().countOperations();
        var countOperationsWs = A.countOperations();
        var countElementsA = A.countElements();
        var countElementsAB = AB.countElements();
        //A.concludeTransaction();

        // A.update();
        //A.commit();
    }

    private static void init3(){
        var user = WorkspaceService.registerUser("user");
        var wsABC = WorkspaceService.createWorkspace("wsABC", null, user, null, false, false);
        var type1_P = WorkspaceService.createInstanceType(wsABC, "InstanceType1", wsABC.TYPES_FOLDER);
        WorkspaceService.createPropertyType(wsABC, wsABC.its(type1_P), "number", Cardinality.SINGLE, wsABC.INTEGER);
        WorkspaceService.concludeWorkspaceTransaction(wsABC);

        Element i1 = WorkspaceService.createInstance(wsABC, "1", wsABC.its(type1_P));
        WorkspaceService.concludeWorkspaceTransaction(wsABC);


        var wsAB = WorkspaceService.createWorkspace("wsAB", wsABC, user, null, true, false);


        Element i2 = WorkspaceService.createInstance(wsABC, "2", wsABC.its(type1_P));
        WorkspaceService.concludeWorkspaceTransaction(wsABC);


        Element i3 = WorkspaceService.createInstance(wsAB, "3", wsAB.its(type1_P));
        WorkspaceService.concludeWorkspaceTransaction(wsAB);


        var wsA = WorkspaceService.createWorkspace("waA", wsAB, user, null, false, false);
        wsA.concludeTransaction();

        Element i4 = WorkspaceService.createInstance(wsABC, "4", wsABC.its(type1_P));
        //WorkspaceService.concludeWorkspaceTransaction(wsABC);


        //Element i5 = WorkspaceService.createInstance(wsAB, "5", wsAB.its(type1_P));
        //WorkspaceService.concludeWorkspaceTransaction(wsAB);
    }

    private static void init2(){
        Workspace wsFirst = WorkspaceService.createWorkspace("ABC", WorkspaceService.PUBLIC_WORKSPACE, WorkspaceService.PUBLIC_WORKSPACE.user(), null, false, false);
        Workspace wsSecond = WorkspaceService.createWorkspace("D", WorkspaceService.PUBLIC_WORKSPACE, WorkspaceService.PUBLIC_WORKSPACE.user(), null, false, false);
        Workspace wsE = WorkspaceService.createWorkspace("E", WorkspaceService.PUBLIC_WORKSPACE, WorkspaceService.PUBLIC_WORKSPACE.user(), null, false, false);

        Workspace AB = WorkspaceService.createWorkspace("AB", wsFirst, WorkspaceService.PUBLIC_WORKSPACE.user(), null, false, false);
        Workspace A = WorkspaceService.createWorkspace("A", AB, WorkspaceService.PUBLIC_WORKSPACE.user(), null, false, false);
        Workspace B = WorkspaceService.createWorkspace("B", AB, WorkspaceService.PUBLIC_WORKSPACE.user(), null, false, false);

        WorkspaceService.createWorkspace("C", wsFirst, WorkspaceService.PUBLIC_WORKSPACE.user(), null, false, false);

        InstanceType superType = WorkspaceService.createInstanceType(wsFirst,"Type1",wsFirst.TYPES_FOLDER);
        PropertyType expectedPropertyType = WorkspaceService.createPropertyType(wsFirst, superType, "primitive", Cardinality.SINGLE, wsFirst.STRING);
        wsFirst.concludeTransaction();

        WorkspaceService.createInstanceType(wsSecond,"Type2",wsSecond.TYPES_FOLDER);
        wsSecond.concludeTransaction();

        WorkspaceService.createInstanceType(wsE,"TypeE",wsE.TYPES_FOLDER);
        wsE.concludeTransaction();

        WorkspaceService.createInstanceType(A,"TypeA",A.TYPES_FOLDER);
        A.concludeTransaction();

        InstanceType btype = WorkspaceService.createInstanceType(B,"TypeB",B.TYPES_FOLDER);
        btype.createPropertyType("age",Cardinality.SINGLE, B.INTEGER);
        Element insPersonB = B.createInstance(btype,"Person");
        insPersonB.getPropertyAsSingle("age").set(20l);
        B.concludeTransaction();
        OperationTree.getInstance().commit(B);
        OperationTree.getInstance().update(A);
        B.commit();
        A.update();
        insPersonB.getPropertyAsSingle("age").set(30l);
        B.concludeTransaction();

        //Element insPersonA = WorkspaceService.getElement(A,insPersonB.id());
        //insPersonA.getPropertyAsSingle("age").set(40);
        //A.concludeTransaction();

        var elementType = WorkspaceService.PUBLIC_WORKSPACE.TYPES_FOLDER.instanceTypes();
        var instances = WorkspaceService.PUBLIC_WORKSPACE.TYPES_FOLDER.instances();
    }

    private static void init4(){
        var user = WorkspaceService.registerUser("user");

        var workspaceP = WorkspaceService.createWorkspace("wsP",null, user, null, true, false);

        var type1 = WorkspaceService.createInstanceType(workspaceP, "InstanceTypeA", workspaceP.TYPES_FOLDER);
        var type2 = WorkspaceService.createInstanceType(workspaceP,"InstanceTypeB", workspaceP.TYPES_FOLDER);

        WorkspaceService.createPropertyType(workspaceP, type1, "Property1", Cardinality.SINGLE, type1);
        WorkspaceService.createPropertyType(workspaceP, type2, "Property2", Cardinality.SINGLE, workspaceP.STRING);

        workspaceP.concludeTransaction();

        var workspaceAB = WorkspaceService.createWorkspace("wsAB", workspaceP, user, null, true, false);

        var workspaceA = WorkspaceService.createWorkspace("wsA", workspaceAB, user, null, true, false);


        Element instanceA = WorkspaceService.createInstance(workspaceA, "", workspaceA.its(type2));
        WorkspaceService.getPropertyAsSingle(workspaceA, instanceA, "Property2").set("initialValue");
        workspaceA.concludeTransaction();
        WorkspaceService.commitWorkspace(workspaceA);

        Element instanceAB = WorkspaceService.getElement(workspaceAB, instanceA.id());
        WorkspaceService.getPropertyAsSingle(workspaceAB, instanceAB, "Property2").set("updatedValue");
        WorkspaceService.concludeWorkspaceTransaction(workspaceAB);

        Element instanceOfWorkspaceAB = WorkspaceService.getElement(workspaceAB, instanceAB.id());
        Element instanceOfWorkspaceA = WorkspaceService.getElement(workspaceA, instanceAB.id());
        String valueInWorkspaceAB = (String) WorkspaceService.getPropertyAsSingle(workspaceAB, instanceOfWorkspaceAB, "Property2").get();
        String valueInWorkspaceA = (String) WorkspaceService.getPropertyAsSingle(workspaceA, instanceOfWorkspaceA, "Property2").get();
        //assertEquals("updatedValue", valueInWorkspaceAB);
        //assertEquals("updatedValue", valueInWorkspaceA);

        WorkspaceService.getPropertyAsSingle(workspaceAB, instanceAB, "Property2").set("newestValue");
        //WorkspaceService.concludeWorkspaceTransaction(workspaceAB);
    }

    private static void init5(){

        var user = WorkspaceService.registerUser("user");

        var workspaceP = WorkspaceService.createWorkspace("wsP",null, user, null, true, false);

        var type1 = WorkspaceService.createInstanceType(workspaceP, "InstanceTypeA", workspaceP.TYPES_FOLDER);
        var type2 = WorkspaceService.createInstanceType(workspaceP,"InstanceTypeB", workspaceP.TYPES_FOLDER);

        WorkspaceService.createPropertyType(workspaceP, type1, "Property1", Cardinality.SINGLE, type1);
        WorkspaceService.createPropertyType(workspaceP, type2, "Property2", Cardinality.SINGLE, workspaceP.STRING);

        workspaceP.concludeTransaction();

        var workspaceAB = WorkspaceService.createWorkspace("wsAB", workspaceP, user, null, true, false);

        var workspaceA = WorkspaceService.createWorkspace("wsA", workspaceAB, user, null, true, false);


        InstanceType typeInChild = WorkspaceService.createInstanceType(workspaceA, "Type", workspaceA.TYPES_FOLDER);
        WorkspaceService.createPropertyType(workspaceA, typeInChild, "Property1", Cardinality.SINGLE, workspaceA.STRING);

        WorkspaceService.concludeWorkspaceTransaction(workspaceA);

        Element instanceInB = WorkspaceService.createInstance(workspaceA, "", typeInChild);
        WorkspaceService.getPropertyAsSingle(workspaceA, instanceInB, "Property1").set("valueB");

        try {
            Element instanceInA = WorkspaceService.createInstance(workspaceAB, "", workspaceAB.its(typeInChild));
            //assertNotNull(null, "We should not be able to create instance of type created in child workspace");
        } catch (Exception ex) {
        }

        workspaceA.concludeTransaction();
        WorkspaceService.commitWorkspace(workspaceA);

        Element instanceInA = WorkspaceService.createInstance(workspaceAB, "", workspaceAB.its(typeInChild));
        WorkspaceService.getPropertyAsSingle(workspaceAB, instanceInA, "Property1").set("newvalueA");
        workspaceAB.concludeTransaction();
        WorkspaceService.commitWorkspace(workspaceAB);

        instanceInA = WorkspaceService.getElement(workspaceAB, instanceInA.id());
        //assertEquals("newvalueA", WorkspaceService.getPropertyAsSingle(workspaceAB, instanceInA, "Property1").get());
    }

    private static void init6(){
        var user = WorkspaceService.registerUser("user");
        var wsABC = WorkspaceService.createWorkspace("wsABC", null, user, null, false, false);
        var type1_P = WorkspaceService.createInstanceType(wsABC, "InstanceType1", wsABC.TYPES_FOLDER);
        WorkspaceService.createPropertyType(wsABC, wsABC.its(type1_P), "number", Cardinality.SINGLE, wsABC.INTEGER);
        WorkspaceService.concludeWorkspaceTransaction(wsABC);

        Workspace wsAB = WorkspaceService.createWorkspace("wsAB", wsABC, user, null, false, false);
        Element p1 = WorkspaceService.createInstance(wsAB, "p1", wsAB.its(type1_P));
        WorkspaceService.concludeWorkspaceTransaction(wsAB);

        Workspace wsA = WorkspaceService.createWorkspace("wsA", wsAB, user, null, true, true);
        Workspace wsB = WorkspaceService.createWorkspace("wsB", wsAB, user, null, false, false);
        WorkspaceService.concludeWorkspaceTransaction(wsA);
        WorkspaceService.concludeWorkspaceTransaction(wsB);


        Element p2 = WorkspaceService.createInstance(wsAB, "p2", wsAB.its(type1_P));
        WorkspaceService.concludeWorkspaceTransaction(wsAB);


        Element a1 = WorkspaceService.createInstance(wsA, "a1", wsA.its(type1_P));
        WorkspaceService.concludeWorkspaceTransaction(wsA);


        Element b1 = WorkspaceService.createInstance(wsB, "b1", wsB.its(type1_P));
        WorkspaceService.concludeWorkspaceTransaction(wsB);

    }

    private static void init7(){

        var user = WorkspaceService.registerUser("user");
        var tool = WorkspaceService.registerTool("dashtest", "0.1Alpha");
        var wsABC = WorkspaceService.createWorkspace("wsABC", null, user, null, false, false);
        var type1_P = WorkspaceService.createInstanceType(wsABC, "InstanceType1", wsABC.TYPES_FOLDER);
        WorkspaceService.createPropertyType(wsABC, wsABC.its(type1_P), "number", Cardinality.SINGLE, wsABC.INTEGER);
        WorkspaceService.concludeWorkspaceTransaction(wsABC);
        ///
        Element i1 = WorkspaceService.createInstance(wsABC, "1", wsABC.its(type1_P));
        WorkspaceService.concludeWorkspaceTransaction(wsABC);


        var wsAB = WorkspaceService.createWorkspace("wsAB", wsABC, user, null, false, false);


        Element i2 = WorkspaceService.createInstance(wsABC, "2", wsABC.its(type1_P));
        WorkspaceService.concludeWorkspaceTransaction(wsABC);


        Element i3 = WorkspaceService.createInstance(wsAB, "3", wsAB.its(type1_P));
        WorkspaceService.concludeWorkspaceTransaction(wsAB);


        var wsA = WorkspaceService.createWorkspace("waA", wsAB, user, null, false, false);


        Element i4 = WorkspaceService.createInstance(wsABC, "4", wsABC.its(type1_P));
        WorkspaceService.concludeWorkspaceTransaction(wsABC);

        Element i5 = WorkspaceService.createInstance(wsABC, "5", wsABC.its(type1_P));
        //WorkspaceService.concludeWorkspaceTransaction(wsABC);

        wsAB.update();

        //Element i5 = WorkspaceService.createInstance(wsAB, "5", wsAB.its(type1_P));
        //WorkspaceService.concludeWorkspaceTransaction(wsAB);
    }

    protected void checkRule(ConsistencyRuleType crd) {
        if (crd.hasRuleError()) {
            System.out.println("Rule was invalid: " + crd.ruleError());
        }
    }

    private String changePropagationScenario(boolean cr1, boolean cr2, boolean cr3, boolean filterByOwner){
        // setup
        Workspace workspace = CollaborationTree.get(2);
        User userA = WorkspaceService.registerUser("Luciano");
        Workspace w2 = WorkspaceService.createWorkspace("w2",workspace,userA,workspace.tool(),false,false);

        InstanceType umlClassType = w2.TYPES_FOLDER.subfolder("UMLLoader3").instanceTypeWithName("Class");
        InstanceType umlLifelineType = w2.TYPES_FOLDER.subfolder("UMLLoader3").instanceTypeWithName("Lifeline");
        InstanceType umlOperationType = w2.TYPES_FOLDER.subfolder("UMLLoader3").instanceTypeWithName("Operation");
        InstanceType umlMessageType = w2.TYPES_FOLDER.subfolder("UMLLoader3").instanceTypeWithName("Message");
        InstanceType umlStateMachineType = w2.TYPES_FOLDER.subfolder("UMLLoader3").instanceTypeWithName("StateMachine");
        InstanceType umlStateType = w2.TYPES_FOLDER.subfolder("UMLLoader3").instanceTypeWithName("State");
        InstanceType umlRegionType = w2.TYPES_FOLDER.subfolder("UMLLoader3").instanceTypeWithName("Region");
        InstanceType umlTransitionType = w2.TYPES_FOLDER.subfolder("UMLLoader3").instanceTypeWithName("Transition");
        InstanceType umlMessageOccurrenceType = w2.TYPES_FOLDER.subfolder("UMLLoader3").instanceTypeWithName("MessageOccurrenceSpecification");
        InstanceType umlInteractionFragmentType = w2.TYPES_FOLDER.subfolder("UMLLoader3").instanceTypeWithName("InteractionFragment");
        for(Object obj : umlMessageType.instances().get()){
            Instance i = (Instance) obj;
            i.addOwner(userA);
        }
        w2.concludeTransaction();
        w2.commit();

        ConsistencyRuleType newCrd1 = ConsistencyRuleType.create(w2, umlMessageType,
                "crd1 - messages must have a corr. operation",
                "self.receiveEvent.asType(<" + umlInteractionFragmentType.getQualifiedName() + ">)" +
                        ".covered.represents.type.asType(<" + umlClassType.getQualifiedName() + ">)" +
                        ".ownedOperation->exists(op| op.name=self.name)");

        ConsistencyRuleType newCrd2 = ConsistencyRuleType.create(w2, umlTransitionType,
                "crd2 - transitions must have a corr. operation",
                "self.owner.asType(<" + umlRegionType.getQualifiedName() + ">)" +
                        ".owner.asType(<" + umlStateMachineType.getQualifiedName() + ">)" +
                        ".specification.owner.asType(<" + umlClassType.getQualifiedName() + ">)" +
                        ".ownedOperation->exists(o| o.name = self.name)");

        ConsistencyRuleType newCrd3 = ConsistencyRuleType.create(w2, umlTransitionType,
                "crd3 - Sequence of messages must match allowed sequence of actions in state machine",
                "self.lifeline.messages->exists(m| m.name = self.name)");

        crds.clear();
        if(cr1) crds.add(newCrd1);
        if(cr2) crds.add(newCrd2);
        if(cr3) crds.add(newCrd3);
        w2.concludeTransaction();
        for (ConsistencyRuleType crd : crds){
            checkRule(crd);
        }
        Set<ConsistencyRule> inconsistencies = new HashSet<>();
        for (ConsistencyRuleType crd : crds){
            inconsistencies.addAll(crd.consistencyRuleEvaluations()
                    .stream()
                    .filter(rule->!rule.isConsistent() && !rule.hasEvaluationError())
                    .collect(Collectors.toSet()));
        }
        Set<ConsistencyRule> ignoredInconsistencies = new HashSet<>(inconsistencies);

        // setup part 2
        Instance operationGrabOrRelease = umlOperationType.instances().stream().filter(instance -> instance.name().equalsIgnoreCase("graborrelease")).findFirst().get();
        Instance classGripper = umlClassType.instances().stream().filter(instance -> instance.name().equalsIgnoreCase("Gripper")).findFirst().get();
        Change change1 = new Change(operationGrabOrRelease, "name", Operator.MOD_EQ, "grab" , Change.ChangeType.NEGATIVE);
        operationGrabOrRelease.getPropertyAsSingle(change1.getProperty()).set(change1.getValue());
        w2.concludeTransaction();



        inconsistencies = new HashSet<>();
        for (ConsistencyRuleType crd : crds){
            inconsistencies.addAll(crd.consistencyRuleEvaluations()
                    .stream()
                    .filter(rule->!rule.isConsistent() && !rule.hasEvaluationError())
                    .collect(Collectors.toSet()));
        }
        ModelState modelState = new ModelState(null,new HashSet<>(), inconsistencies, false);
        modelState.setChange(change1);
        parallelGraphGenerator = new ParallelGraphGenerator(modelState, crds,w2,pastChanges, conflicts, initIgnoredProperties());
        if(filterByOwner) parallelGraphGenerator.setOwnerId(String.valueOf(userA.id));
        Instance operationRelease = w2.createInstance(umlOperationType,"release");
        Change change2 = new Change(classGripper, "ownedOperation", Operator.ADD,  operationRelease, Change.ChangeType.NEGATIVE);
        classGripper.getPropertyAsSet(change2.getProperty()).add(change2.getValue());
        w2.concludeTransaction();
        inconsistencies.clear();
        for (ConsistencyRuleType crd : crds){
            inconsistencies.addAll(crd.consistencyRuleEvaluations()
                    .stream()
                    .filter(rule->!rule.isConsistent() && !rule.hasEvaluationError())
                    .collect(Collectors.toSet()));
        }
        ModelState modelState2 = new ModelState(null,new HashSet<>(inconsistencies), ignoredInconsistencies, false);
        modelState2.setChange(change2);

        parallelGraphGenerator.updatePropagator(modelState2, crds, pastIcon);


        // generate and export Graph
        parallelGraphGenerator.generateGraph(inconsistentPaths, abstractStates, true, true);
        String json = parallelGraphGenerator.exportAsJSON();
        WorkspaceService.deleteWorkspace(w2);
        return json;
    }

    private Set<String> initIgnoredProperties(){
        Set<String> ignoredProperties = new HashSet<>();
        ignoredProperties.add(ReservedNames.INSTANCE_OF);
        ignoredProperties.add(ReservedNames.INSTANCES);
        ignoredProperties.add(ReservedNames.SUPER_TYPES);
        ignoredProperties.add(ReservedNames.SUB_TYPES);
        ignoredProperties.add(ReservedNames.INSTANTIATION_CLASS);
        ignoredProperties.add(ReservedNames.PROPERTY_DEFINITION_PREFIX);
        ignoredProperties.add(ReservedNames.OWNER_INSTANCE_TYPE);
        ignoredProperties.add(ReservedNames.CARDINALITY);
        ignoredProperties.add(ReservedNames.REFERENCED_INSTANCE_TYPE);
        ignoredProperties.add(ReservedNames.NATIVE_TYPE);
        ignoredProperties.add(ReservedNames.DERIVED_RULE);
        ignoredProperties.add(ReservedNames.OPPOSED_PROPERTY_TYPE);
        ignoredProperties.add(ReservedNames.CONTAINED_FOLDER);
        ignoredProperties.add(ReservedNames.OWNER);
        ignoredProperties.add(ReservedNames.MEMBERS);
        ignoredProperties.add(ReservedNames.FOLDER_CONTENT);
        ignoredProperties.add(ReservedNames.FOLDER_SUBFOLDERS);
        ignoredProperties.add(ReservedNames.FOLDER_PARENT);
        ignoredProperties.add(ReservedNames.OWNERSHIP_PROPERTY);
        ignoredProperties.add(at.jku.isse.designspace.rule.model.ReservedNames.RULE_EVALUATIONS_IN_CONTEXT);

        // UML specific
        ignoredProperties.add("ownedElement");
        ignoredProperties.add("nestedPackage");
        ignoredProperties.add("ownedMember");
        ignoredProperties.add("owner");
        ignoredProperties.add("annotatedElement");
        ignoredProperties.add("ownedType");
        ignoredProperties.add("nsURI");
        ignoredProperties.add("elementURI");
        ignoredProperties.add("qualifiedName");
        ignoredProperties.add("featuringClassifier");
        ignoredProperties.add("feature");
        ignoredProperties.add("packagedElement");

        return ignoredProperties;
    }



    /**
     * change2 fixes inconsistency generated by change1 and generates a new one
     */
    private String Scenario1() {
        crds.add(crd1);
        workspace.concludeTransaction();
        Change change1 = new Change(messageGrabOrRelease1, "name", Operator.MOD_EQ, "grab" , Change.ChangeType.NEGATIVE);
        messageGrabOrRelease1.getPropertyAsSingle(change1.getProperty()).set(change1.getValue());
        workspace.concludeTransaction();
        Set<ConsistencyRule> inconsistencies = new HashSet<>();
        for (ConsistencyRuleType crd : crds){
            inconsistencies.addAll(crd.consistencyRuleEvaluations()
                    .stream()
                    .filter(rule->!rule.isConsistent() && !rule.isDeleted())
                    .collect(Collectors.toSet()));
        }
        ModelState modelState = new ModelState(null,inconsistencies, null, false);
        modelState.setChange(change1);
        parallelGraphGenerator = new ParallelGraphGenerator(modelState, crds,
                workspace,pastChanges, conflicts, null);
        // second change
        Change change2 = new Change(operationGrabOrRelease, "name", Operator.MOD_EQ, "grab" , Change.ChangeType.NEGATIVE);
        operationGrabOrRelease.getPropertyAsSingle(change2.getProperty()).set(change2.getValue());
        workspace.concludeTransaction();

        Set<ConsistencyRule> inconsistencies2 = new HashSet<>();
        for (ConsistencyRuleType crd : crds){
            inconsistencies2.addAll(crd.consistencyRuleEvaluations()
                    .stream()
                    .filter(rule->!rule.isConsistent() && !rule.isDeleted())
                    .collect(Collectors.toSet()));
        }

        workspace.concludeTransaction();
        ModelState modelState2 = new ModelState(null,inconsistencies2, null, false);
        modelState2.setChange(change2);
        parallelGraphGenerator.updatePropagator(modelState2, crds, pastIcon);
        parallelGraphGenerator.generateGraph(inconsistentPaths, abstractStates, true, true);

        String json = parallelGraphGenerator.exportAsJSON();
        WorkspaceService.deleteWorkspace(workspace);
        return json;

    }

    /**
     * change2 fixes inconsistency generated by change1 and generates a new one (inverted order of changes)
     */
    private String Scenario1Inverted() {
        crds.add(crd1);
        workspace.concludeTransaction();
        Change change2 = new Change(operationGrabOrRelease, "name", Operator.MOD_EQ, "grab" , Change.ChangeType.NEGATIVE);
        operationGrabOrRelease.getPropertyAsSingle(change2.getProperty()).set(change2.getValue());
        workspace.concludeTransaction();
        Set<ConsistencyRule> inconsistencies = new HashSet<>();
        for (ConsistencyRuleType crd : crds){
            inconsistencies.addAll(crd.consistencyRuleEvaluations()
                    .stream()
                    .filter(rule->!rule.isConsistent() && !rule.isDeleted())
                    .collect(Collectors.toSet()));
        }
        ModelState modelState = new ModelState(null,inconsistencies, null, false);
        modelState.setChange(change2);
        parallelGraphGenerator = new ParallelGraphGenerator(modelState, crds,
                workspace,pastChanges, conflicts, null);
        // second change
        Change change1 = new Change(messageGrabOrRelease1, "name", Operator.MOD_EQ, "grab" , Change.ChangeType.NEGATIVE);
        messageGrabOrRelease1.getPropertyAsSingle(change1.getProperty()).set(change1.getValue());
        workspace.concludeTransaction();

        Set<ConsistencyRule> inconsistencies2 = new HashSet<>();
        for (ConsistencyRuleType crd : crds){
            inconsistencies2.addAll(crd.consistencyRuleEvaluations()
                    .stream()
                    .filter(rule->!rule.isConsistent() && !rule.isDeleted())
                    .collect(Collectors.toSet()));
        }

        workspace.concludeTransaction();
        ModelState modelState2 = new ModelState(null,inconsistencies2, null, false);
        modelState2.setChange(change1);
        parallelGraphGenerator.updatePropagator(modelState2, crds, pastIcon);
        parallelGraphGenerator.generateGraph(inconsistentPaths, abstractStates, true, true);

        String json = parallelGraphGenerator.exportAsJSON();
        WorkspaceService.deleteWorkspace(workspace);
        return json;

    }

    /**
     * change2 is a repair from change1 propagation and does not generate a new incon.
     */
    private String Scenario2() {
        crds.add(crd3);
        Change change1 = new Change(transitionGrabOrRelease1, "name", Operator.MOD_EQ, "grab" , Change.ChangeType.NEGATIVE);
        transitionGrabOrRelease1.getPropertyAsSingle(change1.getProperty()).set(change1.getValue());
        workspace.concludeTransaction();
        Set<ConsistencyRule> inconsistencies = new HashSet<>();
        for (ConsistencyRuleType crd : crds){
            inconsistencies.addAll(crd.consistencyRuleEvaluations()
                    .stream()
                    .filter(rule->!rule.isConsistent() && !rule.isDeleted())
                    .collect(Collectors.toSet()));
        }
        ModelState modelState = new ModelState(null,inconsistencies, null, false);
        modelState.setChange(change1);
        parallelGraphGenerator = new ParallelGraphGenerator(modelState, crds,
                workspace,pastChanges, conflicts, null);
        // second change
        Change change2 = new Change(messageGrabOrRelease1, "name", Operator.MOD_EQ, "grab" , Change.ChangeType.POSITIVE);
        messageGrabOrRelease1.getPropertyAsSingle(change2.getProperty()).set(change2.getValue());
        workspace.concludeTransaction();
        Set<ConsistencyRule> inconsistencies2 = new HashSet<>();
        for (ConsistencyRuleType crd : crds){
            inconsistencies2.addAll(crd.consistencyRuleEvaluations()
                    .stream()
                    .filter(rule->!rule.isConsistent() && !rule.isDeleted())
                    .collect(Collectors.toSet()));
        }

        workspace.concludeTransaction();
        ModelState modelState2 = new ModelState(null,inconsistencies2, inconsistencies, false);
        modelState2.setChange(change2);
        parallelGraphGenerator.updatePropagator(modelState2, crds, pastIcon);
        parallelGraphGenerator.generateGraph(inconsistentPaths, abstractStates, true, true);

        String json = parallelGraphGenerator.exportAsJSON();
        WorkspaceService.deleteWorkspace(workspace);
        return json;
    }

    /**
     * Second change2 is a repair from change1 propagation and does not generate a new incon. (inverted order)
     */
    private String Scenario2Inverted() {
        crds.add(crd3);
        Change change1 = new Change(messageGrabOrRelease1, "name", Operator.MOD_EQ, "grab" , Change.ChangeType.POSITIVE);
        messageGrabOrRelease1.getPropertyAsSingle(change1.getProperty()).set(change1.getValue());
        workspace.concludeTransaction();
        Set<ConsistencyRule> inconsistencies = new HashSet<>();
        for (ConsistencyRuleType crd : crds){
            inconsistencies.addAll(crd.consistencyRuleEvaluations()
                    .stream()
                    .filter(rule->!rule.isConsistent() && !rule.isDeleted())
                    .collect(Collectors.toSet()));
        }
        ModelState modelState = new ModelState(null,inconsistencies, null, false);
        modelState.setChange(change1);
        parallelGraphGenerator = new ParallelGraphGenerator(modelState, crds,
                workspace,pastChanges, conflicts, null);
        // second change
        Change change2 = new Change(transitionGrabOrRelease1, "name", Operator.MOD_EQ, "grab" , Change.ChangeType.NEGATIVE);
        transitionGrabOrRelease1.getPropertyAsSingle(change2.getProperty()).set(change2.getValue());

        workspace.concludeTransaction();
        Set<ConsistencyRule> inconsistencies2 = new HashSet<>();
        for (ConsistencyRuleType crd : crds){
            inconsistencies2.addAll(crd.consistencyRuleEvaluations()
                    .stream()
                    .filter(rule->!rule.isConsistent() && !rule.isDeleted())
                    .collect(Collectors.toSet()));
        }

        workspace.concludeTransaction();
        ModelState modelState2 = new ModelState(null,inconsistencies2, inconsistencies, false);
        modelState2.setChange(change2);
        parallelGraphGenerator.updatePropagator(modelState2, crds, pastIcon);
        parallelGraphGenerator.generateGraph(inconsistentPaths, abstractStates, true, true);

        String json = parallelGraphGenerator.exportAsJSON();
        WorkspaceService.deleteWorkspace(workspace);
        return json;
    }
    /**
     * change1 does not create inconsistencies but affects change2 propagation
     */
    private String Scenario3() {
        crds.add(crd1);
        Change change1 = new Change(operationMove, "name", Operator.MOD_EQ, "release" , Change.ChangeType.NEUTRAL);
        operationMove.getPropertyAsSingle(change1.getProperty()).set(change1.getValue());
        workspace.concludeTransaction();
        Set<ConsistencyRule> inconsistencies = new HashSet<>();
        for (ConsistencyRuleType crd : crds){
            inconsistencies.addAll(crd.consistencyRuleEvaluations()
                    .stream()
                    .filter(rule->!rule.isConsistent() && !rule.isDeleted())
                    .collect(Collectors.toSet()));
        }
        assert inconsistencies.isEmpty();
        ModelState modelState = new ModelState(null,inconsistencies, null, false);
        modelState.setChange(change1);
        parallelGraphGenerator = new ParallelGraphGenerator(modelState, crds,
                workspace,pastChanges, conflicts, null);
        // second change
        Change change2 = new Change(messageGrabOrRelease1, "name", Operator.MOD_EQ, "move" , Change.ChangeType.NEGATIVE);
        messageGrabOrRelease1.getPropertyAsSingle(change2.getProperty()).set(change2.getValue());
        workspace.concludeTransaction();
        Set<ConsistencyRule> inconsistencies2 = new HashSet<>();
        for (ConsistencyRuleType crd : crds){
            inconsistencies2.addAll(crd.consistencyRuleEvaluations()
                    .stream()
                    .filter(rule->!rule.isConsistent() && !rule.isDeleted())
                    .collect(Collectors.toSet()));
        }
        assert inconsistencies.isEmpty();
        assert !inconsistencies2.isEmpty();
        workspace.concludeTransaction();
        ModelState modelState2 = new ModelState(null,inconsistencies2, null, false);
        modelState2.setChange(change2);
        parallelGraphGenerator.updatePropagator(modelState2, crds, pastIcon);
        parallelGraphGenerator.generateGraph(inconsistentPaths, abstractStates, true, true);
        {
            assert workspace.operations().isEmpty();
            assert !parallelGraphGenerator.isGraphEmpty();
            assert !parallelGraphGenerator.getConflictEdges().isEmpty();
        }
        String json = parallelGraphGenerator.exportAsJSON();
        WorkspaceService.deleteWorkspace(workspace);
        return json;
    }

    /**
     * change1 does not create inconsistencies but affects change2 propagation (inverted order)
     */
    private String Scenario3Inverted() {
        crds.add(crd1);
        Change change1 = new Change(messageGrabOrRelease1, "name", Operator.MOD_EQ, "move" , Change.ChangeType.NEGATIVE);
        messageGrabOrRelease1.getPropertyAsSingle(change1.getProperty()).set(change1.getValue());
        workspace.concludeTransaction();
        Set<ConsistencyRule> inconsistencies = new HashSet<>();
        for (ConsistencyRuleType crd : crds){
            inconsistencies.addAll(crd.consistencyRuleEvaluations()
                    .stream()
                    .filter(rule->!rule.isConsistent() && !rule.isDeleted())
                    .collect(Collectors.toSet()));
        }

        ModelState modelState = new ModelState(null,inconsistencies, null, false);
        modelState.setChange(change1);
        parallelGraphGenerator = new ParallelGraphGenerator(modelState, crds,
                workspace,pastChanges, conflicts, null);
        // second change
        Change change2 = new Change(operationMove, "name", Operator.MOD_EQ, "release" , Change.ChangeType.NEUTRAL);
        operationMove.getPropertyAsSingle(change2.getProperty()).set(change2.getValue());
        workspace.concludeTransaction();
        Set<ConsistencyRule> inconsistencies2 = new HashSet<>();
        for (ConsistencyRuleType crd : crds){
            inconsistencies2.addAll(crd.consistencyRuleEvaluations()
                    .stream()
                    .filter(rule->!rule.isConsistent() && !rule.isDeleted())
                    .collect(Collectors.toSet()));
        }

        workspace.concludeTransaction();
        ModelState modelState2 = new ModelState(null,inconsistencies2, null, false);
        modelState2.setChange(change2);
        parallelGraphGenerator.updatePropagator(modelState2, crds, pastIcon);
        parallelGraphGenerator.generateGraph(inconsistentPaths, abstractStates, true, true);
        {
            assert workspace.operations().isEmpty();
            assert !parallelGraphGenerator.isGraphEmpty();
            assert !parallelGraphGenerator.getConflictEdges().isEmpty();
        }
        String json = parallelGraphGenerator.exportAsJSON();
        WorkspaceService.deleteWorkspace(workspace);
        return json;
    }

    /**
     * change1 and change2 cause different inconsistencies. The repairs for each of them are conflicting
     */
    private String Scenario4() {
        crds.add(crd1);
        Change change1 = new Change(messageGrabOrRelease1, "name", Operator.MOD_EQ, "grab" , Change.ChangeType.NEUTRAL);
        messageGrabOrRelease1.getPropertyAsSingle(change1.getProperty()).set(change1.getValue());
        workspace.concludeTransaction();
        Set<ConsistencyRule> inconsistencies = new HashSet<>();
        for (ConsistencyRuleType crd : crds){
            inconsistencies.addAll(crd.consistencyRuleEvaluations()
                    .stream()
                    .filter(rule->!rule.isConsistent() && !rule.isDeleted())
                    .collect(Collectors.toSet()));
        }

        ModelState modelState = new ModelState(null,inconsistencies, null, false);
        modelState.setChange(change1);
        parallelGraphGenerator = new ParallelGraphGenerator(modelState, crds,
                workspace,pastChanges, conflicts, null);
        // second change
        Change change2 = new Change(messageGrabOrRelease2, "name", Operator.MOD_EQ, "release" , Change.ChangeType.NEGATIVE);
        messageGrabOrRelease2.getPropertyAsSingle(change2.getProperty()).set(change2.getValue());
        workspace.concludeTransaction();
        Set<ConsistencyRule> inconsistencies2 = new HashSet<>();
        for (ConsistencyRuleType crd : crds){
            inconsistencies2.addAll(crd.consistencyRuleEvaluations()
                    .stream()
                    .filter(rule->!rule.isConsistent() && !rule.isDeleted())
                    .collect(Collectors.toSet()));
        }


        workspace.concludeTransaction();
        ModelState modelState2 = new ModelState(null,inconsistencies2, inconsistencies, false);
        modelState2.setChange(change2);
        parallelGraphGenerator.updatePropagator(modelState2, crds, pastIcon);
        parallelGraphGenerator.generateGraph(inconsistentPaths, abstractStates, true, true);

        String json = parallelGraphGenerator.exportAsJSON();
        WorkspaceService.deleteWorkspace(workspace);
        return json;
    }

    /**
     * change1 and change2 cause different inconsistencies. The repairs for each of them are conflicting (inverted order)
     */
    private String Scenario4Inverted() {
        crds.add(crd1);
        Change change1 = new Change(messageGrabOrRelease2, "name", Operator.MOD_EQ, "release" , Change.ChangeType.NEGATIVE);
        messageGrabOrRelease2.getPropertyAsSingle(change1.getProperty()).set(change1.getValue());
        workspace.concludeTransaction();
        Set<ConsistencyRule> inconsistencies = new HashSet<>();
        for (ConsistencyRuleType crd : crds){
            inconsistencies.addAll(crd.consistencyRuleEvaluations()
                    .stream()
                    .filter(rule->!rule.isConsistent() && !rule.isDeleted())
                    .collect(Collectors.toSet()));
        }

        ModelState modelState = new ModelState(null,inconsistencies, null, false);
        modelState.setChange(change1);
        parallelGraphGenerator = new ParallelGraphGenerator(modelState, crds,
                workspace,pastChanges, conflicts, null);
        // second change
        Change change2 = new Change(messageGrabOrRelease1, "name", Operator.MOD_EQ, "grab" , Change.ChangeType.NEUTRAL);
        messageGrabOrRelease1.getPropertyAsSingle(change2.getProperty()).set(change2.getValue());
        workspace.concludeTransaction();
        Set<ConsistencyRule> inconsistencies2 = new HashSet<>();
        for (ConsistencyRuleType crd : crds){
            inconsistencies2.addAll(crd.consistencyRuleEvaluations()
                    .stream()
                    .filter(rule->!rule.isConsistent() && !rule.isDeleted())
                    .collect(Collectors.toSet()));
        }


        workspace.concludeTransaction();
        ModelState modelState2 = new ModelState(null,inconsistencies2, inconsistencies, false);
        modelState2.setChange(change2);
        parallelGraphGenerator.updatePropagator(modelState2, crds, pastIcon);
        parallelGraphGenerator.generateGraph(inconsistentPaths, abstractStates, true, true);


        String json = parallelGraphGenerator.exportAsJSON();
        WorkspaceService.deleteWorkspace(workspace);
        return json;
    }

    /**
     * change1's repairs are partially invalid due to change2
     */
    private String Scenario5() {
        crds.add(crd1);
        Instance messageStop = WorkspaceService.createInstance(workspace, "stop", messageType);
        messageStop.getPropertyAsSingle("UMLName").set("stop");
        Change change1 = new Change(classGripper, "messages", Operator.ADD, messageStop, Change.ChangeType.NEGATIVE);
        classGripper.getPropertyAsSet(change1.getProperty()).add(change1.getValue());
        workspace.concludeTransaction();
        Set<ConsistencyRule> inconsistencies = new HashSet<>();
        for (ConsistencyRuleType crd : crds){
            inconsistencies.addAll(crd.consistencyRuleEvaluations()
                    .stream()
                    .filter(rule->!rule.isConsistent() && !rule.isDeleted())
                    .collect(Collectors.toSet()));
        }
        assert inconsistencies.size()==1;
        ModelState modelState = new ModelState(null,inconsistencies, null, false);
        modelState.setChange(change1);
        parallelGraphGenerator = new ParallelGraphGenerator(modelState, crds,
                workspace,pastChanges, conflicts, null);
        // second change
        Change change2 = new Change(operationGrabOrRelease, "name", Operator.MOD_EQ, "grab" , Change.ChangeType.NEGATIVE);
        operationGrabOrRelease.getPropertyAsSingle(change2.getProperty()).set(change2.getValue());
        workspace.concludeTransaction();
        Set<ConsistencyRule> inconsistencies2 = new HashSet<>();
        for (ConsistencyRuleType crd : crds){
            inconsistencies2.addAll(crd.consistencyRuleEvaluations()
                    .stream()
                    .filter(rule->!rule.isConsistent() && !rule.isDeleted())
                    .collect(Collectors.toSet()));
        }

        assert inconsistencies2.size()==1;
        assert inconsistencies2.size()==2;
        workspace.concludeTransaction();
        ModelState modelState2 = new ModelState(null,inconsistencies2, inconsistencies, false);
        modelState2.setChange(change2);
        parallelGraphGenerator.updatePropagator(modelState2, crds, pastIcon);
        parallelGraphGenerator.generateGraph(inconsistentPaths, abstractStates, true, true);
        {
            assert workspace.operations().isEmpty();
            assert !parallelGraphGenerator.isGraphEmpty();
            assert !parallelGraphGenerator.getConflictEdges().isEmpty();
        }
        String json = parallelGraphGenerator.exportAsJSON();
        WorkspaceService.deleteWorkspace(workspace);
        return json;
    }


    /**
     * change1's repairs are partially invalid due to change2 (inverted order)
     */
    private String Scenario5Inverted() {
        crds.add(crd1);

        Change change1 = new Change(operationGrabOrRelease, "name", Operator.MOD_EQ, "grab" , Change.ChangeType.NEGATIVE);
        operationGrabOrRelease.getPropertyAsSingle(change1.getProperty()).set(change1.getValue());
        workspace.concludeTransaction();
        Set<ConsistencyRule> inconsistencies = new HashSet<>();
        for (ConsistencyRuleType crd : crds){
            inconsistencies.addAll(crd.consistencyRuleEvaluations()
                    .stream()
                    .filter(rule->!rule.isConsistent() && !rule.isDeleted())
                    .collect(Collectors.toSet()));
        }
        assert inconsistencies.size()==1;
        ModelState modelState = new ModelState(null,inconsistencies, null, false);
        modelState.setChange(change1);
        parallelGraphGenerator = new ParallelGraphGenerator(modelState, crds,
                workspace,pastChanges, conflicts, null);
        // second change
        Instance messageStop = WorkspaceService.createInstance(workspace, "stop", messageType);
        messageStop.getPropertyAsSingle("UMLName").set("stop");
        Change change2 = new Change(classGripper, "messages", Operator.ADD, messageStop, Change.ChangeType.NEGATIVE);
        classGripper.getPropertyAsSet(change2.getProperty()).add(change2.getValue());
        workspace.concludeTransaction();
        Set<ConsistencyRule> inconsistencies2 = new HashSet<>();
        for (ConsistencyRuleType crd : crds){
            inconsistencies2.addAll(crd.consistencyRuleEvaluations()
                    .stream()
                    .filter(rule->!rule.isConsistent() && !rule.isDeleted())
                    .collect(Collectors.toSet()));
        }

        assert inconsistencies2.size()==1;
        assert inconsistencies2.size()==2;
        workspace.concludeTransaction();
        ModelState modelState2 = new ModelState(null,inconsistencies2, inconsistencies, false);
        modelState2.setChange(change2);
        parallelGraphGenerator.updatePropagator(modelState2, crds, pastIcon);
        parallelGraphGenerator.generateGraph(inconsistentPaths, abstractStates, true, true);
        {
            assert workspace.operations().isEmpty();
            assert !parallelGraphGenerator.isGraphEmpty();
            assert !parallelGraphGenerator.getConflictEdges().isEmpty();
        }
        String json = parallelGraphGenerator.exportAsJSON();
        WorkspaceService.deleteWorkspace(workspace);
        return json;
    }

    /**
     * change1's repairs are completely invalid due to change2
     */
    private String Scenario6() {
        crds.add(crd1);

        WorkspaceService.deleteInstance(workspace, operationMove); // deleting operationMove to reduce repair possibilities
        workspace.concludeTransaction();

        Change change1 = new Change(messageGrabOrRelease1, "name", Operator.MOD_EQ, "grab", Change.ChangeType.NEGATIVE);
        ((Instance)change1.getElement()).getPropertyAsSingle(change1.getProperty()).set(change1.getValue());
        workspace.concludeTransaction();
        Set<ConsistencyRule> inconsistencies = new HashSet<>();
        for (ConsistencyRuleType crd : crds){
            inconsistencies.addAll(crd.consistencyRuleEvaluations()
                    .stream()
                    .filter(rule->!rule.isConsistent() && !rule.isDeleted())
                    .collect(Collectors.toSet()));
        }

        ModelState modelState = new ModelState(null,inconsistencies, null, false);
        modelState.setChange(change1);
        parallelGraphGenerator = new ParallelGraphGenerator(modelState, crds,
                workspace,pastChanges, conflicts, null);

        // second change
        Change change2 = new Change(operationGrabOrRelease, "name", Operator.MOD_EQ, "release" , Change.ChangeType.NEGATIVE);
        ((Instance)change2.getElement()).getPropertyAsSingle(change2.getProperty()).set(change2.getValue());
        workspace.concludeTransaction();
        Set<ConsistencyRule> inconsistencies2 = new HashSet<>();
        for (ConsistencyRuleType crd : crds){
            inconsistencies2.addAll(crd.consistencyRuleEvaluations()
                    .stream()
                    .filter(rule->!rule.isConsistent() && !rule.isDeleted())
                    .collect(Collectors.toSet()));
        }


        workspace.concludeTransaction();
        ModelState modelState2 = new ModelState(null,inconsistencies2, inconsistencies, false);
        modelState2.setChange(change2);
        parallelGraphGenerator.updatePropagator(modelState2, crds, pastIcon);
        parallelGraphGenerator.generateGraph(inconsistentPaths, abstractStates, true, true);


        String json = parallelGraphGenerator.exportAsJSON();
        WorkspaceService.deleteWorkspace(workspace);
        return json;
    }


    /**
     * change 1 is completely invalid due to change2 (inverted order)
     */
    private String Scenario6Inverted() {
        crds.add(crd1);
        WorkspaceService.deleteInstance(workspace, operationMove); // deleting operationMove to reduce repair possibilities
        workspace.concludeTransaction();

        Change change1 = new Change(operationGrabOrRelease, "name", Operator.MOD_EQ, "release" , Change.ChangeType.NEGATIVE);
        ((Instance)change1.getElement()).getPropertyAsSingle(change1.getProperty()).set(change1.getValue());
        workspace.concludeTransaction();
        Set<ConsistencyRule> inconsistencies = new HashSet<>();
        for (ConsistencyRuleType crd : crds){
            inconsistencies.addAll(crd.consistencyRuleEvaluations()
                    .stream()
                    .filter(rule->!rule.isConsistent() && !rule.isDeleted())
                    .collect(Collectors.toSet()));
        }

        ModelState modelState = new ModelState(null,inconsistencies, null, false);
        modelState.setChange(change1);
        parallelGraphGenerator = new ParallelGraphGenerator(modelState, crds,
                workspace,pastChanges, conflicts, null);

        // second change
        Change change2 = new Change(messageGrabOrRelease1, "name", Operator.MOD_EQ, "grab", Change.ChangeType.NEGATIVE);
        ((Instance)change2.getElement()).getPropertyAsSingle(change2.getProperty()).set(change2.getValue());
        workspace.concludeTransaction();
        Set<ConsistencyRule> inconsistencies2 = new HashSet<>();
        for (ConsistencyRuleType crd : crds){
            inconsistencies2.addAll(crd.consistencyRuleEvaluations()
                    .stream()
                    .filter(rule->!rule.isConsistent() && !rule.isDeleted())
                    .collect(Collectors.toSet()));
        }


        workspace.concludeTransaction();
        ModelState modelState2 = new ModelState(null,inconsistencies2, inconsistencies, false);
        modelState2.setChange(change2);
        parallelGraphGenerator.updatePropagator(modelState2, crds, pastIcon);
        parallelGraphGenerator.generateGraph(inconsistentPaths, abstractStates, true, true);


        String json = parallelGraphGenerator.exportAsJSON();
        WorkspaceService.deleteWorkspace(workspace);
        return json;
    }



    private String complexScenario(){
        crds.add(crd1);
        crds.add(crd2);
        crds.add(crd3);
        workspace.concludeTransaction();
        Change initialChange = new Change(transitionGrabOrRelease1, "name", Operator.MOD_EQ, "grab", Change.ChangeType.NEGATIVE);
        transitionGrabOrRelease1.getPropertyAsSingle(initialChange.getProperty()).set(initialChange.getValue());
        workspace.concludeTransaction();
        Set<ConsistencyRule> inconsistencies = new HashSet<>();
        for (ConsistencyRuleType crd : crds){
            inconsistencies.addAll(crd.consistencyRuleEvaluations()
                    .stream()
                    .filter(rule->!rule.isConsistent() && !rule.isDeleted())
                    .collect(Collectors.toSet()));
        }
        ModelState modelState = new ModelState(null,inconsistencies, null, false);
        modelState.setChange(initialChange);
        parallelGraphGenerator = new ParallelGraphGenerator(modelState, crds, workspace,
                pastChanges, conflicts, null);
        // second change
        Change newChange = new Change(transitionGrabOrRelease2, "name", Operator.MOD_EQ, "release", Change.ChangeType.NEGATIVE);
        transitionGrabOrRelease2.getPropertyAsSingle(newChange.getProperty()).set(newChange.getValue());
        workspace.concludeTransaction();
        Set<ConsistencyRule> inconsistencies2 = new HashSet<>();
        for (ConsistencyRuleType crd : crds) {
            inconsistencies2.addAll(crd.consistencyRuleEvaluations()
                    .stream()
                    .filter(rule->!rule.isConsistent())
                    .collect(Collectors.toSet()));
        }
        workspace.concludeTransaction();

        ModelState modelState2 = new ModelState(null,inconsistencies2, inconsistencies, false);
        modelState2.setChange(newChange);
        parallelGraphGenerator.updatePropagator(modelState2, crds, pastIcon);
//        parallelGraphGenerator = new ParallelGraphGenerator(modelState2,newChange,crds, workspace,
//                false,instancesFolder.name());

        // third change
        Change newChange2 = new Change(messageGrabOrRelease1, "name", Operator.MOD_EQ, "grab", Change.ChangeType.NEGATIVE);
        messageGrabOrRelease1.getPropertyAsSingle(newChange.getProperty()).set(newChange.getValue());
        workspace.concludeTransaction();
        Set<ConsistencyRule> inconsistencies3 = new HashSet<>();
        for (ConsistencyRuleType crd : crds) {
            inconsistencies3.addAll(crd.consistencyRuleEvaluations()
                    .stream()
                    .filter(rule->!rule.isConsistent())
                    .collect(Collectors.toSet()));
        }
        workspace.concludeTransaction();

        ModelState modelState3 = new ModelState(null,inconsistencies3, inconsistencies, false);
        modelState3.setChange(newChange2);
        parallelGraphGenerator.updatePropagator(modelState3, crds, pastIcon);

        parallelGraphGenerator.generateGraph(inconsistentPaths, abstractStates, true, true);
        String json = parallelGraphGenerator.exportAsJSON();
        WorkspaceService.deleteWorkspace(workspace);
        return json;
    }

}
