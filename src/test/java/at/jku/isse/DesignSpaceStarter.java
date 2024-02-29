package at.jku.isse;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.web.bind.annotation.*;

import at.jku.isse.designspace.core.model.DesignSpace;

@SpringBootApplication
@CrossOrigin(origins="*")
public class DesignSpaceStarter implements ApplicationListener<ApplicationReadyEvent> {

    static public void main(String[] args) {
        SpringApplication application = new SpringApplication(DesignSpaceStarter.class);        
        application.run(args);
    }        

    @Override
    public void onApplicationEvent(final ApplicationReadyEvent event) {
       
    }
    
}
