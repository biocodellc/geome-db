package biocode.fims.application.config;

import biocode.fims.elasticSearch.ESFimsMetadataPersistenceManager;
import biocode.fims.fileManagers.fimsMetadata.FimsMetadataFileManager;
import biocode.fims.fileManagers.fimsMetadata.FimsMetadataPersistenceManager;
import biocode.fims.elasticSearch.TransportClientFactoryBean;
import biocode.fims.service.ProjectService;
import org.elasticsearch.client.Client;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;

/**
 * Configuration class for and Dipnet-Fims application. Including cli and webapps
 */
@Configuration
@ComponentScan(basePackages = {"biocode.fims.dipnet.services"})
@Import({FimsAppConfig.class, ElasticSearchAppConfig.class})
@ImportResource({
        "classpath:dipnet-data-access-config.xml",
})
// declaring this here allows us to override any properties that are also included in dipnet-fims.props
@PropertySource(value = "classpath:biocode-fims.props", ignoreResourceNotFound = true)
@PropertySource("classpath:dipnet-fims.props")
public class DipnetAppConfig {
    @Autowired
    private FimsAppConfig fimsAppConfig;
    @Autowired
    ElasticSearchAppConfig esAppConfig;
    @Autowired
    ProjectService projectService;

    @Bean
    @Scope("prototype")
    public FimsMetadataFileManager fimsMetadataFileManager() {
        FimsMetadataPersistenceManager persistenceManager = new ESFimsMetadataPersistenceManager(esAppConfig.esClient);
        return new FimsMetadataFileManager(persistenceManager, fimsAppConfig.settingsManager, fimsAppConfig.expeditionService, fimsAppConfig.bcidService);
    }

}
