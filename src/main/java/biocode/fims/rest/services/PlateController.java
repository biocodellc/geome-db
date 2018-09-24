package biocode.fims.rest.services;

import biocode.fims.application.config.FimsProperties;
import biocode.fims.rest.FimsController;
import biocode.fims.tissues.PlateResource;
import org.glassfish.jersey.server.model.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import javax.ws.rs.Path;

/**
 * Photo API endpoint
 */
@Controller
@Path("/tissues/plates")
public class PlateController extends FimsController {

    @Autowired
    PlateController(FimsProperties props) {
        super(props);
    }

    /**
     * @responseType biocode.fims.tissues.PlateResource
     */
    @Path("/")
    public Class<PlateResource> getPlateResource() {
        return PlateResource.class;
    }
}
