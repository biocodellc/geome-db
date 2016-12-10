package biocode.fims.application.config;

import biocode.fims.authorizers.QueryAuthorizer;
import biocode.fims.dipnet.services.DipnetExpeditionService;
import biocode.fims.fileManagers.AuxilaryFileManager;
import biocode.fims.fileManagers.fasta.ESFastaPersistenceManager;
import biocode.fims.fileManagers.fasta.FastaFileManager;
import biocode.fims.fileManagers.fasta.FastaPersistenceManager;
import biocode.fims.fileManagers.fastq.FastqFileManager;
import biocode.fims.elasticSearch.ElasticSearchIndexer;
import biocode.fims.rest.services.rest.ValidateController;
import biocode.fims.service.OAuthProviderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.ArrayList;
import java.util.List;

/**
 * configuration class for dipnet-fims webapp
 */
@Configuration
@EnableScheduling
@Import({DipnetAppConfig.class, FimsWebAppConfig.class})
public class DipnetWebAppConfig {

    @Autowired DipnetAppConfig dipnetAppConfig;
    @Autowired FimsAppConfig fimsAppConfig;
    @Autowired DipnetExpeditionService dipnetExpeditionService;
    @Autowired OAuthProviderService providerService;

    @Bean
    @Scope("prototype")
    public FastaFileManager fastaFileManager() {
        FastaPersistenceManager persistenceManager = new ESFastaPersistenceManager(dipnetAppConfig.esAppConfig.esClient);
        return new FastaFileManager(persistenceManager, fimsAppConfig.settingsManager, 
                fimsAppConfig.bcidService, fimsAppConfig.expeditionService);
    }

    @Bean
    @Scope("prototype")
    public FastqFileManager fastqFileManager() {
        return new FastqFileManager();
    }

    @Bean
    @Scope("prototype")
    public List<AuxilaryFileManager> fileManagers() {
        List<AuxilaryFileManager> fileManagers = new ArrayList<>();
        fileManagers.add(fastaFileManager());
        fileManagers.add(fastqFileManager());
        return fileManagers;
    }

    @Bean
    public ElasticSearchIndexer esIndexer() {
        return new ElasticSearchIndexer(dipnetAppConfig.esAppConfig.esClient);
    }

    @Bean
    @Scope("prototype")
    public ValidateController validate() throws Exception {
        return new ValidateController(fimsAppConfig.expeditionService, dipnetExpeditionService, fileManagers(),
                dipnetAppConfig.fimsMetadataFileManager(), providerService, fimsAppConfig.settingsManager, esIndexer());
    }

    @Bean
    public QueryAuthorizer queryAuthorizer() {
        return new QueryAuthorizer(dipnetAppConfig.projectService, fimsAppConfig.settingsManager);
    }

}

