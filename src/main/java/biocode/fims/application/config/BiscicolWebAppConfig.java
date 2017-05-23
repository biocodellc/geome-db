package biocode.fims.application.config;

import biocode.fims.authorizers.QueryAuthorizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;

/**
 * configuration class for biscicol-fims webapp
 */
@Configuration
@Import({BiscicolAppConfig.class, FimsWebAppConfig.class})
public class BiscicolWebAppConfig {

    @Autowired
    private BiscicolAppConfig biscicolAppConfig;

    @Bean
    public QueryAuthorizer queryAuthorizer() {
        return new QueryAuthorizer(biscicolAppConfig.projectService, biscicolAppConfig.fimsAppConfig.settingsManager);
    }
}
