package biocode.fims.rest;

import biocode.fims.rest.services.rest.subResources.QueryController;
import org.glassfish.jersey.media.multipart.MultiPartFeature;

/**
 * * Jersey Application for Biscicol REST Services
 */
public class BiscicolApplication extends FimsApplication {

    public BiscicolApplication() {
        super();
        packages("biocode.fims.rest.services.rest");
        register(MultiPartFeature.class);
        register(GZIPWriterInterceptor.class);
        register(QueryController.class);
    }
}
