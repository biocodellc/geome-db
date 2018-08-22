package biocode.fims.application.config;

import org.springframework.context.annotation.*;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * configuration class for geome-fims webapp
 */
@Configuration
@EnableScheduling
@Import({GeomeAppConfig.class, FimsWebAppConfig.class})
public class GeomeWebAppConfig {

}

