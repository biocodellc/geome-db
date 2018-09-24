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

    public String tissueEntity() {
        return env.getRequiredProperty("tissueEntity").trim();
    }

    public String tissuePlateUri() {
        return env.getRequiredProperty("tissuePlateUri").trim();
    }

    public String tissueWellUri() {
        return env.getRequiredProperty("tissueWellUri").trim();
    }
}
