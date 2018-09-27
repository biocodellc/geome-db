package biocode.fims.rest.services;

import biocode.fims.application.config.FimsProperties;
import biocode.fims.rest.FimsController;
import biocode.fims.rest.services.subResources.QueryController;
import biocode.fims.rest.services.subResources.RecordsResource;
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
     * @responseType biocode.fims.rest.services.subResources.RecordsResource
     */
    @Path("/")
    public Class<RecordsResource> getRecordsResource() {
        return RecordsResource.class;
    }

    /**
     * @responseType biocode.fims.rest.services.subResources.QueryController
     * @resourceDescription Query a project's records. See <a href='http://fims.readthedocs.io/en/latest/fims/query.html'>Fims Docs</a>
     * for more detailed information regarding queries.
     * @resourceTag Query Records
     */
    // note: we use the regex for entity here so the paths don't collide w/ RecordsResource get by arkID
    @Path("{entity: [a-zA-Z0-9_]+}")
    public Class<QueryController> getQueryController() {
        return QueryController.class;
    }
}

