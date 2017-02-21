package biocode.fims.rest.services.rest;

import biocode.fims.settings.SettingsManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * geome utility services
 */
@Controller
@Path("utils/")
public class UtilsController extends FimsAbstractUtilsController {

    @Autowired
    UtilsController(SettingsManager settingsManager) {
        super(settingsManager);
    }

    @GET
    @Path("/getMapboxToken")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getMapboxToken() {
        String token = settingsManager.retrieveValue("mapboxAccessToken");

        return Response.ok("{\"accessToken\": \"" + token + "\"}").build();
    }
}
