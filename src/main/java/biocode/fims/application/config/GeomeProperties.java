package biocode.fims.application.config;

import org.springframework.core.env.ConfigurableEnvironment;

/**
 * @author rjewing
 */
public class GeomeProperties extends FimsProperties implements NetworkProperties {

    public GeomeProperties(ConfigurableEnvironment env) {
        super(env);
    }

    @Override
    public Integer networkId() {
        return env.getRequiredProperty("networkId", int.class);
    }

    public int sraFetchWeeksInPast() {
        return env.getProperty("sraFetchWeeksInPast", int.class, 2);
    }

    public String sraApiKey() {
        return env.getRequiredProperty("sraApiKey");
    }
}
