package biocode.fims.application.config;

import biocode.fims.service.ProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;

/**
 * Configuration class for Biscicol-Fims applications. Including cli and webapps
 */
@Configuration
@Import({FimsAppConfig.class, ElasticSearchAppConfig.class})
// declaring this here allows us to override any properties that are also included in biscicol-fims.props
@PropertySource(value = "classpath:biocode-fims.props", ignoreResourceNotFound = true)
@PropertySource("classpath:biscicol-fims.props")
public class BiscicolAppConfig {
    @Autowired
    FimsAppConfig fimsAppConfig;

    @Autowired
    ProjectService projectService;
}
