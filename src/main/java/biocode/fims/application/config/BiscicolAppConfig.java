package biocode.fims.application.config;

import biocode.fims.elasticSearch.TransportClientFactoryBean;
import biocode.fims.fileManagers.fimsMetadata.FimsMetadataPersistenceManager;
import biocode.fims.fileManagers.fimsMetadata.FimsMetadataFileManager;
import biocode.fims.fuseki.fileManagers.fimsMetadata.FusekiFimsMetadataPersistenceManager;
import biocode.fims.run.EzidUpdator;
import biocode.fims.service.ProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;

import java.io.FileNotFoundException;

/**
 * Configuration class for Biscicol-Fims applications. Including cli and webapps
 */
@Configuration
@Import({FimsAppConfig.class})
// declaring this here allows us to override any properties that are also included in biscicol-fims.props
@PropertySource(value = "classpath:biocode-fims.props", ignoreResourceNotFound = true)
@PropertySource("classpath:biscicol-fims.props")
public class BiscicolAppConfig {
    @Autowired
    private Environment env;
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
