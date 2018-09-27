package biocode.fims.rest.services;

import biocode.fims.application.config.FimsProperties;
import biocode.fims.rest.FimsController;
import biocode.fims.rest.services.subResources.PhotosResource;
import org.glassfish.jersey.server.model.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import javax.ws.rs.Path;

/**
 * Photo API endpoint
 */
@Controller
@Path("/photos")
public class PhotoController extends FimsController {

    @Autowired
    PhotoController(FimsProperties props) {
        super(props);
    }

    /**
     * @responseType biocode.fims.rest.services.subResources.PhotosResource
     */
    @Path("/")
    public Class<PhotosResource> getPhotosResource() {
        return PhotosResource.class;
    }
}
