package biocode.fims.rest.services;

import biocode.fims.application.config.FimsProperties;
import biocode.fims.rest.FimsController;
import biocode.fims.rest.services.subResources.SraResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import javax.inject.Singleton;
import javax.ws.rs.Path;

/**
 * SRA API endpoint
 */
@Controller
@Path("/sra")
@Singleton
public class SraController extends FimsController {

    @Autowired
    SraController(FimsProperties props) {
        super(props);
    }

    /**
     * @responseType biocode.fims.rest.services.subResources.SraResource
     */
    @Path("/")
    public Class<SraResource> sra() {
        return SraResource.class;
    }
}
