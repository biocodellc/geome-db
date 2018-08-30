package biocode.fims.application.config;

import org.springframework.core.env.ConfigurableEnvironment;

/**
 * @author rjewing
 */
public class GeomeProperties extends FimsProperties {

    public GeomeProperties(ConfigurableEnvironment env) {
        super(env);
    }

    public int networkId() {
        return env.getRequiredProperty("networkId", int.class);
    }
}
