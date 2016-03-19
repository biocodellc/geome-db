package biocode.fims.rest;

import org.glassfish.jersey.media.multipart.MultiPartFeature;

/**
 * Created by rjewing on 3/18/16.
 */
public class BiscicolApplication extends FimsApplication {

    public BiscicolApplication() {
        super();
        packages("biocode.fims.rest.services.rest");
        register(MultiPartFeature.class);
    }
}
