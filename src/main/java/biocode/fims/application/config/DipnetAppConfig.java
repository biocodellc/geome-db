package biocode.fims.application.config;

import biocode.fims.fileManagers.dataset.FimsMetadataFileManager;
import biocode.fims.fileManagers.dataset.DatasetPersistenceManager;
import biocode.fims.fuseki.fileManagers.dataset.FusekiDatasetPersistenceManager;
import biocode.fims.fuseki.query.elasticSearch.FusekiIndexer;
import biocode.fims.genbank.GenbankManager;
import biocode.fims.query.elasticSearch.TransportClientFactoryBean;
import biocode.fims.service.ProjectService;
import biocode.fims.utils.DatasetService;
import org.elasticsearch.client.Client;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;

/**
 * Configuration class for and Dipnet-Fims application. Including cli and webapps
 */
@Configuration
@ComponentScan(basePackages = {"biocode.fims.dipnet.services"})
@Import({FimsAppConfig.class})
@ImportResource({
        "classpath:dipnet-data-access-config.xml",
})
// declaring this here allows us to override any properties that are also included in dipnet-fims.props
@PropertySource(value = "classpath:biocode-fims.props", ignoreResourceNotFound = true)
@PropertySource("classpath:dipnet-fims.props")
public class DipnetAppConfig  {
    @Autowired
    Environment env;
    @Autowired
    FimsAppConfig fimsAppConfig;
    @Autowired
    ProjectService projectService;

    @Bean
    public DatasetService datasetService() {
        return new DatasetService(fimsAppConfig.bcidService);
    }

    @Bean
    public GenbankManager genbankManager() {
        return new GenbankManager(fimsAppConfig.settingsManager, fimsAppConfig.bcidService);
    }

    @Bean
    @Scope("prototype")
    public FimsMetadataFileManager datasetFileManager() {
        DatasetPersistenceManager persistenceManager = new FusekiDatasetPersistenceManager(fimsAppConfig.expeditionService, fimsAppConfig.bcidService);
        return new FimsMetadataFileManager(persistenceManager, fimsAppConfig.settingsManager, fimsAppConfig.expeditionService, fimsAppConfig.bcidService);
    }

    @Bean
    public Client esClient() throws Exception {
        TransportClientFactoryBean factoryBean = new TransportClientFactoryBean();
        factoryBean.setClusterName(env.getProperty("clusterName"));
        factoryBean.setClientIgnoreClusterName(Boolean.valueOf(env.getProperty("clientIgnoreClusterName")));
        factoryBean.setClientNodesSamplerInterval(env.getProperty("clientNodesSamplerInterval"));
        factoryBean.setClientPingTimeout(env.getProperty("clientPingTimeout"));
        factoryBean.setClientTransportSniff(Boolean.valueOf(env.getProperty("clientTransportSniff")));
        factoryBean.setClusterNodes(env.getProperty("clusterNodes"));
        return factoryBean.getObject();
    }

    @Bean
    public FusekiIndexer fusekiIndexer() throws Exception {
        return new FusekiIndexer(esClient(), projectService, datasetFileManager());
    }
}
