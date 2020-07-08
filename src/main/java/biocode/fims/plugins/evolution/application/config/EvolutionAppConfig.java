package biocode.fims.plugins.evolution.application.config;

import biocode.fims.application.config.FimsProperties;
import biocode.fims.plugins.evolution.processing.EvolutionTaskExecutor;
import biocode.fims.plugins.evolution.run.EvolutionDatasetAction;
import biocode.fims.plugins.evolution.service.EvolutionService;
import biocode.fims.repositories.EntityIdentifierRepository;
import biocode.fims.repositories.RecordRepository;
import biocode.fims.service.ExpeditionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;

import javax.ws.rs.client.ClientBuilder;
import java.util.concurrent.Executors;

/**
 * Configuration class for biocode-fims-evolution application.
 */
@Configuration
@PropertySource(value = "classpath:biocode-fims-evolution.props", ignoreResourceNotFound = true)
@Import({EvolutionProperties.class})
public class EvolutionAppConfig {
    @Autowired
    FimsProperties fimsProperties;
    @Autowired
    EvolutionProperties evolutionProperties;

    @Autowired
    RecordRepository recordRepository;
    @Autowired
    EntityIdentifierRepository entityIdentifierRepository;
    @Autowired
    ExpeditionService expeditionService;

    @Bean
    public EvolutionService evolutionService() {
        return new EvolutionService(ClientBuilder.newClient(), evolutionProperties);
    }

    @Bean
    public EvolutionTaskExecutor evolutionTaskExecutor() {
        return new EvolutionTaskExecutor(Executors.newFixedThreadPool(1));
    }

    public EvolutionDatasetAction evolutionDatasetAction() {
        return new EvolutionDatasetAction(recordRepository, evolutionService(), expeditionService, evolutionTaskExecutor(), entityIdentifierRepository, evolutionProperties, fimsProperties);
    }
}
