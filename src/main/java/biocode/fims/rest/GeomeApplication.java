package biocode.fims.rest;

import org.glassfish.jersey.media.multipart.MultiPartFeature;

/**
 * * Jersey Application for GeOMe REST Services
 */
public class GeomeApplication extends FimsApplication {

    public GeomeApplication() {
        super();
        packages("biocode.fims.rest.services.rest");
        register(MultiPartFeature.class);
        register(GZIPWriterInterceptor.class);
    }
}
