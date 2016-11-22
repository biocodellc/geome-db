package biocode.fims.application.config;

import biocode.fims.dipnet.services.DipnetExpeditionService;
import biocode.fims.fileManagers.AuxilaryFileManager;
import biocode.fims.fileManagers.fasta.ESFastaPersistenceManager;
import biocode.fims.fileManagers.fasta.FastaFileManager;
import biocode.fims.fileManagers.fasta.FastaPersistenceManager;
import biocode.fims.fileManagers.fasta.FusekiFastaPersistenceManager;
import biocode.fims.fileManagers.fastq.FastqFileManager;
import biocode.fims.elasticSearch.ElasticSearchIndexer;
import biocode.fims.rest.services.rest.Validate;
import biocode.fims.service.OAuthProviderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * configuration class for dipnet-fims webapp
 */
@Configuration
@Import({DipnetAppConfig.class, FimsWebAppConfig.class})
public class DipnetWebAppConfig {

    @Autowired DipnetAppConfig dipnetAppConfig;
    @Autowired FimsAppConfig fimsAppConfig;
    @Autowired DipnetExpeditionService dipnetExpeditionService;
    @Autowired OAuthProviderService providerService;

    @Bean
    @Scope("prototype")
    public FastaFileManager fastaFileManager() {
        FastaPersistenceManager persistenceManager = new ESFastaPersistenceManager(dipnetAppConfig.esClient);
        return new FastaFileManager(persistenceManager, fimsAppConfig.settingsManager);
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
    public ElasticSearchIndexer esIndexer() throws Exception {
        return new ElasticSearchIndexer(dipnetAppConfig.esClient);
    }

    @Bean
    @Scope("prototype")
    public Validate validate() throws Exception {
        return new Validate(fimsAppConfig.expeditionService, dipnetExpeditionService, fileManagers(),
                dipnetAppConfig.fimsMetadataFileManager(), providerService, fimsAppConfig.settingsManager, esIndexer());
    }

}

