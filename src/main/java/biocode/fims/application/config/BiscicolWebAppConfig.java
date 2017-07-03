package biocode.fims.application.config;

import biocode.fims.authorizers.QueryAuthorizer;
import biocode.fims.elasticSearch.ElasticSearchIndexer;
import biocode.fims.fileManagers.AuxilaryFileManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * configuration class for biscicol-fims webapp
 */
@Configuration
@Import({BiscicolAppConfig.class, FimsWebAppConfig.class})
public class BiscicolWebAppConfig {

    @Autowired
    private BiscicolAppConfig biscicolAppConfig;

    @Bean
    @Scope("prototype")
    public List<AuxilaryFileManager> fileManagers() {
        return new ArrayList<>();
    }

    //TODO move to ElasticSearchAppConfig.class
    @Bean
    public ElasticSearchIndexer esIndexer() {
        return new ElasticSearchIndexer(biscicolAppConfig.esClient);
    }

    @Bean
    public QueryAuthorizer queryAuthorizer() {
        return new QueryAuthorizer(biscicolAppConfig.projectService, biscicolAppConfig.fimsAppConfig.fimsProperties());
    }
}
