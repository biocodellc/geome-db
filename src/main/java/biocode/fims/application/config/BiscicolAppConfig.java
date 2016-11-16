package biocode.fims.application.config;

import biocode.fims.fileManagers.fimsMetadata.FimsMetadataPersistenceManager;
import biocode.fims.fileManagers.fimsMetadata.FimsMetadataFileManager;
import biocode.fims.fuseki.fileManagers.fimsMetadata.FusekiFimsMetadataPersistenceManager;
import biocode.fims.run.EzidUpdator;
import biocode.fims.service.ProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;

import java.io.FileNotFoundException;

/**
 * Configuration class for Biscicol-Fims applications. Including cli and webapps
 */
@Configuration
@Import({FimsAppConfig.class})
public class BiscicolAppConfig {
    @Autowired
    FimsAppConfig fimsAppConfig;
    @Autowired
    ProjectService projectService;

    @Bean
    @Scope("prototype")
    public FimsMetadataFileManager FimsMetadataFileManager() {
        FimsMetadataPersistenceManager persistenceManager = new FusekiFimsMetadataPersistenceManager(fimsAppConfig.expeditionService, fimsAppConfig.bcidService);
        return new FimsMetadataFileManager(persistenceManager, fimsAppConfig.settingsManager, fimsAppConfig.expeditionService, fimsAppConfig.bcidService);
    }

    @Bean
    public EzidUpdator ezidUpdator() throws FileNotFoundException {
        return new EzidUpdator(fimsAppConfig.bcidService, fimsAppConfig.settingsManager, fimsAppConfig.ezidUtils());
    }
}
