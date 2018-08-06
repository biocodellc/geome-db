package biocode.fims.application.config;

import biocode.fims.authorizers.QueryAuthorizer;
import biocode.fims.service.ExpeditionService;
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

    @Autowired
    GeomeAppConfig geomeAppConfig;

    @Autowired
    ExpeditionService expeditionService;

    @Bean
    public QueryAuthorizer queryAuthorizer(FimsProperties fimsProperties) {
        return new QueryAuthorizer(geomeAppConfig.projectService, expeditionService, fimsProperties);
    }

}

