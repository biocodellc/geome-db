package biocode.fims.application.config;

import biocode.fims.fileManagers.AuxilaryFileManager;
import biocode.fims.rest.services.rest.Validate;
import biocode.fims.service.OAuthProviderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * configuration class for biscicol-fims webapp
 */
@Configuration
@Import({BiscicolAppConfig.class})
public class BiscicolWebAppConfig {

    @Autowired
    private BiscicolAppConfig biscicolAppConfig;
    @Autowired
    private OAuthProviderService providerService;

    @Bean
    @Scope("prototype")
    public List<AuxilaryFileManager> fileManagers() {
        return new ArrayList<>();
    }

    @Bean
    @Scope("prototype")
    public Validate validate() throws Exception {
        return new Validate(biscicolAppConfig.fimsAppConfig.expeditionService, biscicolAppConfig.datasetFileManager(),
               fileManagers(), providerService, biscicolAppConfig.fimsAppConfig.settingsManager);
    }
}
