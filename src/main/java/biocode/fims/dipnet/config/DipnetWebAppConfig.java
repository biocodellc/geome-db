package biocode.fims.dipnet.config;

import biocode.fims.application.config.FimsWebAppConfig;
import biocode.fims.dipnet.services.DipnetExpeditionService;
import biocode.fims.fileManagers.AuxilaryFileManager;
import biocode.fims.fileManagers.fasta.FastaFileManager;
import biocode.fims.fileManagers.fasta.FastaPersistenceManager;
import biocode.fims.fileManagers.fasta.FusekiFastaPersistenceManager;
import biocode.fims.fileManagers.fastq.FastqFileManager;
import biocode.fims.rest.services.rest.Validate;
import biocode.fims.service.BcidService;
import biocode.fims.service.ExpeditionService;
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
@Import({DipnetAppConfig.class})
@ComponentScan(basePackages = {"biocode.fims.rest"})
@EnableScheduling
@EnableAspectJAutoProxy
public class DipnetWebAppConfig extends FimsWebAppConfig {

    @Autowired DipnetAppConfig dipnetAppConfig;
    @Autowired BcidService bcidService;
    @Autowired ExpeditionService expeditionService;
    @Autowired DipnetExpeditionService dipnetExpeditionService;
    @Autowired OAuthProviderService providerService;

    @Bean
    @Scope("prototype")
    public FastaFileManager fastaFileManager() {
        FastaPersistenceManager persistenceManager = new FusekiFastaPersistenceManager(bcidService, expeditionService);
        return new FastaFileManager(persistenceManager, null, bcidService);
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
    @Scope("prototype")
    public Validate validate() throws Exception {
        return new Validate(expeditionService, dipnetExpeditionService, fileManagers(),
                dipnetAppConfig.datasetFileManager(), providerService, null, dipnetAppConfig.esClient());
    }

}

