package biocode.fims.dipnet.config;

import biocode.fims.application.config.FimsAppConfig;
import biocode.fims.fileManagers.dataset.DatasetFileManager;
import biocode.fims.fileManagers.dataset.DatasetPersistenceManager;
import biocode.fims.fuseki.fileManagers.dataset.FusekiDatasetPersistenceManager;
import biocode.fims.fuseki.query.elasticSearch.FusekiIndexer;
import biocode.fims.genbank.GenbankManager;
import biocode.fims.query.elasticSearch.TransportClientFactoryBean;
import biocode.fims.service.BcidService;
import biocode.fims.service.ExpeditionService;
import biocode.fims.service.ProjectService;
import biocode.fims.settings.SettingsManager;
import biocode.fims.utils.DatasetService;
import org.elasticsearch.client.Client;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

/**
 * Configuration class for and Dipnet-Fims application. Including cli and webapps
 */
@Configuration
@ComponentScan(basePackages = {"biocode.fims.service", "biocode.fims.dipnet.services"})
//@Import({FimsAppConfig.class})
@ImportResource({
        "classpath:dipnet-data-access-config.xml",
        "classpath:data-access-config.xml"
})
// declaring this here allows us to override any properties that are also included in dipnet-fims.props
@PropertySource(value = "classpath:biocode-fims.props", ignoreResourceNotFound = true)
@PropertySource("classpath:dipnet-fims.props")
public class DipnetAppConfig  extends FimsAppConfig {
    @Autowired
    Environment env;
    @Autowired
    FimsAppConfig fimsAppConfig;
    @Autowired
    BcidService bcidService;
    @Autowired
    ExpeditionService expeditionService;
    @Autowired
    ProjectService projectService;

    @Override
    @Bean
    public SettingsManager settingsManager() {
        Resource propsFileResource = new ClassPathResource("dipnet-fims.props");
        return SettingsManager.getInstance(propsFileResource);
    }

    @Bean
    public DatasetService datasetService() {
        return new DatasetService(bcidService);
    }

    @Bean
    public GenbankManager genbankManager() {
        return new GenbankManager(null, bcidService);
    }

    @Bean
    @Scope("prototype")
    public DatasetFileManager datasetFileManager() {
        DatasetPersistenceManager persistenceManager = new FusekiDatasetPersistenceManager(expeditionService, bcidService);
        return new DatasetFileManager(persistenceManager, null, expeditionService, bcidService);
    }

    @Override
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
