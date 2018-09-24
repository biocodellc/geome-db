package biocode.fims.rest;

import biocode.fims.application.config.GeomeProperties;
import biocode.fims.rest.services.subResources.QueryController;
import biocode.fims.tissues.PlateResource;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * * Jersey Application for GeOMe REST Services
 */
public class GeomeApplication extends FimsApplication {

    @Autowired
    public GeomeApplication(GeomeProperties props) {
        super();
        packages("biocode.fims.rest.services");
        register(MultiPartFeature.class);
        register(GZIPWriterInterceptor.class);
        register(QueryController.class);
        register(PlateResource.class);
        register(new SingleNetworkFeature(props.networkId()));
    }
}
