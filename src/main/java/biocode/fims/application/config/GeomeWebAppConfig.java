package biocode.fims.application.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * configuration class for geome-fims webapp
 */
@Configuration
@EnableScheduling
@Import({GeomeAppConfig.class, FimsWebAppConfig.class})
public class GeomeWebAppConfig {

    // this needs to be here, otherwise we get a bean resolving error for BcidService
    @Autowired
    GeomeAppConfig geomeAppConfig;
}

