package at.jku.isse;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication
public class DesignSpaceStarter {

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(DesignSpaceStarter.class);        
        application.run(args);
    }        


    
}
