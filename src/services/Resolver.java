package services;

import biocode.fims.FimsService;
import org.glassfish.jersey.client.ClientResponse;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

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

        return fimsConnector.createGETConnection(fimsCoreRoot + "id/" + element);
    }
}
