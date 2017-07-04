package biocode.fims.application.config;

import org.springframework.core.env.Environment;

/**
 * @author rjewing
 */
public class GeomeProperties extends FimsProperties {

    public GeomeProperties(Environment env) {
        super(env);
    }

    public int projectId() {
        return env.getRequiredProperty("projectId", int.class);
    }


}
