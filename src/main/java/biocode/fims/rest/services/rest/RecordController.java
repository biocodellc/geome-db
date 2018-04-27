package biocode.fims.rest.services.rest;

import biocode.fims.application.config.FimsProperties;
import biocode.fims.rest.FimsController;
import biocode.fims.rest.services.rest.subResources.QueryController;
import biocode.fims.rest.services.rest.subResources.RecordsResource;
import org.glassfish.jersey.server.model.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import javax.ws.rs.*;

/**
 * @resourceTag Records
 */
@Controller
@Path("/records")
public class RecordController extends FimsController {

    @Autowired
    RecordController(FimsProperties props) {
        super(props);
    }

    /**
     * @responseType biocode.fims.rest.services.rest.subResources.RecordsResource
     */
    @Path("/")
    public Resource getRecordsResource() {
        return Resource.from(RecordsResource.class);
    }

    /**
     * @responseType biocode.fims.rest.services.rest.subResources.QueryController
     */
    // note: we use the regex for projectId here so the path doesn't collide w/ RecordsResource get by arkID
    @Path("{entity}")
    public Resource getQueryController() {
        return Resource.from(QueryController.class);
    }
}

