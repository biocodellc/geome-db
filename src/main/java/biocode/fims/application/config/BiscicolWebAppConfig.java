package biocode.fims.application.config;

import biocode.fims.fileManagers.AuxilaryFileManager;
import biocode.fims.rest.services.rest.ValidateController;
import biocode.fims.service.OAuthProviderService;
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
    @Autowired
    private OAuthProviderService providerService;

    @Bean
    @Scope("prototype")
    public List<AuxilaryFileManager> fileManagers() {
        return new ArrayList<>();
    }

    @Bean
    @Scope("prototype")
    public ValidateController validate() throws Exception {
        return new ValidateController(biscicolAppConfig.fimsAppConfig.expeditionService, biscicolAppConfig.FimsMetadataFileManager(),
               fileManagers(), providerService, biscicolAppConfig.fimsAppConfig.settingsManager);
    }
}
