package biocode.fims.application.config;

import biocode.fims.elasticSearch.ESFimsMetadataPersistenceManager;
import biocode.fims.fileManagers.fimsMetadata.FimsMetadataPersistenceManager;
import biocode.fims.fileManagers.fimsMetadata.FimsMetadataFileManager;
import biocode.fims.fuseki.fileManagers.fimsMetadata.FusekiFimsMetadataPersistenceManager;
import biocode.fims.run.EzidUpdator;
import biocode.fims.service.ProjectService;
import org.elasticsearch.client.Client;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;

import java.io.FileNotFoundException;

/**
 * Configuration class for Biscicol-Fims applications. Including cli and webapps
 */
@Configuration
@Import({FimsAppConfig.class, ElasticSearchAppConfig.class})
// declaring this here allows us to override any properties that are also included in biscicol-fims.props
@PropertySource(value = "classpath:biocode-fims.props", ignoreResourceNotFound = true)
@PropertySource("classpath:biscicol-fims.props")
public class BiscicolAppConfig {
    @Autowired
    FimsAppConfig fimsAppConfig;
    @Autowired
    ProjectService projectService;
    @Autowired
    Client esClient;

    @Bean
    @Scope("prototype")
    public FimsMetadataFileManager fimsMetadataFileManager() {
        FimsMetadataPersistenceManager persistenceManager = new ESFimsMetadataPersistenceManager(esClient);
        return new FimsMetadataFileManager(persistenceManager, fimsAppConfig.settingsManager, fimsAppConfig.expeditionService, fimsAppConfig.bcidService);
    }

    @Bean
    public EzidUpdator ezidUpdator() throws FileNotFoundException {
        return new EzidUpdator(fimsAppConfig.bcidService, fimsAppConfig.settingsManager);
    }
}