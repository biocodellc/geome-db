package biocode.fims.application.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for team-to-search-index synchronization.
 */
@Component
public class TeamIndexProperties {
    private final Environment env;

    @Autowired
    public TeamIndexProperties(Environment env) {
        this.env = env;
    }

    public boolean enabled() {
        return env.getProperty("teamIndex.enabled", Boolean.class, false);
    }

    public String elasticsearchBaseUrl() {
        return env.getProperty("teamIndex.elasticsearch.baseUrl");
    }

    public String elasticsearchIndex() {
        return env.getProperty("teamIndex.elasticsearch.index");
    }

    public String apiKey() {
        return env.getProperty("teamIndex.elasticsearch.apiKey");
    }

    public String basicUsername() {
        return env.getProperty("teamIndex.elasticsearch.username");
    }

    public String basicPassword() {
        return env.getProperty("teamIndex.elasticsearch.password");
    }

    public int bulkBatchSize() {
        return env.getProperty("teamIndex.bulk.batchSize", Integer.class, 500);
    }
}
