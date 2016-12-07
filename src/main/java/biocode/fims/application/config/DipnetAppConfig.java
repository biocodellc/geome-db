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
@Import({FimsAppConfig.class})
@ImportResource({
        "classpath:dipnet-data-access-config.xml",
})
// declaring this here allows us to override any properties that are also included in dipnet-fims.props
@PropertySource(value = "classpath:biocode-fims.props", ignoreResourceNotFound = true)
@PropertySource("classpath:dipnet-fims.props")
public class DipnetAppConfig {
    @Autowired
    private Environment env;
    @Autowired
    private FimsAppConfig fimsAppConfig;
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
    // This bean handles the creation/destruction of the esClient bean that is autowired
    public TransportClientFactoryBean transportClientFactoryBean() {
        TransportClientFactoryBean factoryBean = new TransportClientFactoryBean();
        factoryBean.setClusterName(env.getProperty("clusterName"));
        factoryBean.setClientIgnoreClusterName(Boolean.valueOf(env.getProperty("clientIgnoreClusterName")));
        factoryBean.setClientNodesSamplerInterval(env.getProperty("clientNodesSamplerInterval"));
        factoryBean.setClientPingTimeout(env.getProperty("clientPingTimeout"));
        factoryBean.setClientTransportSniff(Boolean.valueOf(env.getProperty("clientTransportSniff")));
        factoryBean.setClusterNodes(env.getProperty("clusterNodes"));
        return factoryBean;
    }
}
