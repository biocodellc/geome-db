package biocode.fims.rest;

import biocode.fims.rest.services.subResources.QueryController;
import biocode.fims.tissues.PlateResource;
import org.glassfish.jersey.media.multipart.MultiPartFeature;

/**
 * * Jersey Application for GeOMe REST Services
 */
public class GeomeApplication extends FimsApplication {

    public GeomeApplication() {
        super();
        packages("biocode.fims.rest.services");
        register(MultiPartFeature.class);
        register(GZIPWriterInterceptor.class);
        register(QueryController.class);
        register(PlateResource.class);
    }
}
