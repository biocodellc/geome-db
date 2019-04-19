package biocode.fims.run;

import biocode.fims.application.config.GeomeAppConfig;
import biocode.fims.ncbi.sra.SraAccessionHarvester;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.io.IOException;

/**
 * @author rjewing
 */
public class SraAccessionHarvesterRunner {

    public static void main(String[] args) throws IOException {
        ApplicationContext applicationContext = new AnnotationConfigApplicationContext(GeomeAppConfig.class);
        SraAccessionHarvester harvester = applicationContext.getBean(SraAccessionHarvester.class);
        harvester.harvestForAllProjects();
    }
}
