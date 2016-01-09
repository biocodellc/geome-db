package services;

import biocode.fims.SettingsManager;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Biscicol-Frontend utility services
 */
@Path("utils/")
public class Utils {
    @GET
    @Path("/getMapboxToken")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getMapboxToken() {
        SettingsManager sm = SettingsManager.getInstance();
        String token = sm.retrieveValue("mapboxAccessToken");

        return Response.ok("{\"accessToken\": \"" + token + "\"}").build();
    }
}
