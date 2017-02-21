package biocode.fims.application.config;

import biocode.fims.geome.repositories.GeomeResourceRepository;
import biocode.fims.fastq.EsFastqMetadataRepository;
import biocode.fims.elasticSearch.ESFimsMetadataPersistenceManager;
import biocode.fims.fastq.FastqMetadataRepository;
import biocode.fims.fileManagers.fimsMetadata.FimsMetadataFileManager;
import biocode.fims.fileManagers.fimsMetadata.FimsMetadataPersistenceManager;
import biocode.fims.ncbi.entrez.BioSampleRepository;
import biocode.fims.ncbi.entrez.EntrezApiFactory;
import biocode.fims.ncbi.entrez.EntrezApiFactoryImpl;
import biocode.fims.ncbi.entrez.EntrezApiService;
import biocode.fims.ncbi.sra.SraAccessionHarvester;
import biocode.fims.service.ProjectService;
import org.elasticsearch.client.Client;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.*;

import javax.ws.rs.client.ClientBuilder;

/**
 * Configuration class for and GeOMe-db-Fims application. Including cli and webapps
 */
@Configuration
@Import({FimsAppConfig.class, ElasticSearchAppConfig.class})
// declaring this here allows us to override any properties that are also included in geome-db.props
@PropertySource(value = "classpath:biocode-fims.props", ignoreResourceNotFound = true)
@PropertySource("classpath:geome-db.props")
public class GeomeAppConfig {
    @Autowired
    private FimsAppConfig fimsAppConfig;
    @Autowired
    ProjectService projectService;
    @Autowired
    Client esClient;
    @Autowired
    private MessageSource messageSource;

    @Bean
    @Scope("prototype")
    public FimsMetadataFileManager fimsMetadataFileManager() {
        FimsMetadataPersistenceManager persistenceManager = new ESFimsMetadataPersistenceManager(esClient, fimsAppConfig.settingsManager);
        return new FimsMetadataFileManager(persistenceManager, fimsAppConfig.settingsManager, fimsAppConfig.expeditionService, fimsAppConfig.bcidService, messageSource);
    }

    @Bean
    public FastqMetadataRepository fastqMetadataRepository() {
        return new EsFastqMetadataRepository(esClient);
    }

    @Bean
    public GeomeResourceRepository geomeResourceRepository() {
        return new GeomeResourceRepository(esClient, fastqMetadataRepository());
    }
    @Bean
    public BioSampleRepository bioSampleRepository() {
        EntrezApiFactory apiFactory = new EntrezApiFactoryImpl(ClientBuilder.newClient());
        EntrezApiService entrezApiService = new EntrezApiService(apiFactory);
        return new BioSampleRepository(entrezApiService);
    }

    @Bean
    public SraAccessionHarvester sraAccessionHarvester() {
        return new SraAccessionHarvester(geomeResourceRepository(), bioSampleRepository(), projectService, fimsAppConfig.settingsManager);
    }

}
