package biocode.fims.rest;

import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.message.GZipEncoder;
import org.glassfish.jersey.message.filtering.EntityFilteringFeature;
import org.glassfish.jersey.server.filter.EncodingFilter;

/**
 * * Jersey Application for GeOMe REST Services
 */
public class GeomeApplication extends FimsApplication {

    public GeomeApplication() {
        super();
        packages("biocode.fims.rest.services.rest");
        register(MultiPartFeature.class);
//        register(EntityFilteringFeature.class);
//        EncodingFilter.enableFor(this, GZipEncoder.class);
    }
}
