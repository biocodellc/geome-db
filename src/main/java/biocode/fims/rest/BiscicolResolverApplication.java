package biocode.fims.rest;

import org.glassfish.jersey.server.mvc.jsp.JspMvcFeature;

/**
 * Created by rjewing on 3/18/16.
 */
public class BiscicolResolverApplication extends FimsApplication {

    public BiscicolResolverApplication() {
        super();
        packages("biocode.fims.rest.services.id");
        register(JspMvcFeature.class);
    }
}
