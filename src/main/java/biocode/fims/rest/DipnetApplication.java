package biocode.fims.rest;

import org.glassfish.jersey.media.multipart.MultiPartFeature;

/**
 * * Jersey Application for Dipnet REST Services
 */
public class DipnetApplication extends FimsApplication {

    public DipnetApplication() {
        super();
        packages("biocode.fims.rest.services.rest");
        register(MultiPartFeature.class);
    }
}
