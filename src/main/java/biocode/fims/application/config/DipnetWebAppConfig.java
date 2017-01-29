package biocode.fims.application.config;

import biocode.fims.authorizers.QueryAuthorizer;
import biocode.fims.fastq.fileManagers.ESFastqPersistenceManager;
import biocode.fims.fastq.fileManagers.FastqPersistenceManager;
import biocode.fims.fileManagers.AuxilaryFileManager;
import biocode.fims.fasta.fileManagers.ESFastaPersistenceManager;
import biocode.fims.fasta.fileManagers.FastaFileManager;
import biocode.fims.fasta.fileManagers.FastaPersistenceManager;
import biocode.fims.fastq.fileManagers.FastqFileManager;
import biocode.fims.elasticSearch.ElasticSearchIndexer;
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

    @Bean
    @Scope("prototype")
    public FastaFileManager fastaFileManager() {
        FastaPersistenceManager persistenceManager = new ESFastaPersistenceManager(dipnetAppConfig.esClient);
        return new FastaFileManager(persistenceManager, fimsAppConfig.settingsManager, 
                fimsAppConfig.bcidService, fimsAppConfig.expeditionService);
    }

    @Bean
    @Scope("prototype")
    public FastqFileManager fastqFileManager() {
        FastqPersistenceManager persistenceManager = new ESFastqPersistenceManager(dipnetAppConfig.esClient);
        return new FastqFileManager(persistenceManager, fimsAppConfig.expeditionService, fimsAppConfig.bcidService,
                fimsAppConfig.settingsManager);
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
        return new ElasticSearchIndexer(dipnetAppConfig.esClient);
    }

    @Bean
    public QueryAuthorizer queryAuthorizer() {
        return new QueryAuthorizer(dipnetAppConfig.projectService, fimsAppConfig.settingsManager);
    }

}

