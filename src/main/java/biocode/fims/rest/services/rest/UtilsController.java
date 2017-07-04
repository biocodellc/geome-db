package biocode.fims.rest.services.rest;

import biocode.fims.application.config.FimsProperties;
import biocode.fims.tools.FileCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Biscicol-Fims utility services
 */
@Controller
@Path("utils/")
public class UtilsController extends FimsAbstractUtilsController {

    @Autowired
    UtilsController(FileCache fileCache, FimsProperties props) {
        super(fileCache, props);
    }

    @GET
    @Path("/getMapboxToken")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getMapboxToken() {

        return Response.ok("{\"accessToken\": \"" + props.mapboxAccessToken() + "\"}").build();
    }
}
