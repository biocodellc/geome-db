package biocode.fims.application.config;

import biocode.fims.fileManagers.dataset.DatasetPersistenceManager;
import biocode.fims.fileManagers.dataset.FimsMetadataFileManager;
import biocode.fims.fuseki.fileManagers.dataset.FusekiDatasetPersistenceManager;
import biocode.fims.run.EzidUpdator;
import biocode.fims.run.Run;
import biocode.fims.service.ProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;

import java.io.FileNotFoundException;

/**
 * Configuration class for Biscicol-Fims applications. Including cli and webapps
 */
@Configuration
@Import({FimsAppConfig.class})
@ImportResource({
        "classpath:data-access-config.xml"
})
public class BiscicolAppConfig {
    @Autowired
    FimsAppConfig fimsAppConfig;
    @Autowired
    ProjectService projectService;

    @Bean
    @Scope("prototype")
    public FimsMetadataFileManager datasetFileManager() {
        DatasetPersistenceManager persistenceManager = new FusekiDatasetPersistenceManager(fimsAppConfig.expeditionService, fimsAppConfig.bcidService);
        return new FimsMetadataFileManager(persistenceManager, fimsAppConfig.settingsManager, fimsAppConfig.expeditionService, fimsAppConfig.bcidService);
    }

    @Bean
    public Run run() {
        return new Run(fimsAppConfig.settingsManager, fimsAppConfig.expeditionService, datasetFileManager());
    }

    @Bean
    public EzidUpdator ezidUpdator() throws FileNotFoundException {
        return new EzidUpdator(fimsAppConfig.bcidService, fimsAppConfig.settingsManager, fimsAppConfig.ezidUtils());
    }
}
