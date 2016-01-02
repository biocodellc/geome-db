package services;

import biocode.fims.FimsService;
import biocode.fims.fimsExceptions.ServerErrorException;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * REST services dealing with projects
 */
@Path("resolver")
public class Resolver extends FimsService {
    @GET
    @Path("/{scheme}:{naan}/{shoulderPlusIdentifier}")
    public Response resolver(
            @PathParam("scheme") String scheme,
            @PathParam("naan") String naan,
            @PathParam("shoulderPlusIdentifier") String shoulderPlusIdentifier) {
        shoulderPlusIdentifier = shoulderPlusIdentifier.trim();

        // Structure the Bcid element from path parameters
        String element = scheme + "/" + naan + "/" + shoulderPlusIdentifier;

        try {
            String response = fimsConnector.createGETConnection(new URL(fimsCoreRoot + "id/" + element));
            return Response.status(fimsConnector.getResponseCode()).entity(response).build();
        } catch (MalformedURLException e) {
            throw new ServerErrorException(e);
        }
    }
}
