package biocode.fims.rest;

import org.glassfish.jersey.media.multipart.MultiPartFeature;

/**
 * * Jersey Application for Biscicol REST Services
 */
public class BiscicolApplication extends FimsApplication {

    public BiscicolApplication() {
        super();
        packages("biocode.fims.rest.services.rest");
        register(MultiPartFeature.class);
    }
}
